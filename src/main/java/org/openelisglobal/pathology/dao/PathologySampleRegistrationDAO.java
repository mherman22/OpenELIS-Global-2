package org.openelisglobal.pathology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pathology.valueholder.PathologyEnums.SampleCategory;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;

public interface PathologySampleRegistrationDAO extends BaseDAO<PathologySampleRegistration, Integer> {

    /**
     * Find registration by sample item ID
     *
     * @param sampleItemId Sample item ID
     * @return PathologySampleRegistration or null if not found
     */
    PathologySampleRegistration findBySampleItemId(String sampleItemId);

    /**
     * Find all registrations by category
     *
     * @param category Sample category (CLINICAL or RESEARCH)
     * @return List of registrations
     */
    List<PathologySampleRegistration> findByCategory(SampleCategory category);

    /**
     * Find registrations by study ID (research samples)
     *
     * @param studyId Study identifier
     * @return List of registrations
     */
    List<PathologySampleRegistration> findByStudyId(String studyId);

    /**
     * Find registrations by patient ID (clinical samples)
     *
     * @param patientId Patient identifier
     * @return List of registrations
     */
    List<PathologySampleRegistration> findByPatientId(String patientId);

    /**
     * Search registrations with pagination
     *
     * @param category Optional category filter
     * @param studyId  Optional study ID filter
     * @param offset   Pagination offset
     * @param limit    Pagination limit
     * @return List of registrations
     */
    List<PathologySampleRegistration> searchSamples(SampleCategory category, String studyId, int offset, int limit);
}
