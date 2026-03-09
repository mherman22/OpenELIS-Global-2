import {
  TextInput,
  TextArea,
  Checkbox,
  DatePicker,
  Select,
} from "@carbon/react";

// Lazy import specialized components to avoid circular deps
const getDynamicFhirTable = () => require("./DynamicFhirTable").default;

/**
 * FHIR_COMPONENT_MAP — maps FHIR item.type strings and pageType extension
 * values to Carbon Design System React components.
 *
 * Standard FHIR R4 item types map directly to Carbon form controls.
 * pageType extension values map to specialized notebook components.
 */
const FHIR_COMPONENT_MAP = {
  // Standard FHIR item types
  string: TextInput,
  text: TextArea,
  boolean: Checkbox,
  date: DatePicker,
  choice: Select,
  display: null, // rendered as <p> by form engine

  // pageType extension values → specialized components
  specialized_test_assignment: "DynamicFhirTable", // resolved at render time
  bioanalytical_storage: null, // future use
};

export { getDynamicFhirTable };
export default FHIR_COMPONENT_MAP;
