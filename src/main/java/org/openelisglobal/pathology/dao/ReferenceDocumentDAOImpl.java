package org.openelisglobal.pathology.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.pathology.valueholder.ReferenceDocument;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReferenceDocumentDAOImpl extends BaseDAOImpl<ReferenceDocument, Integer> implements ReferenceDocumentDAO {
    public ReferenceDocumentDAOImpl() {
        super(ReferenceDocument.class);
    }
}
