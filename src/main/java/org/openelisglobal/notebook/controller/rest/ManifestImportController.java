package org.openelisglobal.notebook.controller.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.form.ManifestImportForm;
import org.openelisglobal.notebook.service.ManifestImportService;
import org.openelisglobal.notebook.service.ManifestImportService.ManifestImportResult;
import org.openelisglobal.notebook.service.ManifestImportService.ParseError;
import org.openelisglobal.notebook.service.ManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for manifest CSV import operations. Handles uploading
 * manifest files and creating samples for notebook entries.
 *
 * <p>
 * The primary endpoint is {@code POST
 * /rest/notebook/entry/{entryId}/page/{pageId}/import-manifest}, which is fully
 * generic — it drives validation entirely from the {@code manifestColumns}
 * schema stored in the page's {@code config} JSONB column. No per-lab Java code
 * is needed.
 */
@RestController
@RequestMapping(value = "/rest/notebook")
public class ManifestImportController extends BaseRestController {

    /**
     * Fields routed through {@link ManifestImportForm} →
     * {@link ManifestImportService} for core
     * {@link org.openelisglobal.sampleitem.valueholder.SampleItem} creation. All
     * other mapped fields are stored as extra data in
     * {@link NotebookPageSample#data}.
     */
    private static final Set<String> CORE_MANIFEST_FIELDS = Set.of("groupId", "sampleType", "collectionDate", "volume",
            "numOfSamples");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ManifestImportService manifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookPageDAO noteBookPageDAO;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    // ── legacy endpoints (kept for backward compatibility) ─────────────────────

    /**
     * Preview manifest CSV for a notebook entry. POST
     * /rest/notebook/entry/{entryId}/samples/preview-manifest
     */
    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") ManifestImportForm form) {

        if (notebookEntryService.getMatch("id", entryId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            ParsedManifest parsed = manifestImportService.parseManifestCsv(inputStream, form);
            List<ParseError> allErrors = new ArrayList<>(parsed.errors());
            allErrors.addAll(manifestImportService.validateSampleTypes(parsed));

            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("totalRows", parsed.rows().size());
            response.put("totalSamples",
                    parsed.rows().stream().mapToInt(ManifestImportService.ManifestRow::numOfSamples).sum());
            response.put("rows", parsed.rows().stream().map(row -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowNumber", row.rowNumber());
                rowMap.put("groupId", row.groupId());
                rowMap.put("sampleType", row.sampleType());
                rowMap.put("collectionDate", row.collectionDate());
                rowMap.put("volume", row.volume());
                rowMap.put("numOfSamples", row.numOfSamples());
                rowMap.put("notes", row.notes());
                return rowMap;
            }).collect(Collectors.toList()));
            response.put("errors", toErrorList(allErrors));
            response.put("valid", allErrors.isEmpty());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }

