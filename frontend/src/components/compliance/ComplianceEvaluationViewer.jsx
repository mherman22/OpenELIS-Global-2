import React, { useState, useEffect, useContext } from "react";
import {
  Grid,
  Column,
  Button,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Search,
  Pagination,
  Modal,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  InlineLoading,
  InlineNotification,
  Tag,
  ProgressBar,
  Accordion,
  AccordionItem,
  Tooltip,
  OverflowMenu,
  OverflowMenuItem,
  SkeletonText,
  SkeletonPlaceholder,
} from "@carbon/react";
import {
  View,
  Download,
  Checkmark,
  Warning,
  ErrorFilled,
  Information,
  ChevronRight,
  Filter,
  Reset,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  ConfigurationContext,
  NotificationContext,
} from "../layout/Layout";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import "./ComplianceEvaluationViewer.css";

const ComplianceEvaluationViewer = () => {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const { addNotification } = useContext(NotificationContext);

  // Feature flag check
  const isComplianceModuleEnabled =
    configurationProperties?.["compliance.module.enabled"] === "true";

  // State management
  const [evaluations, setEvaluations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedEvaluation, setSelectedEvaluation] = useState(null);
  const [evaluationResults, setEvaluationResults] = useState([]);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [isLoadingResults, setIsLoadingResults] = useState(false);

  // Filter state
  const [filters, setFilters] = useState({
    status: "",
    standard: "",
    sampleId: "",
    startDate: "",
    endDate: "",
  });

  // Pagination state
  const [pagination, setPagination] = useState({
    page: 1,
    pageSize: 20,
    totalItems: 0,
  });

  // Additional data
  const [complianceStandards, setComplianceStandards] = useState([]);

  // Feature flag guard
  if (!isComplianceModuleEnabled) {
    return (
      <Grid className="compliance-evaluation-viewer__disabled">
        <Column lg={16} md={8} sm={4}>
          <div className="compliance-evaluation-viewer__message">
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

  // Load evaluations
  const loadEvaluations = async (page = 1, pageSize = 20) => {
    try {
      setLoading(true);

      const queryParams = new URLSearchParams({
        page: page.toString(),
        pageSize: pageSize.toString(),
        search: searchTerm || "",
        ...Object.fromEntries(
          Object.entries(filters).filter(([_, value]) => value !== ""),
        ),
      });

      const response = await getFromOpenElisServer(
        `/rest/compliance/evaluations?${queryParams}`,
      );

      if (response) {
        setEvaluations(response.content || []);
        setPagination({
          page: response.number + 1,
          pageSize: response.size,
          totalItems: response.totalElements,
        });
      }
    } catch (error) {
      console.error("Error loading evaluations:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "compliance.evaluation.load.error.title",
          defaultMessage: "Error Loading Evaluations",
        }),
        message: intl.formatMessage({
          id: "compliance.evaluation.load.error.message",
          defaultMessage:
            "Unable to load compliance evaluations. Please try again.",
        }),
      });
    } finally {
      setLoading(false);
    }
  };

  // Load compliance standards for filtering
  const loadComplianceStandards = async () => {
    try {
      const response = await getFromOpenElisServer(
        "/rest/compliance/standards",
      );
      if (response) {
        setComplianceStandards(response);
      }
    } catch (error) {
      console.error("Error loading compliance standards:", error);
    }
  };

  // Load evaluation results
  const loadEvaluationResults = async (evaluationId) => {
    try {
      setIsLoadingResults(true);
      const response = await getFromOpenElisServer(
        `/rest/compliance/evaluations/${evaluationId}/results`,
      );

      if (response) {
        setEvaluationResults(response);
      }
    } catch (error) {
      console.error("Error loading evaluation results:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "compliance.evaluation.results.error.title",
          defaultMessage: "Error Loading Evaluation Results",
        }),
        message: intl.formatMessage({
          id: "compliance.evaluation.results.error.message",
          defaultMessage:
            "Unable to load evaluation results. Please try again.",
        }),
      });
    } finally {
      setIsLoadingResults(false);
    }
  };

  // Load data on component mount
  useEffect(() => {
    loadComplianceStandards();
    loadEvaluations();
  }, []);

  // Reload when filters or search change
  useEffect(() => {
    const timer = setTimeout(() => {
      loadEvaluations(1, pagination.pageSize);
    }, 300);

    return () => clearTimeout(timer);
  }, [searchTerm, filters]);

  // Handle view evaluation
  const handleViewEvaluation = async (evaluation) => {
    setSelectedEvaluation(evaluation);
    await loadEvaluationResults(evaluation.id);
    setIsViewModalOpen(true);
  };

  // Handle export evaluation
  const handleExportEvaluation = async (evaluation) => {
    try {
      const response = await getFromOpenElisServer(
        `/rest/compliance/evaluations/${evaluation.id}/export`,
        { responseType: "blob" },
      );

      // Create download link
      const url = window.URL.createObjectURL(new Blob([response]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute(
        "download",
        `compliance-evaluation-${evaluation.id}.pdf`,
      );
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      addNotification({
        kind: "success",
        title: intl.formatMessage({
          id: "compliance.evaluation.export.success.title",
          defaultMessage: "Export Successful",
        }),
        message: intl.formatMessage({
          id: "compliance.evaluation.export.success.message",
          defaultMessage: "Evaluation report has been downloaded successfully.",
        }),
      });
    } catch (error) {
      console.error("Error exporting evaluation:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "compliance.evaluation.export.error.title",
          defaultMessage: "Export Failed",
        }),
        message: intl.formatMessage({
          id: "compliance.evaluation.export.error.message",
          defaultMessage:
            "Unable to export evaluation report. Please try again.",
        }),
      });
    }
  };

  // Handle filter reset
  const handleResetFilters = () => {
    setFilters({
      status: "",
      standard: "",
      sampleId: "",
      startDate: "",
      endDate: "",
    });
    setSearchTerm("");
  };

  // Handle pagination
  const handlePaginationChange = ({ page, pageSize }) => {
    setPagination((prev) => ({ ...prev, page, pageSize }));
    loadEvaluations(page, pageSize);
  };

  // Get status tag
  const getStatusTag = (status) => {
    const statusConfigs = {
      COMPLIANT: {
        type: "green",
        icon: Checkmark,
        text: intl.formatMessage({
          id: "compliance.evaluation.status.compliant",
          defaultMessage: "Compliant",
        }),
      },
      NON_COMPLIANT: {
        type: "red",
        icon: ErrorFilled,
        text: intl.formatMessage({
          id: "compliance.evaluation.status.nonCompliant",
          defaultMessage: "Non-Compliant",
        }),
      },
      WARNING: {
        type: "yellow",
        icon: Warning,
        text: intl.formatMessage({
          id: "compliance.evaluation.status.warning",
          defaultMessage: "Warning",
        }),
      },
      PENDING: {
        type: "cyan",
        icon: Information,
        text: intl.formatMessage({
          id: "compliance.evaluation.status.pending",
          defaultMessage: "Pending",
        }),
      },
    };

    const config = statusConfigs[status] || statusConfigs.PENDING;
    const IconComponent = config.icon;

    return (
      <Tag type={config.type} size="sm" renderIcon={IconComponent}>
        {config.text}
      </Tag>
    );
  };

  // Get compliance percentage
  const getCompliancePercentage = (evaluation) => {
    if (!evaluation.totalParameters || evaluation.totalParameters === 0)
      return 0;
    return Math.round(
      (evaluation.compliantParameters / evaluation.totalParameters) * 100,
    );
  };

  // Get compliance percentage color
  const getComplianceColor = (percentage) => {
    if (percentage >= 95) return "green";
    if (percentage >= 80) return "yellow";
    return "red";
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return "";
    return new Date(dateString).toLocaleDateString(intl.locale, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  // Table headers
  const tableHeaders = [
    {
      key: "sampleId",
      header: intl.formatMessage({
        id: "compliance.evaluation.table.header.sampleId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "standard",
      header: intl.formatMessage({
        id: "compliance.evaluation.table.header.standard",
        defaultMessage: "Standard",
      }),
    },
    {
      key: "evaluationDate",
      header: intl.formatMessage({
        id: "compliance.evaluation.table.header.evaluationDate",
        defaultMessage: "Evaluation Date",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "compliance.evaluation.table.header.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "compliance",
      header: intl.formatMessage({
        id: "compliance.evaluation.table.header.compliance",
        defaultMessage: "Compliance",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "compliance.evaluation.table.header.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  // Prepare table data
  const tableData = evaluations.map((evaluation) => ({
    id: evaluation.id,
    sampleId: evaluation.sampleId || "N/A",
    standard: evaluation.complianceStandardName || "Unknown",
    evaluationDate: evaluation.evaluationDate,
    status: evaluation.status,
    compliance: getCompliancePercentage(evaluation),
    ...evaluation,
  }));

  if (loading && evaluations.length === 0) {
    return (
      <Grid className="compliance-evaluation-viewer">
        <Column lg={16}>
          <div className="compliance-evaluation-viewer__loading">
            <SkeletonText paragraph lineCount={3} />
            <SkeletonPlaceholder className="compliance-evaluation-viewer__skeleton-table" />
          </div>
        </Column>
      </Grid>
    );
  }

  return (
    <Grid className="compliance-evaluation-viewer">
      <Column lg={16}>
        {/* Header */}
        <div className="compliance-evaluation-viewer__header">
          <div className="compliance-evaluation-viewer__title-section">
            <h1>
              <FormattedMessage
                id="compliance.evaluation.viewer.title"
                defaultMessage="Compliance Evaluation Results"
              />
            </h1>
            <p>
              <FormattedMessage
                id="compliance.evaluation.viewer.description"
                defaultMessage="View and analyze compliance evaluation results for laboratory samples"
              />
            </p>
          </div>
        </div>

        {/* Filters */}
        <Accordion>
          <AccordionItem
            title={intl.formatMessage({
              id: "compliance.evaluation.filters.title",
              defaultMessage: "Filter Options",
            })}
            open={Object.values(filters).some((value) => value !== "")}
          >
            <Grid>
              <Column lg={4} md={2} sm={2}>
                <Select
                  id="status-filter"
                  labelText={intl.formatMessage({
                    id: "compliance.evaluation.filter.status",
                    defaultMessage: "Status",
                  })}
                  value={filters.status}
                  onChange={(e) =>
                    setFilters((prev) => ({ ...prev, status: e.target.value }))
                  }
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: "compliance.evaluation.filter.status.all",
                      defaultMessage: "All statuses",
                    })}
                  />
                  <SelectItem
                    value="COMPLIANT"
                    text={intl.formatMessage({
                      id: "compliance.evaluation.status.compliant",
                      defaultMessage: "Compliant",
                    })}
                  />
                  <SelectItem
                    value="NON_COMPLIANT"
                    text={intl.formatMessage({
                      id: "compliance.evaluation.status.nonCompliant",
                      defaultMessage: "Non-Compliant",
                    })}
                  />
                  <SelectItem
                    value="WARNING"
                    text={intl.formatMessage({
                      id: "compliance.evaluation.status.warning",
                      defaultMessage: "Warning",
                    })}
                  />
                  <SelectItem
                    value="PENDING"
                    text={intl.formatMessage({
                      id: "compliance.evaluation.status.pending",
                      defaultMessage: "Pending",
                    })}
                  />
                </Select>
              </Column>

              <Column lg={4} md={2} sm={2}>
                <Select
                  id="standard-filter"
                  labelText={intl.formatMessage({
                    id: "compliance.evaluation.filter.standard",
                    defaultMessage: "Compliance Standard",
                  })}
                  value={filters.standard}
                  onChange={(e) =>
                    setFilters((prev) => ({
                      ...prev,
                      standard: e.target.value,
                    }))
                  }
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: "compliance.evaluation.filter.standard.all",
                      defaultMessage: "All standards",
                    })}
                  />
                  {complianceStandards.map((standard) => (
                    <SelectItem
                      key={standard.id}
                      value={standard.id}
                      text={standard.name}
                    />
                  ))}
                </Select>
              </Column>

              <Column lg={4} md={2} sm={2}>
                <DatePicker
                  dateFormat="d/m/Y"
                  datePickerType="range"
                  onChange={(dates) => {
                    const [start, end] = dates;
                    setFilters((prev) => ({
                      ...prev,
                      startDate: start ? start.toISOString().split("T")[0] : "",
                      endDate: end ? end.toISOString().split("T")[0] : "",
                    }));
                  }}
                >
                  <DatePickerInput
                    id="start-date"
                    placeholder="dd/mm/yyyy"
                    labelText={intl.formatMessage({
                      id: "compliance.evaluation.filter.startDate",
                      defaultMessage: "Start Date",
                    })}
                  />
                  <DatePickerInput
                    id="end-date"
                    placeholder="dd/mm/yyyy"
                    labelText={intl.formatMessage({
                      id: "compliance.evaluation.filter.endDate",
                      defaultMessage: "End Date",
                    })}
                  />
                </DatePicker>
              </Column>

              <Column lg={4} md={2} sm={2}>
                <div className="compliance-evaluation-viewer__filter-actions">
                  <Button
                    kind="secondary"
                    size="sm"
                    renderIcon={Reset}
                    onClick={handleResetFilters}
                  >
                    <FormattedMessage
                      id="compliance.evaluation.filter.reset"
                      defaultMessage="Reset Filters"
                    />
                  </Button>
                </div>
              </Column>
            </Grid>
          </AccordionItem>
        </Accordion>

        {/* Search */}
        <div className="compliance-evaluation-viewer__search">
          <Search
            size="lg"
            placeholder={intl.formatMessage({
              id: "compliance.evaluation.search.placeholder",
              defaultMessage:
                "Search by sample ID, standard name, or evaluation details...",
            })}
            labelText={intl.formatMessage({
              id: "compliance.evaluation.search.label",
              defaultMessage: "Search",
            })}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onClear={() => setSearchTerm("")}
          />
        </div>

        {/* Results Table */}
        <div className="compliance-evaluation-viewer__table">
          {loading ? (
            <InlineLoading
              description={intl.formatMessage({
                id: "compliance.evaluation.loading",
                defaultMessage: "Loading evaluations...",
              })}
            />
          ) : (
            <DataTable
              rows={tableData}
              headers={tableHeaders}
              render={({
                rows,
                headers,
                getHeaderProps,
                getRowProps,
                getTableProps,
                getTableContainerProps,
              }) => (
                <TableContainer
                  title={intl.formatMessage({
                    id: "compliance.evaluation.table.title",
                    defaultMessage: "Evaluation Results",
                  })}
                  description={intl.formatMessage({
                    id: "compliance.evaluation.table.description",
                    defaultMessage: `Showing ${evaluations.length} of ${pagination.totalItems} evaluations`,
                  })}
                  {...getTableContainerProps()}
                >
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
                            if (cell.info.header === "status") {
                              return (
                                <TableCell key={cell.id}>
                                  {getStatusTag(cell.value)}
                                </TableCell>
                              );
                            } else if (cell.info.header === "compliance") {
                              const percentage = cell.value;
                              return (
                                <TableCell key={cell.id}>
                                  <div className="compliance-evaluation-viewer__progress">
                                    <ProgressBar
                                      value={percentage}
                                      max={100}
                                      size="sm"
                                      status={getComplianceColor(percentage)}
                                      labelText={`${percentage}%`}
                                      hideLabel={false}
                                    />
                                  </div>
                                </TableCell>
                              );
                            } else if (cell.info.header === "evaluationDate") {
                              return (
                                <TableCell key={cell.id}>
                                  {formatDate(cell.value)}
                                </TableCell>
                              );
                            } else if (cell.info.header === "actions") {
                              const evaluation = evaluations.find(
                                (e) => e.id === row.id,
                              );
                              return (
                                <TableCell key={cell.id}>
                                  <div className="compliance-evaluation__actions">
                                    <Tooltip
                                      label={intl.formatMessage({
                                        id: "compliance.evaluation.action.view.tooltip",
                                        defaultMessage:
                                          "View evaluation details",
                                      })}
                                    >
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        renderIcon={View}
                                        onClick={() =>
                                          handleViewEvaluation(evaluation)
                                        }
                                        iconDescription={intl.formatMessage({
                                          id: "compliance.evaluation.action.view",
                                          defaultMessage: "View",
                                        })}
                                        hasIconOnly
                                      />
                                    </Tooltip>
                                    <Tooltip
                                      label={intl.formatMessage({
                                        id: "compliance.evaluation.action.export.tooltip",
                                        defaultMessage:
                                          "Export evaluation report",
                                      })}
                                    >
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        renderIcon={Download}
                                        onClick={() =>
                                          handleExportEvaluation(evaluation)
                                        }
                                        iconDescription={intl.formatMessage({
                                          id: "compliance.evaluation.action.export",
                                          defaultMessage: "Export",
                                        })}
                                        hasIconOnly
                                      />
                                    </Tooltip>
                                  </div>
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
          )}
        </div>

        {/* Pagination */}
        {pagination.totalItems > pagination.pageSize && (
          <Pagination
            page={pagination.page}
            pageSize={pagination.pageSize}
            pageSizes={[10, 20, 50, 100]}
            totalItems={pagination.totalItems}
            onChange={handlePaginationChange}
            size="md"
          />
        )}

        {/* Evaluation Details Modal */}
        <Modal
          open={isViewModalOpen}
          onRequestClose={() => setIsViewModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "compliance.evaluation.details.title",
            defaultMessage: "Evaluation Details",
          })}
          size="lg"
          passiveModal
        >
          {selectedEvaluation && (
            <div className="compliance-evaluation-details">
              {/* Evaluation Summary */}
              <div className="compliance-evaluation-details__summary">
                <Grid>
                  <Column lg={8} md={4} sm={2}>
                    <h4>
                      <FormattedMessage
                        id="compliance.evaluation.details.summary"
                        defaultMessage="Evaluation Summary"
                      />
                    </h4>
                    <div className="compliance-evaluation-details__info">
                      <p>
                        <strong>
                          <FormattedMessage
                            id="compliance.evaluation.details.sampleId"
                            defaultMessage="Sample ID:"
                          />
                        </strong>{" "}
                        {selectedEvaluation.sampleId}
                      </p>
                      <p>
                        <strong>
                          <FormattedMessage
                            id="compliance.evaluation.details.standard"
                            defaultMessage="Standard:"
                          />
                        </strong>{" "}
                        {selectedEvaluation.complianceStandardName}
                      </p>
                      <p>
                        <strong>
                          <FormattedMessage
                            id="compliance.evaluation.details.date"
                            defaultMessage="Evaluation Date:"
                          />
                        </strong>{" "}
                        {formatDate(selectedEvaluation.evaluationDate)}
                      </p>
                      <p>
                        <strong>
                          <FormattedMessage
                            id="compliance.evaluation.details.status"
                            defaultMessage="Status:"
                          />
                        </strong>{" "}
                        {getStatusTag(selectedEvaluation.status)}
                      </p>
                    </div>
                  </Column>
                  <Column lg={8} md={4} sm={2}>
                    <h4>
                      <FormattedMessage
                        id="compliance.evaluation.details.compliance"
                        defaultMessage="Compliance Overview"
                      />
                    </h4>
                    <div className="compliance-evaluation-details__progress">
                      <ProgressBar
                        value={getCompliancePercentage(selectedEvaluation)}
                        max={100}
                        size="md"
                        status={getComplianceColor(
                          getCompliancePercentage(selectedEvaluation),
                        )}
                        labelText={intl.formatMessage(
                          {
                            id: "compliance.evaluation.details.progress.label",
                            defaultMessage:
                              "{compliant} of {total} parameters compliant ({percentage}%)",
                          },
                          {
                            compliant:
                              selectedEvaluation.compliantParameters || 0,
                            total: selectedEvaluation.totalParameters || 0,
                            percentage:
                              getCompliancePercentage(selectedEvaluation),
                          },
                        )}
                        hideLabel={false}
                      />
                    </div>
                  </Column>
                </Grid>
              </div>

              {/* Parameter Results */}
              <div className="compliance-evaluation-details__results">
                <h4>
                  <FormattedMessage
                    id="compliance.evaluation.details.parameters"
                    defaultMessage="Parameter Results"
                  />
                </h4>
                {isLoadingResults ? (
                  <InlineLoading
                    description={intl.formatMessage({
                      id: "compliance.evaluation.results.loading",
                      defaultMessage: "Loading parameter results...",
                    })}
                  />
                ) : (
                  <div className="compliance-evaluation-details__parameters">
                    {evaluationResults.map((result, index) => (
                      <div
                        key={index}
                        className="compliance-evaluation-details__parameter"
                      >
                        <div className="compliance-evaluation-details__parameter-header">
                          <span className="compliance-evaluation-details__parameter-name">
                            {result.parameterName}
                          </span>
                          {getStatusTag(result.status)}
                        </div>
                        <div className="compliance-evaluation-details__parameter-values">
                          <span>
                            <FormattedMessage
                              id="compliance.evaluation.details.actualValue"
                              defaultMessage="Actual: {value} {unit}"
                              values={{
                                value: result.actualValue,
                                unit: result.unit,
                              }}
                            />
                          </span>
                          <span>
                            <FormattedMessage
                              id="compliance.evaluation.details.threshold"
                              defaultMessage="Threshold: {threshold}"
                              values={{
                                threshold: result.thresholdDescription,
                              }}
                            />
                          </span>
                        </div>
                        {result.notes && (
                          <div className="compliance-evaluation-details__parameter-notes">
                            <strong>
                              <FormattedMessage
                                id="compliance.evaluation.details.notes"
                                defaultMessage="Notes:"
                              />
                            </strong>{" "}
                            {result.notes}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </Modal>
      </Column>
    </Grid>
  );
};

export default ComplianceEvaluationViewer;
