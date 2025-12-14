import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Tile,
  Button,
  Select,
  SelectItem,
  TextArea,
  InlineNotification,
  Loading,
  ProgressBar,
} from "@carbon/react";
import { Checkmark, Close } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../../notebook/workflow/SampleGrid";

/**
 * PathologyQCPage - QC checkpoint pages (Initial Inspection, Block QC, Slide QC).
 * Allows technicians to perform quality control checks and update sample status.
 */
function PathologyQCPage({
  entryId,
  pageData,
  progress,
  qcType,
  onProgressUpdate,
}) {
  const intl = useIntl();

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [qcStatus, setQcStatus] = useState("PASS");
  const [notes, setNotes] = useState("");
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

  const handleRecordQC = async () => {
    if (selectedSampleIds.length === 0) {
      setNotification({
        kind: "warning",
        title: "No samples selected",
        subtitle: "Please select samples to perform QC",
      });
      return;
    }

    try {
      // Record QC for each selected sample
      for (const sampleId of selectedSampleIds) {
        await postToOpenElisServer(
          "/rest/pathology/qc",
          JSON.stringify({
            sampleItemId: sampleId,
            qcType: qcType,
            status: qcStatus,
            notes: notes,
            pageId: pageData.id,
          }),
        );
      }

      setNotification({
        kind: "success",
        title: "QC Recorded",
        subtitle: `QC status (${qcStatus}) recorded for ${selectedSampleIds.length} sample(s)`,
      });

      // Reset and reload
      setSelectedSampleIds([]);
      setNotes("");
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    } catch (error) {
      setNotification({
        kind: "error",
        title: "Failed to record QC",
        subtitle: error.message,
      });
    }
  };

  if (loading) {
    return <Loading description="Loading samples..." withOverlay={false} />;
  }

  return (
    <Grid fullWidth>
      <Column lg={16}>
        <h3>
          <FormattedMessage
            id={`pathology.workflow.${qcType.toLowerCase()}`}
            defaultMessage={pageData?.title || "Quality Control"}
          />
        </h3>

        <ProgressBar
          label={`${progress.completed} / ${progress.total} samples QC completed`}
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
        <Tile className="pathology-qc-form">
          <Grid narrow>
            <Column lg={8}>
              <Select
                id="qc-status"
                labelText="QC Status"
                value={qcStatus}
                onChange={(e) => setQcStatus(e.target.value)}
              >
                <SelectItem value="PASS" text="Pass" />
                <SelectItem value="FAIL" text="Fail" />
              </Select>
            </Column>

            <Column lg={16}>
              <TextArea
                id="qc-notes"
                labelText="Notes"
                placeholder="Enter QC observations and notes..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={3}
              />
            </Column>

            <Column lg={16}>
              <div className="pathology-action-buttons">
                <Button
                  kind={qcStatus === "PASS" ? "primary" : "danger"}
                  renderIcon={qcStatus === "PASS" ? Checkmark : Close}
                  onClick={handleRecordQC}
                  disabled={selectedSampleIds.length === 0}
                >
                  Record QC ({selectedSampleIds.length} selected)
                </Button>
              </div>
            </Column>
          </Grid>
        </Tile>
      </Column>
    </Grid>
  );
}

export default PathologyQCPage;
