import React, { useState, useContext } from "react";
import {
  Modal,
  FileUploader,
  ProgressBar,
  InlineLoading,
  Select,
  SelectItem,
  Toggle,
  TextArea,
  FormGroup,
  Button,
  Tag,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import {
  CloudUpload,
  DocumentImport,
  CheckmarkFilled,
  WarningFilled,
  ErrorFilled,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

import { postToOpenElisServer, postWithFileUpload } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";

/**
 * ComplianceStandardImportModal - Advanced CSV import modal with validation and preview
 *
 * Features:
 * - File upload with validation
 * - CSV preview with error detection
 * - Import options configuration
 * - Progress tracking
 * - Error reporting with line-by-line details
 */
const ComplianceStandardImportModal = ({
  open,
  onRequestClose,
  onImportComplete,
}) => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [currentStep, setCurrentStep] = useState(1); // 1: Upload, 2: Preview, 3: Import, 4: Results
  const [selectedFile, setSelectedFile] = useState(null);
  const [importOptions, setImportOptions] = useState({
    updateExisting: true,
    skipErrors: false,
    validateOnly: false,
    notifyOnCompletion: true,
  });
  const [importProgress, setImportProgress] = useState(0);
  const [isImporting, setIsImporting] = useState(false);
  const [previewData, setPreviewData] = useState([]);
  const [validationErrors, setValidationErrors] = useState([]);
  const [importResults, setImportResults] = useState(null);

  const handleFileUpload = (event) => {
    const file = event.target.files[0];
    if (file) {
      if (!file.name.endsWith(".csv")) {
        addNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "import.file.error.title",
            defaultMessage: "Invalid File Type",
          }),
          message: intl.formatMessage({
            id: "import.file.error.message",
            defaultMessage: "Please select a CSV file",
          }),
        });
        return;
      }

      setSelectedFile(file);
      previewFile(file);
    }
  };

  const previewFile = async (file) => {
    setCurrentStep(2);
    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("validateOnly", "true");

      const response = await postWithFileUpload(
        "/rest/compliance-standards/import/preview",
        formData,
      );

      if (response.success) {
        setPreviewData(response.previewData || []);
        setValidationErrors(response.validationErrors || []);
      } else {
        throw new Error(response.message || "Preview failed");
      }
    } catch (error) {
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "import.preview.error.title",
          defaultMessage: "Preview Error",
        }),
        message: error.message || "Failed to preview file",
      });
      setCurrentStep(1);
    }
  };

  const startImport = async () => {
    if (!selectedFile) return;

    setCurrentStep(3);
    setIsImporting(true);
    setImportProgress(0);

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);
      formData.append(
        "updateExisting",
        importOptions.updateExisting.toString(),
      );
      formData.append("skipErrors", importOptions.skipErrors.toString());
      formData.append("validateOnly", importOptions.validateOnly.toString());

      // Simulate progress updates
      const progressInterval = setInterval(() => {
        setImportProgress((prev) => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + 10;
        });
      }, 500);

      const response = await postWithFileUpload(
        "/rest/compliance-standards/import",
        formData,
      );

      clearInterval(progressInterval);
      setImportProgress(100);

      if (response.success) {
        setImportResults(response.results);
        setCurrentStep(4);

        if (onImportComplete) {
          onImportComplete(response.results);
        }

        addNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "import.success.title",
            defaultMessage: "Import Completed",
          }),
          message: intl.formatMessage({
            id: "import.success.message",
            defaultMessage: "Compliance standards imported successfully",
          }),
        });
      } else {
        throw new Error(response.message || "Import failed");
      }
    } catch (error) {
      setImportProgress(0);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "import.error.title",
          defaultMessage: "Import Error",
        }),
        message: error.message || "Failed to import compliance standards",
      });
    } finally {
      setIsImporting(false);
    }
  };

  const resetModal = () => {
    setCurrentStep(1);
    setSelectedFile(null);
    setPreviewData([]);
    setValidationErrors([]);
    setImportResults(null);
    setImportProgress(0);
    setIsImporting(false);
  };

  const handleClose = () => {
    resetModal();
    onRequestClose();
  };

  const renderUploadStep = () => (
    <div className="import-modal__upload">
      <FileUploader
        labelTitle={intl.formatMessage({
          id: "import.upload.label",
          defaultMessage: "Upload CSV File",
        })}
        labelDescription={intl.formatMessage({
          id: "import.upload.description",
          defaultMessage:
            "Select a CSV file containing compliance standards data",
        })}
        buttonLabel={intl.formatMessage({
          id: "import.upload.button",
          defaultMessage: "Add file",
        })}
        filenameStatus="edit"
        accept={[".csv"]}
        multiple={false}
        disabled={false}
        iconDescription={intl.formatMessage({
          id: "import.upload.icon",
          defaultMessage: "Delete file",
        })}
        name="complianceStandardsFile"
        onChange={handleFileUpload}
      />

      <div className="import-modal__format-info">
        <h4>
          <FormattedMessage
            id="import.format.title"
            defaultMessage="Required CSV Format"
          />
        </h4>
        <p>
          <FormattedMessage
            id="import.format.description"
            defaultMessage="Your CSV file must include the following columns:"
          />
        </p>
        <ul>
          <li>name (required)</li>
          <li>regulationNumber (required)</li>
          <li>issuingBody (required)</li>
          <li>version</li>
          <li>effectiveDate (YYYY-MM-DD format)</li>
          <li>status (ACTIVE, DRAFT, SUPERSEDED, ARCHIVED)</li>
          <li>description</li>
          <li>localization:en, localization:id (for translations)</li>
        </ul>
      </div>
    </div>
  );

  const renderPreviewStep = () => (
    <div className="import-modal__preview">
      <h4>
        <FormattedMessage
          id="import.preview.title"
          defaultMessage="Import Preview"
        />
      </h4>

      {validationErrors.length > 0 && (
        <div className="import-modal__errors">
          <Tag type="red" renderIcon={ErrorFilled}>
            <FormattedMessage
              id="import.preview.errors"
              defaultMessage="{count} validation errors found"
              values={{ count: validationErrors.length }}
            />
          </Tag>
          {validationErrors.slice(0, 5).map((error, index) => (
            <p key={index} className="import-modal__error-detail">
              Line {error.line}: {error.message}
            </p>
          ))}
          {validationErrors.length > 5 && (
            <p className="import-modal__error-more">
              <FormattedMessage
                id="import.preview.more-errors"
                defaultMessage="...and {count} more errors"
                values={{ count: validationErrors.length - 5 }}
              />
            </p>
          )}
        </div>
      )}

      <DataTable
        rows={previewData.slice(0, 10)}
        headers={[
          { key: "line", header: "Line" },
          { key: "name", header: "Name" },
          { key: "regulationNumber", header: "Regulation Number" },
          { key: "issuingBody", header: "Issuing Body" },
          { key: "status", header: "Status" },
          { key: "validation", header: "Validation" },
        ]}
        render={({
          rows,
          headers,
          getHeaderProps,
          getRowProps,
          getTableProps,
        }) => (
          <TableContainer
            title={intl.formatMessage({
              id: "import.preview.table.title",
              defaultMessage: "Preview Data (first 10 rows)",
            })}
          >
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {headers.map((header) => (
                    <TableHeader
                      {...getHeaderProps({ header })}
                      key={header.key}
                    >
                      {header.header}
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
      />

      <FormGroup className="import-modal__options">
        <Toggle
          id="update-existing"
          labelText={intl.formatMessage({
            id: "import.option.updateExisting",
            defaultMessage: "Update existing standards",
          })}
          toggled={importOptions.updateExisting}
          onToggle={(checked) =>
            setImportOptions((prev) => ({ ...prev, updateExisting: checked }))
          }
        />

        <Toggle
          id="skip-errors"
          labelText={intl.formatMessage({
            id: "import.option.skipErrors",
            defaultMessage: "Skip rows with errors",
          })}
          toggled={importOptions.skipErrors}
          onToggle={(checked) =>
            setImportOptions((prev) => ({ ...prev, skipErrors: checked }))
          }
        />
      </FormGroup>
    </div>
  );

  const renderImportStep = () => (
    <div className="import-modal__import">
      <InlineLoading
        status="active"
        description={intl.formatMessage({
          id: "import.progress.description",
          defaultMessage: "Importing compliance standards...",
        })}
      />
      <ProgressBar
        value={importProgress}
        max={100}
        labelText={intl.formatMessage({
          id: "import.progress.label",
          defaultMessage: "Import Progress",
        })}
      />
    </div>
  );

  const renderResultsStep = () => (
    <div className="import-modal__results">
      <h4>
        <FormattedMessage
          id="import.results.title"
          defaultMessage="Import Results"
        />
      </h4>

      {importResults && (
        <div className="import-modal__results-summary">
          <Tag type="green" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="import.results.success"
              defaultMessage="{count} standards imported"
              values={{ count: importResults.successCount }}
            />
          </Tag>

          {importResults.errorCount > 0 && (
            <Tag type="red" renderIcon={WarningFilled}>
              <FormattedMessage
                id="import.results.errors"
                defaultMessage="{count} errors"
                values={{ count: importResults.errorCount }}
              />
            </Tag>
          )}

          {importResults.warningCount > 0 && (
            <Tag type="yellow" renderIcon={WarningFilled}>
              <FormattedMessage
                id="import.results.warnings"
                defaultMessage="{count} warnings"
                values={{ count: importResults.warningCount }}
              />
            </Tag>
          )}
        </div>
      )}
    </div>
  );

  const getModalProps = () => {
    switch (currentStep) {
      case 1:
        return {
          primaryButtonText: null,
          secondaryButtonText: intl.formatMessage({
            id: "button.cancel",
            defaultMessage: "Cancel",
          }),
          primaryButtonDisabled: true,
        };
      case 2:
        return {
          primaryButtonText: intl.formatMessage({
            id: "button.import",
            defaultMessage: "Import",
          }),
          secondaryButtonText: intl.formatMessage({
            id: "button.back",
            defaultMessage: "Back",
          }),
          primaryButtonDisabled:
            validationErrors.length > 0 && !importOptions.skipErrors,
          onRequestSubmit: startImport,
          onSecondarySubmit: () => setCurrentStep(1),
        };
      case 3:
        return {
          primaryButtonText: null,
          secondaryButtonText: null,
          primaryButtonDisabled: true,
        };
      case 4:
        return {
          primaryButtonText: intl.formatMessage({
            id: "button.done",
            defaultMessage: "Done",
          }),
          secondaryButtonText: null,
          onRequestSubmit: handleClose,
        };
      default:
        return {};
    }
  };

  const modalProps = getModalProps();

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "import.modal.title",
        defaultMessage: "Import Compliance Standards",
      })}
      modalLabel={intl.formatMessage({
        id: "import.modal.label",
        defaultMessage: "CSV Import",
      })}
      size="lg"
      preventCloseOnClickOutside={isImporting}
      {...modalProps}
    >
      <div className="compliance-import-modal">
        {currentStep === 1 && renderUploadStep()}
        {currentStep === 2 && renderPreviewStep()}
        {currentStep === 3 && renderImportStep()}
        {currentStep === 4 && renderResultsStep()}
      </div>
    </Modal>
  );
};

export default ComplianceStandardImportModal;
