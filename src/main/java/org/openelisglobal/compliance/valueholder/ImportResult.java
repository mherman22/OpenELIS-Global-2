package org.openelisglobal.compliance.valueholder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * ImportResult represents the result of a CSV import operation.
 *
 * This class captures comprehensive information about the import process
 * including status, statistics, errors, warnings, and processing details.
 *
 * Follows OpenELIS patterns for result objects with detailed feedback
 * and audit trail capabilities.
 */
public class ImportResult {

    private ImportStatus status;
    private int importedCount;
    private int errorCount;
    private int warningCount;
    private int totalRecords;
    private long processingTimeMs;
    private Date startTime;
    private Date endTime;
    private String importedBy;
    private String filename;
    private long fileSizeBytes;

    private List<ValidationError> validationErrors;
    private List<ValidationError> warnings;
    private List<String> importedIds;
    private String summary;
    private String detailLog;

    // Progress tracking
    private int currentProgress;
    private String currentOperation;
    private Consumer<Integer> progressCallback;

    public ImportResult() {
        this.status = ImportStatus.IN_PROGRESS;
        this.importedCount = 0;
        this.errorCount = 0;
        this.warningCount = 0;
        this.totalRecords = 0;
        this.processingTimeMs = 0L;
        this.startTime = new Date();
        this.validationErrors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.importedIds = new ArrayList<>();
        this.currentProgress = 0;
    }

    public ImportResult(String filename, String importedBy) {
        this();
        this.filename = filename;
        this.importedBy = importedBy;
    }

    // Getters and Setters

    public ImportStatus getStatus() {
        return status;
    }

