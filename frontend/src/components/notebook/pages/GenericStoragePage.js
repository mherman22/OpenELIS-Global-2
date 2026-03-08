import {
  Button,
  Column,
  Dropdown,
  Grid,
  InlineNotification,
  Modal,
  NumberInput,
  Tag,
  TextInput,
  Tile,
} from "@carbon/react";
import { Archive, Renew } from "@carbon/react/icons";
import { useCallback, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServer } from "../../utils/Utils";
import { useNotebookPage } from "../hooks/useNotebookPage";
import BoxLayoutViewer from "../workflow/BoxLayoutViewer";
import "../workflow/NotebookWorkflow.css";
import SampleGrid from "../workflow/SampleGrid";
import StorageHierarchySelector from "../workflow/StorageHierarchySelector";

/**
 * GenericStoragePage — config-driven storage assignment page used by all labs.
 *
 * Replaces per-lab storage pages (TBStorageAssignmentPage, PharmaceuticalStoragePage,
 * MNTDTemporaryStoragePage, BacteriologyTemporaryStoragePage, etc.).
 *
 * Uses StorageHierarchySelector (shared) + BoxLayoutViewer (shared).
 * Posts to /rest/notebook/bulk/page/:id/samples/storage.
 *
 * JSON config (pageData.data):
 * {
 *   "storageConditions": [
 *     { "id": "FROZEN_MINUS80", "label": "Frozen (-80°C)", "default": true },
 *     { "id": "FROZEN_MINUS20", "label": "Frozen (-20°C)" },
 *     { "id": "REFRIGERATED",   "label": "Refrigerated (2-8°C)" },
 *     { "id": "ROOM_TEMP",      "label": "Room Temperature (15-25°C)" },
 *     { "id": "LIQUID_NITROGEN","label": "Liquid Nitrogen (-196°C)" }
 *   ],
 *   "defaultRetentionYears": 5,
 *   "additionalFields": [
 *     { "key": "notes", "label": "Notes", "type": "text" }
 *   ]
 * }
 */
