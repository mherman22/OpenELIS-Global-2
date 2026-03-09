import FHIR_COMPONENT_MAP from "../FHIR_COMPONENT_MAP";
import { TextInput, TextArea, Checkbox, DatePicker, Select } from "@carbon/react";

describe("FHIR_COMPONENT_MAP", () => {
  test("maps string to TextInput", () =>
    expect(FHIR_COMPONENT_MAP.string).toBe(TextInput));
  test("maps text to TextArea", () =>
    expect(FHIR_COMPONENT_MAP.text).toBe(TextArea));
  test("maps boolean to Checkbox", () =>
    expect(FHIR_COMPONENT_MAP.boolean).toBe(Checkbox));
  test("maps date to DatePicker", () =>
    expect(FHIR_COMPONENT_MAP.date).toBe(DatePicker));
  test("maps choice to Select", () =>
    expect(FHIR_COMPONENT_MAP.choice).toBe(Select));
  test("maps display to null", () =>
    expect(FHIR_COMPONENT_MAP.display).toBeNull());
  test("maps specialized_test_assignment to DynamicFhirTable string sentinel", () =>
    expect(FHIR_COMPONENT_MAP.specialized_test_assignment).toBe("DynamicFhirTable"));
});
