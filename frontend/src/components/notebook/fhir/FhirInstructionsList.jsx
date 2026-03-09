import React from "react";
import { FormattedMessage } from "react-intl";
import { getExtension } from "./fhirFormUtils";

/**
 * FhirInstructionsList — renders an ordered list of preparation steps
 * sourced from the `http://openelis-global.org/instructions` FHIR extension.
 *
 * The extension valueString must be a JSON-serialised string array.
 * Renders nothing when the array is absent or empty.
 *
 * @param {Object} props.item - FHIR Questionnaire item of type "display"
 */
export default function FhirInstructionsList({ item }) {
  const raw = getExtension(item, "instructions");
  let steps = [];
  try {
    steps = raw ? JSON.parse(raw) : [];
  } catch {
    steps = [];
  }

  if (steps.length === 0) return null;

  return (
    <div className="fhir-instructions">
      <h5>
        <FormattedMessage
          id="notebook.fhir.instructions.heading"
          defaultMessage="Preparation Steps"
        />
      </h5>
      <ol>
        {steps.map((step, i) => (
          <li key={i}>{step}</li>
        ))}
      </ol>
    </div>
  );
}
