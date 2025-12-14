import React, { useState } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Form,
  Heading,
  Toggle,
  InlineNotification,
  Loading,
  TextInput,
  Select,
  SelectItem,
} from "@carbon/react";
import { Save, Reset } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServer } from "../../utils/Utils";
import ClinicalSpecimenForm from "../components/ClinicalSpecimenForm";
import ResearchSpecimenForm from "../components/ResearchSpecimenForm";

/**
 * SampleReceptionPage - Page for receiving and registering pathology samples.
 * Supports both clinical diagnostic and research specimens.
 *
 * Features:
 * - Category toggle (Clinical vs Research)
 * - Dynamic form fields based on category
 * - Auto-generated accession numbers
 * - Sample source tracking
 * - Receiving staff and date/time recording
 */
function SampleReceptionPage() {
  const intl = useIntl();

  // Form state
  const [category, setCategory] = useState("CLINICAL");
  const [formData, setFormData] = useState({
    sampleItemId: "",
    sampleSource: "Alert Hospital",
    receivingStaffId: "",
    // Clinical fields
    patientId: "",
    requestingClinician: "",
    specimenSite: "",
    clinicalDetails: "",
    // Research fields
    studyId: "",
    piName: "",
    participantId: "",
    ethicalApprovalRef: "",
    // Common fields
    collectionDate: "",
    collectionTime: "",
  });

  // UI state
  const [loading, setLoading] = useState(false);
  const [notification, setNotification] = useState(null);
  const [errors, setErrors] = useState({});
  const [accessionNumber, setAccessionNumber] = useState(null);

  const handleCategoryChange = (checked) => {
    setCategory(checked ? "RESEARCH" : "CLINICAL");
    setErrors({});
    setNotification(null);
  };

  const handleFormChange = (updatedData) => {
    setFormData(updatedData);
    setErrors({});
  };

  const handleInputChange = (field) => (event) => {
    setFormData({
      ...formData,
      [field]: event.target.value,
    });
    setErrors({});
  };

  const validateForm = () => {
    const newErrors = {};

    // Validate sample item ID
    if (!formData.sampleItemId) {
      newErrors.sampleItemId = intl.formatMessage({
        id: "pathology.reception.error.sampleItemId.required",
      });
    }

    // Validate receiving staff
    if (!formData.receivingStaffId) {
      newErrors.receivingStaffId = intl.formatMessage({
        id: "pathology.reception.error.receivingStaffId.required",
      });
    }

    // Category-specific validation
    if (category === "CLINICAL") {
      if (!formData.patientId && !formData.requestingClinician) {
        newErrors.patientId = intl.formatMessage({
          id: "pathology.reception.error.clinical.required",
        });
      }
    } else if (category === "RESEARCH") {
      if (!formData.studyId) {
        newErrors.studyId = intl.formatMessage({
          id: "pathology.reception.error.research.studyId.required",
        });
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!validateForm()) {
      setNotification({
        kind: "error",
        title: intl.formatMessage({ id: "common.error.validation" }),
        subtitle: intl.formatMessage({
          id: "pathology.reception.error.validation.message",
        }),
      });
      return;
    }

    setLoading(true);
    setNotification(null);

    // Prepare form data for submission
    const submissionData = {
      sampleItemId: formData.sampleItemId,
      category: category,
      sampleSource: formData.sampleSource,
      receivingStaffId: formData.receivingStaffId,
      // Category-specific fields
      ...(category === "CLINICAL" && {
        patientId: formData.patientId,
        requestingClinician: formData.requestingClinician,
        specimenSite: formData.specimenSite,
        clinicalDetails: formData.clinicalDetails,
      }),
      ...(category === "RESEARCH" && {
        studyId: formData.studyId,
        piName: formData.piName,
        participantId: formData.participantId,
        ethicalApprovalRef: formData.ethicalApprovalRef,
      }),
      // Collection date/time
      receivingDate:
        formData.collectionDate && formData.collectionTime
          ? `${formData.collectionDate}T${formData.collectionTime}`
          : null,
    };

    try {
      const response = await postToOpenElisServer(
        "/rest/pathology/samples",
        JSON.stringify(submissionData),
      );

      if (response.success) {
        setNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "pathology.reception.success.title",
          }),
          subtitle: intl.formatMessage(
            { id: "pathology.reception.success.message" },
            { accessionNumber: response.fhirUuid },
          ),
        });
        setAccessionNumber(response.fhirUuid);
        handleReset();
      } else {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "common.error.save" }),
          subtitle:
            response.message ||
            intl.formatMessage({
              id: "pathology.reception.error.save.message",
            }),
        });
      }
    } catch (error) {
      setNotification({
        kind: "error",
        title: intl.formatMessage({ id: "common.error.server" }),
        subtitle:
          error.message ||
          intl.formatMessage({
            id: "pathology.reception.error.server.message",
          }),
      });
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setFormData({
      sampleItemId: "",
      sampleSource: "Alert Hospital",
      receivingStaffId: "",
      patientId: "",
      requestingClinician: "",
      specimenSite: "",
      clinicalDetails: "",
      studyId: "",
      piName: "",
      participantId: "",
      ethicalApprovalRef: "",
      collectionDate: "",
      collectionTime: "",
    });
    setErrors({});
    setAccessionNumber(null);
  };

  return (
    <Grid fullWidth className="sample-reception-page">
      <Column lg={16} md={8} sm={4}>
        <Heading>
          <FormattedMessage id="pathology.reception.title" />
        </Heading>
        <p className="page-description">
          <FormattedMessage id="pathology.reception.description" />
        </p>
      </Column>

      {notification && (
        <Column lg={16} md={8} sm={4}>
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onCloseButtonClick={() => setNotification(null)}
          />
        </Column>
      )}

      <Column lg={16} md={8} sm={4}>
        <Tile>
          <Form onSubmit={handleSubmit}>
            <Grid narrow>
              {/* Category Toggle */}
              <Column lg={16} md={8} sm={4}>
                <Toggle
                  id="category-toggle"
                  labelText={intl.formatMessage({
                    id: "pathology.reception.category.label",
                  })}
                  labelA={intl.formatMessage({
                    id: "pathology.category.clinical",
                  })}
                  labelB={intl.formatMessage({
                    id: "pathology.category.research",
                  })}
                  toggled={category === "RESEARCH"}
                  onToggle={handleCategoryChange}
                />
              </Column>

              {/* Common Fields */}
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="sampleItemId"
                  labelText={intl.formatMessage({
                    id: "pathology.reception.sampleItemId",
                  })}
                  placeholder={intl.formatMessage({
                    id: "pathology.reception.sampleItemId.placeholder",
                  })}
                  value={formData.sampleItemId}
                  onChange={handleInputChange("sampleItemId")}
                  invalid={errors?.sampleItemId}
                  invalidText={errors?.sampleItemId}
                  required
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="sampleSource"
                  labelText={intl.formatMessage({
                    id: "pathology.reception.sampleSource",
                  })}
                  value={formData.sampleSource}
                  onChange={handleInputChange("sampleSource")}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="receivingStaffId"
                  labelText={intl.formatMessage({
                    id: "pathology.reception.receivingStaffId",
                  })}
                  placeholder={intl.formatMessage({
                    id: "pathology.reception.receivingStaffId.placeholder",
                  })}
                  value={formData.receivingStaffId}
                  onChange={handleInputChange("receivingStaffId")}
                  invalid={errors?.receivingStaffId}
                  invalidText={errors?.receivingStaffId}
                  required
                />
              </Column>

              {/* Category-Specific Form */}
              <Column lg={16} md={8} sm={4}>
                {category === "CLINICAL" ? (
                  <ClinicalSpecimenForm
                    formData={formData}
                    onChange={handleFormChange}
                    errors={errors}
                  />
                ) : (
                  <ResearchSpecimenForm
                    formData={formData}
                    onChange={handleFormChange}
                    errors={errors}
                  />
                )}
              </Column>

              {/* Action Buttons */}
              <Column lg={16} md={8} sm={4}>
                <div className="button-group">
                  <Button
                    kind="primary"
                    type="submit"
                    renderIcon={Save}
                    disabled={loading}
                  >
                    {loading ? (
                      <Loading small withOverlay={false} />
                    ) : (
                      <FormattedMessage id="common.button.save" />
                    )}
                  </Button>
                  <Button
                    kind="secondary"
                    renderIcon={Reset}
                    onClick={handleReset}
                    disabled={loading}
                  >
                    <FormattedMessage id="common.button.reset" />
                  </Button>
                </div>
              </Column>

              {/* Accession Number Display */}
              {accessionNumber && (
                <Column lg={16} md={8} sm={4}>
                  <InlineNotification
                    kind="info"
                    title={intl.formatMessage({
                      id: "pathology.reception.accessionNumber.title",
                    })}
                    subtitle={accessionNumber}
                    hideCloseButton
                  />
                </Column>
              )}
            </Grid>
          </Form>
        </Tile>
      </Column>
    </Grid>
  );
}

export default SampleReceptionPage;
