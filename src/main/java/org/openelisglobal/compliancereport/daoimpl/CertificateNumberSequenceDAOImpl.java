package org.openelisglobal.compliancereport.daoimpl;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliancereport.dao.CertificateNumberSequenceDAO;
import org.openelisglobal.compliancereport.valueholder.CertificateNumberSequence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CertificateNumberSequenceDAOImpl extends BaseDAOImpl<CertificateNumberSequence, Long>
        implements CertificateNumberSequenceDAO {

    public CertificateNumberSequenceDAOImpl() {
        super(CertificateNumberSequence.class);
    }

    @Override
    public Optional<CertificateNumberSequence> findBySiteIdAndYear(Long siteId, int year) {
        try {
            String hql = "FROM CertificateNumberSequence c WHERE c.siteId = :siteId AND c.year = :year";
            CertificateNumberSequence result = entityManager.unwrap(Session.class)
                    .createQuery(hql, CertificateNumberSequence.class).setParameter("siteId", siteId)
                    .setParameter("year", year).setMaxResults(1).uniqueResult();
            return Optional.ofNullable(result);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in CertificateNumberSequenceDAOImpl.findBySiteIdAndYear", e);
        }
    }

    @Override
    @Transactional
    public int getAndIncrementNextNumber(Long siteId, int year) {
        try {
            // Try UPDATE RETURNING for atomic increment
            String updateSql = "UPDATE clinlims.certificate_number_sequence SET last_number = last_number + 1"
                    + " WHERE site_id = :siteId AND year = :year RETURNING last_number";
            List<?> result = entityManager.unwrap(Session.class).createNativeQuery(updateSql)
                    .setParameter("siteId", siteId).setParameter("year", year).list();

            if (!result.isEmpty()) {
                return ((Number) result.get(0)).intValue();
            }

            // Row does not exist yet — insert it
            String seqNextSql = "SELECT nextval('certificate_number_seq')";
            BigInteger newId = (BigInteger) entityManager.unwrap(Session.class).createNativeQuery(seqNextSql)
                    .uniqueResult();

            String insertSql = "INSERT INTO clinlims.certificate_number_sequence (id, site_id, year, last_number)"
                    + " VALUES (:id, :siteId, :year, 1)"
                    + " ON CONFLICT (site_id, year) DO UPDATE SET last_number = certificate_number_sequence.last_number + 1"
                    + " RETURNING last_number";
            List<?> insertResult = entityManager.unwrap(Session.class).createNativeQuery(insertSql)
                    .setParameter("id", newId.longValue()).setParameter("siteId", siteId).setParameter("year", year)
                    .list();

            return insertResult.isEmpty() ? 1 : ((Number) insertResult.get(0)).intValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in CertificateNumberSequenceDAOImpl.getAndIncrementNextNumber", e);
        }
    }
}
