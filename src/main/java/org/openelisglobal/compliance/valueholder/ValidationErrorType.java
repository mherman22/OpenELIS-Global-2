package org.openelisglobal.compliance.valueholder;

/**
 * Enumeration for validation error types in CSV import operations.
 *
 * Categorizes different types of validation errors to enable appropriate
 * handling and user feedback during import operations.
 */
public enum ValidationErrorType {

    /**
     * General validation error
     */
    GENERAL("General", "ERROR", false, false),

    /**
     * Field-level validation error
     */
    FIELD_VALIDATION("Field Validation", "ERROR", false, false),

    /**
     * Data type validation error
     */
    DATA_TYPE("Data Type", "ERROR", false, false),

    /**
     * Required field validation error
     */
    REQUIRED_FIELD("Required Field", "ERROR", true, false),

    /**
     * Duplicate record validation error
     */
    DUPLICATE_RECORD("Duplicate Record", "WARNING", false, true),

    /**
     * Security policy violation
     */
    SECURITY_VIOLATION("Security Violation", "ERROR", true, false),

    /**
     * Business rule violation
     */
    BUSINESS_RULE("Business Rule", "ERROR", false, false),

    /**
     * Format or structure validation error
     */
    FORMAT_ERROR("Format Error", "ERROR", true, false),

    /**
     * Warning-level validation issue
     */
    WARNING("Warning", "WARNING", false, true),

    /**
     * Constraint violation (database constraints, etc.)
     */
    CONSTRAINT_VIOLATION("Constraint Violation", "ERROR", false, false),

    /**
     * Reference validation error (foreign key, lookup values)
     */
    REFERENCE_VALIDATION("Reference Validation", "ERROR", false, false),

    /**
     * Range or boundary validation error
     */
    RANGE_VALIDATION("Range Validation", "ERROR", false, false);

    private final String displayName;
    private final String severity;
    private final boolean critical;
    private final boolean warning;

    ValidationErrorType(String displayName, String severity, boolean critical, boolean warning) {
        this.displayName = displayName;
        this.severity = severity;
        this.critical = critical;
        this.warning = warning;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean isCritical() {
        return critical;
    }

    public boolean isWarning() {
        return warning;
    }

    /**
     * Check if this error type should stop processing
     */
    public boolean shouldStopProcessing() {
        return critical;
    }

    /**
     * Check if this error type allows continuing with warnings
     */
    public boolean allowsContinuation() {
        return warning;
    }

    /**
     * Get error type by display name (case insensitive)
     */
    public static ValidationErrorType fromDisplayName(String displayName) {
        for (ValidationErrorType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No ValidationErrorType with display name: " + displayName);
    }

    @Override
    public String toString() {
        return displayName;
    }
}