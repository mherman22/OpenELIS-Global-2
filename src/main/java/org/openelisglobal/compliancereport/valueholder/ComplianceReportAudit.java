package org.openelisglobal.compliancereport.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "compliance_report_audit", schema = "clinlims")
@AttributeOverride(name = "lastupdated", column = @Column(name = "last_updated", insertable = true, updatable = true))
@Getter
@Setter
public class ComplianceReportAudit extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compliance_report_audit_seq_gen")
    @SequenceGenerator(name = "compliance_report_audit_seq_gen", sequenceName = "compliance_report_audit_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "certificate_number", nullable = false, length = 50)
    private String certificateNumber;

    @Column(name = "generated_by", nullable = false)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false)
    private Timestamp generatedAt;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "compliance_status", nullable = false, length = 20)
    private String complianceStatus;

    @Column(name = "parameter_count")
    private Integer parameterCount = 0;

    @Column(name = "pass_count")
    private Integer passCount = 0;

    @Column(name = "marginal_count")
    private Integer marginalCount = 0;

    @Column(name = "fail_count")
    private Integer failCount = 0;
}
