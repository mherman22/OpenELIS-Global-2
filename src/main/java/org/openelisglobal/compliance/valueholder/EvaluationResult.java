package org.openelisglobal.compliance.valueholder;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;

/**
 * EvaluationResult value holder representing individual parameter results
 * within compliance evaluations.
 *
 * Follows constitutional requirements: - Extends BaseObject for audit trail
 * support - Includes FHIR UUID for interoperability - Uses JPA annotations (no
 * XML mappings) - Implements validation annotations
 */
@Entity
@Table(name = "evaluation_result")
public class EvaluationResult extends BaseObject<String> implements SimpleBaseEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "evaluation_result_seq_gen")
    @GenericGenerator(
        name = "evaluation_result_seq_gen",
        strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator",
        parameters = @Parameter(name = "sequence_name", value = "evaluation_result_seq")
    )
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String id;

    @NotNull
    @Column(name = "fhir_uuid", unique = true, nullable = false)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    @JsonBackReference
    @NotNull(message = "Compliance evaluation is required")
    private ComplianceEvaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "threshold_id", nullable = false)
    @JsonBackReference
    @NotNull(message = "Compliance threshold is required")
    private ComplianceThreshold threshold;

    @Column(name = "tested_value", precision = 15, scale = 6)
    private BigDecimal testedValue;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "tested_value_text")
    private String testedValueText;

    @NotNull(message = "Compliance status is required")
    @Column(name = "is_compliant", nullable = false)
    private Boolean isCompliant;

    @Column(name = "variance_from_target", precision = 15, scale = 6)
    private BigDecimal varianceFromTarget;

    @DecimalMin(value = "-999.99", message = "Variance percentage must be within valid range")
    @DecimalMax(value = "999.99", message = "Variance percentage must be within valid range")
    @Column(name = "variance_percentage", precision = 5, scale = 2)
    private BigDecimal variancePercentage;

    @Column(name = "exceeded_by", precision = 15, scale = 6)
    private BigDecimal exceededBy;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "measurement_method")
    private String measurementMethod;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "measurement_date")
    private Date measurementDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "measurement_by")
    private String measurementBy;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "quality_flags")
    private String qualityFlags;

    @NotNull
    @Column(name = "detection_limit_applied", nullable = false)
    private Boolean detectionLimitApplied = false;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "result_notes", columnDefinition = "TEXT")
    private String resultNotes;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @NotNull
    @Column(name = "retest_required", nullable = false)
    private Boolean retestRequired = false;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "retest_reason")
    private String retestReason;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    public EvaluationResult() {
        super();
        generateFhirUuid();
    }

    public EvaluationResult(ComplianceEvaluation evaluation, ComplianceThreshold threshold, BigDecimal testedValue,
            Boolean isCompliant) {
        this();
        this.evaluation = evaluation;
        this.threshold = threshold;
        this.testedValue = testedValue;
        this.isCompliant = isCompliant;
        calculateVariances();
    }

    public EvaluationResult(ComplianceEvaluation evaluation, ComplianceThreshold threshold, String testedValueText,
            Boolean isCompliant) {
        this();
        this.evaluation = evaluation;
        this.threshold = threshold;
        this.testedValueText = testedValueText;
        this.isCompliant = isCompliant;
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

    public ComplianceEvaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(ComplianceEvaluation evaluation) {
        this.evaluation = evaluation;
    }

    public String getEvaluationId() {
        return evaluation != null ? evaluation.getId() : null;
    }

    public void setEvaluationId(String evaluationId) {
        if (evaluationId != null) {
            if (this.evaluation == null) {
                this.evaluation = new ComplianceEvaluation();
            }
            this.evaluation.setId(evaluationId);
        } else {
            this.evaluation = null;
        }
    }

    public ComplianceThreshold getThreshold() {
        return threshold;
    }

    public void setThreshold(ComplianceThreshold threshold) {
        this.threshold = threshold;
    }

    public String getThresholdId() {
        return threshold != null ? threshold.getId() : null;
    }

    public void setThresholdId(String thresholdId) {
        if (thresholdId != null) {
            if (this.threshold == null) {
                this.threshold = new ComplianceThreshold();
            }
            this.threshold.setId(thresholdId);
        } else {
            this.threshold = null;
        }
    }

    public BigDecimal getTestedValue() {
        return testedValue;
    }

    public void setTestedValue(BigDecimal testedValue) {
        this.testedValue = testedValue;
        calculateVariances();
    }

    public String getTestedValueText() {
        return testedValueText;
    }

    public void setTestedValueText(String testedValueText) {
        this.testedValueText = testedValueText;
    }

    public Boolean getIsCompliant() {
        return isCompliant;
    }

    public void setIsCompliant(Boolean isCompliant) {
        this.isCompliant = isCompliant != null ? isCompliant : false;
    }

    /**
     * Convenience method for isCompliant() - used by service implementations
     */
    public boolean isCompliant() {
        return Boolean.TRUE.equals(isCompliant);
    }

    /**
     * Convenience method for setCompliant(boolean) - used by service implementations
     */
    public void setCompliant(boolean compliant) {
        this.isCompliant = compliant;
    }

    public BigDecimal getVarianceFromTarget() {
        return varianceFromTarget;
    }

    public void setVarianceFromTarget(BigDecimal varianceFromTarget) {
        this.varianceFromTarget = varianceFromTarget;
    }

    public BigDecimal getVariancePercentage() {
        return variancePercentage;
    }

    public void setVariancePercentage(BigDecimal variancePercentage) {
        this.variancePercentage = variancePercentage;
    }

    public BigDecimal getExceededBy() {
        return exceededBy;
    }

    public void setExceededBy(BigDecimal exceededBy) {
        this.exceededBy = exceededBy;
    }

    public String getMeasurementMethod() {
        return measurementMethod;
    }

    public void setMeasurementMethod(String measurementMethod) {
        this.measurementMethod = measurementMethod;
    }

    public Date getMeasurementDate() {
        return measurementDate;
    }

    public void setMeasurementDate(Date measurementDate) {
        this.measurementDate = measurementDate;
    }

    public String getMeasurementBy() {
        return measurementBy;
    }

    public void setMeasurementBy(String measurementBy) {
        this.measurementBy = measurementBy;
    }

    public String getQualityFlags() {
        return qualityFlags;
    }

    public void setQualityFlags(String qualityFlags) {
        this.qualityFlags = qualityFlags;
    }

    public Boolean getDetectionLimitApplied() {
        return detectionLimitApplied;
    }

    public void setDetectionLimitApplied(Boolean detectionLimitApplied) {
        this.detectionLimitApplied = detectionLimitApplied != null ? detectionLimitApplied : false;
    }

    public String getResultNotes() {
        return resultNotes;
    }

    public void setResultNotes(String resultNotes) {
        this.resultNotes = resultNotes;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Boolean getRetestRequired() {
        return retestRequired;
    }

    public void setRetestRequired(Boolean retestRequired) {
        this.retestRequired = retestRequired != null ? retestRequired : false;
    }

    public String getRetestReason() {
        return retestReason;
    }

    public void setRetestReason(String retestReason) {
        this.retestReason = retestReason;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    // Helper methods

    /**
     * Get the parameter code from the threshold
     */
    public String getParameterCode() {
        return threshold != null ? threshold.getParameterCode() : null;
    }

    /**
     * Get the parameter display name from the threshold
     */
    public String getParameterDisplayName() {
        return threshold != null ? threshold.getDisplayName() : null;
    }

    /**
     * Get the threshold type from the threshold
     */
    public ThresholdType getThresholdType() {
        return threshold != null ? threshold.getThresholdType() : null;
    }

    /**
     * Get the units from the threshold
     */
    public String getUnits() {
        return threshold != null ? threshold.getUnits() : null;
    }

    /**
     * Get the displayed value (numeric or text)
     */
    @JsonIgnore
    public String getDisplayedValue() {
        if (testedValue != null) {
            String units = getUnits();
            String valueStr = testedValue.stripTrailingZeros().toPlainString();
            return units != null ? valueStr + " " + units : valueStr;
        } else if (testedValueText != null) {
            return testedValueText;
        }
        return "No value";
    }

    /**
     * Get the threshold range display from the threshold
     */
    @JsonIgnore
    public String getThresholdRangeDisplay() {
        return threshold != null ? threshold.getThresholdRangeDisplay() : "";
    }

    /**
     * Calculate variance from target value
     */
    private void calculateVariances() {
        if (threshold == null || testedValue == null) {
            return;
        }

        ThresholdType type = threshold.getThresholdType();
        if (type == null) {
            return;
        }

        switch (type) {
        case TARGET:
        case EXACT:
            BigDecimal targetValue = threshold.getTargetValue();
            if (targetValue != null) {
                this.varianceFromTarget = testedValue.subtract(targetValue);
                if (targetValue.compareTo(BigDecimal.ZERO) != 0) {
                    this.variancePercentage = this.varianceFromTarget.multiply(new BigDecimal("100"))
                            .divide(targetValue, 2, BigDecimal.ROUND_HALF_UP);
                }
            }
            break;
        case MAXIMUM:
            BigDecimal maxValue = threshold.getMaxValue();
            if (maxValue != null && testedValue.compareTo(maxValue) > 0) {
                this.exceededBy = testedValue.subtract(maxValue);
            }
            break;
        case MINIMUM:
            BigDecimal minValue = threshold.getMinValue();
            if (minValue != null && testedValue.compareTo(minValue) < 0) {
                this.exceededBy = minValue.subtract(testedValue);
            }
            break;
        case RANGE:
            BigDecimal minVal = threshold.getMinValue();
            BigDecimal maxVal = threshold.getMaxValue();
            if (minVal != null && testedValue.compareTo(minVal) < 0) {
                this.exceededBy = minVal.subtract(testedValue);
            } else if (maxVal != null && testedValue.compareTo(maxVal) > 0) {
                this.exceededBy = testedValue.subtract(maxVal);
            }
            break;
        }
    }

    /**
     * Re-evaluate compliance based on current threshold and tested value
     */
    public void reevaluateCompliance() {
        if (threshold == null) {
            this.isCompliant = false;
            return;
        }

        ThresholdType type = threshold.getThresholdType();
        if (type == null) {
            this.isCompliant = false;
            return;
        }

        if (testedValue != null) {
            this.isCompliant = threshold.evaluateNumericValue(testedValue);
        } else if (testedValueText != null) {
            this.isCompliant = threshold.evaluateTextValue(testedValueText);
        } else {
            this.isCompliant = false;
        }

        calculateVariances();
    }

    /**
     * Check if this result has any quality issues
     */
    @JsonIgnore
    public boolean hasQualityIssues() {
        return (qualityFlags != null && !qualityFlags.trim().isEmpty())
                || (validationErrors != null && !validationErrors.trim().isEmpty())
                || Boolean.TRUE.equals(retestRequired);
    }

    /**
     * Check if this result is below detection limit
     */
    @JsonIgnore
    public boolean isBelowDetectionLimit() {
        return Boolean.TRUE.equals(detectionLimitApplied) || (testedValueText != null
                && (testedValueText.equalsIgnoreCase("ND") || testedValueText.equalsIgnoreCase("NOT DETECTED")
                        || testedValueText.equalsIgnoreCase("BDL")));
    }

    /**
     * Get compliance status as string
     */
    @JsonIgnore
    public String getComplianceStatusDisplay() {
        if (isCompliant == null) {
            return "Unknown";
        }
        return isCompliant ? "Compliant" : "Non-Compliant";
    }

    /**
     * Get result display including compliance status
     */
    @JsonIgnore
    public String getResultDisplay() {
        return String.format("%s - %s", getDisplayedValue(), getComplianceStatusDisplay());
    }

    @Override
    public String toString() {
        return "EvaluationResult{" + "id='" + id + '\'' + ", parameterCode='" + getParameterCode() + '\''
                + ", testedValue=" + testedValue + ", testedValueText='" + testedValueText + '\'' + ", isCompliant="
                + isCompliant + '}';
    }
}