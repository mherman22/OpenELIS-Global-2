package org.openelisglobal.compliance.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandardStatus;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain configuration handler for compliance standards.
 *
 * Loads compliance standards from CSV files at deployment time,
 * following the OpenELIS domain handler pattern for data initialization.
 *
 * Expected CSV format:
 * name,displayName,regulationNumber,issuingBody,effectiveDate,expirationDate,version,status,description,parameterTypes,thresholds,units,supersededById,isPreSeeded,localization:en,localization:id
 * PM.No.22/2021-Coliform,"Coliform Standards (PP No. 22/2021)","PP No. 22/2021","Kementerian Kesehatan RI",2021-03-15,,1.0,ACTIVE,"Indonesian coliform testing standards","COLIFORM_COUNT","0-50","CFU/100ml",,true,"Coliform Standards","Standar Koliform"
 *
 * Constitutional compliance:
 * - Uses @Component for Spring auto-discovery
 * - Implements DomainConfigurationHandler interface
 * - @Transactional for database operations
 * - Proper error handling and logging
 * - Supports internationalization with localization columns
 */
@Component
public class ComplianceStandardConfigurationHandler implements DomainConfigurationHandler {

    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @Override
    public String getDomainName() {
        return "compliance-standards";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 200; // Dependent entity - after base entities like sample types
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Compliance standards configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Map column headers to indices
        Map<String, Integer> columnIndices = createColumnMap(headers);
        Map<String, Integer> localizationColumns = detectLocalizationColumns(headers);

        List<ComplianceStandard> processedStandards = new ArrayList<>();
        String line;
        int lineNumber = 1; // Start at 1 since we already read the header

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines and comments (lines starting with #)
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                ComplianceStandard standard = processCsvLine(values, columnIndices, localizationColumns);
                if (standard != null) {
                    processedStandards.add(standard);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
                throw e; // Re-throw to prevent partial loading
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedStandards.size() + " compliance standards from " + fileName);
    }

    /**
     * Creates a map of column names to their indices for easy lookup
     */
    private Map<String, Integer> createColumnMap(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            columnMap.put(headers[i].trim().toLowerCase(), i);
        }
        return columnMap;
    }

