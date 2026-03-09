package org.openelisglobal.notebook.controller.rest;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.service.ManifestImportService;
import org.openelisglobal.notebook.service.ManifestImportService.ManifestImportResult;
import org.openelisglobal.notebook.service.ManifestImportService.ManifestRow;
import org.openelisglobal.notebook.service.ManifestImportService.ParseError;
import org.openelisglobal.notebook.service.ManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for ManifestImportController — generic manifest import pipeline.
 *
 * <p>
 * Covers:
 * <ul>
 * <li>RFC 4180 CSV parsing ({@code parseCsvLine})
 * <li>Required-mapping validation ({@code validateRequiredMappings})
 * <li>Config-driven per-row validation ({@code validateConfigValues})
 * <li>TypeOfSample DB skip when {@code validValues} present
 * ({@code hasSampleTypeValidValues})
 * <li>Manifest-column extraction from page config ({@code getManifestColumns})
 * <li>Full {@code POST
 * /rest/notebook/entry/{entryId}/page/{pageId}/import-manifest} pipeline
 * </ul>
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ManifestImportControllerTest {

    // ── mocks ────────────────────────────────────────────────────────────────

    @Mock
    private ManifestImportService manifestImportService;

    @Mock
    private NotebookEntryService notebookEntryService;

    @Mock
    private NoteBookPageDAO noteBookPageDAO;

    @Mock
    private NotebookPageSampleService notebookPageSampleService;

    @InjectMocks
    private ManifestImportController controller;

    // ── shared ───────────────────────────────────────────────────────────────

    private MockMvc mockMvc;
    private MockHttpSession mockSession;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        mockSession = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        usd.setLoginName("testuser");
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
    }

    // =========================================================================
    // parseCsvLine — RFC 4180 compliance
    // =========================================================================

    @Test
    public void parseCsvLine_plainFields_splitsByComma() throws Exception {
        String[] result = invokeParseCsvLine("alpha,beta,gamma");
        assertArrayEquals(new String[] { "alpha", "beta", "gamma" }, result);
    }

    @Test
    public void parseCsvLine_quotedFieldContainingComma_treatedAsSingleField() throws Exception {
        // "Bale Mountains, Oromia" must not be split at the comma
        String[] result = invokeParseCsvLine("GRP-001,\"Bale Mountains, Oromia\",10");
        assertEquals(3, result.length);
        assertEquals("GRP-001", result[0]);
        assertEquals("Bale Mountains, Oromia", result[1]);
        assertEquals("10", result[2]);
    }

    @Test
    public void parseCsvLine_escapedDoubleQuoteInsideQuotedField_decodedCorrectly() throws Exception {
        // RFC 4180: "" inside quoted field = literal "
        String[] result = invokeParseCsvLine("GRP-001,\"He said \"\"hello\"\"\",notes");
        assertEquals(3, result.length);
        assertEquals("He said \"hello\"", result[1]);
    }

    @Test
    public void parseCsvLine_emptyField_returnsEmptyString() throws Exception {
        String[] result = invokeParseCsvLine("GRP-001,,notes");
        assertEquals(3, result.length);
        assertEquals("", result[1]);
    }

    @Test
    public void parseCsvLine_leadingTrailingSpaceOutsideQuotes_isTrimmed() throws Exception {
        String[] result = invokeParseCsvLine(" alpha , beta ");
        assertArrayEquals(new String[] { "alpha", "beta" }, result);
    }

    @Test
    public void parseCsvLine_singleField_noCrash() throws Exception {
        String[] result = invokeParseCsvLine("only");
        assertArrayEquals(new String[] { "only" }, result);
    }

    // =========================================================================
    // validateRequiredMappings
    // =========================================================================

    @Test
    public void validateRequiredMappings_allRequiredFieldsMapped_returnsEmpty() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()),
                column("sampleType", "Sample Type", true, List.of()), column("notes", "Notes", false, List.of()));

        Map<String, String> mapping = Map.of("groupId", "Sample ID", "sampleType", "Type");

        List<String> errors = invokeValidateRequiredMappings(mapping, columns);
        assertTrue("No errors expected when all required fields are mapped", errors.isEmpty());
    }

    @Test
    public void validateRequiredMappings_requiredFieldNotMapped_returnsError() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()),
                column("sampleType", "Sample Type", true, List.of()));

        // sampleType is required but not in the mapping
        Map<String, String> mapping = Map.of("groupId", "Sample ID");

        List<String> errors = invokeValidateRequiredMappings(mapping, columns);
        assertEquals(1, errors.size());
        assertTrue("Error should mention field label", errors.get(0).contains("Sample Type"));
    }

    @Test
    public void validateRequiredMappings_requiredFieldMappedToBlank_returnsError() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()));

        Map<String, String> mapping = new HashMap<>();
        mapping.put("groupId", "   "); // blank

        List<String> errors = invokeValidateRequiredMappings(mapping, columns);
        assertEquals(1, errors.size());
    }

    @Test
    public void validateRequiredMappings_optionalFieldNotMapped_noError() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()),
                column("notes", "Notes", false, List.of()));

        Map<String, String> mapping = Map.of("groupId", "Sample ID");
        // notes is optional and not mapped — should produce no error

        List<String> errors = invokeValidateRequiredMappings(mapping, columns);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void validateRequiredMappings_emptyColumnsList_returnsEmpty() throws Exception {
        List<String> errors = invokeValidateRequiredMappings(Map.of(), List.of());
        assertTrue(errors.isEmpty());
    }

    // =========================================================================
    // validateConfigValues
    // =========================================================================

    @Test
    public void validateConfigValues_allValuesValid_noErrors() throws Exception {
        String csv = "sampleType,groupId\nWhole Blood,GRP-001\nSerum,GRP-002\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        List<Map<String, Object>> columns = List
                .of(column("sampleType", "Sample Type", true, List.of("Whole Blood", "Serum", "Plasma")));

        Map<String, String> mapping = Map.of("sampleType", "sampleType");

        List<ParseError> errors = invokeValidateConfigValues(bytes, mapping, columns);
        assertTrue("No errors expected for valid values", errors.isEmpty());
    }

    @Test
    public void validateConfigValues_invalidValidValue_returnsError() throws Exception {
        String csv = "sampleType,groupId\nBacteria,GRP-001\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        List<Map<String, Object>> columns = List
                .of(column("sampleType", "Sample Type", false, List.of("Whole Blood", "Serum")));

        Map<String, String> mapping = Map.of("sampleType", "sampleType");

        List<ParseError> errors = invokeValidateConfigValues(bytes, mapping, columns);
        assertEquals(1, errors.size());
        assertEquals(2, errors.get(0).rowNumber()); // data row is row 2
        assertEquals("sampleType", errors.get(0).column());
        assertTrue(errors.get(0).message().contains("Bacteria"));
    }

    @Test
    public void validateConfigValues_requiredFieldEmpty_returnsError() throws Exception {
        String csv = "sampleType,groupId\n,GRP-001\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        List<Map<String, Object>> columns = List.of(column("sampleType", "Sample Type", true, List.of()));

        Map<String, String> mapping = Map.of("sampleType", "sampleType");

        List<ParseError> errors = invokeValidateConfigValues(bytes, mapping, columns);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).message().contains("required"));
    }

    @Test
    public void validateConfigValues_noConstrainedColumns_returnsEmpty() throws Exception {
        String csv = "groupId,notes\nGRP-001,some note\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        // Neither column is required nor has validValues
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", false, List.of()),
                column("notes", "Notes", false, List.of()));

        Map<String, String> mapping = Map.of("groupId", "groupId", "notes", "notes");

        List<ParseError> errors = invokeValidateConfigValues(bytes, mapping, columns);
        assertTrue("No constraints → no errors", errors.isEmpty());
    }

    @Test
    public void validateConfigValues_caseInsensitiveValidValues() throws Exception {
        // validValues list uses mixed case; CSV uses lowercase — should pass
        String csv = "sampleType\nwhole blood\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        List<Map<String, Object>> columns = List
                .of(column("sampleType", "Sample Type", false, List.of("Whole Blood", "Serum")));

        Map<String, String> mapping = Map.of("sampleType", "sampleType");

        List<ParseError> errors = invokeValidateConfigValues(bytes, mapping, columns);
        assertTrue("Case-insensitive match should pass", errors.isEmpty());
    }

    @Test
    public void validateConfigValues_quotedCsvFieldWithComma_parsedCorrectly() throws Exception {
        // A quoted CSV value containing a comma must not split incorrectly
        String csv = "storageCondition,groupId\n\"Cold, -80°C\",GRP-001\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        List<Map<String, Object>> columns = List
                .of(column("storageCondition", "Storage Condition", false, List.of("Cold, -80°C", "Room Temperature")));

        Map<String, String> mapping = Map.of("storageCondition", "storageCondition");

        List<ParseError> errors = invokeValidateConfigValues(bytes, mapping, columns);
        assertTrue("Quoted field with comma should validate as a single value", errors.isEmpty());
    }

    @Test
    public void validateConfigValues_multipleRowsOneInvalid_reportsCorrectRow() throws Exception {
        String csv = "sampleType\nWhole Blood\nInvalidType\nSerum\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        List<Map<String, Object>> columns = List
                .of(column("sampleType", "Sample Type", false, List.of("Whole Blood", "Serum")));

        Map<String, String> mapping = Map.of("sampleType", "sampleType");

        List<ParseError> errors = invokeValidateConfigValues(bytes, mapping, columns);
        assertEquals(1, errors.size());
        assertEquals(3, errors.get(0).rowNumber()); // row 3 (header=1, valid=2, invalid=3)
    }

    // =========================================================================
    // hasSampleTypeValidValues
    // =========================================================================

    @Test
    public void hasSampleTypeValidValues_sampleTypeHasValidValues_returnsTrue() throws Exception {
        List<Map<String, Object>> columns = List
                .of(column("sampleType", "Sample Type", true, List.of("DNA", "RNA", "Plasma")));

        assertTrue(invokeHasSampleTypeValidValues(columns));
    }

    @Test
    public void hasSampleTypeValidValues_sampleTypeHasEmptyList_returnsFalse() throws Exception {
        List<Map<String, Object>> columns = List.of(column("sampleType", "Sample Type", true, List.of()));

        assertFalse(invokeHasSampleTypeValidValues(columns));
    }

    @Test
    public void hasSampleTypeValidValues_noSampleTypeColumn_returnsFalse() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of("A", "B")));

        assertFalse(invokeHasSampleTypeValidValues(columns));
    }

    @Test
    public void hasSampleTypeValidValues_emptyColumnsList_returnsFalse() throws Exception {
        assertFalse(invokeHasSampleTypeValidValues(List.of()));
    }

    // =========================================================================
    // getManifestColumns
    // =========================================================================

    @Test
    public void getManifestColumns_configHasManifestColumns_returnsFromConfig() throws Exception {
        NoteBookPage page = new NoteBookPage();
        List<Map<String, Object>> cols = List.of(column("groupId", "Group ID", true, List.of()));
        Map<String, Object> config = new HashMap<>();
        config.put("manifestColumns", cols);
        page.setConfig(config);

        List<Map<String, Object>> result = invokeGetManifestColumns(page);
        assertEquals(1, result.size());
        assertEquals("groupId", result.get(0).get("field"));
    }

    @Test
    public void getManifestColumns_noConfig_returnsEmpty() throws Exception {
        NoteBookPage page = new NoteBookPage();
        // no config, no data
        List<Map<String, Object>> result = invokeGetManifestColumns(page);
        assertTrue("Should return empty list when no config", result.isEmpty());
    }

    @Test
    public void getManifestColumns_configHasNoManifestColumnsKey_returnsEmpty() throws Exception {
        NoteBookPage page = new NoteBookPage();
        Map<String, Object> config = new HashMap<>();
        config.put("otherKey", "value");
        page.setConfig(config);

        List<Map<String, Object>> result = invokeGetManifestColumns(page);
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // genericImportManifest endpoint — guard clauses
    // =========================================================================

    @Test
    public void genericImportManifest_entryNotFound_returns404() throws Exception {
        when(notebookEntryService.getMatch("id", 999)).thenReturn(Optional.empty());

        String csv = "groupId\nGRP-001\n";
        MockMultipartFile csvFile = csvFile(csv);

        mockMvc.perform(multipart("/rest/notebook/entry/999/page/1/import-manifest")
                .file(csvFile)
                .part(jsonPart("columnMapping", "{}"))
                .session(mockSession))
                .andExpect(status().isNotFound());
    }

    @Test
    public void genericImportManifest_pageNotFound_returns404() throws Exception {
        when(notebookEntryService.getMatch("id", 1)).thenReturn(Optional.of(new NotebookEntry()));
        when(noteBookPageDAO.get(999)).thenReturn(Optional.empty());

        MockMultipartFile csvFile = csvFile("groupId\nGRP-001\n");

        mockMvc.perform(multipart("/rest/notebook/entry/1/page/999/import-manifest")
                .file(csvFile)
                .part(jsonPart("columnMapping", "{}"))
                .session(mockSession))
                .andExpect(status().isNotFound());
    }

    @Test
    public void genericImportManifest_noSession_returns401() throws Exception {
        when(notebookEntryService.getMatch("id", 1)).thenReturn(Optional.of(new NotebookEntry()));
        when(noteBookPageDAO.get(1)).thenReturn(Optional.of(pageWithManifestColumns(List.of())));

        MockMultipartFile csvFile = csvFile("groupId\nGRP-001\n");

        // No session set — getSysUserId returns null
        mockMvc.perform(multipart("/rest/notebook/entry/1/page/1/import-manifest")
                .file(csvFile)
                .part(jsonPart("columnMapping", "{}")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void genericImportManifest_invalidColumnMappingJson_returns400() throws Exception {
        when(notebookEntryService.getMatch("id", 1)).thenReturn(Optional.of(new NotebookEntry()));
        when(noteBookPageDAO.get(1)).thenReturn(Optional.of(pageWithManifestColumns(List.of())));

        MockMultipartFile csvFile = csvFile("groupId\nGRP-001\n");

        mockMvc.perform(multipart("/rest/notebook/entry/1/page/1/import-manifest")
                .file(csvFile)
                .part(jsonPart("columnMapping", "NOT_JSON"))
                .session(mockSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // =========================================================================
    // genericImportManifest endpoint — required mapping validation
    // =========================================================================

    @Test
    public void genericImportManifest_requiredFieldNotMapped_returns400WithValidationErrors() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()),
                column("sampleType", "Sample Type", true, List.of("Whole Blood")));

        when(notebookEntryService.getMatch("id", 1)).thenReturn(Optional.of(new NotebookEntry()));
        when(noteBookPageDAO.get(1)).thenReturn(Optional.of(pageWithManifestColumns(columns)));

        MockMultipartFile csvFile = csvFile("groupId,sampleType\nGRP-001,Whole Blood\n");
        // sampleType is required but not in the mapping
        String mapping = objectMapper.writeValueAsString(Map.of("groupId", "groupId"));

        mockMvc.perform(multipart("/rest/notebook/entry/1/page/1/import-manifest").file(csvFile)
                .part(jsonPart("columnMapping", mapping)).session(mockSession)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.validationErrors").isArray());
    }

    // =========================================================================
    // genericImportManifest endpoint — config-driven validation
    // =========================================================================

    @Test
    public void genericImportManifest_invalidValidValue_returns400() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()),
                column("sampleType", "Sample Type", true, List.of("Whole Blood", "Serum")));

        when(notebookEntryService.getMatch("id", 1)).thenReturn(Optional.of(new NotebookEntry()));
        when(noteBookPageDAO.get(1)).thenReturn(Optional.of(pageWithManifestColumns(columns)));

        // The service parses core fields without knowing about validValues
        // (it sees "Bacteria" as a valid sampleType string at the parsing level)
        ParsedManifest parsed = new ParsedManifest(
                List.of(new ManifestRow(2, "GRP-001", "Bacteria", null, null, 1, null)), List.of());
        when(manifestImportService.parseManifestCsv(any(), any())).thenReturn(parsed);

        // "Bacteria" is not in the validValues list — validateConfigValues catches it
        MockMultipartFile csvFile = csvFile("groupId,sampleType\nGRP-001,Bacteria\n");
        String mapping = objectMapper.writeValueAsString(Map.of("groupId", "groupId", "sampleType", "sampleType"));

        mockMvc.perform(multipart("/rest/notebook/entry/1/page/1/import-manifest").file(csvFile)
                .part(jsonPart("columnMapping", mapping)).session(mockSession)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.errors").isArray());
    }

    // =========================================================================
    // genericImportManifest endpoint — TypeOfSample DB skip
    // =========================================================================

    @Test
    public void genericImportManifest_sampleTypeHasValidValues_skipsTypeOfSampleLookup() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()),
                column("sampleType", "Sample Type", true, List.of("DNA", "RNA")));

        when(notebookEntryService.getMatch("id", 1)).thenReturn(Optional.of(new NotebookEntry()));
        when(noteBookPageDAO.get(1)).thenReturn(Optional.of(pageWithManifestColumns(columns)));

        // Parsing succeeds with no errors
        ParsedManifest parsed = new ParsedManifest(List.of(new ManifestRow(2, "GRP-001", "DNA", null, null, 1, null)),
                List.of());
        when(manifestImportService.parseManifestCsv(any(), any())).thenReturn(parsed);
        when(manifestImportService.createSamplesForEntry(anyInt(), any(), any()))
                .thenReturn(new ManifestImportResult(1, 1, List.of(), List.of()));
        when(notebookPageSampleService.getByPageId(anyInt())).thenReturn(List.of());

        MockMultipartFile csvFile = csvFile("groupId,sampleType\nGRP-001,DNA\n");
        String mapping = objectMapper.writeValueAsString(Map.of("groupId", "groupId", "sampleType", "sampleType"));

        mockMvc.perform(multipart("/rest/notebook/entry/1/page/1/import-manifest").file(csvFile)
                .part(jsonPart("columnMapping", mapping)).session(mockSession)).andExpect(status().isOk());

        // The TypeOfSample DB lookup must NOT be called
        verify(manifestImportService, never()).validateSampleTypes(any());
    }

    @Test
    public void genericImportManifest_sampleTypeHasNoValidValues_callsTypeOfSampleLookup() throws Exception {
        // sampleType column with empty validValues → fall through to DB lookup
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()),
                column("sampleType", "Sample Type", true, List.of())); // no validValues

        when(notebookEntryService.getMatch("id", 1)).thenReturn(Optional.of(new NotebookEntry()));
        when(noteBookPageDAO.get(1)).thenReturn(Optional.of(pageWithManifestColumns(columns)));

        ParsedManifest parsed = new ParsedManifest(
                List.of(new ManifestRow(2, "GRP-001", "Whole Blood", null, null, 1, null)), List.of());
        when(manifestImportService.parseManifestCsv(any(), any())).thenReturn(parsed);
        when(manifestImportService.validateSampleTypes(any())).thenReturn(List.of()); // no type errors
        when(manifestImportService.createSamplesForEntry(anyInt(), any(), any()))
                .thenReturn(new ManifestImportResult(1, 1, List.of(), List.of()));
        when(notebookPageSampleService.getByPageId(anyInt())).thenReturn(List.of());

        MockMultipartFile csvFile = csvFile("groupId,sampleType\nGRP-001,Whole Blood\n");
        String mapping = objectMapper.writeValueAsString(Map.of("groupId", "groupId", "sampleType", "sampleType"));

        mockMvc.perform(multipart("/rest/notebook/entry/1/page/1/import-manifest").file(csvFile)
                .part(jsonPart("columnMapping", mapping)).session(mockSession)).andExpect(status().isOk());

        verify(manifestImportService).validateSampleTypes(any());
    }

    // =========================================================================
    // genericImportManifest endpoint — extra fields stored
    // =========================================================================

    @Test
    public void genericImportManifest_extraFieldsMapped_storesInNotebookPageSample() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()),
                column("sampleType", "Sample Type", true, List.of("Whole Blood")),
                column("projectName", "Project Name", false, List.of())); // extra field

        when(notebookEntryService.getMatch("id", 1)).thenReturn(Optional.of(new NotebookEntry()));
        when(noteBookPageDAO.get(1)).thenReturn(Optional.of(pageWithManifestColumns(columns)));

        ParsedManifest parsed = new ParsedManifest(
                List.of(new ManifestRow(2, "GRP-001", "Whole Blood", null, null, 1, null)), List.of());
        when(manifestImportService.parseManifestCsv(any(), any())).thenReturn(parsed);
        when(manifestImportService.validateSampleTypes(any())).thenReturn(List.of());

        SampleItem item = new SampleItem();
        item.setId("42");
        when(manifestImportService.createSamplesForEntry(anyInt(), any(), any()))
                .thenReturn(new ManifestImportResult(1, 1, List.of(item), List.of()));

        // Return a NotebookPageSample for the created item
        NotebookPageSample nps = new NotebookPageSample();
        nps.setId(10);
        nps.setSampleItemId("42");
        when(notebookPageSampleService.getByPageId(1)).thenReturn(List.of(nps));

        MockMultipartFile csvFile = csvFile("groupId,sampleType,projectName\nGRP-001,Whole Blood,ProjectX\n");
        String mapping = objectMapper.writeValueAsString(
                Map.of("groupId", "groupId", "sampleType", "sampleType", "projectName", "projectName"));

        mockMvc.perform(multipart("/rest/notebook/entry/1/page/1/import-manifest").file(csvFile)
                .part(jsonPart("columnMapping", mapping)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreated").value(1));

        // The extra field should have been persisted
        verify(notebookPageSampleService)
                .update(argThat(ps -> ps.getData() != null && "ProjectX".equals(ps.getData().get("projectName"))));
    }

    // =========================================================================
    // genericImportManifest endpoint — successful import response
    // =========================================================================

    @Test
    public void genericImportManifest_successfulImport_returnsCreatedCount() throws Exception {
        List<Map<String, Object>> columns = List.of(column("groupId", "Group ID", true, List.of()),
                column("sampleType", "Sample Type", true, List.of("Serum")));

        when(notebookEntryService.getMatch("id", 1)).thenReturn(Optional.of(new NotebookEntry()));
        when(noteBookPageDAO.get(1)).thenReturn(Optional.of(pageWithManifestColumns(columns)));

        ParsedManifest parsed = new ParsedManifest(List.of(new ManifestRow(2, "GRP-001", "Serum", null, null, 3, null),
                new ManifestRow(3, "GRP-002", "Serum", null, null, 2, null)), List.of());
        when(manifestImportService.parseManifestCsv(any(), any())).thenReturn(parsed);
        when(manifestImportService.createSamplesForEntry(anyInt(), any(), any()))
                .thenReturn(new ManifestImportResult(5, 5, List.of(), List.of()));
        when(notebookPageSampleService.getByPageId(anyInt())).thenReturn(List.of());

        MockMultipartFile csvFile = csvFile("groupId,sampleType\nGRP-001,Serum\nGRP-002,Serum\n");
        String mapping = objectMapper.writeValueAsString(Map.of("groupId", "groupId", "sampleType", "sampleType"));

        mockMvc.perform(multipart("/rest/notebook/entry/1/page/1/import-manifest").file(csvFile)
                .part(jsonPart("columnMapping", mapping)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.totalRequested").value(5))
                .andExpect(jsonPath("$.totalCreated").value(5));
    }

    // =========================================================================
    // private method helpers via reflection
    // =========================================================================

    @SuppressWarnings("unchecked")
    private String[] invokeParseCsvLine(String line) throws Exception {
        Method m = ManifestImportController.class.getDeclaredMethod("parseCsvLine", String.class);
        m.setAccessible(true);
        return (String[]) m.invoke(controller, line);
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeValidateRequiredMappings(Map<String, String> mapping, List<Map<String, Object>> columns)
            throws Exception {
        Method m = ManifestImportController.class.getDeclaredMethod("validateRequiredMappings", Map.class, List.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(controller, mapping, columns);
    }

    @SuppressWarnings("unchecked")
    private List<ParseError> invokeValidateConfigValues(byte[] csvBytes, Map<String, String> mapping,
            List<Map<String, Object>> columns) throws Exception {
        Method m = ManifestImportController.class.getDeclaredMethod("validateConfigValues", byte[].class, Map.class,
                List.class);
        m.setAccessible(true);
        return (List<ParseError>) m.invoke(controller, csvBytes, mapping, columns);
    }

    private boolean invokeHasSampleTypeValidValues(List<Map<String, Object>> columns) throws Exception {
        Method m = ManifestImportController.class.getDeclaredMethod("hasSampleTypeValidValues", List.class);
        m.setAccessible(true);
        return (boolean) m.invoke(controller, columns);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> invokeGetManifestColumns(NoteBookPage page) throws Exception {
        Method m = ManifestImportController.class.getDeclaredMethod("getManifestColumns", NoteBookPage.class);
        m.setAccessible(true);
        return (List<Map<String, Object>>) m.invoke(controller, page);
    }

    // =========================================================================
    // test-data builders
    // =========================================================================

    /** Builds a column definition map mirroring the manifestColumns JSON schema. */
    private Map<String, Object> column(String field, String label, boolean required, List<String> validValues) {
        Map<String, Object> col = new HashMap<>();
        col.put("field", field);
        col.put("label", label);
        col.put("required", required);
        if (!validValues.isEmpty()) {
            col.put("validValues", new ArrayList<>(validValues));
        }
        return col;
    }

    /**
     * Builds a NoteBookPage whose config.manifestColumns is set to the given list.
     */
    private NoteBookPage pageWithManifestColumns(List<Map<String, Object>> columns) {
        NoteBookPage page = new NoteBookPage();
        Map<String, Object> config = new HashMap<>();
        config.put("manifestColumns", new ArrayList<>(columns));
        page.setConfig(config);
        return page;
    }

    /** Wraps CSV string as a multipart file. */
    private MockMultipartFile csvFile(String csv) {
        return new MockMultipartFile("file", "manifest.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }

    /** Builds a MockPart for a plain text/JSON request part. */
    private jakarta.servlet.http.Part jsonPart(String name, String json) throws Exception {
        return new org.springframework.mock.web.MockPart(name, json.getBytes(StandardCharsets.UTF_8));
    }
}
