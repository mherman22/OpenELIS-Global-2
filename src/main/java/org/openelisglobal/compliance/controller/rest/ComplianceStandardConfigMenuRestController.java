package org.openelisglobal.compliance.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.form.ComplianceStandardConfigMenuForm;
import org.openelisglobal.compliance.service.ComplianceEvaluationService;
import org.openelisglobal.compliance.service.ComplianceStandardService;
import org.openelisglobal.compliance.valueholder.ComplianceEvaluation;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST Controller for compliance standard configuration menu integrated with
 * Test Editor.
 *
 * Provides JSON API endpoints for compliance standard configuration management.
 * Follows OpenELIS REST controller patterns as established by
 * TestModifyEntryRestController and other test management REST endpoints.
 *
 * Constitutional compliance: - Returns JSON responses instead of ModelAndView -
 * Delegates business logic to service layer - Follows RESTful API patterns -
 * Role-based access control to be added later
 */
@Controller
@RequestMapping("/rest")
public class ComplianceStandardConfigMenuRestController extends BaseController {

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ComplianceEvaluationService complianceEvaluationService;

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    /**
     * Get compliance standard configuration menu data
     *
     * @param sampleTypeId  Optional filter by sample type
     * @param testSectionId Optional filter by test section
     * @param showInactive  Whether to include inactive standards
     * @return ComplianceStandardConfigMenuForm with menu data
     */
    @GetMapping(value = "/ComplianceStandardConfigMenu", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ComplianceStandardConfigMenuForm> getComplianceStandardConfigMenu(
            @RequestParam(required = false) String sampleTypeId, @RequestParam(required = false) String testSectionId,
            @RequestParam(required = false, defaultValue = "false") boolean showInactive, HttpServletRequest request) {

        try {
            ComplianceStandardConfigMenuForm form = new ComplianceStandardConfigMenuForm();
            form.setSampleTypeId(sampleTypeId);
            form.setTestSectionId(testSectionId);
            form.setShowInactive(showInactive);

            // Setup form data
            setupFormForDisplay(form, sampleTypeId, testSectionId, showInactive);

            return ResponseEntity.ok(form);

        } catch (Exception e) {
            LogEvent.logError("ComplianceStandardConfigMenuRestController", "getComplianceStandardConfigMenu",
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save compliance standard configuration
     *
     * @param form Configuration form with updates
     * @return Response with success/error status
     */
    @PostMapping(value = "/ComplianceStandardConfigMenu", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveComplianceStandardConfig(
            @Valid @RequestBody ComplianceStandardConfigMenuForm form, HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate form data
            if (form.getSampleTypeId() == null || form.getSampleTypeId().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", MessageUtil.getMessage("complianceStandard.config.sampleType.required"));
                return ResponseEntity.badRequest().body(response);
            }

            // Save compliance standard configurations
            saveComplianceStandardConfigurations(form);

            response.put("success", true);
            response.put("message", MessageUtil.getMessage("complianceStandard.config.save.success"));
            response.put("savedCount",
                    form.getSelectedComplianceStandardIds() != null ? form.getSelectedComplianceStandardIds().size()
                            : 0);

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            LogEvent.logError("ComplianceStandardConfigMenuRestController", "saveComplianceStandardConfig",
                    e.getReason());
            response.put("success", false);
            response.put("error", e.getReason());
            return ResponseEntity.status(e.getStatusCode()).body(response);
        } catch (Exception e) {
            LogEvent.logError("ComplianceStandardConfigMenuRestController", "saveComplianceStandardConfig",
                    e.getMessage());
            response.put("success", false);
            response.put("error", MessageUtil.getMessage("complianceStandard.config.save.error"));
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get compliance standards filtered by sample type
     *
     * @param sampleTypeId    Sample type to filter by
     * @param includeInactive Whether to include inactive standards
     * @return Filtered list of compliance standards
     */
    @GetMapping(value = "/ComplianceStandardConfigMenu/standards", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<ComplianceStandard>> getComplianceStandardsBySampleType(
            @RequestParam String sampleTypeId,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {

        try {
            List<ComplianceStandard> standards;

            if (includeInactive) {
                // Get all standards that apply to this sample type
                standards = complianceStandardService.getAll();
                // TODO: Add actual filtering by sample type when relationship is established
            } else {
                // Get only active standards for this sample type
                standards = complianceStandardService.getActiveStandardsBySampleType(sampleTypeId);
            }

            return ResponseEntity.ok(standards);

        } catch (Exception e) {
            LogEvent.logError("ComplianceStandardConfigMenuRestController", "getComplianceStandardsBySampleType",
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get tests that use a specific compliance standard
     *
     * @param standardId Compliance standard ID
     * @return List of tests using this standard
     */
    @GetMapping(value = "/ComplianceStandardConfigMenu/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Test>> getTestsUsingComplianceStandard(@RequestParam String standardId) {

        try {
            List<Test> testsUsingStandard = new ArrayList<>();

            // Get compliance evaluations that use this standard
            List<ComplianceEvaluation> evaluations = complianceEvaluationService
                    .getEvaluationsByComplianceStandard(standardId);

            // Extract unique test IDs from evaluations
            // TODO: This logic needs to be updated based on actual test-evaluation
            // relationship
            Map<String, Test> uniqueTests = new HashMap<>();

            for (ComplianceEvaluation evaluation : evaluations) {
                String testId = evaluation.getSampleId(); // Note: This mapping may need adjustment
                if (!uniqueTests.containsKey(testId)) {
                    // Get the actual test object
                    // Test test = testService.get(testId);
                    // if (test != null) {
                    // uniqueTests.put(testId, test);
                    // }
                }
            }

            testsUsingStandard.addAll(uniqueTests.values());

            return ResponseEntity.ok(testsUsingStandard);

        } catch (Exception e) {
            LogEvent.logError("ComplianceStandardConfigMenuRestController", "getTestsUsingComplianceStandard",
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate compliance standard configuration before saving
     *
     * @param form Configuration form to validate
     * @return Validation results
     */
    @PostMapping(value = "/ComplianceStandardConfigMenu/validate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateComplianceStandardConfig(
            @RequestBody ComplianceStandardConfigMenuForm form) {

        Map<String, Object> response = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // Validate sample type selection
            if (form.getSampleTypeId() == null || form.getSampleTypeId().trim().isEmpty()) {
                errors.add(MessageUtil.getMessage("complianceStandard.config.sampleType.required"));
            }

            // Validate selected standards exist and are active
            if (form.getSelectedComplianceStandardIds() != null) {
                for (String standardId : form.getSelectedComplianceStandardIds()) {
                    ComplianceStandard standard = complianceStandardService.get(standardId);
                    if (standard == null) {
                        errors.add("Compliance standard not found: " + standardId);
                    } else if (standard.getStatus().name().equals("INACTIVE")) {
                        warnings.add("Selected standard is inactive: " + standard.getName());
                    }
                }
            }

            // Check for conflicts with existing test configurations
            if (form.getSampleTypeId() != null && form.getSelectedComplianceStandardIds() != null) {
                warnings.addAll(checkForConfigurationConflicts(form.getSampleTypeId(),
                        form.getSelectedComplianceStandardIds()));
            }

            response.put("valid", errors.isEmpty());
            response.put("errors", errors);
            response.put("warnings", warnings);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError("ComplianceStandardConfigMenuRestController", "validateComplianceStandardConfig",
                    e.getMessage());
            response.put("valid", false);
            response.put("errors", List.of("Validation error: " + e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ================== Private Helper Methods ==================

    /**
     * Setup form for display with filtered data
     */
    private void setupFormForDisplay(ComplianceStandardConfigMenuForm form, String sampleTypeId, String testSectionId,
            boolean showInactive) {

        // Get available sample types for filtering
        List<TypeOfSample> sampleTypes = typeOfSampleService.getAllTypeOfSamples();
        form.setSampleTypes(sampleTypes);

        // Get available tests (filtered if needed)
        List<Test> availableTests;
        if (sampleTypeId != null && !sampleTypeId.trim().isEmpty()) {
            availableTests = getTestsForSampleType(sampleTypeId);
        } else {
            availableTests = testService.getAllActiveOrderableTests();
        }
        form.setAvailableTests(availableTests);

        // Get compliance standards (filtered if needed)
        List<ComplianceStandard> standards;
        if (showInactive) {
            standards = complianceStandardService.getAll();
        } else {
            if (sampleTypeId != null && !sampleTypeId.trim().isEmpty()) {
                standards = complianceStandardService.getActiveStandardsBySampleType(sampleTypeId);
            } else {
                standards = complianceStandardService.getActiveComplianceStandards();
            }
        }

        // Sort standards by issuing body and name
        standards.sort((s1, s2) -> {
            int bodyCompare = s1.getIssuingBody().compareTo(s2.getIssuingBody());
            if (bodyCompare != 0)
                return bodyCompare;
            return s1.getName().compareTo(s2.getName());
        });

        form.setMenuList(standards);

        // Load existing relationships
        loadExistingComplianceRelationships(form);
    }

    /**
     * Load existing relationships between tests and compliance standards
     */
    private void loadExistingComplianceRelationships(ComplianceStandardConfigMenuForm form) {
        Map<String, List<String>> testComplianceMap = new HashMap<>();

        try {
            List<ComplianceEvaluation> recentEvaluations = complianceEvaluationService.getRecentEvaluations();

            for (ComplianceEvaluation evaluation : recentEvaluations) {
                String testId = evaluation.getSampleId();
                String standardId = evaluation.getStandardId();

                if (standardId != null && !standardId.isBlank()) {
                    testComplianceMap.computeIfAbsent(testId, k -> new ArrayList<>()).add(standardId);
                }
            }

            form.setTestComplianceMap(testComplianceMap);

        } catch (Exception e) {
            LogEvent.logWarn("ComplianceStandardConfigMenuRestController", "loadExistingComplianceRelationships",
                    "Could not load existing compliance relationships: " + e.getMessage());
            form.setTestComplianceMap(new HashMap<>());
        }
    }

    /**
     * Save compliance standard configurations
     */
    private void saveComplianceStandardConfigurations(ComplianceStandardConfigMenuForm form) {
        String sampleTypeId = form.getSampleTypeId();
        List<String> selectedStandardIds = form.getSelectedComplianceStandardIds();

        // Get applicable tests for the selected sample type
        List<Test> applicableTests = getTestsForSampleType(sampleTypeId);

        if (selectedStandardIds == null || selectedStandardIds.isEmpty()) {
            // Remove all compliance standard associations for this sample type
            removeComplianceStandardAssociations(applicableTests);
            return;
        }

        // Get selected standards
        List<ComplianceStandard> selectedStandards = new ArrayList<>();
        for (String standardId : selectedStandardIds) {
            ComplianceStandard standard = complianceStandardService.get(standardId);
            if (standard != null) {
                selectedStandards.add(standard);
            }
        }

        // Create new associations
        createComplianceStandardAssociations(applicableTests, selectedStandards);
    }

    /**
     * Get tests for a specific sample type
     */
    private List<Test> getTestsForSampleType(String sampleTypeId) {
        // TODO: Implement actual filtering by sample type relationship
        return testService.getAllActiveOrderableTests();
    }

    /**
     * Check for configuration conflicts
     */
    private List<String> checkForConfigurationConflicts(String sampleTypeId, List<String> standardIds) {
        List<String> conflicts = new ArrayList<>();

        // TODO: Implement actual conflict detection logic
        // This might include:
        // - Checking if standards have incompatible parameters
        // - Verifying standards are applicable to the sample type
        // - Ensuring no duplicate standard applications

        return conflicts;
    }

    /**
     * Remove compliance standard associations
     */
    private void removeComplianceStandardAssociations(List<Test> tests) {
        throwComplianceAssociationPersistenceNotImplemented("removeComplianceStandardAssociations",
                "Compliance standard association removal is not implemented; no changes were persisted for "
                        + tests.size() + " tests");
    }

    /**
     * Create compliance standard associations
     */
    private void createComplianceStandardAssociations(List<Test> tests, List<ComplianceStandard> standards) {
        throwComplianceAssociationPersistenceNotImplemented("createComplianceStandardAssociations",
                "Compliance standard association creation is not implemented; no changes were persisted for "
                        + tests.size() + " tests and " + standards.size() + " standards");
    }

    private void throwComplianceAssociationPersistenceNotImplemented(String methodName, String message) {
        LogEvent.logWarn("ComplianceStandardConfigMenuRestController", methodName, message);
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, message);
    }

    @Override
    protected String findLocalForward(String forward) {
        return "compliance-standard-config-api";
    }

    @Override
    protected String getPageTitleKey() {
        return "complianceStandard.config.api.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "complianceStandard.config.api.subtitle";
    }
}
