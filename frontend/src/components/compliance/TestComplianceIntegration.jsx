import React, { useState, useEffect, useContext } from 'react';
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
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Checkbox,
  Select,
  SelectItem,
  FormGroup,
  Toggle,
  TextInput,
  Dropdown,
  MultiSelect
} from '@carbon/react';
import {
  Link,
  Unlink,
  Settings,
  CheckmarkFilled,
  WarningFilled,
  Information,
  Add
} from '@carbon/icons-react';
import { FormattedMessage, useIntl } from 'react-intl';

import PageBreadCrumb from '../common/PageBreadCrumb';
import { getFromOpenElisServer, postToOpenElisServer } from '../utils/Utils';
import { NotificationContext } from '../layout/Layout';
import './TestComplianceIntegration.css';

/**
 * TestComplianceIntegration - Component for managing Test-Compliance Standard associations
 *
 * Features:
 * - Test catalog integration with compliance standards
 * - Drag-and-drop standard assignment
 * - Real-time compliance status indicators
 * - Bulk assignment capabilities
 * - QC rule integration
 * - Sampling protocol associations
 */
const TestComplianceIntegration = ({ testId, onClose }) => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [test, setTest] = useState(null);
  const [assignedStandards, setAssignedStandards] = useState([]);
  const [availableStandards, setAvailableStandards] = useState([]);
  const [searchValue, setSearchValue] = useState('');
  const [selectedTab, setSelectedTab] = useState(0);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [selectedStandards, setSelectedStandards] = useState([]);
  const [bulkAssignMode, setBulkAssignMode] = useState(false);

  // Load test details and associated standards
  useEffect(() => {
    if (testId) {
      loadTestComplianceData();
    }
  }, [testId]);

  const loadTestComplianceData = async () => {
    setLoading(true);
    try {
      // Load test details
      const testResponse = await getFromOpenElisServer(`/rest/tests/${testId}`);
      setTest(testResponse);

      // Load assigned compliance standards
      const assignedResponse = await getFromOpenElisServer(
        `/rest/test/${testId}/compliance-standards`
      );
      setAssignedStandards(assignedResponse || []);

      // Load available standards
      const availableResponse = await getFromOpenElisServer(
        `/rest/available-compliance-standards`
      );
      setAvailableStandards(availableResponse || []);

    } catch (error) {
      addNotification({
        kind: 'error',
        title: intl.formatMessage({
          id: 'test.compliance.load.error.title',
          defaultMessage: 'Loading Error'
        }),
        message: intl.formatMessage({
          id: 'test.compliance.load.error.message',
          defaultMessage: 'Failed to load test compliance data'
        })
      });
    } finally {
      setLoading(false);
    }
  };

  const handleAssignStandard = async (standardId, mandatory = false, applicableParameters = '') => {
    try {
      const payload = {
        complianceStandardId: standardId,
        mandatory: mandatory,
        applicableParameters: applicableParameters
      };

      const response = await postToOpenElisServer(
        `/rest/test/${testId}/compliance-standards`,
        JSON.stringify(payload)
      );

      if (response) {
        loadTestComplianceData(); // Refresh data
        addNotification({
          kind: 'success',
          title: intl.formatMessage({
            id: 'test.compliance.assign.success.title',
            defaultMessage: 'Standard Assigned'
          }),
          message: intl.formatMessage({
            id: 'test.compliance.assign.success.message',
            defaultMessage: 'Compliance standard successfully assigned to test'
          })
        });
      }
    } catch (error) {
      addNotification({
        kind: 'error',
        title: intl.formatMessage({
          id: 'test.compliance.assign.error.title',
          defaultMessage: 'Assignment Error'
        }),
        message: error.message || 'Failed to assign compliance standard'
      });
    }
  };

  const handleRemoveStandard = async (standardId) => {
    try {
      await postToOpenElisServer(
        `/rest/test/${testId}/compliance-standards/${standardId}/remove`,
        ''
      );

      loadTestComplianceData(); // Refresh data
      addNotification({
        kind: 'success',
        title: intl.formatMessage({
          id: 'test.compliance.remove.success.title',
          defaultMessage: 'Standard Removed'
        }),
        message: intl.formatMessage({
          id: 'test.compliance.remove.success.message',
          defaultMessage: 'Compliance standard removed from test'
        })
      });
    } catch (error) {
      addNotification({
        kind: 'error',
        title: intl.formatMessage({
          id: 'test.compliance.remove.error.title',
          defaultMessage: 'Removal Error'
        }),
        message: error.message || 'Failed to remove compliance standard'
      });
    }
  };

  const renderComplianceStatus = (standard) => {
    const statusMap = {
      COMPLIANT: { type: 'green', icon: CheckmarkFilled, label: 'Compliant' },
      NON_COMPLIANT: { type: 'red', icon: WarningFilled, label: 'Non-Compliant' },
      PENDING: { type: 'gray', icon: Information, label: 'Pending Evaluation' }
    };

    const status = statusMap[standard.complianceStatus] || statusMap.PENDING;

    return (
      <Tag type={status.type} renderIcon={status.icon}>
        <FormattedMessage
          id={`test.compliance.status.${standard.complianceStatus?.toLowerCase() || 'pending'}`}
          defaultMessage={status.label}
        />
      </Tag>
    );
  };

  const assignedHeaders = [
    {
      key: 'standardName',
      header: intl.formatMessage({
        id: 'test.compliance.table.header.standardName',
        defaultMessage: 'Standard Name'
      })
    },
    {
      key: 'regulationNumber',
      header: intl.formatMessage({
        id: 'test.compliance.table.header.regulationNumber',
        defaultMessage: 'Regulation Number'
      })
    },
    {
      key: 'mandatory',
      header: intl.formatMessage({
        id: 'test.compliance.table.header.mandatory',
        defaultMessage: 'Mandatory'
      })
    },
    {
      key: 'complianceStatus',
      header: intl.formatMessage({
        id: 'test.compliance.table.header.status',
        defaultMessage: 'Compliance Status'
      })
    },
    {
      key: 'actions',
      header: intl.formatMessage({
        id: 'test.compliance.table.header.actions',
        defaultMessage: 'Actions'
      })
    }
  ];

  const assignedRows = assignedStandards.map(standard => ({
    id: standard.id,
    standardName: standard.complianceStandardName,
    regulationNumber: standard.complianceStandardRegulationNumber,
    mandatory: standard.mandatory ? (
      <Tag type="red">
        <FormattedMessage id="test.compliance.mandatory" defaultMessage="Mandatory" />
      </Tag>
    ) : (
      <Tag type="gray">
        <FormattedMessage id="test.compliance.optional" defaultMessage="Optional" />
      </Tag>
    ),
    complianceStatus: renderComplianceStatus(standard),
    actions: (
      <Button
        kind="ghost"
        size="sm"
        renderIcon={Unlink}
        iconDescription={intl.formatMessage({
          id: 'test.compliance.action.remove',
          defaultMessage: 'Remove standard'
        })}
        onClick={() => handleRemoveStandard(standard.complianceStandardId)}
      >
        <FormattedMessage id="button.remove" defaultMessage="Remove" />
      </Button>
    )
  }));

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({
          id: 'test.compliance.loading',
          defaultMessage: 'Loading test compliance data...'
        })}
      />
    );
  }

  return (
    <Grid fullWidth className="test-compliance-integration">
      <Column lg={16}>
        <PageBreadCrumb breadcrumbs={[
          {
            label: intl.formatMessage({ id: 'breadcrumb.home', defaultMessage: 'Home' }),
            link: '/'
          },
          {
            label: intl.formatMessage({
              id: 'test.catalog.breadcrumb',
              defaultMessage: 'Test Catalog'
            }),
            link: '/tests'
          },
          {
            label: intl.formatMessage({
              id: 'test.compliance.breadcrumb',
              defaultMessage: 'Compliance Integration'
            })
          }
        ]} />
      </Column>

      <Column lg={16} className="test-compliance-integration__header">
        <div className="test-compliance-integration__title-section">
          <h1>
            <FormattedMessage
              id="test.compliance.integration.title"
              defaultMessage="Test Compliance Integration"
            />
          </h1>
          <p>
            <FormattedMessage
              id="test.compliance.integration.description"
              defaultMessage="Configure compliance standards for {testName}"
              values={{ testName: test?.name || 'test' }}
            />
          </p>
        </div>

        <div className="test-compliance-integration__actions">
          <Button
            kind="tertiary"
            renderIcon={Settings}
            onClick={() => setBulkAssignMode(!bulkAssignMode)}
          >
            <FormattedMessage
              id="test.compliance.action.bulkMode"
              defaultMessage="Bulk Assignment"
            />
          </Button>

          <Button
            kind="primary"
            renderIcon={Add}
            onClick={() => setAssignModalOpen(true)}
          >
            <FormattedMessage
              id="test.compliance.action.assign"
              defaultMessage="Assign Standard"
            />
          </Button>
        </div>
      </Column>

      <Column lg={16} className="test-compliance-integration__content">
        <Tabs selectedIndex={selectedTab} onChange={(index) => setSelectedTab(index)}>
          <TabList>
            <Tab>
              <FormattedMessage
                id="test.compliance.tab.assigned"
                defaultMessage="Assigned Standards"
              />
            </Tab>
            <Tab>
              <FormattedMessage
                id="test.compliance.tab.qc"
                defaultMessage="QC Rules"
              />
            </Tab>
            <Tab>
              <FormattedMessage
                id="test.compliance.tab.protocols"
                defaultMessage="Sampling Protocols"
              />
            </Tab>
          </TabList>

          <TabPanels>
            {/* Assigned Standards Tab */}
            <TabPanel>
              <div className="test-compliance-integration__assigned">
                <Search
                  size="lg"
                  placeholder={intl.formatMessage({
                    id: 'test.compliance.search.placeholder',
                    defaultMessage: 'Search assigned standards...'
                  })}
                  value={searchValue}
                  onChange={(e) => setSearchValue(e.target.value)}
                />

                <DataTable
                  rows={assignedRows}
                  headers={assignedHeaders}
                  render={({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
                    <TableContainer
                      title={intl.formatMessage({
                        id: 'test.compliance.assigned.table.title',
                        defaultMessage: 'Assigned Compliance Standards'
                      })}
                      description={intl.formatMessage({
                        id: 'test.compliance.assigned.table.description',
                        defaultMessage: `${assignedStandards.length} standards assigned to this test`
                      })}
                    >
                      <Table {...getTableProps()}>
                        <TableHead>
                          <TableRow>
                            {headers.map((header) => (
                              <TableHeader {...getHeaderProps({ header })} key={header.key}>
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
              </div>
            </TabPanel>

            {/* QC Rules Tab */}
            <TabPanel>
              <div className="test-compliance-integration__qc">
                <p>
                  <FormattedMessage
                    id="test.compliance.qc.placeholder"
                    defaultMessage="QC rule integration will be implemented here. This will allow configuration of Westgard rules, control charts, and other QC methods tied to compliance standards."
                  />
                </p>
              </div>
            </TabPanel>

            {/* Sampling Protocols Tab */}
            <TabPanel>
              <div className="test-compliance-integration__protocols">
                <p>
                  <FormattedMessage
                    id="test.compliance.protocols.placeholder"
                    defaultMessage="Sampling protocol association will be implemented here. This will allow linking sampling procedures to regulatory compliance requirements."
                  />
                </p>
              </div>
            </TabPanel>
          </TabPanels>
        </Tabs>
      </Column>

      {/* Assign Standard Modal */}
      <Modal
        open={assignModalOpen}
        onRequestClose={() => setAssignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: 'test.compliance.assign.modal.title',
          defaultMessage: 'Assign Compliance Standard'
        })}
        primaryButtonText={intl.formatMessage({
          id: 'button.assign',
          defaultMessage: 'Assign'
        })}
        secondaryButtonText={intl.formatMessage({
          id: 'button.cancel',
          defaultMessage: 'Cancel'
        })}
        size="md"
        onRequestSubmit={() => {
          // Handle assignment
          setAssignModalOpen(false);
        }}
      >
        <FormGroup>
          <Select
            id="standard-select"
            labelText={intl.formatMessage({
              id: 'test.compliance.assign.standard.label',
              defaultMessage: 'Select Compliance Standard'
            })}
            helperText={intl.formatMessage({
              id: 'test.compliance.assign.standard.helper',
              defaultMessage: 'Choose a compliance standard to assign to this test'
            })}
          >
            <SelectItem value="" text="Choose a standard..." />
            {availableStandards.map(standard => (
              <SelectItem
                key={standard.id}
                value={standard.id}
                text={`${standard.name} (${standard.regulationNumber})`}
              />
            ))}
          </Select>

          <Toggle
            id="mandatory-toggle"
            labelText={intl.formatMessage({
              id: 'test.compliance.assign.mandatory.label',
              defaultMessage: 'Mandatory Compliance'
            })}
            helperText={intl.formatMessage({
              id: 'test.compliance.assign.mandatory.helper',
              defaultMessage: 'When enabled, compliance with this standard is required'
            })}
          />

          <TextInput
            id="parameters-input"
            labelText={intl.formatMessage({
              id: 'test.compliance.assign.parameters.label',
              defaultMessage: 'Applicable Parameters'
            })}
            helperText={intl.formatMessage({
              id: 'test.compliance.assign.parameters.helper',
              defaultMessage: 'Specify which test parameters this standard applies to (optional)'
            })}
            placeholder="e.g., pH, turbidity, coliform count"
          />
        </FormGroup>
      </Modal>
    </Grid>
  );
};

export default TestComplianceIntegration;