package org.openelisglobal.pathology.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.pathology.dao.QualityControlRecordDAO;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCStatus;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCType;
import org.openelisglobal.pathology.valueholder.QualityControlRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QualityControlRecordServiceImpl extends AuditableBaseObjectServiceImpl<QualityControlRecord, Integer>
        implements QualityControlRecordService {

    @Autowired
    protected QualityControlRecordDAO baseObjectDAO;

    @Autowired(required = false)
    private NotebookPageSampleService notebookPageSampleService;

    QualityControlRecordServiceImpl() {
        super(QualityControlRecord.class);
    }

    @Override
    protected QualityControlRecordDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public QualityControlRecord recordQC(QualityControlRecord qcRecord, Integer pageId) {
        // Set default timestamp if not provided
        if (qcRecord.getRecordedAt() == null) {
            qcRecord.setRecordedAt(new Timestamp(System.currentTimeMillis()));
        }

        // Save the QC record
        Integer id = insert(qcRecord);
        QualityControlRecord savedRecord = get(id);

        // Integrate with notebook workflow if available and page ID is provided
        if (notebookPageSampleService != null && pageId != null && savedRecord.getSampleItem() != null) {
            try {
                String sampleItemId = savedRecord.getSampleItem().getId();
                NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleItemId,
                        pageId);

                if (pageSample != null) {
                    // Update page sample status based on QC result
                    if (QCStatus.PASS.equals(savedRecord.getStatus())) {
                        // QC passed - mark page as completed
                        pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                        pageSample.setCompletedAt(savedRecord.getRecordedAt());

                        LogEvent.logInfo(this.getClass().getSimpleName(), "recordQC",
                                "QC passed for sample " + sampleItemId + " on page " + pageId + " - marking COMPLETED");
                    } else if (QCStatus.FAIL.equals(savedRecord.getStatus())) {
                        // QC failed - mark page as skipped (sample needs reprocessing)
                        pageSample.setStatus(NotebookPageSample.Status.SKIPPED);

                        LogEvent.logInfo(this.getClass().getSimpleName(), "recordQC", "QC failed for sample "
                                + sampleItemId + " on page " + pageId + " - marking SKIPPED for reprocessing");
                    }

                    notebookPageSampleService.update(pageSample);
                }
            } catch (Exception e) {
                // Notebook integration is optional - don't fail QC recording if it fails
                LogEvent.logWarn(this.getClass().getSimpleName(), "recordQC",
                        "Could not update notebook page sample status for QC: " + e.getMessage());
            }
        }

        return savedRecord;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityControlRecord> findBySampleItemId(String sampleItemId) {
        return getBaseObjectDAO().findBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityControlRecord> findByTypeAndStatus(QCType qcType, QCStatus status) {
        return getBaseObjectDAO().findByTypeAndStatus(qcType, status);
    }

    @Override
    @Transactional(readOnly = true)
    public QualityControlRecord findLatestBySampleAndType(String sampleItemId, QCType qcType) {
        return getBaseObjectDAO().findLatestBySampleAndType(sampleItemId, qcType);
    }
}
