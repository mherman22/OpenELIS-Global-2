import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  Loading,
} from "@carbon/react";
import { Upload, Checkmark, Search } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";
import GenericManifestImportModal from "../modals/GenericManifestImportModal";
import "../workflow/NotebookWorkflow.css";

/**
 * SampleReceptionPage — generic sample reception page used by all labs that
 * declare {@code "pageType": "generic_sample_reception"} in their JSON config.
 *
 * Behaviour is driven by two optional arrays stored in {@code pageData.data}:
 * - {@code manifestColumns} — column schema for the generic import modal
 * - {@code columns}         — extra columns to show in the sample grid
 *   (each item: {@code { key: "fieldKey", header: "Display Header" }})
 *
 * Adding a new lab or changing displayed columns requires only a JSON edit —
 * no React changes.
 *
 * @param {Object}   props.entryId          Notebook entry ID
 * @param {Object}   props.pageData         Template page object (includes data)
 * @param {Object}   props.progress         Page progress counts
 * @param {function} props.onProgressUpdate Called when progress changes
 */
function SampleReceptionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Extra columns declared in the JSON config for this lab
  const extraColumns = useMemo(
    () => pageData?.data?.columns ?? [],
    [pageData?.data?.columns],
  );

  // Whether this page has a generic manifest schema configured
  const hasManifestConfig =
    Array.isArray(pageData?.data?.manifestColumns) &&
    pageData.data.manifestColumns.length > 0;

  // State
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [searchModalOpen, setSearchModalOpen] = useState(false);

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    // Skip loading for synthetic page IDs
    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    // Use page-specific endpoint to get samples with their page status
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            // Transform samples for the grid - ensure ID is string for consistent comparison
            const transformedSamples = response.map((sample) => ({
              // Spread extra fields from NotebookPageSample.data first so that
              // fixed fields below take precedence if there is a key collision.
              ...(sample.data || {}),
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              volume: sample.volume,
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

  // Handle manifest import success
  const handleImportSuccess = useCallback(
    (result) => {
      setImportModalOpen(false);
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    },
    [loadPageSamples, onProgressUpdate],
  );

  // Check if page has a real database ID (not a default synthetic ID)
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Handle bulk status change
  const handleBulkMarkVerified = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    // Require real page ID for verification - no local-only updates
    if (!hasRealPageId) {
      setError(
        "Cannot verify samples: Page not properly initialized. Please refresh the page.",
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status. Please try again.");
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle individual sample status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      // Require real page ID for status changes - no local-only updates
      if (!hasRealPageId) {
        setError(
          "Cannot update status: Page not properly initialized. Please refresh the page.",
        );
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status. Please try again.");
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate],
  );

  // Calculate verification stats
  const verifiedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  return (
    <div className="sample-reception-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.sampleReception.title"
            defaultMessage="Sample Reception"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.sampleReception.description"
            defaultMessage="Link existing samples or import new samples from a manifest file. Mark samples as verified when received."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.sampleReception.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.sampleReception.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{verifiedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.sampleReception.pending"
                  defaultMessage="Pending Verification"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Upload}
          disabled={!hasManifestConfig}
          onClick={() => setImportModalOpen(true)}
        >
          <FormattedMessage
            id="notebook.page.sampleReception.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Search}
          onClick={() => setSearchModalOpen(true)}
        >
          <FormattedMessage
            id="notebook.page.sampleReception.searchSamples"
            defaultMessage="Search & Link Samples"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkVerified}
          >
            <FormattedMessage
              id="notebook.page.sampleReception.markVerified"
              defaultMessage="Mark Selected as Verified ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Sample Grid — extra columns come from pageData.data.columns */}
      <div className="sample-grid-container">
        <SampleGrid
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          additionalColumns={extraColumns.map((col) => ({
            key: col.key,
            header: col.header,
            render: (value, sample) => sample?.[col.key] ?? value ?? "-",
          }))}
        />
      </div>

      {/* Instructions for empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.sampleReception.empty"
              defaultMessage="No samples have been added to this notebook yet. Use the buttons above to import samples from a manifest or search for existing samples."
            />
          </p>
        </div>
      )}

      {/* Generic Manifest Import Modal — driven entirely by pageData.data.manifestColumns */}
      {hasManifestConfig && (
        <GenericManifestImportModal
          open={importModalOpen}
          onClose={() => setImportModalOpen(false)}
          entryId={entryId}
          pageData={pageData}
          onImportSuccess={handleImportSuccess}
        />
      )}
    </div>
  );
}

export default SampleReceptionPage;
