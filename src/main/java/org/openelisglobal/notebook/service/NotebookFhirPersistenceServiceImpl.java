package org.openelisglobal.notebook.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotebookFhirPersistenceServiceImpl implements NotebookFhirPersistenceService {

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Autowired
    private FhirConfig fhirConfig;

    private IGenericClient getFhirClient() {
        return fhirConfig.fhirContext().newRestfulGenericClient(fhirConfig.getLocalFhirStorePath());
    }

    /**
     * T011: Upserts a FHIR Questionnaire by URL to the FHIR store. Uses
     * updateFhirResourceInFhirStore — upsert semantics are handled at FHIR store
     * level via the resource's URL canonical identifier.
     *
     * @return the assigned FHIR ID part (UUID string), or the existing ID if the
     *         bundle response is empty
     */
    @Override
    public String saveOrUpdateQuestionnaire(Questionnaire questionnaire) {
        try {
            Bundle result = fhirPersistanceService.updateFhirResourceInFhirStore(questionnaire);
            if (result != null && !result.getEntry().isEmpty()) {
                return result.getEntryFirstRep().getResource().getIdElement().getIdPart();
            }
        } catch (FhirLocalPersistingException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "saveOrUpdateQuestionnaire",
                    "Failed to upsert Questionnaire to FHIR store: " + e.getMessage());
        }
        return questionnaire.getIdElement().getIdPart();
    }

    /**
     * T012: Creates a FHIR QuestionnaireResponse in the FHIR store.
     *
     * @return the assigned FHIR ID part, or the existing ID on failure
     */
    @Override
    public String saveQuestionnaireResponse(QuestionnaireResponse response) {
        try {
            Bundle result = fhirPersistanceService.createFhirResourceInFhirStore(response);
            if (result != null && !result.getEntry().isEmpty()) {
                return result.getEntryFirstRep().getResource().getIdElement().getIdPart();
            }
        } catch (FhirLocalPersistingException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "saveQuestionnaireResponse",
                    "Failed to create QuestionnaireResponse in FHIR store: " + e.getMessage());
        }
        return response.getIdElement().getIdPart();
    }

    /**
     * T013: Finds the latest in-progress QuestionnaireResponse for a given specimen
     * and questionnaire URL.
     *
     * @param specimenId       the specimen FHIR ID
     * @param questionnaireUrl the canonical URL of the Questionnaire
     * @return an Optional containing the most recent in-progress response, or empty
     *         if none found
     */
    @Override
    public Optional<QuestionnaireResponse> getInProgressResponse(String specimenId, String questionnaireUrl) {
        IGenericClient client = getFhirClient();
        Bundle bundle = client.search().forResource(QuestionnaireResponse.class)
                .where(QuestionnaireResponse.QUESTIONNAIRE.hasId(questionnaireUrl))
                .and(QuestionnaireResponse.SUBJECT.hasId("Specimen/" + specimenId))
                .and(QuestionnaireResponse.STATUS.exactly().code("in-progress")).sort()
                .descending(QuestionnaireResponse.AUTHORED).count(1).returnBundle(Bundle.class).execute();
        if (bundle.getEntry().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of((QuestionnaireResponse) bundle.getEntryFirstRep().getResource());
    }

    /**
     * T014: Retrieves the latest active Questionnaire by canonical URL.
     *
     * @param questionnaireUrl the canonical URL of the Questionnaire
     * @return an Optional containing the latest active Questionnaire, or empty if
     *         not found
     */
    @Override
    public Optional<Questionnaire> getLatestQuestionnaire(String questionnaireUrl) {
        IGenericClient client = getFhirClient();
        Bundle bundle = client.search().forResource(Questionnaire.class)
                .where(Questionnaire.URL.matches().value(questionnaireUrl))
                .and(Questionnaire.STATUS.exactly().code("active")).sort().descending("_lastUpdated").count(1)
                .returnBundle(Bundle.class).execute();
        if (bundle.getEntry().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of((Questionnaire) bundle.getEntryFirstRep().getResource());
    }
}
