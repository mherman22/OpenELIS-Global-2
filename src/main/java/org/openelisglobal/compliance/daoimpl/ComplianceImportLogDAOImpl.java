package org.openelisglobal.compliance.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.ComplianceImportLogDAO;
import org.openelisglobal.compliance.valueholder.ComplianceImportLog;
import org.openelisglobal.compliance.valueholder.ComplianceImportStatus;
import org.openelisglobal.compliance.valueholder.ComplianceImportType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for ComplianceImportLog entity.
 *
 * Follows OpenELIS DAO patterns:
 * - Extends BaseDAOImpl<ComplianceImportLog, String>
 * - Uses Hibernate Session for database operations
 * - Proper error handling with LIMSRuntimeException
 * - @Transactional annotations for transaction boundaries
 */
@Component
@Transactional
public class ComplianceImportLogDAOImpl extends BaseDAOImpl<ComplianceImportLog, String>
        implements ComplianceImportLogDAO {

    public ComplianceImportLogDAOImpl() {
        super(ComplianceImportLog.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(ComplianceImportLog complianceImportLog) throws LIMSRuntimeException {
        try {
            ComplianceImportLog data = entityManager.unwrap(Session.class)
                .get(ComplianceImportLog.class, complianceImportLog.getId());
            if (data != null) {
                PropertyUtils.copyProperties(complianceImportLog, data);
            } else {
                complianceImportLog.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByImportType(ComplianceImportType importType) throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE cil.importType = :importType ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("importType", importType);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getLogsByImportType()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByImportStatus(ComplianceImportStatus importStatus) throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE cil.importStatus = :importStatus ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("importStatus", importStatus);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getLogsByImportStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByInitiatedBy(String initiatedBy) throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE cil.initiatedBy = :initiatedBy ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("initiatedBy", initiatedBy);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getLogsByInitiatedBy()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate)
            throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE cil.importStartTime >= :startDate " +
                        "AND cil.importStartTime <= :endDate ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getLogsByDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByCorrelationId(String correlationId) throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE cil.correlationId = :correlationId ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("correlationId", correlationId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getLogsByCorrelationId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByFileChecksum(String fileChecksum) throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE cil.fileChecksum = :fileChecksum ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("fileChecksum", fileChecksum);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getLogsByFileChecksum()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getActiveImportOperations() throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE cil.importStatus IN ('STARTED', 'IN_PROGRESS', 'QUEUED', 'VALIDATING') " +
                        "ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getActiveImportOperations()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getRecentFailedImports(int hours) throws LIMSRuntimeException {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
            String sql = "FROM ComplianceImportLog cil WHERE cil.importStatus = 'FAILED' " +
                        "AND cil.importStartTime >= :cutoffTime ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("cutoffTime", cutoffTime);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getRecentFailedImports()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getRecentSuccessfulImports(int hours) throws LIMSRuntimeException {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
            String sql = "FROM ComplianceImportLog cil WHERE cil.importStatus IN ('COMPLETED', 'COMPLETED_WITH_WARNINGS') " +
                        "AND cil.importStartTime >= :cutoffTime ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("cutoffTime", cutoffTime);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getRecentSuccessfulImports()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getPageOfImportLogs(int startingRecNo, int pageSize) throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setFirstResult(startingRecNo);
            query.setMaxResults(pageSize);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getPageOfImportLogs()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalImportLogCount() throws LIMSRuntimeException {
        try {
            String sql = "SELECT COUNT(*) FROM ComplianceImportLog";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getTotalImportLogCount()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> searchLogsBySource(String sourcePattern, int startingRecNo, int pageSize)
            throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE LOWER(cil.importSource) LIKE LOWER(:pattern) " +
                        "ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("pattern", "%" + sourcePattern + "%");
            query.setFirstResult(startingRecNo);
            query.setMaxResults(pageSize);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog searchLogsBySource()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchResultCount(String sourcePattern) throws LIMSRuntimeException {
        try {
            String sql = "SELECT COUNT(*) FROM ComplianceImportLog cil WHERE LOWER(cil.importSource) LIKE LOWER(:pattern)";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("pattern", "%" + sourcePattern + "%");
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getTotalSearchResultCount()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Object[] getImportStatistics(LocalDateTime startDate, LocalDateTime endDate) throws LIMSRuntimeException {
        try {
            String sql = "SELECT " +
                        "COUNT(*) as totalCount, " +
                        "SUM(CASE WHEN cil.importStatus IN ('COMPLETED', 'COMPLETED_WITH_WARNINGS') THEN 1 ELSE 0 END) as successCount, " +
                        "SUM(CASE WHEN cil.importStatus = 'FAILED' THEN 1 ELSE 0 END) as failureCount, " +
                        "AVG(CASE WHEN cil.importEndTime IS NOT NULL THEN " +
                        "EXTRACT(EPOCH FROM (cil.importEndTime - cil.importStartTime)) ELSE NULL END) as averageDuration " +
                        "FROM ComplianceImportLog cil WHERE cil.importStartTime >= :startDate " +
                        "AND cil.importStartTime <= :endDate";
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(sql, Object[].class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getImportStatistics()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Object[] getImportStatisticsByType(ComplianceImportType importType, LocalDateTime startDate, LocalDateTime endDate)
            throws LIMSRuntimeException {
        try {
            String sql = "SELECT " +
                        "COUNT(*) as totalCount, " +
                        "SUM(CASE WHEN cil.importStatus IN ('COMPLETED', 'COMPLETED_WITH_WARNINGS') THEN 1 ELSE 0 END) as successCount, " +
                        "SUM(CASE WHEN cil.importStatus = 'FAILED' THEN 1 ELSE 0 END) as failureCount, " +
                        "AVG(CASE WHEN cil.importEndTime IS NOT NULL THEN " +
                        "EXTRACT(EPOCH FROM (cil.importEndTime - cil.importStartTime)) ELSE NULL END) as averageDuration " +
                        "FROM ComplianceImportLog cil WHERE cil.importType = :importType " +
                        "AND cil.importStartTime >= :startDate AND cil.importStartTime <= :endDate";
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(sql, Object[].class);
            query.setParameter("importType", importType);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getImportStatisticsByType()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getPageOfUserImportLogs(String initiatedBy, int startingRecNo, int pageSize)
            throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE cil.initiatedBy = :initiatedBy ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("initiatedBy", initiatedBy);
            query.setFirstResult(startingRecNo);
            query.setMaxResults(pageSize);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getPageOfUserImportLogs()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalUserImportLogCount(String initiatedBy) throws LIMSRuntimeException {
        try {
            String sql = "SELECT COUNT(*) FROM ComplianceImportLog cil WHERE cil.initiatedBy = :initiatedBy";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("initiatedBy", initiatedBy);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getTotalUserImportLogCount()", e);
        }
    }

    @Override
    @Transactional
    public Integer deleteOldImportLogs(int olderThanDays) throws LIMSRuntimeException {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
            String sql = "DELETE FROM ComplianceImportLog cil WHERE cil.importStartTime < :cutoffDate";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("cutoffDate", cutoffDate);
            return query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog deleteOldImportLogs()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getRecentImportLogs(int limit) throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setMaxResults(limit);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getRecentImportLogs()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean importLogExistsForChecksum(String fileChecksum) throws LIMSRuntimeException {
        try {
            String sql = "SELECT COUNT(*) FROM ComplianceImportLog cil WHERE cil.fileChecksum = :fileChecksum";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("fileChecksum", fileChecksum);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog importLogExistsForChecksum()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsWithErrorText(String errorText) throws LIMSRuntimeException {
        try {
            String sql = "FROM ComplianceImportLog cil WHERE LOWER(cil.errorDetails) LIKE LOWER(:errorText) " +
                        "ORDER BY cil.importStartTime DESC";
            Query<ComplianceImportLog> query = entityManager.unwrap(Session.class)
                .createQuery(sql, ComplianceImportLog.class);
            query.setParameter("errorText", "%" + errorText + "%");
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceImportLog getLogsWithErrorText()", e);
        }
    }
}