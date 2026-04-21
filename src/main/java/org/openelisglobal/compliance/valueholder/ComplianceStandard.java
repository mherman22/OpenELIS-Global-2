package org.openelisglobal.compliance.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.time.LocalDate;
import java.util.ArrayList;
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
 * ComplianceStandard value holder representing regulatory compliance standards
 * for environmental and vector testing.
 *
 * Follows constitutional requirements: - Extends BaseObject for audit trail
 * support - Includes FHIR UUID for interoperability - Uses JPA annotations (no
 * XML mappings) - Implements validation annotations
 */
@Entity
@Table(name = "compliance_standard")
public class ComplianceStandard extends BaseObject<String> implements SimpleBaseEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "compliance_standard_generator", sequenceName = "compliance_standard_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compliance_standard_generator")
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    @Column(name = "id")
    private String id;

    @NotNull
    @Type(type = "uuid-char")
    @Column(name = "fhir_uuid", unique = true, nullable = false)
    private UUID fhirUuid;

    @NotBlank(message = "Standard name is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Issuing body is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "issuing_body", nullable = false)
    private String issuingBody;

    @NotBlank(message = "Regulation number is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "regulation_number", nullable = false)
    private String regulationNumber;

    @NotBlank(message = "Version is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "version", nullable = false)
    private String version;

    @NotNull(message = "Effective date is required")
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @NotBlank(message = "Country/Region is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "country_region", nullable = false)
    private String countryRegion;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "applicable_sample_types", columnDefinition = "TEXT")
    private String applicableSampleTypes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ComplianceStandardStatus status = ComplianceStandardStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superseded_by_id")
    @JsonIgnoreProperties({ "supersededByStandard", "parameterGroups" })
    private ComplianceStandard supersededByStandard;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "regulatory_context", columnDefinition = "TEXT")
    private String regulatoryContext;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "enforcement_authority")
    private String enforcementAuthority;

    @NotNull
    @Column(name = "is_pre_seeded", nullable = false)
    private Boolean isPreSeeded = false;

    // Bidirectional relationship with parameter groups
    @OneToMany(mappedBy = "standard", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ParameterGroup> parameterGroups = new ArrayList<>();

    // Bidirectional relationship with evaluations
    @OneToMany(mappedBy = "standard", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ComplianceEvaluation> evaluations = new ArrayList<>();

    public ComplianceStandard() {
        super();
        generateFhirUuid();
    }

    public ComplianceStandard(String name, String issuingBody, String regulationNumber, String version) {
        this();
        this.name = name;
        this.issuingBody = issuingBody;
        this.regulationNumber = regulationNumber;
        this.version = version;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuingBody() {
        return issuingBody;
    }

    public void setIssuingBody(String issuingBody) {
        this.issuingBody = issuingBody;
    }

    public String getRegulationNumber() {
        return regulationNumber;
    }

    public void setRegulationNumber(String regulationNumber) {
        this.regulationNumber = regulationNumber;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCountryRegion() {
        return countryRegion;
    }

    public void setCountryRegion(String countryRegion) {
        this.countryRegion = countryRegion;
    }

    public String getApplicableSampleTypes() {
        return applicableSampleTypes;
    }

    public void setApplicableSampleTypes(String applicableSampleTypes) {
        this.applicableSampleTypes = applicableSampleTypes;
    }

    public ComplianceStandardStatus getStatus() {
        return status;
    }

    public void setStatus(ComplianceStandardStatus status) {
        this.status = status;
    }

    public ComplianceStandard getSupersededByStandard() {
        return supersededByStandard;
    }

    public void setSupersededByStandard(ComplianceStandard supersededByStandard) {
        this.supersededByStandard = supersededByStandard;
    }

    public String getSupersededById() {
        return supersededByStandard != null ? supersededByStandard.getId() : null;
    }

    public void setSupersededById(String supersededById) {
        if (supersededById != null) {
            this.supersededByStandard = new ComplianceStandard();
            this.supersededByStandard.setId(supersededById);
        } else {
            this.supersededByStandard = null;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRegulatoryContext() {
        return regulatoryContext;
    }

    public void setRegulatoryContext(String regulatoryContext) {
        this.regulatoryContext = regulatoryContext;
    }

    public String getEnforcementAuthority() {
        return enforcementAuthority;
    }

    public void setEnforcementAuthority(String enforcementAuthority) {
        this.enforcementAuthority = enforcementAuthority;
    }

    public Boolean getIsPreSeeded() {
        return isPreSeeded;
    }

    public void setIsPreSeeded(Boolean isPreSeeded) {
        this.isPreSeeded = isPreSeeded;
    }

    public List<ParameterGroup> getParameterGroups() {
        return parameterGroups;
    }

    public void setParameterGroups(List<ParameterGroup> parameterGroups) {
        this.parameterGroups = parameterGroups;
    }

    public List<ComplianceEvaluation> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(List<ComplianceEvaluation> evaluations) {
        this.evaluations = evaluations;
    }

    // Helper methods

    public boolean isActive() {
        return ComplianceStandardStatus.ACTIVE.equals(status);
    }

    public boolean isSuperseded() {
        return ComplianceStandardStatus.SUPERSEDED.equals(status);
    }

    public boolean isArchived() {
        return ComplianceStandardStatus.ARCHIVED.equals(status);
    }

    public List<String> getApplicableSampleTypesList() {
        if (applicableSampleTypes == null || applicableSampleTypes.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> types = new ArrayList<>();
        String[] typeArray = applicableSampleTypes.split(",");
        for (String type : typeArray) {
            types.add(type.trim());
        }
        return types;
    }

    public void setApplicableSampleTypesList(List<String> sampleTypes) {
        if (sampleTypes == null || sampleTypes.isEmpty()) {
            this.applicableSampleTypes = null;
        } else {
            this.applicableSampleTypes = String.join(",", sampleTypes);
        }
    }

    /**
     * Returns display name for UI purposes
     */
    public String getDisplayName() {
        return String.format("%s (%s %s)", name, regulationNumber, version);
    }

    /**
     * Returns short identifier for reference
     */
    public String getShortIdentifier() {
        return String.format("%s-%s", regulationNumber, version);
    }

    @Override
    public String toString() {
        return "ComplianceStandard{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", regulationNumber='"
                + regulationNumber + '\'' + ", version='" + version + '\'' + ", status=" + status + '}';
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
                fhirTransform.syncComplianceStandardToFhir(this, true);
            }
        } catch (Exception e) {
            // Log error but don't fail transaction
            org.openelisglobal.common.log.LogEvent.logError("ComplianceStandard", "onPostPersist",
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
                fhirTransform.syncComplianceStandardToFhir(this, false);
            }
        } catch (Exception e) {
            // Log error but don't fail transaction
            org.openelisglobal.common.log.LogEvent.logError("ComplianceStandard", "onPostUpdate",
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