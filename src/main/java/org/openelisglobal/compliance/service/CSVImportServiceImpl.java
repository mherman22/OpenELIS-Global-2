package org.openelisglobal.compliance.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandardStatus;
import org.openelisglobal.compliance.valueholder.ImportResult;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.openelisglobal.compliance.valueholder.ComplianceThresholdCriticality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of CSVImportService for secure compliance standards CSV import.
 *
 * Follows OpenELIS security patterns with comprehensive validation, error handling,
 * and progress tracking for bulk compliance standards import operations.
 *
 * Constitutional compliance:
 * - @Transactional boundaries at service level
 * - Comprehensive input validation and sanitization
 * - Business logic validation methods
 * - Security validation patterns from existing OpenELIS CSV imports
 */
@Service
public class CSVImportServiceImpl implements CSVImportService {

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ParameterGroupService parameterGroupService;

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    // Security constants
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "text/csv",
        "application/csv",
        "text/plain"
    );
    private static final Pattern DANGEROUS_CONTENT_PATTERN =
        Pattern.compile("(@|\\+|\\-|=|\\|).*", Pattern.CASE_INSENSITIVE);

    // CSV headers
    private static final String[] REQUIRED_HEADERS = {
        "name", "version", "issuing_body", "regulation_number", "country_region", "status"
    };
    private static final String[] OPTIONAL_HEADERS = {
        "description", "effective_date", "expiry_date", "enforcement_authority",
        "sample_types", "parameter_groups"
    };

    // Thread safety for import operations
    private final AtomicBoolean importInProgress = new AtomicBoolean(false);
    private final AtomicInteger importProgress = new AtomicInteger(0);
    private volatile boolean cancelRequested = false;

    // Import statistics
    private final AtomicInteger totalImports = new AtomicInteger(0);
    private final AtomicInteger successfulImports = new AtomicInteger(0);
    private final AtomicInteger failedImports = new AtomicInteger(0);
    private final AtomicInteger pendingImports = new AtomicInteger(0);

    @Override
    @Transactional
    public ImportResult importComplianceStandards(InputStream csvStream, String userId) {
        return importComplianceStandardsWithProgress(csvStream, userId, null);
    }

    @Override
    @Transactional
    public ImportResult importComplianceStandardsWithProgress(InputStream csvStream, String userId,
                                                            Consumer<Integer> progressCallback) {

        if (!importInProgress.compareAndSet(false, true)) {
            ImportResult result = new ImportResult();
            result.setStatus(ImportStatus.VALIDATION_FAILED);
            result.addValidationError(new ValidationError("Another import operation is already in progress"));
            result.markAsCompleted();
            return result;
        }

        ImportResult result = new ImportResult();
        result.setImportedBy(userId);
        result.setProgressCallback(progressCallback);

        try {
            cancelRequested = false;
            importProgress.set(0);

            // Validate input parameters
            if (csvStream == null) {
                result.setStatus(ImportStatus.VALIDATION_FAILED);
                result.addValidationError(ValidationError.securityError("CSV stream cannot be null"));
                result.markAsCompleted();
                return result;
            }
            if (GenericValidator.isBlankOrNull(userId)) {
                result.setStatus(ImportStatus.VALIDATION_FAILED);
                result.addValidationError(ValidationError.securityError("User ID is required"));
                result.markAsCompleted();
                return result;
            }

            // Parse CSV with security validation
            List<CSVRecord> records;
            try {
                records = parseCSVSafely(csvStream);
            } catch (Exception e) {
                LogEvent.logError("CSVImportService", "importComplianceStandards",
                                "CSV parsing failed: " + e.getMessage());
                result.setStatus(ImportStatus.INVALID_FORMAT);
                result.addValidationError(ValidationError.securityError("Failed to parse CSV file: " + e.getMessage()));
                result.markAsCompleted();
                return result;
            }

            if (records.isEmpty()) {
                result.setStatus(ImportStatus.SUCCESS_WITH_WARNINGS);
                result.addWarning(ValidationError.warning(null, "No data records found in CSV file"));
                result.markAsCompleted();
                return result;
            }

            totalImports.set(records.size());
            pendingImports.set(records.size());
            result.setTotalRecords(records.size());

            int processedCount = 0;

            for (CSVRecord record : records) {
                if (cancelRequested) {
                    result.setStatus(ImportStatus.CANCELLED);
                    result.updateProgress(100, "Import cancelled by user");
                    result.markAsCompleted();
                    return result;
                }

                try {
                    String importedId = processRecord(record, userId, result);
                    if (importedId != null) {
                        result.addImportedId(importedId);
                        successfulImports.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedImports.incrementAndGet();
                    result.addValidationError(
                        ValidationError.fieldError((int) record.getRecordNumber(), null, null, e.getMessage()));
                    LogEvent.logWarn("CSVImportService", "importComplianceStandards",
                                   "Failed to process record " + record.getRecordNumber() + ": " + e.getMessage());
                }

                processedCount++;
                pendingImports.decrementAndGet();
                int progress = (processedCount * 100) / records.size();
                importProgress.set(progress);
                result.updateProgress(progress, "Processing record " + processedCount + " of " + records.size());
            }

            result.markAsCompleted();
            return result;

        } finally {
            importInProgress.set(false);
            importProgress.set(100);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ImportResult validateCSVFile(InputStream csvStream) {
        ImportResult result = new ImportResult();

        try {
            // Parse CSV for validation only
            List<CSVRecord> records = parseCSVSafely(csvStream);

            if (records.isEmpty()) {
                result.setStatus(ImportStatus.VALIDATION_FAILED);
                result.addValidationError(ValidationError.securityError("No data records found in CSV file"));
                result.markAsCompleted();
                return result;
            }

            result.setTotalRecords(records.size());

            // Validate each record without saving
            for (CSVRecord record : records) {
                try {
                    validateRecord(record, result);
                } catch (Exception e) {
                    result.addValidationError(
                        ValidationError.fieldError((int) record.getRecordNumber(), null, null, e.getMessage()));
                }
            }

            // Set final status based on validation results
            if (result.getErrorCount() == 0 && result.getWarningCount() == 0) {
                result.setStatus(ImportStatus.SUCCESS);
                result.setSummary("CSV validation passed. " + records.size() + " records ready for import.");
            } else if (result.getErrorCount() == 0) {
                result.setStatus(ImportStatus.SUCCESS_WITH_WARNINGS);
                result.setSummary("CSV validation passed with " + result.getWarningCount() + " warnings.");
            } else {
                result.setStatus(ImportStatus.VALIDATION_FAILED);
                result.setSummary("CSV validation failed with " + result.getErrorCount() + " errors.");
            }

            result.markAsCompleted();
            return result;

        } catch (Exception e) {
            LogEvent.logError("CSVImportService", "validateCSVFile",
                            "CSV validation failed: " + e.getMessage());
            result.setStatus(ImportStatus.INVALID_FORMAT);
            result.addValidationError(ValidationError.securityError("CSV validation failed: " + e.getMessage()));
            result.markAsCompleted();
            return result;
        }
    }

    @Override
    public String generateCSVTemplate() {
        StringBuilder template = new StringBuilder();

        // Headers
        template.append("name,version,issuing_body,regulation_number,country_region,status,")
                .append("description,effective_date,expiry_date,enforcement_authority,sample_types,parameter_groups\n");

        // Example data
        template.append("\"Indonesian Water Quality Standard PP-22-2021\",")
                .append("\"2021\",")
                .append("\"Indonesia Ministry of Environment\",")
                .append("\"PP-22-2021\",")
                .append("\"Indonesia\",")
                .append("\"ACTIVE\",")
                .append("\"Water quality standards for environmental protection\",")
                .append("\"2021-04-01\",")
                .append("\"2031-04-01\",")
                .append("\"Ministry of Environment and Forestry\",")
                .append("\"Water;Wastewater\",")
                .append("\"Physical Parameters;Chemical Parameters\"\n");

        template.append("\"WHO Drinking Water Guidelines\",")
                .append("\"2022\",")
                .append("\"World Health Organization\",")
                .append("\"WHO-DW-2022\",")
                .append("\"Global\",")
                .append("\"ACTIVE\",")
                .append("\"WHO guidelines for drinking water quality\",")
                .append("\"2022-01-01\",")
                .append("\"\",")
                .append("\"World Health Organization\",")
                .append("\"Drinking Water\",")
                .append("\"Microbiological Parameters;Chemical Parameters\"\n");

        return template.toString();
    }

    @Override
    public List<String> getSupportedHeaders() {
        List<String> headers = new ArrayList<>();
        headers.addAll(Arrays.asList(REQUIRED_HEADERS));
        headers.addAll(Arrays.asList(OPTIONAL_HEADERS));
        return headers;
    }

    @Override
    public boolean isImportInProgress() {
        return importInProgress.get();
    }

    @Override
    public boolean cancelImport() {
        if (importInProgress.get()) {
            cancelRequested = true;
            return true;
        }
        return false;
    }

    @Override
    public int[] getImportStatistics() {
        return new int[]{
            totalImports.get(),
            successfulImports.get(),
            failedImports.get(),
            pendingImports.get()
        };
    }

    @Override
    public void validateFileProperties(long fileSize, String contentType, String filename) {
        // File size validation
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " +
                                             (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        if (fileSize <= 0) {
            throw new IllegalArgumentException("File size must be greater than 0");
        }

        // Content type validation
        if (GenericValidator.isBlankOrNull(contentType) ||
            !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only CSV files are allowed.");
        }

        // Filename validation
        if (GenericValidator.isBlankOrNull(filename)) {
            throw new IllegalArgumentException("Filename is required");
        }

        if (!filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("File must have .csv extension");
        }

        // Path traversal protection
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename. Path traversal detected.");
        }
    }

    @Override
    public List<String> validateSecurity(String csvContent) {
        List<String> violations = new ArrayList<>();

        if (GenericValidator.isBlankOrNull(csvContent)) {
            violations.add("CSV content is empty");
            return violations;
        }

        String[] lines = csvContent.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Check for CSV injection patterns
            String[] fields = line.split(",");
            for (int j = 0; j < fields.length; j++) {
                String field = fields[j].trim().replaceAll("^\"|\"$", ""); // Remove quotes

                if (DANGEROUS_CONTENT_PATTERN.matcher(field).matches()) {
                    violations.add("Line " + (i + 1) + ", Column " + (j + 1) +
                                 ": Potential CSV injection detected - '" +
                                 StringUtils.abbreviate(field, 50) + "'");
                }

                // Check for suspicious patterns
                if (field.contains("<script>") || field.contains("javascript:") ||
                    field.contains("data:") || field.contains("vbscript:")) {
                    violations.add("Line " + (i + 1) + ", Column " + (j + 1) +
                                 ": Suspicious script content detected");
                }
            }
        }

        return violations;
    }

    /**
     * Parse CSV with security validation
     */
    private List<CSVRecord> parseCSVSafely(InputStream csvStream) throws IOException {
        // Security validation on content
        InputStreamReader reader = new InputStreamReader(csvStream);
        StringBuilder contentBuilder = new StringBuilder();
        char[] buffer = new char[1024];
        int bytesRead;

        while ((bytesRead = reader.read(buffer)) != -1) {
            contentBuilder.append(buffer, 0, bytesRead);
        }

        String csvContent = contentBuilder.toString();

        // Security validation
        List<String> securityViolations = validateSecurity(csvContent);
        if (!securityViolations.isEmpty()) {
            throw new SecurityException("Security violations found: " +
                                      String.join(", ", securityViolations));
        }

        // Parse CSV
        CSVFormat format = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withIgnoreEmptyLines(true)
            .withTrim(true);

        try (CSVParser parser = format.parse(new StringReader(csvContent))) {

            // Validate headers
            Set<String> headers = parser.getHeaderMap().keySet();
            List<String> missingRequired = new ArrayList<>();

            for (String required : REQUIRED_HEADERS) {
                if (!headers.contains(required)) {
                    missingRequired.add(required);
                }
            }

            if (!missingRequired.isEmpty()) {
                throw new IllegalArgumentException("Missing required headers: " +
                                                 String.join(", ", missingRequired));
            }

            return parser.getRecords();
        }
    }

    /**
     * Process individual CSV record
     * @return Standard ID if successful, null if skipped
     */
    private String processRecord(CSVRecord record, String userId, ImportResult result) throws Exception {
        int rowNum = (int) record.getRecordNumber();

        // First validate the record
        if (!validateRecord(record, result)) {
            return null; // Validation failed, skip record
        }

        // Create and populate ComplianceStandard
        ComplianceStandard standard = new ComplianceStandard();

        // Required fields
        standard.setName(record.get("name").trim());
        standard.setVersion(record.get("version").trim());
        standard.setIssuingBody(record.get("issuing_body").trim());
        standard.setRegulationNumber(record.get("regulation_number").trim());
        standard.setCountryRegion(record.get("country_region").trim());
        standard.setStatus(ComplianceStandardStatus.valueOf(record.get("status").trim()));

        // Optional fields
        if (record.isSet("description") && StringUtils.isNotBlank(record.get("description"))) {
            standard.setDescription(record.get("description").trim());
        }

        if (record.isSet("effective_date") && StringUtils.isNotBlank(record.get("effective_date"))) {
            try {
                standard.setEffectiveDate(LocalDate.parse(record.get("effective_date").trim(),
                                                        DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (DateTimeParseException e) {
                result.addWarning(ValidationError.warning(rowNum, "Invalid effective_date format, skipped"));
            }
        }

        if (record.isSet("expiry_date") && StringUtils.isNotBlank(record.get("expiry_date"))) {
            try {
                standard.setExpiryDate(LocalDate.parse(record.get("expiry_date").trim(),
                                                     DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (DateTimeParseException e) {
                result.addWarning(ValidationError.warning(rowNum, "Invalid expiry_date format, skipped"));
            }
        }

        if (record.isSet("enforcement_authority") && StringUtils.isNotBlank(record.get("enforcement_authority"))) {
            standard.setEnforcementAuthority(record.get("enforcement_authority").trim());
        }

        // Sample types (semicolon separated)
        if (record.isSet("sample_types") && StringUtils.isNotBlank(record.get("sample_types"))) {
            String[] sampleTypes = record.get("sample_types").split(";");
            Set<String> sampleTypeSet = new HashSet<>();
            for (String sampleType : sampleTypes) {
                String trimmed = sampleType.trim();
                if (StringUtils.isNotBlank(trimmed)) {
                    sampleTypeSet.add(trimmed);
                }
            }
            standard.setSampleTypes(sampleTypeSet);
        }

        // Set audit fields
        standard.setSysUserId(userId);
        standard.setLastupdated(new java.sql.Timestamp(System.currentTimeMillis()));

        // Check for duplicates
        if (complianceStandardService.duplicateStandardExists(standard)) {
            result.addWarning(ValidationError.warning(rowNum, "Duplicate standard exists, skipping"));
            return null;
        }

        // Save standard
        String standardId = complianceStandardService.insert(standard);

        // Process parameter groups if provided
        if (record.isSet("parameter_groups") && StringUtils.isNotBlank(record.get("parameter_groups"))) {
            processParameterGroups(standardId, record.get("parameter_groups"), userId, result);
        }

        return standardId;
    }

    /**
     * Validate individual record
     * @return true if record is valid (may have warnings), false if has errors
     */
    private boolean validateRecord(CSVRecord record, ImportResult result) {
        int rowNum = (int) record.getRecordNumber();
        boolean hasErrors = false;

        // Validate required fields
        for (String required : REQUIRED_HEADERS) {
            if (!record.isSet(required) || GenericValidator.isBlankOrNull(record.get(required))) {
                result.addValidationError(
                    ValidationError.requiredFieldError(rowNum, required));
                hasErrors = true;
            }
        }

        // Validate status enum
        if (record.isSet("status") && StringUtils.isNotBlank(record.get("status"))) {
            try {
                ComplianceStandardStatus.valueOf(record.get("status").trim());
            } catch (IllegalArgumentException e) {
                result.addValidationError(
                    ValidationError.dataTypeError(rowNum, "status", record.get("status"),
                        "Valid values: " + Arrays.toString(ComplianceStandardStatus.values())));
                hasErrors = true;
            }
        }

        // Validate date formats
        if (record.isSet("effective_date") && StringUtils.isNotBlank(record.get("effective_date"))) {
            try {
                LocalDate.parse(record.get("effective_date").trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                result.addValidationError(
                    ValidationError.dataTypeError(rowNum, "effective_date", record.get("effective_date"),
                        "Date format YYYY-MM-DD"));
                hasErrors = true;
            }
        }

        if (record.isSet("expiry_date") && StringUtils.isNotBlank(record.get("expiry_date"))) {
            try {
                LocalDate.parse(record.get("expiry_date").trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                result.addValidationError(
                    ValidationError.dataTypeError(rowNum, "expiry_date", record.get("expiry_date"),
                        "Date format YYYY-MM-DD"));
                hasErrors = true;
            }
        }

        // Validate field lengths
        if (record.isSet("name") && record.get("name").length() > 255) {
            result.addValidationError(
                ValidationError.fieldError(rowNum, "name", record.get("name"),
                    "Name exceeds maximum length of 255 characters"));
            hasErrors = true;
        }

        if (record.isSet("regulation_number") && record.get("regulation_number").length() > 100) {
            result.addValidationError(
                ValidationError.fieldError(rowNum, "regulation_number", record.get("regulation_number"),
                    "Regulation number exceeds maximum length of 100 characters"));
            hasErrors = true;
        }

        return !hasErrors;
    }

    /**
     * Process parameter groups from CSV
     */
    private void processParameterGroups(String standardId, String parameterGroupsText, String userId,
                                      ImportResult result) {
        try {
            String[] groupNames = parameterGroupsText.split(";");

            for (String groupName : groupNames) {
                String trimmed = groupName.trim();
                if (StringUtils.isNotBlank(trimmed)) {

                    ParameterGroup group = new ParameterGroup();
                    group.setComplianceStandardId(standardId);
                    group.setGroupName(trimmed);
                    group.setDisplayOrder(1); // Default order
                    group.setSysUserId(userId);
                    group.setLastupdated(new java.sql.Timestamp(System.currentTimeMillis()));

                    parameterGroupService.insert(group);
                }
            }
        } catch (Exception e) {
            result.addWarning(ValidationError.warning(null,
                "Failed to process parameter groups for standard " + standardId + ": " + e.getMessage()));
            LogEvent.logWarn("CSVImportService", "processParameterGroups",
                           "Failed to process parameter groups: " + e.getMessage());
        }
    }
}