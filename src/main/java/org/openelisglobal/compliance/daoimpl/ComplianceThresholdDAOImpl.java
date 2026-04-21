package org.openelisglobal.compliance.daoimpl;

import java.util.List;
import jakarta.persistence.TypedQuery;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.ComplianceThresholdDAO;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ComplianceThresholdDAO following OpenELIS patterns.
 *
 * Constitutional compliance:
 * - Uses @Transactional annotations
 * - Implements proper exception handling with logging
 * - Uses HQL queries exclusively (no native SQL)
 */
@Component
@Transactional
public class ComplianceThresholdDAOImpl extends BaseDAOImpl<ComplianceThreshold, String> implements ComplianceThresholdDAO {

    public ComplianceThresholdDAOImpl() {
        super(ComplianceThreshold.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByGroupId(String groupId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceThreshold ct WHERE ct.group.id = :groupId ORDER BY ct.sortOrder, ct.parameterCode";
            TypedQuery<ComplianceThreshold> query = entityManager.createQuery(hql, ComplianceThreshold.class);
            query.setParameter("groupId", groupId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold getThresholdsByGroupId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByParameterCode(String parameterCode) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceThreshold ct WHERE ct.parameterCode = :parameterCode ORDER BY ct.group.id, ct.sortOrder";
            TypedQuery<ComplianceThreshold> query = entityManager.createQuery(hql, ComplianceThreshold.class);
            query.setParameter("parameterCode", parameterCode);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold getThresholdsByParameterCode()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceThreshold getThresholdWithEvaluations(String thresholdId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceThreshold ct LEFT JOIN FETCH ct.complianceEvaluations WHERE ct.id = :id";
            TypedQuery<ComplianceThreshold> query = entityManager.createQuery(hql, ComplianceThreshold.class);
            query.setParameter("id", thresholdId);
            List<ComplianceThreshold> list = query.getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold getThresholdWithEvaluations()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean parameterExistsInGroup(String groupId, String parameterCode) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(ct) FROM ComplianceThreshold ct WHERE ct.group.id = :groupId AND ct.parameterCode = :parameterCode";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("groupId", groupId);
            query.setParameter("parameterCode", parameterCode);
            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold parameterExistsInGroup()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByGroupIds(List<String> groupIds) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceThreshold ct WHERE ct.group.id IN :groupIds ORDER BY ct.group.id, ct.sortOrder";
            TypedQuery<ComplianceThreshold> query = entityManager.createQuery(hql, ComplianceThreshold.class);
            query.setParameter("groupIds", groupIds);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold getThresholdsByGroupIds()", e);
        }
    }

    @Override
    @Transactional
    public void deleteThresholdsByGroupId(String groupId) throws LIMSRuntimeException {
        try {
            String hql = "DELETE FROM ComplianceThreshold ct WHERE ct.group.id = :groupId";
            entityManager.createQuery(hql)
                    .setParameter("groupId", groupId)
                    .executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold deleteThresholdsByGroupId()", e);
        }
    }

    @Override
    @Transactional
    public void updateSortOrders(String groupId, List<String> thresholdIds) throws LIMSRuntimeException {
        try {
            for (int i = 0; i < thresholdIds.size(); i++) {
                String hql = "UPDATE ComplianceThreshold ct SET ct.sortOrder = :sortOrder WHERE ct.id = :id AND ct.group.id = :groupId";
                entityManager.createQuery(hql)
                        .setParameter("sortOrder", i + 1)
                        .setParameter("id", thresholdIds.get(i))
                        .setParameter("groupId", groupId)
                        .executeUpdate();
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold updateSortOrders()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getMaxSortOrderForGroup(String groupId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COALESCE(MAX(ct.sortOrder), 0) FROM ComplianceThreshold ct WHERE ct.group.id = :groupId";
            TypedQuery<Integer> query = entityManager.createQuery(hql, Integer.class);
            query.setParameter("groupId", groupId);
            return query.getSingleResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold getMaxSortOrderForGroup()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasLinkedEvaluations(String thresholdId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(er) FROM EvaluationResult er WHERE er.threshold.id = :thresholdId";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("thresholdId", thresholdId);
            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold hasLinkedEvaluations()", e);
        }
    }
}