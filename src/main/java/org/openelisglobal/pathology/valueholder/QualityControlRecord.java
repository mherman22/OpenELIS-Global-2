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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCAction;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCStatus;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * QualityControlRecord entity for QC inspection tracking. Supports initial
 * inspection and post-embedding tissue block QC.
 */
@Entity
@Table(name = "quality_control_record")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Getter
@Setter
public class QualityControlRecord extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quality_control_record_generator")
    @SequenceGenerator(name = "quality_control_record_generator", sequenceName = "quality_control_record_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @Column(name = "fhir_uuid")
    private UUID fhirUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "qc_type", nullable = false, length = 30)
    private QCType qcType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private QCStatus status;

    @Type(type = "jsonb")
    @Column(name = "criteria_results", columnDefinition = "jsonb")
    private String criteriaResults;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", length = 50)
    private QCAction actionTaken;

    @Column(name = "technician_id", nullable = false)
    private Integer technicianId;

    @Column(name = "recorded_at", nullable = false)
    private Timestamp recordedAt;

    @Column(name = "pathologist_escalated")
    private Boolean pathologistEscalated = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public QualityControlRecord() {
        super();
    }
}
