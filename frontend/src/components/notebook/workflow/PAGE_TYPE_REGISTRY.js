/**
 * PAGE_TYPE_REGISTRY - Maps pageType strings (stored in DB) to React page components.
 *
 * To add a new lab:
 *   1. Add a JSON file in volume/configuration/backend/notebook-templates/<lab>.json
 *      with pages declaring pageType, manifestColumns, and columns.
 *   2. For steps that are identical across labs (reception, QC, storage, reporting,
 *      disposal, environmental monitoring), use a generic_* pageType — no new React
 *      component is needed.
 *   3. For truly lab-specific steps (e.g. TB culture reading, Pathology cassettes),
 *      write a component and register it below.
 *
 * Generic pageTypes (config-driven, no per-lab code):
 *   generic_sample_reception      — manifest import + sample grid (columns from JSON)
 *   generic_quality_check         — QC accept/reject workflow
 *   generic_sample_processing     — processing step log
 *   generic_storage               — storage location assignment
 *   generic_disposal              — disposal / archiving
 *   generic_reporting             — metrics and date-range reporting
 *   generic_environmental_monitor — temperature and environment logging
 *
 * FHIR pageTypes (M2 — form engine):
 *   fhir_questionnaire_form       — fully dynamic FHIR Questionnaire renderer
 *
 * No WorkflowTab file is needed per lab. GenericWorkflowTab handles all labs.
 */
import React from "react";
import NotebookFormEngine from "../fhir/NotebookFormEngine";

/**
 * Registry mapping pageType string → React component.
 * Keys must match the pageType values stored in the notebook_page table.
 *
 * NOTE: This file is the minimal M2 addition to PAGE_TYPE_REGISTRY.
 * The full registry (with all lab-specific entries) lives on the
 * generic-notebook / 001-fhir-notebook-engine branches and will be
 * merged in during M3 integration.
 */
const PAGE_TYPE_REGISTRY = {
  // ── FHIR form engine — M2 addition ───────────────────────────────────────
  fhir_questionnaire_form: NotebookFormEngine,
};

export default PAGE_TYPE_REGISTRY;
