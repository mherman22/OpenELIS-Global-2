package org.openelisglobal.pathology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCStatus;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCType;
import org.openelisglobal.pathology.valueholder.QualityControlRecord;

public interface QualityControlRecordDAO extends BaseDAO<QualityControlRecord, Integer> {
    List<QualityControlRecord> findBySampleItemId(String sampleItemId);

    List<QualityControlRecord> findByTypeAndStatus(QCType qcType, QCStatus status);

    QualityControlRecord findLatestBySampleAndType(String sampleItemId, QCType qcType);
}
