import React, { useState, useEffect, useContext } from 'react';
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
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  InlineLoading,
  InlineNotification,
  Tag,
  TreeView,
  TreeNode,
  OverflowMenu,
  OverflowMenuItem
} from '@carbon/react';
import { Add, Edit, TrashCan, View, ChevronRight, ChevronDown } from '@carbon/react/icons';
import { FormattedMessage, useIntl } from 'react-intl';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { ConfigurationContext } from '../common/ComponentContext';
import { getFromOpenElisServer, postToOpenElisServer } from '../utils/Utils';
import { NotificationContext } from '../common/ComponentContext';
import './ParameterGroupManagement.css';

const ParameterGroupManagement = () => {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const { notificationVisible, setNotificationVisible, addNotification } = useContext(NotificationContext);

  // Feature flag check
  const isComplianceModuleEnabled = configurationProperties?.['compliance.module.enabled'] === 'true';

  // State management
  const [parameterGroups, setParameterGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [formMode, setFormMode] = useState('create'); // 'create' | 'edit'
  const [saving, setSaving] = useState(false);
  const [expandedNodes, setExpandedNodes] = useState(new Set());
  const [viewMode, setViewMode] = useState('tree'); // 'tree' | 'table'

  // Feature flag guard
  if (!isComplianceModuleEnabled) {
    return (
      <Grid className="parameter-group-management__disabled">
        <Column lg={16} md={8} sm={4}>
          <div className="parameter-group-management__message">
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

  // Validation schema
  const validationSchema = Yup.object().shape({
    name: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.parameterGroup.form.validation.name.required',
        defaultMessage: 'Parameter group name is required'
      }))
      .min(3, intl.formatMessage({
        id: 'compliance.parameterGroup.form.validation.name.minLength',
        defaultMessage: 'Parameter group name must be at least 3 characters'
      }))
      .max(100, intl.formatMessage({
        id: 'compliance.parameterGroup.form.validation.name.maxLength',
        defaultMessage: 'Parameter group name cannot exceed 100 characters'
      })),
    description: Yup.string()
      .max(500, intl.formatMessage({
        id: 'compliance.parameterGroup.form.validation.description.maxLength',
        defaultMessage: 'Description cannot exceed 500 characters'
      })),
    parentGroupId: Yup.string()
      .nullable()
      .test('no-circular-reference', intl.formatMessage({
        id: 'compliance.parameterGroup.form.validation.parentGroup.circular',
        defaultMessage: 'Cannot select self or child group as parent'
      }), function(value) {
        if (!value || formMode === 'create') return true;
        // Prevent circular references
        const currentId = selectedGroup?.id;
        if (value === currentId) return false;
        // TODO: Add logic to check for child groups
        return true;
      })
  });

  // Formik setup
  const formik = useFormik({
    initialValues: {
      name: '',
      description: '',
      parentGroupId: '',
      isActive: true
    },
    validationSchema,
    onSubmit: async (values) => {
      await handleSaveGroup(values);
    }
  });

  // Load parameter groups
  const loadParameterGroups = async () => {
    try {
      setLoading(true);
      const response = await getFromOpenElisServer('/rest/compliance/parameter-groups');
      if (response) {
        setParameterGroups(response);
      }
    } catch (error) {
      console.error('Error loading parameter groups:', error);
      addNotification({
        kind: 'error',
        title: intl.formatMessage({
          id: 'compliance.parameterGroup.load.error.title',
          defaultMessage: 'Error Loading Parameter Groups'
        }),
        message: intl.formatMessage({
          id: 'compliance.parameterGroup.load.error.message',
          defaultMessage: 'Unable to load parameter groups. Please try again.'
        })
      });
    } finally {
      setLoading(false);
    }
  };

  // Load data on component mount
  useEffect(() => {
    loadParameterGroups();
  }, []);

  // Build hierarchical tree structure
  const buildTreeStructure = (groups, parentId = null) => {
    return groups
      .filter(group => group.parentGroupId === parentId)
      .map(group => ({
        ...group,
        children: buildTreeStructure(groups, group.id)
      }));
  };

  // Get filtered groups for display
  const getFilteredGroups = () => {
    if (!searchTerm) return parameterGroups;
    return parameterGroups.filter(group =>
      group.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (group.description && group.description.toLowerCase().includes(searchTerm.toLowerCase()))
    );
  };

  // Handle form submission
  const handleSaveGroup = async (values) => {
    try {
      setSaving(true);
      const endpoint = formMode === 'create'
        ? '/rest/compliance/parameter-groups'
        : `/rest/compliance/parameter-groups/${selectedGroup.id}`;

      const method = formMode === 'create' ? 'POST' : 'PUT';

      const payload = {
        ...values,
        parentGroupId: values.parentGroupId || null
      };

      await postToOpenElisServer(endpoint, JSON.stringify(payload), method);

      addNotification({
        kind: 'success',
        title: intl.formatMessage({
          id: `compliance.parameterGroup.${formMode}.success.title`,
          defaultMessage: formMode === 'create' ? 'Parameter Group Created' : 'Parameter Group Updated'
        }),
        message: intl.formatMessage({
          id: `compliance.parameterGroup.${formMode}.success.message`,
          defaultMessage: formMode === 'create'
            ? 'The parameter group has been created successfully.'
            : 'The parameter group has been updated successfully.'
        })
      });

      setIsFormModalOpen(false);
      resetForm();
      await loadParameterGroups();
    } catch (error) {
      console.error('Error saving parameter group:', error);
      addNotification({
        kind: 'error',
        title: intl.formatMessage({
          id: `compliance.parameterGroup.${formMode}.error.title`,
          defaultMessage: 'Error Saving Parameter Group'
        }),
        message: intl.formatMessage({
          id: `compliance.parameterGroup.${formMode}.error.message`,
          defaultMessage: 'Unable to save the parameter group. Please try again.'
        })
      });
    } finally {
      setSaving(false);
    }
  };

  // Handle delete
  const handleDeleteGroup = async () => {
    try {
      setSaving(true);
      await postToOpenElisServer(
        `/rest/compliance/parameter-groups/${selectedGroup.id}`,
        null,
        'DELETE'
      );

      addNotification({
        kind: 'success',
        title: intl.formatMessage({
          id: 'compliance.parameterGroup.delete.success.title',
          defaultMessage: 'Parameter Group Deleted'
        }),
        message: intl.formatMessage({
          id: 'compliance.parameterGroup.delete.success.message',
          defaultMessage: 'The parameter group has been deleted successfully.'
        })
      });

      setIsDeleteModalOpen(false);
      setSelectedGroup(null);
      await loadParameterGroups();
    } catch (error) {
      console.error('Error deleting parameter group:', error);
      addNotification({
        kind: 'error',
        title: intl.formatMessage({
          id: 'compliance.parameterGroup.delete.error.title',
          defaultMessage: 'Error Deleting Parameter Group'
        }),
        message: intl.formatMessage({
          id: 'compliance.parameterGroup.delete.error.message',
          defaultMessage: 'Unable to delete the parameter group. It may have dependent data.'
        })
      });
    } finally {
      setSaving(false);
    }
  };

  // Reset form
  const resetForm = () => {
    formik.resetForm();
    setSelectedGroup(null);
    setFormMode('create');
  };

  // Handle create new
  const handleCreateNew = () => {
    resetForm();
    setIsFormModalOpen(true);
  };

  // Handle edit
  const handleEdit = (group) => {
    setSelectedGroup(group);
    setFormMode('edit');
    formik.setValues({
      name: group.name || '',
      description: group.description || '',
      parentGroupId: group.parentGroupId || '',
      isActive: group.isActive !== false
    });
    setIsFormModalOpen(true);
  };

  // Handle view
  const handleView = (group) => {
    setSelectedGroup(group);
    setIsViewModalOpen(true);
  };

  // Handle delete confirmation
  const handleDeleteConfirm = (group) => {
    setSelectedGroup(group);
    setIsDeleteModalOpen(true);
  };

  // Toggle tree node expansion
  const toggleNodeExpansion = (nodeId) => {
    const newExpanded = new Set(expandedNodes);
    if (newExpanded.has(nodeId)) {
      newExpanded.delete(nodeId);
    } else {
      newExpanded.add(nodeId);
    }
    setExpandedNodes(newExpanded);
  };

  // Render tree node
  const renderTreeNode = (group) => {
    const hasChildren = group.children && group.children.length > 0;
    const isExpanded = expandedNodes.has(group.id);

    return (
      <TreeNode
        key={group.id}
        id={group.id}
        value={group.id}
        label={
          <div className="parameter-group-tree__node">
            <div className="parameter-group-tree__info">
              <span className="parameter-group-tree__name">{group.name}</span>
              {!group.isActive && (
                <Tag type="red" size="sm">
                  <FormattedMessage
                    id="compliance.parameterGroup.status.inactive"
                    defaultMessage="Inactive"
                  />
                </Tag>
              )}
            </div>
            <div className="parameter-group-tree__actions">
              <OverflowMenu ariaLabel="Parameter group actions" size="sm">
                <OverflowMenuItem
                  itemText={intl.formatMessage({
                    id: 'compliance.parameterGroup.action.view',
                    defaultMessage: 'View'
                  })}
                  onClick={() => handleView(group)}
                />
                <OverflowMenuItem
                  itemText={intl.formatMessage({
                    id: 'compliance.parameterGroup.action.edit',
                    defaultMessage: 'Edit'
                  })}
                  onClick={() => handleEdit(group)}
                />
                <OverflowMenuItem
                  itemText={intl.formatMessage({
                    id: 'compliance.parameterGroup.action.delete',
                    defaultMessage: 'Delete'
                  })}
                  hasDivider
                  isDelete
                  onClick={() => handleDeleteConfirm(group)}
                />
              </OverflowMenu>
            </div>
          </div>
        }
        isExpanded={isExpanded}
        onToggle={() => toggleNodeExpansion(group.id)}
      >
        {hasChildren && group.children.map(child => renderTreeNode(child))}
      </TreeNode>
    );
  };

  // Table headers
  const tableHeaders = [
    {
      key: 'name',
      header: intl.formatMessage({
        id: 'compliance.parameterGroup.table.header.name',
        defaultMessage: 'Name'
      })
    },
    {
      key: 'description',
      header: intl.formatMessage({
        id: 'compliance.parameterGroup.table.header.description',
        defaultMessage: 'Description'
      })
    },
    {
      key: 'parentGroup',
      header: intl.formatMessage({
        id: 'compliance.parameterGroup.table.header.parentGroup',
        defaultMessage: 'Parent Group'
      })
    },
    {
      key: 'status',
      header: intl.formatMessage({
        id: 'compliance.parameterGroup.table.header.status',
        defaultMessage: 'Status'
      })
    },
    {
      key: 'actions',
      header: intl.formatMessage({
        id: 'compliance.parameterGroup.table.header.actions',
        defaultMessage: 'Actions'
      })
    }
  ];

  // Prepare table data
  const tableData = getFilteredGroups().map(group => ({
    ...group,
    parentGroup: group.parentGroupId
      ? parameterGroups.find(p => p.id === group.parentGroupId)?.name || 'Unknown'
      : '',
    status: group.isActive ? 'Active' : 'Inactive'
  }));

  const treeData = buildTreeStructure(getFilteredGroups());

  if (loading) {
    return (
      <Grid className="parameter-group-management">
        <Column lg={16}>
          <InlineLoading description={intl.formatMessage({
            id: 'compliance.parameterGroup.loading',
            defaultMessage: 'Loading parameter groups...'
          })} />
        </Column>
      </Grid>
    );
  }

  return (
    <Grid className="parameter-group-management">
      <Column lg={16}>
        {/* Header */}
        <div className="parameter-group-management__header">
          <div className="parameter-group-management__title-section">
            <h1>
              <FormattedMessage
                id="compliance.parameterGroup.title"
                defaultMessage="Parameter Group Management"
              />
            </h1>
            <p>
              <FormattedMessage
                id="compliance.parameterGroup.description"
                defaultMessage="Organize testing parameters into hierarchical groups for compliance evaluation"
              />
            </p>
          </div>
          <div className="parameter-group-management__actions">
            <Button
              kind="primary"
              renderIcon={Add}
              onClick={handleCreateNew}
            >
              <FormattedMessage
                id="compliance.parameterGroup.action.create"
                defaultMessage="Create Parameter Group"
              />
            </Button>
            <Button
              kind="secondary"
              onClick={() => setViewMode(viewMode === 'tree' ? 'table' : 'tree')}
            >
              <FormattedMessage
                id={`compliance.parameterGroup.view.${viewMode === 'tree' ? 'table' : 'tree'}`}
                defaultMessage={viewMode === 'tree' ? 'Table View' : 'Tree View'}
              />
            </Button>
          </div>
        </div>

        {/* Search */}
        <div className="parameter-group-management__search">
          <Search
            size="lg"
            placeholder={intl.formatMessage({
              id: 'compliance.parameterGroup.search.placeholder',
              defaultMessage: 'Search parameter groups...'
            })}
            labelText={intl.formatMessage({
              id: 'compliance.parameterGroup.search.label',
              defaultMessage: 'Search'
            })}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onClear={() => setSearchTerm('')}
          />
        </div>

        {/* Content */}
        {viewMode === 'tree' ? (
          /* Tree View */
          <div className="parameter-group-management__tree">
            {treeData.length > 0 ? (
              <TreeView
                hideLabel
                label="Parameter Groups"
                selected={[]}
                onSelect={() => {}}
              >
                {treeData.map(group => renderTreeNode(group))}
              </TreeView>
            ) : (
              <div className="parameter-group-management__empty">
                <FormattedMessage
                  id="compliance.parameterGroup.empty.message"
                  defaultMessage="No parameter groups found. Create your first parameter group to get started."
                />
              </div>
            )}
          </div>
        ) : (
          /* Table View */
          <div className="parameter-group-management__table">
            <DataTable
              rows={tableData}
              headers={tableHeaders}
              render={({
                rows,
                headers,
                getHeaderProps,
                getRowProps,
                getTableProps,
                getTableContainerProps
              }) => (
                <TableContainer
                  title={intl.formatMessage({
                    id: 'compliance.parameterGroup.table.title',
                    defaultMessage: 'Parameter Groups'
                  })}
                  {...getTableContainerProps()}
                >
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        {headers.map(header => (
                          <TableHeader {...getHeaderProps({ header })}>
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map(row => (
                        <TableRow {...getRowProps({ row })}>
                          {row.cells.map(cell => {
                            if (cell.info.header === 'status') {
                              return (
                                <TableCell key={cell.id}>
                                  <Tag
                                    type={cell.value === 'Active' ? 'green' : 'red'}
                                    size="sm"
                                  >
                                    {cell.value === 'Active' ? (
                                      <FormattedMessage
                                        id="compliance.parameterGroup.status.active"
                                        defaultMessage="Active"
                                      />
                                    ) : (
                                      <FormattedMessage
                                        id="compliance.parameterGroup.status.inactive"
                                        defaultMessage="Inactive"
                                      />
                                    )}
                                  </Tag>
                                </TableCell>
                              );
                            } else if (cell.info.header === 'actions') {
                              const group = parameterGroups.find(g => g.id === row.id);
                              return (
                                <TableCell key={cell.id}>
                                  <div className="parameter-group__actions">
                                    <Button
                                      kind="ghost"
                                      size="sm"
                                      renderIcon={View}
                                      onClick={() => handleView(group)}
                                      iconDescription={intl.formatMessage({
                                        id: 'compliance.parameterGroup.action.view',
                                        defaultMessage: 'View'
                                      })}
                                      hasIconOnly
                                    />
                                    <Button
                                      kind="ghost"
                                      size="sm"
                                      renderIcon={Edit}
                                      onClick={() => handleEdit(group)}
                                      iconDescription={intl.formatMessage({
                                        id: 'compliance.parameterGroup.action.edit',
                                        defaultMessage: 'Edit'
                                      })}
                                      hasIconOnly
                                    />
                                    <Button
                                      kind="danger--ghost"
                                      size="sm"
                                      renderIcon={TrashCan}
                                      onClick={() => handleDeleteConfirm(group)}
                                      iconDescription={intl.formatMessage({
                                        id: 'compliance.parameterGroup.action.delete',
                                        defaultMessage: 'Delete'
                                      })}
                                      hasIconOnly
                                    />
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
          </div>
        )}

        {/* Create/Edit Modal */}
        <Modal
          open={isFormModalOpen}
          onRequestClose={() => {
            if (!saving) {
              setIsFormModalOpen(false);
              resetForm();
            }
          }}
          modalHeading={intl.formatMessage({
            id: `compliance.parameterGroup.form.title.${formMode}`,
            defaultMessage: formMode === 'create' ? 'Create Parameter Group' : 'Edit Parameter Group'
          })}
          primaryButtonText={saving ? intl.formatMessage({
            id: 'compliance.parameterGroup.form.saving',
            defaultMessage: 'Saving...'
          }) : intl.formatMessage({
            id: `compliance.parameterGroup.form.${formMode}`,
            defaultMessage: formMode === 'create' ? 'Create' : 'Update'
          })}
          secondaryButtonText={intl.formatMessage({
            id: 'compliance.parameterGroup.form.cancel',
            defaultMessage: 'Cancel'
          })}
          onRequestSubmit={formik.handleSubmit}
          primaryButtonDisabled={saving || !formik.isValid}
        >
          <div className="parameter-group-form">
            <TextInput
              id="name"
              name="name"
              labelText={intl.formatMessage({
                id: 'compliance.parameterGroup.form.name.label',
                defaultMessage: 'Parameter Group Name'
              })}
              placeholder={intl.formatMessage({
                id: 'compliance.parameterGroup.form.name.placeholder',
                defaultMessage: 'Enter parameter group name'
              })}
              value={formik.values.name}
              onChange={formik.handleChange}
              onBlur={formik.handleBlur}
              invalid={formik.touched.name && !!formik.errors.name}
              invalidText={formik.touched.name && formik.errors.name}
              helperText={intl.formatMessage({
                id: 'compliance.parameterGroup.form.name.help',
                defaultMessage: 'A descriptive name for the parameter group'
              })}
            />

            <TextArea
              id="description"
              name="description"
              labelText={intl.formatMessage({
                id: 'compliance.parameterGroup.form.description.label',
                defaultMessage: 'Description'
              })}
              placeholder={intl.formatMessage({
                id: 'compliance.parameterGroup.form.description.placeholder',
                defaultMessage: 'Describe the purpose of this parameter group'
              })}
              value={formik.values.description}
              onChange={formik.handleChange}
              onBlur={formik.handleBlur}
              invalid={formik.touched.description && !!formik.errors.description}
              invalidText={formik.touched.description && formik.errors.description}
              rows={3}
              maxCount={500}
              enableCounter
            />

            <Select
              id="parentGroupId"
              name="parentGroupId"
              labelText={intl.formatMessage({
                id: 'compliance.parameterGroup.form.parentGroup.label',
                defaultMessage: 'Parent Group'
              })}
              value={formik.values.parentGroupId}
              onChange={formik.handleChange}
              onBlur={formik.handleBlur}
              invalid={formik.touched.parentGroupId && !!formik.errors.parentGroupId}
              invalidText={formik.touched.parentGroupId && formik.errors.parentGroupId}
              helperText={intl.formatMessage({
                id: 'compliance.parameterGroup.form.parentGroup.help',
                defaultMessage: 'Optional: Select a parent group to create hierarchy'
              })}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: 'compliance.parameterGroup.form.parentGroup.none',
                  defaultMessage: 'No parent group (root level)'
                })}
              />
              {parameterGroups
                .filter(group => formMode === 'create' || group.id !== selectedGroup?.id)
                .map(group => (
                  <SelectItem
                    key={group.id}
                    value={group.id}
                    text={group.name}
                  />
                ))}
            </Select>
          </div>
        </Modal>

        {/* View Modal */}
        <Modal
          open={isViewModalOpen}
          onRequestClose={() => setIsViewModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: 'compliance.parameterGroup.view.title',
            defaultMessage: 'Parameter Group Details'
          })}
          passiveModal
        >
          {selectedGroup && (
            <div className="parameter-group-view">
              <p>
                <strong>
                  <FormattedMessage
                    id="compliance.parameterGroup.view.name"
                    defaultMessage="Name:"
                  />
                </strong>{' '}
                {selectedGroup.name}
              </p>
              <p>
                <strong>
                  <FormattedMessage
                    id="compliance.parameterGroup.view.description"
                    defaultMessage="Description:"
                  />
                </strong>{' '}
                {selectedGroup.description || intl.formatMessage({
                  id: 'compliance.parameterGroup.view.noDescription',
                  defaultMessage: 'No description provided'
                })}
              </p>
              <p>
                <strong>
                  <FormattedMessage
                    id="compliance.parameterGroup.view.parentGroup"
                    defaultMessage="Parent Group:"
                  />
                </strong>{' '}
                {selectedGroup.parentGroupId
                  ? parameterGroups.find(p => p.id === selectedGroup.parentGroupId)?.name || 'Unknown'
                  : intl.formatMessage({
                      id: 'compliance.parameterGroup.view.rootLevel',
                      defaultMessage: 'Root level'
                    })}
              </p>
              <p>
                <strong>
                  <FormattedMessage
                    id="compliance.parameterGroup.view.status"
                    defaultMessage="Status:"
                  />
                </strong>{' '}
                <Tag
                  type={selectedGroup.isActive ? 'green' : 'red'}
                  size="sm"
                >
                  {selectedGroup.isActive ? (
                    <FormattedMessage
                      id="compliance.parameterGroup.status.active"
                      defaultMessage="Active"
                    />
                  ) : (
                    <FormattedMessage
                      id="compliance.parameterGroup.status.inactive"
                      defaultMessage="Inactive"
                    />
                  )}
                </Tag>
              </p>
            </div>
          )}
        </Modal>

        {/* Delete Confirmation Modal */}
        <Modal
          danger
          open={isDeleteModalOpen}
          onRequestClose={() => {
            if (!saving) {
              setIsDeleteModalOpen(false);
              setSelectedGroup(null);
            }
          }}
          modalHeading={intl.formatMessage({
            id: 'compliance.parameterGroup.delete.title',
            defaultMessage: 'Delete Parameter Group'
          })}
          primaryButtonText={saving ? intl.formatMessage({
            id: 'compliance.parameterGroup.delete.deleting',
            defaultMessage: 'Deleting...'
          }) : intl.formatMessage({
            id: 'compliance.parameterGroup.delete.confirm',
            defaultMessage: 'Delete'
          })}
          secondaryButtonText={intl.formatMessage({
            id: 'compliance.parameterGroup.delete.cancel',
            defaultMessage: 'Cancel'
          })}
          onRequestSubmit={handleDeleteGroup}
          primaryButtonDisabled={saving}
        >
          <p>
            <FormattedMessage
              id="compliance.parameterGroup.delete.message"
              defaultMessage="Are you sure you want to delete the parameter group '{name}'? This action cannot be undone."
              values={{ name: selectedGroup?.name }}
            />
          </p>
          {selectedGroup && (
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: 'compliance.parameterGroup.delete.warning.title',
                defaultMessage: 'Warning'
              })}
              subtitle={intl.formatMessage({
                id: 'compliance.parameterGroup.delete.warning.message',
                defaultMessage: 'Deleting this parameter group may affect compliance evaluations that reference it.'
              })}
              hideCloseButton
            />
          )}
        </Modal>
      </Column>
    </Grid>
  );
};

export default ParameterGroupManagement;