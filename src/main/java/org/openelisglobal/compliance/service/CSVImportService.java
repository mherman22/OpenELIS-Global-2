package org.openelisglobal.compliance.service;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import org.openelisglobal.compliance.valueholder.ImportResult;

/**
 * Service interface for CSV import operations in compliance module.
 *
 * Provides secure CSV import functionality with validation, progress tracking,
 * and comprehensive error reporting.
 *
 * Constitutional compliance: - Declares @Transactional boundaries at service
 * level - Provides business logic validation methods - Supports all-or-nothing
 * import semantics - Includes security validation patterns
 */
public interface CSVImportService {

    /**
     * Import compliance standards from CSV input stream
     *
     * @param csvStream CSV data input stream
     * @param userId    User ID performing the import
     * @return ImportResult with detailed status and error information
     */
    ImportResult importComplianceStandards(InputStream csvStream, String userId);

    /**
     * Import compliance standards with progress tracking
     *
     * @param csvStream        CSV data input stream
     * @param userId           User ID performing the import
     * @param progressCallback Callback for progress updates (0-100)
     * @return ImportResult with detailed status and error information
     */
    ImportResult importComplianceStandardsWithProgress(InputStream csvStream, String userId,
            Consumer<Integer> progressCallback);

    /**
     * Validate CSV file structure and content without importing
     *
     * @param csvStream CSV data input stream
     * @return ImportResult with validation results only
     */
    ImportResult validateCSVFile(InputStream csvStream);

    /**
     * Generate CSV template with proper headers and example data
     *
     * @return CSV template content as string
     */
    String generateCSVTemplate();

    /**
     * Get supported CSV column headers
     *
     * @return List of supported column names
     */
    List<String> getSupportedHeaders();

    /**
     * Check if import is currently in progress (thread safety)
     *
     * @return true if import operation is running
     */
    boolean isImportInProgress();

    /**
     * Cancel ongoing import operation
     *
     * @return true if cancellation was successful
     */
    boolean cancelImport();

    /**
     * Get import statistics
     *
     * @return Array of import statistics [total imports, successful, failed,
     *         pending]
     */
    int[] getImportStatistics();

    /**
     * Validate file size and format before processing
     *
     * @param fileSize    Size of the uploaded file in bytes
     * @param contentType MIME type of the file
     * @param filename    Original filename
     * @throws IllegalArgumentException if validation fails
     */
    void validateFileProperties(long fileSize, String contentType, String filename);

    /**
     * Security validation for CSV content
     *
     * @param csvContent Raw CSV content
     * @return List of security violations found
     */
    List<String> validateSecurity(String csvContent);
}