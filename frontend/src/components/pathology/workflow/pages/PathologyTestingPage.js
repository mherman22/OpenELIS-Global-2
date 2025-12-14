import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Tile,
  Button,
  Select,
  SelectItem,
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
 * PathologyTestingPage - Test execution and result entry page.
 * Handles microscopic examination, IHC, and molecular testing.
 */
function PathologyTestingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();

  const [samples, setSamples] = useState([]);
  const [selectedSample, setSelectedSample] = useState(null);
  const [loading, setLoading] = useState(true);
  const [testType, setTestType] = useState("HISTOCHEMISTRY");
  const [stainName, setStainName] = useState("");
  const [resultData, setResultData] = useState("");
  const [controlStatus, setControlStatus] = useState({
    positive: "NOT_TESTED",
    negative: "NOT_TESTED",
  });
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

  const handleRecordTest = async () => {
    if (!selectedSample) {
      setNotification({
        kind: "warning",
        title: "No sample selected",
        subtitle: "Please select a sample to record test results",
      });
      return;
    }

    try {
      await postToOpenElisServer(
        "/rest/pathology/test-results",
        JSON.stringify({
          sampleItemId: selectedSample.id,
          testType: testType,
          stainName: stainName,
          resultData: resultData,
          positiveControlStatus: controlStatus.positive,
          negativeControlStatus: controlStatus.negative,
          pageId: pageData.id,
        }),
      );

      setNotification({
        kind: "success",
        title: "Test Results Recorded",
        subtitle: `Test results saved for sample ${selectedSample.accessionNumber}`,
      });

      // Reset form
      setSelectedSample(null);
      setStainName("");
      setResultData("");
      setControlStatus({ positive: "NOT_TESTED", negative: "NOT_TESTED" });

      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    } catch (error) {
      setNotification({
        kind: "error",
        title: "Failed to record test",
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
            id="pathology.workflow.microscopy"
            defaultMessage="Microscopic Examination & Testing"
          />
        </h3>

        <ProgressBar
          label={`${progress.completed} / ${progress.total} samples tested`}
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
          selectedSampleIds={selectedSample ? [selectedSample.id] : []}
          onSampleSelect={(ids) => {
            const sample = samples.find((s) => s.id === ids[0]);
            setSelectedSample(sample || null);
          }}
          enableSelection={true}
          singleSelect={true}
        />
      </Column>

      {selectedSample && (
        <Column lg={16}>
          <Tile className="pathology-test-form">
            <h4>Test Entry for Sample: {selectedSample.accessionNumber}</h4>
            <Grid narrow>
              <Column lg={8}>
                <Select
                  id="test-type"
                  labelText="Test Type"
                  value={testType}
                  onChange={(e) => setTestType(e.target.value)}
                >
                  <SelectItem value="HISTOCHEMISTRY" text="Histochemistry" />
                  <SelectItem
                    value="IMMUNOHISTOCHEMISTRY"
                    text="Immunohistochemistry (IHC)"
                  />
                  <SelectItem value="MOLECULAR" text="Molecular Testing" />
                  <SelectItem value="CYTOLOGY" text="Cytology" />
                </Select>
              </Column>

              <Column lg={8}>
                <TextInput
                  id="stain-name"
                  labelText="Stain/Test Name"
                  placeholder="e.g., H&E, Ki-67, ER/PR"
                  value={stainName}
                  onChange={(e) => setStainName(e.target.value)}
                  required
                />
              </Column>

              <Column lg={16} className="pathology-control-section">
                <h5>Control Validation</h5>
                <Grid narrow>
                  <Column lg={8}>
                    <Select
                      id="positive-control"
                      labelText="Positive Control"
                      value={controlStatus.positive}
                      onChange={(e) =>
                        setControlStatus({
                          ...controlStatus,
                          positive: e.target.value,
                        })
                      }
                    >
                      <SelectItem value="NOT_TESTED" text="Not Tested" />
                      <SelectItem value="PASS" text="Pass" />
                      <SelectItem value="FAIL" text="Fail" />
                    </Select>
                  </Column>

                  <Column lg={8}>
                    <Select
                      id="negative-control"
                      labelText="Negative Control"
                      value={controlStatus.negative}
                      onChange={(e) =>
                        setControlStatus({
                          ...controlStatus,
                          negative: e.target.value,
                        })
                      }
                    >
                      <SelectItem value="NOT_TESTED" text="Not Tested" />
                      <SelectItem value="PASS" text="Pass" />
                      <SelectItem value="FAIL" text="Fail" />
                    </Select>
                  </Column>
                </Grid>
              </Column>

              <Column lg={16}>
                <TextArea
                  id="result-data"
                  labelText="Test Results / Observations"
                  placeholder="Enter test results, scoring, and observations..."
                  value={resultData}
                  onChange={(e) => setResultData(e.target.value)}
                  rows={5}
                  required
                />
              </Column>

              <Column lg={16}>
                <div className="pathology-action-buttons">
                  <Button
                    kind="primary"
                    renderIcon={Save}
                    onClick={handleRecordTest}
                  >
                    Save Test Results
                  </Button>
                </div>
              </Column>
            </Grid>
          </Tile>
        </Column>
      )}
    </Grid>
  );
}

export default PathologyTestingPage;
