package org.openelisglobal.compliance.valueholder;

/**
 * ValidationError represents individual validation errors during CSV import
 * operations.
 *
 * This class captures detailed information about validation failures including
 * location, field information, and error messages to provide meaningful
 * feedback to users during the import process.
 *
 * Follows OpenELIS patterns for value objects with proper validation and error
 * reporting capabilities.
 */
public class ValidationError {

    private Integer lineNumber;
    private String fieldName;
    private String fieldValue;
    private String message;
    private String errorCode;
    private ValidationErrorType errorType;

    public ValidationError() {
    }

    public ValidationError(String message) {
        this.message = message;
        this.errorType = ValidationErrorType.GENERAL;
    }

    public ValidationError(Integer lineNumber, String fieldName, String message) {
        this.lineNumber = lineNumber;
        this.fieldName = fieldName;
        this.message = message;
        this.errorType = ValidationErrorType.FIELD_VALIDATION;
    }

    public ValidationError(Integer lineNumber, String fieldName, String fieldValue, String message) {
        this.lineNumber = lineNumber;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.message = message;
        this.errorType = ValidationErrorType.FIELD_VALIDATION;
    }

    public ValidationError(ValidationErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;
    }

    public ValidationError(ValidationErrorType errorType, Integer lineNumber, String fieldName, String fieldValue,
            String message, String errorCode) {
        this.errorType = errorType;
        this.lineNumber = lineNumber;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.message = message;
        this.errorCode = errorCode;
    }

    // Getters and Setters

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public ValidationErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ValidationErrorType errorType) {
        this.errorType = errorType;
    }

    // Helper methods

    /**
     * Get a formatted error message including location information
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();

        if (lineNumber != null) {
            sb.append("Line ").append(lineNumber);
            if (fieldName != null) {
                sb.append(", Column '").append(fieldName).append("'");
            }
            sb.append(": ");
        } else if (fieldName != null) {
            sb.append("Column '").append(fieldName).append("': ");
        }

        if (message != null) {
            sb.append(message);
        }

        if (fieldValue != null && !fieldValue.trim().isEmpty()) {
            sb.append(" (Value: '").append(fieldValue).append("')");
        }

        return sb.toString();
    }

    /**
     * Get error severity level
     */
    public String getSeverity() {
        if (errorType != null) {
            return errorType.getSeverity();
        }
        return "ERROR";
    }

    /**
     * Check if this is a warning-level error
     */
    public boolean isWarning() {
        return errorType != null && errorType.isWarning();
    }

    /**
     * Check if this is a critical error that should stop processing
     */
    public boolean isCritical() {
        return errorType != null && errorType.isCritical();
    }

    /**
     * Create a field validation error
     */
    public static ValidationError fieldError(Integer lineNumber, String fieldName, String fieldValue, String message) {
        return new ValidationError(ValidationErrorType.FIELD_VALIDATION, lineNumber, fieldName, fieldValue, message,
                null);
    }

    /**
     * Create a data type validation error
     */
    public static ValidationError dataTypeError(Integer lineNumber, String fieldName, String fieldValue,
            String expectedType) {
        String message = String.format("Invalid data type. Expected %s but got '%s'", expectedType, fieldValue);
        return new ValidationError(ValidationErrorType.DATA_TYPE, lineNumber, fieldName, fieldValue, message,
                "INVALID_DATA_TYPE");
    }

    /**
     * Create a required field error
     */
    public static ValidationError requiredFieldError(Integer lineNumber, String fieldName) {
        return new ValidationError(ValidationErrorType.REQUIRED_FIELD, lineNumber, fieldName, null,
                "Required field is missing", "REQUIRED_FIELD");
    }

    /**
     * Create a duplicate record error
     */
    public static ValidationError duplicateError(Integer lineNumber, String message) {
        return new ValidationError(ValidationErrorType.DUPLICATE_RECORD, lineNumber, null, null, message,
                "DUPLICATE_RECORD");
    }

    /**
     * Create a security violation error
     */
    public static ValidationError securityError(String message) {
        return new ValidationError(ValidationErrorType.SECURITY_VIOLATION, null, null, null, message,
                "SECURITY_VIOLATION");
    }

    /**
     * Create a business rule violation error
     */
    public static ValidationError businessRuleError(Integer lineNumber, String message) {
        return new ValidationError(ValidationErrorType.BUSINESS_RULE, lineNumber, null, null, message,
                "BUSINESS_RULE_VIOLATION");
    }

    /**
     * Create a warning
     */
    public static ValidationError warning(Integer lineNumber, String message) {
        return new ValidationError(ValidationErrorType.WARNING, lineNumber, null, null, message, "WARNING");
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        ValidationError that = (ValidationError) obj;

        if (lineNumber != null ? !lineNumber.equals(that.lineNumber) : that.lineNumber != null)
            return false;
        if (fieldName != null ? !fieldName.equals(that.fieldName) : that.fieldName != null)
            return false;
        if (message != null ? !message.equals(that.message) : that.message != null)
            return false;
        return errorCode != null ? errorCode.equals(that.errorCode) : that.errorCode == null;
    }

    @Override
    public int hashCode() {
        int result = lineNumber != null ? lineNumber.hashCode() : 0;
        result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (errorCode != null ? errorCode.hashCode() : 0);
        return result;
    }
}