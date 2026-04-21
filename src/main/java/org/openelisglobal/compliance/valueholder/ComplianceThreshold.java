package org.openelisglobal.compliance.valueholder;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.util.ValidationHelper;
import org.openelisglobal.common.util.validator.SafeHtml;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;

/**
 * ComplianceThreshold value holder representing individual parameter thresholds
 * within parameter groups.
 *
 * Follows constitutional requirements: - Extends BaseObject for audit trail
 * support - Includes FHIR UUID for interoperability - Uses JPA annotations (no
 * XML mappings) - Implements validation annotations
 */
@Entity
@Table(name = "compliance_threshold")
public class ComplianceThreshold extends BaseObject<String> implements SimpleBaseEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "compliance_threshold_generator", sequenceName = "compliance_threshold_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compliance_threshold_generator")
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    @Column(name = "id")
    private String id;

    @NotNull
    @Type(type = "uuid-char")
    @Column(name = "fhir_uuid", unique = true, nullable = false)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonBackReference
    @NotNull(message = "Parameter group is required")
    private ParameterGroup group;

    @NotBlank(message = "Parameter code is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "parameter_code", nullable = false)
    private String parameterCode;

    @NotBlank(message = "Display name is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @NotNull(message = "Threshold type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_type", nullable = false)
    private ThresholdType thresholdType;

    @DecimalMin(value = "0", inclusive = false, message = "Minimum value must be positive")
    @Column(name = "min_value", precision = 15, scale = 6)
    private BigDecimal minValue;

    @DecimalMin(value = "0", inclusive = false, message = "Maximum value must be positive")
    @Column(name = "max_value", precision = 15, scale = 6)
    private BigDecimal maxValue;

    @DecimalMin(value = "0", inclusive = false, message = "Target value must be positive")
    @Column(name = "target_value", precision = 15, scale = 6)
    private BigDecimal targetValue;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "units")
    private String units;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "method_reference")
    private String methodReference;

    @DecimalMin(value = "0", inclusive = false, message = "Detection limit must be positive")
    @Column(name = "detection_limit", precision = 15, scale = 6)
    private BigDecimal detectionLimit;

    @NotNull
    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = true;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Bidirectional relationship with evaluation results
    @OneToMany(mappedBy = "threshold", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<EvaluationResult> evaluationResults = new ArrayList<>();

    public ComplianceThreshold() {
        super();
        generateFhirUuid();
    }

    public ComplianceThreshold(String parameterCode, String displayName, ThresholdType thresholdType) {
        this();
        this.parameterCode = parameterCode;
        this.displayName = displayName;
        this.thresholdType = thresholdType;
    }

    private void generateFhirUuid() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Getters and Setters

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public ParameterGroup getGroup() {
        return group;
    }

    public void setGroup(ParameterGroup group) {
        this.group = group;
    }

    public String getGroupId() {
        return group != null ? group.getId() : null;
    }

    public void setGroupId(String groupId) {
        if (groupId != null) {
            if (this.group == null) {
                this.group = new ParameterGroup();
            }
            this.group.setId(groupId);
        } else {
            this.group = null;
        }
    }

    public String getParameterCode() {
        return parameterCode;
    }

    public void setParameterCode(String parameterCode) {
        this.parameterCode = parameterCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ThresholdType getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(ThresholdType thresholdType) {
        this.thresholdType = thresholdType;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getMethodReference() {
        return methodReference;
    }

    public void setMethodReference(String methodReference) {
        this.methodReference = methodReference;
    }

    public BigDecimal getDetectionLimit() {
        return detectionLimit;
    }

    public void setDetectionLimit(BigDecimal detectionLimit) {
        this.detectionLimit = detectionLimit;
    }

    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory != null ? isMandatory : true;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    public String getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(String validationRules) {
        this.validationRules = validationRules;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<EvaluationResult> getEvaluationResults() {
        return evaluationResults;
    }

    public void setEvaluationResults(List<EvaluationResult> evaluationResults) {
        this.evaluationResults = evaluationResults != null ? evaluationResults : new ArrayList<>();
    }

    // Helper methods

    /**
     * Get the parameter group name for display purposes
     */
    public String getGroupName() {
        return group != null ? group.getName() : null;
    }

    /**
     * Get the standard name from the associated group
     */
    public String getStandardName() {
        return group != null ? group.getStandardName() : null;
    }

    /**
     * Returns the full parameter display name with units
     */
    @JsonIgnore
    public String getFullDisplayName() {
        if (units != null && !units.trim().isEmpty()) {
            return String.format("%s (%s)", displayName, units);
        }
        return displayName;
    }

    /**
     * Get the threshold range as a display string
     */
    @JsonIgnore
    public String getThresholdRangeDisplay() {
        switch (thresholdType) {
        case RANGE:
            return String.format("%s - %s %s", formatValue(minValue), formatValue(maxValue),
                    units != null ? units : "");
        case MINIMUM:
            return String.format("≥ %s %s", formatValue(minValue), units != null ? units : "");
        case MAXIMUM:
            return String.format("≤ %s %s", formatValue(maxValue), units != null ? units : "");
        case EXACT:
        case TARGET:
            return String.format("= %s %s", formatValue(targetValue), units != null ? units : "");
        case NOT_DETECTED:
            return "Not Detected";
        case QUALITATIVE:
            return "Pass/Fail";
        default:
            return "";
        }
    }

    /**
     * Evaluate a numeric value against this threshold
     */
    public boolean evaluateNumericValue(BigDecimal value) {
        if (thresholdType == null) {
            return false;
        }
        return thresholdType.evaluate(value, minValue, maxValue, targetValue);
    }

    /**
     * Evaluate a text value against this threshold
     */
    public boolean evaluateTextValue(String value) {
        if (thresholdType == null) {
            return false;
        }
        return thresholdType.evaluate(value, null);
    }

    /**
     * Check if this threshold requires validation
     */
    public boolean requiresValidation() {
        return thresholdType != null && (thresholdType.requiresMinValue() || thresholdType.requiresMaxValue()
                || thresholdType.requiresTargetValue());
    }

    /**
     * Validate threshold configuration
     */
    @JsonIgnore
    public boolean isValidConfiguration() {
        if (thresholdType == null) {
            return false;
        }

        switch (thresholdType) {
        case RANGE:
            return minValue != null && maxValue != null && minValue.compareTo(maxValue) <= 0;
        case MINIMUM:
            return minValue != null;
        case MAXIMUM:
            return maxValue != null;
        case EXACT:
        case TARGET:
            return targetValue != null;
        case NOT_DETECTED:
        case QUALITATIVE:
            return true;
        default:
            return false;
        }
    }

    /**
     * Get validation error message if configuration is invalid
     */
    @JsonIgnore
    public String getValidationError() {
        if (isValidConfiguration()) {
            return null;
        }

        if (thresholdType == null) {
            return "Threshold type is required";
        }

        switch (thresholdType) {
        case RANGE:
            if (minValue == null || maxValue == null) {
                return "Range threshold requires both minimum and maximum values";
            } else if (minValue.compareTo(maxValue) > 0) {
                return "Minimum value cannot be greater than maximum value";
            }
            break;
        case MINIMUM:
            if (minValue == null) {
                return "Minimum threshold requires a minimum value";
            }
            break;
        case MAXIMUM:
            if (maxValue == null) {
                return "Maximum threshold requires a maximum value";
            }
            break;
        case EXACT:
        case TARGET:
            if (targetValue == null) {
                return thresholdType.getDisplayName() + " threshold requires a target value";
            }
            break;
        }

        return "Invalid threshold configuration";
    }

    private String formatValue(BigDecimal value) {
        if (value == null) {
            return "";
        }
        // Remove trailing zeros and unnecessary decimal point
        return value.stripTrailingZeros().toPlainString();
    }

    @Override
    public String toString() {
        return "ComplianceThreshold{" + "id='" + id + '\'' + ", parameterCode='" + parameterCode + '\''
                + ", displayName='" + displayName + '\'' + ", thresholdType=" + thresholdType + ", groupId='"
                + getGroupId() + '\'' + '}';
    }
}