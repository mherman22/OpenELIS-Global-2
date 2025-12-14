import React from "react";
import {
  TextInput,
  DatePicker,
  DatePickerInput,
  TimePicker,
  TimePickerSelect,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";

/**
 * Research Specimen Form - Form fields for research specimens
 *
 * Fields: Study ID, PI Name, Participant ID, Ethical Approval Ref,
 * Collection Date/Time
 */
function ResearchSpecimenForm({ formData, onChange, errors }) {
  const intl = useIntl();

  const handleInputChange = (field) => (event) => {
    onChange({
      ...formData,
      [field]: event.target.value,
    });
  };

  const handleDateChange = (dates) => {
    if (dates && dates.length > 0) {
      onChange({
        ...formData,
        collectionDate: dates[0],
      });
    }
  };

  return (
    <div className="research-specimen-form">
      <TextInput
        id="studyId"
        labelText={intl.formatMessage({
          id: "pathology.reception.research.studyId",
        })}
        placeholder={intl.formatMessage({
          id: "pathology.reception.research.studyId.placeholder",
        })}
        value={formData.studyId || ""}
        onChange={handleInputChange("studyId")}
        invalid={errors?.studyId}
        invalidText={errors?.studyId}
        required
      />

      <TextInput
        id="piName"
        labelText={intl.formatMessage({
          id: "pathology.reception.research.piName",
        })}
        placeholder={intl.formatMessage({
          id: "pathology.reception.research.piName.placeholder",
        })}
        value={formData.piName || ""}
        onChange={handleInputChange("piName")}
      />

      <TextInput
        id="participantId"
        labelText={intl.formatMessage({
          id: "pathology.reception.research.participantId",
        })}
        placeholder={intl.formatMessage({
          id: "pathology.reception.research.participantId.placeholder",
        })}
        value={formData.participantId || ""}
        onChange={handleInputChange("participantId")}
      />

      <TextInput
        id="ethicalApprovalRef"
        labelText={intl.formatMessage({
          id: "pathology.reception.research.ethicalApprovalRef",
        })}
        placeholder={intl.formatMessage({
          id: "pathology.reception.research.ethicalApprovalRef.placeholder",
        })}
        value={formData.ethicalApprovalRef || ""}
        onChange={handleInputChange("ethicalApprovalRef")}
      />

      <DatePicker
        datePickerType="single"
        onChange={handleDateChange}
        value={formData.collectionDate}
      >
        <DatePickerInput
          id="collectionDate"
          placeholder="mm/dd/yyyy"
          labelText={intl.formatMessage({
            id: "pathology.reception.research.collectionDate",
          })}
          invalid={errors?.collectionDate}
          invalidText={errors?.collectionDate}
        />
      </DatePicker>

      <TimePicker
        id="collectionTime"
        labelText={intl.formatMessage({
          id: "pathology.reception.research.collectionTime",
        })}
        value={formData.collectionTime || ""}
        onChange={handleInputChange("collectionTime")}
      >
        <TimePickerSelect
          id="collectionTime-hours"
          labelText={intl.formatMessage({ id: "common.time.hours" })}
        >
          {Array.from({ length: 24 }, (_, i) => (
            <SelectItem
              key={i}
              value={String(i).padStart(2, "0")}
              text={String(i).padStart(2, "0")}
            />
          ))}
        </TimePickerSelect>
        <TimePickerSelect
          id="collectionTime-minutes"
          labelText={intl.formatMessage({ id: "common.time.minutes" })}
        >
          {Array.from({ length: 60 }, (_, i) => (
            <SelectItem
              key={i}
              value={String(i).padStart(2, "0")}
              text={String(i).padStart(2, "0")}
            />
          ))}
        </TimePickerSelect>
      </TimePicker>
    </div>
  );
}

ResearchSpecimenForm.propTypes = {
  formData: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  errors: PropTypes.object,
};

export default ResearchSpecimenForm;
