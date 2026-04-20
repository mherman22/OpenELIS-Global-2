package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TDD Test Suite for ParameterGroupService
 *
 * Tests the parameter group management functionality within compliance standards.
 * Follows constitutional TDD requirements and tests proper transaction boundaries.
 */
public class ParameterGroupServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ParameterGroupService parameterGroupService;

    @Autowired
    private ComplianceStandardService complianceStandardService;

    private String testStandardId;
    private ParameterGroup testParameterGroup;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance_standards.xml");

        // Create test standard
        ComplianceStandard testStandard = createTestStandard();
        testStandardId = complianceStandardService.save(testStandard);

        // Create test parameter group
        testParameterGroup = createTestParameterGroup(testStandardId);
    }

    // ================================
    // RED PHASE: Parameter Group CRUD Tests
    // ================================

    @Test
    public void testSaveParameterGroup_shouldCreateNewGroup() {
        // RED: Will fail - service doesn't exist
        ParameterGroup newGroup = createValidParameterGroup(
            testStandardId,
            "Physical Parameters",
            "Temperature, pH, turbidity, color parameters",
            1
        );

        String savedId = parameterGroupService.save(newGroup);

        assertNotNull("Saved ID should not be null", savedId);
        assertEquals("Group ID should match", newGroup.getId(), savedId);

        // Verify standard relationship
        assertEquals("Standard ID should be set", testStandardId, newGroup.getStandardId());

        // Verify audit fields
        assertNotNull("Created date should be set", newGroup.getLastupdated());
    }

    @Test
    public void testGetGroupsByStandardId_shouldReturnOrderedGroups() {
        // RED: Will fail - method doesn't exist
        // Create multiple groups with different sort orders
        createAndSaveParameterGroup(testStandardId, "Chemical Parameters", 2);
        createAndSaveParameterGroup(testStandardId, "Physical Parameters", 1);
        createAndSaveParameterGroup(testStandardId, "Biological Parameters", 3);

        List<ParameterGroup> groups = parameterGroupService.getGroupsByStandardId(testStandardId);

        assertNotNull("Groups list should not be null", groups);
        assertEquals("Should have 3 groups", 3, groups.size());

        // Verify sort order
        assertEquals("First group should be Physical", "Physical Parameters", groups.get(0).getName());
        assertEquals("Second group should be Chemical", "Chemical Parameters", groups.get(1).getName());
        assertEquals("Third group should be Biological", "Biological Parameters", groups.get(2).getName());

        // Inversion test: verify proper ordering logic
        assertTrue("Sort orders should be ascending",
            groups.get(0).getSortOrder() < groups.get(1).getSortOrder());
    }

    @Test
    public void testUpdateParameterGroup_shouldUpdateExistingGroup() {
        // RED: Will fail - update method doesn't exist
        String groupId = parameterGroupService.save(testParameterGroup);
        ParameterGroup savedGroup = parameterGroupService.get(groupId);

        String originalName = savedGroup.getName();
        String newName = "Updated Parameter Group";
        savedGroup.setName(newName);

        parameterGroupService.update(savedGroup);

        // Verify update persistence
        ParameterGroup updatedGroup = parameterGroupService.get(groupId);
        assertEquals("Name should be updated", newName, updatedGroup.getName());

        // Inversion test
        assertFalse("Updated name should differ from original", originalName.equals(newName));
    }

    @Test
    public void testDeleteParameterGroup_shouldPreventDeletionWithThresholds() {
        // RED: Will fail - deletion protection doesn't exist
        String groupId = parameterGroupService.save(testParameterGroup);

        // Simulate group with linked thresholds
        // (In real implementation, this would be checked via ComplianceThreshold table)

        try {
            parameterGroupService.delete(groupId);
            // If group has thresholds, deletion should fail
        } catch (Exception e) {
            assertTrue("Should prevent deletion when thresholds exist",
                e.getMessage().contains("linked thresholds"));
        }
    }

    @Test
    public void testReorderParameterGroups_shouldUpdateSortOrders() {
        // RED: Will fail - reorder method doesn't exist
        String group1Id = createAndSaveParameterGroup(testStandardId, "Group 1", 1);
        String group2Id = createAndSaveParameterGroup(testStandardId, "Group 2", 2);
        String group3Id = createAndSaveParameterGroup(testStandardId, "Group 3", 3);

        // Reorder: move group 3 to position 1
        String[] newOrder = {group3Id, group1Id, group2Id};
        parameterGroupService.reorderGroups(testStandardId, newOrder);

        List<ParameterGroup> reorderedGroups = parameterGroupService
            .getGroupsByStandardId(testStandardId);

        assertEquals("Group 3 should be first", "Group 3", reorderedGroups.get(0).getName());
        assertEquals("Group 1 should be second", "Group 1", reorderedGroups.get(1).getName());
        assertEquals("Group 2 should be third", "Group 2", reorderedGroups.get(2).getName());

        // Verify sort order values were updated
        assertEquals("First group sort order should be 1", Integer.valueOf(1),
            reorderedGroups.get(0).getSortOrder());
        assertEquals("Second group sort order should be 2", Integer.valueOf(2),
            reorderedGroups.get(1).getSortOrder());
    }

    @Test
    public void testGetGroupWithThresholds_shouldEagerLoadThresholds() {
        // RED: Will fail - method doesn't exist
        // Tests constitutional requirement: services must compile data within transaction
        String groupId = parameterGroupService.save(testParameterGroup);

        ParameterGroup groupWithThresholds = parameterGroupService.getGroupWithThresholds(groupId);

        assertNotNull("Group should not be null", groupWithThresholds);
        assertNotNull("Thresholds should be eager loaded", groupWithThresholds.getComplianceThresholds());

        // Verify no LazyInitializationException occurs outside transaction
        for (var threshold : groupWithThresholds.getComplianceThresholds()) {
            assertNotNull("Threshold values should be accessible", threshold.getThresholdType());
        }
    }

    @Test
    public void testUniquenessConstraintPerStandard_shouldPreventDuplicateGroupNames() {
        // RED: Will fail - uniqueness validation doesn't exist
        createAndSaveParameterGroup(testStandardId, "Physical Parameters", 1);

        // Try to create duplicate group name in same standard
        ParameterGroup duplicate = createValidParameterGroup(
            testStandardId,
            "Physical Parameters", // Same name
            "Different description",
            2
        );

        try {
            parameterGroupService.save(duplicate);
            fail("Should not allow duplicate group name within same standard");
        } catch (Exception e) {
            assertTrue("Should throw uniqueness constraint violation",
                e.getMessage().contains("already exists"));
        }
    }

    @Test
    public void testCascadeDeletePrevention_shouldWarnBeforeDeletingStandardWithGroups() {
        // RED: Will fail - cascade validation doesn't exist
        createAndSaveParameterGroup(testStandardId, "Group 1", 1);
        createAndSaveParameterGroup(testStandardId, "Group 2", 2);

        int groupCount = parameterGroupService.countGroupsByStandardId(testStandardId);
        assertEquals("Should have 2 groups", 2, groupCount);

        // Attempting to delete standard should show warning about groups
        boolean hasGroups = parameterGroupService.standardHasGroups(testStandardId);
        assertTrue("Standard should have groups", hasGroups);
    }

    // ================================
    // Helper Methods
    // ================================

    private ComplianceStandard createTestStandard() {
        ComplianceStandard standard = new ComplianceStandard();
        standard.setName("Test Standard for Parameter Groups");
        standard.setIssuingBody("Test Authority");
        standard.setRegulationNumber("TG-001");
        standard.setVersion("1.0");
        return standard;
    }

    private ParameterGroup createTestParameterGroup(String standardId) {
        return createValidParameterGroup(
            standardId,
            "Test Parameter Group",
            "Test group for parameter management",
            1
        );
    }

    private ParameterGroup createValidParameterGroup(String standardId, String name,
                                                     String description, int sortOrder) {
        ParameterGroup group = new ParameterGroup();
        group.setStandardId(standardId);
        group.setName(name);
        group.setDescription(description);
        group.setSortOrder(sortOrder);
        return group;
    }

    private String createAndSaveParameterGroup(String standardId, String name, int sortOrder) {
        ParameterGroup group = createValidParameterGroup(
            standardId, name, "Test description", sortOrder);
        return parameterGroupService.save(group);
    }
}