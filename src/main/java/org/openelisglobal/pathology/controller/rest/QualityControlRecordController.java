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
import org.openelisglobal.pathology.service.QualityControlRecordService;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCStatus;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCType;
import org.openelisglobal.pathology.valueholder.QualityControlRecord;
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
 * REST controller for quality control operations in pathology workflow. Handles
 * QC checkpoints for initial inspection, block QC, and slide QC.
 */
@RestController
@RequestMapping("/rest/pathology/qc")
public class QualityControlRecordController extends BaseRestController {

    @Autowired
    private QualityControlRecordService qcService;

    @Autowired
    private SampleItemService sampleItemService;

    /**
     * Record quality control inspection results. Automatically updates notebook
     * page sample status based on QC outcome.
     *
     * @param request     QC inspection data
     * @param httpRequest HTTP servlet request for user session data
     * @return Created QC record with HTTP 201 status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QualityControlRecord> recordQC(@Valid @RequestBody QCRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            QualityControlRecord qcRecord = convertToEntity(request, sysUserId);
            QualityControlRecord savedRecord = qcService.recordQC(qcRecord, request.getPageId());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedRecord);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get QC records for a specific sample.
     *
     * @param sampleItemId Sample item ID
     * @return List of QC records
     */
    @GetMapping(value = "/sample", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<QualityControlRecord>> getBySampleItemId(@RequestParam String sampleItemId) {
        try {
            List<QualityControlRecord> records = qcService.findBySampleItemId(sampleItemId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get QC records by type and status. Useful for filtering failed QC samples for
     * reprocessing.
     *
     * @param qcType Type of QC (INITIAL_INSPECTION, BLOCK_QC, SLIDE_QC)
     * @param status QC status (PASS, FAIL)
     * @return List of QC records matching criteria
     */
    @GetMapping(value = "/filter", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<QualityControlRecord>> getByTypeAndStatus(@RequestParam QCType qcType,
            @RequestParam QCStatus status) {
        try {
            List<QualityControlRecord> records = qcService.findByTypeAndStatus(qcType, status);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the most recent QC record for a sample and QC type.
     *
     * @param sampleItemId Sample item ID
     * @param qcType       Type of QC
     * @return Latest QC record
     */
    @GetMapping(value = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QualityControlRecord> getLatestBySampleAndType(@RequestParam String sampleItemId,
            @RequestParam QCType qcType) {
        try {
            QualityControlRecord record = qcService.findLatestBySampleAndType(sampleItemId, qcType);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private QualityControlRecord convertToEntity(QCRequest request, String sysUserId) {
        QualityControlRecord qcRecord = new QualityControlRecord();

        // Get sample item
        SampleItem sampleItem = sampleItemService.get(request.getSampleItemId());
        if (sampleItem == null) {
            throw new IllegalArgumentException("Sample item not found: " + request.getSampleItemId());
        }
        qcRecord.setSampleItem(sampleItem);

        // Set QC fields
        qcRecord.setQcType(request.getQcType());
        qcRecord.setStatus(request.getStatus());
        qcRecord.setNotes(request.getNotes());
        qcRecord.setRecordedAt(new Timestamp(System.currentTimeMillis()));
        qcRecord.setTechnicianId(Integer.valueOf(sysUserId));

        return qcRecord;
    }

    /**
     * Request DTO for recording QC inspections.
     */
    @Setter
    @Getter
    public static class QCRequest {

        @jakarta.validation.constraints.NotNull
        @SafeHtml
        private String sampleItemId;

        @jakarta.validation.constraints.NotNull
        private QCType qcType;

        @jakarta.validation.constraints.NotNull
        private QCStatus status;

        @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
        private String notes;

        /**
         * Optional notebook page ID for workflow tracking. If provided, the
         * corresponding NotebookPageSample status will be updated.
         */
        private Integer pageId;
    }
}
