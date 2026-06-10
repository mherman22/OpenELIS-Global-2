package org.openelisglobal.compliancereport.daoimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliancereport.dao.CertificateNumberSequenceDAO;
import org.openelisglobal.compliancereport.valueholder.CertificateNumberSequence;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link CertificateNumberSequenceDAOImpl}.
 *
 * <p>
 * Verifies site/year isolation, lookup of existing sequences, and the atomic
 * increment-or-insert used during certificate number allocation. Test data is
 * loaded from {@code testdata/compliance_report_audit.xml} which seeds two
 * sequence rows (site 1 at 3, site 2 at 10).
 */
public class CertificateNumberSequenceDAOImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CertificateNumberSequenceDAO certificateNumberSequenceDAO;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance_report_audit.xml");
    }

    @Test
    public void testFindBySiteIdAndYear_shouldReturnExistingSequence() {
        Optional<CertificateNumberSequence> result = certificateNumberSequenceDAO.findBySiteIdAndYear(1L, 2025);

        assertTrue("Should find the sequence seeded for site 1 / 2025", result.isPresent());
        assertEquals(Integer.valueOf(3), result.get().getLastNumber());
    }

    @Test
    public void testFindBySiteIdAndYear_shouldReturnEmptyForUnknownSite() {
        Optional<CertificateNumberSequence> result = certificateNumberSequenceDAO.findBySiteIdAndYear(99L, 2025);

        assertFalse("Unknown site should return empty Optional", result.isPresent());
    }

    @Test
    public void testFindBySiteIdAndYear_shouldReturnEmptyForUnknownYear() {
        Optional<CertificateNumberSequence> result = certificateNumberSequenceDAO.findBySiteIdAndYear(1L, 1990);

        assertFalse("Unknown year should return empty Optional", result.isPresent());
    }

    @Test
    public void testFindBySiteIdAndYear_shouldIsolateBySite() {
        Optional<CertificateNumberSequence> site1 = certificateNumberSequenceDAO.findBySiteIdAndYear(1L, 2025);
        Optional<CertificateNumberSequence> site2 = certificateNumberSequenceDAO.findBySiteIdAndYear(2L, 2025);

        assertTrue(site1.isPresent());
        assertTrue(site2.isPresent());
        assertEquals("Site 1 last_number should be 3", Integer.valueOf(3), site1.get().getLastNumber());
        assertEquals("Site 2 last_number should be 10", Integer.valueOf(10), site2.get().getLastNumber());
    }

    @Test
    public void testGetAndIncrementNextNumber_shouldIncrementExistingRow() {
        int number = certificateNumberSequenceDAO.getAndIncrementNextNumber(1L, 2025);

        assertEquals("Increment of 3 should yield 4", 4, number);

        Optional<CertificateNumberSequence> seq = certificateNumberSequenceDAO.findBySiteIdAndYear(1L, 2025);
        assertTrue(seq.isPresent());
        assertEquals("Persisted last_number should reflect increment", Integer.valueOf(4), seq.get().getLastNumber());
    }

    @Test
    public void testGetAndIncrementNextNumber_shouldNotAffectOtherSites() {
        certificateNumberSequenceDAO.getAndIncrementNextNumber(1L, 2025);

        Optional<CertificateNumberSequence> site2 = certificateNumberSequenceDAO.findBySiteIdAndYear(2L, 2025);
        assertTrue(site2.isPresent());
        assertEquals("Site 2 should be unaffected by site 1 increment", Integer.valueOf(10),
                site2.get().getLastNumber());
    }

    @Test
    public void testGetAndIncrementNextNumber_shouldCreateNewSequenceForNewSite() {
        int number = certificateNumberSequenceDAO.getAndIncrementNextNumber(99L, 2025);

        assertEquals("New sequence should start at 1", 1, number);

        Optional<CertificateNumberSequence> created = certificateNumberSequenceDAO.findBySiteIdAndYear(99L, 2025);
        assertTrue("New sequence row should be findable after creation", created.isPresent());
        assertNotNull(created.get().getId());
        assertEquals(Integer.valueOf(1), created.get().getLastNumber());
    }

    @Test
    public void testGetAndIncrementNextNumber_shouldBeMonotonicallyIncreasing() {
        int first = certificateNumberSequenceDAO.getAndIncrementNextNumber(1L, 2025);
        int second = certificateNumberSequenceDAO.getAndIncrementNextNumber(1L, 2025);

        assertTrue("Each call should yield a strictly higher number", second > first);
        assertEquals("Consecutive increments should differ by 1", 1, second - first);
    }
}
