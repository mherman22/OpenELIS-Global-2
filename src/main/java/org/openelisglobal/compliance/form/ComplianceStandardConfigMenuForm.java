package org.openelisglobal.compliance.form;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Form for compliance standard configuration menu integrated with Test Editor.
 *
 * This form captures user selections for linking compliance standards to laboratory tests
 * and sample types. Follows OpenELIS form patterns as established by other test management
 * configuration forms.
 *
 * Constitutional compliance:
 * - Extends AdminOptionMenuForm following established patterns
 * - Uses proper validation annotations
 * - Provides clear data binding structure
 */
public class ComplianceStandardConfigMenuForm extends AdminOptionMenuForm<ComplianceStandard> {

    private static final long serialVersionUID = 1L;

    // Filter options
    private String sampleTypeId;
    private String testSectionId;
    private boolean showInactiveStandards = false;

    // Available data for dropdowns
    private List<TypeOfSample> sampleTypes;
    private List<Test> availableTests;

    // Selected compliance standards
    private List<String> selectedComplianceStandardIds;

    // Status configuration for each standard
    private List<Boolean> activeStatusFlags;

    // Existing relationships between tests and compliance standards
    private Map<String, List<String>> testComplianceMap;

    // Detailed compliance standard configurations
    private List<ComplianceStandardConfigItem> complianceStandardConfigs;

    /**
     * Default constructor
     */
    public ComplianceStandardConfigMenuForm() {
        setFormName("complianceStandardConfigMenuForm");
    }

    // ================== Getters and Setters ==================

    public String getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(String sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public boolean isShowInactiveStandards() {
        return showInactiveStandards;
    }

    public void setShowInactiveStandards(boolean showInactiveStandards) {
        this.showInactiveStandards = showInactiveStandards;
    }

    public List<TypeOfSample> getSampleTypes() {
        return sampleTypes;
    }

    public void setSampleTypes(List<TypeOfSample> sampleTypes) {
        this.sampleTypes = sampleTypes;
    }

    public List<Test> getAvailableTests() {
        return availableTests;
    }

    public void setAvailableTests(List<Test> availableTests) {
        this.availableTests = availableTests;
    }

    public List<String> getSelectedComplianceStandardIds() {
        return selectedComplianceStandardIds;
    }

    public void setSelectedComplianceStandardIds(List<String> selectedComplianceStandardIds) {
        this.selectedComplianceStandardIds = selectedComplianceStandardIds;
    }

    public List<Boolean> getActiveStatusFlags() {
        return activeStatusFlags;
    }

    public void setActiveStatusFlags(List<Boolean> activeStatusFlags) {
        this.activeStatusFlags = activeStatusFlags;
    }

    public Map<String, List<String>> getTestComplianceMap() {
        return testComplianceMap;
    }

    public void setTestComplianceMap(Map<String, List<String>> testComplianceMap) {
        this.testComplianceMap = testComplianceMap;
    }

    public List<ComplianceStandardConfigItem> getComplianceStandardConfigs() {
        return complianceStandardConfigs;
    }

    public void setComplianceStandardConfigs(List<ComplianceStandardConfigItem> complianceStandardConfigs) {
        this.complianceStandardConfigs = complianceStandardConfigs;
    }

    // ================== Helper Methods ==================

    /**
     * Check if any compliance standards are selected
     */
    public boolean hasSelectedStandards() {
        return selectedComplianceStandardIds != null && !selectedComplianceStandardIds.isEmpty();
    }

    /**
     * Get count of selected compliance standards
     */
    public int getSelectedStandardCount() {
        return selectedComplianceStandardIds != null ? selectedComplianceStandardIds.size() : 0;
    }

    /**
     * Check if a specific compliance standard is selected
     */
    public boolean isStandardSelected(String standardId) {
        return selectedComplianceStandardIds != null && selectedComplianceStandardIds.contains(standardId);
    }

    /**
     * Check if filtering is applied
     */
    public boolean hasFilters() {
        return (sampleTypeId != null && !sampleTypeId.trim().isEmpty()) ||
               (testSectionId != null && !testSectionId.trim().isEmpty()) ||
               showInactiveStandards;
    }

    /**
     * Get filter description for display
     */
    public String getFilterDescription() {
        StringBuilder desc = new StringBuilder();

        if (sampleTypeId != null && !sampleTypeId.trim().isEmpty()) {
            TypeOfSample sampleType = getSampleTypeById(sampleTypeId);
            if (sampleType != null) {
                desc.append("Sample Type: ").append(sampleType.getLocalizedName());
            }
        }

        if (testSectionId != null && !testSectionId.trim().isEmpty()) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("Test Section: ").append(testSectionId);
        }

        if (showInactiveStandards) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("Including Inactive");
        }

        return desc.toString();
    }

    /**
     * Get sample type by ID
     */
    private TypeOfSample getSampleTypeById(String sampleTypeId) {
        if (sampleTypes != null) {
            return sampleTypes.stream()
                .filter(type -> sampleTypeId.equals(type.getId()))
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    // ================== Nested Configuration Item Class ==================

    /**
     * Configuration item for a compliance standard with its settings
     */
    public static class ComplianceStandardConfigItem {
        private String standardId;
        private String standardName;
        private String issuingBody;
        private boolean active;
        private boolean mandatory;
        private String applicabilityNotes;

        // Default constructor
        public ComplianceStandardConfigItem() {}

        // Constructor with basic info
        public ComplianceStandardConfigItem(String standardId, String standardName, String issuingBody) {
            this.standardId = standardId;
            this.standardName = standardName;
            this.issuingBody = issuingBody;
        }

        // Getters and setters
        public String getStandardId() {
            return standardId;
        }

        public void setStandardId(String standardId) {
            this.standardId = standardId;
        }

        public String getStandardName() {
            return standardName;
        }

        public void setStandardName(String standardName) {
            this.standardName = standardName;
        }

        public String getIssuingBody() {
            return issuingBody;
        }

        public void setIssuingBody(String issuingBody) {
            this.issuingBody = issuingBody;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isMandatory() {
            return mandatory;
        }

        public void setMandatory(boolean mandatory) {
            this.mandatory = mandatory;
        }

        public String getApplicabilityNotes() {
            return applicabilityNotes;
        }

        public void setApplicabilityNotes(String applicabilityNotes) {
            this.applicabilityNotes = applicabilityNotes;
        }
    }

    @Override
    public String toString() {
        return "ComplianceStandardConfigMenuForm{" +
                "sampleTypeId='" + sampleTypeId + '\'' +
                ", testSectionId='" + testSectionId + '\'' +
                ", showInactiveStandards=" + showInactiveStandards +
                ", selectedStandardCount=" + getSelectedStandardCount() +
                ", hasFilters=" + hasFilters() +
                '}';
    }
}