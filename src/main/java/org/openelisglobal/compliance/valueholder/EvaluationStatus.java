package org.openelisglobal.compliance.valueholder;

/**
 * Enumeration for compliance evaluation status values.
 *
 * Represents the lifecycle status of a compliance evaluation within the system.
 * Follows OpenELIS naming conventions and constitutional requirements.
 */
public enum EvaluationStatus {

    /**
     * Evaluation has been created but not yet started
     */
    PENDING("Pending", "Evaluation has been created but not yet started"),

    /**
     * Evaluation is currently in progress
     */
    IN_PROGRESS("In Progress", "Evaluation is currently being performed"),

    /**
     * Evaluation has been completed successfully
     */
    COMPLETED("Completed", "Evaluation has been completed successfully"),

    /**
     * Evaluation has been reviewed and approved
     */
    REVIEWED("Reviewed", "Evaluation has been reviewed and approved"),

    /**
     * Evaluation failed due to errors
     */
    FAILED("Failed", "Evaluation failed due to errors or missing data"),

    /**
     * Evaluation was cancelled before completion
     */
    CANCELLED("Cancelled", "Evaluation was cancelled before completion"),

    /**
     * Evaluation requires re-evaluation due to issues
     */
    REQUIRES_RETEST("Requires Retest", "Evaluation requires re-evaluation due to questionable results"),

    /**
     * Evaluation concluded with compliant result
     */
    COMPLIANT("Compliant", "Evaluation concluded that all requirements are met"),

    /**
     * Evaluation concluded with non-compliant result
     */
    NON_COMPLIANT("Non-Compliant", "Evaluation concluded that requirements are not met"),

    /**
     * Evaluation concluded with warning result
     */
    WARNING("Warning", "Evaluation concluded with compliance concerns requiring attention");

    private final String displayName;
    private final String description;

    EvaluationStatus(String displayName, String description) {
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
    public static EvaluationStatus fromDisplayName(String displayName) {
        for (EvaluationStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No EvaluationStatus with display name: " + displayName);
    }

    /**
     * Check if the evaluation is in a final state
     */
    public boolean isFinalState() {
        return this == COMPLETED || this == REVIEWED || this == FAILED || this == CANCELLED
                || this == COMPLIANT || this == NON_COMPLIANT || this == WARNING;
    }

    /**
     * Check if the evaluation is active (in progress)
     */
    public boolean isActive() {
        return this == IN_PROGRESS;
    }

    /**
     * Check if the evaluation can be modified
     */
    public boolean allowsModification() {
        return this == PENDING || this == IN_PROGRESS || this == REQUIRES_RETEST;
    }

    /**
     * Check if the evaluation can be deleted
     */
    public boolean allowsDeletion() {
        return this == PENDING || this == CANCELLED || this == FAILED;
    }

    /**
     * Check if the evaluation results can be used for reporting
     */
    public boolean isValidForReporting() {
        return this == COMPLETED || this == REVIEWED || this == COMPLIANT || this == NON_COMPLIANT || this == WARNING;
    }

    /**
     * Check if the evaluation requires attention
     */
    public boolean requiresAttention() {
        return this == FAILED || this == REQUIRES_RETEST || this == NON_COMPLIANT || this == WARNING;
    }

    @Override
    public String toString() {
        return displayName;
    }
}