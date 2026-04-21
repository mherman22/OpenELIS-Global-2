package org.openelisglobal.compliance.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;

/**
 * Service interface for ComplianceThreshold entity operations.
 *
 * Follows OpenELIS service patterns extending BaseObjectService for standard CRUD operations.
 * Provides domain-specific methods for threshold management and evaluation.
 */
public interface ComplianceThresholdService extends BaseObjectService<ComplianceThreshold, String> {

    /**
     * Get all thresholds for a specific parameter group, ordered by sort order
     */
    List<ComplianceThreshold> getThresholdsByGroupId(String groupId);

    /**
     * Get all thresholds by parameter code across all groups
     */
    List<ComplianceThreshold> getThresholdsByParameterCode(String parameterCode);

    /**
     * Evaluate a threshold against a tested value
     */
    boolean evaluateThreshold(String thresholdId, BigDecimal testedValue);

    /**
     * Get threshold with eagerly loaded evaluation results
     */
    ComplianceThreshold getThresholdWithEvaluations(String thresholdId);

    /**
     * Bulk import thresholds for a parameter group
     */
    List<String> bulkImport(String groupId, List<ComplianceThreshold> thresholds);

    /**
     * Reorder thresholds within a parameter group
     */
    void reorderThresholds(String groupId, String[] newOrderIds);

    /**
     * Check if a parameter code already exists within a group
     */
    boolean parameterExistsInGroup(String groupId, String parameterCode);

    /**
     * Get thresholds for multiple groups
     */
    List<ComplianceThreshold> getThresholdsByGroupIds(List<String> groupIds);

    /**
     * Delete all thresholds for a parameter group
     */
    void deleteThresholdsByGroupId(String groupId);
}