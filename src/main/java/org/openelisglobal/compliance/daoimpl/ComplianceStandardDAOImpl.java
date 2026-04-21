package org.openelisglobal.compliance.daoimpl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.TypedQuery;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.compliance.dao.ComplianceStandardDAO;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandardStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ComplianceStandardDAO following OpenELIS patterns.
 *
 * Follows constitutional requirements: - Uses @Transactional annotations -
 * Implements proper exception handling with logging - Uses HQL queries
 * exclusively (no native SQL) - Supports pagination and search operations
 */
@Component
@Transactional
public class ComplianceStandardDAOImpl extends BaseDAOImpl<ComplianceStandard, String>
        implements ComplianceStandardDAO {

    public ComplianceStandardDAOImpl() {
        super(ComplianceStandard.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getAllStandards() throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getAllStandards()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getStandardByFhirId(UUID fhirUuid) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.fhirUuid = :fhirUuid";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("fhirUuid", fhirUuid);
            List<ComplianceStandard> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardByFhirId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getStandardByNaturalKey(String issuingBody, String regulationNumber, String version)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.issuingBody = :issuingBody "
                    + "AND cs.regulationNumber = :regulationNumber AND cs.version = :version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("issuingBody", issuingBody);
            query.setParameter("regulationNumber", regulationNumber);
            query.setParameter("version", version);
            List<ComplianceStandard> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardByNaturalKey()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsByStatus(ComplianceStandardStatus status) throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.status = :status "
                    + "ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("status", status);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardsByStatus()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getActiveStandardsBySampleType(String sampleType) throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.status = :status "
                    + "AND (cs.applicableSampleTypes IS NULL OR cs.applicableSampleTypes LIKE :sampleType) "
                    + "ORDER BY cs.name";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("status", ComplianceStandardStatus.ACTIVE);
            query.setParameter("sampleType", "%" + sampleType + "%");
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getActiveStandardsBySampleType()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsByCountryRegion(String countryRegion) throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs WHERE LOWER(cs.countryRegion) LIKE LOWER(:countryRegion) "
                    + "ORDER BY cs.name";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("countryRegion", "%" + countryRegion + "%");
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardsByCountryRegion()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsByIssuingBody(String issuingBody) throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs WHERE LOWER(cs.issuingBody) LIKE LOWER(:issuingBody) "
                    + "ORDER BY cs.regulationNumber, cs.version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("issuingBody", "%" + issuingBody + "%");
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardsByIssuingBody()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsEffectiveInRange(LocalDate startDate, LocalDate endDate)
            throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.effectiveDate >= :startDate "
                    + "AND (cs.expiryDate IS NULL OR cs.expiryDate <= :endDate) " + "ORDER BY cs.effectiveDate";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardsEffectiveInRange()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getStandardWithParameterGroups(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT DISTINCT cs FROM ComplianceStandard cs " + "LEFT JOIN FETCH cs.parameterGroups pg "
                    + "WHERE cs.id = :standardId " + "ORDER BY pg.sortOrder";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("standardId", standardId);
            List<ComplianceStandard> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardWithParameterGroups()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getStandardWithFullHierarchy(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT DISTINCT cs FROM ComplianceStandard cs " + "LEFT JOIN FETCH cs.parameterGroups pg "
                    + "LEFT JOIN FETCH pg.complianceThresholds ct " + "WHERE cs.id = :standardId "
                    + "ORDER BY pg.sortOrder, ct.sortOrder";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("standardId", standardId);
            List<ComplianceStandard> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardWithFullHierarchy()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsWithGroupCounts() throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "SELECT DISTINCT cs FROM ComplianceStandard cs " + "LEFT JOIN FETCH cs.parameterGroups "
                    + "ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardsWithGroupCounts()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getPageOfStandards(int startingRecNo) throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String hql = "FROM ComplianceStandard cs ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getPageOfStandards()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getPagesOfSearchedStandards(int startingRecNo, String searchString)
            throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        String wildCard = "*";
        String newSearchStr;
        String hql;

        try {
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);
            int wCdPosition = searchString.indexOf(wildCard);

            if (wCdPosition == -1) { // no wild card - exact match
                newSearchStr = searchString.toLowerCase().trim();
                hql = "FROM ComplianceStandard cs WHERE LOWER(cs.name) = :param "
                        + "OR LOWER(cs.issuingBody) = :param OR LOWER(cs.regulationNumber) = :param "
                        + "ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            } else {
                newSearchStr = searchString.replace(wildCard, "%").toLowerCase().trim();
                hql = "FROM ComplianceStandard cs WHERE LOWER(cs.name) LIKE :param "
                        + "OR LOWER(cs.issuingBody) LIKE :param OR LOWER(cs.regulationNumber) LIKE :param "
                        + "ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            }
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("param", newSearchStr);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getPagesOfSearchedStandards()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalStandardCount() throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(cs) FROM ComplianceStandard cs";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            return query.getSingleResult().intValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getTotalStandardCount()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchedStandardCount(String searchString) throws LIMSRuntimeException {
        String wildCard = "*";
        String newSearchStr;
        String hql;

        try {
            int wCdPosition = searchString.indexOf(wildCard);

            if (wCdPosition == -1) { // no wild card - exact match
                newSearchStr = searchString.toLowerCase().trim();
                hql = "SELECT COUNT(cs) FROM ComplianceStandard cs WHERE LOWER(cs.name) = :param "
                        + "OR LOWER(cs.issuingBody) = :param OR LOWER(cs.regulationNumber) = :param";
            } else {
                newSearchStr = searchString.replace(wildCard, "%").toLowerCase().trim();
                hql = "SELECT COUNT(cs) FROM ComplianceStandard cs WHERE LOWER(cs.name) LIKE :param "
                        + "OR LOWER(cs.issuingBody) LIKE :param OR LOWER(cs.regulationNumber) LIKE :param";
            }

            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("param", newSearchStr);
            return query.getSingleResult().intValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getTotalSearchedStandardCount()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean duplicateStandardExists(ComplianceStandard standard) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(cs) FROM ComplianceStandard cs WHERE cs.issuingBody = :issuingBody "
                    + "AND cs.regulationNumber = :regulationNumber AND cs.version = :version";

            // If updating existing standard, exclude current record
            if (standard.getId() != null) {
                hql += " AND cs.id != :currentId";
            }

            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("issuingBody", standard.getIssuingBody());
            query.setParameter("regulationNumber", standard.getRegulationNumber());
            query.setParameter("version", standard.getVersion());

            if (standard.getId() != null) {
                query.setParameter("currentId", standard.getId());
            }

            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard duplicateStandardExists()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean standardHasEvaluations(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(ce) FROM ComplianceEvaluation ce WHERE ce.standard.id = :standardId";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("standardId", standardId);
            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard standardHasEvaluations()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean standardHasParameterGroups(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(pg) FROM ParameterGroup pg WHERE pg.standard.id = :standardId";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("standardId", standardId);
            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard standardHasParameterGroups()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getSupersededStandards(String supersededByStandardId) throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.supersededByStandard.id = :supersededByStandardId "
                    + "ORDER BY cs.effectiveDate DESC";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("supersededByStandardId", supersededByStandardId);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getSupersededStandards()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getVersionHistory(String issuingBody, String regulationNumber)
            throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.issuingBody = :issuingBody "
                    + "AND cs.regulationNumber = :regulationNumber "
                    + "ORDER BY cs.effectiveDate DESC, cs.version DESC";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("issuingBody", issuingBody);
            query.setParameter("regulationNumber", regulationNumber);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getVersionHistory()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> searchStandards(String name, String issuingBody, String regulationNumber,
            ComplianceStandardStatus status, String countryRegion, String sampleType) throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            StringBuilder hql = new StringBuilder("FROM ComplianceStandard cs WHERE 1=1");

            if (name != null && !name.trim().isEmpty()) {
                hql.append(" AND LOWER(cs.name) LIKE :name");
            }
            if (issuingBody != null && !issuingBody.trim().isEmpty()) {
                hql.append(" AND LOWER(cs.issuingBody) LIKE :issuingBody");
            }
            if (regulationNumber != null && !regulationNumber.trim().isEmpty()) {
                hql.append(" AND LOWER(cs.regulationNumber) LIKE :regulationNumber");
            }
            if (status != null) {
                hql.append(" AND cs.status = :status");
            }
            if (countryRegion != null && !countryRegion.trim().isEmpty()) {
                hql.append(" AND LOWER(cs.countryRegion) LIKE :countryRegion");
            }
            if (sampleType != null && !sampleType.trim().isEmpty()) {
                hql.append(" AND (cs.applicableSampleTypes IS NULL OR cs.applicableSampleTypes LIKE :sampleType)");
            }

            hql.append(" ORDER BY cs.issuingBody, cs.regulationNumber, cs.version");

            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql.toString(), ComplianceStandard.class);

            if (name != null && !name.trim().isEmpty()) {
                query.setParameter("name", "%" + name.toLowerCase() + "%");
            }
            if (issuingBody != null && !issuingBody.trim().isEmpty()) {
                query.setParameter("issuingBody", "%" + issuingBody.toLowerCase() + "%");
            }
            if (regulationNumber != null && !regulationNumber.trim().isEmpty()) {
                query.setParameter("regulationNumber", "%" + regulationNumber.toLowerCase() + "%");
            }
            if (status != null) {
                query.setParameter("status", status);
            }
            if (countryRegion != null && !countryRegion.trim().isEmpty()) {
                query.setParameter("countryRegion", "%" + countryRegion.toLowerCase() + "%");
            }
            if (sampleType != null && !sampleType.trim().isEmpty()) {
                query.setParameter("sampleType", "%" + sampleType + "%");
            }

            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard searchStandards()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsExpiringWithinDays(int days) throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            LocalDate cutoffDate = LocalDate.now().plusDays(days);
            String hql = "FROM ComplianceStandard cs WHERE cs.expiryDate IS NOT NULL "
                    + "AND cs.expiryDate <= :cutoffDate AND cs.status = :status " + "ORDER BY cs.expiryDate";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("cutoffDate", cutoffDate);
            query.setParameter("status", ComplianceStandardStatus.ACTIVE);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardsExpiringWithinDays()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsForExport() throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "SELECT NEW ComplianceStandard(cs.id, cs.name, cs.issuingBody, cs.regulationNumber, "
                    + "cs.version, cs.effectiveDate, cs.countryRegion, cs.status) FROM ComplianceStandard cs "
                    + "ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardsForExport()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getLatestVersionStandards() throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.id IN "
                    + "(SELECT cs2.id FROM ComplianceStandard cs2 WHERE cs2.issuingBody = cs.issuingBody "
                    + "AND cs2.regulationNumber = cs.regulationNumber "
                    + "AND cs2.effectiveDate = (SELECT MAX(cs3.effectiveDate) FROM ComplianceStandard cs3 "
                    + "WHERE cs3.issuingBody = cs2.issuingBody AND cs3.regulationNumber = cs2.regulationNumber)) "
                    + "ORDER BY cs.issuingBody, cs.regulationNumber";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getLatestVersionStandards()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getSupersedableStandards() throws LIMSRuntimeException {
        List<ComplianceStandard> list;
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.status = :status "
                    + "ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("status", ComplianceStandardStatus.ACTIVE);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getSupersedableStandards()", e);
        }
        return list;
    }

    @Override
    @Transactional
    public void bulkUpdateStatus(List<String> standardIds, ComplianceStandardStatus newStatus, String userId)
            throws LIMSRuntimeException {
        try {
            String hql = "UPDATE ComplianceStandard cs SET cs.status = :newStatus, cs.sysUserId = :userId, "
                    + "cs.lastupdated = CURRENT_TIMESTAMP WHERE cs.id IN :standardIds";
            entityManager.createQuery(hql).setParameter("newStatus", newStatus).setParameter("userId", userId)
                    .setParameter("standardIds", standardIds).executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard bulkUpdateStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getStandardsStatistics() throws LIMSRuntimeException {
        List<Object[]> list;
        try {
            String hql = "SELECT cs.status, COUNT(cs), cs.issuingBody FROM ComplianceStandard cs "
                    + "GROUP BY cs.status, cs.issuingBody ORDER BY cs.issuingBody, cs.status";
            TypedQuery<Object[]> query = entityManager.createQuery(hql, Object[].class);
            list = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardsStatistics()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getByRegulationNumberAndName(String regulationNumber, String name)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.regulationNumber = :regulationNumber AND cs.name = :name";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("regulationNumber", regulationNumber);
            query.setParameter("name", name);
            List<ComplianceStandard> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getByRegulationNumberAndName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getComplianceStandardsByName(String name) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.name LIKE :name";
            List<ComplianceStandard> list = entityManager.createQuery(hql, ComplianceStandard.class)
                    .setParameter("name", "%" + name + "%")
                    .getResultList();
            return list;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getComplianceStandardsByName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getComplianceStandardByRegulationNumber(String regulationNumber) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.regulationNumber = :regulationNumber";
            List<ComplianceStandard> list = entityManager.createQuery(hql, ComplianceStandard.class)
                    .setParameter("regulationNumber", regulationNumber)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getComplianceStandardByRegulationNumber()", e);
        }
    }

    @Override
    @Transactional
    public void supersedseStandard(String standardId, String supersedingStandardId) throws LIMSRuntimeException {
        try {
            String hql = "UPDATE ComplianceStandard cs SET cs.supersededByStandardId = :supersedingId WHERE cs.id = :id";
            entityManager.createQuery(hql)
                    .setParameter("supersedingId", supersedingStandardId)
                    .setParameter("id", standardId)
                    .executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard supersedseStandard()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getVersionForEvaluation(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT cs.version FROM ComplianceStandard cs WHERE cs.id = :id";
            List<String> list = entityManager.createQuery(hql, String.class)
                    .setParameter("id", standardId)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getVersionForEvaluation()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getStandardWithGroups(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceStandard cs LEFT JOIN FETCH cs.parameterGroups WHERE cs.id = :id";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("id", standardId);
            List<ComplianceStandard> list = query.getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardWithGroups()", e);
        }
    }
}