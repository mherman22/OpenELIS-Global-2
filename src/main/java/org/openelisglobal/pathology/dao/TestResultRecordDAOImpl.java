package org.openelisglobal.pathology.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pathology.valueholder.PathologyEnums.TestType;
import org.openelisglobal.pathology.valueholder.TestResultRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestResultRecordDAOImpl extends BaseDAOImpl<TestResultRecord, Integer> implements TestResultRecordDAO {

    public TestResultRecordDAOImpl() {
        super(TestResultRecord.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultRecord> findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM TestResultRecord tr WHERE tr.sampleItem.id = :sampleItemId ORDER BY tr.performedAt DESC";
            Query<TestResultRecord> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestResultRecord.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding TestResultRecords by sample item ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultRecord> findByTestType(TestType testType) {
        try {
            String hql = "FROM TestResultRecord tr WHERE tr.testType = :testType ORDER BY tr.performedAt DESC";
            Query<TestResultRecord> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestResultRecord.class);
            query.setParameter("testType", testType);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding TestResultRecords by test type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TestResultRecord findLatestBySampleAndType(String sampleItemId, TestType testType) {
        try {
            String hql = "FROM TestResultRecord tr WHERE tr.sampleItem.id = :sampleItemId "
                    + "AND tr.testType = :testType ORDER BY tr.performedAt DESC";
            Query<TestResultRecord> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestResultRecord.class);
            query.setParameter("sampleItemId", sampleItemId);
            query.setParameter("testType", testType);
            query.setMaxResults(1);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding latest TestResultRecord by sample and type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultRecord> findUnsignedResults() {
        try {
            String hql = "FROM TestResultRecord tr WHERE tr.pathologistSignoffId IS NULL "
                    + "ORDER BY tr.performedAt ASC";
            Query<TestResultRecord> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestResultRecord.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding unsigned TestResultRecords", e);
        }
    }
}
