package org.openelisglobal.compliance.valueholder;

/**
 * Enumeration for compliance standard status values.
 *
 * Represents the lifecycle status of a compliance standard within the system.
 * Follows OpenELIS naming conventions and constitutional requirements.
 */
public enum ComplianceStandardStatus {

    /**
     * Standard is being drafted and is not yet ready for use
     */
    DRAFT("Draft", "Standard is being drafted and reviewed"),

    /**
     * Standard is active and can be used for evaluations
     */
    ACTIVE("Active", "Standard is active and available for compliance evaluations"),

    /**
     * Standard has been replaced by a newer version
     */
    SUPERSEDED("Superseded", "Standard has been replaced by a newer version"),

    /**
     * Standard is no longer in use but preserved for historical reference
     */
    ARCHIVED("Archived", "Standard is archived and no longer in active use"),

    /**
     * Standard is temporarily suspended
     */
    SUSPENDED("Suspended", "Standard is temporarily suspended");

    private final String displayName;
    private final String description;

    ComplianceStandardStatus(String displayName, String description) {
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
    public static ComplianceStandardStatus fromDisplayName(String displayName) {
        for (ComplianceStandardStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No ComplianceStandardStatus with display name: " + displayName);
    }

    /**
     * Check if the status allows evaluations to be performed
     */
    public boolean allowsEvaluations() {
        return this == ACTIVE;
    }

    /**
     * Check if the standard can be modified
     */
    public boolean allowsModification() {
        return this == DRAFT || this == SUSPENDED;
    }

    /**
     * Check if the standard can be deleted
     */
    public boolean allowsDeletion() {
        return this == DRAFT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}