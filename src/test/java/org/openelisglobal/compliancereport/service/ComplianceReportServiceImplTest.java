package org.openelisglobal.compliancereport.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.EligibleOrdersResponseDto;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * SQL smoke tests for {@link ComplianceReportServiceImpl}.
 *
 * <p>
 * These tests do NOT assert business logic. Their sole purpose is to execute
 * each native SQL query against a real PostgreSQL schema (via Testcontainers)
 * so that column-name typos, wrong table aliases, and invalid SQL syntax are
 * caught at test time rather than at runtime in production.
 *
 * <p>
 * No domain seed data is required: the queries are designed to return empty
 * results when the database is empty, and an empty-but-valid result proves the
 * SQL is correct.
 */
public class ComplianceReportServiceImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ComplianceReportService complianceReportService;

    @Test
    public void testGetEligibleOrders_noFilters_sqlExecutesWithoutError() {
        EligibleOrdersResponseDto result = complianceReportService.getEligibleOrders(null, null, null, null, null,
                null);

        assertNotNull("Response must not be null", result);
        assertNotNull("Orders list must not be null", result.getOrders());
    }

    @Test
    public void testGetEligibleOrders_allFiltersSet_sqlExecutesWithoutError() {
        EligibleOrdersResponseDto result = complianceReportService.getEligibleOrders("2020-01-01", "2025-12-31", "1",
                "1", "generated", "COMPLIANT");

        assertNotNull(result);
        assertNotNull(result.getOrders());
    }

    @Test
    public void testGetEligibleOrders_statusFilter_notGenerated_sqlExecutesWithoutError() {
        EligibleOrdersResponseDto result = complianceReportService.getEligibleOrders(null, null, null, null,
                "notGenerated", null);

        assertNotNull(result);
        assertNotNull(result.getOrders());
    }

    @Test
    public void testGetOrderPreview_nonExistentOrder_returnsEmptyDtoWithoutError() {
        OrderPreviewDto preview = complianceReportService.getOrderPreview("99999");

        assertNotNull("Preview must not be null even for unknown order", preview);
        // inner collections must be non-null to avoid NPE in callers
        assertNotNull(preview.getCollectionConditions());
        assertNotNull(preview.getParameters());
        assertNotNull(preview.getSignatures());
    }

    @Test
    public void testGetFilterOptions_sqlExecutesWithoutError() {
        Map<String, Object> options = complianceReportService.getFilterOptions();

        assertNotNull("Options map must not be null", options);
        assertTrue("Options must contain 'sites' key", options.containsKey("sites"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> sites = (List<Map<String, String>>) options.get("sites");
        assertNotNull("Sites list must not be null", sites);
    }
}
