package org.openelisglobal.compliance.valueholder;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;
import org.openelisglobal.spring.util.SpringContext;

/**
 * ComplianceEvaluation value holder representing compliance evaluation records
 * for samples against compliance standards.
 *
 * Follows constitutional requirements: - Extends BaseObject for audit trail
 * support - Includes FHIR UUID for interoperability - Uses JPA annotations (no
 * XML mappings) - Implements validation annotations
 */
@Entity
@Table(name = "compliance_evaluation")
public class ComplianceEvaluation extends BaseObject<String> implements SimpleBaseEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "compliance_evaluation_generator", sequenceName = "compliance_evaluation_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compliance_evaluation_generator")
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    @Column(name = "id")
    private String id;

    @NotNull
    @Type(type = "uuid-char")
    @Column(name = "fhir_uuid", unique = true, nullable = false)
    private UUID fhirUuid;

    @NotBlank(message = "Sample ID is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "sample_id", nullable = false)
    private String sampleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "standard_id", nullable = false)
    @JsonBackReference
    @NotNull(message = "Compliance standard is required")
    private ComplianceStandard standard;

    @NotBlank(message = "Standard version is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "standard_version", nullable = false)
    private String standardVersion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false)
    private EvaluationStatus status = EvaluationStatus.PENDING;

    @Column(name = "overall_compliance")
    private Boolean overallCompliance;

    @DecimalMin(value = "0.0", message = "Compliance percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Compliance percentage cannot exceed 100")
    @Column(name = "compliance_percentage", precision = 5, scale = 2)
    private BigDecimal compliancePercentage;

    @NotNull(message = "Evaluation date is required")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "evaluated_date", nullable = false)
    private Date evaluatedDate;

    @NotBlank(message = "Evaluator is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "evaluated_by", nullable = false)
    private String evaluatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "completed_date")
    private Date completedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "reviewed_date")
    private Date reviewedDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "reviewed_by")
    private String reviewedBy;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "evaluation_notes", columnDefinition = "TEXT")
    private String evaluationNotes;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "corrective_actions", columnDefinition = "TEXT")
    private String correctiveActions;

    @NotNull
    @Column(name = "follow_up_required", nullable = false)
    private Boolean followUpRequired = false;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @NotNull
    @Column(name = "regulatory_notification_sent", nullable = false)
    private Boolean regulatoryNotificationSent = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "notification_sent_date")
    private Date notificationSentDate;

    // Bidirectional relationship with evaluation results
    @OneToMany(mappedBy = "evaluation", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<EvaluationResult> evaluationResults = new ArrayList<>();

    public ComplianceEvaluation() {
        super();
        generateFhirUuid();
        this.evaluatedDate = new Date();
    }

    public ComplianceEvaluation(String sampleId, ComplianceStandard standard, String evaluatedBy) {
        this();
        this.sampleId = sampleId;
        this.standard = standard;
        this.standardVersion = standard != null ? standard.getVersion() : null;
        this.evaluatedBy = evaluatedBy;
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

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public ComplianceStandard getStandard() {
        return standard;
    }

    public void setStandard(ComplianceStandard standard) {
        this.standard = standard;
        if (standard != null && this.standardVersion == null) {
            this.standardVersion = standard.getVersion();
        }
    }

    public String getStandardId() {
        return standard != null ? standard.getId() : null;
    }

    public void setStandardId(String standardId) {
        if (standardId != null) {
            if (this.standard == null) {
                this.standard = new ComplianceStandard();
            }
            this.standard.setId(standardId);
        } else {
            this.standard = null;
        }
    }

    public String getStandardVersion() {
        return standardVersion;
    }

    public void setStandardVersion(String standardVersion) {
        this.standardVersion = standardVersion;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(EvaluationStatus status) {
        this.status = status != null ? status : EvaluationStatus.PENDING;
    }

    public Boolean getOverallCompliance() {
        return overallCompliance;
    }

    public void setOverallCompliance(Boolean overallCompliance) {
        this.overallCompliance = overallCompliance;
    }

    public BigDecimal getCompliancePercentage() {
        return compliancePercentage;
    }

    public void setCompliancePercentage(BigDecimal compliancePercentage) {
        this.compliancePercentage = compliancePercentage;
    }

    public Date getEvaluatedDate() {
        return evaluatedDate;
    }

    public void setEvaluatedDate(Date evaluatedDate) {
        this.evaluatedDate = evaluatedDate != null ? evaluatedDate : new Date();
    }

    public String getEvaluatedBy() {
        return evaluatedBy;
    }

    public void setEvaluatedBy(String evaluatedBy) {
        this.evaluatedBy = evaluatedBy;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public Date getReviewedDate() {
        return reviewedDate;
    }

    public void setReviewedDate(Date reviewedDate) {
        this.reviewedDate = reviewedDate;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getEvaluationNotes() {
        return evaluationNotes;
    }

    public void setEvaluationNotes(String evaluationNotes) {
        this.evaluationNotes = evaluationNotes;
    }

    public String getCorrectiveActions() {
        return correctiveActions;
    }

    public void setCorrectiveActions(String correctiveActions) {
        this.correctiveActions = correctiveActions;
    }

    public Boolean getFollowUpRequired() {
        return followUpRequired;
    }

    public void setFollowUpRequired(Boolean followUpRequired) {
        this.followUpRequired = followUpRequired != null ? followUpRequired : false;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public Boolean getRegulatoryNotificationSent() {
        return regulatoryNotificationSent;
    }

    public void setRegulatoryNotificationSent(Boolean regulatoryNotificationSent) {
        this.regulatoryNotificationSent = regulatoryNotificationSent != null ? regulatoryNotificationSent : false;
    }

    public Date getNotificationSentDate() {
        return notificationSentDate;
    }

    public void setNotificationSentDate(Date notificationSentDate) {
        this.notificationSentDate = notificationSentDate;
    }

    public List<EvaluationResult> getEvaluationResults() {
        return evaluationResults;
    }

    public void setEvaluationResults(List<EvaluationResult> evaluationResults) {
        this.evaluationResults = evaluationResults != null ? evaluationResults : new ArrayList<>();
    }

    // Helper methods

    /**
     * Get the standard name for display purposes
     */
    public String getStandardName() {
        return standard != null ? standard.getName() : null;
    }

    /**
     * Get the standard display name for UI purposes
     */
    public String getStandardDisplayName() {
        return standard != null ? standard.getDisplayName() : null;
    }

    /**
     * Get the number of evaluation results
     */
    public int getResultCount() {
        return evaluationResults != null ? evaluationResults.size() : 0;
    }

    /**
     * Get the number of compliant results
     */
    @JsonIgnore
    public int getCompliantResultCount() {
        if (evaluationResults == null) {
            return 0;
        }
        return (int) evaluationResults.stream().filter(result -> Boolean.TRUE.equals(result.getIsCompliant())).count();
    }

    /**
     * Get the number of non-compliant results
     */
    @JsonIgnore
    public int getNonCompliantResultCount() {
        if (evaluationResults == null) {
            return 0;
        }
        return (int) evaluationResults.stream().filter(result -> Boolean.FALSE.equals(result.getIsCompliant())).count();
    }

    /**
     * Calculate compliance percentage based on results
     */
    @JsonIgnore
    public BigDecimal calculateCompliancePercentage() {
        int totalResults = getResultCount();
        if (totalResults == 0) {
            return BigDecimal.ZERO;
        }

        int compliantResults = getCompliantResultCount();
        return new BigDecimal(compliantResults).multiply(new BigDecimal("100")).divide(new BigDecimal(totalResults), 2,
                BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Update compliance percentage and overall compliance status
     */
    public void updateComplianceStatus() {
        this.compliancePercentage = calculateCompliancePercentage();
        this.overallCompliance = this.compliancePercentage.compareTo(new BigDecimal("100")) == 0;
    }

    /**
     * Add an evaluation result
     */
    public void addEvaluationResult(EvaluationResult result) {
        if (result != null) {
            if (evaluationResults == null) {
                evaluationResults = new ArrayList<>();
            }
            evaluationResults.add(result);
            result.setEvaluation(this);
        }
    }

    /**
     * Remove an evaluation result
     */
    public void removeEvaluationResult(EvaluationResult result) {
        if (result != null && evaluationResults != null) {
            evaluationResults.remove(result);
            result.setEvaluation(null);
        }
    }

    /**
     * Clear all evaluation results
     */
    public void clearEvaluationResults() {
        if (evaluationResults != null) {
            for (EvaluationResult result : evaluationResults) {
                result.setEvaluation(null);
            }
            evaluationResults.clear();
        }
    }

    /**
     * Check if the evaluation is complete
     */
    @JsonIgnore
    public boolean isComplete() {
        return status == EvaluationStatus.COMPLETED || status == EvaluationStatus.REVIEWED;
    }

    /**
     * Check if the evaluation can be modified
     */
    @JsonIgnore
    public boolean canModify() {
        return status != null && status.allowsModification();
    }

    /**
     * Mark evaluation as completed
     */
    public void markAsCompleted() {
        this.status = EvaluationStatus.COMPLETED;
        this.completedDate = new Date();
        updateComplianceStatus();
    }

    /**
     * Mark evaluation as reviewed
     */
    public void markAsReviewed(String reviewedBy) {
        this.status = EvaluationStatus.REVIEWED;
        this.reviewedDate = new Date();
        this.reviewedBy = reviewedBy;
    }

    /**
     * Get evaluation display name
     */
    @JsonIgnore
    public String getDisplayName() {
        String standardName = getStandardName();
        if (standardName != null) {
            return String.format("Sample %s vs %s", sampleId, standardName);
        }
        return String.format("Evaluation %s", id);
    }

    @Override
    public String toString() {
        return "ComplianceEvaluation{" + "id='" + id + '\'' + ", sampleId='" + sampleId + '\'' + ", standardId='"
                + getStandardId() + '\'' + ", status=" + status + ", overallCompliance=" + overallCompliance + '}';
    }

    /**
     * FHIR R4 integration - automatically sync to FHIR server on entity creation
     * Following established OpenELIS patterns for async FHIR persistence
     */
    @PostPersist
    public void onPostPersist() {
        try {
            org.openelisglobal.compliance.fhir.ComplianceFhirTransform fhirTransform = SpringContext
                    .getBean(org.openelisglobal.compliance.fhir.ComplianceFhirTransform.class);
            if (fhirTransform != null) {
                fhirTransform.syncComplianceEvaluationToFhir(this, true);
            }
        } catch (Exception e) {
            // Log error but don't fail transaction
            org.openelisglobal.common.log.LogEvent.logError("ComplianceEvaluation", "onPostPersist",
                    "Failed to sync to FHIR on create: " + e.getMessage());
        }
    }

    /**
     * FHIR R4 integration - automatically sync to FHIR server on entity update
     */
    @PostUpdate
    public void onPostUpdate() {
        try {
            org.openelisglobal.compliance.fhir.ComplianceFhirTransform fhirTransform = SpringContext
                    .getBean(org.openelisglobal.compliance.fhir.ComplianceFhirTransform.class);
            if (fhirTransform != null) {
                fhirTransform.syncComplianceEvaluationToFhir(this, false);
            }
        } catch (Exception e) {
            // Log error but don't fail transaction
            org.openelisglobal.common.log.LogEvent.logError("ComplianceEvaluation", "onPostUpdate",
                    "Failed to sync to FHIR on update: " + e.getMessage());
        }
    }

    /**
     * Get FHIR UUID as string for FHIR resource ID
     */
    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
    }
}