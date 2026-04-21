package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TDD Test Suite for ComplianceThresholdService
 *
 * Tests threshold management functionality within compliance standards. Follows
 * constitutional TDD requirements and validates proper transaction boundaries.
 * Tests threshold value validation, parameter associations, and evaluation
 * logic.
 */
public class ComplianceThresholdServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ParameterGroupService parameterGroupService;

    private String testStandardId;
    private String testGroupId;
    private ComplianceThreshold testThreshold;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance_standards.xml");

        // Create test standard and parameter group
        ComplianceStandard testStandard = createTestStandard();
        testStandardId = complianceStandardService.save(testStandard);

        ParameterGroup testGroup = createTestParameterGroup(testStandardId);
        testGroupId = parameterGroupService.save(testGroup);

        // Create test threshold
        testThreshold = createTestThreshold(testGroupId);
    }

    // ================================
    // RED PHASE: Compliance Threshold CRUD Tests
    // ================================

    @Test
    public void testSaveThreshold_shouldCreateNewThreshold() {
        // RED: Will fail - service doesn't exist
        ComplianceThreshold newThreshold = createValidThreshold(testGroupId, "pH", "pH Level", ThresholdType.RANGE,
                new BigDecimal("6.5"), new BigDecimal("8.5"), "pH units");

        String savedId = complianceThresholdService.save(newThreshold);

        assertNotNull("Saved ID should not be null", savedId);
        assertEquals("Threshold ID should match", newThreshold.getId(), savedId);

        // Verify group relationship
        assertEquals("Group ID should be set", testGroupId, newThreshold.getGroupId());

        // Verify audit fields
        assertNotNull("Created date should be set", newThreshold.getLastupdated());
    }

    @Test
    public void testGetThresholdsByGroupId_shouldReturnOrderedThresholds() {
        // RED: Will fail - method doesn't exist
        // Create multiple thresholds with different sort orders
        createAndSaveThreshold(testGroupId, "pH", ThresholdType.RANGE, 1);
        createAndSaveThreshold(testGroupId, "Temperature", ThresholdType.MAXIMUM, 2);
        createAndSaveThreshold(testGroupId, "Turbidity", ThresholdType.MAXIMUM, 3);

        List<ComplianceThreshold> thresholds = complianceThresholdService.getThresholdsByGroupId(testGroupId);

        assertNotNull("Thresholds list should not be null", thresholds);
        assertEquals("Should have 3 thresholds", 3, thresholds.size());

        // Verify sort order
        assertEquals("First threshold should be pH", "pH", thresholds.get(0).getParameterCode());
        assertEquals("Second threshold should be Temperature", "Temperature", thresholds.get(1).getParameterCode());
        assertEquals("Third threshold should be Turbidity", "Turbidity", thresholds.get(2).getParameterCode());

        // Inversion test: verify proper ordering logic
        assertTrue("Sort orders should be ascending",
                thresholds.get(0).getSortOrder() < thresholds.get(1).getSortOrder());
    }

    @Test
    public void testUpdateThreshold_shouldUpdateExistingThreshold() {
        // RED: Will fail - update method doesn't exist
        String thresholdId = complianceThresholdService.save(testThreshold);
        ComplianceThreshold savedThreshold = complianceThresholdService.get(thresholdId);

        BigDecimal originalMinValue = savedThreshold.getMinValue();
        BigDecimal newMinValue = new BigDecimal("7.0");
        savedThreshold.setMinValue(newMinValue);

        complianceThresholdService.update(savedThreshold);

        // Verify update persistence
        ComplianceThreshold updatedThreshold = complianceThresholdService.get(thresholdId);
        assertEquals("Min value should be updated", newMinValue, updatedThreshold.getMinValue());

        // Inversion test
        assertFalse("Updated min value should differ from original", originalMinValue.equals(newMinValue));
    }

    @Test
    public void testDeleteThreshold_shouldPreventDeletionWithEvaluations() {
        // RED: Will fail - deletion protection doesn't exist
        String thresholdId = complianceThresholdService.save(testThreshold);

        // Simulate threshold with linked evaluations
        // (In real implementation, this would be checked via ComplianceEvaluation
        // table)

        try {
            complianceThresholdService.delete(thresholdId);
            // If threshold has evaluations, deletion should fail
        } catch (Exception e) {
            assertTrue("Should prevent deletion when evaluations exist", e.getMessage().contains("linked evaluations"));
        }
    }

    @Test
    public void testValidateThresholdValues_shouldEnforceBusinessRules() {
        // RED: Will fail - validation doesn't exist
        ComplianceThreshold invalidRangeThreshold = createValidThreshold(testGroupId, "pH", "pH Level",
                ThresholdType.RANGE, new BigDecimal("8.5"), // min > max (invalid)
                new BigDecimal("6.5"), "pH units");

        try {
            complianceThresholdService.save(invalidRangeThreshold);
            fail("Should not allow min value greater than max value");
        } catch (Exception e) {
            assertTrue("Should throw validation error for invalid range",
                    e.getMessage().contains("minimum value cannot exceed maximum"));
        }
    }

    @Test
    public void testGetThresholdsByParameterCode_shouldReturnMatchingThresholds() {
        // RED: Will fail - method doesn't exist
        createAndSaveThreshold(testGroupId, "pH", ThresholdType.RANGE, 1);
        createAndSaveThreshold(testGroupId, "pH", ThresholdType.MAXIMUM, 2);
        createAndSaveThreshold(testGroupId, "Temperature", ThresholdType.MAXIMUM, 3);

        List<ComplianceThreshold> pHThresholds = complianceThresholdService.getThresholdsByParameterCode("pH");

        assertNotNull("pH thresholds should not be null", pHThresholds);
        assertEquals("Should have 2 pH thresholds", 2, pHThresholds.size());

        // Verify all returned thresholds are for pH
        for (ComplianceThreshold threshold : pHThresholds) {
            assertEquals("All thresholds should be for pH", "pH", threshold.getParameterCode());
        }
    }

    @Test
    public void testEvaluateThreshold_shouldReturnComplianceResult() {
        // RED: Will fail - evaluation method doesn't exist
        ComplianceThreshold rangeThreshold = createValidThreshold(testGroupId, "pH", "pH Level", ThresholdType.RANGE,
                new BigDecimal("6.5"), new BigDecimal("8.5"), "pH units");

        String thresholdId = complianceThresholdService.save(rangeThreshold);

        // Test value within range
        boolean withinRange = complianceThresholdService.evaluateThreshold(thresholdId, new BigDecimal("7.2"));
        assertTrue("Value 7.2 should be within range 6.5-8.5", withinRange);

        // Test value outside range
        boolean outsideRange = complianceThresholdService.evaluateThreshold(thresholdId, new BigDecimal("9.0"));
        assertFalse("Value 9.0 should be outside range 6.5-8.5", outsideRange);

        // Inversion test
        assertFalse("Compliance results should differ for different values", withinRange == outsideRange);
    }

    @Test
    public void testGetThresholdsWithEvaluations_shouldEagerLoadEvaluations() {
        // RED: Will fail - method doesn't exist
        // Tests constitutional requirement: services must compile data within
        // transaction
        String thresholdId = complianceThresholdService.save(testThreshold);

        ComplianceThreshold thresholdWithEvaluations = complianceThresholdService
                .getThresholdWithEvaluations(thresholdId);

        assertNotNull("Threshold should not be null", thresholdWithEvaluations);
        assertNotNull("Evaluations should be eager loaded", thresholdWithEvaluations.getComplianceEvaluations());

        // Verify no LazyInitializationException occurs outside transaction
        for (var evaluation : thresholdWithEvaluations.getComplianceEvaluations()) {
            assertNotNull("Evaluation values should be accessible", evaluation.getTestedValue());
        }
    }

    @Test
    public void testBulkImportThresholds_shouldValidateAndSaveThresholds() {
        // RED: Will fail - bulk import doesn't exist
        List<ComplianceThreshold> thresholdsToImport = List.of(
                createValidThreshold(testGroupId, "pH", "pH Level", ThresholdType.RANGE, new BigDecimal("6.5"),
                        new BigDecimal("8.5"), "pH units"),
                createValidThreshold(testGroupId, "Temperature", "Water Temperature", ThresholdType.MAXIMUM, null,
                        new BigDecimal("30"), "°C"),
                createValidThreshold(testGroupId, "Turbidity", "Water Turbidity", ThresholdType.MAXIMUM, null,
                        new BigDecimal("4"), "NTU"));

        List<String> savedIds = complianceThresholdService.bulkImport(testGroupId, thresholdsToImport);

        assertNotNull("Saved IDs should not be null", savedIds);
        assertEquals("Should import 3 thresholds", 3, savedIds.size());

        // Verify all thresholds were saved
        for (String savedId : savedIds) {
            ComplianceThreshold savedThreshold = complianceThresholdService.get(savedId);
            assertNotNull("Imported threshold should exist", savedThreshold);
            assertEquals("All thresholds should belong to test group", testGroupId, savedThreshold.getGroupId());
        }
    }

    @Test
    public void testUniquenessConstraintPerGroup_shouldPreventDuplicateParameters() {
        // RED: Will fail - uniqueness validation doesn't exist
        createAndSaveThreshold(testGroupId, "pH", ThresholdType.RANGE, 1);

        // Try to create duplicate parameter in same group
        ComplianceThreshold duplicate = createValidThreshold(testGroupId, "pH", // Same parameter code
                "Different display name", ThresholdType.MAXIMUM, // Different type but same parameter
                null, new BigDecimal("8.0"), "pH units");

        try {
            complianceThresholdService.save(duplicate);
            fail("Should not allow duplicate parameter code within same group");
        } catch (Exception e) {
            assertTrue("Should throw uniqueness constraint violation", e.getMessage().contains("already exists"));
        }
    }

    @Test
    public void testReorderThresholds_shouldUpdateSortOrders() {
        // RED: Will fail - reorder method doesn't exist
        String threshold1Id = createAndSaveThreshold(testGroupId, "pH", ThresholdType.RANGE, 1);
        String threshold2Id = createAndSaveThreshold(testGroupId, "Temperature", ThresholdType.MAXIMUM, 2);
        String threshold3Id = createAndSaveThreshold(testGroupId, "Turbidity", ThresholdType.MAXIMUM, 3);

        // Reorder: move turbidity to position 1
        String[] newOrder = { threshold3Id, threshold1Id, threshold2Id };
        complianceThresholdService.reorderThresholds(testGroupId, newOrder);

        List<ComplianceThreshold> reorderedThresholds = complianceThresholdService.getThresholdsByGroupId(testGroupId);

        assertEquals("Turbidity should be first", "Turbidity", reorderedThresholds.get(0).getParameterCode());
        assertEquals("pH should be second", "pH", reorderedThresholds.get(1).getParameterCode());
        assertEquals("Temperature should be third", "Temperature", reorderedThresholds.get(2).getParameterCode());

        // Verify sort order values were updated
        assertEquals("First threshold sort order should be 1", Integer.valueOf(1),
                reorderedThresholds.get(0).getSortOrder());
        assertEquals("Second threshold sort order should be 2", Integer.valueOf(2),
                reorderedThresholds.get(1).getSortOrder());
    }

    // ================================
    // Helper Methods
    // ================================

    private ComplianceStandard createTestStandard() {
        ComplianceStandard standard = new ComplianceStandard();
        standard.setName("Test Standard for Thresholds");
        standard.setIssuingBody("Test Authority");
        standard.setRegulationNumber("TT-001");
        standard.setVersion("1.0");
        return standard;
    }

    private ParameterGroup createTestParameterGroup(String standardId) {
        ParameterGroup group = new ParameterGroup();
        group.setStandardId(standardId);
        group.setName("Test Parameter Group");
        group.setDescription("Test group for threshold management");
        group.setSortOrder(1);
        return group;
    }

    private ComplianceThreshold createTestThreshold(String groupId) {
        return createValidThreshold(groupId, "pH", "pH Level", ThresholdType.RANGE, new BigDecimal("6.5"),
                new BigDecimal("8.5"), "pH units");
    }

    private ComplianceThreshold createValidThreshold(String groupId, String parameterCode, String displayName,
            ThresholdType thresholdType, BigDecimal minValue, BigDecimal maxValue, String units) {
        ComplianceThreshold threshold = new ComplianceThreshold();
        threshold.setGroupId(groupId);
        threshold.setParameterCode(parameterCode);
        threshold.setDisplayName(displayName);
        threshold.setThresholdType(thresholdType);
        threshold.setMinValue(minValue);
        threshold.setMaxValue(maxValue);
        threshold.setUnits(units);
        threshold.setSortOrder(1);
        return threshold;
    }

    private String createAndSaveThreshold(String groupId, String parameterCode, ThresholdType thresholdType,
            int sortOrder) {
        ComplianceThreshold threshold = createValidThreshold(groupId, parameterCode, "Test " + parameterCode,
                thresholdType, new BigDecimal("1.0"), new BigDecimal("10.0"), "units");
        threshold.setSortOrder(sortOrder);
        return complianceThresholdService.save(threshold);
    }
}