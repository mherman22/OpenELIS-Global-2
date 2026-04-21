package org.openelisglobal.compliance.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ParameterGroup;

/**
 * ParameterGroupDAO interface for parameter group data access operations.
 *
 * Follows OpenELIS DAO pattern extending BaseDAO for standard CRUD operations.
 */
public interface ParameterGroupDAO extends BaseDAO<ParameterGroup, String> {

    /**
     * Get all parameter groups for a compliance standard.
     */
    List<ParameterGroup> getParameterGroupsByStandard(String standardId) throws LIMSRuntimeException;

    /**
     * Get parameter groups ordered by sort order.
     */
    List<ParameterGroup> getParameterGroupsByStandardOrdered(String standardId) throws LIMSRuntimeException;

    /**
     * Check if parameter group exists with given name for standard.
     */
    boolean parameterGroupExistsForStandard(String standardId, String groupName) throws LIMSRuntimeException;

    /**
     * Get all parameter groups for a standard by ID.
     */
    List<ParameterGroup> getGroupsByStandardId(String standardId) throws LIMSRuntimeException;

    /**
     * Reorder parameter groups within a standard.
     */
    void reorderGroups(String standardId, String[] groupIds) throws LIMSRuntimeException;

    /**
     * Get parameter group with eagerly loaded thresholds.
     */
    ParameterGroup getGroupWithThresholds(String groupId) throws LIMSRuntimeException;

    /**
     * Count parameter groups for a standard.
     */
    int countGroupsByStandardId(String standardId) throws LIMSRuntimeException;

    /**
     * Check if a standard has any parameter groups.
     */
    boolean standardHasGroups(String standardId) throws LIMSRuntimeException;
}