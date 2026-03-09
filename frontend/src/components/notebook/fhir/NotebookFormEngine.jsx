import React from "react";
import {
  TextInput,
  TextArea,
  Checkbox,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Stack,
  Form,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { shouldRender, getExtension } from "./fhirFormUtils";
import DynamicFhirTable from "./DynamicFhirTable";
import FhirInstructionsList from "./FhirInstructionsList";

const TYPE_COMPONENT_MAP = {
  string: TextInput,
  text: TextArea,
  boolean: Checkbox,
  date: DatePicker,
  choice: Select,
};

/**
 * NotebookFormEngine — fully controlled FHIR Questionnaire form renderer.
 *
 * Iterates `questionnaire.item[]` recursively. Each item is rendered based on:
 *  1. FHIR `page-type` extension → specialized component (e.g. DynamicFhirTable)
 *  2. `item.type === "display"` → FhirInstructionsList
 *  3. `item.type === "group"` → recursive group rendering
 *  4. `item.type === "choice"` → Carbon Select with answerOption children
 *  5. `item.type === "boolean"` → Carbon Checkbox
 *  6. All other types → mapped via TYPE_COMPONENT_MAP (default: TextInput)
 *
 * @param {Object}   props.questionnaire  - FHIR R4 Questionnaire resource
 * @param {Object}   props.formData       - current form state keyed by linkId
 * @param {function} props.onFieldChange  - callback(linkId, value)
 * @param {boolean}  props.readOnly       - disables all controls when true
 */
export default function NotebookFormEngine({
  questionnaire,
  formData = {},
  onFieldChange,
  readOnly = false,
}) {
  const intl = useIntl();

  const requiredText = intl.formatMessage({
    id: "notebook.fhir.validation.required",
    defaultMessage: "This field is required.",
  });

  const renderItem = (item) => {
    if (!shouldRender(item, formData)) return null;

    // Check for specialized page-type extension
    const pageType = getExtension(item, "page-type");
    if (pageType === "specialized_test_assignment") {
      return (
        <DynamicFhirTable
          key={item.linkId}
          item={item}
          selectedId={formData[item.linkId] ?? null}
          onSelect={(id) => onFieldChange?.(item.linkId, id)}
        />
      );
    }

    // Display item → instructions list
    if (item.type === "display") {
      return <FhirInstructionsList key={item.linkId} item={item} />;
    }

    // Group → render children recursively
    if (item.type === "group") {
      return (
        <div key={item.linkId} className="fhir-group">
          {item.text && <h4>{item.text}</h4>}
          {item.item?.map(renderItem)}
        </div>
      );
    }

    const isInvalid = item.required && !formData[item.linkId];

    const commonProps = {
      id: item.linkId,
      labelText: item.text ?? item.linkId,
      placeholder: item.definition ?? "",
      value: formData[item.linkId] ?? "",
      disabled: readOnly,
      invalid: isInvalid,
      invalidText: requiredText,
      onChange: (e) => onFieldChange?.(item.linkId, e.target?.value ?? e),
    };

    // Choice type → Select with answerOption children
    if (item.type === "choice" && item.answerOption?.length > 0) {
      return (
        <Select key={item.linkId} {...commonProps}>
          <SelectItem value="" text="" />
          {item.answerOption.map((opt) => (
            <SelectItem
              key={opt.valueCoding?.code}
              value={opt.valueCoding?.code}
              text={opt.valueCoding?.display ?? opt.valueCoding?.code}
            />
          ))}
        </Select>
      );
    }

    // Boolean type → Checkbox
    if (item.type === "boolean") {
      return (
        <Checkbox
          key={item.linkId}
          id={item.linkId}
          labelText={item.text ?? item.linkId}
          checked={!!formData[item.linkId]}
          disabled={readOnly}
          onChange={(_, { checked }) => onFieldChange?.(item.linkId, checked)}
        />
      );
    }

    // Date type → DatePicker with DatePickerInput
    if (item.type === "date") {
      return (
        <DatePicker
          key={item.linkId}
          datePickerType="single"
          value={formData[item.linkId] ?? ""}
          onChange={(dates) =>
            onFieldChange?.(
              item.linkId,
              dates[0] ? dates[0].toISOString().slice(0, 10) : "",
            )
          }
        >
          <DatePickerInput
            id={item.linkId}
            labelText={item.text ?? item.linkId}
            placeholder="yyyy-mm-dd"
            disabled={readOnly}
            invalid={isInvalid}
            invalidText={requiredText}
          />
        </DatePicker>
      );
    }

    // All other types — look up in TYPE_COMPONENT_MAP, default to TextInput
    const Component = TYPE_COMPONENT_MAP[item.type] ?? TextInput;
    return <Component key={item.linkId} {...commonProps} />;
  };

  return (
    <Form>
      <Stack gap={6}>{questionnaire?.item?.map(renderItem)}</Stack>
    </Form>
  );
}
