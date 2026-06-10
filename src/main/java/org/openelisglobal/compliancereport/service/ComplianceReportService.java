package org.openelisglobal.compliancereport.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.EligibleOrdersResponseDto;

public interface ComplianceReportService {

    EligibleOrdersResponseDto getEligibleOrders(String fromDate, String toDate, String siteId, String standardId,
            String status, String complianceStatus);

    OrderPreviewDto getOrderPreview(String orderId);

    byte[] generateReport(String orderId, String sysUserId, Locale locale);

    byte[] generateBatch(List<String> orderIds, String sysUserId, Locale locale);

    Map<String, Object> getFilterOptions();
}
