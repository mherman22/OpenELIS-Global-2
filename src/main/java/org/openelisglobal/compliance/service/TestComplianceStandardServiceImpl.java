package org.openelisglobal.compliance.service;

import java.util.List;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.dao.TestComplianceStandardDAO;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.TestComplianceStandard;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TestComplianceStandard operations.
 *
 * Follows OpenELIS service patterns:
 * - Extends AuditableBaseObjectServiceImpl for standard CRUD operations
 * - Uses @Service annotation for Spring component scanning
 * - @Transactional boundaries at service level (not controller)
 * - Delegates to DAO layer for data access
 * - Implements business logic validation
 */
@Service
public class TestComplianceStandardServiceImpl extends AuditableBaseObjectServiceImpl<TestComplianceStandard, String>
        implements TestComplianceStandardService {

    @Autowired
    protected TestComplianceStandardDAO baseObjectDAO;

    TestComplianceStandardServiceImpl() {
        super(TestComplianceStandard.class);
    }

    @Override
    protected TestComplianceStandardDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestComplianceStandard testComplianceStandard) {
        getBaseObjectDAO().getData(testComplianceStandard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getComplianceStandardsForTest(String testId) {
        return getBaseObjectDAO().getComplianceStandardsForTest(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getTestsForComplianceStandard(String complianceStandardId) {
        return getBaseObjectDAO().getTestsForComplianceStandard(complianceStandardId);
    }

    @Override
    @Transactional(readOnly = true)
    public TestComplianceStandard getTestComplianceStandardAssociation(String testId, String complianceStandardId) {
        return getBaseObjectDAO().getTestComplianceStandardAssociation(testId, complianceStandardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getMandatoryComplianceStandardsForTest(String testId) {
        return getBaseObjectDAO().getMandatoryComplianceStandardsForTest(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getTestsWithMandatoryCompliance() {
        return getBaseObjectDAO().getTestsWithMandatoryCompliance();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getComplianceStandardsForTestOrdered(String testId) {
        return getBaseObjectDAO().getComplianceStandardsForTestOrdered(testId);
    }

    @Override
    @Transactional
    public void updateTestComplianceStandards(List<TestComplianceStandard> testComplianceStandards, Test test,
                                             String currentUser, List<ComplianceStandard> newStandards) {

        if (test == null || test.getId() == null) {
            throw new IllegalArgumentException("Test cannot be null and must have an ID");
        }

        String testId = test.getId();

        // Remove existing associations for this test
        removeAllComplianceStandardsForTest(testId);

        // Add new associations
        if (testComplianceStandards != null && !testComplianceStandards.isEmpty()) {
            for (TestComplianceStandard association : testComplianceStandards) {
                association.setTest(test);
                association.setSysUserId(currentUser);

                // Validate association
                if (association.getComplianceStandard() == null ||
                    association.getComplianceStandard().getId() == null) {
                    throw new IllegalArgumentException("ComplianceStandard cannot be null and must have an ID");
                }

                save(association);
            }
        }
    }

    @Override
    @Transactional
    public void removeAllComplianceStandardsForTest(String testId) {
        getBaseObjectDAO().removeAllComplianceStandardsForTest(testId);
    }

    @Override
    @Transactional
    public void removeAllTestsForComplianceStandard(String complianceStandardId) {
        getBaseObjectDAO().removeAllTestsForComplianceStandard(complianceStandardId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean testHasComplianceStandards(String testId) {
        return getBaseObjectDAO().testHasComplianceStandards(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean complianceStandardHasTests(String complianceStandardId) {
        return getBaseObjectDAO().complianceStandardHasTests(complianceStandardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getAllWithEntities() {
        return getBaseObjectDAO().getAllWithEntities();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getTestsWithActiveCompliance() {
        return getBaseObjectDAO().getTestsWithActiveCompliance();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean duplicateTestComplianceStandardExists(TestComplianceStandard testComplianceStandard)
            throws LIMSRuntimeException {

        if (testComplianceStandard == null ||
            testComplianceStandard.getTest() == null ||
            testComplianceStandard.getComplianceStandard() == null) {
            return false;
        }

        TestComplianceStandard existing = getTestComplianceStandardAssociation(
            testComplianceStandard.getTest().getId(),
            testComplianceStandard.getComplianceStandard().getId()
        );

        // If existing association found, check if it's different from the current one
        if (existing != null) {
            // If we're updating an existing association, make sure we're not comparing it with itself
            return !existing.getId().equals(testComplianceStandard.getId());
        }

        return false;
    }

    @Override
    @Transactional
    public TestComplianceStandard associateTestWithComplianceStandard(String testId, String complianceStandardId,
                                                                      boolean mandatory, String applicableParameters,
                                                                      String currentUser) {

        if (testId == null || complianceStandardId == null) {
            throw new IllegalArgumentException("Test ID and Compliance Standard ID cannot be null");
        }

        // Check if association already exists
        TestComplianceStandard existing = getTestComplianceStandardAssociation(testId, complianceStandardId);
        if (existing != null) {
            throw new LIMSRuntimeException("Association between test and compliance standard already exists");
        }

        // Create new association
        TestComplianceStandard association = new TestComplianceStandard();

        // Set test (create proxy object)
        Test test = new Test();
        test.setId(testId);
        association.setTest(test);

        // Set compliance standard (create proxy object)
        ComplianceStandard standard = new ComplianceStandard();
        standard.setId(complianceStandardId);
        association.setComplianceStandard(standard);

        // Set association properties
        association.setMandatory(mandatory);
        association.setApplicableParameters(applicableParameters);
        association.setSysUserId(currentUser);

        // Save and return
        return save(association);
    }

    @Override
    @Transactional
    public boolean disassociateTestFromComplianceStandard(String testId, String complianceStandardId) {
        TestComplianceStandard association = getTestComplianceStandardAssociation(testId, complianceStandardId);
        if (association != null) {
            delete(association);
            return true;
        }
        return false;
    }
}