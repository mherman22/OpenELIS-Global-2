package org.openelisglobal.compliancereport.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.compliance.valueholder.ComplianceStatus;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.EligibleOrdersResponseDto;
import org.openelisglobal.compliancereport.service.ComplianceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/compliance/reports")
@PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'RECEPTION', 'RESULTS', 'LIMITED_RESULTS')")
public class ComplianceReportRestController extends BaseRestController {

    @Autowired
    private ComplianceReportService complianceReportService;

    @GetMapping("/eligible-orders")
    public ResponseEntity<EligibleOrdersResponseDto> getEligibleOrders(@RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate, @RequestParam(required = false) String siteId,
            @RequestParam(required = false) String standardId, @RequestParam(required = false) String status,
            @RequestParam(required = false) String complianceStatus) {
        try {
            EligibleOrdersResponseDto response = complianceReportService.getEligibleOrders(fromDate, toDate, siteId,
                    standardId, status, complianceStatus);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/eligible-orders/{id}/preview")
    public ResponseEntity<OrderPreviewDto> getOrderPreview(@PathVariable String id) {
        try {
            OrderPreviewDto preview = complianceReportService.getOrderPreview(id);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/generate")
    public void generateReport(@RequestBody Map<String, String> request, HttpServletRequest httpRequest,
            HttpServletResponse response) throws IOException {
        String orderId = request.get("orderId");
        if (orderId == null || orderId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            String sysUserId = getSysUserId(httpRequest);
            Locale locale = httpRequest.getLocale();
            byte[] pdf = complianceReportService.generateReport(orderId, sysUserId, locale);

            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"LaporanHasil_" + orderId + ".pdf\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
            response.getOutputStream().flush();
        } catch (Exception e) {
            LogEvent.logError(e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/generate-batch")
    public void generateBatch(@RequestBody Map<String, List<String>> request, HttpServletRequest httpRequest,
            HttpServletResponse response) throws IOException {
        List<String> orderIds = request.get("orderIds");
        if (orderIds == null || orderIds.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            String sysUserId = getSysUserId(httpRequest);
            Locale locale = httpRequest.getLocale();
            byte[] zip = complianceReportService.generateBatch(orderIds, sysUserId, locale);

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"LaporanHasil_batch.zip\"");
            response.setContentLength(zip.length);
            response.getOutputStream().write(zip);
            response.getOutputStream().flush();
        } catch (Exception e) {
            LogEvent.logError(e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/compliance-statuses")
    public ResponseEntity<List<Map<String, String>>> getComplianceStatuses() {
        List<Map<String, String>> statuses = Arrays.stream(ComplianceStatus.values()).map(s -> {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", s.name());
            entry.put("displayName", s.getDisplayName());
            return entry;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/filter-options")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        try {
            Map<String, Object> options = complianceReportService.getFilterOptions();
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
