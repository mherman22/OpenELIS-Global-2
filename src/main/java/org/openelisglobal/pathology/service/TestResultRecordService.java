package org.openelisglobal.pathology.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.pathology.valueholder.PathologyEnums.TestType;
import org.openelisglobal.pathology.valueholder.TestResultRecord;

public interface TestResultRecordService extends BaseObjectService<TestResultRecord, Integer> {

    /**
     * Record a test result and update notebook page sample status. If provided with
     * a page ID, marks the sample as completed on that workflow page.
     *
     * @param testResult The test result record to save
     * @param pageId     Optional notebook page ID for workflow tracking
     * @return The saved test result record
     */
    TestResultRecord recordTestResult(TestResultRecord testResult, Integer pageId);

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
     * @param testType The type of test (HISTOCHEMISTRY, IMMUNOHISTOCHEMISTRY, etc.)
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
