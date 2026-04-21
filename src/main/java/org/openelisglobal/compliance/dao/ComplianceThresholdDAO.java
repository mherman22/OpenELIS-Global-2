package org.openelisglobal.compliance.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;

/**
 * DAO interface for ComplianceThreshold entity operations.
 *
 * Follows OpenELIS DAO patterns extending BaseDAO for standard CRUD operations.
 * Provides domain-specific query methods for threshold management.
 */
public interface ComplianceThresholdDAO extends BaseDAO<ComplianceThreshold, String> {

    /**
     * Get all thresholds for a specific parameter group, ordered by sort order
     */
    List<ComplianceThreshold> getThresholdsByGroupId(String groupId) throws LIMSRuntimeException;

    /**
     * Get all thresholds by parameter code across all groups
     */
    List<ComplianceThreshold> getThresholdsByParameterCode(String parameterCode) throws LIMSRuntimeException;

    /**
     * Get threshold with eagerly loaded evaluation results
     */
    ComplianceThreshold getThresholdWithEvaluations(String thresholdId) throws LIMSRuntimeException;

    /**
     * Check if a parameter code already exists within a group
     */
    boolean parameterExistsInGroup(String groupId, String parameterCode) throws LIMSRuntimeException;

    /**
     * Get thresholds for multiple groups
     */
    List<ComplianceThreshold> getThresholdsByGroupIds(List<String> groupIds) throws LIMSRuntimeException;

    /**
     * Delete all thresholds for a parameter group
     */
    void deleteThresholdsByGroupId(String groupId) throws LIMSRuntimeException;

    /**
     * Update sort orders for thresholds in a group
     */
    void updateSortOrders(String groupId, List<String> thresholdIds) throws LIMSRuntimeException;

    /**
     * Get maximum sort order for a parameter group
     */
    Integer getMaxSortOrderForGroup(String groupId) throws LIMSRuntimeException;

    /**
     * Check if threshold has linked evaluation results
     */
    boolean hasLinkedEvaluations(String thresholdId) throws LIMSRuntimeException;
}