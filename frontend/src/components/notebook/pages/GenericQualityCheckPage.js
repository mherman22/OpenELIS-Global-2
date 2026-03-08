import {
  Accordion,
  AccordionItem,
  Button,
  Column,
  Grid,
  InlineNotification,
  Loading,
  Modal,
  Select,
  SelectItem,
  Tag,
  TextArea,
  Tile,
} from "@carbon/react";
import { Checkmark, Close, Edit } from "@carbon/react/icons";
import { useCallback, useMemo, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useNotebookPage } from "../hooks/useNotebookPage";
import "../workflow/NotebookWorkflow.css";
import SampleGrid from "../workflow/SampleGrid";

/**
 * GenericQualityCheckPage — config-driven QC page for all labs.
 *
 * Replaces all per-lab QC page components. The entire checklist, result
 * options, and extra fields are driven by `pageData.data` JSON:
 *
 * ```json
 * {
 *   "qcSections": [
 *     {
 *       "id": "sample_integrity",
 *       "title": "Sample Integrity",
 *       "criteria": [
 *         { "key": "leakCheck", "label": "No leaks", "criticalForPass": true },
 *         { "key": "temperatureCheck", "label": "Temperature compliant" }
 *       ]
 *     }
 *   ],
 *   "qcResultOptions": [
 *     { "id": "PASS",         "label": "Pass – Proceed to processing" },
 *     { "id": "FAIL_DISCARD", "label": "Fail – Discard sample" },
 *     { "id": "FAIL_PROCEED", "label": "Fail – Proceed with remarks" }
 *   ],
 *   "additionalFields": [
 *     { "key": "rejectionReason", "label": "Rejection Reason", "type": "text" },
 *     { "key": "remarks",         "label": "Remarks",          "type": "textarea" }
 *   ]
 * }
 * ```
 *
 * If no config is present the page falls back to a simple pass/fail form
 * (useful during migration of pages that haven't been configured yet).
 *
 * @param {Object}   props.entryId          - Notebook entry ID
 * @param {Object}   props.pageData         - Template page object (includes data)
 * @param {function} props.onProgressUpdate - Called when progress changes
 */
