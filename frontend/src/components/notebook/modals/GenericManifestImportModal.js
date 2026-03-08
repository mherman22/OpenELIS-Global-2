import {
  Button,
  DataTable,
  InlineNotification,
  Loading,
  Modal,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { Upload } from "@carbon/react/icons";
import { useCallback, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerFullResponse } from "../../utils/Utils";

/**
 * GenericManifestImportModal — lab-agnostic manifest import.
 *
 * Reads its column schema from {@code pageData.data.manifestColumns}, which is
 * populated by {@code NotebookTemplateConfigurationHandler} at startup from the
 * lab's JSON config file.  No React code changes are needed when a new lab is
 * added; just declare {@code manifestColumns} in the JSON.
 *
 * Schema shape (one object per column):
 * <pre>
 * {
 *   "csvHeader": "Sample ID",   // suggested CSV column header (shown as hint)
 *   "field":     "groupId",     // target field key
 *   "label":     "Sample ID",   // UI label shown next to the dropdown
 *   "required":  true           // marks required fields
 * }
 * </pre>
 *
 * @param {boolean}  open             Modal open state
 * @param {function} onClose          Called when modal is dismissed
 * @param {number}   entryId          Notebook entry ID
 * @param {Object}   pageData         Template page object (must contain data.manifestColumns)
 * @param {function} onImportSuccess  Called with the result object after a successful import
 */
function GenericManifestImportModal({
  open,
  onClose,
  entryId,
  pageData,
  onImportSuccess,
}) {
  const intl = useIntl();
  const fileRef = useRef(null);

  // The column schema declared in the JSON config for this page
  const manifestColumns = pageData?.data?.manifestColumns ?? [];

  // ── state ──────────────────────────────────────────────────────────────────
  const [file, setFile] = useState(null);
  const [csvHeaders, setCsvHeaders] = useState([]); // headers detected in uploaded CSV
  // Mapping: fieldKey → csvHeaderName selected by user
  const [columnMapping, setColumnMapping] = useState({});
  const [preview, setPreview] = useState(null); // array of first-N preview rows
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // ── CSV header detection ───────────────────────────────────────────────────
  const parseHeaders = useCallback(
    (csvFile) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target.result;
        const firstLine = text.split(/\r?\n/)[0] ?? "";
        const headers = firstLine
          .split(",")
          .map((h) => h.trim())
          .filter(Boolean);
        setCsvHeaders(headers);

        // Auto-map: if a schema column's csvHeader exactly matches a detected header
        // (case-insensitive), pre-select it
        const autoMap = {};
        manifestColumns.forEach((col) => {
          if (!col.field) return;
          const match = headers.find(
            (h) => h.toLowerCase() === (col.csvHeader ?? "").toLowerCase(),
          );
          if (match) autoMap[col.field] = match;
        });
        setColumnMapping(autoMap);

        // Build a small preview (first 5 data rows)
        const lines = text.split(/\r?\n/).filter((l) => l.trim());
        const dataLines = lines.slice(1, 6);
        const rows = dataLines.map((line) =>
          line.split(",").map((v) => v.trim()),
        );
        setPreview({ headers, rows });
      };
      reader.readAsText(csvFile);
    },
    [manifestColumns],
  );

  const handleFileChange = useCallback(
    (e) => {
      const selected = e.target.files?.[0];
      if (!selected) return;
      setFile(selected);
      setError(null);
      setSuccess(null);
      setPreview(null);
      parseHeaders(selected);
    },
    [parseHeaders],
  );

  // ── import ─────────────────────────────────────────────────────────────────
  const handleImport = useCallback(() => {
    if (!file) {
      setError(
        intl.formatMessage({
          id: "notebook.manifest.error.noFile",
          defaultMessage: "Please select a CSV file first.",
        }),
      );
      return;
    }

    // Validate required fields
    const missing = manifestColumns
      .filter((col) => col.required && !columnMapping[col.field])
      .map((col) => col.label ?? col.field);
    if (missing.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.manifest.error.requiredColumns",
            defaultMessage: "Please map required columns: {columns}",
          },
          { columns: missing.join(", ") },
        ),
      );
      return;
    }

    setLoading(true);
    setError(null);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("columnMapping", JSON.stringify(columnMapping));

    postToOpenElisServerFullResponse(
      `/rest/notebook/entry/${entryId}/page/${pageData.id}/import-manifest`,
      formData,
      (response) => {
        setLoading(false);
        if (response?.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.manifest.success",
                defaultMessage: "Successfully imported {count} sample(s).",
              },
              { count: response.totalCreated },
            ),
          );
          if (onImportSuccess) onImportSuccess(response);
        } else {
          const msgs = (response?.errors ?? [])
            .map((e) => `Row ${e.rowNumber}: ${e.message}`)
            .join("; ");
          setError(
            response?.error
              ? `${response.error}${msgs ? ` — ${msgs}` : ""}`
              : intl.formatMessage({
                  id: "notebook.manifest.error.generic",
                  defaultMessage:
                    "Import failed. Please check the file and mapping.",
                }),
          );
        }
      },
      true, // multipart
    );
  }, [
    file,
    columnMapping,
    manifestColumns,
    entryId,
    pageData,
    intl,
    onImportSuccess,
  ]);

  const handleClose = () => {
    setFile(null);
    setCsvHeaders([]);
    setColumnMapping({});
    setPreview(null);
    setError(null);
    setSuccess(null);
    onClose();
  };

  // ── render ─────────────────────────────────────────────────────────────────
  const canImport =
    file !== null &&
    manifestColumns
      .filter((c) => c.required)
      .every((c) => columnMapping[c.field]);

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "notebook.manifest.modal.title",
        defaultMessage: "Import Samples from Manifest",
      })}
      primaryButtonText={
        loading ? (
          <Loading small withOverlay={false} />
        ) : (
          intl.formatMessage({
            id: "notebook.manifest.import",
            defaultMessage: "Import",
          })
        )
      }
      secondaryButtonText={intl.formatMessage({
        id: "label.button.cancel",
        defaultMessage: "Cancel",
      })}
      primaryButtonDisabled={!canImport || loading || !!success}
      onRequestSubmit={handleImport}
      size="lg"
    >
      {/* ── Step 1: file upload ── */}
      <div style={{ marginBottom: "1.5rem" }}>
        <p style={{ marginBottom: "0.5rem", fontWeight: 600 }}>
          <FormattedMessage
            id="notebook.manifest.step1"
            defaultMessage="Step 1 — Upload CSV file"
          />
        </p>
        <input
          ref={fileRef}
          type="file"
          accept=".csv,text/csv"
          style={{ display: "none" }}
          onChange={handleFileChange}
        />
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Upload}
          onClick={() => fileRef.current?.click()}
        >
          <FormattedMessage
            id="notebook.manifest.selectFile"
            defaultMessage="Select CSV File"
          />
        </Button>
        {file && (
          <Tag type="blue" style={{ marginLeft: "0.5rem" }}>
            {file.name}
          </Tag>
        )}
      </div>

      {/* ── Step 2: column mapping ── */}
      {csvHeaders.length > 0 && (
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "0.75rem", fontWeight: 600 }}>
            <FormattedMessage
              id="notebook.manifest.step2"
              defaultMessage="Step 2 — Map CSV columns to fields"
            />
          </p>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: "0.75rem 2rem",
            }}
          >
            {manifestColumns.map((col) => (
              <div key={col.field}>
                <Select
                  id={`col-map-${col.field}`}
                  labelText={
                    <>
                      {col.label ?? col.field}
                      {col.required && <span style={{ color: "red" }}> *</span>}
                    </>
                  }
                  value={columnMapping[col.field] ?? ""}
                  onChange={(e) =>
                    setColumnMapping((prev) => ({
                      ...prev,
                      [col.field]: e.target.value || undefined,
                    }))
                  }
                >
                  <SelectItem value="" text="— select column —" />
                  {csvHeaders.map((h) => (
                    <SelectItem key={h} value={h} text={h} />
                  ))}
                </Select>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Step 3: preview ── */}
      {preview && (
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "0.5rem", fontWeight: 600 }}>
            <FormattedMessage
              id="notebook.manifest.preview"
              defaultMessage="Preview (first {n} rows)"
              values={{ n: preview.rows.length }}
            />
          </p>
          <div style={{ overflowX: "auto" }}>
            <DataTable
              rows={preview.rows.map((r, i) => ({
                id: String(i),
                ...Object.fromEntries(
                  preview.headers.map((h, j) => [h, r[j] ?? ""]),
                ),
              }))}
              headers={preview.headers.map((h) => ({ key: h, header: h }))}
              size="sm"
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        {headers.map((h) => (
                          <TableHeader
                            {...getHeaderProps({ header: h })}
                            key={h.key}
                          >
                            {h.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow {...getRowProps({ row })} key={row.id}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
          </div>
        </div>
      )}

      {/* ── notifications ── */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton
          lowContrast
        />
      )}
    </Modal>
  );
}

export default GenericManifestImportModal;
