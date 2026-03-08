package org.openelisglobal.notebook.controller.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.form.ManifestImportForm;
import org.openelisglobal.notebook.service.ManifestImportService;
import org.openelisglobal.notebook.service.ManifestImportService.ManifestImportResult;
import org.openelisglobal.notebook.service.ManifestImportService.ParseError;
import org.openelisglobal.notebook.service.ManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for manifest CSV import operations. Handles uploading
 * manifest files and creating samples for notebook entries.
 */
@RestController
@RequestMapping(value = "/rest/notebook")
public class ManifestImportController extends BaseRestController {

    // Fields routed through ManifestImportForm → ManifestImportServiceImpl for
    // core SampleItem creation. "notes" is intentionally excluded: SampleItem has
    // no notes column, so notes must travel through storeExtraFields and be stored
    // in NotebookPageSample.data like any other lab-specific extra field.
    private static final Set<String> CORE_MANIFEST_FIELDS = Set.of("groupId", "sampleType", "collectionDate", "volume",
            "numOfSamples");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ManifestImportService manifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookPageDAO noteBookPageDAO;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * Get available sample types for column mapping validation.
     *
     * @return list of sample type descriptions
     */
    @PostMapping(value = "/sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSampleTypes() {
        // This endpoint can be implemented to return available sample types
        // for the frontend to validate against during column mapping
        return ResponseEntity.ok(List.of());
    }

    /**
     * Preview manifest CSV for a notebook entry. POST
     * /rest/notebook/entry/{entryId}/samples/preview-manifest
     *
     * @param entryId the notebook entry ID
     * @param file    the CSV file
     * @param form    column mapping configuration
     * @return parsed rows and validation errors
     */
    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") ManifestImportForm form) {

        // Verify entry exists
        java.util.Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService
                .getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Parse the CSV
            ParsedManifest parsed = manifestImportService.parseManifestCsv(inputStream, form);

            // Validate sample types
            List<ParseError> validationErrors = manifestImportService.validateSampleTypes(parsed);