    public void setStatus(ImportStatus status) {
        this.status = status;
        if (status != null && status.isFinalState() && endTime == null) {
            markAsCompleted();
        }
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = Math.max(0, importedCount);
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = Math.max(0, errorCount);
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = Math.max(0, warningCount);
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = Math.max(0, totalRecords);
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = Math.max(0L, processingTimeMs);
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            this.processingTimeMs = endTime.getTime() - startTime.getTime();
        }
    }

    public String getImportedBy() {
        return importedBy;
    }

    public void setImportedBy(String importedBy) {
        this.importedBy = importedBy;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = Math.max(0L, fileSizeBytes);
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
        updateErrorCount();
    }

    public List<ValidationError> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ValidationError> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        updateWarningCount();
    }

    public List<String> getImportedIds() {
        return importedIds;
    }

    public void setImportedIds(List<String> importedIds) {
        this.importedIds = importedIds != null ? importedIds : new ArrayList<>();
        this.importedCount = this.importedIds.size();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetailLog() {
        return detailLog;
    }

    public void setDetailLog(String detailLog) {
        this.detailLog = detailLog;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = Math.max(0, Math.min(100, currentProgress));
        if (progressCallback != null) {
            progressCallback.accept(this.currentProgress);
        }
    }

    public String getCurrentOperation() {
        return currentOperation;
    }

    public void setCurrentOperation(String currentOperation) {
        this.currentOperation = currentOperation;
    }

    public Consumer<Integer> getProgressCallback() {
        return progressCallback;
    }

    public void setProgressCallback(Consumer<Integer> progressCallback) {
        this.progressCallback = progressCallback;
    }

    // Helper methods

    /**
     * Add a validation error
     */
    public void addValidationError(ValidationError error) {
        if (error != null) {
            if (error.isWarning()) {
                addWarning(error);
            } else {
                if (validationErrors == null) {
                    validationErrors = new ArrayList<>();
                }
                validationErrors.add(error);
                updateErrorCount();
            }
        }
    }

    /**
     * Add a warning
     */
    public void addWarning(ValidationError warning) {
        if (warning != null) {
            if (warnings == null) {
                warnings = new ArrayList<>();
            }
            warnings.add(warning);
            updateWarningCount();
        }
    }

    /**
     * Add an imported record ID
     */
    public void addImportedId(String id) {
        if (id != null) {
            if (importedIds == null) {
                importedIds = new ArrayList<>();
            }
            importedIds.add(id);
            this.importedCount = importedIds.size();
        }
    }

    /**
     * Mark the import as completed
     */
    public void markAsCompleted() {
        if (endTime == null) {
            this.endTime = new Date();
            if (startTime != null) {
                this.processingTimeMs = endTime.getTime() - startTime.getTime();
            }
        }
        this.currentProgress = 100;

        // Determine final status if not already set
        if (status == ImportStatus.IN_PROGRESS) {
            if (errorCount > 0) {
                this.status = ImportStatus.VALIDATION_FAILED;
            } else if (warningCount > 0) {
                this.status = ImportStatus.SUCCESS_WITH_WARNINGS;
            } else {
                this.status = ImportStatus.SUCCESS;
            }
        }

        generateSummary();
    }

    /**
     * Mark the import as failed
     */
    public void markAsFailed(ImportStatus failureStatus, String message) {
        this.status = failureStatus;
        this.endTime = new Date();
        if (startTime != null) {
            this.processingTimeMs = endTime.getTime() - startTime.getTime();
        }

        if (message != null) {
            addValidationError(new ValidationError(message));
        }

        generateSummary();
    }

    /**
     * Update progress with operation description
     */
    public void updateProgress(int progress, String operation) {
        setCurrentProgress(progress);
        setCurrentOperation(operation);
    }

    /**
     * Get success rate as percentage
     */
    public double getSuccessRate() {
        if (totalRecords <= 0) {
            return 0.0;
        }
        return ((double) importedCount / totalRecords) * 100.0;
    }

    /**
     * Get error rate as percentage
     */
    public double getErrorRate() {
        if (totalRecords <= 0) {
            return 0.0;
        }
        return ((double) errorCount / totalRecords) * 100.0;
    }

    /**
     * Get processing speed (records per second)
     */
    public double getProcessingSpeed() {
        if (processingTimeMs <= 0 || totalRecords <= 0) {
            return 0.0;
        }
        return (totalRecords / (double) processingTimeMs) * 1000.0;
    }

    /**
     * Get file size in human-readable format
     */
    public String getFileSizeDisplay() {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " bytes";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Get processing time in human-readable format
     */
    public String getProcessingTimeDisplay() {
        if (processingTimeMs < 1000) {
            return processingTimeMs + " ms";
        } else if (processingTimeMs < 60000) {
            return String.format("%.1f seconds", processingTimeMs / 1000.0);
        } else {
            long minutes = processingTimeMs / 60000;
            long seconds = (processingTimeMs % 60000) / 1000;
            return String.format("%d:%02d minutes", minutes, seconds);
        }
    }

    /**
     * Check if import has any issues (errors or warnings)
     */
    public boolean hasIssues() {
        return errorCount > 0 || warningCount > 0;
    }

    /**
     * Check if import was successful without warnings
     */
    public boolean isCompleteSuccess() {
        return status == ImportStatus.SUCCESS && errorCount == 0 && warningCount == 0;
    }

    private void updateErrorCount() {
        this.errorCount = validationErrors != null ? validationErrors.size() : 0;
    }

    private void updateWarningCount() {
        this.warningCount = warnings != null ? warnings.size() : 0;
    }

    private void generateSummary() {
        StringBuilder sb = new StringBuilder();

        sb.append("Import ").append(status.getDisplayName().toLowerCase());

        if (filename != null) {
            sb.append(" for file '").append(filename).append("'");
        }

        sb.append(". ");

        if (totalRecords > 0) {
            sb.append("Processed ").append(totalRecords).append(" records: ");
            sb.append(importedCount).append(" imported");

            if (errorCount > 0) {
                sb.append(", ").append(errorCount).append(" errors");
            }

            if (warningCount > 0) {
                sb.append(", ").append(warningCount).append(" warnings");
            }

            sb.append(". ");

            if (processingTimeMs > 0) {
                sb.append("Processing took ").append(getProcessingTimeDisplay()).append(".");
            }
        }

        this.summary = sb.toString();
    }

    @Override
    public String toString() {
        return summary != null ? summary : "ImportResult{" +
                "status=" + status +
                ", imported=" + importedCount +
                ", errors=" + errorCount +
                ", warnings=" + warningCount +
                '}';
    }
}