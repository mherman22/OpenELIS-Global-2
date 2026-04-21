package org.openelisglobal.compliance.valueholder;

/**
 * Enumeration of compliance import operation statuses.
 *
 * Tracks the lifecycle status of compliance standard import operations,
 * providing detailed state information for audit and monitoring purposes.
 */
public enum ComplianceImportStatus {

    /**
     * Import operation has been initiated but not yet started processing
     */
    STARTED("Started"),

    /**
     * Import operation is currently in progress
     */
    IN_PROGRESS("In Progress"),

    /**
     * Import operation completed successfully without any issues
     */
    COMPLETED("Completed"),

    /**
     * Import operation completed but with warnings or non-critical issues
     */
    COMPLETED_WITH_WARNINGS("Completed with Warnings"),

    /**
     * Import operation failed due to errors
     */
    FAILED("Failed"),

    /**
     * Import operation was cancelled by user or system
     */
    CANCELLED("Cancelled"),

    /**
     * Import operation timed out
     */
    TIMEOUT("Timeout"),

    /**
     * Import operation is queued waiting for processing
     */
    QUEUED("Queued"),

    /**
     * Import operation is being validated before processing
     */
    VALIDATING("Validating"),

    /**
     * Import operation validation failed
     */
    VALIDATION_FAILED("Validation Failed"),

    /**
     * Import operation is being rolled back due to failure
     */
    ROLLING_BACK("Rolling Back"),

    /**
     * Import operation was rolled back successfully
     */
    ROLLED_BACK("Rolled Back");

    private final String displayName;

    ComplianceImportStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get the display name for UI purposes
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if the status indicates the operation is still active/running
     */
    public boolean isActive() {
        return this == STARTED || this == IN_PROGRESS || this == QUEUED || this == VALIDATING || this == ROLLING_BACK;
    }

    /**
     * Check if the status indicates the operation completed successfully
     */
    public boolean isSuccess() {
        return this == COMPLETED || this == COMPLETED_WITH_WARNINGS;
    }

    /**
     * Check if the status indicates the operation failed
     */
    public boolean isFailure() {
        return this == FAILED || this == VALIDATION_FAILED || this == CANCELLED || this == TIMEOUT;
    }

    /**
     * Check if the status indicates the operation finished (success or failure)
     */
    public boolean isFinished() {
        return !isActive();
    }

    /**
     * Get enum value from display name
     */
    public static ComplianceImportStatus fromDisplayName(String displayName) {
        for (ComplianceImportStatus status : values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Get status color for UI display
     */
    public String getStatusColor() {
        switch (this) {
        case COMPLETED:
            return "green";
        case COMPLETED_WITH_WARNINGS:
            return "orange";
        case FAILED:
        case VALIDATION_FAILED:
        case TIMEOUT:
            return "red";
        case CANCELLED:
        case ROLLED_BACK:
            return "gray";
        case STARTED:
        case IN_PROGRESS:
        case QUEUED:
        case VALIDATING:
        case ROLLING_BACK:
            return "blue";
        default:
            return "gray";
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}