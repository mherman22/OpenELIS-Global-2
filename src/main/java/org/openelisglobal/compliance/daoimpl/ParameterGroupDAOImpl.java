package org.openelisglobal.compliance.daoimpl;

import java.util.List;
import jakarta.persistence.TypedQuery;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.ParameterGroupDAO;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ParameterGroupDAO following OpenELIS patterns.
 *
 * Constitutional compliance:
 * - Uses @Transactional annotations
 * - Implements proper exception handling with logging
 * - Uses HQL queries exclusively (no native SQL)
 */
@Component
@Transactional
public class ParameterGroupDAOImpl extends BaseDAOImpl<ParameterGroup, String> implements ParameterGroupDAO {

    public ParameterGroupDAOImpl() {
        super(ParameterGroup.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterGroup> getParameterGroupsByStandard(String standardId) throws LIMSRuntimeException {
        List<ParameterGroup> list;
        try {
            String hql = "FROM ParameterGroup pg WHERE pg.standard.id = :standardId";
            TypedQuery<ParameterGroup> query = entityManager.createQuery(hql, ParameterGroup.class);
            query.setParameter("standardId", standardId);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup getParameterGroupsByStandard()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterGroup> getParameterGroupsByStandardOrdered(String standardId) throws LIMSRuntimeException {
        List<ParameterGroup> list;
        try {
            String hql = "FROM ParameterGroup pg WHERE pg.standard.id = :standardId ORDER BY pg.sortOrder, pg.name";
            TypedQuery<ParameterGroup> query = entityManager.createQuery(hql, ParameterGroup.class);
            query.setParameter("standardId", standardId);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup getParameterGroupsByStandardOrdered()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean parameterGroupExistsForStandard(String standardId, String groupName) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(pg) FROM ParameterGroup pg WHERE pg.standard.id = :standardId AND pg.name = :groupName";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("standardId", standardId);
            query.setParameter("groupName", groupName);
            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup parameterGroupExistsForStandard()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterGroup> getGroupsByStandardId(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ParameterGroup pg WHERE pg.standardId = :standardId ORDER BY pg.sortOrder, pg.name";
            TypedQuery<ParameterGroup> query = entityManager.createQuery(hql, ParameterGroup.class);
            query.setParameter("standardId", standardId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup getGroupsByStandardId()", e);
        }
    }

    @Override
    @Transactional
    public void reorderGroups(String standardId, String[] groupIds) throws LIMSRuntimeException {
        try {
            for (int i = 0; i < groupIds.length; i++) {
                String hql = "UPDATE ParameterGroup pg SET pg.sortOrder = :sortOrder WHERE pg.id = :id AND pg.standardId = :standardId";
                entityManager.createQuery(hql)
                        .setParameter("sortOrder", i + 1)
                        .setParameter("id", groupIds[i])
                        .setParameter("standardId", standardId)
                        .executeUpdate();
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup reorderGroups()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ParameterGroup getGroupWithThresholds(String groupId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ParameterGroup pg LEFT JOIN FETCH pg.complianceThresholds WHERE pg.id = :id";
            TypedQuery<ParameterGroup> query = entityManager.createQuery(hql, ParameterGroup.class);
            query.setParameter("id", groupId);
            List<ParameterGroup> list = query.getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup getGroupWithThresholds()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countGroupsByStandardId(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(pg) FROM ParameterGroup pg WHERE pg.standardId = :standardId";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("standardId", standardId);
            return query.getSingleResult().intValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup countGroupsByStandardId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean standardHasGroups(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(pg) FROM ParameterGroup pg WHERE pg.standardId = :standardId";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("standardId", standardId);
            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup standardHasGroups()", e);
        }
    }
}