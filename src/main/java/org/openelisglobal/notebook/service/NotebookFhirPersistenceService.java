package org.openelisglobal.notebook.service;

import java.util.Optional;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

public interface NotebookFhirPersistenceService {

    String saveOrUpdateQuestionnaire(Questionnaire questionnaire);

    String saveQuestionnaireResponse(QuestionnaireResponse response);

    Optional<QuestionnaireResponse> getInProgressResponse(String specimenId, String questionnaireUrl);

    Optional<Questionnaire> getLatestQuestionnaire(String questionnaireUrl);
}