function GenericStoragePage({ entryId, pageData, progress, onProgressUpdate }) {
  const intl = useIntl();

  // Read config from pageData.config (template-seeded, read-only).
  // Fall back to pageData.data for backwards-compat with rows created before migration 030.
  const config = pageData?.config ?? pageData?.data ?? {};
  const storageConditions = config.storageConditions ?? [
    { id: "REFRIGERATED", label: "Refrigerated (2-8°C)" },
    { id: "FROZEN_MINUS20", label: "Frozen (-20°C)" },
    { id: "FROZEN_MINUS80", label: "Frozen (-80°C)" },
    { id: "ROOM_TEMP", label: "Room Temperature (15-25°C)" },
    { id: "LIQUID_NITROGEN", label: "Liquid Nitrogen (-196°C)" },
  ];
  const defaultRetentionYears = config.defaultRetentionYears ?? 5;
  const additionalFields = config.additionalFields ?? [];

  // Default storage condition: first item marked "default", else first item
  const defaultCondition =
    storageConditions.find((c) => c.default) ?? storageConditions[0] ?? null;

  // Core page state from shared hook
  const {
    samples,
    setSamples,
    selectedSampleIds,
    setSelectedSampleIds,
    statusFilter,
    setStatusFilter,
    loading,
    error,
    setError,
    successMessage,
    setSuccessMessage,
    hasRealPageId,
    componentMounted,
  } = useNotebookPage(entryId, pageData, onProgressUpdate);

  // Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [assigning, setAssigning] = useState(false);

  // Storage hierarchy from StorageHierarchySelector
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  const [boxLayout, setBoxLayout] = useState({});
  const [wellAssignments, setWellAssignments] = useState({});

  // Form fields
  const [selectedCondition, setSelectedCondition] = useState(defaultCondition);
  const [retentionYears, setRetentionYears] = useState(defaultRetentionYears);
  const [additionalValues, setAdditionalValues] = useState({});

  // Summary counts
  const pendingCount = samples.filter(
    (s) => !s.storageBox && s.status !== "COMPLETED",
  ).length;
  const assignedCount = samples.filter((s) => s.storageBox).length;

  const openModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.storage.error.noSelection",
          defaultMessage: "Select at least one sample to assign storage.",
        }),
      );
      return;
    }
    setWellAssignments({});
    setStorageSelection({
      room: null,
      device: null,
      shelf: null,
      rack: null,
      box: null,
    });
    setSelectedCondition(defaultCondition);
    setRetentionYears(defaultRetentionYears);
    setAdditionalValues({});
    setModalOpen(true);
  }, [
    selectedSampleIds,
    defaultCondition,
    defaultRetentionYears,
    intl,
    setError,
  ]);

  const handleStorageSelectionChange = useCallback((selection) => {
    setStorageSelection(selection);
  }, []);

  const handleBoxLayoutLoaded = useCallback((layout) => {
    setBoxLayout(layout);
  }, []);

  const handleWellAssignment = useCallback((sampleId, well) => {
    setWellAssignments((prev) => ({ ...prev, [sampleId]: well }));
  }, []);

  const handleAssign = useCallback(() => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.storage.error.noBox",
          defaultMessage: "Select a storage box before assigning.",
        }),
      );
      return;
    }

    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.storage.error.noWells",
          defaultMessage: "Assign at least one sample to a well position.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const storagePath = [
      storageSelection.room?.label,
      storageSelection.device?.label,
      storageSelection.shelf?.label,
      storageSelection.rack?.label,
      storageSelection.box?.label,
    ]
      .filter(Boolean)
      .join(" > ");

    const payload = {
      sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
      boxId: parseInt(storageSelection.box.id, 10),
      wellAssignments,
      data: {
        storageRoom: storageSelection.room?.label,
        storageFreezer: storageSelection.device?.label,
        storageShelf: storageSelection.shelf?.label,
        storageRack: storageSelection.rack?.label,
        storageBox: storageSelection.box?.label,
        storagePath,
        storageCondition: selectedCondition?.id,
        retentionYears,
        assignedDateTime: new Date().toISOString(),
        ...additionalValues,
      },
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/storage`,
      JSON.stringify(payload),
      (status) => {
        if (!componentMounted.current) return;
        setAssigning(false);
        if (status === 200) {
          setModalOpen(false);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.storage.success",
                defaultMessage: "{count} sample(s) assigned to {box}.",
              },
              {
                count: Object.keys(wellAssignments).length,
                box: storageSelection.box?.label,
              },
            ),
          );
          // Update local sample state to reflect assignment
          const assignedIds = new Set(Object.keys(wellAssignments));
          setSamples((prev) =>
            prev.map((s) =>
              assignedIds.has(String(s.id))
                ? { ...s, storageBox: storageSelection.box?.label, storagePath }
                : s,
            ),
          );
          setSelectedSampleIds([]);
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.storage.error.save",
              defaultMessage: "Failed to assign storage. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    storageSelection,
    wellAssignments,
    selectedCondition,
    retentionYears,
    additionalValues,
    pageData?.id,
    intl,
    setError,
    setSuccessMessage,
    setSamples,
    setSelectedSampleIds,
    onProgressUpdate,
    componentMounted,
  ]);

  const columns = [
    { key: "groupId", header: "Sample ID" },
    { key: "sampleType", header: "Type" },
    {
      key: "storagePath",
      header: "Storage Location",
      render: (s) =>
        s.storagePath ? (
          <Tag type="green" size="sm">
            {s.storagePath}
          </Tag>
        ) : (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.storage.pending"
              defaultMessage="Pending"
            />
          </Tag>
        ),
    },
    { key: "storageCondition", header: "Condition" },
  ];

  return (
    <div className="generic-storage-page notebook-page">
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header">
            <h3>{pageData?.title}</h3>
            {pageData?.instructions && (
              <p className="page-description">{pageData.instructions}</p>
            )}
          </div>
        </Column>

        {/* Summary tiles */}
        <Column lg={4} md={4} sm={4}>
          <Tile className="summary-tile">
            <p className="summary-label">
              <FormattedMessage
                id="notebook.storage.pending"
                defaultMessage="Pending"
              />
            </p>
            <p className="summary-value">{pendingCount}</p>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="summary-tile">
            <p className="summary-label">
              <FormattedMessage
                id="notebook.storage.assigned"
                defaultMessage="Assigned"
              />
            </p>
            <p className="summary-value">{assignedCount}</p>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="summary-tile">
            <p className="summary-label">
              <FormattedMessage
                id="notebook.storage.total"
                defaultMessage="Total"
              />
            </p>
            <p className="summary-value">{samples.length}</p>
          </Tile>
        </Column>

        {/* Notifications */}
        {error && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              title={error}
              onCloseButtonClick={() => setError(null)}
            />
          </Column>
        )}
        {successMessage && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="success"
              title={successMessage}
              onCloseButtonClick={() => setSuccessMessage(null)}
            />
          </Column>
        )}

        {/* Action bar */}
        <Column lg={16} md={8} sm={4}>
          <div className="action-bar">
            <Button
              kind="primary"
              renderIcon={Archive}
              onClick={openModal}
              disabled={!hasRealPageId || selectedSampleIds.length === 0}
            >
              <FormattedMessage
                id="notebook.storage.assignButton"
                defaultMessage="Assign to Storage ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="ghost"
              renderIcon={Renew}
              onClick={() => window.location.reload()}
              disabled={!hasRealPageId}
            >
              <FormattedMessage
                id="notebook.refresh"
                defaultMessage="Refresh"
              />
            </Button>
          </div>
        </Column>

        {/* Sample grid */}
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            samples={samples}
            selectedSampleIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            loading={loading}
            columns={columns}
          />
        </Column>
      </Grid>

      {/* Storage assignment modal */}
      <Modal
        open={modalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.storage.modal.title",
          defaultMessage: "Assign Storage Location",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "notebook.storage.modal.saving",
                defaultMessage: "Assigning…",
              })
            : intl.formatMessage({
                id: "notebook.storage.modal.confirm",
                defaultMessage: "Confirm Assignment",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssign}
        onRequestClose={() => setModalOpen(false)}
        onSecondarySubmit={() => setModalOpen(false)}
        primaryButtonDisabled={
          assigning ||
          !storageSelection.box ||
          Object.keys(wellAssignments).length === 0
        }
        size="lg"
      >
        <div className="storage-modal-content">
          <p className="storage-modal-intro">
            <FormattedMessage
              id="notebook.storage.modal.intro"
              defaultMessage="Select a storage location for {count} sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Shared hierarchy selector */}
          <StorageHierarchySelector
            onSelectionChange={handleStorageSelectionChange}
            entryId={entryId}
            onBoxLayoutLoaded={handleBoxLayoutLoaded}
          />

          {/* Well assignment via BoxLayoutViewer */}
          {storageSelection.box && (
            <div className="storage-well-section">
              <h5>
                <FormattedMessage
                  id="notebook.storage.wellAssignment"
                  defaultMessage="Well Assignment"
                />
              </h5>
              <BoxLayoutViewer
                boxId={storageSelection.box.id}
                entryId={entryId}
                samples={samples.filter((s) =>
                  selectedSampleIds.includes(String(s.id)),
                )}
                onWellAssignment={handleWellAssignment}
                wellAssignments={wellAssignments}
                existingLayout={boxLayout}
              />
            </div>
          )}

          {/* Storage condition */}
          <div className="storage-condition-section">
            <Dropdown
              id="storage-condition"
              titleText={intl.formatMessage({
                id: "notebook.storage.condition.label",
                defaultMessage: "Storage Condition",
              })}
              items={storageConditions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={selectedCondition}
              onChange={({ selectedItem }) =>
                setSelectedCondition(selectedItem)
              }
            />
          </div>

          {/* Retention period */}
          <div className="storage-retention-section">
            <NumberInput
              id="retention-years"
              label={intl.formatMessage({
                id: "notebook.storage.retention.label",
                defaultMessage: "Retention Period (years)",
              })}
              value={retentionYears}
              min={1}
              max={50}
              onChange={(e, { value }) => setRetentionYears(value)}
            />
          </div>

          {/* Additional fields from JSON config */}
          {additionalFields.length > 0 && (
            <div className="storage-additional-fields">
              {additionalFields.map((field) => (
                <TextInput
                  key={field.key}
                  id={`storage-field-${field.key}`}
                  labelText={field.label}
                  value={additionalValues[field.key] ?? ""}
                  onChange={(e) =>
                    setAdditionalValues((prev) => ({
                      ...prev,
                      [field.key]: e.target.value,
                    }))
                  }
                />
              ))}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default GenericStoragePage;
