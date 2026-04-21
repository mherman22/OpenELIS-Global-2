package org.openelisglobal.compliance.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.dao.ComplianceImportLogDAO;
import org.openelisglobal.compliance.valueholder.ComplianceImportLog;
import org.openelisglobal.compliance.valueholder.ComplianceImportStatus;
import org.openelisglobal.compliance.valueholder.ComplianceImportType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for ComplianceImportLog operations.
 *
 * Follows OpenELIS service patterns:
 * - Extends AuditableBaseObjectServiceImpl for standard CRUD operations
 * - Uses @Service annotation for Spring component scanning
 * - @Transactional boundaries at service level
 * - Implements business logic for import audit trail
 */
@Service
public class ComplianceImportLogServiceImpl extends AuditableBaseObjectServiceImpl<ComplianceImportLog, String>
        implements ComplianceImportLogService {

    @Autowired
    protected ComplianceImportLogDAO baseObjectDAO;

    ComplianceImportLogServiceImpl() {
        super(ComplianceImportLog.class);
    }

    @Override
    protected ComplianceImportLogDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(ComplianceImportLog complianceImportLog) {
        getBaseObjectDAO().getData(complianceImportLog);
    }

    @Override
    @Transactional
    public ComplianceImportLog startImportOperation(ComplianceImportType importType, String importSource, String initiatedBy) {
        ComplianceImportLog importLog = new ComplianceImportLog(importType, importSource, initiatedBy);
        importLog.setSysUserId(initiatedBy);
        return save(importLog);
    }

    @Override
    @Transactional
    public ComplianceImportLog startImportOperation(ComplianceImportType importType, String importSource,
                                                  String fileName, Long fileSize, String fileChecksum, String initiatedBy) {
        ComplianceImportLog importLog = new ComplianceImportLog(importType, importSource, initiatedBy);
        importLog.setFileName(fileName);
        importLog.setFileSize(fileSize);
        importLog.setFileChecksum(fileChecksum);
        importLog.setSysUserId(initiatedBy);
        return save(importLog);
    }

    @Override
    @Transactional
    public void completeImportOperation(String importLogId, int recordsProcessed, int recordsSuccessful,
                                      int recordsFailed, int recordsSkipped, String summary) {
        ComplianceImportLog importLog = get(importLogId);
        if (importLog != null) {
            importLog.setRecordsProcessed(recordsProcessed);
            importLog.setRecordsSuccessful(recordsSuccessful);
            importLog.setRecordsFailed(recordsFailed);
            importLog.setRecordsSkipped(recordsSkipped);
            importLog.setImportSummary(summary);
            importLog.markCompleted();
            save(importLog);
        }
    }

    @Override
    @Transactional
    public void markImportOperationFailed(String importLogId, int recordsProcessed, int recordsSuccessful,
                                        int recordsFailed, String errorDetails) {
        ComplianceImportLog importLog = get(importLogId);
        if (importLog != null) {
            importLog.setRecordsProcessed(recordsProcessed);
            importLog.setRecordsSuccessful(recordsSuccessful);
            importLog.setRecordsFailed(recordsFailed);
            importLog.markFailed(errorDetails);
            save(importLog);
        }
    }

    @Override
    @Transactional
    public void completeImportOperationWithWarnings(String importLogId, int recordsProcessed, int recordsSuccessful,
                                                   int recordsFailed, int recordsSkipped, String summary, String warnings) {
        ComplianceImportLog importLog = get(importLogId);
        if (importLog != null) {
            importLog.setRecordsProcessed(recordsProcessed);
            importLog.setRecordsSuccessful(recordsSuccessful);
            importLog.setRecordsFailed(recordsFailed);
            importLog.setRecordsSkipped(recordsSkipped);
            importLog.setImportSummary(summary);
            importLog.markCompletedWithWarnings(warnings);
            save(importLog);
        }
    }

    @Override
    @Transactional
    public void updateImportProgress(String importLogId, int recordsProcessed, int recordsSuccessful,
                                   int recordsFailed, ComplianceImportStatus status) {
        ComplianceImportLog importLog = get(importLogId);
        if (importLog != null) {
            importLog.setRecordsProcessed(recordsProcessed);
            importLog.setRecordsSuccessful(recordsSuccessful);
            importLog.setRecordsFailed(recordsFailed);
            importLog.setImportStatus(status);
            save(importLog);
        }
    }

    @Override
    @Transactional
    public void cancelImportOperation(String importLogId, String reason) {
        ComplianceImportLog importLog = get(importLogId);
        if (importLog != null) {
            importLog.setImportStatus(ComplianceImportStatus.CANCELLED);
            importLog.setImportEndTime(LocalDateTime.now());
            importLog.setErrorDetails(reason);
            save(importLog);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByImportType(ComplianceImportType importType) {
        return getBaseObjectDAO().getLogsByImportType(importType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByImportStatus(ComplianceImportStatus importStatus) {
        return getBaseObjectDAO().getLogsByImportStatus(importStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByInitiatedBy(String initiatedBy) {
        return getBaseObjectDAO().getLogsByInitiatedBy(initiatedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return getBaseObjectDAO().getLogsByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsByCorrelationId(String correlationId) {
        return getBaseObjectDAO().getLogsByCorrelationId(correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getActiveImportOperations() {
        return getBaseObjectDAO().getActiveImportOperations();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getRecentFailedImports(int hours) {
        return getBaseObjectDAO().getRecentFailedImports(hours);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getRecentSuccessfulImports(int hours) {
        return getBaseObjectDAO().getRecentSuccessfulImports(hours);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getPageOfImportLogs(int startingRecNo, int pageSize) {
        return getBaseObjectDAO().getPageOfImportLogs(startingRecNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalImportLogCount() {
        return getBaseObjectDAO().getTotalImportLogCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> searchLogsBySource(String sourcePattern, int startingRecNo, int pageSize) {
        return getBaseObjectDAO().searchLogsBySource(sourcePattern, startingRecNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchResultCount(String sourcePattern) {
        return getBaseObjectDAO().getTotalSearchResultCount(sourcePattern);
    }

    @Override
    @Transactional(readOnly = true)
    public Object[] getImportStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        return getBaseObjectDAO().getImportStatistics(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Object[] getImportStatisticsByType(ComplianceImportType importType, LocalDateTime startDate, LocalDateTime endDate) {
        return getBaseObjectDAO().getImportStatisticsByType(importType, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getRecentImportLogs(int limit) {
        return getBaseObjectDAO().getRecentImportLogs(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean importLogExistsForChecksum(String fileChecksum) {
        if (fileChecksum == null || fileChecksum.trim().isEmpty()) {
            return false;
        }
        return getBaseObjectDAO().importLogExistsForChecksum(fileChecksum);
    }

    @Override
    @Transactional
    public Integer deleteOldImportLogs(int olderThanDays) {
        return getBaseObjectDAO().deleteOldImportLogs(olderThanDays);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceImportLog> getLogsWithErrorText(String errorText) {
        return getBaseObjectDAO().getLogsWithErrorText(errorText);
    }

    @Override
    public String generateCorrelationId() {
        return "COMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canStartImport(String fileChecksum) {
        // Check if there are any active import operations
        List<ComplianceImportLog> activeImports = getActiveImportOperations();
        if (!activeImports.isEmpty()) {
            // Could implement more sophisticated logic here, e.g., allow concurrent imports
            // For now, allow concurrent imports but log the information
            org.openelisglobal.common.log.LogEvent.logInfo(
                "ComplianceImportLogService", "canStartImport",
                "Starting import while " + activeImports.size() + " other imports are active");
        }

        // Check for duplicate file if checksum is provided
        if (fileChecksum != null && !fileChecksum.trim().isEmpty()) {
            if (importLogExistsForChecksum(fileChecksum)) {
                org.openelisglobal.common.log.LogEvent.logWarn(
                    "ComplianceImportLogService", "canStartImport",
                    "Import attempt with duplicate file checksum: " + fileChecksum);
                return false;
            }
        }

        return true;
    }
}