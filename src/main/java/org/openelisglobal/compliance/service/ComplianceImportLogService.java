package org.openelisglobal.compliance.service;

import java.time.LocalDateTime;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.ComplianceImportLog;
import org.openelisglobal.compliance.valueholder.ComplianceImportStatus;
import org.openelisglobal.compliance.valueholder.ComplianceImportType;

/**
 * Service interface for ComplianceImportLog operations.
 *
 * Provides business logic for compliance import audit trail functionality,
 * including logging, monitoring, and reporting of compliance standard import
 * operations.
 */
public interface ComplianceImportLogService extends BaseObjectService<ComplianceImportLog, String> {

    void getData(ComplianceImportLog complianceImportLog);

    /**
     * Start a new import operation log
     *
     * @param importType   Type of import operation
     * @param importSource Source of import (file path, URL, etc.)
     * @param initiatedBy  User ID who initiated the import
     * @return Created import log
     */
    ComplianceImportLog startImportOperation(ComplianceImportType importType, String importSource, String initiatedBy);

    /**
     * Start a new import operation log with additional details
     *
     * @param importType   Type of import operation
     * @param importSource Source of import
     * @param fileName     Name of the imported file
     * @param fileSize     Size of the imported file
     * @param fileChecksum Checksum of the imported file
     * @param initiatedBy  User ID who initiated the import
     * @return Created import log
     */
    ComplianceImportLog startImportOperation(ComplianceImportType importType, String importSource, String fileName,
            Long fileSize, String fileChecksum, String initiatedBy);

    /**
     * Complete an import operation successfully
     *
     * @param importLogId       Import log ID
     * @param recordsProcessed  Total records processed
     * @param recordsSuccessful Records successfully imported
     * @param recordsFailed     Records that failed to import
     * @param recordsSkipped    Records that were skipped
     * @param summary           Summary message
     */
    void completeImportOperation(String importLogId, int recordsProcessed, int recordsSuccessful, int recordsFailed,
            int recordsSkipped, String summary);

    /**
     * Mark an import operation as failed
     *
     * @param importLogId       Import log ID
     * @param recordsProcessed  Total records processed before failure
     * @param recordsSuccessful Records successfully imported before failure
     * @param recordsFailed     Records that failed to import
     * @param errorDetails      Detailed error message
     */
    void markImportOperationFailed(String importLogId, int recordsProcessed, int recordsSuccessful, int recordsFailed,
            String errorDetails);

    /**
     * Complete an import operation with warnings
     *
     * @param importLogId       Import log ID
     * @param recordsProcessed  Total records processed
     * @param recordsSuccessful Records successfully imported
     * @param recordsFailed     Records that failed to import
     * @param recordsSkipped    Records that were skipped
     * @param summary           Summary message
     * @param warnings          Warning details
     */
    void completeImportOperationWithWarnings(String importLogId, int recordsProcessed, int recordsSuccessful,
            int recordsFailed, int recordsSkipped, String summary, String warnings);

    /**
     * Update import operation progress
     *
     * @param importLogId       Import log ID
     * @param recordsProcessed  Total records processed so far
     * @param recordsSuccessful Records successfully imported so far
     * @param recordsFailed     Records that failed to import so far
     * @param status            Current import status
     */
    void updateImportProgress(String importLogId, int recordsProcessed, int recordsSuccessful, int recordsFailed,
            ComplianceImportStatus status);

    /**
     * Get import logs by import type
     *
     * @param importType The type of import
     * @return List of matching import logs
     */
    List<ComplianceImportLog> getLogsByImportType(ComplianceImportType importType);

    /**
     * Get import logs by import status
     *
     * @param importStatus The import status
     * @return List of matching import logs
     */
    List<ComplianceImportLog> getLogsByImportStatus(ComplianceImportStatus importStatus);

    /**
     * Get import logs by user who initiated the import
     *
     * @param initiatedBy User ID who initiated the import
     * @return List of import logs initiated by the specified user
     */
    List<ComplianceImportLog> getLogsByInitiatedBy(String initiatedBy);

