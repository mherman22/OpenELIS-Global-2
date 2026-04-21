package org.openelisglobal.compliance.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.dao.ParameterGroupDAO;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ParameterGroupService following OpenELIS patterns.
 *
 * Constitutional compliance:
 * - Extends AuditableBaseObjectServiceImpl for audit trail support
 * - Uses @Transactional annotations for data integrity
 * - Implements proper exception handling
 */
@Service
public class ParameterGroupServiceImpl extends AuditableBaseObjectServiceImpl<ParameterGroup, String>
        implements ParameterGroupService {

    @Autowired
    protected ParameterGroupDAO baseObjectDAO;

    ParameterGroupServiceImpl() {
        super(ParameterGroup.class);
    }

    @Override
    protected ParameterGroupDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterGroup> getParameterGroupsByStandard(String standardId) {
        return getBaseObjectDAO().getParameterGroupsByStandard(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterGroup> getParameterGroupsByStandardOrdered(String standardId) {
        return getBaseObjectDAO().getParameterGroupsByStandardOrdered(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean parameterGroupExistsForStandard(String standardId, String groupName) {
        return getBaseObjectDAO().parameterGroupExistsForStandard(standardId, groupName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterGroup> getGroupsByStandardId(String standardId) {
        return getBaseObjectDAO().getGroupsByStandardId(standardId);
    }

    @Override
    @Transactional
    public void reorderGroups(String standardId, String[] groupIds) {
        getBaseObjectDAO().reorderGroups(standardId, groupIds);
    }

    @Override
    @Transactional(readOnly = true)
    public ParameterGroup getGroupWithThresholds(String groupId) {
        return getBaseObjectDAO().getGroupWithThresholds(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countGroupsByStandardId(String standardId) {
        return getBaseObjectDAO().countGroupsByStandardId(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean standardHasGroups(String standardId) {
        return getBaseObjectDAO().standardHasGroups(standardId);
    }
}