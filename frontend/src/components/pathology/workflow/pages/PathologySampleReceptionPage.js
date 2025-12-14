import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Toggle,
  TextInput,
  Select,
  SelectItem,
  TextArea,
  ProgressBar,
} from "@carbon/react";
import { Save, Reset } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../../notebook/workflow/SampleGrid";
import "../PathologyWorkflow.css";

/**
 * PathologySampleReceptionPage - Page 1 of pathology workflow.
 * Handles pathology sample registration with clinical/research metadata.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress stats
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PathologySampleReceptionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [registering, setRegistering] = useState(false);
  const [notification, setNotification] = useState(null);

  // Registration form state
  const [category, setCategory] = useState("CLINICAL");
  const [formData, setFormData] = useState({
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
  });

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("pathology-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              accessionNumber: sample.accessionNumber,
              category: sample.category,
              sampleSource: sample.sampleSource,
              receivingDate: sample.receivingDate,
              status: sample.pageStatus || "PENDING",
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const handleCategoryChange = (checked) => {
    setCategory(checked ? "RESEARCH" : "CLINICAL");
    setNotification(null);
  };

  const handleInputChange = (field) => (event) => {
    setFormData({
      ...formData,
      [field]: event.target.value,
    });
  };

  const handleRegister = async () => {
    setRegistering(true);
    setNotification(null);

    const submissionData = {
      sampleItemId: formData.sampleItemId,
      category: category,
      sampleSource: formData.sampleSource,
      receivingStaffId: formData.receivingStaffId,
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
      receivingDate: new Date().toISOString(),
    };

    try {
      await postToOpenElisServer(
        "/rest/pathology/samples",
        JSON.stringify(submissionData),
      );

      setNotification({
        kind: "success",
        title: intl.formatMessage({ id: "pathology.reception.success.title" }),
        subtitle: intl.formatMessage({
          id: "pathology.reception.success.message",
        }),
      });

      // Reset form
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
      });

      // Reload samples and update progress
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    } catch (error) {
      setNotification({
        kind: "error",
        title: intl.formatMessage({ id: "common.error.save" }),
        subtitle:
          error.message ||
          intl.formatMessage({ id: "pathology.reception.error.save.message" }),
      });
    } finally {
      setRegistering(false);
    }
  };

  if (loading) {
    return <Loading description="Loading samples..." withOverlay={false} />;
  }

  return (
    <div className="pathology-sample-reception">
      <Grid fullWidth>
        <Column lg={16}>
          <h3>
            <FormattedMessage id="pathology.reception.title" />
          </h3>
          <p>
            <FormattedMessage id="pathology.reception.description" />
          </p>

          <ProgressBar
            label={`${progress.completed} / ${progress.total} samples registered`}
            value={progress.percentage}
            max={100}
            className="pathology-progress-indicator"
          />
        </Column>

        {notification && (
          <Column lg={16}>
            <InlineNotification
              kind={notification.kind}
              title={notification.title}
              subtitle={notification.subtitle}
              onCloseButtonClick={() => setNotification(null)}
            />
          </Column>
        )}

        <Column lg={16}>
          <Tile>
            <Grid narrow>
              <Column lg={16} className="pathology-category-toggle">
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

              <Column lg={8}>
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
                  required
                />
              </Column>

              <Column lg={8}>
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
                  required
                />
              </Column>

              {category === "CLINICAL" ? (
                <>
                  <Column lg={8}>
                    <TextInput
                      id="patientId"
                      labelText={intl.formatMessage({
                        id: "pathology.reception.clinical.patientId",
                      })}
                      placeholder={intl.formatMessage({
                        id: "pathology.reception.clinical.patientId.placeholder",
                      })}
                      value={formData.patientId}
                      onChange={handleInputChange("patientId")}
                    />
                  </Column>

                  <Column lg={8}>
                    <TextInput
                      id="requestingClinician"
                      labelText={intl.formatMessage({
                        id: "pathology.reception.clinical.requestingClinician",
                      })}
                      placeholder={intl.formatMessage({
                        id: "pathology.reception.clinical.requestingClinician.placeholder",
                      })}
                      value={formData.requestingClinician}
                      onChange={handleInputChange("requestingClinician")}
                    />
                  </Column>

                  <Column lg={16}>
                    <TextArea
                      id="clinicalDetails"
                      labelText={intl.formatMessage({
                        id: "pathology.reception.clinical.clinicalDetails",
                      })}
                      placeholder={intl.formatMessage({
                        id: "pathology.reception.clinical.clinicalDetails.placeholder",
                      })}
                      value={formData.clinicalDetails}
                      onChange={handleInputChange("clinicalDetails")}
                      rows={3}
                    />
                  </Column>
                </>
              ) : (
                <>
                  <Column lg={8}>
                    <TextInput
                      id="studyId"
                      labelText={intl.formatMessage({
                        id: "pathology.reception.research.studyId",
                      })}
                      placeholder={intl.formatMessage({
                        id: "pathology.reception.research.studyId.placeholder",
                      })}
                      value={formData.studyId}
                      onChange={handleInputChange("studyId")}
                      required
                    />
                  </Column>

                  <Column lg={8}>
                    <TextInput
                      id="piName"
                      labelText={intl.formatMessage({
                        id: "pathology.reception.research.piName",
                      })}
                      placeholder={intl.formatMessage({
                        id: "pathology.reception.research.piName.placeholder",
                      })}
                      value={formData.piName}
                      onChange={handleInputChange("piName")}
                    />
                  </Column>
                </>
              )}

              <Column lg={16}>
                <div className="pathology-action-buttons">
                  <Button
                    kind="primary"
                    renderIcon={Save}
                    onClick={handleRegister}
                    disabled={registering}
                  >
                    {registering ? (
                      <Loading small withOverlay={false} />
                    ) : (
                      <FormattedMessage
                        id="common.button.register"
                        defaultMessage="Register"
                      />
                    )}
                  </Button>
                  <Button
                    kind="secondary"
                    renderIcon={Reset}
                    onClick={() =>
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
                      })
                    }
                  >
                    <FormattedMessage id="common.button.reset" />
                  </Button>
                </div>
              </Column>
            </Grid>
          </Tile>
        </Column>

        <Column lg={16}>
          <h4>
            <FormattedMessage
              id="pathology.reception.registered.samples"
              defaultMessage="Registered Samples"
            />
          </h4>
          <SampleGrid
            samples={samples}
            onSampleSelect={(ids) => console.log("Selected:", ids)}
            enableSelection={false}
          />
        </Column>
      </Grid>
    </div>
  );
}

export default PathologySampleReceptionPage;
