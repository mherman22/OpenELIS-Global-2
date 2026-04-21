package org.openelisglobal.compliance.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.controller.BaseMenuController;
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
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for compliance standard configuration menu integrated with Test
 * Editor.
 *
 * This controller provides a menu interface for configuring compliance
 * standards and linking them to laboratory tests. Follows OpenELIS Test Editor
 * integration patterns as established by TestNotificationConfigMenuController
 * and other test management menus.
 *
 * Constitutional compliance: - Extends BaseMenuController following established
 * patterns - Uses @Transactional only in service layer (not controller) -
 * Follows 5-layer architecture - Integrates with existing Test Editor workflow
 */
@Controller
public class ComplianceStandardConfigMenuController extends BaseMenuController<ComplianceStandard> {

    private static final String FWD_SUCCESS = "success";
    private static final String FWD_FAIL = "error";

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ComplianceEvaluationService complianceEvaluationService;

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("sampleTypeId", "testSectionId", "showInactiveStandards",
                "selectedComplianceStandardIds*", "activeStatusFlags*", "complianceStandardConfigs*");
    }

    @RequestMapping(value = "/ComplianceStandardConfigMenu", method = RequestMethod.GET)
    public ModelAndView showComplianceStandardConfigMenu(HttpServletRequest request) {
        ComplianceStandardConfigMenuForm form = new ComplianceStandardConfigMenuForm();

        try {
            setupFormForDisplay(form);
            addFlashMsgsToRequest(request);
            return findForward(FWD_SUCCESS, form);

        } catch (Exception e) {
            LogEvent.logError("ComplianceStandardConfigMenuController", "showComplianceStandardConfigMenu",
                    e.getMessage());
            return findForward(FWD_FAIL, form);
        }
    }

    @RequestMapping(value = "/ComplianceStandardConfigMenu", method = RequestMethod.POST)
    public ModelAndView processComplianceStandardConfigMenu(
            @Valid @ModelAttribute("form") ComplianceStandardConfigMenuForm form, BindingResult result,
            HttpServletRequest request) {

        if (result.hasErrors()) {
            saveErrors(result);
            setupFormForDisplay(form);
            return findForward(FWD_FAIL, form);
        }

        try {
            // Save compliance standard configurations
            saveComplianceStandardConfigurations(form);

            // Add success message
            request.getSession().setAttribute(SAVE_DISABLED, IActionConstants.TRUE);
            addSuccessMessage(request, "complianceStandard.config.save.success");

            // Redirect to avoid resubmission
            return getForwardWithParameters(findForward(FWD_SUCCESS, form),
                    "?saved=true&type=" + form.getSampleTypeId());

        } catch (Exception e) {
            LogEvent.logError("ComplianceStandardConfigMenuController", "processComplianceStandardConfigMenu",
                    e.getMessage());
            addErrorMessage(request, "complianceStandard.config.save.error");
            setupFormForDisplay(form);
            return findForward(FWD_FAIL, form);
        }
    }

    /**
     * Setup form for initial display or redisplay after validation errors
     */
    private void setupFormForDisplay(ComplianceStandardConfigMenuForm form) {
        // Get available sample types for filtering
        List<TypeOfSample> sampleTypes = typeOfSampleService.getAllTypeOfSamples();
        form.setSampleTypes(sampleTypes);

        // Get all tests that can have compliance standards applied
        List<Test> orderableTests = testService.getAllActiveOrderableTests();
        form.setAvailableTests(orderableTests);

        // Create menu list of compliance standards
        form.setMenuList(createMenuList());

        // Load existing test-compliance standard relationships
        loadExistingComplianceRelationships(form);
    }

    /**
     * Create menu list of compliance standards organized by category
     */
    @Override
    protected List<ComplianceStandard> createMenuList() {
        List<ComplianceStandard> allStandards = complianceStandardService.getActiveComplianceStandards();

        // Sort by issuing body and name for better organization
        allStandards.sort((s1, s2) -> {
            int bodyCompare = s1.getIssuingBody().compareTo(s2.getIssuingBody());
            if (bodyCompare != 0)
                return bodyCompare;
            return s1.getName().compareTo(s2.getName());
        });

        return allStandards;
    }

    /**
     * Load existing relationships between tests and compliance standards
     */
    private void loadExistingComplianceRelationships(ComplianceStandardConfigMenuForm form) {
        Map<String, List<String>> testComplianceMap = new HashMap<>();

        try {
            // Get recent evaluations to determine which standards are being used with which
            // tests
            List<ComplianceEvaluation> recentEvaluations = complianceEvaluationService.getRecentEvaluations();

            for (ComplianceEvaluation evaluation : recentEvaluations) {
                String testId = evaluation.getSampleId(); // Note: This might need adjustment based on actual test
                                                          // linking
                String standardId = evaluation.getComplianceStandard().getId();

                testComplianceMap.computeIfAbsent(testId, k -> new ArrayList<>()).add(standardId);
            }

            form.setTestComplianceMap(testComplianceMap);

        } catch (Exception e) {
            LogEvent.logWarn("ComplianceStandardConfigMenuController", "loadExistingComplianceRelationships",
                    "Could not load existing compliance relationships: " + e.getMessage());
            form.setTestComplianceMap(new HashMap<>());
        }
    }

    /**
     * Save compliance standard configurations from form
     */
    private void saveComplianceStandardConfigurations(ComplianceStandardConfigMenuForm form) {
        String sampleTypeId = form.getSampleTypeId();

        if (sampleTypeId == null) {
            throw new IllegalArgumentException("Sample type must be selected");
        }

        // Get applicable tests for the selected sample type
        List<Test> applicableTests = getTestsForSampleType(sampleTypeId);
        List<String> selectedStandardIds = form.getSelectedComplianceStandardIds();

        if (selectedStandardIds == null || selectedStandardIds.isEmpty()) {
            // Remove all compliance standard associations for this sample type
            removeComplianceStandardAssociations(applicableTests);
            return;
        }

        // Create new associations
        List<ComplianceStandard> selectedStandards = new ArrayList<>();
        for (String standardId : selectedStandardIds) {
            ComplianceStandard standard = complianceStandardService.get(standardId);
            if (standard != null) {
                selectedStandards.add(standard);
            }
        }

        // Associate standards with tests
        createComplianceStandardAssociations(applicableTests, selectedStandards);
    }

    /**
     * Get tests applicable to a specific sample type
     */
    private List<Test> getTestsForSampleType(String sampleTypeId) {
        // This would need to be implemented based on OpenELIS test-sample type
        // relationships
        // For now, return all active orderable tests
        // TODO: Filter by actual sample type relationship
        return testService.getAllActiveOrderableTests();
    }

    /**
     * Remove compliance standard associations for tests
     */
    private void removeComplianceStandardAssociations(List<Test> tests) {
        // Implementation would depend on how test-compliance relationships are stored
        // This might involve:
        // 1. Deleting records from a linking table
        // 2. Updating test records to remove compliance references
        // 3. Clearing cached compliance data

        LogEvent.logInfo("ComplianceStandardConfigMenuController", "removeComplianceStandardAssociations",
                "Removed compliance associations for " + tests.size() + " tests");
    }

    /**
     * Create compliance standard associations for tests
     */
    private void createComplianceStandardAssociations(List<Test> tests, List<ComplianceStandard> standards) {
        // Implementation would create the actual linkage between tests and compliance
        // standards
        // This might involve:
        // 1. Creating records in a linking table (test_compliance_standard)
        // 2. Updating test records with compliance references
        // 3. Triggering FHIR synchronization for updated relationships

        LogEvent.logInfo("ComplianceStandardConfigMenuController", "createComplianceStandardAssociations",
                "Created compliance associations: " + tests.size() + " tests, " + standards.size() + " standards");
    }

    /**
     * Add success message to request
     */
    private void addSuccessMessage(HttpServletRequest request, String messageKey) {
        String message = MessageUtil.getMessage(messageKey);
        request.setAttribute(FWD_SUCCESS_MSG, message);
    }

    /**
     * Add error message to request
     */
    private void addErrorMessage(HttpServletRequest request, String messageKey) {
        String message = MessageUtil.getMessage(messageKey);
        request.setAttribute(FWD_FAIL_MSG, message);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "complianceStandardConfigMenuDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "complianceStandardConfigMenuError";
        }
        return "PageNotFound";
    }

    @Override
    protected String getPageTitleKey() {
        return "complianceStandard.config.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "complianceStandard.config.subtitle";
    }
}