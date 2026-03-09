package org.openelisglobal.notebook.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.controller.rest.NotebookFhirController;
import org.openelisglobal.notebook.form.NotebookFhirResponseForm;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookFhirPersistenceService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for NotebookFhirController. T016–T019: Controller endpoint
 * behaviour using standalone MockMvc.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NotebookFhirControllerTest {

    @Mock
    private NotebookFhirPersistenceService notebookFhirPersistenceService;

    @Mock
    private NoteBookService noteBookService;

    @InjectMocks
    private NotebookFhirController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ===== T016: getQuestionnaire returns 200 with questionnaire when found =====

    /**
     * T016: GET /rest/notebook/fhir/{notebookId}/questionnaire returns 200 and the
     * Questionnaire when the notebook and FHIR questionnaire both exist.
     */
    @Test
    public void getQuestionnaire_whenNotebookAndQuestionnaireExist_returns200() throws Exception {
        NoteBook notebook = new NoteBook();
        notebook.setId(1);
        notebook.setWorkflowType("bacteriology");
        notebook.setQuestionnaireUuid("some-uuid");

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId("q-bacteriology-001");
        questionnaire.setUrl("http://openelis-global.org/notebook/bacteriology");
        questionnaire.setTitle("Bacteriology Notebook");

        when(noteBookService.get(1)).thenReturn(notebook);
        when(notebookFhirPersistenceService.getLatestQuestionnaire("http://openelis-global.org/notebook/bacteriology"))
                .thenReturn(Optional.of(questionnaire));

        mockMvc.perform(get("/rest/notebook/fhir/1/questionnaire").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    // ===== T017: getQuestionnaire returns 404 when notebook does not exist =====

    /**
     * T017: GET /rest/notebook/fhir/{notebookId}/questionnaire returns 404 when
     * the notebook is not found.
     */
    @Test
    public void getQuestionnaire_whenNotebookNotFound_returns404() throws Exception {
        when(noteBookService.get(999)).thenReturn(null);

        mockMvc.perform(get("/rest/notebook/fhir/999/questionnaire").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    // ===== T018: saveQuestionnaireResponse returns 200 with fhirId =====

    /**
     * T018: POST /rest/notebook/fhir/{notebookId}/response returns 200 with the
     * FHIR ID when the notebook exists and the form contains a valid specimenId.
     */
    @Test
    public void saveQuestionnaireResponse_validRequest_returns200WithFhirId() throws Exception {
        NoteBook notebook = new NoteBook();
        notebook.setId(2);
        notebook.setWorkflowType("immunology");

        when(noteBookService.get(2)).thenReturn(notebook);
        when(notebookFhirPersistenceService.saveQuestionnaireResponse(any(QuestionnaireResponse.class)))
                .thenReturn("fhir-qr-uuid-saved");

        NotebookFhirResponseForm form = new NotebookFhirResponseForm();
        form.setSpecimenId("specimen-001");
        form.setStatus("in-progress");
        form.setResponses(Map.of("sampleType", "Blood", "volume", "5ml"));

        mockMvc.perform(post("/rest/notebook/fhir/2/response").contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fhirId").value("fhir-qr-uuid-saved"));
    }

    // ===== T019: getInProgressResponse returns 404 when none found =====

    /**
     * T019: GET /rest/notebook/fhir/{notebookId}/response/in-progress returns 404
     * when no in-progress response exists for the given specimen.
     */
    @Test
    public void getInProgressResponse_whenNoneExists_returns404() throws Exception {
        NoteBook notebook = new NoteBook();
        notebook.setId(3);
        notebook.setWorkflowType("tuberculosis");
        notebook.setQuestionnaireUuid("tb-uuid");

        when(noteBookService.get(3)).thenReturn(notebook);
        when(notebookFhirPersistenceService.getInProgressResponse(eq("specimen-xyz"),
                eq("http://openelis-global.org/notebook/tuberculosis"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/rest/notebook/fhir/3/response/in-progress").param("specimenId", "specimen-xyz")
                .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isNotFound());
    }
}
