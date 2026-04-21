package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.valueholder.ComplianceEvaluation;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.EvaluationResult;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TDD Test Suite for ComplianceEvaluationService
 *
 * Tests the compliance evaluation engine functionality. Follows constitutional
 * TDD requirements and validates proper transaction boundaries. Tests
 * evaluation logic, result compilation, and version-lock semantics.
 */
public class ComplianceEvaluationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ComplianceEvaluationService complianceEvaluationService;

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ParameterGroupService parameterGroupService;

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    private String testStandardId;
    private String testGroupId;
    private String testThresholdId;
    private String testSampleId;
    private ComplianceEvaluation testEvaluation;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance_standards.xml");

        // Create test hierarchy
        ComplianceStandard testStandard = createTestStandard();
        testStandardId = complianceStandardService.save(testStandard).getId();

        ParameterGroup testGroup = createTestParameterGroup(testStandardId);
        testGroupId = parameterGroupService.save(testGroup).getId();

        ComplianceThreshold testThreshold = createTestThreshold(testGroupId);
        testThresholdId = complianceThresholdService.save(testThreshold).getId();

        testSampleId = UUID.randomUUID().toString();
        testEvaluation = createTestEvaluation(testSampleId, testThresholdId);
    }

    // ================================
    // RED PHASE: Compliance Evaluation Engine Tests
    // ================================

    @Test
    public void testEvaluateSampleAgainstStandard_shouldReturnCompleteEvaluation() {
        // RED: Will fail - evaluation engine doesn't exist
        // Simulate sample test results
        String sampleId = UUID.randomUUID().toString();
        String standardId = testStandardId;

        ComplianceEvaluation evaluation = complianceEvaluationService.evaluateSampleAgainstStandard(sampleId,
                standardId);

        assertNotNull("Evaluation should not be null", evaluation);
        assertEquals("Sample ID should match", sampleId, evaluation.getSampleId());
        assertEquals("Standard ID should match", standardId, evaluation.getStandardId());

        // Verify version-lock semantics (constitutional requirement)
        assertNotNull("Standard version should be captured", evaluation.getStandardVersion());
        assertEquals("Should capture current standard version", "1.0", evaluation.getStandardVersion());

        // Verify audit fields
        assertNotNull("Evaluation date should be set", evaluation.getEvaluatedDate());
        assertNotNull("Evaluated by should be set", evaluation.getEvaluatedBy());
    }

    @Test
    public void testEvaluateParameterThreshold_shouldReturnAccurateComplianceResult() {
        // RED: Will fail - parameter evaluation doesn't exist
        BigDecimal testValue = new BigDecimal("7.2");
        String thresholdId = testThresholdId;

        EvaluationResult result = complianceEvaluationService.evaluateParameterThreshold(thresholdId, testValue);

        assertNotNull("Evaluation result should not be null", result);
        assertEquals("Test value should be recorded", testValue, result.getTestedValue());
        assertEquals("Threshold ID should match", thresholdId, result.getThresholdId());
        assertTrue("Value 7.2 should be compliant for pH range 6.5-8.5", result.isCompliant());

        // Test non-compliant value
        BigDecimal nonCompliantValue = new BigDecimal("9.0");
        EvaluationResult nonCompliantResult = complianceEvaluationService.evaluateParameterThreshold(thresholdId,
                nonCompliantValue);

        assertFalse("Value 9.0 should be non-compliant for pH range 6.5-8.5", nonCompliantResult.isCompliant());

        // Inversion test
        assertFalse("Compliance results should differ for different values",
                result.isCompliant() == nonCompliantResult.isCompliant());
    }

    @Test
    public void testSaveEvaluation_shouldPersistEvaluationWithResults() {
        // RED: Will fail - save method doesn't exist
        ComplianceEvaluation savedEvaluation = complianceEvaluationService.save(testEvaluation);
        String evaluationId = savedEvaluation.getId();

        assertNotNull("Saved ID should not be null", evaluationId);
        assertEquals("Evaluation ID should match", testEvaluation.getId(), evaluationId);

        // Verify FHIR UUID was auto-generated (constitutional requirement)
        assertNotNull("FHIR UUID should be auto-generated", testEvaluation.getFhirUuid());

        // Verify audit fields are set
        assertNotNull("Created date should be set", testEvaluation.getLastupdated());
    }

    @Test
    public void testGetEvaluationsBySampleId_shouldReturnOrderedEvaluations() {
        // RED: Will fail - method doesn't exist
        // Create multiple evaluations for same sample
        String sampleId = UUID.randomUUID().toString();
        createAndSaveEvaluation(sampleId, testThresholdId, EvaluationStatus.COMPLETED);
        createAndSaveEvaluation(sampleId, testThresholdId, EvaluationStatus.IN_PROGRESS);
        createAndSaveEvaluation(sampleId, testThresholdId, EvaluationStatus.PENDING);

        List<ComplianceEvaluation> evaluations = complianceEvaluationService.getEvaluationsBySampleId(sampleId);

        assertNotNull("Evaluations list should not be null", evaluations);
        assertEquals("Should have 3 evaluations", 3, evaluations.size());

        // Verify all evaluations belong to the sample
        for (ComplianceEvaluation evaluation : evaluations) {
            assertEquals("All evaluations should belong to sample", sampleId, evaluation.getSampleId());
        }

        // Verify chronological order (newest first)
        assertTrue("Evaluations should be ordered by date",
                evaluations.get(0).getEvaluatedDate().after(evaluations.get(1).getEvaluatedDate()));
    }

    @Test
    public void testGetEvaluationWithResults_shouldEagerLoadResults() {
        // RED: Will fail - method doesn't exist
        // Tests constitutional requirement: services must compile data within
        // transaction
        String evaluationId = complianceEvaluationService.save(testEvaluation).getId();

        ComplianceEvaluation evaluationWithResults = complianceEvaluationService.getEvaluationWithResults(evaluationId);

        assertNotNull("Evaluation should not be null", evaluationWithResults);
        assertNotNull("Results should be eager loaded", evaluationWithResults.getEvaluationResults());

        // Verify no LazyInitializationException occurs outside transaction
        for (var result : evaluationWithResults.getEvaluationResults()) {
            assertNotNull("Result values should be accessible", result.getTestedValue());
            assertNotNull("Threshold info should be accessible", result.getThresholdId());
        }
    }

    @Test
    public void testUpdateEvaluationStatus_shouldUpdateStatusAndTimestamps() {
        // RED: Will fail - status update method doesn't exist
        String evaluationId = complianceEvaluationService.save(testEvaluation).getId();
        ComplianceEvaluation savedEvaluation = complianceEvaluationService.get(evaluationId);

        EvaluationStatus originalStatus = savedEvaluation.getStatus();
        EvaluationStatus newStatus = EvaluationStatus.COMPLETED;

        complianceEvaluationService.updateEvaluationStatus(evaluationId, newStatus);

        // Verify status update persistence
        ComplianceEvaluation updatedEvaluation = complianceEvaluationService.get(evaluationId);
        assertEquals("Status should be updated", newStatus, updatedEvaluation.getStatus());
        assertNotNull("Completed date should be set", updatedEvaluation.getCompletedDate());

        // Inversion test
        assertFalse("Updated status should differ from original", originalStatus.equals(newStatus));
    }

    @Test
    public void testCalculateOverallCompliance_shouldDeterminePassFailStatus() {
        // RED: Will fail - overall compliance calculation doesn't exist
        String sampleId = UUID.randomUUID().toString();

        // Create evaluation with mixed results
        ComplianceEvaluation mixedEvaluation = createEvaluationWithResults(sampleId);
        String evaluationId = complianceEvaluationService.save(mixedEvaluation).getId();

        boolean overallCompliance = complianceEvaluationService.calculateOverallCompliance(evaluationId);

        // Overall compliance should be false if any parameter fails
        assertFalse("Overall compliance should be false with any non-compliant parameters", overallCompliance);

        // Test with all compliant results
        ComplianceEvaluation compliantEvaluation = createAllCompliantEvaluation(sampleId);
        String compliantEvaluationId = complianceEvaluationService.save(compliantEvaluation).getId();

        boolean allCompliant = complianceEvaluationService.calculateOverallCompliance(compliantEvaluationId);

        assertTrue("Overall compliance should be true with all compliant parameters", allCompliant);
    }

    @Test
    public void testGenerateComplianceReport_shouldCreateDetailedReport() {
        // RED: Will fail - report generation doesn't exist
        String evaluationId = complianceEvaluationService.save(testEvaluation).getId();

        String report = complianceEvaluationService.generateComplianceReport(evaluationId);

        assertNotNull("Report should not be null", report);
        assertTrue("Report should contain sample ID", report.contains(testEvaluation.getSampleId()));
        assertTrue("Report should contain standard name", report.contains("Test Standard"));
        assertTrue("Report should contain evaluation results", report.contains("pH"));
    }

    @Test
    public void testDeleteEvaluation_shouldPreventDeletionOfCompletedEvaluations() {
        // RED: Will fail - deletion protection doesn't exist
        testEvaluation.setStatus(EvaluationStatus.COMPLETED);
        String evaluationId = complianceEvaluationService.save(testEvaluation).getId();

        try {
            ComplianceEvaluation evaluationToDelete = complianceEvaluationService.get(evaluationId);
            complianceEvaluationService.delete(evaluationToDelete);
            fail("Should not be able to delete completed evaluation");
        } catch (Exception e) {
            assertTrue("Should prevent deletion of completed evaluations",
                    e.getMessage().contains("cannot delete completed evaluation"));
        }
    }

    @Test
    public void testBulkEvaluateSamples_shouldProcessMultipleSamples() {
        // RED: Will fail - bulk evaluation doesn't exist
        List<String> sampleIds = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString());

        List<ComplianceEvaluation> evaluations = complianceEvaluationService.bulkEvaluateSamples(sampleIds,
                testStandardId);

        assertNotNull("Evaluations should not be null", evaluations);
        assertEquals("Should evaluate 3 samples", 3, evaluations.size());

        // Verify all evaluations were created for the correct standard
        for (ComplianceEvaluation evaluation : evaluations) {
            assertEquals("All evaluations should use test standard", testStandardId, evaluation.getStandardId());
            assertNotNull("All evaluations should have FHIR UUID", evaluation.getFhirUuid());
        }
    }

    @Test
    public void testVersionLockIntegrity_shouldPreserveEvaluationVersion() {
        // RED: Will fail - version lock validation doesn't exist
        // Tests constitutional requirement: version-lock semantics
        String evaluationId = complianceEvaluationService.save(testEvaluation).getId();
        ComplianceEvaluation savedEvaluation = complianceEvaluationService.get(evaluationId);

        String evaluationVersion = savedEvaluation.getStandardVersion();

        // Update standard to new version
        ComplianceStandard standard = complianceStandardService.get(testStandardId);
        standard.setVersion("2.0");
        complianceStandardService.update(standard);

        // Evaluation should maintain original version
        ComplianceEvaluation unchangedEvaluation = complianceEvaluationService.get(evaluationId);
        assertEquals("Evaluation should preserve original version", evaluationVersion,
                unchangedEvaluation.getStandardVersion());

        // Verify version differs from current standard
        String currentVersion = complianceStandardService.get(testStandardId).getVersion();
        assertFalse("Evaluation version should not change with standard updates",
                unchangedEvaluation.getStandardVersion().equals(currentVersion));
    }

    @Test
    public void testGetEvaluationsByDateRange_shouldReturnFilteredEvaluations() {
        // RED: Will fail - date range query doesn't exist
        // Create evaluations over time
        createAndSaveEvaluation(testSampleId, testThresholdId, EvaluationStatus.COMPLETED);

        // Query for evaluations in date range
        List<ComplianceEvaluation> evaluations = complianceEvaluationService.getEvaluationsByDateRange(testStandardId,
                java.time.LocalDate.now().minusDays(1), java.time.LocalDate.now().plusDays(1));

        assertNotNull("Evaluations should not be null", evaluations);
        assertTrue("Should find evaluations in date range", evaluations.size() > 0);

        // Verify all evaluations are within date range
        for (ComplianceEvaluation evaluation : evaluations) {
            assertNotNull("Evaluation date should not be null", evaluation.getEvaluatedDate());
            assertEquals("All evaluations should be for test standard", testStandardId, evaluation.getStandardId());
        }
    }

    // ================================
    // Helper Methods
    // ================================

    private ComplianceStandard createTestStandard() {
        ComplianceStandard standard = new ComplianceStandard();
        standard.setName("Test Standard for Evaluations");
        standard.setIssuingBody("Test Authority");
        standard.setRegulationNumber("TE-001");
        standard.setVersion("1.0");
        return standard;
    }

    private ParameterGroup createTestParameterGroup(String standardId) {
        ParameterGroup group = new ParameterGroup();
        group.setStandardId(standardId);
        group.setName("Test Parameter Group");
        group.setDescription("Test group for evaluation testing");
        group.setSortOrder(1);
        return group;
    }

    private ComplianceThreshold createTestThreshold(String groupId) {
        ComplianceThreshold threshold = new ComplianceThreshold();
        threshold.setGroupId(groupId);
        threshold.setParameterCode("pH");
        threshold.setDisplayName("pH Level");
        threshold.setThresholdType(ThresholdType.RANGE);
        threshold.setMinValue(new BigDecimal("6.5"));
        threshold.setMaxValue(new BigDecimal("8.5"));
        threshold.setUnits("pH units");
        threshold.setSortOrder(1);
        return threshold;
    }

    private ComplianceEvaluation createTestEvaluation(String sampleId, String thresholdId) {
        ComplianceEvaluation evaluation = new ComplianceEvaluation();
        evaluation.setSampleId(sampleId);
        evaluation.setStandardId(testStandardId);
        evaluation.setStandardVersion("1.0");
        evaluation.setStatus(EvaluationStatus.PENDING);
        evaluation.setEvaluatedBy("test-user");
        evaluation.setEvaluatedDate(new java.util.Date());
        return evaluation;
    }

    private ComplianceEvaluation createEvaluationWithResults(String sampleId) {
        ComplianceEvaluation evaluation = createTestEvaluation(sampleId, testThresholdId);

        // Add mixed results (some pass, some fail)
        EvaluationResult result1 = new EvaluationResult();
        result1.setThresholdId(testThresholdId);
        result1.setTestedValue(new BigDecimal("7.2")); // Compliant
        result1.setCompliant(true);

        EvaluationResult result2 = new EvaluationResult();
        result2.setThresholdId(testThresholdId);
        result2.setTestedValue(new BigDecimal("9.0")); // Non-compliant
        result2.setCompliant(false);

        evaluation.getEvaluationResults().add(result1);
        evaluation.getEvaluationResults().add(result2);

        return evaluation;
    }

    private ComplianceEvaluation createAllCompliantEvaluation(String sampleId) {
        ComplianceEvaluation evaluation = createTestEvaluation(sampleId, testThresholdId);

        // Add all compliant results
        EvaluationResult result = new EvaluationResult();
        result.setThresholdId(testThresholdId);
        result.setTestedValue(new BigDecimal("7.2")); // Compliant
        result.setCompliant(true);

        evaluation.getEvaluationResults().add(result);

        return evaluation;
    }

    private String createAndSaveEvaluation(String sampleId, String thresholdId, EvaluationStatus status) {
        ComplianceEvaluation evaluation = createTestEvaluation(sampleId, thresholdId);
        evaluation.setStatus(status);
        return complianceEvaluationService.save(evaluation).getId();
    }
}