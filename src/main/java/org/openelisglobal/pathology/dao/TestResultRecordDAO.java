package org.openelisglobal.pathology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pathology.valueholder.PathologyEnums.TestType;
import org.openelisglobal.pathology.valueholder.TestResultRecord;

public interface TestResultRecordDAO extends BaseDAO<TestResultRecord, Integer> {

    /**
     * Find all test results for a specific sample item.
     *
     * @param sampleItemId The sample item ID
     * @return List of test results for this sample
     */
    List<TestResultRecord> findBySampleItemId(String sampleItemId);

    /**
     * Find test results by test type.
     *
     * @param testType The type of test
     * @return List of test results of this type
     */
    List<TestResultRecord> findByTestType(TestType testType);

    /**
     * Find the latest test result for a sample and test type.
     *
     * @param sampleItemId The sample item ID
     * @param testType     The type of test
     * @return The most recent test result, or null if none found
     */
    TestResultRecord findLatestBySampleAndType(String sampleItemId, TestType testType);

    /**
     * Find unsigned test results (awaiting pathologist review).
     *
     * @return List of test results without pathologist sign-off
     */
    List<TestResultRecord> findUnsignedResults();
}
