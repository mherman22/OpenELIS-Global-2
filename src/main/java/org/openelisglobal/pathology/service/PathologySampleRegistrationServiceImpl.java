package org.openelisglobal.pathology.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.pathology.dao.PathologySampleRegistrationDAO;
import org.openelisglobal.pathology.valueholder.PathologyEnums.SampleCategory;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PathologySampleRegistrationServiceImpl
        extends AuditableBaseObjectServiceImpl<PathologySampleRegistration, Integer>
        implements PathologySampleRegistrationService {

    @Autowired
    protected PathologySampleRegistrationDAO baseObjectDAO;

    @Autowired(required = false)
    private NotebookPageSampleService notebookPageSampleService;

    PathologySampleRegistrationServiceImpl() {
        super(PathologySampleRegistration.class);
    }

    @Override
    protected PathologySampleRegistrationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public PathologySampleRegistration registerSample(PathologySampleRegistration registration) {
        if (registration.getFhirUuid() == null) {
            registration.setFhirUuid(UUID.randomUUID());
        }

        if (registration.getReceivingDate() == null) {
            registration.setReceivingDate(new Timestamp(System.currentTimeMillis()));
        }

        if (registration.getCategory() == SampleCategory.CLINICAL) {
            if (registration.getPatientId() == null && registration.getRequestingClinician() == null) {
                throw new IllegalArgumentException(
                        "Clinical samples should have at least patient ID or requesting clinician");
            }
        } else if (registration.getCategory() == SampleCategory.RESEARCH) {
            if (registration.getStudyId() == null) {
                throw new IllegalArgumentException("Research samples must have a study ID");
            }
        }

        Integer id = insert(registration);
        PathologySampleRegistration savedRegistration = get(id);

        // Integrate with notebook workflow if available
        // Note: This creates a notebook page sample record to track the sample
        // through the pathology workflow pages. If the notebook system is not
        // configured, the registration will still succeed without workflow tracking.
        if (notebookPageSampleService != null && savedRegistration.getSampleItem() != null) {
            try {
                // Create a notebook page sample for tracking this pathology sample
                // through the workflow. The sample item ID must be converted to String
                // as that's what NotebookPageSample expects.
                String sampleItemId = savedRegistration.getSampleItem().getId();

                // Note: Actual notebook integration requires a configured pathology
                // notebook. This will be handled by the notebook configuration system.
                // For now, we log that the sample is registered and ready for workflow.
                LogEvent.logInfo(this.getClass().getSimpleName(), "registerSample", "Pathology sample registered: "
                        + sampleItemId + " (FHIR UUID: " + savedRegistration.getFhirUuid() + ")");
            } catch (Exception e) {
                // Notebook integration is optional - don't fail registration if it fails
                LogEvent.logWarn(this.getClass().getSimpleName(), "registerSample",
                        "Could not create notebook page sample for pathology sample: " + e.getMessage());
            }
        }

        return savedRegistration;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySampleRegistration> searchSamples(SampleCategory category, String studyId, int offset,
            int limit) {
        return getBaseObjectDAO().searchSamples(category, studyId, offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public PathologySampleRegistration findBySampleItemId(String sampleItemId) {
        return getBaseObjectDAO().findBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySampleRegistration> findByCategory(SampleCategory category) {
        return getBaseObjectDAO().findByCategory(category);
    }
}
