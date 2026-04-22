package org.openelisglobal.compliance.daoimpl;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.TypedQuery;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.ComplianceEvaluationDAO;
import org.openelisglobal.compliance.valueholder.ComplianceEvaluation;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ComplianceEvaluationDAO following OpenELIS patterns.
 *
 * Constitutional compliance:
 * - Uses @Transactional annotations
 * - Implements proper exception handling with logging
 * - Uses HQL queries exclusively (no native SQL)
 */
@Component
@Transactional
public class ComplianceEvaluationDAOImpl extends BaseDAOImpl<ComplianceEvaluation, String> implements ComplianceEvaluationDAO {

    public ComplianceEvaluationDAOImpl() {
        super(ComplianceEvaluation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceEvaluation getEvaluationByFhirId(UUID fhirUuid) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce WHERE ce.fhirUuid = :fhirUuid";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("fhirUuid", fhirUuid);
            List<ComplianceEvaluation> list = query.getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationByFhirId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsByComplianceStandard(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce WHERE ce.standardId = :standardId ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("standardId", standardId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationsByComplianceStandard()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsBySampleId(String sampleId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce WHERE ce.sampleId = :sampleId ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("sampleId", sampleId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationsBySampleId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsByStatus(EvaluationStatus status) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce WHERE ce.status = :status ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("status", status);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationsByStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsInRange(Date startDate, Date endDate) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce WHERE ce.evaluatedDate >= :startDate AND ce.evaluatedDate <= :endDate ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationsInRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsSince(Date sinceDate) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce WHERE ce.evaluatedDate >= :sinceDate ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("sinceDate", sinceDate);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationsSince()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceEvaluation getEvaluationWithResults(String evaluationId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce LEFT JOIN FETCH ce.evaluationResults WHERE ce.id = :id";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("id", evaluationId);
            List<ComplianceEvaluation> list = query.getResultList();
            return list.isEmpty() ? null : list.getFirst();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationWithResults()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getPageOfEvaluations(int startingRecNo) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setFirstResult(startingRecNo);
            query.setMaxResults(20); // Standard page size
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getPageOfEvaluations()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getPagesOfSearchedEvaluations(int startingRecNo, String searchString) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce WHERE ce.sampleId LIKE :searchString OR ce.standardId LIKE :searchString ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("searchString", "%" + searchString + "%");
            query.setFirstResult(startingRecNo);
            query.setMaxResults(20); // Standard page size
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getPagesOfSearchedEvaluations()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalEvaluationCount() throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(ce) FROM ComplianceEvaluation ce";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            return query.getSingleResult().intValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getTotalEvaluationCount()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchedEvaluationCount(String searchString) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(ce) FROM ComplianceEvaluation ce WHERE ce.sampleId LIKE :searchString OR ce.standardId LIKE :searchString";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("searchString", "%" + searchString + "%");
            return query.getSingleResult().intValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getTotalSearchedEvaluationCount()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> searchEvaluations(String sampleId, String standardId, EvaluationStatus status,
            Date startDate, Date endDate) throws LIMSRuntimeException {
        try {
            StringBuilder hqlBuilder = new StringBuilder("FROM ComplianceEvaluation ce WHERE 1=1");

            if (sampleId != null) {
                hqlBuilder.append(" AND ce.sampleId LIKE :sampleId");
            }
            if (standardId != null) {
                hqlBuilder.append(" AND ce.standardId = :standardId");
            }
            if (status != null) {
                hqlBuilder.append(" AND ce.status = :status");
            }
            if (startDate != null) {
                hqlBuilder.append(" AND ce.evaluatedDate >= :startDate");
            }
            if (endDate != null) {
                hqlBuilder.append(" AND ce.evaluatedDate <= :endDate");
            }

            hqlBuilder.append(" ORDER BY ce.evaluatedDate DESC");

            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hqlBuilder.toString(), ComplianceEvaluation.class);

            if (sampleId != null) {
                query.setParameter("sampleId", "%" + sampleId + "%");
            }
            if (standardId != null) {
                query.setParameter("standardId", standardId);
            }
            if (status != null) {
                query.setParameter("status", status);
            }
            if (startDate != null) {
                query.setParameter("startDate", startDate);
            }
            if (endDate != null) {
                query.setParameter("endDate", endDate);
            }

            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation searchEvaluations()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsForExport() throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationsForExport()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getEvaluationStatistics() throws LIMSRuntimeException {
        try {
            String hql = "SELECT ce.status, COUNT(ce) FROM ComplianceEvaluation ce GROUP BY ce.status";
            TypedQuery<Object[]> query = entityManager.createQuery(hql, Object[].class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationStatistics()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Double getComplianceRate(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(ce) FROM ComplianceEvaluation ce WHERE ce.standardId = :standardId";
            TypedQuery<Long> totalQuery = entityManager.createQuery(hql, Long.class);
            totalQuery.setParameter("standardId", standardId);
            Long totalEvaluations = totalQuery.getSingleResult();

            if (totalEvaluations == 0) {
                return 0.0;
            }

            // This is a simplified calculation - in reality would check actual compliance results
            hql = "SELECT COUNT(ce) FROM ComplianceEvaluation ce WHERE ce.standardId = :standardId AND ce.status = :status";
            TypedQuery<Long> compliantQuery = entityManager.createQuery(hql, Long.class);
            compliantQuery.setParameter("standardId", standardId);
            compliantQuery.setParameter("status", EvaluationStatus.COMPLETED);
            Long compliantEvaluations = compliantQuery.getSingleResult();

            return (compliantEvaluations.doubleValue() / totalEvaluations.doubleValue()) * 100.0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getComplianceRate()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getComplianceTrend(String standardId, Date startDate, Date endDate) throws LIMSRuntimeException {
        try {
            String hql = "SELECT DATE(ce.evaluatedDate), ce.status, COUNT(ce) FROM ComplianceEvaluation ce " +
                        "WHERE ce.standardId = :standardId AND ce.evaluatedDate >= :startDate AND ce.evaluatedDate <= :endDate " +
                        "GROUP BY DATE(ce.evaluatedDate), ce.status ORDER BY DATE(ce.evaluatedDate)";
            TypedQuery<Object[]> query = entityManager.createQuery(hql, Object[].class);
            query.setParameter("standardId", standardId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getComplianceTrend()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsByComplianceStandardFhirId(String standardFhirId) throws LIMSRuntimeException {
        try {
            // This would join to ComplianceStandard table to find by FHIR ID
            String hql = "FROM ComplianceEvaluation ce JOIN ComplianceStandard cs ON ce.standardId = cs.id WHERE cs.fhirUuid = :fhirUuid ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("fhirUuid", UUID.fromString(standardFhirId));
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationsByComplianceStandardFhirId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsBySampleIdentifier(String sampleIdentifier) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce WHERE ce.sampleId LIKE :sampleIdentifier ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("sampleIdentifier", "%" + sampleIdentifier + "%");
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationsBySampleIdentifier()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsByDateRange(String standardId, Date startDate, Date endDate) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceEvaluation ce WHERE ce.standardId = :standardId AND ce.evaluatedDate >= :startDate AND ce.evaluatedDate <= :endDate ORDER BY ce.evaluatedDate DESC";
            TypedQuery<ComplianceEvaluation> query = entityManager.createQuery(hql, ComplianceEvaluation.class);
            query.setParameter("standardId", standardId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceEvaluation getEvaluationsByDateRange()", e);
        }
    }
}