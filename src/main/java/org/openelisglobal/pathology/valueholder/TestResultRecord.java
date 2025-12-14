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
import org.openelisglobal.pathology.valueholder.PathologyEnums.ControlStatus;
import org.openelisglobal.pathology.valueholder.PathologyEnums.StainQuality;
import org.openelisglobal.pathology.valueholder.PathologyEnums.TestType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * TestResultRecord entity for test/assay results with control validation and
 * pathologist sign-off.
 */
@Entity
@Table(name = "test_result_record")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Getter
@Setter
public class TestResultRecord extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_result_record_generator")
    @SequenceGenerator(name = "test_result_record_generator", sequenceName = "test_result_record_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @Column(name = "fhir_uuid")
    private UUID fhirUuid;

    @Column(name = "block_slide_id", length = 100)
    private String blockSlideId;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false, length = 50)
    private TestType testType;

    @Column(name = "stain_name", nullable = false, length = 100)
    private String stainName;

    @Column(name = "protocol_reference_id")
    private Integer protocolReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "positive_control_status", length = 10)
    private ControlStatus positiveControlStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "negative_control_status", length = 10)
    private ControlStatus negativeControlStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "stain_quality", length = 20)
    private StainQuality stainQuality;

    @Type(type = "jsonb")
    @Column(name = "result_data", nullable = false, columnDefinition = "jsonb")
    private String resultData;

    @Column(name = "pathologist_signoff_id")
    private Integer pathologistSignoffId;

    @Column(name = "signoff_timestamp")
    private Timestamp signoffTimestamp;

    @Column(name = "performed_by", nullable = false)
    private Integer performedBy;

    @Column(name = "performed_at", nullable = false)
    private Timestamp performedAt;

    public TestResultRecord() {
        super();
    }
}
