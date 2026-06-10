package org.openelisglobal.compliancereport.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Root DTO class for the S-06 Laporan Hasil feature. All request/response DTOs
 * for compliance reporting are nested here.
 */
@Data
public class OrderPreviewDto {

    private String orderId;
    private String labNumber;
    private String siteName;
    private String collectionDate;
    private String standardName;
    private List<CollectionConditionDto> collectionConditions;
    private List<ParameterResultDto> parameters;
    private List<SignatureDto> signatures;

    // ------------------------------------------------------------------
    // Sub-structures of OrderPreviewDto
    // ------------------------------------------------------------------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionConditionDto {
        private String label;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterResultDto {
        private String name;
        private String value;
        private String unit;
        private String threshold;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignatureDto {
        private String role;
        private String name;
        private String timestamp;
    }

    // ------------------------------------------------------------------
    // Eligible orders list
    // ------------------------------------------------------------------

    @Data
    public static class EligibleOrderDto {
        private String id;
        private String labNumber;
        private String siteName;
        private String standardId;
        private String standardName;
        private String collectionDate;
        private int testCount;
        private int testCountRequired;
        private String complianceStatus;
        private String lastGeneratedAt;
        private String lastCertificateNumber;
        private int parameterCount;
        private int passCount;
        private int marginalCount;
        private int failCount;
    }

    @Data
    public static class EligibleOrdersResponseDto {
        private List<EligibleOrderDto> orders;
        private int totalEligible;
        private int totalGenerated;
        private int totalNotGenerated;
    }

    // ------------------------------------------------------------------
    // Admin report configuration
    // ------------------------------------------------------------------

    @Data
    public static class ReportConfigDto {
        private String labName;
        private String labSubtitle;
        private String addressLine1;
        private String addressLine2;
        private String phone;
        private String email;
        private String website;
        private String labLogoBase64;
        private String accreditationNumber;
        private String accreditationBody;
        private String accreditationLogoBase64;
        private String footerText;
        private boolean showPageNumbers = true;
        private String pageNumberFormat = "Page {page} of {total}";
        private String certificatePrefix = "LH";
        private String dateFormat = "YYYY";
    }
}