            // Combine all errors
            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("totalRows", parsed.rows().size());
            response.put("totalSamples",
                    parsed.rows().stream().mapToInt(ManifestImportService.ManifestRow::numOfSamples).sum());
            response.put("rows", parsed.rows().stream().map(row -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowNumber", row.rowNumber());
                rowMap.put("groupId", row.groupId());
                rowMap.put("sampleType", row.sampleType());
                rowMap.put("collectionDate", row.collectionDate());
                rowMap.put("volume", row.volume());
                rowMap.put("numOfSamples", row.numOfSamples());
                rowMap.put("notes", row.notes());
                return rowMap;
            }).collect(Collectors.toList()));
            response.put("errors", allErrors.stream().map(error -> {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("rowNumber", error.rowNumber());
                errorMap.put("column", error.column());
                errorMap.put("message", error.message());
                return errorMap;
            }).collect(Collectors.toList()));
            response.put("valid", allErrors.isEmpty());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Create samples from manifest CSV for a notebook entry. POST
     * /rest/notebook/entry/{entryId}/samples/create-from-manifest
     *
     * @param entryId     the notebook entry ID
     * @param file        the CSV file
     * @param form        column mapping configuration
     * @param httpRequest for getting user session
     * @return creation result with created sample count
     */
    @PostMapping(value = "/entry/{entryId}/samples/create-from-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSamplesForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") ManifestImportForm form,
            HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService
                .getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Parse the CSV
            ParsedManifest parsed = manifestImportService.parseManifestCsv(inputStream, form);

            // Check for parsing errors
            if (!parsed.errors().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "CSV parsing errors");
                response.put("errors", parsed.errors().stream().map(error -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("rowNumber", error.rowNumber());
                    errorMap.put("column", error.column());
                    errorMap.put("message", error.message());
                    return errorMap;
                }).collect(Collectors.toList()));
                return ResponseEntity.badRequest().body(response);
            }

            // Validate sample types
            List<ParseError> validationErrors = manifestImportService.validateSampleTypes(parsed);
            if (!validationErrors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Sample type validation errors");
                response.put("errors", validationErrors.stream().map(error -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("rowNumber", error.rowNumber());
                    errorMap.put("column", error.column());
                    errorMap.put("message", error.message());
                    return errorMap;
                }).collect(Collectors.toList()));
                return ResponseEntity.badRequest().body(response);
            }

            // Create samples for the entry
            ManifestImportResult result = manifestImportService.createSamplesForEntry(entryId, parsed, sysUserId);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.errors().isEmpty());
            response.put("entryId", entryId);
            response.put("totalRequested", result.totalRequested());
            response.put("totalCreated", result.totalCreated());
            response.put("createdSamples", result.createdSamples().stream().map(sample -> {
                Map<String, Object> sampleMap = new HashMap<>();
                sampleMap.put("id", sample.getId());
                sampleMap.put("externalId", sample.getExternalId());
                sampleMap.put("sampleType",
                        sample.getTypeOfSample() != null ? sample.getTypeOfSample().getDescription() : null);
                return sampleMap;
            }).collect(Collectors.toList()));

            if (!result.errors().isEmpty()) {
                response.put("errors", result.errors().stream().map(error -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("rowNumber", error.rowNumber());
                    errorMap.put("column", error.column());
                    errorMap.put("message", error.message());
                    return errorMap;
                }).collect(Collectors.toList()));
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Generic manifest import for any lab page. Uses the {@code manifestColumns}
     * schema stored in the page's JSONB {@code data} field to parse the CSV and
     * route each column to either the core {@link ManifestImportForm} (for the six
     * standard fields) or the {@link NotebookPageSample#data} map (for lab-specific
     * extras). This single endpoint replaces all per-lab manifest import endpoints.
     *
     * <p>
     * POST /rest/notebook/entry/{entryId}/page/{pageId}/import-manifest
     *
     * @param entryId       the notebook entry to attach samples to
     * @param pageId        the template page whose {@code manifestColumns} schema
     *                      drives parsing; also the page samples are linked to
     * @param file          the uploaded CSV file
     * @param columnMapping JSON object mapping field keys to CSV header names, e.g.
     *                      {@code {"groupId":"Sample ID","projectName":"Project"}}
     * @param request       HTTP request for user session
     */
    @PostMapping(value = "/entry/{entryId}/page/{pageId}/import-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> genericImportManifest(@PathVariable("entryId") Integer entryId,
            @PathVariable("pageId") Integer pageId, @RequestPart("file") MultipartFile file,
            @RequestPart("columnMapping") String columnMapping, HttpServletRequest request) {

        // Verify entry
        if (notebookEntryService.getMatch("id", entryId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Load page for its schema
        NoteBookPage page = noteBookPageDAO.get(pageId).orElse(null);
        if (page == null) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        // Parse user's column-mapping JSON: { fieldKey -> csvHeaderName }
        Map<String, String> userMapping;
        try {
            userMapping = objectMapper.readValue(columnMapping, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid columnMapping JSON: " + e.getMessage()));
        }

        // Split into core fields (handled by ManifestImportForm) and extras
        ManifestImportForm form = buildCoreForm(userMapping);
        Map<String, String> extraMapping = userMapping.entrySet().stream().filter(
                e -> !CORE_MANIFEST_FIELDS.contains(e.getKey()) && e.getValue() != null && !e.getValue().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        byte[] csvBytes;
        try {
            csvBytes = file.getBytes();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }

        // Parse core fields
        ParsedManifest parsed;
        try (InputStream in = new ByteArrayInputStream(csvBytes)) {
            parsed = manifestImportService.parseManifestCsv(in, form);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }

        if (!parsed.errors().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "error", "CSV parsing errors", "errors", toErrorList(parsed.errors())));
        }

        List<ParseError> validationErrors = manifestImportService.validateSampleTypes(parsed);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Sample type validation errors",
                    "errors", toErrorList(validationErrors)));
        }

        // Create samples
        ManifestImportResult result = manifestImportService.createSamplesForEntry(entryId, parsed, sysUserId);

        // If there are extra fields, parse them from the CSV and store in each
        // sample's NotebookPageSample.data. Pass the numOfSamples column name so
        // the helper can advance the sample cursor by the correct amount per row.
        if (!extraMapping.isEmpty() && !result.createdSamples().isEmpty()) {
            storeExtraFields(csvBytes, extraMapping, userMapping.get("numOfSamples"), pageId, result.createdSamples());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.errors().isEmpty());
        response.put("entryId", entryId);
        response.put("totalRequested", result.totalRequested());
        response.put("totalCreated", result.totalCreated());
        response.put("createdSamples", result.createdSamples().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("externalId", s.getExternalId());
            m.put("sampleType", s.getTypeOfSample() != null ? s.getTypeOfSample().getDescription() : null);
            return m;
        }).collect(Collectors.toList()));
        if (!result.errors().isEmpty()) {
            response.put("errors", toErrorList(result.errors()));
        }
        return ResponseEntity.ok(response);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    /** Map the subset of core fields from the user's column mapping into a form. */
    private ManifestImportForm buildCoreForm(Map<String, String> userMapping) {
        ManifestImportForm form = new ManifestImportForm();
        form.setGroupIdColumn(userMapping.get("groupId"));
        form.setSampleTypeColumn(userMapping.get("sampleType"));
        form.setCollectionDateColumn(userMapping.get("collectionDate"));
        form.setVolumeColumn(userMapping.get("volume"));
        form.setNumOfSamplesColumn(userMapping.get("numOfSamples"));
        form.setNotesColumn(userMapping.get("notes"));
        return form;
    }

    /**
     * Re-parse the CSV to extract extra (lab-specific) column values and store them
     * in each sample's {@link NotebookPageSample#data} map.
     *
     * <p>
     * Matching is done by row order. {@code numOfSamplesColName} is the CSV header
     * name of the "number of samples" column (may be {@code null} when absent from
     * the manifest). When present, the cursor advances by N per row so all N
     * samples created from a single CSV row receive the same extra data.
     */
    private void storeExtraFields(byte[] csvBytes, Map<String, String> extraMapping, String numOfSamplesColName,
            Integer pageId, List<org.openelisglobal.sampleitem.valueholder.SampleItem> createdSamples) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null)
                return;

            String[] headers = headerLine.split(",", -1);
            // Build header-name → column-index map (case-insensitive)
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Build fieldKey → column-index for extra fields
            Map<String, Integer> extraIndexes = new HashMap<>();
            for (Map.Entry<String, String> e : extraMapping.entrySet()) {
                Integer idx = headerIndex.get(e.getValue().trim().toLowerCase());
                if (idx != null) {
                    extraIndexes.put(e.getKey(), idx);
                }
            }
            if (extraIndexes.isEmpty())
                return;

            // Column index for numOfSamples — used to advance cursor by N per row
            Integer numOfSamplesIdx = (numOfSamplesColName != null && !numOfSamplesColName.isBlank())
                    ? headerIndex.get(numOfSamplesColName.trim().toLowerCase())
                    : null;

            int sampleCursor = 0;
            String line;
            while ((line = reader.readLine()) != null && sampleCursor < createdSamples.size()) {
                if (line.isBlank())
                    continue;
                String[] vals = line.split(",", -1);

                // How many SampleItems were created for this CSV row?
                int n = 1;
                if (numOfSamplesIdx != null && numOfSamplesIdx < vals.length) {
                    try {
                        int parsed = Integer.parseInt(vals[numOfSamplesIdx].trim());
                        if (parsed > 0)
                            n = parsed;
                    } catch (NumberFormatException ignored) {
                        // default to 1
                    }
                }

                // Extract extra values for this row
                Map<String, Object> extraData = new HashMap<>();
                for (Map.Entry<String, Integer> e : extraIndexes.entrySet()) {
                    int idx = e.getValue();
                    if (idx < vals.length) {
                        String val = vals[idx].trim();
                        if (!val.isEmpty()) {
                            extraData.put(e.getKey(), val);
                        }
                    }
                }

                // Apply the same extra data to ALL n samples created from this row
                if (!extraData.isEmpty()) {
                    for (int i = 0; i < n && sampleCursor + i < createdSamples.size(); i++) {
                        org.openelisglobal.sampleitem.valueholder.SampleItem item = createdSamples
                                .get(sampleCursor + i);
                        NotebookPageSample existing = notebookPageSampleService.getBySampleItemIdAndPageId(item.getId(),
                                pageId);
                        if (existing != null) {
                            Map<String, Object> merged = existing.getData() != null ? new HashMap<>(existing.getData())
                                    : new HashMap<>();
                            merged.putAll(extraData);
                            existing.setData(merged);
                            notebookPageSampleService.update(existing);
                        }
                    }
                }
                sampleCursor += n;
            }
        } catch (IOException e) {
            // Non-fatal — extra fields won't be stored but core import succeeded
        }
    }

    private List<Map<String, Object>> toErrorList(List<ParseError> errors) {
        return errors.stream().map(err -> {
            Map<String, Object> m = new HashMap<>();
            m.put("rowNumber", err.rowNumber());
            m.put("column", err.column());
            m.put("message", err.message());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }
}
