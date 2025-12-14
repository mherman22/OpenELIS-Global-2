package org.openelisglobal.pathology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.pathology.service.TestResultRecordService;
import org.openelisglobal.pathology.valueholder.PathologyEnums.ControlStatus;
import org.openelisglobal.pathology.valueholder.PathologyEnums.StainQuality;
import org.openelisglobal.pathology.valueholder.PathologyEnums.TestType;
import org.openelisglobal.pathology.valueholder.TestResultRecord;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for test result operations in pathology workflow. Handles
 * test execution, result recording, and pathologist sign-off.
 */
@RestController
@RequestMapping("/rest/pathology/test-results")
public class TestResultRecordController extends BaseRestController {

    @Autowired
    private TestResultRecordService testResultService;

    @Autowired
    private SampleItemService sampleItemService;

    /**
     * Record test result with control validation. Automatically updates notebook
     * page sample status to completed.
     *
     * @param request     Test result data
     * @param httpRequest HTTP servlet request for user session data
     * @return Created test result record with HTTP 201 status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestResultRecord> recordTestResult(@Valid @RequestBody TestResultRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            TestResultRecord testResult = convertToEntity(request, sysUserId);
            TestResultRecord savedResult = testResultService.recordTestResult(testResult, request.getPageId());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedResult);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get test results for a specific sample.
     *
     * @param sampleItemId Sample item ID
     * @return List of test results
     */
    @GetMapping(value = "/sample", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TestResultRecord>> getBySampleItemId(@RequestParam String sampleItemId) {
        try {
            List<TestResultRecord> results = testResultService.findBySampleItemId(sampleItemId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get test results by test type.
     *
     * @param testType Type of test
     * @return List of test results
     */
    @GetMapping(value = "/type", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TestResultRecord>> getByTestType(@RequestParam TestType testType) {
        try {
            List<TestResultRecord> results = testResultService.findByTestType(testType);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unsigned test results awaiting pathologist review.
     *
     * @return List of unsigned test results
     */
    @GetMapping(value = "/unsigned", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TestResultRecord>> getUnsignedResults() {
        try {
            List<TestResultRecord> results = testResultService.findUnsignedResults();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the most recent test result for a sample and test type.
     *
     * @param sampleItemId Sample item ID
     * @param testType     Type of test
     * @return Latest test result
     */
    @GetMapping(value = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestResultRecord> getLatestBySampleAndType(@RequestParam String sampleItemId,
            @RequestParam TestType testType) {
        try {
            TestResultRecord result = testResultService.findLatestBySampleAndType(sampleItemId, testType);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private TestResultRecord convertToEntity(TestResultRequest request, String sysUserId) {
        TestResultRecord testResult = new TestResultRecord();

        // Get sample item
        SampleItem sampleItem = sampleItemService.get(request.getSampleItemId());
        if (sampleItem == null) {
            throw new IllegalArgumentException("Sample item not found: " + request.getSampleItemId());
        }
        testResult.setSampleItem(sampleItem);

        // Set test fields
        testResult.setTestType(request.getTestType());
        testResult.setStainName(request.getStainName());
        testResult.setResultData(request.getResultData());
        testResult.setBlockSlideId(request.getBlockSlideId());
        testResult.setProtocolReferenceId(request.getProtocolReferenceId());

        // Set control statuses
        testResult.setPositiveControlStatus(request.getPositiveControlStatus());
        testResult.setNegativeControlStatus(request.getNegativeControlStatus());
        testResult.setStainQuality(request.getStainQuality());

        // Set audit fields
        testResult.setPerformedBy(Integer.valueOf(sysUserId));
        testResult.setPerformedAt(new Timestamp(System.currentTimeMillis()));

        return testResult;
    }

    /**
     * Request DTO for recording test results.
     */
    @Setter
    @Getter
    public static class TestResultRequest {

        @jakarta.validation.constraints.NotNull
        @SafeHtml
        private String sampleItemId;

        @jakarta.validation.constraints.NotNull
        private TestType testType;

        @jakarta.validation.constraints.NotNull
        @SafeHtml
        private String stainName;

        @jakarta.validation.constraints.NotNull
        @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
        private String resultData;

        @SafeHtml
        private String blockSlideId;

        private Integer protocolReferenceId;

        private ControlStatus positiveControlStatus;

        private ControlStatus negativeControlStatus;

        private StainQuality stainQuality;

        /**
         * Optional notebook page ID for workflow tracking. If provided, the
         * corresponding NotebookPageSample will be marked as COMPLETED.
         */
        private Integer pageId;
    }
}
