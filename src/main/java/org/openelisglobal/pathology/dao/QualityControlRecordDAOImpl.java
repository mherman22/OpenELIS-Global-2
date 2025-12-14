package org.openelisglobal.pathology.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCStatus;
import org.openelisglobal.pathology.valueholder.PathologyEnums.QCType;
import org.openelisglobal.pathology.valueholder.QualityControlRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QualityControlRecordDAOImpl extends BaseDAOImpl<QualityControlRecord, Integer>
        implements QualityControlRecordDAO {

    public QualityControlRecordDAOImpl() {
        super(QualityControlRecord.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityControlRecord> findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM QualityControlRecord q WHERE q.sampleItem.id = :sampleItemId ORDER BY q.recordedAt DESC";
            Query<QualityControlRecord> query = entityManager.unwrap(Session.class).createQuery(hql,
                    QualityControlRecord.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QualityControlRecords by sample item ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityControlRecord> findByTypeAndStatus(QCType qcType, QCStatus status) {
        try {
            String hql = "FROM QualityControlRecord q WHERE q.qcType = :qcType AND q.status = :status ORDER BY q.recordedAt DESC";
            Query<QualityControlRecord> query = entityManager.unwrap(Session.class).createQuery(hql,
                    QualityControlRecord.class);
            query.setParameter("qcType", qcType);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QualityControlRecords by type and status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QualityControlRecord findLatestBySampleAndType(String sampleItemId, QCType qcType) {
        try {
            String hql = "FROM QualityControlRecord q WHERE q.sampleItem.id = :sampleItemId AND q.qcType = :qcType ORDER BY q.recordedAt DESC";
            Query<QualityControlRecord> query = entityManager.unwrap(Session.class).createQuery(hql,
                    QualityControlRecord.class);
            query.setParameter("sampleItemId", sampleItemId);
            query.setParameter("qcType", qcType);
            query.setMaxResults(1);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding latest QualityControlRecord by sample and type", e);
        }
    }
}
