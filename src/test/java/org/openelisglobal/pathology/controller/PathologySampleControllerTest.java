package org.openelisglobal.pathology.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for PathologySampleController REST endpoints
 */
public class PathologySampleControllerTest extends BaseWebContextSensitiveTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Integer TEST_SAMPLE_ITEM_ID_1 = 10001;
    private static final Integer TEST_SAMPLE_ITEM_ID_2 = 10002;
    private static final Integer TEST_STAFF_ID = 1;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/pathology-sample-test-data.xml");
    }

    @Test
    public void testRegisterClinicalSample_Success() throws Exception {
        Map<String, Object> requestBody = getStringObjectMap();
        String jsonRequest = mapToJson(requestBody);
        MvcResult result = mockMvc
                .perform(post("/rest/pathology/samples").contentType(MediaType.APPLICATION_JSON).content(jsonRequest))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fhirUuid").exists()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertTrue("Response should indicate success", response.get("success").asBoolean());
        assertNotNull("Response should contain ID", response.get("id"));
        assertNotNull("Response should contain FHIR UUID", response.get("fhirUuid"));
    }

    private static Map<String, Object> getStringObjectMap() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sampleItemId", String.valueOf(TEST_SAMPLE_ITEM_ID_1));
        requestBody.put("category", "CLINICAL");
        requestBody.put("patientId", "P12345");
        requestBody.put("requestingClinician", "Dr. Smith");
        requestBody.put("specimenSite", "Liver");
        requestBody.put("clinicalDetails", "Suspected hepatocellular carcinoma");
        requestBody.put("sampleSource", "Alert Hospital");
        requestBody.put("receivingStaffId", TEST_STAFF_ID);
        return requestBody;
    }

    @Test
    public void testRegisterResearchSample_Success() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sampleItemId", String.valueOf(TEST_SAMPLE_ITEM_ID_2));
        requestBody.put("category", "RESEARCH");
        requestBody.put("studyId", "STUDY-2024-001");
        requestBody.put("piName", "Dr. Johnson");
        requestBody.put("participantId", "PART-123");
        requestBody.put("ethicalApprovalRef", "IRB-2024-045");
        requestBody.put("sampleSource", "Alert Hospital");
        requestBody.put("receivingStaffId", TEST_STAFF_ID);

        String jsonRequest = mapToJson(requestBody);

        MvcResult result = mockMvc
                .perform(post("/rest/pathology/samples").contentType(MediaType.APPLICATION_JSON).content(jsonRequest))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.id").exists()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertTrue("Response should indicate success", response.get("success").asBoolean());
        assertNotNull("Response should contain ID", response.get("id"));
    }

    @Test
    public void testRegisterSample_ValidationError_MissingRequiredFields() throws Exception {
        // Prepare request body with missing required fields
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sampleItemId", String.valueOf(TEST_SAMPLE_ITEM_ID_1));
        requestBody.put("category", "CLINICAL");
        // Missing patientId and requestingClinician for clinical sample
        requestBody.put("sampleSource", "Alert Hospital");
        requestBody.put("receivingStaffId", TEST_STAFF_ID);

        String jsonRequest = mapToJson(requestBody);

        // Perform POST request - expecting bad request
        mockMvc.perform(post("/rest/pathology/samples").contentType(MediaType.APPLICATION_JSON).content(jsonRequest))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testRegisterSample_ValidationError_ResearchMissingStudyId() throws Exception {
        // Prepare request body with missing study ID for research sample
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sampleItemId", String.valueOf(TEST_SAMPLE_ITEM_ID_2));
        requestBody.put("category", "RESEARCH");
        requestBody.put("piName", "Dr. Johnson");
        // Missing required studyId
        requestBody.put("sampleSource", "Alert Hospital");
        requestBody.put("receivingStaffId", TEST_STAFF_ID);

        String jsonRequest = mapToJson(requestBody);

        // Perform POST request - expecting bad request
        mockMvc.perform(post("/rest/pathology/samples").contentType(MediaType.APPLICATION_JSON).content(jsonRequest))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testSearchSamples_FilterByCategory() throws Exception {
        // Perform GET request with category filter
        MvcResult result = mockMvc
                .perform(get("/rest/pathology/samples/search").param("category", "CLINICAL").param("offset", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").isArray()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertTrue("Response should indicate success", response.get("success").asBoolean());
        JsonNode dataNode = response.get("data");
        assertNotNull("Response should contain data", dataNode);

        // Verify all returned samples are clinical
        if (dataNode.isArray() && dataNode.size() > 0) {
            for (JsonNode sample : dataNode) {
                assertEquals("All samples should be CLINICAL category", "CLINICAL", sample.get("category").asText());
            }
        }
    }

    @Test
    public void testSearchSamples_FilterByStudyId() throws Exception {
        String testStudyId = "STUDY-2024-001";

        // Perform GET request with study ID filter
        MvcResult result = mockMvc
                .perform(get("/rest/pathology/samples/search").param("studyId", testStudyId).param("offset", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertTrue("Response should indicate success", response.get("success").asBoolean());
    }

    @Test
    public void testSearchSamples_Pagination() throws Exception {
        // Test pagination with limit
        MvcResult result = mockMvc
                .perform(get("/rest/pathology/samples/search").param("offset", "0").param("limit", "5"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.limit").value(5)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        JsonNode dataNode = response.get("data");
        if (dataNode.isArray()) {
            assertTrue("Returned samples should not exceed limit", dataNode.size() <= 5);
        }
    }

    @Test
    public void testGetSamplesByCategory_Clinical() throws Exception {
        // Perform GET request for clinical samples
        MvcResult result = mockMvc.perform(get("/rest/pathology/samples/category").param("category", "CLINICAL"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").isArray()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertTrue("Response should indicate success", response.get("success").asBoolean());
        JsonNode dataNode = response.get("data");

        // Verify all returned samples are clinical
        if (dataNode.isArray() && dataNode.size() > 0) {
            for (JsonNode sample : dataNode) {
                assertEquals("All samples should be CLINICAL category", "CLINICAL", sample.get("category").asText());
            }
        }
    }

    @Test
    public void testGetSamplesByCategory_Research() throws Exception {
        // Perform GET request for research samples
        MvcResult result = mockMvc.perform(get("/rest/pathology/samples/category").param("category", "RESEARCH"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertTrue("Response should indicate success", response.get("success").asBoolean());
    }

    @Test
    public void testRegisterSample_InvalidSampleItemId() throws Exception {
        // Prepare request body with non-existent sample item ID
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sampleItemId", "999999");
        requestBody.put("category", "CLINICAL");
        requestBody.put("patientId", "P12345");
        requestBody.put("sampleSource", "Alert Hospital");
        requestBody.put("receivingStaffId", TEST_STAFF_ID);

        String jsonRequest = mapToJson(requestBody);

        // Perform POST request - expecting bad request or error
        mockMvc.perform(post("/rest/pathology/samples").contentType(MediaType.APPLICATION_JSON).content(jsonRequest))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false));
    }
}