    /**
     * Get import logs within a date range
     *
     * @param startDate Start date/time (inclusive)
     * @param endDate   End date/time (inclusive)
     * @return List of import logs within the specified date range
     */
    List<ComplianceImportLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get currently active (running) import operations
     *
     * @return List of import logs with active status
     */
    List<ComplianceImportLog> getActiveImportOperations();

    /**
     * Get failed import operations within the last N hours
     *
     * @param hours Number of hours to look back
     * @return List of failed import logs within the specified time
     */
    List<ComplianceImportLog> getRecentFailedImports(int hours);

    /**
     * Get successful import operations within the last N hours
     *
     * @param hours Number of hours to look back
     * @return List of successful import logs within the specified time
     */
    List<ComplianceImportLog> getRecentSuccessfulImports(int hours);

    /**
     * Get import logs with pagination
     *
     * @param startingRecNo Starting record number (0-based)
     * @param pageSize      Number of records per page
     * @return List of import logs for the specified page
     */
    List<ComplianceImportLog> getPageOfImportLogs(int startingRecNo, int pageSize);

    /**
     * Get total count of import logs
     *
     * @return Total number of import logs
     */
    Integer getTotalImportLogCount();

    /**
     * Search import logs by source pattern with pagination
     *
     * @param sourcePattern Pattern to search in import source field
     * @param startingRecNo Starting record number (0-based)
     * @param pageSize      Number of records per page
     * @return List of matching import logs
     */
    List<ComplianceImportLog> searchLogsBySource(String sourcePattern, int startingRecNo, int pageSize);

    /**
     * Get total count of import logs matching source pattern
     *
     * @param sourcePattern Pattern to search in import source field
     * @return Count of matching import logs
     */
    Integer getTotalSearchResultCount(String sourcePattern);

    /**
     * Get import statistics for a specific date range
     *
     * @param startDate Start date/time (inclusive)
     * @param endDate   End date/time (inclusive)
     * @return Import statistics [totalCount, successCount, failureCount,
     *         averageDuration]
     */
    Object[] getImportStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get import statistics by type for a specific date range
     *
     * @param importType The import type
     * @param startDate  Start date/time (inclusive)
     * @param endDate    End date/time (inclusive)
     * @return Import statistics [totalCount, successCount, failureCount,
     *         averageDuration]
     */
    Object[] getImportStatisticsByType(ComplianceImportType importType, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get recent import logs (newest first)
     *
     * @param limit Maximum number of records to return
     * @return List of recent import logs
     */
    List<ComplianceImportLog> getRecentImportLogs(int limit);

    /**
     * Check if an import with the same file checksum already exists
     *
     * @param fileChecksum File checksum to check
     * @return true if a log with the same checksum exists
     */
    boolean importLogExistsForChecksum(String fileChecksum);

    /**
     * Delete old import logs older than specified days
     *
     * @param olderThanDays Number of days - logs older than this will be deleted
     * @return Number of records deleted
     */
    Integer deleteOldImportLogs(int olderThanDays);

    /**
     * Get import logs with error details containing specific text
     *
     * @param errorText Text to search for in error details
     * @return List of import logs with matching error text
     */
    List<ComplianceImportLog> getLogsWithErrorText(String errorText);

    /**
     * Cancel a running import operation
     *
     * @param importLogId Import log ID
     * @param reason      Reason for cancellation
     */
    void cancelImportOperation(String importLogId, String reason);

    /**
     * Get import logs by correlation ID
     *
     * @param correlationId Correlation ID to search for
     * @return List of import logs with matching correlation ID
     */
    List<ComplianceImportLog> getLogsByCorrelationId(String correlationId);

    /**
     * Generate correlation ID for related import operations
     *
     * @return Generated correlation ID
     */
    String generateCorrelationId();

    /**
     * Validate if a new import can be started
     *
     * @param fileChecksum File checksum (optional, for duplicate detection)
     * @return true if import can be started
     */
    boolean canStartImport(String fileChecksum);
}