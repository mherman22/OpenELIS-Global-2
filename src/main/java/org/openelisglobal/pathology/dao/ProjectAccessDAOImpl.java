package org.openelisglobal.pathology.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.pathology.valueholder.ProjectAccess;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ProjectAccessDAOImpl extends BaseDAOImpl<ProjectAccess, Integer> implements ProjectAccessDAO {
    public ProjectAccessDAOImpl() {
        super(ProjectAccess.class);
    }
}