    /**
     * Create samples from manifest CSV for a notebook entry. POST
     * /rest/notebook/entry/{entryId}/samples/create-from-manifest
     */
    @PostMapping(value = "/entry/{entryId}/samples/create-from-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSamplesForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") ManifestImportForm form,
            HttpServletRequest httpRequest) {

        if (notebookEntryService.getMatch("id", entryId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try (InputStream inputStream = file.getInputStream()) {
            ParsedManifest parsed = manifestImportService.parseManifestCsv(inputStream, form);
            if (!parsed.errors().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "CSV parsing errors",
                        "errors", toErrorList(parsed.errors())));
            }

            List<ParseError> validationErrors = manifestImportService.validateSampleTypes(parsed);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error",
                        "Sample type validation errors", "errors", toErrorList(validationErrors)));
            }

            ManifestImportResult result = manifestImportService.createSamplesForEntry(entryId, parsed, sysUserId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.errors().isEmpty());
            response.put("entryId", entryId);
            response.put("totalRequested", result.totalRequested());
            response.put("totalCreated", result.totalCreated());
            response.put("createdSamples", result.createdSamples().stream().map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", s.getId());
                m.put("externalId", s.getExternalId());
                m.put("sampleType", s.getTypeOfSample() != null ? s.getTypeOfSample().getDescription() : null);
                return m;
            }).collect(Collectors.toList()));
            if (!result.errors().isEmpty()) {
                response.put("errors", toErrorList(result.errors()));
            }
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }

    // ── primary generic endpoint ───────────────────────────────────────────────

    /**
     * Generic manifest import for any lab page. Driven entirely by the
     * {@code manifestColumns} schema in the page's {@code config} JSONB column — no
     * per-lab Java code required.
     *
     * <p>
     * Validation pipeline (in order):
     * <ol>
     * <li>All {@code required} fields are mapped to a CSV column.</li>
     * <li>CSV parses without structural errors (missing groupId / sampleType).</li>
     * <li>Per-row values satisfy {@code validValues} constraints and required
     * checks.</li>
     * <li>TypeOfSample DB lookup — only when {@code sampleType} has <em>no</em>
     * {@code validValues} in config (config is then the source of truth).</li>
     * </ol>
     *
     * <p>
     * POST /rest/notebook/entry/{entryId}/page/{pageId}/import-manifest
     *
     * @param entryId       notebook entry to attach samples to
     * @param pageId        template page whose {@code manifestColumns} drives
     *                      parsing
     * @param file          uploaded CSV file
     * @param columnMapping JSON object {@code { fieldKey → csvHeaderName }}, e.g.
     *                      {@code {"groupId":"Sample ID","projectName":"Project"}}
     * @param request       HTTP request (for user session)
     */
    @PostMapping(value = "/entry/{entryId}/page/{pageId}/import-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> genericImportManifest(@PathVariable("entryId") Integer entryId,
            @PathVariable("pageId") Integer pageId, @RequestPart("file") MultipartFile file,
            @RequestPart("columnMapping") String columnMapping, HttpServletRequest request) {

        // ── 1. Guard clauses ───────────────────────────────────────────────────
        if (notebookEntryService.getMatch("id", entryId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        NoteBookPage page = noteBookPageDAO.get(pageId).orElse(null);
        if (page == null) {
            return ResponseEntity.notFound().build();
        }
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        // ── 2. Parse column-mapping JSON ───────────────────────────────────────
        Map<String, String> userMapping;
        try {
            userMapping = objectMapper.readValue(columnMapping, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid columnMapping JSON: " + e.getMessage()));
        }

        // ── 3. Load schema from page config ───────────────────────────────────
        List<Map<String, Object>> manifestColumns = getManifestColumns(page);

        // ── 4. Validate required-field mappings (before touching the CSV) ──────
        List<String> mappingErrors = validateRequiredMappings(userMapping, manifestColumns);
        if (!mappingErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error",
                    "Required fields are not mapped to CSV columns", "validationErrors", mappingErrors));
        }

        // ── 5. Read CSV bytes once (reused for parsing, validation, extra fields) ─
        byte[] csvBytes;
        try {
            csvBytes = file.getBytes();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }

        // ── 6. Parse core fields via service ──────────────────────────────────
        ManifestImportForm form = buildCoreForm(userMapping);
        ParsedManifest parsed;
        try (InputStream in = new ByteArrayInputStream(csvBytes)) {
            parsed = manifestImportService.parseManifestCsv(in, form);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to parse CSV: " + e.getMessage()));
        }

        if (!parsed.errors().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "error", "CSV parsing errors", "errors", toErrorList(parsed.errors())));
        }

        // ── 7. Config-driven per-row validation (validValues + required checks) ─
        // This validates ALL fields — core and lab-specific — against the
        // manifestColumns schema. It is the primary validation gate.
        List<ParseError> configErrors = validateConfigValues(csvBytes, userMapping, manifestColumns);
        if (!configErrors.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Validation errors", "errors", toErrorList(configErrors)));
        }

        // ── 8. TypeOfSample DB lookup — skipped when config provides validValues ─
        // When manifestColumns defines validValues for sampleType, the config IS
        // the source of truth and the DB lookup is redundant (and would reject
        // lab-specific values like "DNA - Genomic" not in the global catalog).
        if (!hasSampleTypeValidValues(manifestColumns)) {
            List<ParseError> typeErrors = manifestImportService.validateSampleTypes(parsed);
            if (!typeErrors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error",
                        "Sample type validation errors", "errors", toErrorList(typeErrors)));
            }
        }

        // ── 9. Create samples ──────────────────────────────────────────────────
        ManifestImportResult result = manifestImportService.createSamplesForEntry(entryId, parsed, sysUserId);

        // ── 10. Store lab-specific extra fields in NotebookPageSample.data ──────
        Map<String, String> extraMapping = userMapping.entrySet().stream().filter(
                e -> !CORE_MANIFEST_FIELDS.contains(e.getKey()) && e.getValue() != null && !e.getValue().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!extraMapping.isEmpty() && !result.createdSamples().isEmpty()) {
            storeExtraFields(csvBytes, extraMapping, userMapping.get("numOfSamples"), pageId, result.createdSamples());
        }

        // ── 11. Build response ─────────────────────────────────────────────────
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.errors().isEmpty());
        response.put("entryId", entryId);
        response.put("totalRequested", result.totalRequested());
        response.put("totalCreated", result.totalCreated());
        response.put("createdSamples", result.createdSamples().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("externalId", s.getExternalId());
            m.put("sampleType", s.getTypeOfSample() != null ? s.getTypeOfSample().getDescription() : null);
            return m;
        }).collect(Collectors.toList()));
        if (!result.errors().isEmpty()) {
            response.put("errors", toErrorList(result.errors()));
        }
        return ResponseEntity.ok(response);
    }

    // ── config-schema helpers ─────────────────────────────────────────────────

    /**
     * Extracts the {@code manifestColumns} list from {@code page.config}, falling
     * back to {@code page.data} for pages that pre-date the config/data split
     * (Liquibase migration 030).
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getManifestColumns(NoteBookPage page) {
        Map<String, Object> source = page.getConfig() != null ? page.getConfig() : page.getData();
        if (source == null)
            return List.of();
        Object mc = source.get("manifestColumns");
        if (mc instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
            return (List<Map<String, Object>>) mc;
        }
        return List.of();
    }

    /**
     * Returns {@code true} when the {@code sampleType} field in
     * {@code manifestColumns} carries a non-empty {@code validValues} list. In that
     * case, config is the source of truth for sample-type validation and the
     * TypeOfSample DB lookup should be skipped.
     */
    private boolean hasSampleTypeValidValues(List<Map<String, Object>> manifestColumns) {
        return manifestColumns.stream().anyMatch(col -> "sampleType".equals(col.get("field"))
                && col.get("validValues") instanceof List<?> vv && !vv.isEmpty());
    }

    /**
     * Checks that every {@code required} field in {@code manifestColumns} has a
     * non-blank entry in {@code userMapping}. Returns human-readable error messages
     * suitable for display in the frontend mapping step.
     */
    private List<String> validateRequiredMappings(Map<String, String> userMapping,
            List<Map<String, Object>> manifestColumns) {
        List<String> errors = new ArrayList<>();
        for (Map<String, Object> col : manifestColumns) {
            if (!Boolean.TRUE.equals(col.get("required")))
                continue;
            String field = (String) col.get("field");
            if (field == null)
                continue;
            String mapped = userMapping.get(field);
            if (mapped == null || mapped.isBlank()) {
                String label = col.get("label") instanceof String s ? s : field;
                errors.add("Required field '" + label + "' is not mapped to any CSV column");
            }
        }
        return errors;
    }

    /**
     * Validates every CSV data row against the constraints declared in
     * {@code manifestColumns}:
     * <ul>
     * <li>{@code required} — the cell must be non-blank.</li>
     * <li>{@code validValues} — the trimmed cell value must match one of the
     * allowed strings (case-insensitive).</li>
     * </ul>
     *
     * <p>
     * Only columns that are either {@code required} or carry {@code validValues}
     * are examined, so unmapped optional columns are silently skipped.
     *
     * <p>
     * Uses proper quoted-CSV parsing (RFC 4180) — cell values containing commas are
     * handled correctly.
     *
     * @param csvBytes        raw CSV bytes (already in memory)
     * @param userMapping     {@code fieldKey → csvHeaderName} from the user
     * @param manifestColumns the page's column schema from config
     * @return per-row errors; empty list means all rows are valid
     */
    private List<ParseError> validateConfigValues(byte[] csvBytes, Map<String, String> userMapping,
            List<Map<String, Object>> manifestColumns) {

        // Narrow to columns that actually have constraints
        List<Map<String, Object>> constrainedCols = manifestColumns.stream()
                .filter(col -> Boolean.TRUE.equals(col.get("required"))
                        || (col.get("validValues") instanceof List<?> vv && !vv.isEmpty()))
                .collect(Collectors.toList());

        if (constrainedCols.isEmpty())
            return List.of();

        List<ParseError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null)
                return List.of();

            // Build header-name → column-index (case-insensitive)
            String[] headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Pre-build per-column context: field, label, required, validValues, index
            // Only include columns that are actually mapped to a CSV column.
            List<String[]> ctxField = new ArrayList<>();
            List<String[]> ctxLabel = new ArrayList<>();
            List<boolean[]> ctxRequired = new ArrayList<>();
            List<List<String>> ctxValidValues = new ArrayList<>();
            List<int[]> ctxIdx = new ArrayList<>();

            for (Map<String, Object> col : constrainedCols) {
                String field = (String) col.get("field");
                if (field == null)
                    continue;
                String csvHeader = userMapping.get(field);
                if (csvHeader == null || csvHeader.isBlank())
                    continue; // not mapped
                Integer colIdx = headerIndex.get(csvHeader.trim().toLowerCase());
                if (colIdx == null)
                    continue; // CSV header not found

                @SuppressWarnings("unchecked")
                List<String> vv = col.get("validValues") instanceof List<?> lst ? (List<String>) lst : List.of();
                String label = col.get("label") instanceof String s ? s : field;

                ctxField.add(new String[] { field });
                ctxLabel.add(new String[] { label });
                ctxRequired.add(new boolean[] { Boolean.TRUE.equals(col.get("required")) });
                ctxValidValues.add(vv);
                ctxIdx.add(new int[] { colIdx });
            }

            if (ctxField.isEmpty())
                return List.of();

            String line;
            int rowNumber = 1; // header = row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank())
                    continue;
                String[] vals = parseCsvLine(line);

                for (int c = 0; c < ctxField.size(); c++) {
                    String field = ctxField.get(c)[0];
                    String label = ctxLabel.get(c)[0];
                    boolean required = ctxRequired.get(c)[0];
                    List<String> vv = ctxValidValues.get(c);
                    int colIdx = ctxIdx.get(c)[0];

                    String val = colIdx < vals.length ? vals[colIdx].trim() : "";

                    if (val.isEmpty()) {
                        if (required) {
                            errors.add(new ParseError(rowNumber, field, "'" + label + "' is required but empty"));
                        }
                        continue; // nothing more to check for blank cells
                    }

                    if (!vv.isEmpty()) {
                        boolean valid = vv.stream().anyMatch(v -> v.equalsIgnoreCase(val));
                        if (!valid) {
                            // Truncate the valid-values list in the message to keep it readable
                            String allowed = vv.size() <= 10 ? String.join(", ", vv)
                                    : String.join(", ", vv.subList(0, 10)) + ", … (" + vv.size() + " total)";
                            errors.add(new ParseError(rowNumber, field,
                                    "Invalid value '" + val + "' for '" + label + "'. Allowed: " + allowed));
                        }
                    }
                }
            }

        } catch (IOException e) {
            errors.add(new ParseError(0, "file", "Failed to validate CSV: " + e.getMessage()));
        }

        return errors;
    }

    // ── import helpers ─────────────────────────────────────────────────────────

    /**
     * Maps the subset of core fields from userMapping into a ManifestImportForm.
     */
    private ManifestImportForm buildCoreForm(Map<String, String> userMapping) {
        ManifestImportForm form = new ManifestImportForm();
        form.setGroupIdColumn(userMapping.get("groupId"));
        form.setSampleTypeColumn(userMapping.get("sampleType"));
        form.setCollectionDateColumn(userMapping.get("collectionDate"));
        form.setVolumeColumn(userMapping.get("volume"));
        form.setNumOfSamplesColumn(userMapping.get("numOfSamples"));
        form.setNotesColumn(userMapping.get("notes"));
        return form;
    }

    /**
     * Re-parses the CSV and stores lab-specific extra column values in each
     * sample's {@link NotebookPageSample#data}.
     *
     * <p>
     * <strong>Optimisation — batch read:</strong> all existing
     * {@link NotebookPageSample} records for the page are loaded in a
     * <em>single</em> query (keyed by sampleItemId), eliminating the N individual
     * lookups the previous implementation performed. Individual {@code update()}
     * calls are still issued per sample, but within a single request the overall DB
     * round-trips drop from {@code 2N} to {@code N+1}.
     *
     * <p>
     * <strong>Correctness — proper CSV parsing:</strong> uses
     * {@link #parseCsvLine(String)} (RFC 4180 compliant) instead of
     * {@link String#split(String)}, so cell values containing commas (e.g.,
     * {@code "Bale Mountains, Oromia"}) are handled correctly.
     */
    private void storeExtraFields(byte[] csvBytes, Map<String, String> extraMapping, String numOfSamplesColName,
            Integer pageId, List<SampleItem> createdSamples) {

        // Batch-load all NotebookPageSample records for this page in one query
        List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(pageId);
        Map<String, NotebookPageSample> sampleById = new HashMap<>(pageSamples.size() * 2);
        for (NotebookPageSample ps : pageSamples) {
            // Keep first entry on duplicate key (shouldn't happen, but be safe)
            sampleById.putIfAbsent(ps.getSampleItemId(), ps);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null)
                return;

            String[] headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // fieldKey → column-index for each mapped extra field
            Map<String, Integer> extraIndexes = new HashMap<>();
            for (Map.Entry<String, String> e : extraMapping.entrySet()) {
                Integer idx = headerIndex.get(e.getValue().trim().toLowerCase());
                if (idx != null)
                    extraIndexes.put(e.getKey(), idx);
            }
            if (extraIndexes.isEmpty())
                return;

            Integer numOfSamplesIdx = (numOfSamplesColName != null && !numOfSamplesColName.isBlank())
                    ? headerIndex.get(numOfSamplesColName.trim().toLowerCase())
                    : null;

            int sampleCursor = 0;
            String line;
            while ((line = reader.readLine()) != null && sampleCursor < createdSamples.size()) {
                if (line.isBlank())
                    continue;
                String[] vals = parseCsvLine(line); // RFC 4180 — handles commas inside quoted fields

                // Determine how many SampleItems were created for this CSV row
                int n = 1;
                if (numOfSamplesIdx != null && numOfSamplesIdx < vals.length) {
                    try {
                        int parsed = Integer.parseInt(vals[numOfSamplesIdx].trim());
                        if (parsed > 0)
                            n = parsed;
                    } catch (NumberFormatException ignored) {
                    }
                }

                // Collect extra values for this row
                Map<String, Object> extraData = new HashMap<>();
                for (Map.Entry<String, Integer> e : extraIndexes.entrySet()) {
                    int idx = e.getValue();
                    if (idx < vals.length) {
                        String val = vals[idx].trim();
                        if (!val.isEmpty())
                            extraData.put(e.getKey(), val);
                    }
                }

                // Merge extra data into each sample created from this CSV row
                if (!extraData.isEmpty()) {
                    for (int i = 0; i < n && sampleCursor + i < createdSamples.size(); i++) {
                        String itemId = createdSamples.get(sampleCursor + i).getId();
                        NotebookPageSample ps = sampleById.get(itemId);
                        if (ps != null) {
                            Map<String, Object> merged = ps.getData() != null ? new HashMap<>(ps.getData())
                                    : new HashMap<>();
                            merged.putAll(extraData);
                            ps.setData(merged);
                            notebookPageSampleService.update(ps);
                        }
                    }
                }
                sampleCursor += n;
            }

        } catch (IOException e) {
            // Non-fatal — core import succeeded; log and continue.
            // Extra fields will be absent but the samples are created.
            org.openelisglobal.common.log.LogEvent.logWarn(getClass().getSimpleName(), "storeExtraFields",
                    "Failed to store extra manifest fields for page " + pageId + ": " + e.getMessage());
        }
    }

    // ── CSV utility ────────────────────────────────────────────────────────────

    /**
     * Parses a single CSV line according to RFC 4180:
     * <ul>
     * <li>Fields enclosed in double-quotes may contain commas.</li>
     * <li>A double-quote inside a quoted field is escaped as {@code ""}.</li>
     * <li>Leading/trailing whitespace outside quotes is trimmed.</li>
     * </ul>
     *
     * <p>
     * This is intentionally kept as a self-contained helper to avoid a dependency
     * on an external CSV library for this lightweight use case.
     */
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    // Peek ahead: "" inside quotes = escaped quote character
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // consume the second quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    values.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }

    // ── response helpers ───────────────────────────────────────────────────────

    private List<Map<String, Object>> toErrorList(List<ParseError> errors) {
        return errors.stream().map(err -> {
            Map<String, Object> m = new HashMap<>();
            m.put("rowNumber", err.rowNumber());
            m.put("column", err.column());
            m.put("message", err.message());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        return usd == null ? null : String.valueOf(usd.getSystemUserId());
    }
}
