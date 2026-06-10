package org.openelisglobal.compliancereport.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "certificate_number_sequence", schema = "clinlims")
@Getter
@Setter
public class CertificateNumberSequence extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "certificate_number_seq_gen")
    @SequenceGenerator(name = "certificate_number_seq_gen", sequenceName = "certificate_number_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "site_id")
    private Long siteId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "last_number")
    private Integer lastNumber = 0;
}
