package org.openelisglobal.compliance.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.compliance.valueholder.ComplianceImportLog;
import org.openelisglobal.compliance.valueholder.ComplianceImportStatus;
import org.openelisglobal.compliance.valueholder.ComplianceImportType;

/**
 * DAO interface for ComplianceImportLog entity.
 *
 * Provides data access methods for compliance import audit trail operations,
 * including queries for monitoring, reporting, and administrative purposes.
 */
public interface ComplianceImportLogDAO extends BaseDAO<ComplianceImportLog, String> {

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
     * @param endDate End date/time (inclusive)
     * @return List of import logs within the specified date range
     */
    List<ComplianceImportLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get import logs by correlation ID
     *
     * @param correlationId Correlation ID to search for
     * @return List of import logs with matching correlation ID
     */
    List<ComplianceImportLog> getLogsByCorrelationId(String correlationId);

    /**
     * Get import logs by file checksum
     *
     * @param fileChecksum File checksum to search for
     * @return List of import logs with matching file checksum
     */
    List<ComplianceImportLog> getLogsByFileChecksum(String fileChecksum);

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
     * @param pageSize Number of records per page
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
     * Search import logs by source pattern
     *
     * @param sourcePattern Pattern to search in import source field
     * @param startingRecNo Starting record number (0-based)
     * @param pageSize Number of records per page
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
     * @param endDate End date/time (inclusive)
     * @return Array of statistics [totalCount, successCount, failureCount, averageDuration]
     */
    Object[] getImportStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get import statistics by type for a specific date range
     *
     * @param importType The import type
     * @param startDate Start date/time (inclusive)
     * @param endDate End date/time (inclusive)
     * @return Array of statistics [totalCount, successCount, failureCount, averageDuration]
     */
    Object[] getImportStatisticsByType(ComplianceImportType importType, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get import logs for a specific user with pagination
     *
     * @param initiatedBy User ID who initiated the import
     * @param startingRecNo Starting record number (0-based)
     * @param pageSize Number of records per page
     * @return List of import logs for the specified user and page
     */
    List<ComplianceImportLog> getPageOfUserImportLogs(String initiatedBy, int startingRecNo, int pageSize);

    /**
     * Get total count of import logs for a specific user
     *
     * @param initiatedBy User ID who initiated the import
     * @return Total number of import logs for the specified user
     */
    Integer getTotalUserImportLogCount(String initiatedBy);

    /**
     * Delete old import logs older than specified days
     *
     * @param olderThanDays Number of days - logs older than this will be deleted
     * @return Number of records deleted
     */
    Integer deleteOldImportLogs(int olderThanDays);

    /**
     * Get import logs ordered by start time (newest first)
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
     * Get import logs with error details containing specific text
     *
     * @param errorText Text to search for in error details
     * @return List of import logs with matching error text
     */
    List<ComplianceImportLog> getLogsWithErrorText(String errorText);
}