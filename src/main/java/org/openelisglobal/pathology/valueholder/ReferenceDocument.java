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
import java.sql.Date;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.pathology.valueholder.PathologyEnums.DocumentType;

/**
 * ReferenceDocument entity for SOP and protocol management with version
 * control.
 */
@Entity
@Table(name = "reference_document")
@Getter
@Setter
public class ReferenceDocument extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reference_document_generator")
    @SequenceGenerator(name = "reference_document_generator", sequenceName = "reference_document_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_content", columnDefinition = "BYTEA")
    private byte[] fileContent;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "effective_date", nullable = false)
    private Date effectiveDate;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "uploaded_by", nullable = false)
    private Integer uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Timestamp uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    private ReferenceDocument replacedBy;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public ReferenceDocument() {
        super();
    }
}