function GenericQualityCheckPage({ entryId, pageData, onProgressUpdate }) {
  const intl = useIntl();

  const {
    samples,
    selectedSampleIds,
    setSelectedSampleIds,
    statusFilter,
    setStatusFilter,
    loading,
    error,
    setError,
    successMessage,
    setSuccessMessage,
    applyBulkData,
  } = useNotebookPage(entryId, pageData, onProgressUpdate);

  // ── Config from pageData.config (template-seeded, read-only) ────────────
  // Fall back to pageData.data for backwards-compat with rows created before migration 030.
  const cfg = pageData?.config ?? pageData?.data ?? {};
  const qcSections = useMemo(
    () => cfg?.qcSections ?? [],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [pageData?.config, pageData?.data],
  );
  const qcResultOptions = useMemo(
    () =>
      cfg?.qcResultOptions ?? [
        { id: "PASS", label: "Pass – Proceed" },
        { id: "FAIL_DISCARD", label: "Fail – Discard sample" },
        { id: "FAIL_PROCEED", label: "Fail – Proceed with remarks" },
      ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [pageData?.config, pageData?.data],
  );
  const additionalFields = useMemo(
    () => cfg?.additionalFields ?? [],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [pageData?.config, pageData?.data],
  );

  // ── Modal state ───────────────────────────────────────────────────────────
  const [modalOpen, setModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  // qcChecks: { [criterionKey]: true | false | null }
  const [qcChecks, setQcChecks] = useState({});
  const [qcResult, setQcResult] = useState("");
  const [manualResultOverride, setManualResultOverride] = useState(false);
  const [extraValues, setExtraValues] = useState({});

  // ── Auto-calculate QC result ──────────────────────────────────────────────
  const calculatedResult = useMemo(() => {
    const allCriteria = qcSections.flatMap((s) => s.criteria ?? []);
    if (allCriteria.length === 0) return "";

    const critical = allCriteria.filter((c) => c.criticalForPass);
    const criticalFailed = critical.some((c) => qcChecks[c.key] === false);
    const anyFailed = allCriteria.some((c) => qcChecks[c.key] === false);

    if (criticalFailed) return "FAIL_DISCARD";
    if (anyFailed) return "FAIL_PROCEED";

    const anyChecked = allCriteria.some((c) => qcChecks[c.key] !== undefined);
    return anyChecked ? "PASS" : "";
  }, [qcChecks, qcSections]);

  // Sync auto-calculation into qcResult unless user manually overrode
  const effectiveQcResult = manualResultOverride
    ? qcResult
    : calculatedResult || qcResult;

  // ── Stats ─────────────────────────────────────────────────────────────────
  const passedCount = samples.filter(
    (s) => s.qcResult === "PASS" || s.status === "COMPLETED",
  ).length;
  const failedCount = samples.filter(
    (s) => s.qcResult === "FAIL_DISCARD" || s.qcResult === "FAIL_PROCEED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // ── Handlers ──────────────────────────────────────────────────────────────
  const openModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError("Please select at least one sample to apply QC.");
      return;
    }
    setQcChecks({});
    setQcResult("");
    setManualResultOverride(false);
    setExtraValues({});
    setModalOpen(true);
  }, [selectedSampleIds, setError]);

  const handleCheck = useCallback((key, value) => {
    setQcChecks((prev) => ({ ...prev, [key]: value }));
  }, []);

  const handleExtraChange = useCallback((key, value) => {
    setExtraValues((prev) => ({ ...prev, [key]: value }));
  }, []);

  const handleSubmit = useCallback(() => {
    const finalResult = effectiveQcResult;
    if (!finalResult) {
      setError("Please select a QC result before submitting.");
      return;
    }

    setIsSubmitting(true);

    // Build data payload
    const data = {};
    Object.entries(qcChecks).forEach(([k, v]) => {
      if (v !== null && v !== undefined) data[k] = v;
    });
    Object.entries(extraValues).forEach(([k, v]) => {
      if (v !== "" && v !== null && v !== undefined) data[k] = v;
    });
    data.qcResult = finalResult;

    applyBulkData(
      selectedSampleIds,
      data,
      () => {
        setIsSubmitting(false);
        setModalOpen(false);

        const isPass = finalResult === "PASS" || finalResult.startsWith("PASS");
        setSuccessMessage(
          isPass
            ? intl.formatMessage(
                {
                  id: "notebook.qc.applied.pass",
                  defaultMessage: "QC passed for {count} sample(s).",
                },
                { count: selectedSampleIds.length },
              )
            : intl.formatMessage(
                {
                  id: "notebook.qc.applied.fail",
                  defaultMessage: "QC recorded for {count} sample(s).",
                },
                { count: selectedSampleIds.length },
              ),
        );
      },
      () => {
        // Error path: hook already called setError; reset submitting state
        setIsSubmitting(false);
      },
    );
  }, [
    effectiveQcResult,
    qcChecks,
    extraValues,
    selectedSampleIds,
    applyBulkData,
    intl,
    setSuccessMessage,
  ]);

  const renderCriterion = useCallback(
    (criterion) => {
      const val = qcChecks[criterion.key];
      return (
        <div key={criterion.key} className="qc-criterion-row">
          <span className="qc-criterion-label">
            {criterion.label}
            {criterion.criticalForPass && (
              <span className="qc-critical-badge"> *</span>
            )}
          </span>
          <div className="qc-criterion-buttons">
            <Button
              kind={val === true ? "primary" : "tertiary"}
              size="sm"
              renderIcon={Checkmark}
              onClick={() => handleCheck(criterion.key, true)}
            >
              <FormattedMessage id="notebook.qc.pass" defaultMessage="Pass" />
            </Button>
            <Button
              kind={val === false ? "danger" : "ghost"}
              size="sm"
              renderIcon={Close}
              onClick={() => handleCheck(criterion.key, false)}
            >
              <FormattedMessage id="notebook.qc.fail" defaultMessage="Fail" />
            </Button>
          </div>
        </div>
      );
    },
    [qcChecks, handleCheck],
  );

  const renderAdditionalField = useCallback(
    (field) => {
      const val = extraValues[field.key] ?? "";
      if (field.type === "textarea") {
        return (
          <TextArea
            key={field.key}
            id={`qc-extra-${field.key}`}
            labelText={field.label}
            value={val}
            onChange={(e) => handleExtraChange(field.key, e.target.value)}
            rows={3}
          />
        );
      }
      if (field.type === "select" && Array.isArray(field.options)) {
        return (
          <Select
            key={field.key}
            id={`qc-extra-${field.key}`}
            labelText={field.label}
            value={val}
            onChange={(e) => handleExtraChange(field.key, e.target.value)}
          >
            <SelectItem value="" text="" />
            {field.options.map((opt) => (
              <SelectItem
                key={opt.id ?? opt}
                value={opt.id ?? opt}
                text={opt.label ?? opt}
              />
            ))}
          </Select>
        );
      }
      // default: text input
      return (
        <div key={field.key} className="cds--form-item">
          <label htmlFor={`qc-extra-${field.key}`} className="cds--label">
            {field.label}
          </label>
          <input
            id={`qc-extra-${field.key}`}
            className="cds--text-input"
            type={field.type === "number" ? "number" : "text"}
            value={val}
            onChange={(e) => handleExtraChange(field.key, e.target.value)}
          />
        </div>
      );
    },
    [extraValues, handleExtraChange],
  );

  return (
    <div className="qc-page">
      {/* Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.qc.title"
            defaultMessage="Quality Check"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.qc.description"
            defaultMessage="Assess sample quality. Select samples and apply QC criteria to accept or reject."
          />
        </p>
      </div>

      {/* Progress summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.qc.total"
                  defaultMessage="Total"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.qc.passed"
                  defaultMessage="Passed"
                />
              </span>
              <span className="progress-value">{passedCount}</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ borderLeft: "4px solid #da1e28" }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.qc.failed"
                  defaultMessage="Failed"
                />
              </span>
              <span className="progress-value">{failedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.qc.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action bar */}
      <div className="page-actions-bar">
        {selectedSampleIds.length > 0 && (
          <Button
            kind="primary"
            size="sm"
            renderIcon={Edit}
            onClick={openModal}
          >
            <FormattedMessage
              id="notebook.page.qc.applyQc"
              defaultMessage="Apply QC ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
          onCloseButtonClick={() => setError(null)}
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          hideCloseButton
          lowContrast
          onCloseButtonClick={() => setSuccessMessage(null)}
        />
      )}

      {/* Sample grid */}
      <div className="sample-grid-container">
        {loading ? (
          <Loading withOverlay={false} />
        ) : (
          <SampleGrid
            samples={samples}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={false}
          />
        )}
      </div>

      {/* QC Modal */}
      <Modal
        open={modalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.qc.modal.title",
          defaultMessage: "Apply Quality Check",
        })}
        primaryButtonText={
          isSubmitting
            ? intl.formatMessage({
                id: "notebook.saving",
                defaultMessage: "Saving…",
              })
            : intl.formatMessage({
                id: "notebook.page.qc.modal.submit",
                defaultMessage: "Submit QC",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSubmit}
        onRequestClose={() => setModalOpen(false)}
        onSecondarySubmit={() => setModalOpen(false)}
        primaryButtonDisabled={isSubmitting}
        size="md"
      >
        <div className="qc-modal-content">
          <p className="qc-modal-intro">
            <FormattedMessage
              id="notebook.page.qc.modal.intro"
              defaultMessage="Applying QC to {count} sample(s). Mark each criterion as Pass or Fail."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* QC criteria sections */}
          {qcSections.length > 0 ? (
            <Accordion>
              {qcSections.map((section) => (
                <AccordionItem
                  key={section.id}
                  title={section.title}
                  open={true}
                >
                  <div className="qc-criteria-list">
                    {(section.criteria ?? []).map(renderCriterion)}
                  </div>
                  {section.criteria?.some((c) => c.criticalForPass) && (
                    <p className="qc-critical-note">
                      <FormattedMessage
                        id="notebook.page.qc.criticalNote"
                        defaultMessage="* Critical criteria — a Fail here sets the result to Discard."
                      />
                    </p>
                  )}
                </AccordionItem>
              ))}
            </Accordion>
          ) : null}

          {/* Auto-calculated or manual result */}
          <div className="qc-result-section">
            {effectiveQcResult && !manualResultOverride && (
              <div className="qc-auto-result">
                <Tag
                  type={
                    effectiveQcResult === "PASS"
                      ? "green"
                      : effectiveQcResult.startsWith("PASS")
                        ? "teal"
                        : "red"
                  }
                >
                  {qcResultOptions.find((o) => o.id === effectiveQcResult)
                    ?.label ?? effectiveQcResult}
                </Tag>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => {
                    setQcResult(effectiveQcResult);
                    setManualResultOverride(true);
                  }}
                >
                  <FormattedMessage
                    id="notebook.page.qc.overrideResult"
                    defaultMessage="Override"
                  />
                </Button>
              </div>
            )}

            {(manualResultOverride || !calculatedResult) && (
              <Select
                id="qc-result-select"
                labelText={intl.formatMessage({
                  id: "notebook.page.qc.result",
                  defaultMessage: "QC Result",
                })}
                value={effectiveQcResult}
                onChange={(e) => {
                  setQcResult(e.target.value);
                  setManualResultOverride(true);
                }}
              >
                <SelectItem value="" text="" />
                {qcResultOptions.map((opt) => (
                  <SelectItem key={opt.id} value={opt.id} text={opt.label} />
                ))}
              </Select>
            )}
          </div>

          {/* Additional lab-specific fields */}
          {additionalFields.length > 0 && (
            <div className="qc-additional-fields">
              {additionalFields.map(renderAdditionalField)}
            </div>
          )}

          {/* Always show a remarks field */}
          {!additionalFields.some((f) => f.key === "remarks") && (
            <TextArea
              id="qc-remarks"
              labelText={intl.formatMessage({
                id: "notebook.page.qc.remarks",
                defaultMessage: "Remarks",
              })}
              value={extraValues.remarks ?? ""}
              onChange={(e) => handleExtraChange("remarks", e.target.value)}
              rows={3}
            />
          )}
        </div>
      </Modal>
    </div>
  );
}

export default GenericQualityCheckPage;
