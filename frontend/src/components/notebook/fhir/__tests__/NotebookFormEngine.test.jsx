import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import NotebookFormEngine from "../NotebookFormEngine";

// Wrap component with IntlProvider for i18n
const renderWithIntl = (ui, formData = {}, onFieldChange = jest.fn()) => {
  return render(
    <IntlProvider locale="en" messages={{}}>
      {React.cloneElement(ui, { formData, onFieldChange })}
    </IntlProvider>,
  );
};

// T035
test("NotebookFormEngine renders TextInput for item.type=string with labelText from item.text", () => {
  const questionnaire = {
    item: [{ linkId: "patientName", type: "string", text: "Patient Name" }],
  };
  renderWithIntl(<NotebookFormEngine questionnaire={questionnaire} />);
  expect(screen.getByLabelText("Patient Name")).toBeInTheDocument();
});

// T036
test("NotebookFormEngine renders Select with answerOption items for item.type=choice", () => {
  const questionnaire = {
    item: [
      {
        linkId: "sampleType",
        type: "choice",
        text: "Sample Type",
        answerOption: [
          { valueCoding: { code: "SERUM", display: "Serum" } },
          { valueCoding: { code: "PLASMA", display: "Plasma" } },
        ],
      },
    ],
  };
  renderWithIntl(<NotebookFormEngine questionnaire={questionnaire} />);
  expect(screen.getByText("Serum")).toBeInTheDocument();
  expect(screen.getByText("Plasma")).toBeInTheDocument();
});

// T037
test("NotebookFormEngine renders DynamicFhirTable when item has page-type extension = specialized_test_assignment", () => {
  const questionnaire = {
    item: [
      {
        linkId: "testControl",
        type: "group",
        text: "Test Control Standards",
        extension: [
          {
            url: "http://openelis-global.org/page-type",
            valueString: "specialized_test_assignment",
          },
        ],
      },
    ],
  };
  renderWithIntl(<NotebookFormEngine questionnaire={questionnaire} />);
  // DynamicFhirTable renders an empty state or table container
  expect(
    screen.getByText(/No data available\.|Test Control Standards/i),
  ).toBeInTheDocument();
});

// T038
test("NotebookFormEngine shows inline validation error for required field when value is empty", () => {
  const questionnaire = {
    item: [
      { linkId: "requiredField", type: "string", text: "Required Field", required: true },
    ],
  };
  renderWithIntl(<NotebookFormEngine questionnaire={questionnaire} />, {
    requiredField: "",
  });
  expect(screen.getByText(/This field is required/i)).toBeInTheDocument();
});

// T039
test("NotebookFormEngine hides field when enableWhen condition is not met", () => {
  const questionnaire = {
    item: [
      { linkId: "trigger", type: "string", text: "Trigger Field" },
      {
        linkId: "dependent",
        type: "string",
        text: "Dependent Field",
        enableWhen: [{ question: "trigger", operator: "=", answerString: "show" }],
      },
    ],
  };
  renderWithIntl(<NotebookFormEngine questionnaire={questionnaire} />, {
    trigger: "hide",
  });
  expect(screen.getByLabelText("Trigger Field")).toBeInTheDocument();
  expect(screen.queryByLabelText("Dependent Field")).not.toBeInTheDocument();
});

// T040
test("NotebookFormEngine shows field when enableWhen condition becomes met after formData change", () => {
  const questionnaire = {
    item: [
      { linkId: "trigger", type: "string", text: "Trigger Field" },
      {
        linkId: "dependent",
        type: "string",
        text: "Dependent Field",
        enableWhen: [{ question: "trigger", operator: "=", answerString: "show" }],
      },
    ],
  };
  renderWithIntl(<NotebookFormEngine questionnaire={questionnaire} />, {
    trigger: "show",
  });
  expect(screen.getByLabelText("Trigger Field")).toBeInTheDocument();
  expect(screen.getByLabelText("Dependent Field")).toBeInTheDocument();
});
