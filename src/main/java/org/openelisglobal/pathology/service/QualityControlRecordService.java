package org.openelisglobal.pathology.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCStatus;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCType;
import org.openelisglobal.pathology.valueholder.QualityControlRecord;

/**
 * Service interface for QualityControlRecord operations. Provides QC inspection
 * tracking for pathology workflow and integrates with notebook page sample
 * status updates.
 */
public interface QualityControlRecordService extends BaseObjectService<QualityControlRecord, Integer> {

    /**
     * Record a quality control inspection and update notebook page sample status.
     * If QC passes, the sample progresses to COMPLETED status on the current page.
     * If QC fails, the sample is marked as SKIPPED and may be routed for
     * reprocessing.
     *
     * @param qcRecord the QC record to save
     * @param pageId   the notebook page ID where this QC is being performed
     *                 (optional)
     * @return the saved QC record
     */
    QualityControlRecord recordQC(QualityControlRecord qcRecord, Integer pageId);

    /**
     * Find QC records by sample item ID.
     *
     * @param sampleItemId the sample item ID
     * @return list of QC records for the sample
     */
    List<QualityControlRecord> findBySampleItemId(String sampleItemId);

    /**
     * Find QC records by type and status.
     *
     * @param qcType the QC type
     * @param status the QC status
     * @return list of matching QC records
     */
    List<QualityControlRecord> findByTypeAndStatus(QCType qcType, QCStatus status);

    /**
     * Find the most recent QC record for a sample and QC type.
     *
     * @param sampleItemId the sample item ID
     * @param qcType       the QC type
     * @return the most recent QC record or null
     */
    QualityControlRecord findLatestBySampleAndType(String sampleItemId, QCType qcType);
}
