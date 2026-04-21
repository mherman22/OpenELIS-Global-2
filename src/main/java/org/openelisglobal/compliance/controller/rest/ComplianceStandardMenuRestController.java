package org.openelisglobal.compliance.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.compliance.form.ComplianceStandardConfigMenuForm;
import org.openelisglobal.compliance.service.ComplianceStandardService;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * REST Controller for Compliance Standards menu following OpenELIS patterns.
 *
 * Extends BaseMenuController to leverage existing OpenELIS menu infrastructure
 * including pagination, search, and bulk operations, following the exact same
 * pattern as DictionaryMenuRestController.
 *
 * Constitutional compliance:
 * - Extends BaseMenuController<ComplianceStandard>
 * - Uses @RestController with /rest mapping
 * - @PreAuthorize for role-based access control
 * - Implements required abstract methods from BaseMenuController
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER')")
public class ComplianceStandardMenuRestController extends BaseMenuController<ComplianceStandard> {

    private static final String[] ALLOWED_FIELDS = new String[] {
        "selectedIDs*",
        "sampleTypeId",
        "showInactive"
    };

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    /**
     * Show compliance standards menu with pagination and search support
     */
    @RequestMapping(value = { "/ComplianceStandardMenu",
                             "/SearchComplianceStandardMenu" },
                   produces = MediaType.APPLICATION_JSON_VALUE,
                   method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER', 'COMPLIANCE_VIEWER')")
    public ResponseEntity<?> showComplianceStandardMenu(HttpServletRequest request,
                                                       RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        ComplianceStandardConfigMenuForm form = new ComplianceStandardConfigMenuForm();

        // Get sample types for filtering
        List<TypeOfSample> sampleTypes = typeOfSampleService.getAllTypeOfSamples();
        form.setSampleTypes(sampleTypes);

        String forward = performMenuAction(form, request);
        if (FWD_FAIL.equals(forward)) {
            Errors errors = new BaseErrors();
            errors.reject("complianceStandard.menu.error.generic");
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        } else {
            request.setAttribute("menuDefinition", "ComplianceStandardMenuDefinition");
            addFlashMsgsToRequest(request);
            return ResponseEntity.ok(form);
        }
    }

    /**
     * Get sample types for filtering
     */
    @RequestMapping(value = "/compliance-sample-types",
                   produces = MediaType.APPLICATION_JSON_VALUE,
                   method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER', 'COMPLIANCE_VIEWER')")
    public List<TypeOfSample> fetchSampleTypes() {
        return typeOfSampleService.getAllTypeOfSamples();
    }

    /**
     * Delete compliance standards (bulk operation)
     */
    @RequestMapping(value = "/DeleteComplianceStandard", method = RequestMethod.POST)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN')")
    public ResponseEntity<?> deleteComplianceStandards(HttpServletRequest request,
                                                      @RequestParam(value = ID, required = false) String id) {

        String[] IDs = id.split(",");
        List<String> selectedIDs = new ArrayList<>();
        for (String standardId : IDs) {
            selectedIDs.add(standardId);
        }

        List<ComplianceStandard> standards = new ArrayList<>();
        for (String selectedId : selectedIDs) {
            ComplianceStandard standard = new ComplianceStandard();
            standard.setId(selectedId);
            standard.setSysUserId(getSysUserId(request));
            standards.add(standard);
        }

        try {
            // Check if standards can be deleted (business rule validation)
            for (ComplianceStandard standard : standards) {
                if (!complianceStandardService.canDelete(standard.getId())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot delete compliance standard: " + standard.getId() +
                              " - it has linked evaluations or parameter groups");
                }
            }

            complianceStandardService.deleteAll(standards);

        } catch (LIMSRuntimeException e) {
            LogEvent.logError("ComplianceStandardMenuRestController", "deleteComplianceStandards", e.getMessage());
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Compliance standard was modified by another user. Please refresh and try again.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete compliance standards: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("Compliance standards deleted successfully");
    }

    /**
     * Archive compliance standards (bulk operation)
     */
    @RequestMapping(value = "/ArchiveComplianceStandard", method = RequestMethod.POST)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN')")
    public ResponseEntity<?> archiveComplianceStandards(HttpServletRequest request,
                                                       @RequestParam(value = ID, required = false) String id) {

        String[] IDs = id.split(",");
        List<String> selectedIDs = new ArrayList<>();
        for (String standardId : IDs) {
            selectedIDs.add(standardId);
        }

        try {
            String userId = getSysUserId(request);
            complianceStandardService.bulkUpdateStatus(selectedIDs,
                org.openelisglobal.compliance.valueholder.ComplianceStandardStatus.ARCHIVED, userId);

        } catch (Exception e) {
            LogEvent.logError("ComplianceStandardMenuRestController", "archiveComplianceStandards", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to archive compliance standards: " + e.getMessage());
        }

        return ResponseEntity.ok("Compliance standards archived successfully");
    }

    @Override
    protected List<ComplianceStandard> createMenuList(AdminOptionMenuForm<ComplianceStandard> form,
                                                     HttpServletRequest request) {
        List<ComplianceStandard> standards;
        int startingRecNo = Integer.parseInt((String) request.getAttribute("startingRecNo"));
        int total;

        // Check for sample type filter
        String sampleTypeId = request.getParameter("sampleTypeId");
        boolean showInactive = "true".equals(request.getParameter("showInactive"));

        if (YES.equals(request.getParameter("search"))) {
            // Search functionality
            String searchString = request.getParameter("searchString");
            standards = complianceStandardService.getPagesOfSearchedStandards(startingRecNo, searchString);
            total = complianceStandardService.getTotalSearchedStandardCount(searchString);
        } else if (sampleTypeId != null && !sampleTypeId.trim().isEmpty()) {
            // Filter by sample type
            if (showInactive) {
                standards = complianceStandardService.getAll(); // TODO: Filter by sample type
            } else {
                standards = complianceStandardService.getActiveStandardsBySampleType(sampleTypeId);
            }
            total = standards.size();

            // Apply pagination manually for filtered results
            int pageSize = Integer.parseInt(
                ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
            int fromIndex = Math.min(startingRecNo, standards.size());
            int toIndex = Math.min(fromIndex + pageSize, standards.size());
            standards = standards.subList(fromIndex, toIndex);

        } else {
            // Default: show all standards with pagination
            if (showInactive) {
                standards = complianceStandardService.getPageOfStandards(startingRecNo);
                total = complianceStandardService.getTotalStandardCount();
            } else {
                List<ComplianceStandard> allActive = complianceStandardService.getActiveComplianceStandards();
                total = allActive.size();

                // Apply pagination
                int pageSize = Integer.parseInt(
                    ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
                int fromIndex = Math.min(startingRecNo, allActive.size());
                int toIndex = Math.min(fromIndex + pageSize, allActive.size());
                standards = allActive.subList(fromIndex, toIndex);
            }
        }

        // Set up pagination attributes (following DictionaryMenuRestController pattern)
        request.setAttribute("menuDefinition", "ComplianceStandardMenuDefinition");
        request.setAttribute(MENU_TOTAL_RECORDS, String.valueOf(total));
        request.setAttribute(MENU_FROM_RECORD, String.valueOf(startingRecNo));

        int numOfRecs = 0;
        int defaultPageSize = Integer.parseInt(
            ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

        if (standards.size() > defaultPageSize) {
            numOfRecs = defaultPageSize;
        } else {
            numOfRecs = standards.size();
        }

        if (numOfRecs > 0) {
            numOfRecs--;
        }

        int endingRecNo = startingRecNo + numOfRecs;
        request.setAttribute(MENU_TO_RECORD, String.valueOf(endingRecNo));
        form.setToRecordCount(String.valueOf(endingRecNo));
        form.setFromRecordCount(String.valueOf(startingRecNo));
        form.setTotalRecordCount(String.valueOf(total));

        // Set search column for the menu
        request.setAttribute(MENU_SEARCH_BY_TABLE_COLUMN, "compliance_standard.name");

        if (YES.equals(request.getParameter("search"))) {
            request.setAttribute(IN_MENU_SELECT_LIST_HEADER_SEARCH, "true");
        }

        return standards;
    }

    @Override
    protected String getDeactivateDisabled() {
        return "false"; // Allow deactivation/archiving of compliance standards
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "complianceStandardMenuDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_DELETE.equals(forward)) {
            return "redirect:/ComplianceStandardMenu";
        } else if (FWD_FAIL_DELETE.equals(forward)) {
            return "redirect:/ComplianceStandardMenu";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "complianceStandard.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "complianceStandard.browse.subtitle";
    }
}