package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ImportResult;
import org.openelisglobal.compliance.valueholder.ImportStatus;
import org.openelisglobal.compliance.valueholder.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TDD Test Suite for CSVImportService
 *
 * Tests CSV import functionality for compliance standards data. Follows
 * constitutional TDD requirements with security validation emphasis. Tests
 * import parsing, validation, error handling, and batch processing.
 */
public class CSVImportServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CSVImportService csvImportService;

    @Autowired
    private ComplianceStandardService complianceStandardService;

    private String validCSVContent;
    private String invalidCSVContent;
    private String maliciousCSVContent;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance_standards.xml");

        // Create test CSV content
        setupTestCSVData();
    }

    // ================================
    // RED PHASE: CSV Import Service Tests
    // ================================

    @Test
    public void testImportComplianceStandards_shouldImportValidCSV() {
        // RED: Will fail - import service doesn't exist
        InputStream csvStream = new ByteArrayInputStream(validCSVContent.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(csvStream, "test-user");

        assertNotNull("Import result should not be null", result);
        assertEquals("Import should succeed", ImportStatus.SUCCESS, result.getStatus());
        assertEquals("Should import 3 standards", 3, result.getImportedCount());
        assertEquals("Should have no errors", 0, result.getErrorCount());

        // Verify standards were actually saved
        List<ComplianceStandard> importedStandards = complianceStandardService.getAll();
        assertTrue("Standards should be imported", importedStandards.size() >= 3);

        // Verify FHIR UUIDs were generated (constitutional requirement)
        for (ComplianceStandard standard : importedStandards) {
            if (standard.getName().startsWith("Imported")) {
                assertNotNull("FHIR UUID should be generated", standard.getFhirUuid());
            }
        }
    }

    @Test
    public void testValidateCSVFormat_shouldDetectFormatErrors() {
        // RED: Will fail - validation doesn't exist
        InputStream invalidStream = new ByteArrayInputStream(invalidCSVContent.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(invalidStream, "test-user");

        assertEquals("Import should fail validation", ImportStatus.VALIDATION_FAILED, result.getStatus());
        assertEquals("Should import 0 standards", 0, result.getImportedCount());
        assertTrue("Should have validation errors", result.getErrorCount() > 0);

        // Verify error details
        List<ValidationError> errors = result.getValidationErrors();
        assertNotNull("Validation errors should not be null", errors);
        assertFalse("Should have specific error messages", errors.isEmpty());

        // Check for missing required fields error
        boolean hasMissingFieldError = errors.stream().anyMatch(error -> error.getMessage().contains("required field"));
        assertTrue("Should detect missing required fields", hasMissingFieldError);
    }

    @Test
    public void testSecurityValidation_shouldRejectMaliciousContent() {
        // RED: Will fail - security validation doesn't exist
        // Tests constitutional requirement: CSV import with security validation
        InputStream maliciousStream = new ByteArrayInputStream(maliciousCSVContent.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(maliciousStream, "test-user");

        assertEquals("Import should be rejected", ImportStatus.SECURITY_VIOLATION, result.getStatus());
        assertEquals("Should import 0 standards", 0, result.getImportedCount());

        // Verify security error is reported
        assertTrue("Should report security violation", result.getErrorCount() > 0);
        boolean hasSecurityError = result.getValidationErrors().stream()
                .anyMatch(error -> error.getMessage().contains("security"));
        assertTrue("Should have security-related error", hasSecurityError);
    }

    @Test
    public void testBatchProcessing_shouldHandleLargeFiles() {
        // RED: Will fail - batch processing doesn't exist
        String largeCsvContent = createLargeCSVContent(1000); // 1000 records
        InputStream largeStream = new ByteArrayInputStream(largeCsvContent.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(largeStream, "test-user");

        assertNotNull("Result should not be null for large file", result);

        if (result.getStatus() == ImportStatus.SUCCESS) {
            assertEquals("Should import all 1000 records", 1000, result.getImportedCount());
        }

        // Verify batch processing completed within reasonable time
        assertTrue("Batch processing should complete efficiently", result.getProcessingTimeMs() < 30000); // Less than
                                                                                                          // 30 seconds
    }

    @Test
    public void testDuplicateHandling_shouldDetectAndSkipDuplicates() {
        // RED: Will fail - duplicate detection doesn't exist
        String csvWithDuplicates = createCSVWithDuplicates();
        InputStream duplicateStream = new ByteArrayInputStream(csvWithDuplicates.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(duplicateStream, "test-user");

        assertEquals("Import should succeed with warnings", ImportStatus.SUCCESS_WITH_WARNINGS, result.getStatus());
        assertTrue("Should have some imported records", result.getImportedCount() > 0);
        assertTrue("Should have duplicate warnings", result.getWarningCount() > 0);

        // Verify duplicate warnings
        List<ValidationError> warnings = result.getWarnings();
        boolean hasDuplicateWarning = warnings.stream().anyMatch(warning -> warning.getMessage().contains("duplicate"));
        assertTrue("Should warn about duplicates", hasDuplicateWarning);
    }

    @Test
    public void testRollbackOnFailure_shouldNotPersistOnValidationFailure() {
        // RED: Will fail - transaction rollback doesn't exist
        // Tests constitutional requirement: proper transaction boundaries
        List<ComplianceStandard> beforeImport = complianceStandardService.getAll();
        int initialCount = beforeImport.size();

        InputStream mixedValidityStream = new ByteArrayInputStream(
                createMixedValidityCSV().getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(mixedValidityStream, "test-user");

        assertEquals("Import should fail validation", ImportStatus.VALIDATION_FAILED, result.getStatus());

        // Verify no partial import occurred (all-or-nothing)
        List<ComplianceStandard> afterImport = complianceStandardService.getAll();
        assertEquals("No standards should be imported on validation failure", initialCount, afterImport.size());
    }

    @Test
    public void testProgressTracking_shouldReportImportProgress() {
        // RED: Will fail - progress tracking doesn't exist
        String progressCsv = createLargeCSVContent(100);
        InputStream progressStream = new ByteArrayInputStream(progressCsv.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandardsWithProgress(progressStream, "test-user",
                progress -> {
                    // Verify progress callback is called
                    assertTrue("Progress should be between 0 and 100", progress >= 0 && progress <= 100);
                });

        assertNotNull("Result should include progress information", result);
        assertTrue("Should track processing time", result.getProcessingTimeMs() > 0);
    }

    @Test
    public void testColumnMapping_shouldHandleFlexibleCSVFormats() {
        // RED: Will fail - flexible column mapping doesn't exist
        String differentFormatCSV = createDifferentFormatCSV();
        InputStream formatStream = new ByteArrayInputStream(differentFormatCSV.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(formatStream, "test-user");

        assertEquals("Import should succeed with different column order", ImportStatus.SUCCESS, result.getStatus());
        assertTrue("Should import records despite column reordering", result.getImportedCount() > 0);
    }

    @Test
    public void testExportTemplate_shouldGenerateValidCSVTemplate() {
        // RED: Will fail - template export doesn't exist
        String csvTemplate = csvImportService.generateCSVTemplate();

        assertNotNull("Template should not be null", csvTemplate);
        assertTrue("Template should contain headers", csvTemplate.contains("Name"));
        assertTrue("Template should contain required fields", csvTemplate.contains("Issuing Body"));
        assertTrue("Template should be valid CSV format", csvTemplate.split("\n").length >= 1);

        // Verify template can be used for import (round-trip test)
        String sampleDataCSV = csvTemplate + "\n"
                + "\"Test Standard\",\"Test Authority\",\"TS-001\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\"";

        InputStream templateStream = new ByteArrayInputStream(sampleDataCSV.getBytes(StandardCharsets.UTF_8));
        ImportResult result = csvImportService.importComplianceStandards(templateStream, "test-user");

        assertEquals("Template-based import should succeed", ImportStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void testErrorRecovery_shouldContinueAfterNonFatalErrors() {
        // RED: Will fail - error recovery doesn't exist
        String csvWithErrors = createCSVWithRecoverableErrors();
        InputStream errorStream = new ByteArrayInputStream(csvWithErrors.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(errorStream, "test-user");

        assertEquals("Import should succeed with warnings", ImportStatus.SUCCESS_WITH_WARNINGS, result.getStatus());
        assertTrue("Should import valid records", result.getImportedCount() > 0);
        assertTrue("Should report errors for invalid records", result.getErrorCount() > 0);

        // Verify error details are preserved
        for (ValidationError error : result.getValidationErrors()) {
            assertNotNull("Error should have line number", error.getLineNumber());
            assertNotNull("Error should have field name", error.getFieldName());
            assertNotNull("Error should have message", error.getMessage());
        }
    }

    @Test
    public void testCharacterEncoding_shouldHandleInternationalCharacters() {
        // RED: Will fail - encoding handling doesn't exist
        String internationalCSV = createInternationalCSV();
        InputStream encodingStream = new ByteArrayInputStream(internationalCSV.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(encodingStream, "test-user");

        assertEquals("Import should handle international characters", ImportStatus.SUCCESS, result.getStatus());

        // Verify international characters were preserved
        List<ComplianceStandard> standards = complianceStandardService.getAll();
        boolean hasInternationalChars = standards.stream()
                .anyMatch(standard -> standard.getName().contains("中文") || standard.getName().contains("العربية"));
        assertTrue("Should preserve international characters", hasInternationalChars);
    }

    @Test
    public void testMaxFileSizeValidation_shouldRejectOversizedFiles() {
        // RED: Will fail - file size validation doesn't exist
        // Create a very large CSV content (simulate oversized file)
        String oversizedContent = createOversizedCSVContent();
        InputStream oversizedStream = new ByteArrayInputStream(oversizedContent.getBytes(StandardCharsets.UTF_8));

        ImportResult result = csvImportService.importComplianceStandards(oversizedStream, "test-user");

        assertEquals("Import should reject oversized files", ImportStatus.FILE_TOO_LARGE, result.getStatus());
        assertEquals("Should not import any records", 0, result.getImportedCount());

        // Verify appropriate error message
        assertTrue("Should have file size error", result.getErrorCount() > 0);
        boolean hasFileSizeError = result.getValidationErrors().stream()
                .anyMatch(error -> error.getMessage().contains("file size"));
        assertTrue("Should report file size violation", hasFileSizeError);
    }

    // ================================
    // Helper Methods
    // ================================

    private void setupTestCSVData() {
        validCSVContent = "Name,Issuing Body,Regulation Number,Version,Country Region,Effective Date,Applicable Sample Types,Status\n"
                + "\"Imported Standard 1\",\"Indonesian Government\",\"IS-001\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\"\n"
                + "\"Imported Standard 2\",\"WHO\",\"WHO-002\",\"2.0\",\"Global\",\"2024-02-01\",\"Water,Soil\",\"ACTIVE\"\n"
                + "\"Imported Standard 3\",\"EPA\",\"EPA-003\",\"1.5\",\"United States\",\"2024-03-01\",\"Air\",\"DRAFT\"";

        invalidCSVContent = "Name,Issuing Body,Regulation Number\n" + "\"Invalid Standard\",\"\",\"\""; // Missing
                                                                                                        // required
                                                                                                        // fields

        maliciousCSVContent = "Name,Issuing Body,Regulation Number,Version,Country Region,Effective Date,Applicable Sample Types,Status\n"
                + "\"<script>alert('xss')</script>\",\"../../../etc/passwd\",\"'; DROP TABLE compliance_standard; --\",\"1.0\",\"<img src=x>\",\"2024-01-01\",\"Water\",\"ACTIVE\"";
    }

    private String createLargeCSVContent(int recordCount) {
        StringBuilder csv = new StringBuilder();
        csv.append(
                "Name,Issuing Body,Regulation Number,Version,Country Region,Effective Date,Applicable Sample Types,Status\n");

        for (int i = 1; i <= recordCount; i++) {
            csv.append(String.format(
                    "\"Large Standard %d\",\"Authority %d\",\"LS-%03d\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\"\n",
                    i, i, i));
        }

        return csv.toString();
    }

    private String createCSVWithDuplicates() {
        return "Name,Issuing Body,Regulation Number,Version,Country Region,Effective Date,Applicable Sample Types,Status\n"
                + "\"Duplicate Standard\",\"Authority\",\"DUP-001\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\"\n"
                + "\"Unique Standard\",\"Authority\",\"UNI-001\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\"\n"
                + "\"Duplicate Standard\",\"Authority\",\"DUP-001\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\""; // Duplicate
    }

    private String createMixedValidityCSV() {
        return "Name,Issuing Body,Regulation Number,Version,Country Region,Effective Date,Applicable Sample Types,Status\n"
                + "\"Valid Standard\",\"Authority\",\"VAL-001\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\"\n"
                + "\"Invalid Standard\",\"\",\"\",\"\",\"\",\"invalid-date\",\"Water\",\"INVALID_STATUS\""; // Invalid
                                                                                                            // row
    }

    private String createDifferentFormatCSV() {
        return "Regulation Number,Name,Status,Issuing Body,Version,Country Region,Effective Date,Applicable Sample Types\n"
                + "\"DF-001\",\"Different Format Standard\",\"ACTIVE\",\"Authority\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\"";
    }

    private String createCSVWithRecoverableErrors() {
        return "Name,Issuing Body,Regulation Number,Version,Country Region,Effective Date,Applicable Sample Types,Status\n"
                + "\"Valid Standard 1\",\"Authority\",\"VS1-001\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\"\n"
                + "\"Invalid Standard\",\"\",\"INVALID\",\"1.0\",\"Indonesia\",\"invalid-date\",\"Water\",\"ACTIVE\"\n"
                + "\"Valid Standard 2\",\"Authority\",\"VS2-001\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\"";
    }

    private String createInternationalCSV() {
        return "Name,Issuing Body,Regulation Number,Version,Country Region,Effective Date,Applicable Sample Types,Status\n"
                + "\"中文标准\",\"中国政府\",\"CN-001\",\"1.0\",\"China\",\"2024-01-01\",\"Water\",\"ACTIVE\"\n"
                + "\"المعيار العربي\",\"الحكومة العربية\",\"AR-001\",\"1.0\",\"UAE\",\"2024-01-01\",\"Water\",\"ACTIVE\"";
    }

    private String createOversizedCSVContent() {
        // Simulate a 10MB+ file by creating many large fields
        StringBuilder oversized = new StringBuilder();
        oversized.append(
                "Name,Issuing Body,Regulation Number,Version,Country Region,Effective Date,Applicable Sample Types,Status\n");

        String largeField = "X".repeat(10000); // 10KB field
        for (int i = 0; i < 1000; i++) {
            oversized.append(String.format(
                    "\"%s\",\"Authority\",\"OS-%03d\",\"1.0\",\"Indonesia\",\"2024-01-01\",\"Water\",\"ACTIVE\"\n",
                    largeField, i));
        }

        return oversized.toString();
    }
}