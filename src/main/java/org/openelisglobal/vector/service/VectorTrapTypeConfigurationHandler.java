package org.openelisglobal.vector.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.vector.valueholder.VectorTrapType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads vector trap types from {@code vector-trap-types.csv} at startup.
 *
 * <p>
 * Replaces the inline {@code <sql>INSERT</sql>} seeds in
 * {@code 020-vector-surveillance-schema.xml} which couldn't populate the
 * {@code vector_trap_type_sample_type} join table because they ran during
 * Liquibase, before the CSV-driven {@code TypeOfSampleConfigurationHandler} had
 * created the {@code Mosquito} sample-type row.
 *
 * <p>
 * This handler runs in the configuration-handler chain, scheduled
 * <strong>after</strong> sample types
 * ({@link org.openelisglobal.typeofsample.service.TypeOfSampleConfigurationHandler}
 * at order 100) and after vector species (order 400), so all sample-type-abbrev
 * lookups resolve. Idempotent: rows are matched by name (case-insensitive) and
 * updated in place; new rows are created.
 *
 * <h2>CSV format</h2>
 *
 * <pre>
 *   name,description,sampleTypeAbbrevs,active
 *   BG-Sentinel Trap,"CO2-baited mosquito trap",MOSQUITO,Y
 *   Mixed-Vector Trap,"covers two domains",MOSQUITO;TICK,Y
 * </pre>
 *
 * <ul>
 * <li>{@code sampleTypeAbbrevs} — semicolon-separated
 * {@code type_of_sample.local_abbrev} values; resolved against
 * {@code domain='V'} only. Unresolved abbrevs are dropped with a warning; if
 * all abbrevs fail to resolve the row is skipped.</li>
 * <li>{@code active} — {@code Y} or {@code N}; missing / blank treated as
 * active.</li>
 * <li>Lines starting with {@code #} are comments.</li>
 * </ul>
 */
@Component
public class VectorTrapTypeConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private VectorTrapTypeService vectorTrapTypeService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Override
    public String getDomainName() {
        return "vector-trap-types";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        // After sample-types (100), dictionaries (300), and vector species (400).
        return 500;
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = null;
        String readLine;
        while ((readLine = reader.readLine()) != null) {
            String trimmed = readLine.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                headerLine = readLine;
                break;
            }
        }
        if (headerLine == null) {
            throw new IllegalArgumentException("Vector trap-types file " + fileName + " is empty or has no header");
        }

        String[] headers = parseCsvLine(headerLine);
        int nameIdx = findColumn(headers, "name");
        int descIdx = findColumn(headers, "description");
        int sampleTypesIdx = findColumn(headers, "sampleTypeAbbrevs");
        int activeIdx = findColumn(headers, "active");

        if (nameIdx < 0 || sampleTypesIdx < 0) {
            throw new IllegalArgumentException(
                    "Vector trap-types CSV " + fileName + " must have name and sampleTypeAbbrevs columns");
        }

        int loaded = 0;
        int lineNum = 1;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            try {
                if (processRow(parseCsvLine(line), nameIdx, descIdx, sampleTypesIdx, activeIdx)) {
                    loaded++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error on line " + lineNum + " of " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Loaded " + loaded + " vector trap types from " + fileName);
    }

    private boolean processRow(String[] values, int nameIdx, int descIdx, int sampleTypesIdx, int activeIdx) {
        String name = get(values, nameIdx);
        if (name.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow", "Skipping row with empty name");
            return false;
        }

        String description = get(values, descIdx);
        boolean isActive = !"N".equalsIgnoreCase(get(values, activeIdx));

        Set<String> sampleTypeIds = resolveSampleTypeIds(get(values, sampleTypesIdx));
        if (sampleTypeIds.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow",
                    "No resolvable sampleTypeAbbrevs for trap type '" + name + "' — skipping");
            return false;
        }

        VectorTrapType existing = findByName(name);
        if (existing != null) {
            VectorTrapType patch = new VectorTrapType();
            patch.setName(name);
            patch.setDescription(description.isEmpty() ? null : description);
            patch.setActive(isActive);
            vectorTrapTypeService.patchUpdate(existing.getId(), patch, sampleTypeIds, "1");
            LogEvent.logDebug(this.getClass().getSimpleName(), "processRow", "Updated vector trap type: " + name);
        } else {
            VectorTrapType newType = new VectorTrapType();
            newType.setName(name);
            newType.setDescription(description.isEmpty() ? null : description);
            newType.setActive(isActive);
            vectorTrapTypeService.create(newType, sampleTypeIds, "1");
            LogEvent.logInfo(this.getClass().getSimpleName(), "processRow", "Created vector trap type: " + name);
        }
        return true;
    }

    /**
     * Resolve a {@code MOSQUITO;TICK} list into stringified TypeOfSample IDs,
     * scoped to {@code domain='V'}. Unknown abbrevs are dropped with a warning
     * rather than aborting the row — a typo in one abbrev shouldn't block the
     * trap-type from being seeded against the others.
     */
    private Set<String> resolveSampleTypeIds(String abbrevList) {
        Set<String> ids = new HashSet<>();
        if (abbrevList == null || abbrevList.isEmpty()) {
            return ids;
        }
        for (String raw : abbrevList.split(";")) {
            String abbrev = raw.trim();
            if (abbrev.isEmpty()) {
                continue;
            }
            TypeOfSample tos = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(abbrev, "V");
            if (tos == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "resolveSampleTypeIds",
                        "Unknown vector sampleTypeAbbrev '" + abbrev + "' — dropping");
                continue;
            }
            ids.add(tos.getId());
        }
        return ids;
    }

    private VectorTrapType findByName(String name) {
        // VectorTrapTypeService doesn't expose a getByName, so iterate
        // the (small) list of all trap types. The catalog is rarely more
        // than a few dozen rows.
        List<VectorTrapType> all = vectorTrapTypeService.getAll();
        for (VectorTrapType t : all) {
            if (name.equalsIgnoreCase(t.getName())) {
                return t;
            }
        }
        return null;
    }

    private String get(String[] values, int idx) {
        if (idx < 0 || idx >= values.length) {
            return "";
        }
        return values[idx] != null ? values[idx] : "";
    }

    private int findColumn(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (name.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Minimal CSV parser supporting double-quoted fields that contain commas.
     * Mirrors {@link VectorSpeciesConfigurationHandler}'s parser for consistency.
     */
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }
}
