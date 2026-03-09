package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.form.NotebookFhirResponseForm;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookFhirPersistenceService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for FHIR-native notebook engine endpoints.
 *
 * <p>
 * T021: GET /rest/notebook/fhir/{notebookId}/questionnaire — retrieves the
 * latest active FHIR Questionnaire for a given notebook template. <br>
 * T022: POST /rest/notebook/fhir/{notebookId}/response — saves a
 * QuestionnaireResponse for a given specimen. <br>
 * T023: GET /rest/notebook/fhir/{notebookId}/response/in-progress — returns the
 * current in-progress draft for a given specimen.
 */
@RestController
@RequestMapping(value = "/rest/notebook/fhir")
public class NotebookFhirController extends BaseRestController {

    @Autowired
    private NotebookFhirPersistenceService notebookFhirPersistenceService;

    @Autowired
    private NoteBookService noteBookService;

    /**
     * T021: GET /rest/notebook/fhir/{notebookId}/questionnaire
     *
     * <p>
     * Returns the latest active FHIR Questionnaire for the given notebook template.
     * The questionnaire URL is derived from the notebook's questionnaireUuid /
     * workflowType.
     *
     * @param notebookId the notebook template ID
     * @return 200 with the Questionnaire, 404 if the notebook or questionnaire is
     *         not found
     */
    @GetMapping(value = "/{notebookId}/questionnaire", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Questionnaire> getQuestionnaire(@PathVariable("notebookId") Integer notebookId) {
        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        String questionnaireUrl = buildQuestionnaireUrl(notebook);
        if (questionnaireUrl == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<Questionnaire> questionnaire = notebookFhirPersistenceService.getLatestQuestionnaire(questionnaireUrl);
        return questionnaire.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * T022: POST /rest/notebook/fhir/{notebookId}/response
     *
     * <p>
     * Creates a new QuestionnaireResponse for the given notebook and specimen. The
     * response is stored in the FHIR server with status derived from the form.
     *
     * @param notebookId the notebook template ID
     * @param form       the response form containing specimenId, status, and
     *                   responses map
     * @param request    the HTTP request (for sysUserId)
     * @return 200 with the assigned FHIR ID, 404 if the notebook is not found, 400
     *         if the specimenId is missing
     */
    @PostMapping(value = "/{notebookId}/response", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String, String>> saveQuestionnaireResponse(
            @PathVariable("notebookId") Integer notebookId, @RequestBody NotebookFhirResponseForm form,
            HttpServletRequest request) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        if (form.getSpecimenId() == null || form.getSpecimenId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String questionnaireUrl = buildQuestionnaireUrl(notebook);

        QuestionnaireResponse response = new QuestionnaireResponse();
        if (questionnaireUrl != null) {
            response.setQuestionnaire(questionnaireUrl);
        }
        response.getSubject().setReference("Specimen/" + form.getSpecimenId());

        QuestionnaireResponseStatus status = resolveResponseStatus(form.getStatus());
        response.setStatus(status);

        // Map flat response fields to FHIR QuestionnaireResponse items
        if (form.getResponses() != null) {
            form.getResponses().forEach((linkId, value) -> {
                QuestionnaireResponse.QuestionnaireResponseItemComponent item = response.addItem();
                item.setLinkId(linkId);
                QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answer = item.addAnswer();
                answer.setValue(new org.hl7.fhir.r4.model.StringType(String.valueOf(value)));
            });
        }

        String fhirId = notebookFhirPersistenceService.saveQuestionnaireResponse(response);
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveQuestionnaireResponse",
                "Saved QuestionnaireResponse with FHIR ID: " + fhirId + " for notebook: " + notebookId);

        return ResponseEntity.ok(java.util.Map.of("fhirId", fhirId != null ? fhirId : ""));
    }

    /**
     * T023: GET /rest/notebook/fhir/{notebookId}/response/in-progress
     *
     * <p>
     * Returns the current in-progress QuestionnaireResponse draft for the given
     * notebook and specimen.
     *
     * @param notebookId the notebook template ID
     * @param specimenId the specimen FHIR ID
     * @return 200 with the QuestionnaireResponse, 404 if none found
     */
    @GetMapping(value = "/{notebookId}/response/in-progress", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<QuestionnaireResponse> getInProgressResponse(@PathVariable("notebookId") Integer notebookId,
            @RequestParam(value = "specimenId", required = true) String specimenId) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        String questionnaireUrl = buildQuestionnaireUrl(notebook);
        if (questionnaireUrl == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<QuestionnaireResponse> response = notebookFhirPersistenceService.getInProgressResponse(specimenId,
                questionnaireUrl);
        return response.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Derives the canonical Questionnaire URL for a notebook template. Uses
     * questionnaireUuid if set, otherwise falls back to workflowType-based URL.
     */
    private String buildQuestionnaireUrl(NoteBook notebook) {
        if (notebook.getQuestionnaireUuid() != null && !notebook.getQuestionnaireUuid().isBlank()) {
            return "http://openelis-global.org/notebook/" + notebook.getWorkflowType();
        }
        if (notebook.getWorkflowType() != null && !notebook.getWorkflowType().isBlank()) {
            return "http://openelis-global.org/notebook/" + notebook.getWorkflowType();
        }
        return null;
    }

    private QuestionnaireResponseStatus resolveResponseStatus(String status) {
        if (status == null) {
            return QuestionnaireResponseStatus.INPROGRESS;
        }
        switch (status.toLowerCase()) {
        case "completed":
            return QuestionnaireResponseStatus.COMPLETED;
        case "amended":
            return QuestionnaireResponseStatus.AMENDED;
        case "entered-in-error":
            return QuestionnaireResponseStatus.ENTEREDINERROR;
        case "stopped":
            return QuestionnaireResponseStatus.STOPPED;
        default:
            return QuestionnaireResponseStatus.INPROGRESS;
        }
    }
}
