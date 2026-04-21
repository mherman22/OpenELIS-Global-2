package org.openelisglobal.compliance.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.compliance.valueholder.TestComplianceStandard;

/**
 * DAO interface for TestComplianceStandard entity.
 *
 * Follows OpenELIS DAO patterns: - Extends BaseDAO for standard CRUD operations
 * - Provides domain-specific query methods - Supports relationship queries
 * between Test and ComplianceStandard
 */
public interface TestComplianceStandardDAO extends BaseDAO<TestComplianceStandard, String> {

    /**
     * Get all compliance standards associated with a specific test
     *
     * @param testId The test ID
     * @return List of TestComplianceStandard associations
     */
    List<TestComplianceStandard> getComplianceStandardsForTest(String testId);

    /**
     * Get all tests associated with a specific compliance standard
     *
     * @param complianceStandardId The compliance standard ID
     * @return List of TestComplianceStandard associations
     */
    List<TestComplianceStandard> getTestsForComplianceStandard(String complianceStandardId);

    /**
     * Get a specific test-compliance standard association
     *
     * @param testId               The test ID
     * @param complianceStandardId The compliance standard ID
     * @return TestComplianceStandard association or null if not found
     */
    TestComplianceStandard getTestComplianceStandardAssociation(String testId, String complianceStandardId);

    /**
     * Get all mandatory compliance standards for a specific test
     *
     * @param testId The test ID
     * @return List of mandatory TestComplianceStandard associations
     */
    List<TestComplianceStandard> getMandatoryComplianceStandardsForTest(String testId);

    /**
     * Get all tests that have mandatory compliance standards
     *
     * @return List of TestComplianceStandard associations where mandatory=true
     */
    List<TestComplianceStandard> getTestsWithMandatoryCompliance();

    /**
     * Get compliance standards for tests, ordered by sort order
     *
     * @param testId The test ID
     * @return List of TestComplianceStandard associations ordered by sort_order
     */
    List<TestComplianceStandard> getComplianceStandardsForTestOrdered(String testId);

    /**
     * Remove all compliance standard associations for a test
     *
     * @param testId The test ID
     */
    void removeAllComplianceStandardsForTest(String testId);

    /**
     * Remove all test associations for a compliance standard
     *
     * @param complianceStandardId The compliance standard ID
     */
    void removeAllTestsForComplianceStandard(String complianceStandardId);

    /**
     * Check if a test has any compliance standard associations
     *
     * @param testId The test ID
     * @return true if test has compliance standards associated
     */
    boolean testHasComplianceStandards(String testId);

    /**
     * Check if a compliance standard has any test associations
     *
     * @param complianceStandardId The compliance standard ID
     * @return true if compliance standard has tests associated
     */
    boolean complianceStandardHasTests(String complianceStandardId);

    /**
     * Get all test-compliance standard associations with full entity details
     *
     * @return List of TestComplianceStandard with Test and ComplianceStandard
     *         entities loaded
     */
    List<TestComplianceStandard> getAllWithEntities();

    /**
     * Get tests associated with active compliance standards only
     *
     * @return List of TestComplianceStandard where compliance standard status is
     *         ACTIVE
     */
    List<TestComplianceStandard> getTestsWithActiveCompliance();
}