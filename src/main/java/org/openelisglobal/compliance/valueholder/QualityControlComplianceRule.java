package org.openelisglobal.compliance.valueholder;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;

/**
 * Entity representing the relationship between Quality Control rules and Compliance Standards.
 *
 * This entity links QC rules (control charts, Westgard rules, etc.) to specific compliance
 * standards, enabling automatic compliance evaluation during QC analysis.
 *
 * Business Rules:
 * - Each QC rule can be associated with multiple compliance standards
 * - A compliance standard can have multiple QC rules
 * - QC rules are mandatory when specified as required for compliance
 * - Evaluation frequency determines how often compliance is checked
 *
 * Constitutional Compliance:
 * - Extends BaseObject for audit trail
 * - Uses FHIR UUID for interoperability
 * - Follows OpenELIS entity patterns
 */
@Entity
@Table(name = "qc_compliance_rule")
public class QualityControlComplianceRule extends BaseObject<String> implements SimpleBaseEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qc_compliance_rule_generator")
    @SequenceGenerator(name = "qc_compliance_rule_generator", sequenceName = "qc_compliance_rule_seq", allocationSize = 1)
    @Column(name = "id")
    private String id;

    @NotNull
    @Type(type = "uuid-char")
    @Column(name = "fhir_uuid", updatable = false, unique = true, nullable = false)
    private UUID fhirUuid;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compliance_standard_id", referencedColumnName = "id", nullable = false)
    private ComplianceStandard complianceStandard;

    @NotBlank
    @Column(name = "qc_rule_name", nullable = false)
    private String qcRuleName;

    @NotBlank
    @Column(name = "qc_rule_type", nullable = false)
    private String qcRuleType; // WESTGARD, CONTROL_CHART, TREND_ANALYSIS, etc.

    @Column(name = "rule_description", length = 1000)
    private String ruleDescription;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_frequency", nullable = false)
    private QCEvaluationFrequency evaluationFrequency;

    @NotNull
    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = false;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "failure_action", length = 500)
    private String failureAction; // Actions to take when QC rule fails compliance

    @Column(name = "notification_recipients", length = 1000)
    private String notificationRecipients; // Email addresses for compliance alerts

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "configuration_json", length = 4000)
    private String configurationJson; // JSON configuration for rule parameters

    // Constructors

    public QualityControlComplianceRule() {
        super();
        this.fhirUuid = UUID.randomUUID();
    }

    public QualityControlComplianceRule(ComplianceStandard complianceStandard, String qcRuleName, String qcRuleType) {
        this();
        this.complianceStandard = complianceStandard;
        this.qcRuleName = qcRuleName;
        this.qcRuleType = qcRuleType;
        this.evaluationFrequency = QCEvaluationFrequency.PER_RUN;
        this.isMandatory = false;
        this.isActive = true;
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

    public ComplianceStandard getComplianceStandard() {
        return complianceStandard;
    }

    public void setComplianceStandard(ComplianceStandard complianceStandard) {
        this.complianceStandard = complianceStandard;
    }

    public String getQcRuleName() {
        return qcRuleName;
    }

    public void setQcRuleName(String qcRuleName) {
        this.qcRuleName = qcRuleName;
    }

    public String getQcRuleType() {
        return qcRuleType;
    }

    public void setQcRuleType(String qcRuleType) {
        this.qcRuleType = qcRuleType;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }

    public QCEvaluationFrequency getEvaluationFrequency() {
        return evaluationFrequency;
    }

    public void setEvaluationFrequency(QCEvaluationFrequency evaluationFrequency) {
        this.evaluationFrequency = evaluationFrequency;
    }

    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getFailureAction() {
        return failureAction;
    }

    public void setFailureAction(String failureAction) {
        this.failureAction = failureAction;
    }

    public String getNotificationRecipients() {
        return notificationRecipients;
    }

    public void setNotificationRecipients(String notificationRecipients) {
        this.notificationRecipients = notificationRecipients;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public void setConfigurationJson(String configurationJson) {
        this.configurationJson = configurationJson;
    }

    // Business Logic Methods

    /**
     * Check if this QC compliance rule should be evaluated based on frequency
     */
    public boolean shouldEvaluate(String lastEvaluationContext) {
        if (!isActive) {
            return false;
        }

        switch (evaluationFrequency) {
            case EVERY_RESULT:
                return true;
            case PER_RUN:
                // Implement run-based evaluation logic
                return true;
            case DAILY:
                // Implement daily evaluation logic
                return true;
            case WEEKLY:
                // Implement weekly evaluation logic
                return true;
            default:
                return false;
        }
    }

    /**
     * Get display name combining rule name and standard
     */
    public String getDisplayName() {
        if (complianceStandard != null) {
            return qcRuleName + " (" + complianceStandard.getName() + ")";
        }
        return qcRuleName;
    }

    /**
     * Check if this rule requires immediate action on failure
     */
    public boolean requiresImmediateAction() {
        return isMandatory && isActive;
    }

    @Override
    public String toString() {
        return "QualityControlComplianceRule{" +
                "id='" + id + '\'' +
                ", qcRuleName='" + qcRuleName + '\'' +
                ", qcRuleType='" + qcRuleType + '\'' +
                ", evaluationFrequency=" + evaluationFrequency +
                ", isMandatory=" + isMandatory +
                ", isActive=" + isActive +
                '}';
    }
}

/**
 * Enumeration for QC evaluation frequency options
 */
enum QCEvaluationFrequency {
    EVERY_RESULT,  // Evaluate with every test result
    PER_RUN,       // Evaluate once per analytical run
    DAILY,         // Evaluate once per day
    WEEKLY         // Evaluate once per week
}