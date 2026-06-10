package org.openelisglobal.compliancereport.daoimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliancereport.dao.ComplianceReportAuditDAO;
import org.openelisglobal.compliancereport.valueholder.ComplianceReportAudit;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ComplianceReportAuditDAOImpl}.
 *
 * <p>
 * Verifies that the custom HQL queries correctly filter and order audit
 * records. Test data is loaded from
 * {@code testdata/compliance_report_audit.xml} which seeds three audit rows
 * across two order IDs.
 */
public class ComplianceReportAuditDAOImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ComplianceReportAuditDAO complianceReportAuditDAO;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance_report_audit.xml");
    }

    @Test
    public void testGetByOrderId_shouldReturnAllAuditsForGivenOrder() {
        List<ComplianceReportAudit> audits = complianceReportAuditDAO.getByOrderId(1001L);

        assertNotNull(audits);
        assertEquals("Should return both audit records for order 1001", 2, audits.size());
    }

    @Test
    public void testGetByOrderId_shouldReturnAuditsInDescendingDateOrder() {
        List<ComplianceReportAudit> audits = complianceReportAuditDAO.getByOrderId(1001L);

        assertEquals("Most recent audit should be first", "LH-2025-00002", audits.get(0).getCertificateNumber());
        assertEquals("Older audit should be second", "LH-2025-00001", audits.get(1).getCertificateNumber());
    }

    @Test
    public void testGetByOrderId_shouldReturnEmptyListForUnknownOrder() {
        List<ComplianceReportAudit> audits = complianceReportAuditDAO.getByOrderId(9999L);

        assertNotNull(audits);
        assertTrue("Unknown order should return empty list", audits.isEmpty());
    }

    @Test
    public void testGetByOrderId_shouldNotReturnAuditsFromOtherOrders() {
        List<ComplianceReportAudit> audits = complianceReportAuditDAO.getByOrderId(1002L);

        assertEquals("Should return only the single audit for order 1002", 1, audits.size());
        assertEquals("LH-2025-00003", audits.get(0).getCertificateNumber());
        assertEquals("FAIL", audits.get(0).getComplianceStatus());
    }

    @Test
    public void testGetMostRecentByOrderId_shouldReturnLatestAuditOnly() {
        ComplianceReportAudit audit = complianceReportAuditDAO.getMostRecentByOrderId(1001L);

        assertNotNull("Should find the most recent audit", audit);
        assertEquals("Most recent certificate number should be returned", "LH-2025-00002",
                audit.getCertificateNumber());
        assertEquals("MARGINAL", audit.getComplianceStatus());
    }

    @Test
    public void testGetMostRecentByOrderId_shouldReturnNullForUnknownOrder() {
        ComplianceReportAudit audit = complianceReportAuditDAO.getMostRecentByOrderId(9999L);

        assertNull("Unknown order should return null", audit);
    }

    @Test
    public void testGetMostRecentByOrderId_shouldReturnSingleAuditWhenOnlyOneExists() {
        ComplianceReportAudit audit = complianceReportAuditDAO.getMostRecentByOrderId(1002L);

        assertNotNull(audit);
        assertEquals("LH-2025-00003", audit.getCertificateNumber());
        assertEquals(Integer.valueOf(5), audit.getParameterCount());
        assertEquals(Integer.valueOf(1), audit.getPassCount());
        assertEquals(Integer.valueOf(3), audit.getFailCount());
    }

    @Test
    public void testGetByOrderId_shouldExposeAllAuditFields() {
        List<ComplianceReportAudit> audits = complianceReportAuditDAO.getByOrderId(1001L);
        ComplianceReportAudit first = audits.get(0); // most recent per DESC order

        assertEquals("orderId should match seeded value", Long.valueOf(1001L), first.getOrderId());
        assertEquals("certificateNumber should be LH-2025-00002", "LH-2025-00002", first.getCertificateNumber());
        assertEquals("generatedBy should be testuser", "testuser", first.getGeneratedBy());
        assertNotNull("generatedAt should be populated", first.getGeneratedAt());
        assertEquals("fileHash should be 64-char hex string", 64, first.getFileHash().length());
        assertEquals("fileSize should be 98304", Long.valueOf(98304L), first.getFileSize());
        assertEquals("complianceStatus should be MARGINAL", "MARGINAL", first.getComplianceStatus());
    }
}
