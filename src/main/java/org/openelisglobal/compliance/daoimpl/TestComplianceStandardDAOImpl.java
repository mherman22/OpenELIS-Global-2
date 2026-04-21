package org.openelisglobal.compliance.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.TestComplianceStandardDAO;
import org.openelisglobal.compliance.valueholder.TestComplianceStandard;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for TestComplianceStandard entity.
 *
 * Follows OpenELIS DAO patterns:
 * - Extends BaseDAOImpl<TestComplianceStandard, String>
 * - Uses Hibernate Session for database operations
 * - Proper error handling with LIMSRuntimeException
 * - @Transactional annotations for transaction boundaries
 */
@Component
@Transactional
public class TestComplianceStandardDAOImpl extends BaseDAOImpl<TestComplianceStandard, String>
        implements TestComplianceStandardDAO {

    public TestComplianceStandardDAOImpl() {
        super(TestComplianceStandard.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestComplianceStandard testComplianceStandard) throws LIMSRuntimeException {
        try {
            TestComplianceStandard data = entityManager.unwrap(Session.class)
                .get(TestComplianceStandard.class, testComplianceStandard.getId());
            if (data != null) {
                PropertyUtils.copyProperties(testComplianceStandard, data);
            } else {
                testComplianceStandard.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getComplianceStandardsForTest(String testId) throws LIMSRuntimeException {
        try {
            String sql = "FROM TestComplianceStandard tcs WHERE tcs.test.id = :testId ORDER BY tcs.sortOrder";
            Query<TestComplianceStandard> query = entityManager.unwrap(Session.class)
                .createQuery(sql, TestComplianceStandard.class);
            query.setParameter("testId", testId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard getComplianceStandardsForTest()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getTestsForComplianceStandard(String complianceStandardId)
            throws LIMSRuntimeException {
        try {
            String sql = "FROM TestComplianceStandard tcs WHERE tcs.complianceStandard.id = :standardId " +
                        "ORDER BY tcs.test.description, tcs.sortOrder";
            Query<TestComplianceStandard> query = entityManager.unwrap(Session.class)
                .createQuery(sql, TestComplianceStandard.class);
            query.setParameter("standardId", complianceStandardId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard getTestsForComplianceStandard()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TestComplianceStandard getTestComplianceStandardAssociation(String testId, String complianceStandardId)
            throws LIMSRuntimeException {
        try {
            String sql = "FROM TestComplianceStandard tcs WHERE tcs.test.id = :testId " +
                        "AND tcs.complianceStandard.id = :standardId";
            Query<TestComplianceStandard> query = entityManager.unwrap(Session.class)
                .createQuery(sql, TestComplianceStandard.class);
            query.setParameter("testId", testId);
            query.setParameter("standardId", complianceStandardId);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard getTestComplianceStandardAssociation()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getMandatoryComplianceStandardsForTest(String testId)
            throws LIMSRuntimeException {
        try {
            String sql = "FROM TestComplianceStandard tcs WHERE tcs.test.id = :testId " +
                        "AND tcs.mandatory = true ORDER BY tcs.sortOrder";
            Query<TestComplianceStandard> query = entityManager.unwrap(Session.class)
                .createQuery(sql, TestComplianceStandard.class);
            query.setParameter("testId", testId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard getMandatoryComplianceStandardsForTest()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getTestsWithMandatoryCompliance() throws LIMSRuntimeException {
        try {
            String sql = "FROM TestComplianceStandard tcs WHERE tcs.mandatory = true " +
                        "ORDER BY tcs.test.description, tcs.sortOrder";
            Query<TestComplianceStandard> query = entityManager.unwrap(Session.class)
                .createQuery(sql, TestComplianceStandard.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard getTestsWithMandatoryCompliance()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getComplianceStandardsForTestOrdered(String testId)
            throws LIMSRuntimeException {
        try {
            String sql = "FROM TestComplianceStandard tcs WHERE tcs.test.id = :testId " +
                        "ORDER BY CAST(tcs.sortOrder AS INTEGER), tcs.complianceStandard.name";
            Query<TestComplianceStandard> query = entityManager.unwrap(Session.class)
                .createQuery(sql, TestComplianceStandard.class);
            query.setParameter("testId", testId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard getComplianceStandardsForTestOrdered()", e);
        }
    }

    @Override
    @Transactional
    public void removeAllComplianceStandardsForTest(String testId) throws LIMSRuntimeException {
        try {
            String sql = "DELETE FROM TestComplianceStandard tcs WHERE tcs.test.id = :testId";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("testId", testId);
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard removeAllComplianceStandardsForTest()", e);
        }
    }

    @Override
    @Transactional
    public void removeAllTestsForComplianceStandard(String complianceStandardId) throws LIMSRuntimeException {
        try {
            String sql = "DELETE FROM TestComplianceStandard tcs WHERE tcs.complianceStandard.id = :standardId";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("standardId", complianceStandardId);
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard removeAllTestsForComplianceStandard()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean testHasComplianceStandards(String testId) throws LIMSRuntimeException {
        try {
            String sql = "SELECT COUNT(*) FROM TestComplianceStandard tcs WHERE tcs.test.id = :testId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("testId", testId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard testHasComplianceStandards()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean complianceStandardHasTests(String complianceStandardId) throws LIMSRuntimeException {
        try {
            String sql = "SELECT COUNT(*) FROM TestComplianceStandard tcs WHERE tcs.complianceStandard.id = :standardId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("standardId", complianceStandardId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard complianceStandardHasTests()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getAllWithEntities() throws LIMSRuntimeException {
        try {
            String sql = "FROM TestComplianceStandard tcs " +
                        "JOIN FETCH tcs.test t " +
                        "JOIN FETCH tcs.complianceStandard cs " +
                        "ORDER BY t.description, cs.name";
            Query<TestComplianceStandard> query = entityManager.unwrap(Session.class)
                .createQuery(sql, TestComplianceStandard.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard getAllWithEntities()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestComplianceStandard> getTestsWithActiveCompliance() throws LIMSRuntimeException {
        try {
            String sql = "FROM TestComplianceStandard tcs " +
                        "WHERE tcs.complianceStandard.status = 'ACTIVE' " +
                        "ORDER BY tcs.test.description, tcs.sortOrder";
            Query<TestComplianceStandard> query = entityManager.unwrap(Session.class)
                .createQuery(sql, TestComplianceStandard.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestComplianceStandard getTestsWithActiveCompliance()", e);
        }
    }
}