import React from "react";
import {
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
  TimePicker,
  TimePickerSelect,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";

/**
 * Clinical Specimen Form - Form fields for clinical diagnostic specimens
 *
 * Fields: Patient ID, Requesting Clinician, Specimen Site,
 * Collection Date/Time, Clinical Details
 */
function ClinicalSpecimenForm({ formData, onChange, errors }) {
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
    <div className="clinical-specimen-form">
      <TextInput
        id="patientId"
        labelText={intl.formatMessage({
          id: "pathology.reception.clinical.patientId",
        })}
        placeholder={intl.formatMessage({
          id: "pathology.reception.clinical.patientId.placeholder",
        })}
        value={formData.patientId || ""}
        onChange={handleInputChange("patientId")}
        invalid={errors?.patientId}
        invalidText={errors?.patientId}
      />

      <TextInput
        id="requestingClinician"
        labelText={intl.formatMessage({
          id: "pathology.reception.clinical.requestingClinician",
        })}
        placeholder={intl.formatMessage({
          id: "pathology.reception.clinical.requestingClinician.placeholder",
        })}
        value={formData.requestingClinician || ""}
        onChange={handleInputChange("requestingClinician")}
        invalid={errors?.requestingClinician}
        invalidText={errors?.requestingClinician}
      />

      <TextInput
        id="specimenSite"
        labelText={intl.formatMessage({
          id: "pathology.reception.clinical.specimenSite",
        })}
        placeholder={intl.formatMessage({
          id: "pathology.reception.clinical.specimenSite.placeholder",
        })}
        value={formData.specimenSite || ""}
        onChange={handleInputChange("specimenSite")}
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
            id: "pathology.reception.clinical.collectionDate",
          })}
          invalid={errors?.collectionDate}
          invalidText={errors?.collectionDate}
        />
      </DatePicker>

      <TimePicker
        id="collectionTime"
        labelText={intl.formatMessage({
          id: "pathology.reception.clinical.collectionTime",
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

      <TextArea
        id="clinicalDetails"
        labelText={intl.formatMessage({
          id: "pathology.reception.clinical.clinicalDetails",
        })}
        placeholder={intl.formatMessage({
          id: "pathology.reception.clinical.clinicalDetails.placeholder",
        })}
        value={formData.clinicalDetails || ""}
        onChange={handleInputChange("clinicalDetails")}
        rows={4}
      />
    </div>
  );
}

ClinicalSpecimenForm.propTypes = {
  formData: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  errors: PropTypes.object,
};

export default ClinicalSpecimenForm;
