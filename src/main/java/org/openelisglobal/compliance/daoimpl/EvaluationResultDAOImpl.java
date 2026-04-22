package org.openelisglobal.compliance.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.EvaluationResultDAO;
import org.openelisglobal.compliance.valueholder.EvaluationResult;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for EvaluationResult entity.
 *
 * Follows OpenELIS DAO patterns: - Extends BaseDAOImpl<EvaluationResult,
 * String> - Uses Hibernate Session for database operations - Proper error
 * handling with LIMSRuntimeException - @Transactional annotations for
 * transaction boundaries
 */
@Component
@Transactional
public class EvaluationResultDAOImpl extends BaseDAOImpl<EvaluationResult, String>
        implements EvaluationResultDAO {

    public EvaluationResultDAOImpl() {
        super(EvaluationResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(EvaluationResult evaluationResult) throws LIMSRuntimeException {
        try {
            EvaluationResult data = entityManager.unwrap(Session.class).get(EvaluationResult.class,
                    evaluationResult.getId());
            if (data != null) {
                PropertyUtils.copyProperties(evaluationResult, data);
            } else {
                evaluationResult.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsByEvaluationId(String evaluationId) {
        try {
            String hql = "FROM EvaluationResult er WHERE er.evaluation.id = :evaluationId ORDER BY er.threshold.parameterCode";
            Query<EvaluationResult> query = entityManager.unwrap(Session.class).createQuery(hql, EvaluationResult.class);
            query.setParameter("evaluationId", evaluationId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getResultsByEvaluationId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsByParameterName(String parameterName) {
        try {
            String hql = "FROM EvaluationResult er WHERE er.threshold.parameterCode = :parameterName ORDER BY er.measurementDate DESC";
            Query<EvaluationResult> query = entityManager.unwrap(Session.class).createQuery(hql, EvaluationResult.class);
            query.setParameter("parameterName", parameterName);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getResultsByParameterName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsByStatus(Boolean isCompliant) {
        try {
            String hql = "FROM EvaluationResult er WHERE er.isCompliant = :status ORDER BY er.measurementDate DESC";
            Query<EvaluationResult> query = entityManager.unwrap(Session.class).createQuery(hql, EvaluationResult.class);
            query.setParameter("status", isCompliant);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getResultsByStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsByThresholdId(String thresholdId) {
        try {
            String hql = "FROM EvaluationResult er WHERE er.threshold.id = :thresholdId ORDER BY er.measurementDate DESC";
            Query<EvaluationResult> query = entityManager.unwrap(Session.class).createQuery(hql, EvaluationResult.class);
            query.setParameter("thresholdId", thresholdId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getResultsByThresholdId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationResult getResultByEvaluationAndParameter(String evaluationId, String parameterName) {
        try {
            String hql = "FROM EvaluationResult er WHERE er.evaluation.id = :evaluationId AND er.threshold.parameterCode = :parameterName";
            Query<EvaluationResult> query = entityManager.unwrap(Session.class).createQuery(hql, EvaluationResult.class);
            query.setParameter("evaluationId", evaluationId);
            query.setParameter("parameterName", parameterName);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getResultByEvaluationAndParameter()", e);
        }
    }

    @Override
    @Transactional
    public void deleteResultsByEvaluationId(String evaluationId) {
        try {
            String hql = "DELETE FROM EvaluationResult er WHERE er.evaluation.id = :evaluationId";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(hql);
            query.setParameter("evaluationId", evaluationId);
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult deleteResultsByEvaluationId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getPageOfResults(int startingRecNo) {
        try {
            String hql = "FROM EvaluationResult er ORDER BY er.measurementDate DESC";
            Query<EvaluationResult> query = entityManager.unwrap(Session.class).createQuery(hql, EvaluationResult.class);
            query.setFirstResult(startingRecNo);
            query.setMaxResults(20); // Default page size
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getPageOfResults()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalResultCount() {
        try {
            String hql = "SELECT COUNT(*) FROM EvaluationResult er";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            return query.uniqueResult().intValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getTotalResultCount()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> searchResults(String evaluationId, String parameterName, Boolean isCompliant) {
        try {
            StringBuilder hql = new StringBuilder("FROM EvaluationResult er WHERE 1=1");

            if (evaluationId != null && !evaluationId.isEmpty()) {
                hql.append(" AND er.evaluation.id = :evaluationId");
            }
            if (parameterName != null && !parameterName.isEmpty()) {
                hql.append(" AND er.threshold.parameterCode LIKE :parameterName");
            }
            if (isCompliant != null) {
                hql.append(" AND er.isCompliant = :isCompliant");
            }

            hql.append(" ORDER BY er.measurementDate DESC");

            Query<EvaluationResult> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), EvaluationResult.class);

            if (evaluationId != null && !evaluationId.isEmpty()) {
                query.setParameter("evaluationId", evaluationId);
            }
            if (parameterName != null && !parameterName.isEmpty()) {
                query.setParameter("parameterName", "%" + parameterName + "%");
            }
            if (isCompliant != null) {
                query.setParameter("isCompliant", isCompliant);
            }

            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult searchResults()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsForExport() {
        try {
            String hql = "FROM EvaluationResult er ORDER BY er.evaluation.id, er.threshold.parameterCode";
            Query<EvaluationResult> query = entityManager.unwrap(Session.class).createQuery(hql, EvaluationResult.class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getResultsForExport()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getResultStatistics() {
        try {
            String hql = "SELECT er.isCompliant, COUNT(*), AVG(er.variancePercentage) " +
                        "FROM EvaluationResult er " +
                        "GROUP BY er.isCompliant " +
                        "ORDER BY er.isCompliant";
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EvaluationResult getResultStatistics()", e);
        }
    }
}