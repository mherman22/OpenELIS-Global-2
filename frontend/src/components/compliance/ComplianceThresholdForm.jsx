import React, { useState, useEffect, useContext } from 'react';
import {
  Grid,
  Column,
  Button,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  NumberInput,
  Toggle,
  InlineLoading,
  InlineNotification,
  Modal,
  FormGroup,
  RadioButtonGroup,
  RadioButton,
  Accordion,
  AccordionItem,
  Tag
} from '@carbon/react';
import { Save, Reset, Close, Add } from '@carbon/react/icons';
import { FormattedMessage, useIntl } from 'react-intl';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { ConfigurationContext, NotificationContext } from '../common/ComponentContext';
import { getFromOpenElisServer, postToOpenElisServer } from '../utils/Utils';
import './ComplianceThresholdForm.css';

const ComplianceThresholdForm = ({
  thresholdId = null,
  complianceStandardId = null,
  onSave = () => {},
  onCancel = () => {},
  readOnly = false
}) => {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const { addNotification } = useContext(NotificationContext);

  // Feature flag check
  const isComplianceModuleEnabled = configurationProperties?.['compliance.module.enabled'] === 'true';

  // State management
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [complianceStandards, setComplianceStandards] = useState([]);
  const [parameterGroups, setParameterGroups] = useState([]);
  const [testParameters, setTestParameters] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [units, setUnits] = useState([]);
  const [isAdvancedExpanded, setIsAdvancedExpanded] = useState(false);

  // Feature flag guard
  if (!isComplianceModuleEnabled) {
    return (
      <Grid className="compliance-threshold-form__disabled">
        <Column lg={16} md={8} sm={4}>
          <div className="compliance-threshold-form__message">
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
    complianceStandardId: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.threshold.form.validation.standard.required',
        defaultMessage: 'Compliance standard is required'
      })),
    parameterName: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.threshold.form.validation.parameter.required',
        defaultMessage: 'Parameter name is required'
      }))
      .min(2, intl.formatMessage({
        id: 'compliance.threshold.form.validation.parameter.minLength',
        defaultMessage: 'Parameter name must be at least 2 characters'
      }))
      .max(100, intl.formatMessage({
        id: 'compliance.threshold.form.validation.parameter.maxLength',
        defaultMessage: 'Parameter name cannot exceed 100 characters'
      })),
    thresholdType: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.threshold.form.validation.type.required',
        defaultMessage: 'Threshold type is required'
      }))
      .oneOf(['MAXIMUM', 'MINIMUM', 'RANGE', 'EXACT'], intl.formatMessage({
        id: 'compliance.threshold.form.validation.type.invalid',
        defaultMessage: 'Invalid threshold type'
      })),
    minValue: Yup.number()
      .nullable()
      .when(['thresholdType'], (thresholdType, schema) => {
        if (thresholdType && ['MINIMUM', 'RANGE'].includes(thresholdType[0])) {
          return schema.required(intl.formatMessage({
            id: 'compliance.threshold.form.validation.minValue.required',
            defaultMessage: 'Minimum value is required for this threshold type'
          }));
        }
        return schema;
      })
      .test('min-less-than-max', intl.formatMessage({
        id: 'compliance.threshold.form.validation.minValue.lessThanMax',
        defaultMessage: 'Minimum value must be less than maximum value'
      }), function(value) {
        const { maxValue } = this.parent;
        if (value != null && maxValue != null) {
          return parseFloat(value) < parseFloat(maxValue);
        }
        return true;
      }),
    maxValue: Yup.number()
      .nullable()
      .when(['thresholdType'], (thresholdType, schema) => {
        if (thresholdType && ['MAXIMUM', 'RANGE'].includes(thresholdType[0])) {
          return schema.required(intl.formatMessage({
            id: 'compliance.threshold.form.validation.maxValue.required',
            defaultMessage: 'Maximum value is required for this threshold type'
          }));
        }
        return schema;
      }),
    exactValue: Yup.number()
      .nullable()
      .when(['thresholdType'], (thresholdType, schema) => {
        if (thresholdType && thresholdType[0] === 'EXACT') {
          return schema.required(intl.formatMessage({
            id: 'compliance.threshold.form.validation.exactValue.required',
            defaultMessage: 'Exact value is required for this threshold type'
          }));
        }
        return schema;
      }),
    unit: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.threshold.form.validation.unit.required',
        defaultMessage: 'Unit is required'
      })),
    criticalityLevel: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.threshold.form.validation.criticality.required',
        defaultMessage: 'Criticality level is required'
      }))
      .oneOf(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'], intl.formatMessage({
        id: 'compliance.threshold.form.validation.criticality.invalid',
        defaultMessage: 'Invalid criticality level'
      })),
    tolerance: Yup.number()
      .nullable()
      .min(0, intl.formatMessage({
        id: 'compliance.threshold.form.validation.tolerance.min',
        defaultMessage: 'Tolerance cannot be negative'
      }))
      .max(100, intl.formatMessage({
        id: 'compliance.threshold.form.validation.tolerance.max',
        defaultMessage: 'Tolerance cannot exceed 100%'
      })),
    description: Yup.string()
      .max(500, intl.formatMessage({
        id: 'compliance.threshold.form.validation.description.maxLength',
        defaultMessage: 'Description cannot exceed 500 characters'
      }))
  });

  // Formik setup
  const formik = useFormik({
    initialValues: {
      complianceStandardId: complianceStandardId || '',
      parameterGroupId: '',
      parameterName: '',
      thresholdType: 'MAXIMUM',
      minValue: '',
      maxValue: '',
      exactValue: '',
      unit: '',
      criticalityLevel: 'MEDIUM',
      tolerance: '',
      sampleTypeId: '',
      methodReference: '',
      regulatoryBasis: '',
      description: '',
      isActive: true,
      requiresConfirmation: false
    },
    validationSchema,
    onSubmit: async (values) => {
      await handleSaveThreshold(values);
    }
  });

  // Load form data
  const loadFormData = async () => {
    try {
      setLoading(true);

      const [
        standardsResponse,
        groupsResponse,
        parametersResponse,
        sampleTypesResponse,
        unitsResponse
      ] = await Promise.all([
        getFromOpenElisServer('/rest/compliance/standards/active'),
        getFromOpenElisServer('/rest/compliance/parameter-groups'),
        getFromOpenElisServer('/rest/test-parameters'),
        getFromOpenElisServer('/rest/sample-types'),
        getFromOpenElisServer('/rest/units')
      ]);

      if (standardsResponse) setComplianceStandards(standardsResponse);
      if (groupsResponse) setParameterGroups(groupsResponse);
      if (parametersResponse) setTestParameters(parametersResponse);
      if (sampleTypesResponse) setSampleTypes(sampleTypesResponse);
      if (unitsResponse) setUnits(unitsResponse);

      // Load existing threshold data if editing
      if (thresholdId) {
        const thresholdResponse = await getFromOpenElisServer(`/rest/compliance/thresholds/${thresholdId}`);
        if (thresholdResponse) {
          formik.setValues({
            complianceStandardId: thresholdResponse.complianceStandardId || '',
            parameterGroupId: thresholdResponse.parameterGroupId || '',
            parameterName: thresholdResponse.parameterName || '',
            thresholdType: thresholdResponse.thresholdType || 'MAXIMUM',
            minValue: thresholdResponse.minValue || '',
            maxValue: thresholdResponse.maxValue || '',
            exactValue: thresholdResponse.exactValue || '',
            unit: thresholdResponse.unit || '',
            criticalityLevel: thresholdResponse.criticalityLevel || 'MEDIUM',
            tolerance: thresholdResponse.tolerance || '',
            sampleTypeId: thresholdResponse.sampleTypeId || '',
            methodReference: thresholdResponse.methodReference || '',
            regulatoryBasis: thresholdResponse.regulatoryBasis || '',
            description: thresholdResponse.description || '',
            isActive: thresholdResponse.isActive !== false,
            requiresConfirmation: thresholdResponse.requiresConfirmation === true
          });
        }
      }
    } catch (error) {
      console.error('Error loading form data:', error);
      addNotification({
        kind: 'error',
        title: intl.formatMessage({
          id: 'compliance.threshold.form.load.error.title',
          defaultMessage: 'Error Loading Form Data'
        }),
        message: intl.formatMessage({
          id: 'compliance.threshold.form.load.error.message',
          defaultMessage: 'Unable to load form data. Please refresh the page.'
        })
      });
    } finally {
      setLoading(false);
    }
  };

  // Load data on component mount
  useEffect(() => {
    loadFormData();
  }, [thresholdId]);

  // Handle save
  const handleSaveThreshold = async (values) => {
    try {
      setSaving(true);

      const endpoint = thresholdId
        ? `/rest/compliance/thresholds/${thresholdId}`
        : '/rest/compliance/thresholds';

      const method = thresholdId ? 'PUT' : 'POST';

      // Prepare payload based on threshold type
      const payload = {
        ...values,
        minValue: ['MINIMUM', 'RANGE'].includes(values.thresholdType) ? parseFloat(values.minValue) : null,
        maxValue: ['MAXIMUM', 'RANGE'].includes(values.thresholdType) ? parseFloat(values.maxValue) : null,
        exactValue: values.thresholdType === 'EXACT' ? parseFloat(values.exactValue) : null,
        tolerance: values.tolerance ? parseFloat(values.tolerance) : null,
        parameterGroupId: values.parameterGroupId || null,
        sampleTypeId: values.sampleTypeId || null
      };

      const response = await postToOpenElisServer(endpoint, JSON.stringify(payload), method);

      addNotification({
        kind: 'success',
        title: intl.formatMessage({
          id: `compliance.threshold.${thresholdId ? 'update' : 'create'}.success.title`,
          defaultMessage: thresholdId ? 'Threshold Updated' : 'Threshold Created'
        }),
        message: intl.formatMessage({
          id: `compliance.threshold.${thresholdId ? 'update' : 'create'}.success.message`,
          defaultMessage: thresholdId
            ? 'The compliance threshold has been updated successfully.'
            : 'The compliance threshold has been created successfully.'
        })
      });

      onSave(response);
    } catch (error) {
      console.error('Error saving threshold:', error);
      addNotification({
        kind: 'error',
        title: intl.formatMessage({
          id: 'compliance.threshold.save.error.title',
          defaultMessage: 'Error Saving Threshold'
        }),
        message: intl.formatMessage({
          id: 'compliance.threshold.save.error.message',
          defaultMessage: 'Unable to save the compliance threshold. Please try again.'
        })
      });
    } finally {
      setSaving(false);
    }
  };

  // Handle reset form
  const handleReset = () => {
    formik.resetForm();
    if (thresholdId) {
      loadFormData(); // Reload original data
    }
  };

  // Get threshold type description
  const getThresholdTypeDescription = (type) => {
    const descriptions = {
      MAXIMUM: intl.formatMessage({
        id: 'compliance.threshold.type.maximum.description',
        defaultMessage: 'Value must not exceed the maximum limit'
      }),
      MINIMUM: intl.formatMessage({
        id: 'compliance.threshold.type.minimum.description',
        defaultMessage: 'Value must meet or exceed the minimum limit'
      }),
      RANGE: intl.formatMessage({
        id: 'compliance.threshold.type.range.description',
        defaultMessage: 'Value must fall within the specified range'
      }),
      EXACT: intl.formatMessage({
        id: 'compliance.threshold.type.exact.description',
        defaultMessage: 'Value must match the exact specified value'
      })
    };
    return descriptions[type] || '';
  };

  // Get criticality color
  const getCriticalityTag = (level) => {
    const configs = {
      LOW: { type: 'blue', text: intl.formatMessage({ id: 'compliance.threshold.criticality.low', defaultMessage: 'Low' }) },
      MEDIUM: { type: 'cyan', text: intl.formatMessage({ id: 'compliance.threshold.criticality.medium', defaultMessage: 'Medium' }) },
      HIGH: { type: 'yellow', text: intl.formatMessage({ id: 'compliance.threshold.criticality.high', defaultMessage: 'High' }) },
      CRITICAL: { type: 'red', text: intl.formatMessage({ id: 'compliance.threshold.criticality.critical', defaultMessage: 'Critical' }) }
    };

    const config = configs[level] || configs.MEDIUM;
    return (
      <Tag type={config.type} size="sm">
        {config.text}
      </Tag>
    );
  };

  if (loading) {
    return (
      <Grid className="compliance-threshold-form">
        <Column lg={16}>
          <InlineLoading description={intl.formatMessage({
            id: 'compliance.threshold.form.loading',
            defaultMessage: 'Loading threshold form...'
          })} />
        </Column>
      </Grid>
    );
  }

  return (
    <Grid className="compliance-threshold-form">
      <Column lg={16}>
        {/* Header */}
        <div className="compliance-threshold-form__header">
          <h1>
            <FormattedMessage
              id={`compliance.threshold.form.title.${thresholdId ? 'edit' : 'create'}`}
              defaultMessage={thresholdId ? 'Edit Compliance Threshold' : 'Create Compliance Threshold'}
            />
          </h1>
          <p>
            <FormattedMessage
              id="compliance.threshold.form.description"
              defaultMessage="Define parameter-specific compliance limits and evaluation criteria"
            />
          </p>
        </div>

        <form onSubmit={formik.handleSubmit}>
          {/* Basic Information */}
          <div className="compliance-threshold-form__section">
            <h2>
              <FormattedMessage
                id="compliance.threshold.form.section.basic"
                defaultMessage="Basic Information"
              />
            </h2>

            <Grid>
              <Column lg={8} md={4} sm={2}>
                <Select
                  id="complianceStandardId"
                  name="complianceStandardId"
                  labelText={intl.formatMessage({
                    id: 'compliance.threshold.form.standard.label',
                    defaultMessage: 'Compliance Standard'
                  })}
                  value={formik.values.complianceStandardId}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  invalid={formik.touched.complianceStandardId && !!formik.errors.complianceStandardId}
                  invalidText={formik.touched.complianceStandardId && formik.errors.complianceStandardId}
                  disabled={readOnly}
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: 'compliance.threshold.form.standard.select',
                      defaultMessage: 'Select compliance standard'
                    })}
                  />
                  {complianceStandards.map(standard => (
                    <SelectItem
                      key={standard.id}
                      value={standard.id}
                      text={`${standard.name} (${standard.organizationName})`}
                    />
                  ))}
                </Select>
              </Column>

              <Column lg={8} md={4} sm={2}>
                <Select
                  id="parameterGroupId"
                  name="parameterGroupId"
                  labelText={intl.formatMessage({
                    id: 'compliance.threshold.form.parameterGroup.label',
                    defaultMessage: 'Parameter Group (Optional)'
                  })}
                  value={formik.values.parameterGroupId}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  disabled={readOnly}
                  helperText={intl.formatMessage({
                    id: 'compliance.threshold.form.parameterGroup.help',
                    defaultMessage: 'Group this parameter for organizational purposes'
                  })}
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: 'compliance.threshold.form.parameterGroup.none',
                      defaultMessage: 'No parameter group'
                    })}
                  />
                  {parameterGroups.map(group => (
                    <SelectItem
                      key={group.id}
                      value={group.id}
                      text={group.name}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>

            <Grid>
              <Column lg={12} md={6} sm={4}>
                <TextInput
                  id="parameterName"
                  name="parameterName"
                  labelText={intl.formatMessage({
                    id: 'compliance.threshold.form.parameter.label',
                    defaultMessage: 'Parameter Name'
                  })}
                  placeholder={intl.formatMessage({
                    id: 'compliance.threshold.form.parameter.placeholder',
                    defaultMessage: 'Enter parameter name (e.g., pH, Turbidity, Chlorine)'
                  })}
                  value={formik.values.parameterName}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  invalid={formik.touched.parameterName && !!formik.errors.parameterName}
                  invalidText={formik.touched.parameterName && formik.errors.parameterName}
                  disabled={readOnly}
                />
              </Column>

              <Column lg={4} md={2} sm={2}>
                <Select
                  id="unit"
                  name="unit"
                  labelText={intl.formatMessage({
                    id: 'compliance.threshold.form.unit.label',
                    defaultMessage: 'Unit'
                  })}
                  value={formik.values.unit}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  invalid={formik.touched.unit && !!formik.errors.unit}
                  invalidText={formik.touched.unit && formik.errors.unit}
                  disabled={readOnly}
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: 'compliance.threshold.form.unit.select',
                      defaultMessage: 'Select unit'
                    })}
                  />
                  {units.map(unit => (
                    <SelectItem
                      key={unit.id}
                      value={unit.name}
                      text={unit.name}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>
          </div>

          {/* Threshold Configuration */}
          <div className="compliance-threshold-form__section">
            <h2>
              <FormattedMessage
                id="compliance.threshold.form.section.threshold"
                defaultMessage="Threshold Configuration"
              />
            </h2>

            <Grid>
              <Column lg={8} md={4} sm={2}>
                <FormGroup>
                  <RadioButtonGroup
                    name="thresholdType"
                    legendText={intl.formatMessage({
                      id: 'compliance.threshold.form.type.label',
                      defaultMessage: 'Threshold Type'
                    })}
                    value={formik.values.thresholdType}
                    onChange={(value) => {
                      formik.setFieldValue('thresholdType', value);
                      // Reset value fields when type changes
                      formik.setFieldValue('minValue', '');
                      formik.setFieldValue('maxValue', '');
                      formik.setFieldValue('exactValue', '');
                    }}
                    disabled={readOnly}
                  >
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: 'compliance.threshold.type.maximum',
                        defaultMessage: 'Maximum'
                      })}
                      value="MAXIMUM"
                      id="threshold-type-maximum"
                    />
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: 'compliance.threshold.type.minimum',
                        defaultMessage: 'Minimum'
                      })}
                      value="MINIMUM"
                      id="threshold-type-minimum"
                    />
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: 'compliance.threshold.type.range',
                        defaultMessage: 'Range'
                      })}
                      value="RANGE"
                      id="threshold-type-range"
                    />
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: 'compliance.threshold.type.exact',
                        defaultMessage: 'Exact Value'
                      })}
                      value="EXACT"
                      id="threshold-type-exact"
                    />
                  </RadioButtonGroup>
                  <div className="compliance-threshold-form__type-description">
                    {getThresholdTypeDescription(formik.values.thresholdType)}
                  </div>
                </FormGroup>
              </Column>

              <Column lg={8} md={4} sm={2}>
                <div className="compliance-threshold-form__values">
                  {/* Maximum Value */}
                  {['MAXIMUM', 'RANGE'].includes(formik.values.thresholdType) && (
                    <NumberInput
                      id="maxValue"
                      name="maxValue"
                      label={intl.formatMessage({
                        id: 'compliance.threshold.form.maxValue.label',
                        defaultMessage: 'Maximum Value'
                      })}
                      placeholder={intl.formatMessage({
                        id: 'compliance.threshold.form.maxValue.placeholder',
                        defaultMessage: 'Enter maximum allowed value'
                      })}
                      value={formik.values.maxValue}
                      onChange={(e) => formik.setFieldValue('maxValue', e.target.value)}
                      onBlur={formik.handleBlur}
                      invalid={formik.touched.maxValue && !!formik.errors.maxValue}
                      invalidText={formik.touched.maxValue && formik.errors.maxValue}
                      disabled={readOnly}
                      allowEmpty
                      step="any"
                    />
                  )}

                  {/* Minimum Value */}
                  {['MINIMUM', 'RANGE'].includes(formik.values.thresholdType) && (
                    <NumberInput
                      id="minValue"
                      name="minValue"
                      label={intl.formatMessage({
                        id: 'compliance.threshold.form.minValue.label',
                        defaultMessage: 'Minimum Value'
                      })}
                      placeholder={intl.formatMessage({
                        id: 'compliance.threshold.form.minValue.placeholder',
                        defaultMessage: 'Enter minimum required value'
                      })}
                      value={formik.values.minValue}
                      onChange={(e) => formik.setFieldValue('minValue', e.target.value)}
                      onBlur={formik.handleBlur}
                      invalid={formik.touched.minValue && !!formik.errors.minValue}
                      invalidText={formik.touched.minValue && formik.errors.minValue}
                      disabled={readOnly}
                      allowEmpty
                      step="any"
                    />
                  )}

                  {/* Exact Value */}
                  {formik.values.thresholdType === 'EXACT' && (
                    <NumberInput
                      id="exactValue"
                      name="exactValue"
                      label={intl.formatMessage({
                        id: 'compliance.threshold.form.exactValue.label',
                        defaultMessage: 'Exact Value'
                      })}
                      placeholder={intl.formatMessage({
                        id: 'compliance.threshold.form.exactValue.placeholder',
                        defaultMessage: 'Enter exact required value'
                      })}
                      value={formik.values.exactValue}
                      onChange={(e) => formik.setFieldValue('exactValue', e.target.value)}
                      onBlur={formik.handleBlur}
                      invalid={formik.touched.exactValue && !!formik.errors.exactValue}
                      invalidText={formik.touched.exactValue && formik.errors.exactValue}
                      disabled={readOnly}
                      allowEmpty
                      step="any"
                    />
                  )}
                </div>
              </Column>
            </Grid>

            <Grid>
              <Column lg={4} md={2} sm={2}>
                <Select
                  id="criticalityLevel"
                  name="criticalityLevel"
                  labelText={intl.formatMessage({
                    id: 'compliance.threshold.form.criticality.label',
                    defaultMessage: 'Criticality Level'
                  })}
                  value={formik.values.criticalityLevel}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  invalid={formik.touched.criticalityLevel && !!formik.errors.criticalityLevel}
                  invalidText={formik.touched.criticalityLevel && formik.errors.criticalityLevel}
                  disabled={readOnly}
                  helperText={intl.formatMessage({
                    id: 'compliance.threshold.form.criticality.help',
                    defaultMessage: 'Impact level when threshold is exceeded'
                  })}
                >
                  <SelectItem value="LOW" text={intl.formatMessage({ id: 'compliance.threshold.criticality.low', defaultMessage: 'Low' })} />
                  <SelectItem value="MEDIUM" text={intl.formatMessage({ id: 'compliance.threshold.criticality.medium', defaultMessage: 'Medium' })} />
                  <SelectItem value="HIGH" text={intl.formatMessage({ id: 'compliance.threshold.criticality.high', defaultMessage: 'High' })} />
                  <SelectItem value="CRITICAL" text={intl.formatMessage({ id: 'compliance.threshold.criticality.critical', defaultMessage: 'Critical' })} />
                </Select>
              </Column>

              <Column lg={4} md={2} sm={2}>
                <NumberInput
                  id="tolerance"
                  name="tolerance"
                  label={intl.formatMessage({
                    id: 'compliance.threshold.form.tolerance.label',
                    defaultMessage: 'Tolerance (%)'
                  })}
                  placeholder={intl.formatMessage({
                    id: 'compliance.threshold.form.tolerance.placeholder',
                    defaultMessage: 'Enter tolerance percentage'
                  })}
                  value={formik.values.tolerance}
                  onChange={(e) => formik.setFieldValue('tolerance', e.target.value)}
                  onBlur={formik.handleBlur}
                  invalid={formik.touched.tolerance && !!formik.errors.tolerance}
                  invalidText={formik.touched.tolerance && formik.errors.tolerance}
                  disabled={readOnly}
                  allowEmpty
                  step="0.1"
                  min={0}
                  max={100}
                  helperText={intl.formatMessage({
                    id: 'compliance.threshold.form.tolerance.help',
                    defaultMessage: 'Acceptable deviation from threshold (optional)'
                  })}
                />
              </Column>

              <Column lg={8} md={4} sm={2}>
                <div className="compliance-threshold-form__preview">
                  <h4>
                    <FormattedMessage
                      id="compliance.threshold.form.preview.title"
                      defaultMessage="Threshold Preview"
                    />
                  </h4>
                  <div className="compliance-threshold-form__preview-content">
                    <span className="compliance-threshold-form__preview-parameter">
                      {formik.values.parameterName || intl.formatMessage({
                        id: 'compliance.threshold.form.preview.placeholder',
                        defaultMessage: 'Parameter Name'
                      })}
                    </span>
                    <span className="compliance-threshold-form__preview-threshold">
                      {formik.values.thresholdType === 'MAXIMUM' && formik.values.maxValue && `≤ ${formik.values.maxValue}`}
                      {formik.values.thresholdType === 'MINIMUM' && formik.values.minValue && `≥ ${formik.values.minValue}`}
                      {formik.values.thresholdType === 'RANGE' && formik.values.minValue && formik.values.maxValue &&
                        `${formik.values.minValue} - ${formik.values.maxValue}`}
                      {formik.values.thresholdType === 'EXACT' && formik.values.exactValue && `= ${formik.values.exactValue}`}
                    </span>
                    <span className="compliance-threshold-form__preview-unit">
                      {formik.values.unit}
                    </span>
                    {getCriticalityTag(formik.values.criticalityLevel)}
                  </div>
                </div>
              </Column>
            </Grid>
          </div>

          {/* Advanced Configuration */}
          <Accordion>
            <AccordionItem
              title={intl.formatMessage({
                id: 'compliance.threshold.form.section.advanced',
                defaultMessage: 'Advanced Configuration'
              })}
              open={isAdvancedExpanded}
              onHeadingClick={() => setIsAdvancedExpanded(!isAdvancedExpanded)}
            >
              <Grid>
                <Column lg={8} md={4} sm={2}>
                  <Select
                    id="sampleTypeId"
                    name="sampleTypeId"
                    labelText={intl.formatMessage({
                      id: 'compliance.threshold.form.sampleType.label',
                      defaultMessage: 'Sample Type (Optional)'
                    })}
                    value={formik.values.sampleTypeId}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    disabled={readOnly}
                    helperText={intl.formatMessage({
                      id: 'compliance.threshold.form.sampleType.help',
                      defaultMessage: 'Restrict threshold to specific sample types'
                    })}
                  >
                    <SelectItem
                      value=""
                      text={intl.formatMessage({
                        id: 'compliance.threshold.form.sampleType.all',
                        defaultMessage: 'All sample types'
                      })}
                    />
                    {sampleTypes.map(type => (
                      <SelectItem
                        key={type.id}
                        value={type.id}
                        text={type.name}
                      />
                    ))}
                  </Select>
                </Column>

                <Column lg={8} md={4} sm={2}>
                  <TextInput
                    id="methodReference"
                    name="methodReference"
                    labelText={intl.formatMessage({
                      id: 'compliance.threshold.form.methodReference.label',
                      defaultMessage: 'Method Reference'
                    })}
                    placeholder={intl.formatMessage({
                      id: 'compliance.threshold.form.methodReference.placeholder',
                      defaultMessage: 'Enter testing method reference'
                    })}
                    value={formik.values.methodReference}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    disabled={readOnly}
                    helperText={intl.formatMessage({
                      id: 'compliance.threshold.form.methodReference.help',
                      defaultMessage: 'Reference to analytical method used'
                    })}
                  />
                </Column>
              </Grid>

              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <TextInput
                    id="regulatoryBasis"
                    name="regulatoryBasis"
                    labelText={intl.formatMessage({
                      id: 'compliance.threshold.form.regulatoryBasis.label',
                      defaultMessage: 'Regulatory Basis'
                    })}
                    placeholder={intl.formatMessage({
                      id: 'compliance.threshold.form.regulatoryBasis.placeholder',
                      defaultMessage: 'Enter regulatory basis or citation'
                    })}
                    value={formik.values.regulatoryBasis}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    disabled={readOnly}
                    helperText={intl.formatMessage({
                      id: 'compliance.threshold.form.regulatoryBasis.help',
                      defaultMessage: 'Legal or regulatory authority for this threshold'
                    })}
                  />
                </Column>
              </Grid>

              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="description"
                    name="description"
                    labelText={intl.formatMessage({
                      id: 'compliance.threshold.form.description.label',
                      defaultMessage: 'Description'
                    })}
                    placeholder={intl.formatMessage({
                      id: 'compliance.threshold.form.description.placeholder',
                      defaultMessage: 'Describe this threshold and its purpose'
                    })}
                    value={formik.values.description}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    invalid={formik.touched.description && !!formik.errors.description}
                    invalidText={formik.touched.description && formik.errors.description}
                    disabled={readOnly}
                    rows={3}
                    maxCount={500}
                    enableCounter
                  />
                </Column>
              </Grid>

              <Grid>
                <Column lg={8} md={4} sm={2}>
                  <Toggle
                    id="isActive"
                    name="isActive"
                    labelText={intl.formatMessage({
                      id: 'compliance.threshold.form.isActive.label',
                      defaultMessage: 'Active'
                    })}
                    toggled={formik.values.isActive}
                    onToggle={(toggled) => formik.setFieldValue('isActive', toggled)}
                    disabled={readOnly}
                    helperText={intl.formatMessage({
                      id: 'compliance.threshold.form.isActive.help',
                      defaultMessage: 'Enable this threshold for compliance evaluation'
                    })}
                  />
                </Column>

                <Column lg={8} md={4} sm={2}>
                  <Toggle
                    id="requiresConfirmation"
                    name="requiresConfirmation"
                    labelText={intl.formatMessage({
                      id: 'compliance.threshold.form.requiresConfirmation.label',
                      defaultMessage: 'Requires Confirmation'
                    })}
                    toggled={formik.values.requiresConfirmation}
                    onToggle={(toggled) => formik.setFieldValue('requiresConfirmation', toggled)}
                    disabled={readOnly}
                    helperText={intl.formatMessage({
                      id: 'compliance.threshold.form.requiresConfirmation.help',
                      defaultMessage: 'Require confirmation when this threshold is exceeded'
                    })}
                  />
                </Column>
              </Grid>
            </AccordionItem>
          </Accordion>

          {/* Actions */}
          <div className="compliance-threshold-form__actions">
            {!readOnly && (
              <>
                <Button
                  kind="primary"
                  type="submit"
                  renderIcon={Save}
                  disabled={saving || !formik.isValid}
                >
                  {saving ? (
                    <FormattedMessage
                      id="compliance.threshold.form.saving"
                      defaultMessage="Saving..."
                    />
                  ) : (
                    <FormattedMessage
                      id={`compliance.threshold.form.${thresholdId ? 'update' : 'save'}`}
                      defaultMessage={thresholdId ? 'Update Threshold' : 'Save Threshold'}
                    />
                  )}
                </Button>
                <Button
                  kind="secondary"
                  renderIcon={Reset}
                  onClick={handleReset}
                  disabled={saving}
                >
                  <FormattedMessage
                    id="compliance.threshold.form.reset"
                    defaultMessage="Reset"
                  />
                </Button>
              </>
            )}
            <Button
              kind="secondary"
              renderIcon={Close}
              onClick={onCancel}
              disabled={saving}
            >
              <FormattedMessage
                id="compliance.threshold.form.cancel"
                defaultMessage="Cancel"
              />
            </Button>
          </div>
        </form>
      </Column>
    </Grid>
  );
};

export default ComplianceThresholdForm;