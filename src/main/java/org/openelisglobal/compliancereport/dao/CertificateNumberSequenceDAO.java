package org.openelisglobal.compliancereport.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.compliancereport.valueholder.CertificateNumberSequence;

public interface CertificateNumberSequenceDAO extends BaseDAO<CertificateNumberSequence, Long> {

    Optional<CertificateNumberSequence> findBySiteIdAndYear(Long siteId, int year);

    int getAndIncrementNextNumber(Long siteId, int year);
}
