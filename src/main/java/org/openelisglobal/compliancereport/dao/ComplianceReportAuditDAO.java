package org.openelisglobal.compliancereport.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.compliancereport.valueholder.ComplianceReportAudit;

public interface ComplianceReportAuditDAO extends BaseDAO<ComplianceReportAudit, Long> {

    List<ComplianceReportAudit> getByOrderId(Long orderId);

    ComplianceReportAudit getMostRecentByOrderId(Long orderId);
}
