package org.openelisglobal.compliancereport.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.valueholder.ComplianceStatus;
import org.openelisglobal.compliancereport.dao.CertificateNumberSequenceDAO;
import org.openelisglobal.compliancereport.dao.ComplianceReportAuditDAO;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.CollectionConditionDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.EligibleOrderDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.EligibleOrdersResponseDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.ParameterResultDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.SignatureDto;
import org.openelisglobal.compliancereport.pdf.LaporanHasilPdfGenerator;
import org.openelisglobal.compliancereport.pdf.LaporanHasilPdfGenerator.CertificateData;
import org.openelisglobal.compliancereport.valueholder.ComplianceReportAudit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ComplianceReportServiceImpl implements ComplianceReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ComplianceReportAuditDAO auditDAO;

    @Autowired
    private CertificateNumberSequenceDAO certSeqDAO;

    @Autowired
    private ReportConfigService reportConfigService;

    @Autowired
    private LaporanHasilPdfGenerator pdfGenerator;

    @Override
    public EligibleOrdersResponseDto getEligibleOrders(String fromDate, String toDate, String siteId, String standardId,
            String status, String complianceStatus) {

        StringBuilder sql = new StringBuilder(
                """
                        SELECT
                            s.id AS sample_id,
                            s.accession_number,
                            COALESCE(
                                (SELECT oh.value FROM clinlims.observation_history oh
                                 JOIN clinlims.observation_history_type oht ON oht.id = oh.observation_history_type_id
                                 WHERE oh.sample_id = s.id
                                   AND oht.type_name IN ('vecCollectionSiteName', 'envSamplingSiteName')
                                 LIMIT 1),
                                o.name, ''
                            ) AS site_name,
                            CAST(cs.id AS text) AS standard_id,
                            cs.name AS standard_name,
                            CAST(COALESCE(s.collection_date, CAST(s.received_date AS date)) AS text) AS collection_date,
                            (SELECT COUNT(DISTINCT ct2.test_id)
                             FROM clinlims.parameter_group pg2
                             JOIN clinlims.compliance_threshold ct2 ON ct2.group_id = pg2.id
                                 AND ct2.archived = false AND ct2.is_active = true
                                 AND ct2.threshold_type NOT IN ('BORDERLINE','SELECT_MAP')
                             WHERE pg2.standard_id = cs.id) AS test_count_required,
                            (SELECT COUNT(DISTINCT a2.test_id)
                             FROM clinlims.analysis a2
                             JOIN clinlims.sample_item si2 ON a2.sampitem_id = si2.id
                             JOIN clinlims.parameter_group pg2 ON pg2.standard_id = cs.id
                             JOIN clinlims.compliance_threshold ct2 ON ct2.group_id = pg2.id
                                 AND CAST(ct2.test_id AS bigint) = CAST(a2.test_id AS bigint)
                                 AND ct2.archived = false AND ct2.is_active = true
                             WHERE si2.samp_id = s.id) AS test_count_done,
                            cra.certificate_number AS last_cert_num,
                            CAST(cra.generated_at AS text) AS last_generated_at,
                            cra.compliance_status AS last_status,
                            COALESCE(cra.pass_count, 0) AS pass_count,
                            COALESCE(cra.marginal_count, 0) AS marginal_count,
                            COALESCE(cra.fail_count, 0) AS fail_count,
                            COALESCE(cra.parameter_count, 0) AS parameter_count
                        FROM clinlims.sample s
                        INNER JOIN clinlims.sample_compliance_standards scs ON scs.sample_id = s.id
                        INNER JOIN clinlims.compliance_standard cs ON cs.id = scs.compliance_standard_id
                        LEFT JOIN clinlims.sample_requester sr
                            ON sr.sample_id = s.id AND sr.requester_type_id = (SELECT rt.id FROM clinlims.requester_type rt WHERE rt.requester_type = 'organization')
                        LEFT JOIN clinlims.organization o ON o.id = sr.requester_id
                        LEFT JOIN LATERAL (
                            SELECT certificate_number, generated_at, compliance_status,
                                   pass_count, marginal_count, fail_count, parameter_count
                            FROM clinlims.compliance_report_audit
                            WHERE order_id = s.id
                            ORDER BY generated_at DESC
                            LIMIT 1
                        ) cra ON true
                        WHERE NOT EXISTS (
                            SELECT 1 FROM clinlims.analysis a2
                            JOIN clinlims.sample_item si2 ON a2.sampitem_id = si2.id
                            WHERE si2.samp_id = s.id
                            AND a2.status_id NOT IN (
                                SELECT id FROM clinlims.status_of_sample
                                WHERE name = 'Finalized' AND status_type = 'ANALYSIS'
                            )
                        )
                        AND EXISTS (
                            SELECT 1 FROM clinlims.analysis a3
                            JOIN clinlims.sample_item si3 ON a3.sampitem_id = si3.id
                            WHERE si3.samp_id = s.id
                        )
                        """);

        Map<String, Object> params = new HashMap<>();

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND COALESCE(s.collection_date, CAST(s.received_date AS date)) >= CAST(:fromDate AS date)");
            params.put("fromDate", fromDate);
        }
        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND COALESCE(s.collection_date, CAST(s.received_date AS date)) <= CAST(:toDate AS date)");
            params.put("toDate", toDate);
        }
        if (siteId != null && !siteId.isBlank()) {
            sql.append("""
                     AND EXISTS (
                        SELECT 1 FROM clinlims.observation_history oh
                        JOIN clinlims.observation_history_type oht ON oht.id = oh.observation_history_type_id
                        WHERE oh.sample_id = s.id
                        AND oht.type_name = 'vecCollectionSiteId'
                        AND oh.value = CAST(:siteId AS text)
                    )""");
            params.put("siteId", siteId);
        }
        if (standardId != null && !standardId.isBlank()) {
            sql.append(" AND CAST(cs.id AS text) = :standardId");
            params.put("standardId", standardId);
        }
        if (complianceStatus != null && !complianceStatus.isBlank()) {
            sql.append(" AND cra.compliance_status = :complianceStatus");
            params.put("complianceStatus", complianceStatus);
        }

        // No aggregate functions remain — GROUP BY not needed

        if ("generated".equalsIgnoreCase(status)) {
            sql.append(" HAVING cra.certificate_number IS NOT NULL");
        } else if ("notGenerated".equalsIgnoreCase(status)) {
            sql.append(" HAVING cra.certificate_number IS NULL");
        }

        sql.append(" ORDER BY s.collection_date DESC, s.accession_number");

        try {
            NativeQuery<?> query = entityManager.unwrap(Session.class).createNativeQuery(sql.toString());
            params.forEach(query::setParameter);
            List<?> rows = query.list();

            List<EligibleOrderDto> orders = new ArrayList<>();
            for (Object raw : rows) {
                Object[] row = (Object[]) raw;
                EligibleOrderDto dto = mapRowToDto(row);
                orders.add(dto);
            }

            int totalGenerated = (int) orders.stream()
                    .filter(o -> o.getLastCertificateNumber() != null && !o.getLastCertificateNumber().isBlank())
                    .count();

            EligibleOrdersResponseDto response = new EligibleOrdersResponseDto();
            response.setOrders(orders);
            response.setTotalEligible(orders.size());
            response.setTotalGenerated(totalGenerated);
            response.setTotalNotGenerated(orders.size() - totalGenerated);
            return response;

        } catch (Exception e) {
            LogEvent.logError(e);
            EligibleOrdersResponseDto empty = new EligibleOrdersResponseDto();
            empty.setOrders(new ArrayList<>());
            return empty;
        }
    }

    private EligibleOrderDto mapRowToDto(Object[] row) {
        EligibleOrderDto dto = new EligibleOrderDto();
        dto.setId(String.valueOf(row[0]));
        dto.setLabNumber(String.valueOf(row[1]));
        dto.setSiteName(row[2] != null ? String.valueOf(row[2]) : "");
        dto.setStandardId(row[3] != null ? String.valueOf(row[3]) : "");
        dto.setStandardName(row[4] != null ? String.valueOf(row[4]) : "");
        dto.setCollectionDate(row[5] != null ? String.valueOf(row[5]) : "");
        dto.setTestCountRequired(row[6] != null ? ((Number) row[6]).intValue() : 0);
        dto.setTestCount(row[7] != null ? ((Number) row[7]).intValue() : 0);
        dto.setLastCertificateNumber(row[8] != null ? String.valueOf(row[8]) : null);
        dto.setLastGeneratedAt(row[9] != null ? String.valueOf(row[9]) : null);
        String lastStatus = row[10] != null ? String.valueOf(row[10]) : null;
        dto.setComplianceStatus(lastStatus != null ? lastStatus : "UNKNOWN");
        dto.setPassCount(row[11] != null ? ((Number) row[11]).intValue() : 0);
        dto.setMarginalCount(row[12] != null ? ((Number) row[12]).intValue() : 0);
        dto.setFailCount(row[13] != null ? ((Number) row[13]).intValue() : 0);
        dto.setParameterCount(row[14] != null ? ((Number) row[14]).intValue() : 0);
        return dto;
    }

    @Override
    public OrderPreviewDto getOrderPreview(String orderId) {
        try {
            OrderPreviewDto preview = new OrderPreviewDto();
            preview.setOrderId(orderId);

            // Basic order info
            String orderSql = """
                    SELECT s.accession_number,
                           COALESCE(
                               (SELECT oh.value FROM clinlims.observation_history oh
                                JOIN clinlims.observation_history_type oht ON oht.id = oh.observation_history_type_id
                                WHERE oh.sample_id = s.id
                                  AND oht.type_name IN ('vecCollectionSiteName', 'envSamplingSiteName')
                                LIMIT 1),
                               o.name, ''
                           ) AS site_name,
                           CAST(COALESCE(s.collection_date, CAST(s.received_date AS date)) AS text),
                           cs.name AS standard_name,
                           CAST(cs.id AS text) AS standard_id
                    FROM clinlims.sample s
                    INNER JOIN clinlims.sample_compliance_standards scs ON scs.sample_id = s.id
                    INNER JOIN clinlims.compliance_standard cs ON cs.id = scs.compliance_standard_id
                    LEFT JOIN clinlims.sample_requester sr
                        ON sr.sample_id = s.id AND sr.requester_type_id = (SELECT rt.id FROM clinlims.requester_type rt WHERE rt.requester_type = 'organization')
                    LEFT JOIN clinlims.organization o ON o.id = sr.requester_id
                    WHERE s.id = CAST(:orderId AS integer)
                    LIMIT 1
                    """;
            List<?> orderRows = entityManager.unwrap(Session.class).createNativeQuery(orderSql)
                    .setParameter("orderId", orderId).list();

            String standardId = null;
            if (!orderRows.isEmpty()) {
                Object[] row = (Object[]) orderRows.get(0);
                preview.setLabNumber(String.valueOf(row[0]));
                preview.setSiteName(row[1] != null ? String.valueOf(row[1]) : "");
                preview.setCollectionDate(row[2] != null ? String.valueOf(row[2]) : "");
                preview.setStandardName(row[3] != null ? String.valueOf(row[3]) : "");
                standardId = row[4] != null ? String.valueOf(row[4]) : null;
            }

            // Collection conditions from observation history
            preview.setCollectionConditions(loadCollectionConditions(orderId));

            // Parameter results
            preview.setParameters(loadParameterResults(orderId, standardId));

            // Electronic signatures
            preview.setSignatures(loadSignatures(orderId));

            return preview;
        } catch (Exception e) {
            LogEvent.logError(e);
            return new OrderPreviewDto();
        }
    }

    private List<CollectionConditionDto> loadCollectionConditions(String orderId) {
        List<CollectionConditionDto> conditions = new ArrayList<>();
        try {
            String sql = """
                    SELECT oht.type_name, oh.value
                    FROM clinlims.observation_history oh
                    JOIN clinlims.observation_history_type oht ON oht.id = oh.observation_history_type_id
                    WHERE oh.sample_id = CAST(:orderId AS integer)
                    AND oht.type_name IN ('envWaterTemp','envAirTemp','envWeather',
                                          'envPreservationMethod','envFieldNotes')
                    ORDER BY oht.type_name
                    """;
            List<?> rows = entityManager.unwrap(Session.class).createNativeQuery(sql).setParameter("orderId", orderId)
                    .list();
            for (Object raw : rows) {
                Object[] row = (Object[]) raw;
                String label = humanizeConditionKey(String.valueOf(row[0]));
                String value = row[1] != null ? String.valueOf(row[1]) : "";
                conditions.add(new CollectionConditionDto(label, value));
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }
        return conditions;
    }

    private String humanizeConditionKey(String typeKey) {
        switch (typeKey) {
        case "envWaterTemp":
            return "Water Temperature";
        case "envAirTemp":
            return "Air Temperature";
        case "envWeather":
            return "Weather";
        case "envPreservationMethod":
            return "Preservation Method";
        case "envFieldNotes":
            return "Field Notes";
        default:
            return typeKey;
        }
    }

    private List<ParameterResultDto> loadParameterResults(String orderId, String standardId) {
        List<ParameterResultDto> results = new ArrayList<>();
        if (standardId == null)
            return results;
        try {
            // Enumerate from the standard's required tests outward so ALL required
            // tests appear — ones without an analysis on this order show as "Not Tested".
            String sql = """
                    SELECT DISTINCT ON (t.id)
                        t.name AS test_name,
                        ord.result_value AS result_value,
                        COALESCE(ct.units, uom.name, '') AS unit,
                        CASE WHEN ct.threshold_type IN ('RANGE','MINIMUM') THEN ct.min_value ELSE NULL END AS low_normal,
                        CASE WHEN ct.threshold_type IN ('RANGE','MAXIMUM') THEN ct.max_value ELSE NULL END AS high_normal,
                        ct_bl.min_value AS low_valid,
                        ct_bl.max_value AS high_valid,
                        ct.threshold_type,
                        ct.value_descriptive
                    FROM clinlims.parameter_group pg
                    JOIN clinlims.compliance_threshold ct
                        ON ct.group_id = pg.id
                        AND ct.threshold_type NOT IN ('BORDERLINE','SELECT_MAP')
                        AND ct.archived = false AND ct.is_active = true
                    JOIN clinlims.test t ON CAST(ct.test_id AS bigint) = t.id
                    LEFT JOIN clinlims.compliance_threshold ct_bl
                        ON ct_bl.group_id = pg.id
                        AND CAST(ct_bl.test_id AS bigint) = CAST(ct.test_id AS bigint)
                        AND ct_bl.threshold_type = 'BORDERLINE'
                        AND ct_bl.archived = false AND ct_bl.is_active = true
                    LEFT JOIN clinlims.unit_of_measure uom ON uom.id = t.uom_id
                    LEFT JOIN (
                        SELECT a.test_id,
                               CASE WHEN r.value IS NOT NULL AND r.value <> '' THEN r.value ELSE NULL END AS result_value
                        FROM clinlims.analysis a
                        JOIN clinlims.sample_item si ON a.sampitem_id = si.id
                        LEFT JOIN clinlims.result r ON r.analysis_id = a.id AND r.is_reportable = 'Y'
                        WHERE si.samp_id = CAST(:orderId AS integer)
                    ) ord ON CAST(ord.test_id AS bigint) = CAST(ct.test_id AS bigint)
                    WHERE pg.standard_id = CAST(:standardId AS integer)
                    ORDER BY t.id, pg.sort_order, t.sort_order, t.name
                    """;
            List<?> rows = entityManager.unwrap(Session.class).createNativeQuery(sql).setParameter("orderId", orderId)
                    .setParameter("standardId", standardId).list();

            for (Object raw : rows) {
                Object[] row = (Object[]) raw;
                String testName = String.valueOf(row[0]);
                boolean hasResult = row[1] != null && !String.valueOf(row[1]).isBlank();
                String value = hasResult ? String.valueOf(row[1]) : "—";
                String unit = row[2] != null ? String.valueOf(row[2]) : "";
                Double lowNormal = row[3] != null ? ((Number) row[3]).doubleValue() : null;
                Double highNormal = row[4] != null ? ((Number) row[4]).doubleValue() : null;
                Double lowValid = row[5] != null ? ((Number) row[5]).doubleValue() : null;
                Double highValid = row[6] != null ? ((Number) row[6]).doubleValue() : null;
                String thresholdType = row[7] != null ? String.valueOf(row[7]) : null;
                String valueDescriptive = row[8] != null ? String.valueOf(row[8]) : null;

                String threshold = buildThresholdDisplay(lowNormal, highNormal, unit, thresholdType, valueDescriptive);
                // row[1] being NULL means the test was not performed on this order at all
                String status = row[1] == null ? "NOT_TESTED"
                        : computeStatus(value, lowNormal, highNormal, lowValid, highValid, thresholdType);

                results.add(new ParameterResultDto(testName, value, unit, threshold, status));
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }
        return results;
    }

    private String buildThresholdDisplay(Double low, Double high, String unit, String thresholdType,
            String valueDescriptive) {
        if ("DESCRIPTIVE".equals(thresholdType))
            return valueDescriptive != null && !valueDescriptive.isBlank() ? valueDescriptive : "—";
        if (low == null && high == null)
            return "—";
        StringBuilder sb = new StringBuilder();
        if (low == null) {
            sb.append("≤ ").append(formatNum(high));
        } else if (high == null) {
            sb.append("≥ ").append(formatNum(low));
        } else {
            sb.append(formatNum(low)).append(" – ").append(formatNum(high));
        }
        if (unit != null && !unit.isBlank())
            sb.append(" ").append(unit);
        return sb.toString();
    }

    private String formatNum(Double d) {
        return d == d.longValue() ? String.valueOf(d.longValue()) : String.valueOf(d);
    }

    private String computeStatus(String rawValue, Double lowNormal, Double highNormal, Double lowValid,
            Double highValid, String thresholdType) {
        if ("DESCRIPTIVE".equals(thresholdType))
            return (rawValue != null && !rawValue.isBlank() && !rawValue.equals("—")) ? "PASS" : "UNKNOWN";
        if (rawValue == null || rawValue.isBlank() || rawValue.equals("—"))
            return "UNKNOWN";
        if (lowNormal == null && highNormal == null)
            return "UNKNOWN";
        try {
            double v = Double.parseDouble(rawValue.trim());
            boolean inNormal = (lowNormal == null || v >= lowNormal) && (highNormal == null || v <= highNormal);
            if (inNormal)
                return "PASS";
            boolean inValid = (lowValid == null || v >= lowValid) && (highValid == null || v <= highValid);
            return inValid ? "MARGINAL" : "FAIL";
        } catch (NumberFormatException e) {
            return "UNKNOWN";
        }
    }

    private List<SignatureDto> loadSignatures(String orderId) {
        List<SignatureDto> signatures = new ArrayList<>();
        try {
            String sql = """
                    SELECT es.signature_meaning, es.signer_name_printed,
                           CAST(es.signed_at AS text)
                    FROM clinlims.electronic_signature es
                    WHERE es.record_type = 'Sample' AND es.record_id = CAST(:orderId AS bigint)
                    ORDER BY es.signed_at
                    """;
            List<?> rows = entityManager.unwrap(Session.class).createNativeQuery(sql).setParameter("orderId", orderId)
                    .list();
            for (Object raw : rows) {
                Object[] row = (Object[]) raw;
                String meaning = String.valueOf(row[0]);
                String name = row[1] != null ? String.valueOf(row[1]) : "";
                String timestamp = row[2] != null ? String.valueOf(row[2]) : "";
                signatures.add(new SignatureDto(meaning, name, timestamp));
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }
        return signatures;
    }

    @Override
    @Transactional
    public byte[] generateReport(String orderId, String sysUserId, Locale locale) {
        OrderPreviewDto preview = getOrderPreview(orderId);
        String certNumber = allocateCertificateNumber(sysUserId);

        CertificateData data = buildCertificateData(preview, certNumber);
        byte[] pdfBytes = pdfGenerator.generate(data, locale);

        recordAudit(orderId, certNumber, sysUserId, pdfBytes, preview.getParameters());
        return pdfBytes;
    }

    @Override
    @Transactional
    public byte[] generateBatch(List<String> orderIds, String sysUserId, Locale locale) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String orderId : orderIds) {
                byte[] pdf = generateReport(orderId, sysUserId, locale);
                String entryName = "LaporanHasil_" + orderId + ".pdf";
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(pdf);
                zos.closeEntry();
            }
        } catch (IOException e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to create batch ZIP", e);
        }
        return baos.toByteArray();
    }

    private String allocateCertificateNumber(String sysUserId) {
        String prefix = reportConfigService.getConfig().getCertificatePrefix();
        int year = LocalDate.now().getYear();
        // Use 0L as the global-sequence sentinel: NULL breaks ON CONFLICT in PostgreSQL
        // because NULL != NULL in unique-index comparisons, so the counter never
        // increments.
        int seq = certSeqDAO.getAndIncrementNextNumber(0L, year);
        return String.format("%s-%d-%06d", prefix, year, seq);
    }

    private CertificateData buildCertificateData(OrderPreviewDto preview, String certNumber) {
        CertificateData data = new CertificateData();
        data.setCertificateNumber(certNumber);
        data.setGeneratedAt(new Timestamp(System.currentTimeMillis()).toString());

        var config = reportConfigService.getConfig();
        data.setLabName(config.getLabName());
        data.setLabSubtitle(config.getLabSubtitle());
        data.setAddressLine1(config.getAddressLine1());
        data.setAddressLine2(config.getAddressLine2());
        data.setPhone(config.getPhone());
        data.setLabLogoBase64(config.getLabLogoBase64());
        data.setAccreditationNumber(config.getAccreditationNumber());
        data.setAccreditationBody(config.getAccreditationBody());
        data.setAccreditationLogoBase64(config.getAccreditationLogoBase64());
        data.setFooterText(config.getFooterText());
        data.setShowPageNumbers(config.isShowPageNumbers());
        data.setPageNumberFormat(config.getPageNumberFormat());

        data.setLabNumber(preview.getLabNumber());
        data.setSiteName(preview.getSiteName());
        data.setStandardName(preview.getStandardName());
        data.setCollectionDate(preview.getCollectionDate());
        data.setCollectionConditions(preview.getCollectionConditions());
        data.setParameters(preview.getParameters());
        data.setSignatures(preview.getSignatures());
        return data;
    }

    private void recordAudit(String orderId, String certNumber, String sysUserId, byte[] pdfBytes,
            List<ParameterResultDto> parameters) {

        int passCount = 0, marginalCount = 0, failCount = 0, paramCount = 0;
        for (var p : parameters) {
            String s = p.getStatus();
            if ("UNKNOWN".equals(s))
                continue;
            paramCount++;
            if ("PASS".equals(s))
                passCount++;
            else if ("MARGINAL".equals(s))
                marginalCount++;
            else if ("FAIL".equals(s))
                failCount++;
        }

        String overallStatus;
        if (failCount > 0)
            overallStatus = ComplianceStatus.NON_COMPLIANT.name();
        else if (marginalCount > 0)
            overallStatus = ComplianceStatus.BORDERLINE.name();
        else if (passCount > 0)
            overallStatus = ComplianceStatus.COMPLIANT.name();
        else
            overallStatus = "UNKNOWN";

        String fileHash = sha256Hex(pdfBytes);

        ComplianceReportAudit audit = new ComplianceReportAudit();
        audit.setOrderId(Long.valueOf(orderId));
        audit.setCertificateNumber(certNumber);
        audit.setGeneratedBy(sysUserId);
        audit.setGeneratedAt(new Timestamp(System.currentTimeMillis()));
        audit.setFileHash(fileHash);
        audit.setFileSize((long) pdfBytes.length);
        audit.setComplianceStatus(overallStatus);
        audit.setParameterCount(paramCount);
        audit.setPassCount(passCount);
        audit.setMarginalCount(marginalCount);
        audit.setFailCount(failCount);
        audit.setSysUserId(sysUserId);
        auditDAO.insert(audit);
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash)
                hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("sites", loadSiteOptions());
        return options;
    }

    private List<Map<String, String>> loadSiteOptions() {
        List<Map<String, String>> list = new ArrayList<>();
        try {
            String sql = """
                    SELECT DISTINCT CAST(o.id AS text), o.name
                    FROM clinlims.organization o
                    JOIN clinlims.sample_requester sr
                        ON sr.requester_id = o.id AND sr.requester_type_id = (SELECT rt.id FROM clinlims.requester_type rt WHERE rt.requester_type = 'organization')
                    JOIN clinlims.sample_compliance_standards scs ON scs.sample_id = sr.sample_id
                    ORDER BY o.name
                    """;
            List<?> rows = entityManager.unwrap(Session.class).createNativeQuery(sql).list();
            for (Object raw : rows) {
                Object[] row = (Object[]) raw;
                Map<String, String> opt = new LinkedHashMap<>();
                opt.put("id", String.valueOf(row[0]));
                opt.put("name", String.valueOf(row[1]));
                list.add(opt);
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }
        return list;
    }

}
