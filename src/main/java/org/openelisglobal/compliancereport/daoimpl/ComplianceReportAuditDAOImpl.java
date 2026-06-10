package org.openelisglobal.compliancereport.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliancereport.dao.ComplianceReportAuditDAO;
import org.openelisglobal.compliancereport.valueholder.ComplianceReportAudit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ComplianceReportAuditDAOImpl extends BaseDAOImpl<ComplianceReportAudit, Long>
        implements ComplianceReportAuditDAO {

    public ComplianceReportAuditDAOImpl() {
        super(ComplianceReportAudit.class);
    }

    @Override
    public List<ComplianceReportAudit> getByOrderId(Long orderId) {
        try {
            String hql = "FROM ComplianceReportAudit a WHERE a.orderId = :orderId ORDER BY a.generatedAt DESC";
            return entityManager.unwrap(Session.class).createQuery(hql, ComplianceReportAudit.class)
                    .setParameter("orderId", orderId).list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceReportAuditDAOImpl.getByOrderId", e);
        }
    }

    @Override
    public ComplianceReportAudit getMostRecentByOrderId(Long orderId) {
        try {
            String hql = "FROM ComplianceReportAudit a WHERE a.orderId = :orderId ORDER BY a.generatedAt DESC";
            return entityManager.unwrap(Session.class).createQuery(hql, ComplianceReportAudit.class)
                    .setParameter("orderId", orderId).setMaxResults(1).uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceReportAuditDAOImpl.getMostRecentByOrderId", e);
        }
    }
}
