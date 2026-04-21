package org.openelisglobal.compliance.valueholder;

/**
 * Enumeration for CSV import status values.
 *
 * Represents the status of a CSV import operation within the compliance module.
 * Follows OpenELIS naming conventions and constitutional requirements.
 */
public enum ImportStatus {

    /**
     * Import completed successfully without any issues
     */
    SUCCESS("Success", "Import completed successfully"),

    /**
     * Import completed with warnings but no errors
     */
    SUCCESS_WITH_WARNINGS("Success with Warnings", "Import completed successfully but with warnings"),

    /**
     * Import failed due to validation errors
     */
    VALIDATION_FAILED("Validation Failed", "Import failed due to validation errors"),

    /**
     * Import failed due to security policy violations
     */
    SECURITY_VIOLATION("Security Violation", "Import rejected due to security policy violations"),

    /**
     * Import failed due to file size exceeding limits
     */
    FILE_TOO_LARGE("File Too Large", "Import rejected because file size exceeds the maximum allowed"),

    /**
     * Import failed due to invalid file format
     */
    INVALID_FORMAT("Invalid Format", "Import failed due to invalid file format or structure"),

    /**
     * Import failed due to system error
     */
    SYSTEM_ERROR("System Error", "Import failed due to an internal system error"),

    /**
     * Import was cancelled by user or system
     */
    CANCELLED("Cancelled", "Import operation was cancelled"),

    /**
     * Import is currently in progress
     */
    IN_PROGRESS("In Progress", "Import operation is currently in progress"),

    /**
     * Import failed due to insufficient permissions
     */
    PERMISSION_DENIED("Permission Denied", "Import failed due to insufficient user permissions");

    private final String displayName;
    private final String description;

    ImportStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get status by display name (case insensitive)
     */
    public static ImportStatus fromDisplayName(String displayName) {
        for (ImportStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No ImportStatus with display name: " + displayName);
    }

    /**
     * Check if the import was successful (complete or with warnings)
     */
    public boolean isSuccessful() {
        return this == SUCCESS || this == SUCCESS_WITH_WARNINGS;
    }

    /**
     * Check if the import failed
     */
    public boolean isFailed() {
        return this == VALIDATION_FAILED || this == SECURITY_VIOLATION || this == FILE_TOO_LARGE
                || this == INVALID_FORMAT || this == SYSTEM_ERROR || this == PERMISSION_DENIED;
    }

    /**
     * Check if the import is in progress
     */
    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    /**
     * Check if the import was cancelled
     */
    public boolean isCancelled() {
        return this == CANCELLED;
    }

    /**
     * Check if the import is in a final state (not in progress)
     */
    public boolean isFinalState() {
        return !isInProgress();
    }

    /**
     * Check if the status indicates data was imported
     */
    public boolean hasImportedData() {
        return isSuccessful();
    }

    @Override
    public String toString() {
        return displayName;
    }
}