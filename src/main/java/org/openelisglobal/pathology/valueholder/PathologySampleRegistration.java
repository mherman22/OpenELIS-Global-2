package org.openelisglobal.pathology.valueholder;

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
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.pathology.valueholder.PathologyEnums.SampleCategory;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * PathologySampleRegistration entity for extended pathology specimen metadata.
 * Distinguishes between clinical and research samples with category-specific
 * fields.
 */
@Entity
@Table(name = "pathology_sample_registration")
@Getter
@Setter
public class PathologySampleRegistration extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_sample_registration_generator")
    @SequenceGenerator(name = "pathology_sample_registration_generator", sequenceName = "pathology_sample_registration_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false, unique = true)
    private SampleItem sampleItem;

    @Column(name = "fhir_uuid")
    private UUID fhirUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private SampleCategory category;

    // Clinical specimen fields
    @Column(name = "patient_id", length = 100)
    private String patientId;

    @Column(name = "requesting_clinician", length = 255)
    private String requestingClinician;

    @Column(name = "clinical_details", columnDefinition = "TEXT")
    private String clinicalDetails;

    @Column(name = "specimen_site", length = 255)
    private String specimenSite;

    // Research specimen fields
    @Column(name = "study_id", length = 100)
    private String studyId;

    @Column(name = "pi_name", length = 255)
    private String piName;

    @Column(name = "participant_id", length = 100)
    private String participantId;

    @Column(name = "ethical_approval_ref", length = 255)
    private String ethicalApprovalRef;

    // Common fields
    @Column(name = "sample_source", length = 255)
    private String sampleSource = "Alert Hospital";

    @Column(name = "receiving_date", nullable = false)
    private Timestamp receivingDate;

    @Column(name = "receiving_staff_id")
    private Integer receivingStaffId;

    public PathologySampleRegistration() {
        super();
    }
}
