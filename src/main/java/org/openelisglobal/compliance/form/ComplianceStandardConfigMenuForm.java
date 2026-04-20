package org.openelisglobal.compliance.form;

import jakarta.validation.constraints.Pattern;
import java.util.List;

import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form for compliance standards menu following OpenELIS patterns.
 *
 * Simplified form extending AdminOptionMenuForm with validation annotations
 * following the DictionaryMenuForm pattern.
 *
 * Constitutional compliance:
 * - Extends AdminOptionMenuForm<ComplianceStandard>
 * - Uses proper validation annotations (@SafeHtml, @Pattern)
 * - Simple structure matching existing OpenELIS form patterns
 */
public class ComplianceStandardConfigMenuForm extends AdminOptionMenuForm<ComplianceStandard> {

    private static final long serialVersionUID = 1L;

    // For display
    private List<ComplianceStandard> menuList;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedIDs;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String searchString = "";

    // Filter options for compliance standards
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeId;

    private boolean showInactive = false;

    // Available sample types for filtering
    private List<TypeOfSample> sampleTypes;

    public ComplianceStandardConfigMenuForm() {
        setFormName("complianceStandardMenuForm");
    }

    @Override
    public List<ComplianceStandard> getMenuList() {
        return menuList;
    }

    @Override
    public void setMenuList(List<ComplianceStandard> menuList) {
        this.menuList = menuList;
    }

    @Override
    public List<String> getSelectedIDs() {
        return selectedIDs;
    }

    @Override
    public void setSelectedIDs(List<String> selectedIDs) {
        this.selectedIDs = selectedIDs;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(String sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public boolean isShowInactive() {
        return showInactive;
    }

    public void setShowInactive(boolean showInactive) {
        this.showInactive = showInactive;
    }

    public List<TypeOfSample> getSampleTypes() {
        return sampleTypes;
    }

    public void setSampleTypes(List<TypeOfSample> sampleTypes) {
        this.sampleTypes = sampleTypes;
    }
}