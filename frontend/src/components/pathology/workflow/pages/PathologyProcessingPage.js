import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Tile,
  Button,
  TextInput,
  TextArea,
  InlineNotification,
  Loading,
  ProgressBar,
} from "@carbon/react";
import { Save } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../../notebook/workflow/SampleGrid";

/**
 * PathologyProcessingPage - Generic processing page for workflow steps.
 * Used for Grossing, Microtomy, Staining, Pathologist Review, and Results Release pages.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress stats
 * @param {string} props.processingType - Type of processing (GROSSING, MICROTOMY, etc.)
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PathologyProcessingPage({
  entryId,
  pageData,
  progress,
  processingType,
  onProgressUpdate,
}) {
  const intl = useIntl();

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processingNotes, setProcessingNotes] = useState("");
  const [technician, setTechnician] = useState("");
  const [notification, setNotification] = useState(null);

  useEffect(() => {
    loadPageSamples();
  }, [pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("pathology-")) {
      setLoading(false);
      return;
    }

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (response && Array.isArray(response)) {
          const transformedSamples = response.map((sample) => ({
            id: String(sample.id || sample.sampleItemId),
            accessionNumber: sample.accessionNumber,
            status: sample.pageStatus || "PENDING",
          }));
          setSamples(transformedSamples);
        }
        setLoading(false);
      },
    );
  }, [pageData?.id]);

  const handleProcessSamples = async () => {
    if (selectedSampleIds.length === 0) {
      setNotification({
        kind: "warning",
        title: "No samples selected",
        subtitle: "Please select samples to process",
      });
      return;
    }

    setNotification({
      kind: "info",
      title: "Processing Functionality",
      subtitle: `${processingType} processing endpoint will be implemented here. Selected ${selectedSampleIds.length} sample(s).`,
    });

    // TODO: Implement actual processing endpoint when backend is ready
    // For now, just simulate success
    setTimeout(() => {
      setNotification({
        kind: "success",
        title: "Samples Processed",
        subtitle: `${selectedSampleIds.length} sample(s) marked for ${processingType} processing`,
      });
      setSelectedSampleIds([]);
      setProcessingNotes("");
      setTechnician("");
    }, 1000);
  };

  const getPageTitle = () => {
    switch (processingType) {
      case "GROSSING":
        return "Grossing & Block Preparation";
      case "MICROTOMY":
        return "Microtomy & Slide Preparation";
      case "STAINING":
        return "Staining & Processing";
      case "PATHOLOGIST_REVIEW":
        return "Pathologist Review & Sign-off";
      case "RESULTS_RELEASE":
        return "Results Release & Archive";
      default:
        return pageData?.title || "Sample Processing";
    }
  };

  const getPageDescription = () => {
    switch (processingType) {
      case "GROSSING":
        return "Perform macroscopic examination and tissue block preparation";
      case "MICROTOMY":
        return "Section tissue blocks and prepare slides for staining";
      case "STAINING":
        return "Apply H&E and special stains to prepared slides";
      case "PATHOLOGIST_REVIEW":
        return "Review completed slides and sign off on results";
      case "RESULTS_RELEASE":
        return "Final verification and release of pathology reports";
      default:
        return "Process samples for this workflow step";
    }
  };

  if (loading) {
    return <Loading description="Loading samples..." withOverlay={false} />;
  }

  return (
    <Grid fullWidth>
      <Column lg={16}>
        <h3>{getPageTitle()}</h3>
        <p className="pathology-page-description">{getPageDescription()}</p>

        <ProgressBar
          label={`${progress.completed} / ${progress.total} samples processed`}
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
        <SampleGrid
          samples={samples}
          selectedSampleIds={selectedSampleIds}
          onSampleSelect={setSelectedSampleIds}
          enableSelection={true}
        />
      </Column>

      <Column lg={16}>
        <Tile className="pathology-processing-form">
          <Grid narrow>
            <Column lg={8}>
              <TextInput
                id="technician-id"
                labelText="Technician/Pathologist ID"
                placeholder="Enter staff ID"
                value={technician}
                onChange={(e) => setTechnician(e.target.value)}
              />
            </Column>

            <Column lg={16}>
              <TextArea
                id="processing-notes"
                labelText="Processing Notes"
                placeholder={`Enter ${processingType.toLowerCase()} details and observations...`}
                value={processingNotes}
                onChange={(e) => setProcessingNotes(e.target.value)}
                rows={4}
              />
            </Column>

            <Column lg={16}>
              <div className="pathology-action-buttons">
                <Button
                  kind="primary"
                  renderIcon={Save}
                  onClick={handleProcessSamples}
                  disabled={selectedSampleIds.length === 0}
                >
                  Process Samples ({selectedSampleIds.length} selected)
                </Button>
              </div>
            </Column>
          </Grid>
        </Tile>
      </Column>
    </Grid>
  );
}

export default PathologyProcessingPage;
