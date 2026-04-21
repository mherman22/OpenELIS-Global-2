package org.openelisglobal.compliance.valueholder;

import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.test.valueholder.Test;

/**
 * Junction entity for Test-ComplianceStandard many-to-many relationship.
 *
 * Follows OpenELIS patterns: - Extends EnumValueItemImpl like PanelItem - Uses
 * ValueHolder pattern for relationships - Provides convenience methods for
 * entity access
 *
 * Represents the association between a Test and a ComplianceStandard, allowing
 * tests to be linked to multiple compliance standards and compliance standards
 * to apply to multiple tests.
 */
public class TestComplianceStandard extends EnumValueItemImpl {

    private static final long serialVersionUID = 1L;

    private String id;

    private ValueHolderInterface test;
    private ValueHolderInterface complianceStandard;

    // Cached values for performance
    private String testName;
    private String complianceStandardName;

    // Relationship metadata
    private String sortOrder;
    private String applicableParameters; // Specific parameters this standard applies to for this test
    private Boolean mandatory; // Whether compliance with this standard is mandatory for this test

    public TestComplianceStandard() {
        super();
        this.test = new ValueHolder();
        this.complianceStandard = new ValueHolder();
        this.mandatory = false;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    // Test relationship methods
    public Test getTest() {
        return (Test) this.test.getValue();
    }

    public void setTest(Test test) {
        this.test.setValue(test);
    }

    protected ValueHolderInterface getTestHolder() {
        return this.test;
    }

    protected void setTestHolder(ValueHolderInterface test) {
        this.test = test;
    }

    // ComplianceStandard relationship methods
    public ComplianceStandard getComplianceStandard() {
        return (ComplianceStandard) this.complianceStandard.getValue();
    }

    public void setComplianceStandard(ComplianceStandard complianceStandard) {
        this.complianceStandard.setValue(complianceStandard);
    }

    protected ValueHolderInterface getComplianceStandardHolder() {
        return this.complianceStandard;
    }

    protected void setComplianceStandardHolder(ValueHolderInterface complianceStandard) {
        this.complianceStandard = complianceStandard;
    }

    // Cached name getters/setters for performance
    public String getTestName() {
        return this.testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getComplianceStandardName() {
        return this.complianceStandardName;
    }

    public void setComplianceStandardName(String complianceStandardName) {
        this.complianceStandardName = complianceStandardName;
    }

    // Sort order for ordered relationships
    @Override
    public String getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    // Relationship metadata
    public String getApplicableParameters() {
        return applicableParameters;
    }

    public void setApplicableParameters(String applicableParameters) {
        this.applicableParameters = applicableParameters;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMandatory() {
        return Boolean.TRUE.equals(mandatory);
    }

    /**
     * Convenience method to get the compliance standard's display name
     */
    public String getComplianceStandardDisplayName() {
        ComplianceStandard standard = getComplianceStandard();
        return standard != null ? standard.getDisplayName() : complianceStandardName;
    }

    /**
     * Convenience method to get the compliance standard's regulation number
     */
    public String getComplianceStandardRegulationNumber() {
        ComplianceStandard standard = getComplianceStandard();
        return standard != null ? standard.getRegulationNumber() : null;
    }

    /**
     * Convenience method to get the compliance standard's status
     */
    public String getComplianceStandardStatus() {
        ComplianceStandard standard = getComplianceStandard();
        return standard != null && standard.getStatus() != null ? standard.getStatus().toString() : null;
    }

    /**
     * Check if the associated compliance standard is active
     */
    public boolean isComplianceStandardActive() {
        ComplianceStandard standard = getComplianceStandard();
        return standard != null && standard.isActive();
    }

    @Override
    public String toString() {
        return "TestComplianceStandard{" + "id='" + id + '\'' + ", testName='" + testName + '\''
                + ", complianceStandardName='" + complianceStandardName + '\'' + ", mandatory=" + mandatory + '}';
    }
}