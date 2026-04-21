package org.openelisglobal.compliance.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.compliance.service.ComplianceStandardService;
import org.openelisglobal.compliance.service.TestComplianceStandardService;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandardStatus;
import org.openelisglobal.compliance.valueholder.TestComplianceStandard;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Test-Compliance Standard associations.
 *
 * Handles the management of relationships between Tests and Compliance Standards
 * for the Test Catalog Compliance tab functionality.
 *
 * Constitutional compliance:
 * - Uses REST API pattern (/rest mapping)
 * - @PreAuthorize for role-based access control
 * - Extends BaseController for standard OpenELIS patterns
 * - Proper error handling and validation
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER')")
public class TestComplianceStandardRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] {
        "testId",
        "complianceStandardId",
        "mandatory",
        "applicableParameters"
    };

    @Autowired
    private TestComplianceStandardService testComplianceStandardService;

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private TestService testService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    /**
     * Get all compliance standards associated with a test
     */
    @GetMapping(value = "/test/{testId}/compliance-standards", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER', 'COMPLIANCE_VIEWER')")
    public ResponseEntity<?> getComplianceStandardsForTest(@PathVariable String testId) {
        try {
            List<TestComplianceStandard> associations =
                testComplianceStandardService.getComplianceStandardsForTestOrdered(testId);

            List<TestComplianceStandardDTO> dtoList = new ArrayList<>();
            for (TestComplianceStandard association : associations) {
                dtoList.add(convertToDTO(association));
            }

            return ResponseEntity.ok(dtoList);

        } catch (Exception e) {
            LogEvent.logError("TestComplianceStandardRestController", "getComplianceStandardsForTest",
                "Error retrieving compliance standards for test " + testId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to retrieve compliance standards");
        }
    }

    /**
     * Get all available compliance standards that can be associated with tests
     */
    @GetMapping(value = "/available-compliance-standards", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER', 'COMPLIANCE_VIEWER')")
    public ResponseEntity<?> getAvailableComplianceStandards() {
        try {
            List<ComplianceStandard> standards = complianceStandardService.getActiveComplianceStandards();

            List<ComplianceStandardSummaryDTO> summaryList = new ArrayList<>();
            for (ComplianceStandard standard : standards) {
                ComplianceStandardSummaryDTO summary = new ComplianceStandardSummaryDTO();
                summary.setId(standard.getId());
                summary.setName(standard.getName());
                summary.setDisplayName(standard.getDisplayName());
                summary.setRegulationNumber(standard.getRegulationNumber());
                summary.setVersion(standard.getVersion());
                summary.setIssuingBody(standard.getIssuingBody());
                summary.setStatus(standard.getStatus().toString());
                summaryList.add(summary);
            }

            return ResponseEntity.ok(summaryList);

        } catch (Exception e) {
            LogEvent.logError("TestComplianceStandardRestController", "getAvailableComplianceStandards",
                "Error retrieving available compliance standards: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to retrieve available compliance standards");
        }
    }

    /**
     * Associate a test with a compliance standard
     */
    @PostMapping(value = "/test/{testId}/compliance-standards", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER')")
    public ResponseEntity<?> associateTestWithComplianceStandard(
            @PathVariable String testId,
            @RequestBody TestComplianceStandardDTO associationDTO,
            HttpServletRequest request) {

        try {
            // Validate input
            Errors errors = validateAssociation(associationDTO, testId);
            if (errors.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }

            String currentUser = getSysUserId(request);
            TestComplianceStandard association = testComplianceStandardService.associateTestWithComplianceStandard(
                testId,
                associationDTO.getComplianceStandardId(),
                associationDTO.isMandatory(),
                associationDTO.getApplicableParameters(),
                currentUser
            );

            return ResponseEntity.ok(convertToDTO(association));

        } catch (Exception e) {
            LogEvent.logError("TestComplianceStandardRestController", "associateTestWithComplianceStandard",
                "Error associating test " + testId + " with compliance standard: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to create association: " + e.getMessage());
        }
    }

    /**
     * Remove association between test and compliance standard
     */
    @PostMapping(value = "/test/{testId}/compliance-standards/{standardId}/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER')")
    public ResponseEntity<?> removeTestComplianceStandardAssociation(
            @PathVariable String testId,
            @PathVariable String standardId) {

        try {
            boolean removed = testComplianceStandardService.disassociateTestFromComplianceStandard(testId, standardId);

            if (removed) {
                return ResponseEntity.ok("Association removed successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Association not found");
            }

        } catch (Exception e) {
            LogEvent.logError("TestComplianceStandardRestController", "removeTestComplianceStandardAssociation",
                "Error removing association between test " + testId + " and compliance standard " + standardId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to remove association: " + e.getMessage());
        }
    }

    /**
     * Update multiple test-compliance standard associations
     */
    @PostMapping(value = "/test/{testId}/compliance-standards/bulk-update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER')")
    public ResponseEntity<?> updateTestComplianceStandards(
            @PathVariable String testId,
            @RequestBody List<TestComplianceStandardDTO> associationDTOs,
            HttpServletRequest request) {

        try {
            // Validate test exists
            Test test = testService.get(testId);
            if (test == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Test not found: " + testId);
            }

            // Convert DTOs to entities
            List<TestComplianceStandard> associations = new ArrayList<>();
            List<ComplianceStandard> newStandards = new ArrayList<>();

            for (TestComplianceStandardDTO dto : associationDTOs) {
                TestComplianceStandard association = convertFromDTO(dto, testId);
                associations.add(association);

                ComplianceStandard standard = complianceStandardService.get(dto.getComplianceStandardId());
                if (standard != null) {
                    newStandards.add(standard);
                }
            }

            String currentUser = getSysUserId(request);
            testComplianceStandardService.updateTestComplianceStandards(associations, test, currentUser, newStandards);

            return ResponseEntity.ok("Test compliance standards updated successfully");

        } catch (Exception e) {
            LogEvent.logError("TestComplianceStandardRestController", "updateTestComplianceStandards",
                "Error updating test compliance standards for test " + testId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to update test compliance standards: " + e.getMessage());
        }
    }

    // Helper methods

    private Errors validateAssociation(TestComplianceStandardDTO dto, String testId) {
        Errors errors = new BaseErrors();

        if (dto.getComplianceStandardId() == null || dto.getComplianceStandardId().trim().isEmpty()) {
            errors.reject("testCompliance.complianceStandardId.required", "Compliance Standard ID is required");
        }

        if (testId == null || testId.trim().isEmpty()) {
            errors.reject("testCompliance.testId.required", "Test ID is required");
        }

        return errors;
    }

    private TestComplianceStandardDTO convertToDTO(TestComplianceStandard association) {
        TestComplianceStandardDTO dto = new TestComplianceStandardDTO();
        dto.setId(association.getId());
        dto.setTestId(association.getTest() != null ? association.getTest().getId() : null);
        dto.setComplianceStandardId(association.getComplianceStandard() != null ?
            association.getComplianceStandard().getId() : null);
        dto.setComplianceStandardName(association.getComplianceStandardDisplayName());
        dto.setComplianceStandardRegulationNumber(association.getComplianceStandardRegulationNumber());
        dto.setComplianceStandardStatus(association.getComplianceStandardStatus());
        dto.setMandatory(association.isMandatory());
        dto.setApplicableParameters(association.getApplicableParameters());
        dto.setSortOrder(association.getSortOrder());
        return dto;
    }

    private TestComplianceStandard convertFromDTO(TestComplianceStandardDTO dto, String testId) {
        TestComplianceStandard association = new TestComplianceStandard();

        // Set test
        Test test = new Test();
        test.setId(testId);
        association.setTest(test);

        // Set compliance standard
        ComplianceStandard standard = new ComplianceStandard();
        standard.setId(dto.getComplianceStandardId());
        association.setComplianceStandard(standard);

        association.setMandatory(dto.isMandatory());
        association.setApplicableParameters(dto.getApplicableParameters());
        association.setSortOrder(dto.getSortOrder());

        return association;
    }

    @Override
    protected String findLocalForward(String forward) {
        return forward;
    }

    @Override
    protected String getPageTitleKey() {
        return "testCompliance.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "testCompliance.subtitle";
    }

    // DTOs for API responses

    public static class TestComplianceStandardDTO {
        private String id;
        private String testId;
        private String complianceStandardId;
        private String complianceStandardName;
        private String complianceStandardRegulationNumber;
        private String complianceStandardStatus;
        private boolean mandatory;
        private String applicableParameters;
        private String sortOrder;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTestId() { return testId; }
        public void setTestId(String testId) { this.testId = testId; }

        public String getComplianceStandardId() { return complianceStandardId; }
        public void setComplianceStandardId(String complianceStandardId) { this.complianceStandardId = complianceStandardId; }

        public String getComplianceStandardName() { return complianceStandardName; }
        public void setComplianceStandardName(String complianceStandardName) { this.complianceStandardName = complianceStandardName; }

        public String getComplianceStandardRegulationNumber() { return complianceStandardRegulationNumber; }
        public void setComplianceStandardRegulationNumber(String complianceStandardRegulationNumber) { this.complianceStandardRegulationNumber = complianceStandardRegulationNumber; }

        public String getComplianceStandardStatus() { return complianceStandardStatus; }
        public void setComplianceStandardStatus(String complianceStandardStatus) { this.complianceStandardStatus = complianceStandardStatus; }

        public boolean isMandatory() { return mandatory; }
        public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

        public String getApplicableParameters() { return applicableParameters; }
        public void setApplicableParameters(String applicableParameters) { this.applicableParameters = applicableParameters; }

        public String getSortOrder() { return sortOrder; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class ComplianceStandardSummaryDTO {
        private String id;
        private String name;
        private String displayName;
        private String regulationNumber;
        private String version;
        private String issuingBody;
        private String status;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getRegulationNumber() { return regulationNumber; }
        public void setRegulationNumber(String regulationNumber) { this.regulationNumber = regulationNumber; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getIssuingBody() { return issuingBody; }
        public void setIssuingBody(String issuingBody) { this.issuingBody = issuingBody; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}