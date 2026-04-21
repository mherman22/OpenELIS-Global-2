package org.openelisglobal.compliance.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.ParameterGroup;

/**
 * ParameterGroupService interface for managing parameter groups within compliance standards.
 *
 * Follows OpenELIS service pattern extending BaseObjectService for standard CRUD operations.
 */
public interface ParameterGroupService extends BaseObjectService<ParameterGroup, String> {

    /**
     * Get all parameter groups for a compliance standard.
     */
    List<ParameterGroup> getParameterGroupsByStandard(String standardId);

    /**
     * Get parameter groups ordered by sort order.
     */
    List<ParameterGroup> getParameterGroupsByStandardOrdered(String standardId);

    /**
     * Check if parameter group exists with given name for standard.
     */
    boolean parameterGroupExistsForStandard(String standardId, String groupName);

    // Additional methods required by tests

    /**
     * Get all parameter groups for a standard by ID.
     */
    List<ParameterGroup> getGroupsByStandardId(String standardId);

    /**
     * Reorder parameter groups within a standard.
     */
    void reorderGroups(String standardId, String[] groupIds);

    /**
     * Get parameter group with eagerly loaded thresholds.
     */
    ParameterGroup getGroupWithThresholds(String groupId);

    /**
     * Count parameter groups for a standard.
     */
    int countGroupsByStandardId(String standardId);

    /**
     * Check if a standard has any parameter groups.
     */
    boolean standardHasGroups(String standardId);
}