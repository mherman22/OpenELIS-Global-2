package org.openelisglobal.pathology.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pathology.valueholder.PathologyEnums.SampleCategory;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PathologySampleRegistrationDAOImpl extends BaseDAOImpl<PathologySampleRegistration, Integer>
        implements PathologySampleRegistrationDAO {

    public PathologySampleRegistrationDAOImpl() {
        super(PathologySampleRegistration.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PathologySampleRegistration findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM PathologySampleRegistration p WHERE p.sampleItem.id = :sampleItemId";
            Query<PathologySampleRegistration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    PathologySampleRegistration.class);
            query.setParameter("sampleItemId", sampleItemId);
            query.setMaxResults(1);
            List<PathologySampleRegistration> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding PathologySampleRegistration by sample item ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySampleRegistration> findByCategory(SampleCategory category) {
        try {
            String hql = "FROM PathologySampleRegistration p WHERE p.category = :category ORDER BY p.receivingDate DESC";
            Query<PathologySampleRegistration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    PathologySampleRegistration.class);
            query.setParameter("category", category);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding PathologySampleRegistrations by category", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySampleRegistration> findByStudyId(String studyId) {
        try {
            String hql = "FROM PathologySampleRegistration p WHERE p.studyId = :studyId ORDER BY p.receivingDate DESC";
            Query<PathologySampleRegistration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    PathologySampleRegistration.class);
            query.setParameter("studyId", studyId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding PathologySampleRegistrations by study ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySampleRegistration> findByPatientId(String patientId) {
        try {
            String hql = "FROM PathologySampleRegistration p WHERE p.patientId = :patientId ORDER BY p.receivingDate DESC";
            Query<PathologySampleRegistration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    PathologySampleRegistration.class);
            query.setParameter("patientId", patientId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding PathologySampleRegistrations by patient ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySampleRegistration> searchSamples(SampleCategory category, String studyId, int offset,
            int limit) {
        try {
            StringBuilder hql = new StringBuilder("FROM PathologySampleRegistration p WHERE 1=1");

            if (category != null) {
                hql.append(" AND p.category = :category");
            }
            if (studyId != null && !studyId.trim().isEmpty()) {
                hql.append(" AND p.studyId = :studyId");
            }
            hql.append(" ORDER BY p.receivingDate DESC");

            Query<PathologySampleRegistration> query = entityManager.unwrap(Session.class).createQuery(hql.toString(),
                    PathologySampleRegistration.class);

            if (category != null) {
                query.setParameter("category", category);
            }
            if (studyId != null && !studyId.trim().isEmpty()) {
                query.setParameter("studyId", studyId);
            }

            query.setFirstResult(offset);
            query.setMaxResults(limit);

            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error searching PathologySampleRegistrations", e);
        }
    }
}
