import React, { useState, useContext, useRef } from "react";
import {
  Grid,
  Column,
  Button,
  FileUploader,
  ProgressIndicator,
  ProgressStep,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  InlineNotification,
  InlineLoading,
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Toggle,
  Accordion,
  AccordionItem,
  Tag,
  Link,
  CodeSnippet,
  SkeletonText,
} from "@carbon/react";
import {
  Upload,
  CloudUpload,
  CheckmarkFilled,
  ErrorFilled,
  WarningFilled,
  Download,
  DocumentImport,
  Reset,
  View,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  ConfigurationContext,
  NotificationContext,
} from "../layout/Layout";
import { postToOpenElisServer, getFromOpenElisServer } from "../utils/Utils";
import "./StandardImportWizard.css";

const StandardImportWizard = ({ onComplete = () => {} }) => {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const { addNotification } = useContext(NotificationContext);
  const fileUploaderRef = useRef(null);

  // Feature flag check
  const isComplianceModuleEnabled =
    configurationProperties?.["compliance.module.enabled"] === "true";

  // Wizard state
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [isUploading, setIsUploading] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  // Import data
  const [importPreview, setImportPreview] = useState(null);
  const [importResults, setImportResults] = useState(null);
  const [validationErrors, setValidationErrors] = useState([]);

  // Import settings
  const [importSettings, setImportSettings] = useState({
    skipDuplicates: true,
    updateExisting: false,
    validateThresholds: true,
    createMissingParameters: false,
    organization: "",
    description: "",
  });

  // Confirmation modal
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);

  // Feature flag guard
  if (!isComplianceModuleEnabled) {
    return (
      <Grid className="standard-import-wizard__disabled">
        <Column lg={16} md={8} sm={4}>
          <div className="standard-import-wizard__message">
            <h3>
              <FormattedMessage
                id="compliance.module.disabled.title"
                defaultMessage="Compliance Module Disabled"
              />
            </h3>
            <p>
              <FormattedMessage
                id="compliance.module.disabled.description"
                defaultMessage="The compliance module is currently disabled. Please contact your system administrator to enable this feature."
              />
            </p>
          </div>
        </Column>
      </Grid>
    );
  }

  // Wizard steps configuration
  const wizardSteps = [
    {
      label: intl.formatMessage({
        id: "compliance.import.wizard.step.upload.title",
        defaultMessage: "Upload File",
      }),
      description: intl.formatMessage({
        id: "compliance.import.wizard.step.upload.description",
        defaultMessage: "Select and upload your CSV file",
      }),
    },
    {
      label: intl.formatMessage({
        id: "compliance.import.wizard.step.preview.title",
        defaultMessage: "Preview & Validate",
      }),
      description: intl.formatMessage({
        id: "compliance.import.wizard.step.preview.description",
        defaultMessage: "Review data and validation results",
      }),
    },
    {
      label: intl.formatMessage({
        id: "compliance.import.wizard.step.settings.title",
        defaultMessage: "Import Settings",
      }),
      description: intl.formatMessage({
        id: "compliance.import.wizard.step.settings.description",
        defaultMessage: "Configure import options",
      }),
    },
    {
      label: intl.formatMessage({
        id: "compliance.import.wizard.step.import.title",
        defaultMessage: "Import",
      }),
      description: intl.formatMessage({
        id: "compliance.import.wizard.step.import.description",
        defaultMessage: "Process and import data",
      }),
    },
  ];

  // Handle file selection
  const handleFileSelect = (event) => {
    const files = event.target.files;
    if (files && files.length > 0) {
      const file = files[0];

      // Validate file type
      if (!file.name.toLowerCase().endsWith(".csv")) {
        addNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "compliance.import.file.error.invalidType.title",
            defaultMessage: "Invalid File Type",
          }),
          message: intl.formatMessage({
            id: "compliance.import.file.error.invalidType.message",
            defaultMessage: "Please select a CSV file.",
          }),
        });
        return;
      }

      // Validate file size (max 10MB)
      if (file.size > 10 * 1024 * 1024) {
        addNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "compliance.import.file.error.tooLarge.title",
            defaultMessage: "File Too Large",
          }),
          message: intl.formatMessage({
            id: "compliance.import.file.error.tooLarge.message",
            defaultMessage: "File size cannot exceed 10MB.",
          }),
        });
        return;
      }

      setSelectedFile(file);
    }
  };

  // Upload and validate file
  const handleUploadFile = async () => {
    if (!selectedFile) return;

    try {
      setIsUploading(true);
      setUploadProgress(0);

      const formData = new FormData();
      formData.append("file", selectedFile);
      formData.append("validateOnly", "true");

      // Simulate upload progress
      const progressInterval = setInterval(() => {
        setUploadProgress((prev) => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + 10;
        });
      }, 200);

      const response = await postToOpenElisServer(
        "/rest/compliance/import/upload",
        formData,
        "POST",
        { "Content-Type": "multipart/form-data" },
      );

      clearInterval(progressInterval);
      setUploadProgress(100);

      if (response) {
        setImportPreview(response);
        setValidationErrors(response.validationErrors || []);

        setTimeout(() => {
          setCurrentStep(1);
        }, 500);
      }
    } catch (error) {
      console.error("Error uploading file:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "compliance.import.upload.error.title",
          defaultMessage: "Upload Failed",
        }),
        message: intl.formatMessage({
          id: "compliance.import.upload.error.message",
          defaultMessage:
            "Unable to upload and validate file. Please try again.",
        }),
      });
    } finally {
      setIsUploading(false);
    }
  };

  // Execute import
  const handleExecuteImport = async () => {
    try {
      setIsProcessing(true);

      const payload = {
        fileId: importPreview.fileId,
        settings: importSettings,
      };

      const response = await postToOpenElisServer(
        "/rest/compliance/import/execute",
        JSON.stringify(payload),
        "POST",
      );

      if (response) {
        setImportResults(response);
        setCurrentStep(3);

        addNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "compliance.import.success.title",
            defaultMessage: "Import Completed",
          }),
          message: intl.formatMessage({
            id: "compliance.import.success.message",
            defaultMessage:
              "Standards and thresholds have been imported successfully.",
          }),
        });
      }
    } catch (error) {
      console.error("Error executing import:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "compliance.import.execute.error.title",
          defaultMessage: "Import Failed",
        }),
        message: intl.formatMessage({
          id: "compliance.import.execute.error.message",
          defaultMessage:
            "Unable to complete import. Please check the data and try again.",
        }),
      });
    } finally {
      setIsProcessing(false);
    }
  };

  // Handle wizard navigation
  const handleNext = () => {
    if (currentStep === 0) {
      handleUploadFile();
    } else if (currentStep === 1) {
      setCurrentStep(2);
    } else if (currentStep === 2) {
      setIsConfirmModalOpen(true);
    }
  };

  const handlePrevious = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleReset = () => {
    setCurrentStep(0);
    setSelectedFile(null);
    setImportPreview(null);
    setImportResults(null);
    setValidationErrors([]);
    setUploadProgress(0);
    setIsUploading(false);
    setIsProcessing(false);
    if (fileUploaderRef.current) {
      fileUploaderRef.current.clearFiles();
    }
  };

  // Download template
  const handleDownloadTemplate = async () => {
    try {
      const response = await getFromOpenElisServer(
        "/rest/compliance/import/template",
        { responseType: "blob" },
      );

      // Create download link
      const url = window.URL.createObjectURL(new Blob([response]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "compliance-standards-template.csv");
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Error downloading template:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "compliance.import.template.error.title",
          defaultMessage: "Download Failed",
        }),
        message: intl.formatMessage({
          id: "compliance.import.template.error.message",
          defaultMessage: "Unable to download template file. Please try again.",
        }),
      });
    }
  };

  // Get validation error severity
  const getErrorSeverity = (error) => {
    const severityMap = {
      ERROR: { type: "red", icon: ErrorFilled },
      WARNING: { type: "yellow", icon: WarningFilled },
      INFO: { type: "blue", icon: CheckmarkFilled },
    };
    return severityMap[error.severity] || severityMap.INFO;
  };

  // Get step status
  const getStepStatus = (index) => {
    if (index < currentStep) return "complete";
    if (index === currentStep) return "current";
    return "incomplete";
  };

  // Check if next button should be enabled
  const isNextEnabled = () => {
    if (currentStep === 0) return selectedFile !== null && !isUploading;
    if (currentStep === 1)
      return (
        validationErrors.filter((e) => e.severity === "ERROR").length === 0
      );
    if (currentStep === 2) return true;
    return false;
  };

  return (
    <Grid className="standard-import-wizard">
      <Column lg={16}>
        {/* Header */}
        <div className="standard-import-wizard__header">
          <h1>
            <FormattedMessage
              id="compliance.import.wizard.title"
              defaultMessage="Import Compliance Standards"
            />
          </h1>
          <p>
            <FormattedMessage
              id="compliance.import.wizard.description"
              defaultMessage="Import compliance standards and thresholds from CSV files with validation and preview"
            />
          </p>
        </div>

        {/* Progress Indicator */}
        <div className="standard-import-wizard__progress">
          <ProgressIndicator currentIndex={currentStep} spaceEqually>
            {wizardSteps.map((step, index) => (
              <ProgressStep
                key={index}
                label={step.label}
                description={step.description}
                complete={getStepStatus(index) === "complete"}
                current={getStepStatus(index) === "current"}
                invalid={
                  index === 1 &&
                  validationErrors.length > 0 &&
                  validationErrors.some((e) => e.severity === "ERROR")
                }
              />
            ))}
          </ProgressIndicator>
        </div>

        {/* Step Content */}
        <div className="standard-import-wizard__content">
          {/* Step 0: File Upload */}
          {currentStep === 0 && (
            <div className="standard-import-wizard__step">
              <h2>
                <FormattedMessage
                  id="compliance.import.wizard.step.upload.heading"
                  defaultMessage="Select CSV File"
                />
              </h2>

              {/* Template Download */}
              <div className="standard-import-wizard__template">
                <InlineNotification
                  kind="info"
                  title={intl.formatMessage({
                    id: "compliance.import.template.info.title",
                    defaultMessage: "Need a template?",
                  })}
                  subtitle={intl.formatMessage({
                    id: "compliance.import.template.info.message",
                    defaultMessage:
                      "Download our CSV template to ensure your data is properly formatted.",
                  })}
                  hideCloseButton
                  actions={[
                    {
                      label: intl.formatMessage({
                        id: "compliance.import.template.download",
                        defaultMessage: "Download Template",
                      }),
                      onClick: handleDownloadTemplate,
                    },
                  ]}
                />
              </div>

              {/* File Uploader */}
              <div className="standard-import-wizard__uploader">
                <FileUploader
                  ref={fileUploaderRef}
                  accept=".csv"
                  buttonLabel={intl.formatMessage({
                    id: "compliance.import.file.select",
                    defaultMessage: "Select CSV file",
                  })}
                  filenameStatus="edit"
                  iconDescription={intl.formatMessage({
                    id: "compliance.import.file.clear",
                    defaultMessage: "Clear file",
                  })}
                  labelDescription={intl.formatMessage({
                    id: "compliance.import.file.description",
                    defaultMessage:
                      "Only CSV files are supported. Maximum file size is 10MB.",
                  })}
                  labelTitle={intl.formatMessage({
                    id: "compliance.import.file.label",
                    defaultMessage: "Upload",
                  })}
                  multiple={false}
                  name="importFile"
                  onChange={handleFileSelect}
                  size="md"
                  buttonKind="secondary"
                />
              </div>

              {/* Upload Progress */}
              {isUploading && (
                <div className="standard-import-wizard__upload-progress">
                  <InlineLoading
                    description={intl.formatMessage(
                      {
                        id: "compliance.import.uploading",
                        defaultMessage:
                          "Uploading and validating file... {progress}%",
                      },
                      { progress: uploadProgress },
                    )}
                  />
                </div>
              )}

              {/* CSV Format Guidelines */}
              <Accordion>
                <AccordionItem
                  title={intl.formatMessage({
                    id: "compliance.import.format.guidelines.title",
                    defaultMessage: "CSV Format Guidelines",
                  })}
                >
                  <div className="standard-import-wizard__guidelines">
                    <h4>
                      <FormattedMessage
                        id="compliance.import.format.required.columns"
                        defaultMessage="Required Columns"
                      />
                    </h4>
                    <ul>
                      <li>standard_name: Name of the compliance standard</li>
                      <li>
                        organization_name: Organization that issued the standard
                      </li>
                      <li>parameter_name: Name of the testing parameter</li>
                      <li>
                        threshold_type: Type of threshold (MAXIMUM, MINIMUM,
                        RANGE, EXACT)
                      </li>
                      <li>unit: Unit of measurement</li>
                    </ul>

                    <h4>
                      <FormattedMessage
                        id="compliance.import.format.optional.columns"
                        defaultMessage="Optional Columns"
                      />
                    </h4>
                    <ul>
                      <li>description: Description of the standard</li>
                      <li>
                        effective_date: When the standard becomes effective
                      </li>
                      <li>
                        min_value: Minimum threshold value (for MINIMUM, RANGE)
                      </li>
                      <li>
                        max_value: Maximum threshold value (for MAXIMUM, RANGE)
                      </li>
                      <li>exact_value: Exact threshold value (for EXACT)</li>
                      <li>tolerance: Acceptable tolerance percentage</li>
                      <li>
                        criticality_level: Impact level (LOW, MEDIUM, HIGH,
                        CRITICAL)
                      </li>
                    </ul>

                    <CodeSnippet
                      type="multi"
                      feedback={intl.formatMessage({
                        id: "compliance.import.format.example.copied",
                        defaultMessage: "Example copied to clipboard",
                      })}
                    >
                      {`standard_name,organization_name,parameter_name,threshold_type,unit,max_value,criticality_level
"WHO Guidelines","World Health Organization","pH","RANGE","pH Units","8.5","MEDIUM"
"EPA Standards","US Environmental Protection Agency","Turbidity","MAXIMUM","NTU","4","HIGH"`}
                    </CodeSnippet>
                  </div>
                </AccordionItem>
              </Accordion>
            </div>
          )}

          {/* Step 1: Preview & Validation */}
          {currentStep === 1 && importPreview && (
            <div className="standard-import-wizard__step">
              <h2>
                <FormattedMessage
                  id="compliance.import.wizard.step.preview.heading"
                  defaultMessage="Preview & Validation Results"
                />
              </h2>

              {/* Summary */}
              <div className="standard-import-wizard__summary">
                <Grid>
                  <Column lg={4} md={2} sm={2}>
                    <div className="standard-import-wizard__stat">
                      <span className="standard-import-wizard__stat-value">
                        {importPreview.totalRecords || 0}
                      </span>
                      <span className="standard-import-wizard__stat-label">
                        <FormattedMessage
                          id="compliance.import.preview.total.records"
                          defaultMessage="Total Records"
                        />
                      </span>
                    </div>
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <div className="standard-import-wizard__stat">
                      <span className="standard-import-wizard__stat-value">
                        {importPreview.validRecords || 0}
                      </span>
                      <span className="standard-import-wizard__stat-label">
                        <FormattedMessage
                          id="compliance.import.preview.valid.records"
                          defaultMessage="Valid Records"
                        />
                      </span>
                    </div>
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <div className="standard-import-wizard__stat">
                      <span className="standard-import-wizard__stat-value">
                        {
                          validationErrors.filter((e) => e.severity === "ERROR")
                            .length
                        }
                      </span>
                      <span className="standard-import-wizard__stat-label">
                        <FormattedMessage
                          id="compliance.import.preview.errors"
                          defaultMessage="Errors"
                        />
                      </span>
                    </div>
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <div className="standard-import-wizard__stat">
                      <span className="standard-import-wizard__stat-value">
                        {
                          validationErrors.filter(
                            (e) => e.severity === "WARNING",
                          ).length
                        }
                      </span>
                      <span className="standard-import-wizard__stat-label">
                        <FormattedMessage
                          id="compliance.import.preview.warnings"
                          defaultMessage="Warnings"
                        />
                      </span>
                    </div>
                  </Column>
                </Grid>
              </div>

              {/* Validation Errors */}
              {validationErrors.length > 0 && (
                <div className="standard-import-wizard__validation">
                  <h3>
                    <FormattedMessage
                      id="compliance.import.validation.results.title"
                      defaultMessage="Validation Results"
                    />
                  </h3>
                  <DataTable
                    rows={validationErrors.map((error, index) => ({
                      id: index,
                      row: error.rowNumber || "N/A",
                      field: error.fieldName || "N/A",
                      severity: error.severity,
                      message: error.message,
                    }))}
                    headers={[
                      {
                        key: "row",
                        header: intl.formatMessage({
                          id: "compliance.import.validation.row",
                          defaultMessage: "Row",
                        }),
                      },
                      {
                        key: "field",
                        header: intl.formatMessage({
                          id: "compliance.import.validation.field",
                          defaultMessage: "Field",
                        }),
                      },
                      {
                        key: "severity",
                        header: intl.formatMessage({
                          id: "compliance.import.validation.severity",
                          defaultMessage: "Severity",
                        }),
                      },
                      {
                        key: "message",
                        header: intl.formatMessage({
                          id: "compliance.import.validation.message",
                          defaultMessage: "Message",
                        }),
                      },
                    ]}
                    render={({
                      rows,
                      headers,
                      getHeaderProps,
                      getRowProps,
                      getTableProps,
                      getTableContainerProps,
                    }) => (
                      <TableContainer {...getTableContainerProps()}>
                        <Table {...getTableProps()}>
                          <TableHead>
                            <TableRow>
                              {headers.map((header) => (
                                <TableHeader
                                  key={header.key}
                                  {...getHeaderProps({ header })}
                                >
                                  {header.header}
                                </TableHeader>
                              ))}
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {rows.map((row) => (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                {row.cells.map((cell) => {
                                  if (cell.info.header === "severity") {
                                    const { type, icon: IconComponent } =
                                      getErrorSeverity(cell.value);
                                    return (
                                      <TableCell key={cell.id}>
                                        <Tag
                                          type={type}
                                          size="sm"
                                          renderIcon={IconComponent}
                                        >
                                          {cell.value}
                                        </Tag>
                                      </TableCell>
                                    );
                                  } else {
                                    return (
                                      <TableCell key={cell.id}>
                                        {cell.value}
                                      </TableCell>
                                    );
                                  }
                                })}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  />
                </div>
              )}

              {/* Preview Data */}
              <div className="standard-import-wizard__preview-data">
                <div className="standard-import-wizard__preview-header">
                  <h3>
                    <FormattedMessage
                      id="compliance.import.preview.data.title"
                      defaultMessage="Data Preview"
                    />
                  </h3>
                  <Button
                    kind="secondary"
                    size="sm"
                    renderIcon={View}
                    onClick={() => setIsPreviewModalOpen(true)}
                  >
                    <FormattedMessage
                      id="compliance.import.preview.data.view.all"
                      defaultMessage="View All"
                    />
                  </Button>
                </div>

                {importPreview.previewData &&
                  importPreview.previewData.length > 0 && (
                    <div className="standard-import-wizard__preview-table">
                      <SkeletonText />
                      <p className="standard-import-wizard__preview-note">
                        <FormattedMessage
                          id="compliance.import.preview.data.note"
                          defaultMessage="Showing first 5 records. Click 'View All' to see complete data."
                        />
                      </p>
                    </div>
                  )}
              </div>
            </div>
          )}

          {/* Step 2: Import Settings */}
          {currentStep === 2 && (
            <div className="standard-import-wizard__step">
              <h2>
                <FormattedMessage
                  id="compliance.import.wizard.step.settings.heading"
                  defaultMessage="Configure Import Settings"
                />
              </h2>

              <Grid>
                <Column lg={8} md={4} sm={2}>
                  <TextInput
                    id="organization"
                    labelText={intl.formatMessage({
                      id: "compliance.import.settings.organization.label",
                      defaultMessage: "Default Organization",
                    })}
                    placeholder={intl.formatMessage({
                      id: "compliance.import.settings.organization.placeholder",
                      defaultMessage:
                        "Enter organization name for standards without one",
                    })}
                    value={importSettings.organization}
                    onChange={(e) =>
                      setImportSettings((prev) => ({
                        ...prev,
                        organization: e.target.value,
                      }))
                    }
                    helperText={intl.formatMessage({
                      id: "compliance.import.settings.organization.help",
                      defaultMessage:
                        "Used when organization_name is not specified in CSV",
                    })}
                  />
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="description"
                    labelText={intl.formatMessage({
                      id: "compliance.import.settings.description.label",
                      defaultMessage: "Import Description",
                    })}
                    placeholder={intl.formatMessage({
                      id: "compliance.import.settings.description.placeholder",
                      defaultMessage: "Describe this import batch",
                    })}
                    value={importSettings.description}
                    onChange={(e) =>
                      setImportSettings((prev) => ({
                        ...prev,
                        description: e.target.value,
                      }))
                    }
                    rows={3}
                  />
                </Column>
              </Grid>

              <div className="standard-import-wizard__settings">
                <h3>
                  <FormattedMessage
                    id="compliance.import.settings.options.title"
                    defaultMessage="Import Options"
                  />
                </h3>

                <div className="standard-import-wizard__settings-group">
                  <Toggle
                    id="skip-duplicates"
                    labelText={intl.formatMessage({
                      id: "compliance.import.settings.skipDuplicates.label",
                      defaultMessage: "Skip Duplicate Records",
                    })}
                    toggled={importSettings.skipDuplicates}
                    onToggle={(toggled) =>
                      setImportSettings((prev) => ({
                        ...prev,
                        skipDuplicates: toggled,
                      }))
                    }
                  />
                  <p className="standard-import-wizard__setting-description">
                    <FormattedMessage
                      id="compliance.import.settings.skipDuplicates.description"
                      defaultMessage="Skip records that already exist in the system"
                    />
                  </p>
                </div>

                <div className="standard-import-wizard__settings-group">
                  <Toggle
                    id="update-existing"
                    labelText={intl.formatMessage({
                      id: "compliance.import.settings.updateExisting.label",
                      defaultMessage: "Update Existing Records",
                    })}
                    toggled={importSettings.updateExisting}
                    onToggle={(toggled) =>
                      setImportSettings((prev) => ({
                        ...prev,
                        updateExisting: toggled,
                      }))
                    }
                  />
                  <p className="standard-import-wizard__setting-description">
                    <FormattedMessage
                      id="compliance.import.settings.updateExisting.description"
                      defaultMessage="Update existing standards and thresholds with new values"
                    />
                  </p>
                </div>

                <div className="standard-import-wizard__settings-group">
                  <Toggle
                    id="validate-thresholds"
                    labelText={intl.formatMessage({
                      id: "compliance.import.settings.validateThresholds.label",
                      defaultMessage: "Validate Threshold Logic",
                    })}
                    toggled={importSettings.validateThresholds}
                    onToggle={(toggled) =>
                      setImportSettings((prev) => ({
                        ...prev,
                        validateThresholds: toggled,
                      }))
                    }
                  />
                  <p className="standard-import-wizard__setting-description">
                    <FormattedMessage
                      id="compliance.import.settings.validateThresholds.description"
                      defaultMessage="Ensure threshold values are logically consistent"
                    />
                  </p>
                </div>

                <div className="standard-import-wizard__settings-group">
                  <Toggle
                    id="create-missing-parameters"
                    labelText={intl.formatMessage({
                      id: "compliance.import.settings.createMissingParameters.label",
                      defaultMessage: "Create Missing Parameters",
                    })}
                    toggled={importSettings.createMissingParameters}
                    onToggle={(toggled) =>
                      setImportSettings((prev) => ({
                        ...prev,
                        createMissingParameters: toggled,
                      }))
                    }
                  />
                  <p className="standard-import-wizard__setting-description">
                    <FormattedMessage
                      id="compliance.import.settings.createMissingParameters.description"
                      defaultMessage="Automatically create parameter groups for unknown parameters"
                    />
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Step 3: Import Results */}
          {currentStep === 3 && importResults && (
            <div className="standard-import-wizard__step">
              <h2>
                <FormattedMessage
                  id="compliance.import.wizard.step.results.heading"
                  defaultMessage="Import Completed"
                />
              </h2>

              <div className="standard-import-wizard__results">
                <InlineNotification
                  kind="success"
                  title={intl.formatMessage({
                    id: "compliance.import.results.success.title",
                    defaultMessage: "Import Successful",
                  })}
                  subtitle={intl.formatMessage({
                    id: "compliance.import.results.success.message",
                    defaultMessage:
                      "Your compliance standards have been imported successfully.",
                  })}
                  hideCloseButton
                />

                <Grid className="standard-import-wizard__results-summary">
                  <Column lg={3} md={2} sm={2}>
                    <div className="standard-import-wizard__stat">
                      <span className="standard-import-wizard__stat-value">
                        {importResults.standardsCreated || 0}
                      </span>
                      <span className="standard-import-wizard__stat-label">
                        <FormattedMessage
                          id="compliance.import.results.standards.created"
                          defaultMessage="Standards Created"
                        />
                      </span>
                    </div>
                  </Column>
                  <Column lg={3} md={2} sm={2}>
                    <div className="standard-import-wizard__stat">
                      <span className="standard-import-wizard__stat-value">
                        {importResults.thresholdsCreated || 0}
                      </span>
                      <span className="standard-import-wizard__stat-label">
                        <FormattedMessage
                          id="compliance.import.results.thresholds.created"
                          defaultMessage="Thresholds Created"
                        />
                      </span>
                    </div>
                  </Column>
                  <Column lg={3} md={2} sm={2}>
                    <div className="standard-import-wizard__stat">
                      <span className="standard-import-wizard__stat-value">
                        {importResults.recordsUpdated || 0}
                      </span>
                      <span className="standard-import-wizard__stat-label">
                        <FormattedMessage
                          id="compliance.import.results.records.updated"
                          defaultMessage="Records Updated"
                        />
                      </span>
                    </div>
                  </Column>
                  <Column lg={3} md={2} sm={2}>
                    <div className="standard-import-wizard__stat">
                      <span className="standard-import-wizard__stat-value">
                        {importResults.recordsSkipped || 0}
                      </span>
                      <span className="standard-import-wizard__stat-label">
                        <FormattedMessage
                          id="compliance.import.results.records.skipped"
                          defaultMessage="Records Skipped"
                        />
                      </span>
                    </div>
                  </Column>
                </Grid>
              </div>
            </div>
          )}
        </div>

        {/* Navigation */}
        <div className="standard-import-wizard__navigation">
          <Button
            kind="secondary"
            onClick={handlePrevious}
            disabled={currentStep === 0 || isUploading || isProcessing}
          >
            <FormattedMessage
              id="compliance.import.wizard.previous"
              defaultMessage="Previous"
            />
          </Button>

          <div className="standard-import-wizard__navigation-right">
            {currentStep === 3 ? (
              <>
                <Button
                  kind="secondary"
                  renderIcon={Reset}
                  onClick={handleReset}
                >
                  <FormattedMessage
                    id="compliance.import.wizard.import.another"
                    defaultMessage="Import Another File"
                  />
                </Button>
                <Button
                  kind="primary"
                  onClick={() => onComplete(importResults)}
                >
                  <FormattedMessage
                    id="compliance.import.wizard.finish"
                    defaultMessage="Finish"
                  />
                </Button>
              </>
            ) : (
              <Button
                kind="primary"
                onClick={handleNext}
                disabled={!isNextEnabled() || isUploading || isProcessing}
              >
                {currentStep === 2 ? (
                  <FormattedMessage
                    id="compliance.import.wizard.import"
                    defaultMessage="Import Data"
                  />
                ) : (
                  <FormattedMessage
                    id="compliance.import.wizard.next"
                    defaultMessage="Next"
                  />
                )}
              </Button>
            )}
          </div>
        </div>

        {/* Confirmation Modal */}
        <Modal
          open={isConfirmModalOpen}
          onRequestClose={() => setIsConfirmModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "compliance.import.confirm.title",
            defaultMessage: "Confirm Import",
          })}
          primaryButtonText={
            isProcessing
              ? intl.formatMessage({
                  id: "compliance.import.confirm.processing",
                  defaultMessage: "Processing...",
                })
              : intl.formatMessage({
                  id: "compliance.import.confirm.import",
                  defaultMessage: "Import Data",
                })
          }
          secondaryButtonText={intl.formatMessage({
            id: "compliance.import.confirm.cancel",
            defaultMessage: "Cancel",
          })}
          onRequestSubmit={async () => {
            setIsConfirmModalOpen(false);
            await handleExecuteImport();
          }}
          primaryButtonDisabled={isProcessing}
        >
          <p>
            <FormattedMessage
              id="compliance.import.confirm.message"
              defaultMessage="Are you sure you want to import {count} records? This action cannot be undone."
              values={{ count: importPreview?.validRecords || 0 }}
            />
          </p>
          {validationErrors.filter((e) => e.severity === "WARNING").length >
            0 && (
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "compliance.import.confirm.warnings.title",
                defaultMessage: "Warnings Detected",
              })}
              subtitle={intl.formatMessage(
                {
                  id: "compliance.import.confirm.warnings.message",
                  defaultMessage:
                    "There are {count} warnings. Import will continue but please review the results.",
                },
                {
                  count: validationErrors.filter(
                    (e) => e.severity === "WARNING",
                  ).length,
                },
              )}
              hideCloseButton
            />
          )}
        </Modal>

        {/* Preview Modal */}
        <Modal
          open={isPreviewModalOpen}
          onRequestClose={() => setIsPreviewModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "compliance.import.preview.modal.title",
            defaultMessage: "Full Data Preview",
          })}
          size="lg"
          passiveModal
        >
          <div className="standard-import-wizard__preview-modal">
            {/* Full preview data would be rendered here */}
            <p>
              <FormattedMessage
                id="compliance.import.preview.modal.placeholder"
                defaultMessage="Full data preview would be shown here..."
              />
            </p>
          </div>
        </Modal>
      </Column>
    </Grid>
  );
};

export default StandardImportWizard;
