package org.openelisglobal.pathology.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.pathology.dao.TestResultRecordDAO;
import org.openelisglobal.pathology.valueholder.PathologyEnums.TestType;
import org.openelisglobal.pathology.valueholder.TestResultRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestResultRecordServiceImpl extends AuditableBaseObjectServiceImpl<TestResultRecord, Integer>
        implements TestResultRecordService {

    @Autowired
    protected TestResultRecordDAO baseObjectDAO;

    @Autowired(required = false)
    private NotebookPageSampleService notebookPageSampleService;

    public TestResultRecordServiceImpl() {
        super(TestResultRecord.class);
    }

    @Override
    protected TestResultRecordDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public TestResultRecord recordTestResult(TestResultRecord testResult, Integer pageId) {
        // Set timestamp if not provided
        if (testResult.getPerformedAt() == null) {
            testResult.setPerformedAt(new Timestamp(System.currentTimeMillis()));
        }

        // Insert the test result
        Integer id = insert(testResult);
        TestResultRecord savedResult = get(id);

        // Integrate with notebook workflow if available and page ID is provided
        if (notebookPageSampleService != null && pageId != null && savedResult.getSampleItem() != null) {
            try {
                String sampleItemId = savedResult.getSampleItem().getId();
                NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleItemId,
                        pageId);

                if (pageSample != null) {
                    // Mark the sample as completed on this page
                    pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                    pageSample.setCompletedAt(savedResult.getPerformedAt());
                    notebookPageSampleService.update(pageSample);

                    LogEvent.logInfo(this.getClass().getSimpleName(), "recordTestResult",
                            "Test result recorded for sample " + sampleItemId + " on page " + pageId
                                    + " - marking COMPLETED");
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "recordTestResult",
                        "Could not update notebook page sample status for test result: " + e.getMessage());
            }
        }

        return savedResult;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultRecord> findBySampleItemId(String sampleItemId) {
        return getBaseObjectDAO().findBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultRecord> findByTestType(TestType testType) {
        return getBaseObjectDAO().findByTestType(testType);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResultRecord findLatestBySampleAndType(String sampleItemId, TestType testType) {
        return getBaseObjectDAO().findLatestBySampleAndType(sampleItemId, testType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultRecord> findUnsignedResults() {
        return getBaseObjectDAO().findUnsignedResults();
    }
}