    /**
     * Detects localization columns from headers
     */
    private Map<String, Integer> detectLocalizationColumns(String[] headers) {
        Map<String, Integer> localizationColumns = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase();
            if (header.startsWith(LOCALIZATION_COLUMN_PREFIX)) {
                String locale = header.substring(LOCALIZATION_COLUMN_PREFIX.length());
                if (!locale.isEmpty()) {
                    localizationColumns.put(locale, i);
                }
            }
        }
        return localizationColumns;
    }

    private String[] parseCsvLine(String line) {
        // Simple CSV parser that handles quoted fields
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());

        return values.toArray(new String[0]);
    }

    private void validateHeaders(String[] headers, String fileName) {
        boolean hasNameColumn = false;
        boolean hasRegulationNumberColumn = false;

        for (String header : headers) {
            if ("name".equalsIgnoreCase(header.trim())) {
                hasNameColumn = true;
            }
            if ("regulationNumber".equalsIgnoreCase(header.trim())) {
                hasRegulationNumberColumn = true;
            }
        }

        if (!hasNameColumn) {
            throw new IllegalArgumentException(
                    "Compliance standards configuration file " + fileName + " must have a 'name' column");
        }
        if (!hasRegulationNumberColumn) {
            throw new IllegalArgumentException(
                    "Compliance standards configuration file " + fileName + " must have a 'regulationNumber' column");
        }
    }

    private ComplianceStandard processCsvLine(String[] values, Map<String, Integer> columnIndices,
                                            Map<String, Integer> localizationColumns) {

        String name = getValueOrEmpty(values, columnIndices.get("name"));
        String regulationNumber = getValueOrEmpty(values, columnIndices.get("regulationnumber"));

        if (name.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing name");
            return null;
        }

        if (regulationNumber.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing regulationNumber");
            return null;
        }

        // Check if standard already exists by regulation number and name
        ComplianceStandard existing = complianceStandardService.getByRegulationNumberAndName(regulationNumber, name);

        if (existing != null) {
            // Update existing standard
            updateStandardFromCsv(existing, values, columnIndices, localizationColumns);
            complianceStandardService.update(existing);
            return existing;
        } else {
            // Create new standard
            ComplianceStandard newStandard = new ComplianceStandard();
            updateStandardFromCsv(newStandard, values, columnIndices, localizationColumns);
            String standardId = complianceStandardService.insert(newStandard);
            newStandard.setId(standardId);
            return newStandard;
        }
    }

    private void updateStandardFromCsv(ComplianceStandard standard, String[] values,
                                     Map<String, Integer> columnIndices,
                                     Map<String, Integer> localizationColumns) {

        // Set basic fields
        standard.setName(getValueOrEmpty(values, columnIndices.get("name")));
        standard.setDisplayName(getValueOrEmpty(values, columnIndices.get("displayname")));
        standard.setRegulationNumber(getValueOrEmpty(values, columnIndices.get("regulationnumber")));
        standard.setIssuingBody(getValueOrEmpty(values, columnIndices.get("issuingbody")));
        standard.setVersion(getValueOrEmpty(values, columnIndices.get("version")));
        standard.setDescription(getValueOrEmpty(values, columnIndices.get("description")));
        standard.setParameterTypes(getValueOrEmpty(values, columnIndices.get("parametertypes")));
        standard.setThresholds(getValueOrEmpty(values, columnIndices.get("thresholds")));
        standard.setUnits(getValueOrEmpty(values, columnIndices.get("units")));
        standard.setSupersededById(getValueOrEmpty(values, columnIndices.get("supersededbyid")));

        // Handle dates
        setDateField(standard, "effectiveDate", values, columnIndices.get("effectivedate"));
        setDateField(standard, "expirationDate", values, columnIndices.get("expirationdate"));

        // Handle status
        String statusStr = getValueOrEmpty(values, columnIndices.get("status"));
        if (!statusStr.isEmpty()) {
            try {
                standard.setStatus(ComplianceStandardStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "updateStandardFromCsv",
                        "Invalid status value: " + statusStr + ", defaulting to ACTIVE");
                standard.setStatus(ComplianceStandardStatus.ACTIVE);
            }
        } else {
            standard.setStatus(ComplianceStandardStatus.ACTIVE);
        }

        // Handle isPreSeeded boolean
        String preSeededStr = getValueOrEmpty(values, columnIndices.get("ispreseeded"));
        if ("true".equalsIgnoreCase(preSeededStr) || "1".equals(preSeededStr) || "yes".equalsIgnoreCase(preSeededStr)) {
            standard.setIsPreSeeded(true);
        } else {
            standard.setIsPreSeeded(false);
        }

        // Generate UUID if not set
        if (standard.getFhirUuid() == null) {
            standard.setFhirUuid(UUID.randomUUID());
        }

        // Set audit fields
        standard.setSysUserId("1"); // System user for configuration loading

        // Handle localization
        processLocalization(standard, values, localizationColumns);
    }

    private void setDateField(ComplianceStandard standard, String fieldName, String[] values, Integer columnIndex) {
        String dateStr = getValueOrEmpty(values, columnIndex);
        if (!dateStr.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                if ("effectiveDate".equals(fieldName)) {
                    standard.setEffectiveDate(date);
                } else if ("expirationDate".equals(fieldName)) {
                    standard.setExpirationDate(date);
                }
            } catch (DateTimeParseException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "setDateField",
                        "Invalid date format for " + fieldName + ": " + dateStr + ", expected yyyy-MM-dd");
            }
        }
    }

    private String getValueOrEmpty(String[] values, Integer index) {
        if (index != null && index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value.trim() : "";
        }
        return "";
    }

    /**
     * Processes localization columns and sets up translations for the compliance standard
     */
    private void processLocalization(ComplianceStandard standard, String[] values,
                                   Map<String, Integer> localizationColumns) {

        if (localizationColumns.isEmpty()) {
            return; // No localization to process
        }

        // Get or create localization
        Localization localization = standard.getLocalizedName();
        boolean isNewLocalization = false;

        if (localization == null) {
            localization = new Localization();
            localization.setDescription("compliance standard: " + standard.getName());
            localization.setSysUserId("1");
            isNewLocalization = true;
        }

        // Collect translations
        Map<String, String> translations = new HashMap<>();
        for (Map.Entry<String, Integer> entry : localizationColumns.entrySet()) {
            String locale = entry.getKey();
            String translationValue = getValueOrEmpty(values, entry.getValue());
            if (!translationValue.isEmpty()) {
                translations.put(locale, translationValue);
            }
        }

        if (translations.isEmpty()) {
            return; // No translations found
        }

        if (isNewLocalization) {
            // Set initial values for legacy support
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                if ("en".equals(entry.getKey())) {
                    localization.setEnglish(entry.getValue());
                } else if ("fr".equals(entry.getKey())) {
                    localization.setFrench(entry.getValue());
                }
            }

            // Insert the localization to get an ID
            String localizationId = localizationService.insert(localization);
            localization.setId(localizationId);
            standard.setLocalizedName(localization);

            // Set all translations using the service
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                localizationValueService.setTranslation(localizationId, entry.getKey(), entry.getValue(), "1");
            }
        } else {
            // Update existing localization translations
            String localizationId = localization.getId();
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                localizationValueService.setTranslation(localizationId, entry.getKey(), entry.getValue(), "1");
            }
        }
    }
}