package org.openelisglobal.pathology.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.pathology.valueholder.PathologyEnums.SampleCategory;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;

public interface PathologySampleRegistrationService extends BaseObjectService<PathologySampleRegistration, Integer> {

    /**
     * Register a new pathology sample with metadata
     *
     * @param registration Sample registration entity
     * @return Saved registration with generated ID
     */
    PathologySampleRegistration registerSample(PathologySampleRegistration registration);

    /**
     * Search samples with filters and pagination
     *
     * @param category Optional category filter
     * @param studyId  Optional study ID filter
     * @param offset   Pagination offset
     * @param limit    Pagination limit
     * @return List of matching registrations
     */
    List<PathologySampleRegistration> searchSamples(SampleCategory category, String studyId, int offset, int limit);

    /**
     * Find registration by sample item ID
     *
     * @param sampleItemId Sample item ID
     * @return PathologySampleRegistration or null
     */
    PathologySampleRegistration findBySampleItemId(String sampleItemId);

    /**
     * Find all registrations by category
     *
     * @param category Sample category
     * @return List of registrations
     */
    List<PathologySampleRegistration> findByCategory(SampleCategory category);
}
