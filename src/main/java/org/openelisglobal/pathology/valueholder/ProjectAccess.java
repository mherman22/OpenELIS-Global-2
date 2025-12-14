package org.openelisglobal.pathology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.pathology.valueholder.PathologyEnums.AccessRole;

/**
 * ProjectAccess entity for project-based access control for research samples.
 */
@Entity
@Table(name = "project_access")
@Getter
@Setter
public class ProjectAccess extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_access_generator")
    @SequenceGenerator(name = "project_access_generator", sequenceName = "project_access_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_role", nullable = false, length = 20)
    private AccessRole accessRole;

    @Column(name = "granted_by", nullable = false)
    private Integer grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Timestamp grantedAt;

    @Column(name = "revoked_at")
    private Timestamp revokedAt;

    public ProjectAccess() {
        super();
    }
}
