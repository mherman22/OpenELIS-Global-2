package org.openelisglobal.compliance.valueholder;

import java.math.BigDecimal;

/**
 * Enumeration for threshold types in compliance evaluations.
 *
 * Defines different types of threshold conditions that can be applied
 * to parameter values during compliance evaluation.
 */
public enum ThresholdType {

    /**
     * Value must be within a specified range (min <= value <= max)
     */
    RANGE("Range", "Value must be between minimum and maximum limits"),

    /**
     * Value must not be less than the minimum (value >= min)
     */
    MINIMUM("Minimum", "Value must be greater than or equal to minimum limit"),

    /**
     * Value must not exceed the maximum (value <= max)
     */
    MAXIMUM("Maximum", "Value must be less than or equal to maximum limit"),

    /**
     * Value should match the target value exactly
     */
    EXACT("Exact", "Value must match the target value exactly"),

    /**
     * Value should be close to the target value (with tolerance)
     */
    TARGET("Target", "Value should be close to the target value"),

    /**
     * Value is not detected (below detection limit)
     */
    NOT_DETECTED("Not Detected", "Parameter should not be detected"),

    /**
     * Qualitative result - pass/fail evaluation
     */
    QUALITATIVE("Qualitative", "Pass/fail or positive/negative evaluation");

    private final String displayName;
    private final String description;

    ThresholdType(String displayName, String description) {
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
     * Get threshold type by display name (case insensitive)
     */
    public static ThresholdType fromDisplayName(String displayName) {
        for (ThresholdType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No ThresholdType with display name: " + displayName);
    }

    /**
     * Check if this threshold type requires a minimum value
     */
    public boolean requiresMinValue() {
        return this == RANGE || this == MINIMUM;
    }

    /**
     * Check if this threshold type requires a maximum value
     */
    public boolean requiresMaxValue() {
        return this == RANGE || this == MAXIMUM;
    }

    /**
     * Check if this threshold type requires a target value
     */
    public boolean requiresTargetValue() {
        return this == EXACT || this == TARGET;
    }

    /**
     * Check if this threshold type supports numeric values
     */
    public boolean supportsNumericValues() {
        return this != QUALITATIVE && this != NOT_DETECTED;
    }

    /**
     * Check if this threshold type supports text values
     */
    public boolean supportsTextValues() {
        return this == QUALITATIVE || this == NOT_DETECTED || this == EXACT;
    }

    /**
     * Evaluate if a numeric value is compliant with this threshold
     */
    public boolean evaluate(BigDecimal value, BigDecimal minValue, BigDecimal maxValue, BigDecimal targetValue) {
        if (value == null) {
            return false;
        }

        switch (this) {
            case RANGE:
                return minValue != null && maxValue != null &&
                       value.compareTo(minValue) >= 0 && value.compareTo(maxValue) <= 0;
            case MINIMUM:
                return minValue != null && value.compareTo(minValue) >= 0;
            case MAXIMUM:
                return maxValue != null && value.compareTo(maxValue) <= 0;
            case EXACT:
                return targetValue != null && value.compareTo(targetValue) == 0;
            case TARGET:
                // For target, we might allow some tolerance (could be configurable)
                return targetValue != null && value.compareTo(targetValue) == 0;
            case NOT_DETECTED:
                // For not detected, value should be null, zero, or below detection limit
                return value.compareTo(BigDecimal.ZERO) == 0;
            default:
                return false;
        }
    }

    /**
     * Evaluate if a text value is compliant with this threshold
     */
    public boolean evaluate(String value, String expectedValue) {
        if (this == QUALITATIVE) {
            if (expectedValue == null) {
                return value != null && (value.equalsIgnoreCase("PASS") ||
                                       value.equalsIgnoreCase("NEGATIVE") ||
                                       value.equalsIgnoreCase("ABSENT"));
            }
            return value != null && value.equalsIgnoreCase(expectedValue);
        } else if (this == NOT_DETECTED) {
            return value == null || value.isEmpty() ||
                   value.equalsIgnoreCase("NOT DETECTED") ||
                   value.equalsIgnoreCase("ABSENT") ||
                   value.equalsIgnoreCase("ND");
        } else if (this == EXACT) {
            return value != null && expectedValue != null && value.equalsIgnoreCase(expectedValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return displayName;
    }
}