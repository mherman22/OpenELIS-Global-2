import React, { useState, useEffect, useContext } from "react";
import {
  Grid,
  Column,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Search,
  Tag,
  Modal,
  Loading,
  Pagination,
} from "@carbon/react";
import { Add, View, Edit, Download, Upload } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

import PageBreadCrumb from "../common/PageBreadCrumb";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { ConfigurationContext } from "../layout/Layout";
import ComplianceStandardImportModal from "./ComplianceStandardImportModal";
import "./ComplianceStandardsDashboard.css";

/**
 * ComplianceStandardsDashboard - Main dashboard for compliance standards administration
 *
 * Follows OpenELIS patterns:
 * - Carbon Design System v1.15.0 components exclusively
 * - React Intl for all UI strings (constitutional requirement)
 * - Callback-based API integration
 * - Feature flag support for gradual rollout
 * - Role-based access control
 */
const ComplianceStandardsDashboard = () => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  // Feature flag check (constitutional requirement for gradual rollout)
  const isComplianceModuleEnabled =
    configurationProperties?.["compliance.module.enabled"] === "true";

  const [standards, setStandards] = useState([]);
  const [filteredStandards, setFilteredStandards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchValue, setSearchValue] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [pageSize] = useState(25);
  const [selectedStandard, setSelectedStandard] = useState(null);
  const [viewModalOpen, setViewModalOpen] = useState(false);
  const [importModalOpen, setImportModalOpen] = useState(false);

  // Carbon DataTable headers with React Intl
  const headers = [
    {
      key: "name",
      header: intl.formatMessage({
        id: "compliance.standards.table.header.name",
        defaultMessage: "Standard Name",
      }),
    },
    {
      key: "issuingBody",
      header: intl.formatMessage({
        id: "compliance.standards.table.header.issuingBody",
        defaultMessage: "Issuing Body",
      }),
    },
    {
      key: "regulationNumber",
      header: intl.formatMessage({
        id: "compliance.standards.table.header.regulationNumber",
        defaultMessage: "Regulation Number",
      }),
    },
    {
      key: "version",
      header: intl.formatMessage({
        id: "compliance.standards.table.header.version",
        defaultMessage: "Version",
      }),
    },
    {
      key: "effectiveDate",
      header: intl.formatMessage({
        id: "compliance.standards.table.header.effectiveDate",
        defaultMessage: "Effective Date",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "compliance.standards.table.header.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "countryRegion",
      header: intl.formatMessage({
        id: "compliance.standards.table.header.countryRegion",
        defaultMessage: "Country/Region",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "compliance.standards.table.header.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  // Load compliance standards on component mount
  useEffect(() => {
    if (isComplianceModuleEnabled) {
      loadComplianceStandards();
    }
  }, [isComplianceModuleEnabled, currentPage]);

  // Filter standards based on search
  useEffect(() => {
    if (searchValue.trim() === "") {
      setFilteredStandards(standards);
    } else {
      const filtered = standards.filter(
        (standard) =>
          standard.name.toLowerCase().includes(searchValue.toLowerCase()) ||
          standard.issuingBody
            .toLowerCase()
            .includes(searchValue.toLowerCase()) ||
          standard.regulationNumber
            .toLowerCase()
            .includes(searchValue.toLowerCase()),
      );
      setFilteredStandards(filtered);
    }
  }, [searchValue, standards]);

  const loadComplianceStandards = () => {
    setLoading(true);
    const endpoint = `/rest/ComplianceStandardMenu?page=${currentPage}&pageSize=${pageSize}`;

    getFromOpenElisServer(endpoint, (response) => {
      if (response) {
        setStandards(response.standards || []);
        setTotalPages(Math.ceil((response.totalCount || 0) / pageSize));
        setLoading(false);
      } else {
        addNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "compliance.standards.load.error.title",
            defaultMessage: "Error Loading Standards",
          }),
          message: intl.formatMessage({
            id: "compliance.standards.load.error.message",
            defaultMessage:
              "Failed to load compliance standards. Please try again.",
          }),
        });
        setLoading(false);
      }
    });
  };

  const handleSearch = (event) => {
    setSearchValue(event.target.value);
  };

  const handleViewStandard = (standard) => {
    setSelectedStandard(standard);
    setViewModalOpen(true);
  };

  const handleExportStandards = () => {
    const endpoint = "/rest/compliance-standards/export";
    postToOpenElisServer(endpoint, "", (status) => {
      if (status === 200) {
        addNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "compliance.standards.export.success.title",
            defaultMessage: "Export Started",
          }),
          message: intl.formatMessage({
            id: "compliance.standards.export.success.message",
            defaultMessage:
              "Standards export has been initiated. You will receive a notification when ready.",
          }),
        });
      }
    });
  };

  const renderStatusTag = (status) => {
    const statusConfig = {
      ACTIVE: { type: "green", label: "Active" },
      DRAFT: { type: "gray", label: "Draft" },
      SUPERSEDED: { type: "red", label: "Superseded" },
      ARCHIVED: { type: "outline", label: "Archived" },
      SUSPENDED: { type: "yellow", label: "Suspended" },
    };

    const config = statusConfig[status] || { type: "outline", label: status };

    return (
      <Tag type={config.type}>
        <FormattedMessage
          id={`compliance.standards.status.${status.toLowerCase()}`}
          defaultMessage={config.label}
        />
      </Tag>
    );
  };

  const renderActionButtons = (standard) => (
    <div className="compliance-standards__actions">
      <Button
        kind="ghost"
        size="sm"
        renderIcon={View}
        iconDescription={intl.formatMessage({
          id: "compliance.standards.action.view",
          defaultMessage: "View standard details",
        })}
        onClick={() => handleViewStandard(standard)}
      >
        <FormattedMessage id="button.view" defaultMessage="View" />
      </Button>

      {standard.status !== "ARCHIVED" && (
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Edit}
          iconDescription={intl.formatMessage({
            id: "compliance.standards.action.edit",
            defaultMessage: "Edit standard",
          })}
          onClick={() => {
            /* Navigate to edit */
          }}
        >
          <FormattedMessage id="button.edit" defaultMessage="Edit" />
        </Button>
      )}
    </div>
  );

  const rows = filteredStandards.map((standard) => ({
    id: standard.id,
    name: standard.name,
    issuingBody: standard.issuingBody,
    regulationNumber: standard.regulationNumber,
    version: standard.version,
    effectiveDate: new Date(standard.effectiveDate).toLocaleDateString(),
    status: renderStatusTag(standard.status),
    countryRegion: standard.countryRegion,
    actions: renderActionButtons(standard),
  }));

  // Feature flag guard
  if (!isComplianceModuleEnabled) {
    return (
      <Grid className="compliance-standards-dashboard__disabled">
        <Column lg={16}>
          <div className="compliance-standards-dashboard__message">
            <h3>
              <FormattedMessage
                id="compliance.module.disabled.title"
                defaultMessage="Compliance Module Disabled"
              />
            </h3>
            <p>
              <FormattedMessage
                id="compliance.module.disabled.message"
                defaultMessage="The compliance standards module is currently disabled. Please contact your administrator to enable this feature."
              />
            </p>
          </div>
        </Column>
      </Grid>
    );
  }

  return (
    <Grid fullWidth className="compliance-standards-dashboard">
      <Column lg={16}>
        <PageBreadCrumb
          breadcrumbs={[
            {
              label: intl.formatMessage({
                id: "breadcrumb.home",
                defaultMessage: "Home",
              }),
              link: "/",
            },
            {
              label: intl.formatMessage({
                id: "breadcrums.admin.managment",
                defaultMessage: "Administration",
              }),
              link: "/MasterListsPage",
            },
            {
              label: intl.formatMessage({
                id: "master.lists.page.test.management",
                defaultMessage: "Test Management",
              }),
              link: "/MasterListsPage/testManagementConfigMenu",
            },
            {
              label: intl.formatMessage({
                id: "compliance.standards.breadcrumb",
                defaultMessage: "Compliance Standards",
              }),
            },
          ]}
        />
      </Column>

      <Column lg={16} className="compliance-standards-dashboard__header">
        <div className="compliance-standards-dashboard__title-section">
          <h1>
            <FormattedMessage
              id="compliance.standards.dashboard.title"
              defaultMessage="Compliance Standards Administration"
            />
          </h1>
          <p>
            <FormattedMessage
              id="compliance.standards.dashboard.description"
              defaultMessage="Manage regulatory compliance standards for environmental and vector testing"
            />
          </p>
        </div>

        <div className="compliance-standards-dashboard__actions">
          <Button
            kind="tertiary"
            renderIcon={Upload}
            onClick={() => setImportModalOpen(true)}
          >
            <FormattedMessage
              id="compliance.standards.action.import"
              defaultMessage="Import Standards"
            />
          </Button>

          <Button
            kind="tertiary"
            renderIcon={Download}
            onClick={handleExportStandards}
          >
            <FormattedMessage
              id="compliance.standards.action.export"
              defaultMessage="Export Standards"
            />
          </Button>

          <Button
            kind="primary"
            renderIcon={Add}
            onClick={() => {
              /* Navigate to create */
            }}
          >
            <FormattedMessage
              id="compliance.standards.action.add"
              defaultMessage="Add Standard"
            />
          </Button>
        </div>
      </Column>

      <Column lg={16} className="compliance-standards-dashboard__search">
        <Search
          size="lg"
          placeholder={intl.formatMessage({
            id: "compliance.standards.search.placeholder",
            defaultMessage:
              "Search standards by name, issuing body, or regulation number...",
          })}
          labelText={intl.formatMessage({
            id: "compliance.standards.search.label",
            defaultMessage: "Search compliance standards",
          })}
          value={searchValue}
          onChange={handleSearch}
        />
      </Column>

      <Column lg={16} className="compliance-standards-dashboard__table">
        {loading ? (
          <Loading
            description={intl.formatMessage({
              id: "compliance.standards.loading",
              defaultMessage: "Loading compliance standards...",
            })}
          />
        ) : (
          <>
            <DataTable
              rows={rows}
              headers={headers}
              render={({
                rows,
                headers,
                getHeaderProps,
                getRowProps,
                getTableProps,
              }) => (
                <TableContainer
                  title={intl.formatMessage({
                    id: "compliance.standards.table.title",
                    defaultMessage: "Compliance Standards",
                  })}
                  description={intl.formatMessage({
                    id: "compliance.standards.table.description",
                    defaultMessage: `Showing ${filteredStandards.length} of ${standards.length} standards`,
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

            <Pagination
              backwardText={intl.formatMessage({
                id: "pagination.backward",
                defaultMessage: "Previous page",
              })}
              forwardText={intl.formatMessage({
                id: "pagination.forward",
                defaultMessage: "Next page",
              })}
              itemsPerPageText={intl.formatMessage({
                id: "pagination.itemsPerPage",
                defaultMessage: "Items per page:",
              })}
              page={currentPage}
              pageNumberText={intl.formatMessage({
                id: "pagination.pageNumber",
                defaultMessage: "Page Number",
              })}
              pageSize={pageSize}
              pageSizes={[10, 25, 50, 100]}
              totalItems={standards.length}
              onChange={({ page }) => setCurrentPage(page)}
            />
          </>
        )}
      </Column>

      {/* View Standard Modal */}
      <Modal
        open={viewModalOpen}
        onRequestClose={() => setViewModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "compliance.standards.view.modal.title",
          defaultMessage: "View Compliance Standard",
        })}
        modalLabel={selectedStandard?.regulationNumber}
        primaryButtonText={intl.formatMessage({
          id: "button.close",
          defaultMessage: "Close",
        })}
        size="lg"
      >
        {selectedStandard && (
          <div className="compliance-standard-view">
            {/* Standard details will be implemented in a separate component */}
            <p>
              <strong>Name:</strong> {selectedStandard.name}
            </p>
            <p>
              <strong>Issuing Body:</strong> {selectedStandard.issuingBody}
            </p>
            <p>
              <strong>Status:</strong> {selectedStandard.status}
            </p>
            {/* More details to be added */}
          </div>
        )}
      </Modal>

      {/* Advanced Import Modal */}
      <ComplianceStandardImportModal
        open={importModalOpen}
        onRequestClose={() => setImportModalOpen(false)}
        onImportComplete={(results) => {
          console.log("Import completed:", results);
          loadComplianceStandards(); // Refresh the list
        }}
      />
    </Grid>
  );
};

export default ComplianceStandardsDashboard;
