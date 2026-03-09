package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ISort;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;

/**
 * Unit tests for NotebookFhirPersistenceServiceImpl. T005–T009: FHIR
 * persistence service behaviour.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotebookFhirPersistenceServiceTest {

    @Mock
    private FhirPersistanceService fhirPersistanceService;

    @Mock
    private FhirConfig fhirConfig;

    @Mock
    private FhirContext fhirContext;

    @Mock
    private IGenericClient fhirClient;

    @InjectMocks
    private NotebookFhirPersistenceServiceImpl service;

    @Before
    public void setUp() {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("http://localhost:8080/fhir");
        when(fhirConfig.fhirContext()).thenReturn(fhirContext);
        when(fhirContext.newRestfulGenericClient("http://localhost:8080/fhir")).thenReturn(fhirClient);
    }

    // ===== T005: shouldUpsertQuestionnaireByUrl =====

    /**
     * T005: saveOrUpdateQuestionnaire returns the ID from the FHIR store response
     * bundle when the store accepts the upsert.
     */
    @Test
    public void shouldUpsertQuestionnaireByUrl() throws FhirLocalPersistingException {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId("q-123");
        questionnaire.setUrl("http://openelis-global.org/notebook/bacteriology");

        Questionnaire returnedQuestionnaire = new Questionnaire();
        returnedQuestionnaire.setId(new IdType("Questionnaire", "stored-uuid-001"));

        Bundle responseBundle = new Bundle();
        BundleEntryComponent entry = new BundleEntryComponent();
        entry.setResource(returnedQuestionnaire);
        responseBundle.addEntry(entry);

        when(fhirPersistanceService.updateFhirResourceInFhirStore(questionnaire)).thenReturn(responseBundle);

        String result = service.saveOrUpdateQuestionnaire(questionnaire);

        assertEquals("stored-uuid-001", result);
        verify(fhirPersistanceService, times(1)).updateFhirResourceInFhirStore(questionnaire);
    }

    // ===== T006: shouldNotCreateDuplicateOnSecondUpload =====

    /**
     * T006: Calling saveOrUpdateQuestionnaire twice for the same questionnaire
     * invokes updateFhirResourceInFhirStore both times (upsert — no duplicate
     * creation).
     */
    @Test
    public void shouldNotCreateDuplicateOnSecondUpload() throws FhirLocalPersistingException {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId("q-456");
        questionnaire.setUrl("http://openelis-global.org/notebook/immunology");

        Questionnaire returnedQuestionnaire = new Questionnaire();
        returnedQuestionnaire.setId(new IdType("Questionnaire", "stored-uuid-002"));

        Bundle responseBundle = new Bundle();
        BundleEntryComponent entry = new BundleEntryComponent();
        entry.setResource(returnedQuestionnaire);
        responseBundle.addEntry(entry);

        when(fhirPersistanceService.updateFhirResourceInFhirStore(questionnaire)).thenReturn(responseBundle);

        String firstResult = service.saveOrUpdateQuestionnaire(questionnaire);
        String secondResult = service.saveOrUpdateQuestionnaire(questionnaire);

        // Both calls return same ID — the FHIR store handles the upsert
        assertEquals("stored-uuid-002", firstResult);
        assertEquals("stored-uuid-002", secondResult);
        // update (not create) called both times — prevents duplicates
        verify(fhirPersistanceService, times(2)).updateFhirResourceInFhirStore(questionnaire);
    }

    // ===== T007: shouldSaveInProgressResponse =====

    /**
     * T007: saveQuestionnaireResponse stores a QuestionnaireResponse and returns
     * the ID assigned by the FHIR store.
     */
    @Test
    public void shouldSaveInProgressResponse() throws FhirLocalPersistingException {
        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setId("qr-local-01");
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);

        QuestionnaireResponse returnedResponse = new QuestionnaireResponse();
        returnedResponse.setId(new IdType("QuestionnaireResponse", "fhir-qr-uuid-001"));

        Bundle responseBundle = new Bundle();
        BundleEntryComponent entry = new BundleEntryComponent();
        entry.setResource(returnedResponse);
        responseBundle.addEntry(entry);

        when(fhirPersistanceService.createFhirResourceInFhirStore(response)).thenReturn(responseBundle);

        String result = service.saveQuestionnaireResponse(response);

        assertEquals("fhir-qr-uuid-001", result);
        verify(fhirPersistanceService, times(1)).createFhirResourceInFhirStore(response);
    }

    // ===== T008: shouldReturnLatestInProgressDraft =====

    /**
     * T008: getInProgressResponse returns the most recent in-progress
     * QuestionnaireResponse when one exists in the FHIR store.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnLatestInProgressDraft() {
        String specimenId = "specimen-abc";
        String questionnaireUrl = "http://openelis-global.org/notebook/bacteriology";

        QuestionnaireResponse existingResponse = new QuestionnaireResponse();
        existingResponse.setId(new IdType("QuestionnaireResponse", "in-progress-qr-001"));
        existingResponse.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);

        Bundle searchBundle = new Bundle();
        BundleEntryComponent entry = new BundleEntryComponent();
        entry.setResource(existingResponse);
        searchBundle.addEntry(entry);

        // Build mock chain:
        // search().forResource(...).where(...).and(...).and(...).sort()...
        @SuppressWarnings("rawtypes")
        IUntypedQuery untypedQueryRaw = org.mockito.Mockito.mock(IUntypedQuery.class);
        IQuery<Bundle> query = org.mockito.Mockito.mock(IQuery.class);
        ISort<Bundle> sort = org.mockito.Mockito.mock(ISort.class);
        IQuery<Bundle> returnBundleQuery = org.mockito.Mockito.mock(IQuery.class);

        // fhirConfig chain set up in setUp()
        when(fhirClient.search()).thenReturn(untypedQueryRaw);
        when(untypedQueryRaw.forResource(QuestionnaireResponse.class)).thenReturn(query);
        when(query.where(any(ca.uhn.fhir.rest.gclient.ICriterion.class))).thenReturn(query);
        when(query.and(any(ca.uhn.fhir.rest.gclient.ICriterion.class))).thenReturn(query);
        when(query.sort()).thenReturn(sort);
        when(sort.descending(any(ca.uhn.fhir.rest.gclient.IParam.class))).thenReturn(returnBundleQuery);
        when(returnBundleQuery.count(1)).thenReturn(returnBundleQuery);
        when(returnBundleQuery.returnBundle(Bundle.class)).thenReturn(returnBundleQuery);
        when(returnBundleQuery.execute()).thenReturn(searchBundle);

        Optional<QuestionnaireResponse> result = service.getInProgressResponse(specimenId, questionnaireUrl);

        assertTrue(result.isPresent());
        assertEquals("in-progress-qr-001", result.get().getIdElement().getIdPart());
    }

    // ===== T009: shouldReturnEmptyWhenNoDraftExists =====

    /**
     * T009: getInProgressResponse returns Optional.empty() when no in-progress
     * QuestionnaireResponse exists for the given specimen and questionnaire URL.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldReturnEmptyWhenNoDraftExists() {
        String specimenId = "specimen-xyz";
        String questionnaireUrl = "http://openelis-global.org/notebook/immunology";

        Bundle emptyBundle = new Bundle();

        IUntypedQuery untypedQueryRaw = org.mockito.Mockito.mock(IUntypedQuery.class);
        IQuery<Bundle> query = org.mockito.Mockito.mock(IQuery.class);
        ISort<Bundle> sort = org.mockito.Mockito.mock(ISort.class);
        IQuery<Bundle> returnBundleQuery = org.mockito.Mockito.mock(IQuery.class);

        // fhirConfig chain set up in setUp()
        when(fhirClient.search()).thenReturn(untypedQueryRaw);
        when(untypedQueryRaw.forResource(QuestionnaireResponse.class)).thenReturn(query);
        when(query.where(any(ca.uhn.fhir.rest.gclient.ICriterion.class))).thenReturn(query);
        when(query.and(any(ca.uhn.fhir.rest.gclient.ICriterion.class))).thenReturn(query);
        when(query.sort()).thenReturn(sort);
        when(sort.descending(any(ca.uhn.fhir.rest.gclient.IParam.class))).thenReturn(returnBundleQuery);
        when(returnBundleQuery.count(1)).thenReturn(returnBundleQuery);
        when(returnBundleQuery.returnBundle(Bundle.class)).thenReturn(returnBundleQuery);
        when(returnBundleQuery.execute()).thenReturn(emptyBundle);

        Optional<QuestionnaireResponse> result = service.getInProgressResponse(specimenId, questionnaireUrl);

        assertFalse(result.isPresent());
    }
}
