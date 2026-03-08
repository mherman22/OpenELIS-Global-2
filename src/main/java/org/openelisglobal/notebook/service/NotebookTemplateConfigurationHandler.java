package org.openelisglobal.notebook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.notebook.dao.NoteBookDAO;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for creating notebook templates from JSON files.
 * <p>
 * Reads JSON files with the format:
 * 
 * <pre>
 * {
 *   "title": "Lab Name",
 *   "workflowType": "bacteriology",
 *   "departments": ["Bacteriology"],
 *   "objective": "...",
 *   "protocol": "...",
 *   "content": "...",
 *   "status": "ACTIVE",
 *   "pages": [
 *     { "order": 1, "title": "...", "pageType": "...", "instructions": "...", "content": "..." },
 *     ...
 *   ]
 * }
 * </pre>
 * 
 * Idempotent — skips if the template (by title) already exists. Pages are also
 * created only if missing (checked by page_order).
 * <p>
 * Config files are placed in:
 * <ul>
 * <li>Classpath: {@code configuration/notebook-templates/*.json}</li>
 * <li>Filesystem: {@code {configDir}/notebook-templates/*.json}</li>
 * </ul>
 * Adding a new lab requires only a JSON file — no Liquibase migration.
 */
@Component
@Transactional
public class NotebookTemplateConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private NoteBookDAO noteBookDAO;

    @Autowired
    private NoteBookPageDAO noteBookPageDAO;

    @Autowired
    private TestSectionService testSectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getDomainName() {
        return "notebook-templates";
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public int getLoadOrder() {
        return 210;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        JsonNode root = objectMapper.readTree(inputStream);

        String title = textOrNull(root, "title");
        if (title == null) {
            throw new IllegalArgumentException("Notebook template config " + fileName + " missing 'title'");
        }

        // Find or create the template
        NoteBook template = findTemplate(title);
        if (template == null) {
            template = createTemplate(root, title, fileName);
        } else {
            LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                    "Template '" + title + "' already exists — skipping creation.");
        }

        // Apply workflowType if present and not already set
        String workflowType = textOrNull(root, "workflowType");
        if (workflowType != null && !workflowType.equals(template.getWorkflowType())) {
            template.setWorkflowType(workflowType);
            noteBookDAO.update(template);
        }

        // Link departments if present
        JsonNode departmentsNode = root.get("departments");
        if (departmentsNode != null && departmentsNode.isArray()) {
            for (JsonNode deptNode : departmentsNode) {
                String deptName = deptNode.asText("").trim();
                if (deptName.isEmpty()) {
                    continue;
                }
                TestSection dept = testSectionService.getTestSectionByName(deptName);
                if (dept == null) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                            "Department '" + deptName + "' not found for template '" + title + "' in " + fileName);
                    continue;
                }
                if (!template.getDepartments().contains(dept)) {
                    template.getDepartments().add(dept);
                    noteBookDAO.update(template);
                }
            }
        }

        // Ensure all pages exist
        JsonNode pagesNode = root.get("pages");
        if (pagesNode == null || !pagesNode.isArray()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                    "No pages array in " + fileName + " for template '" + title + "'");
            return;
        }

        List<NoteBookPage> existingPages = noteBookPageDAO.getByNotebookId(template.getId());
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (JsonNode pageNode : pagesNode) {
            int order = pageNode.has("order") ? pageNode.get("order").asInt() : -1;
            if (order < 1) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                        "Skipping page with missing/invalid order in " + fileName);
                skipped++;
                continue;
            }

            NoteBookPage existing = existingPages.stream().filter(p -> order == p.getOrder()).findFirst().orElse(null);

            if (existing != null) {
                // Update pageType and config if changed
                boolean needsUpdate = false;
                String newPageType = textOrNull(pageNode, "pageType");
                if (newPageType != null && !newPageType.equals(existing.getPageType())) {
                    existing.setPageType(newPageType);
                    needsUpdate = true;
                }
                Map<String, Object> schema = buildPageSchema(pageNode);
                if (!schema.isEmpty()) {
                    // Config is always overwritten from JSON — it is read-only template data,
                    // not user data, so we always want the latest JSON to win.
                    Map<String, Object> existingConfig = existing.getConfig() != null
                            ? new HashMap<>(existing.getConfig())
                            : new HashMap<>();
                    for (Map.Entry<String, Object> entry : schema.entrySet()) {
                        if (!entry.getValue().equals(existingConfig.get(entry.getKey()))) {
                            existingConfig.put(entry.getKey(), entry.getValue());
                            needsUpdate = true;
                        }
                    }
                    if (needsUpdate) {
                        existing.setConfig(existingConfig);
                    }
                }
                if (needsUpdate) {
                    noteBookPageDAO.update(existing);
                    updated++;
                } else {
                    skipped++;
                }
                continue;
            }

            NoteBookPage page = new NoteBookPage();
            page.setNotebook(template);
            page.setOrder(order);
            page.setTitle(textOrNull(pageNode, "title"));
            page.setPageType(textOrNull(pageNode, "pageType"));
            page.setInstructions(textOrNull(pageNode, "instructions"));
            page.setContent(textOrNull(pageNode, "content"));
            page.setCompleted(false);
            Map<String, Object> schema = buildPageSchema(pageNode);
            if (!schema.isEmpty()) {
                page.setConfig(schema);
            }
            noteBookPageDAO.insert(page);
            created++;
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration", "Template '" + title + "' from "
                + fileName + ": pages created=" + created + ", updated=" + updated + ", skipped=" + skipped);
    }

    private NoteBook findTemplate(String title) {
        List<NoteBook> matches = noteBookDAO.getAllMatching("title", title);
        for (NoteBook nb : matches) {
            if (Boolean.TRUE.equals(nb.getIsTemplate())) {
                return nb;
            }
        }
        return null;
    }

    private NoteBook createTemplate(JsonNode root, String title, String fileName) {
        NoteBook template = new NoteBook();
        template.setTitle(title);
        template.setWorkflowType(textOrNull(root, "workflowType"));
        template.setObjective(textOrNull(root, "objective"));
        template.setProtocol(textOrNull(root, "protocol"));
        template.setContent(textOrNull(root, "content"));
        template.setIsTemplate(true);

        String status = textOrNull(root, "status");
        if (status != null) {
            try {
                template.setStatus(NoteBook.NoteBookStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                template.setStatus(NoteBook.NoteBookStatus.ACTIVE);
            }
        } else {
            template.setStatus(NoteBook.NoteBookStatus.ACTIVE);
        }

        noteBookDAO.insert(template);
        LogEvent.logInfo(this.getClass().getSimpleName(), "createTemplate",
                "Created notebook template '" + title + "' from " + fileName);
        return template;
    }

    /**
     * Builds the page schema data map from a JSON page node.
     * <p>
     * Reads top-level {@code manifestColumns} / {@code columns} arrays, then reads
     * all fields from the optional {@code data} object. Both sets of values are
     * stored in the page's JSONB {@code data} field so the frontend can drive
     * generic QC, storage, and manifest behaviour from config without any new Java
     * or React code per lab.
     */
    private Map<String, Object> buildPageSchema(JsonNode pageNode) {
        Map<String, Object> schema = new HashMap<>();

        // Top-level arrays (legacy locations kept for backwards-compat)
        JsonNode manifestColumnsNode = pageNode.get("manifestColumns");
        if (manifestColumnsNode != null && manifestColumnsNode.isArray()) {
            schema.put("manifestColumns",
                    objectMapper.convertValue(manifestColumnsNode, new TypeReference<List<Object>>() {
                    }));
        }

        JsonNode columnsNode = pageNode.get("columns");
        if (columnsNode != null && columnsNode.isArray()) {
            schema.put("columns", objectMapper.convertValue(columnsNode, new TypeReference<List<Object>>() {
            }));
        }

        // Structured config block — qcSections, storageConditions, additionalFields,
        // etc.
        JsonNode dataNode = pageNode.get("data");
        if (dataNode != null && dataNode.isObject()) {
            dataNode.fields().forEachRemaining(entry -> {
                Object value = objectMapper.convertValue(entry.getValue(), Object.class);
                schema.put(entry.getKey(), value);
            });
        }

        return schema;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        String value = child.asText("").trim();
        return value.isEmpty() ? null : value;
    }

}
