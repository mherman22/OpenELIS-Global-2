package org.openelisglobal.tenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.FilterChain;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Integration tests for {@link TenantFilterAspect}.
 *
 * <p>
 * Verifies that the Hibernate {@code labUnitFilter} is applied correctly based
 * on the current {@link TenantContext} state. Uses the existing
 * {@code testdata/analysis.xml} dataset which provides two analysis rows in
 * separate samples:
 * <ul>
 * <li>id=1, sampitem_id=1 (sample 1), test_sect_id=1 (TB section)
 * <li>id=2, sampitem_id=2 (sample 2), test_sect_id=2 (TestSection2)
 * </ul>
 *
 * <p>
 * Tests use {@link AnalysisService#getAnalysesBySampleId(String)} which
 * executes a Hibernate HQL query ({@code from Analysis a where
 * a.sampleItem.sample.id = :sampleId}). Hibernate correctly binds named-filter
 * parameters for HQL queries, appending {@code AND test_sect_id = :labUnitId}
 * when the filter is active.
 *
 * <p>
 * Note: {@code Session.get()} / {@code EntityManager.find()} (primary-key
 * lookups) bypass Hibernate named filters by design, so
 * {@code AnalysisService.getAnalysisById()} cannot be used here.
 */
public class TenantFilterAspectIT extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private TenantContextFilter tenantContextFilter;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
    }

    @After
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    public void getAnalysesBySampleId_withNoTenantContext_returnsAllAnalyses() {
        List<Analysis> sample1 = analysisService.getAnalysesBySampleId("1");
        List<Analysis> sample2 = analysisService.getAnalysesBySampleId("2");

        assertEquals("Sample 1 must have 1 analysis with no tenant context", 1, sample1.size());
        assertEquals("Sample 2 must have 1 analysis with no tenant context", 1, sample2.size());
    }

    @Test
    public void getAnalysesBySampleId_withTenantContextSetToSection1_returnsOnlySection1Analysis() {
        TenantContext.set(1);

        List<Analysis> sample1 = analysisService.getAnalysesBySampleId("1");
        List<Analysis> sample2 = analysisService.getAnalysesBySampleId("2");

        assertEquals("Sample 1 analysis (test_sect_id=1) must be visible when filter=1", 1, sample1.size());
        assertTrue("Sample 2 analysis (test_sect_id=2) must be hidden when filter=1", sample2.isEmpty());
    }

    @Test
    public void getAnalysesBySampleId_withTenantContextSetToSection2_returnsOnlySection2Analysis() {
        TenantContext.set(2);

        List<Analysis> sample1 = analysisService.getAnalysesBySampleId("1");
        List<Analysis> sample2 = analysisService.getAnalysesBySampleId("2");

        assertTrue("Sample 1 analysis (test_sect_id=1) must be hidden when filter=2", sample1.isEmpty());
        assertEquals("Sample 2 analysis (test_sect_id=2) must be visible when filter=2", 1, sample2.size());
    }

    @Test
    public void getAnalysesBySampleId_withBypassedTenantContext_returnsAllAnalyses() {
        TenantContext.bypass();

        List<Analysis> sample1 = analysisService.getAnalysesBySampleId("1");
        List<Analysis> sample2 = analysisService.getAnalysesBySampleId("2");

        assertEquals("Admin bypass: sample 1 analysis must be visible", 1, sample1.size());
        assertEquals("Admin bypass: sample 2 analysis must be visible", 1, sample2.size());
    }

    @Test
    public void getAnalysesBySampleId_afterClearingTenantContext_returnsAllAnalyses() {
        TenantContext.set(1);
        TenantContext.clear();

        List<Analysis> sample1 = analysisService.getAnalysesBySampleId("1");
        List<Analysis> sample2 = analysisService.getAnalysesBySampleId("2");

        assertEquals("After clear(), sample 1 analysis must be visible", 1, sample1.size());
        assertEquals("After clear(), sample 2 analysis must be visible", 1, sample2.size());
    }

    @Test
    public void httpPipeline_nonAdminWithLabUnit_filtersData() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Objects.requireNonNull(request.getSession(true)).setAttribute(IActionConstants.USER_SESSION_DATA,
                createSessionData(1, false));

        List<Analysis> capturedSample1 = new ArrayList<>();
        List<Analysis> capturedSample2 = new ArrayList<>();
        AtomicReference<Integer> capturedLabUnitId = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedLabUnitId.set(TenantContext.get());
            capturedSample1.addAll(analysisService.getAnalysesBySampleId("1"));
            capturedSample2.addAll(analysisService.getAnalysesBySampleId("2"));
        };

        tenantContextFilter.doFilter(request, response, chain);

        assertEquals("TenantContext must be set to lab unit 1 during request", Integer.valueOf(1),
                capturedLabUnitId.get());
        assertEquals("Sample 1 (test_sect_id=1) must be visible for lab unit 1", 1, capturedSample1.size());
        assertTrue("Sample 2 (test_sect_id=2) must be hidden for lab unit 1", capturedSample2.isEmpty());
        assertNull("TenantContext must be cleared after filter completes", TenantContext.get());
    }

    @Test
    public void httpPipeline_adminUser_bypassesFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Objects.requireNonNull(request.getSession(true)).setAttribute(IActionConstants.USER_SESSION_DATA,
                createSessionData(1, true));

        List<Analysis> capturedSample1 = new ArrayList<>();
        List<Analysis> capturedSample2 = new ArrayList<>();

        FilterChain chain = (req, res) -> {
            capturedSample1.addAll(analysisService.getAnalysesBySampleId("1"));
            capturedSample2.addAll(analysisService.getAnalysesBySampleId("2"));
        };

        tenantContextFilter.doFilter(request, response, chain);

        assertEquals("Admin: sample 1 must be visible", 1, capturedSample1.size());
        assertEquals("Admin: sample 2 must be visible", 1, capturedSample2.size());
        assertNull("TenantContext must be cleared after filter completes", TenantContext.get());
    }

    @Test
    public void httpPipeline_noSession_noFiltering() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // No session attribute set

        List<Analysis> capturedSample1 = new ArrayList<>();
        List<Analysis> capturedSample2 = new ArrayList<>();

        FilterChain chain = (req, res) -> {
            capturedSample1.addAll(analysisService.getAnalysesBySampleId("1"));
            capturedSample2.addAll(analysisService.getAnalysesBySampleId("2"));
        };

        tenantContextFilter.doFilter(request, response, chain);

        assertEquals("No session: sample 1 must be visible", 1, capturedSample1.size());
        assertEquals("No session: sample 2 must be visible", 1, capturedSample2.size());
    }

    @Test
    public void httpPipeline_labUnitZero_noFiltering() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Objects.requireNonNull(request.getSession(true)).setAttribute(IActionConstants.USER_SESSION_DATA,
                createSessionData(0, false));

        List<Analysis> capturedSample1 = new ArrayList<>();
        List<Analysis> capturedSample2 = new ArrayList<>();

        FilterChain chain = (req, res) -> {
            capturedSample1.addAll(analysisService.getAnalysesBySampleId("1"));
            capturedSample2.addAll(analysisService.getAnalysesBySampleId("2"));
        };

        tenantContextFilter.doFilter(request, response, chain);

        assertEquals("Lab unit 0: sample 1 must be visible", 1, capturedSample1.size());
        assertEquals("Lab unit 0: sample 2 must be visible", 1, capturedSample2.size());
    }

    // For End-to-end HTTP pipeline tests.
    // These drive TenantContextFilter.doFilter() with mock HTTP requests and
    // verify that data isolation flows through the full pipeline:
    // HTTP request → TenantContextFilter → TenantContext → TenantFilterAspect
    // → Hibernate filter → filtered query results → TenantContext cleared.
    private UserSessionData createSessionData(int labUnit, boolean admin) {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        usd.setLoginName("testUser");
        usd.setLoginLabUnit(labUnit);
        usd.setAdmin(admin);
        return usd;
    }
}
