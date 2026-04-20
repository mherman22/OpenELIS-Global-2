import React, { useState, useEffect, useContext } from 'react';
import {
  Grid,
  Column,
  Form,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Button,
  ButtonSet,
  Toggle,
  Modal,
  InlineLoading,
  Tag
} from '@carbon/react';
import { Save, Close, Information } from '@carbon/icons-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { Formik } from 'formik';
import * as Yup from 'yup';

import PageBreadCrumb from '../common/PageBreadCrumb';
import { getFromOpenElisServer, postToOpenElisServer, putToOpenElisServer } from '../utils/Utils';
import { NotificationContext } from '../layout/Layout';
import './ComplianceStandardForm.css';

/**
 * ComplianceStandardForm - Create/Edit form for compliance standards
 *
 * Follows OpenELIS patterns:
 * - Formik + Yup for form handling (constitutional requirement)
 * - Carbon Design System components exclusively
 * - React Intl for all UI strings
 * - Separated validation schema and initial values
 * - Proper error handling and notifications
 */
const ComplianceStandardForm = ({ standardId, onSave, onCancel }) => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [existingStandard, setExistingStandard] = useState(null);
  const [issuingBodies, setIssuingBodies] = useState([]);
  const [countries, setCountries] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [duplicateCheckModal, setDuplicateCheckModal] = useState(false);

  const isEditing = !!standardId;

  // Yup validation schema following OpenELIS patterns
  const validationSchema = Yup.object().shape({
    name: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.standard.form.validation.name.required',
        defaultMessage: 'Standard name is required'
      }))
      .min(5, intl.formatMessage({
        id: 'compliance.standard.form.validation.name.minLength',
        defaultMessage: 'Standard name must be at least 5 characters'
      }))
      .max(255, intl.formatMessage({
        id: 'compliance.standard.form.validation.name.maxLength',
        defaultMessage: 'Standard name cannot exceed 255 characters'
      })),

    issuingBody: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.standard.form.validation.issuingBody.required',
        defaultMessage: 'Issuing body is required'
      })),

    regulationNumber: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.standard.form.validation.regulationNumber.required',
        defaultMessage: 'Regulation number is required'
      }))
      .matches(/^[A-Z0-9\-\/\s]+$/i, intl.formatMessage({
        id: 'compliance.standard.form.validation.regulationNumber.format',
        defaultMessage: 'Regulation number can only contain letters, numbers, hyphens, and slashes'
      })),

    version: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.standard.form.validation.version.required',
        defaultMessage: 'Version is required'
      })),

    effectiveDate: Yup.date()
      .required(intl.formatMessage({
        id: 'compliance.standard.form.validation.effectiveDate.required',
        defaultMessage: 'Effective date is required'
      }))
      .typeError(intl.formatMessage({
        id: 'compliance.standard.form.validation.effectiveDate.invalid',
        defaultMessage: 'Please enter a valid date'
      })),

    expiryDate: Yup.date()
      .nullable()
      .min(Yup.ref('effectiveDate'), intl.formatMessage({
        id: 'compliance.standard.form.validation.expiryDate.afterEffective',
        defaultMessage: 'Expiry date must be after effective date'
      }))
      .typeError(intl.formatMessage({
        id: 'compliance.standard.form.validation.expiryDate.invalid',
        defaultMessage: 'Please enter a valid expiry date'
      })),

    countryRegion: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.standard.form.validation.countryRegion.required',
        defaultMessage: 'Country/Region is required'
      })),

    status: Yup.string()
      .required(intl.formatMessage({
        id: 'compliance.standard.form.validation.status.required',
        defaultMessage: 'Status is required'
      })),

    applicableSampleTypes: Yup.array()
      .min(1, intl.formatMessage({
        id: 'compliance.standard.form.validation.sampleTypes.required',
        defaultMessage: 'At least one sample type must be selected'
      })),

    enforcementAuthority: Yup.string()
      .max(255, intl.formatMessage({
        id: 'compliance.standard.form.validation.enforcementAuthority.maxLength',
        defaultMessage: 'Enforcement authority cannot exceed 255 characters'
      })),

    description: Yup.string()
      .max(1000, intl.formatMessage({
        id: 'compliance.standard.form.validation.description.maxLength',
        defaultMessage: 'Description cannot exceed 1000 characters'
      }))
  });

  // Initial values following OpenELIS patterns
  const getInitialValues = () => ({
    name: existingStandard?.name || '',
    issuingBody: existingStandard?.issuingBody || '',
    regulationNumber: existingStandard?.regulationNumber || '',
    version: existingStandard?.version || '1.0',
    effectiveDate: existingStandard?.effectiveDate || '',
    expiryDate: existingStandard?.expiryDate || '',
    countryRegion: existingStandard?.countryRegion || 'Indonesia',
    status: existingStandard?.status || 'DRAFT',
    applicableSampleTypes: existingStandard?.applicableSampleTypesList || [],
    description: existingStandard?.description || '',
    regulatoryContext: existingStandard?.regulatoryContext || '',
    enforcementAuthority: existingStandard?.enforcementAuthority || ''
  });

  // Status options
  const statusOptions = [
    { value: 'DRAFT', label: intl.formatMessage({ id: 'compliance.standard.status.draft', defaultMessage: 'Draft' }) },
    { value: 'ACTIVE', label: intl.formatMessage({ id: 'compliance.standard.status.active', defaultMessage: 'Active' }) },
    { value: 'SUSPENDED', label: intl.formatMessage({ id: 'compliance.standard.status.suspended', defaultMessage: 'Suspended' }) }
  ];

  // Load form data on mount
  useEffect(() => {
    loadFormData();
    if (isEditing) {
      loadExistingStandard();
    }
  }, [standardId]);

  const loadFormData = () => {
    // Load supporting data for dropdowns
    const endpoints = [
      '/rest/compliance/issuing-bodies',
      '/rest/compliance/countries',
      '/rest/compliance/sample-types'
    ];

    endpoints.forEach((endpoint, index) => {
      getFromOpenElisServer(endpoint, (response) => {
        switch (index) {
          case 0:
            setIssuingBodies(response || []);
            break;
          case 1:
            setCountries(response || []);
            break;
          case 2:
            setSampleTypes(response || []);
            break;
        }
      });
    });
  };

  const loadExistingStandard = () => {
    setLoading(true);
    getFromOpenElisServer(`/rest/compliance/standards/${standardId}`, (response) => {
      if (response) {
        setExistingStandard(response);
        setLoading(false);
      } else {
        addNotification({
          kind: 'error',
          title: intl.formatMessage({
            id: 'compliance.standard.load.error.title',
            defaultMessage: 'Error Loading Standard'
          }),
          message: intl.formatMessage({
            id: 'compliance.standard.load.error.message',
            defaultMessage: 'Failed to load compliance standard. Please try again.'
          })
        });
        setLoading(false);
      }
    });
  };

  const checkForDuplicates = (values, callback) => {
    const checkData = {
      issuingBody: values.issuingBody,
      regulationNumber: values.regulationNumber,
      version: values.version,
      excludeId: standardId // Exclude current standard when editing
    };

    postToOpenElisServer('/rest/compliance/standards/check-duplicate', JSON.stringify(checkData), (status, response) => {
      if (status === 200) {
        const isDuplicate = response?.isDuplicate || false;
        callback(isDuplicate, response?.existingStandard);
      } else {
        callback(false);
      }
    });
  };

  const handleSubmit = (values, { setSubmitting, setFieldError }) => {
    setSubmitting(true);

    // Check for duplicates first
    checkForDuplicates(values, (isDuplicate, existingStandard) => {
      if (isDuplicate && !isEditing) {
        setDuplicateCheckModal(true);
        setSubmitting(false);
        return;
      }

      // Prepare data for submission
      const submitData = {
        ...values,
        id: standardId || null,
        applicableSampleTypes: values.applicableSampleTypes.join(','),
        effectiveDate: new Date(values.effectiveDate).toISOString().split('T')[0],
        expiryDate: values.expiryDate ? new Date(values.expiryDate).toISOString().split('T')[0] : null
      };

      const endpoint = isEditing
        ? `/rest/compliance/standards/${standardId}`
        : '/rest/compliance/standards';

      const method = isEditing ? putToOpenElisServer : postToOpenElisServer;

      method(endpoint, JSON.stringify(submitData), (status, response) => {
        setSubmitting(false);

        if (status === 200 || status === 201) {
          addNotification({
            kind: 'success',
            title: intl.formatMessage({
              id: 'compliance.standard.save.success.title',
              defaultMessage: 'Standard Saved'
            }),
            message: intl.formatMessage({
              id: 'compliance.standard.save.success.message',
              defaultMessage: 'Compliance standard has been saved successfully.'
            })
          });

          if (onSave) {
            onSave(response);
          }
        } else {
          // Handle validation errors
          if (response?.validationErrors) {
            response.validationErrors.forEach(error => {
              if (error.fieldName) {
                setFieldError(error.fieldName, error.message);
              }
            });
          }

          addNotification({
            kind: 'error',
            title: intl.formatMessage({
              id: 'compliance.standard.save.error.title',
              defaultMessage: 'Error Saving Standard'
            }),
            message: response?.message || intl.formatMessage({
              id: 'compliance.standard.save.error.message',
              defaultMessage: 'Failed to save compliance standard. Please check the form and try again.'
            })
          });
        }
      });
    });
  };

  return (
    <Grid fullWidth className="compliance-standard-form">
      <Column lg={16}>
        <PageBreadCrumb breadcrumbs={[
          { label: intl.formatMessage({ id: 'breadcrumb.home', defaultMessage: 'Home' }), link: '/' },
          {
            label: intl.formatMessage({
              id: 'compliance.standards.breadcrumb',
              defaultMessage: 'Compliance Standards'
            }),
            link: '/compliance/standards'
          },
          {
            label: isEditing
              ? intl.formatMessage({ id: 'compliance.standard.edit.breadcrumb', defaultMessage: 'Edit Standard' })
              : intl.formatMessage({ id: 'compliance.standard.create.breadcrumb', defaultMessage: 'Create Standard' })
          }
        ]} />
      </Column>

      <Column lg={16} className="compliance-standard-form__header">
        <h1>
          <FormattedMessage
            id={isEditing ? 'compliance.standard.edit.title' : 'compliance.standard.create.title'}
            defaultMessage={isEditing ? 'Edit Compliance Standard' : 'Create Compliance Standard'}
          />
        </h1>
        <p>
          <FormattedMessage
            id="compliance.standard.form.description"
            defaultMessage="Configure regulatory compliance standard details, effective dates, and applicable sample types."
          />
        </p>
      </Column>

      {loading ? (
        <Column lg={16}>
          <InlineLoading
            description={intl.formatMessage({
              id: 'compliance.standard.form.loading',
              defaultMessage: 'Loading standard details...'
            })}
          />
        </Column>
      ) : (
        <Formik
          initialValues={getInitialValues()}
          validationSchema={validationSchema}
          enableReinitialize={true}
          onSubmit={handleSubmit}
        >
          {({
            values,
            errors,
            touched,
            handleChange,
            handleBlur,
            handleSubmit,
            isSubmitting,
            setFieldValue,
            resetForm
          }) => (
            <Form onSubmit={handleSubmit}>
              <Column lg={16} className="compliance-standard-form__content">
                <Grid>
                  {/* Basic Information Section */}
                  <Column lg={16} className="compliance-standard-form__section">
                    <h2>
                      <FormattedMessage
                        id="compliance.standard.form.section.basic"
                        defaultMessage="Basic Information"
                      />
                    </h2>
                  </Column>

                  <Column lg={8}>
                    <TextInput
                      id="name"
                      name="name"
                      labelText={intl.formatMessage({
                        id: 'compliance.standard.form.field.name',
                        defaultMessage: 'Standard Name'
                      })}
                      value={values.name}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      invalid={touched.name && errors.name}
                      invalidText={errors.name}
                      placeholder={intl.formatMessage({
                        id: 'compliance.standard.form.field.name.placeholder',
                        defaultMessage: 'Enter the full name of the compliance standard'
                      })}
                      maxLength={255}
                    />
                  </Column>

                  <Column lg={8}>
                    <Select
                      id="issuingBody"
                      name="issuingBody"
                      labelText={intl.formatMessage({
                        id: 'compliance.standard.form.field.issuingBody',
                        defaultMessage: 'Issuing Body'
                      })}
                      value={values.issuingBody}
                      onChange={(e) => setFieldValue('issuingBody', e.target.value)}
                      invalid={touched.issuingBody && errors.issuingBody}
                      invalidText={errors.issuingBody}
                    >
                      <SelectItem text={intl.formatMessage({
                        id: 'select.placeholder',
                        defaultMessage: 'Select an option'
                      })} value="" />
                      {issuingBodies.map(body => (
                        <SelectItem key={body.value} text={body.label} value={body.value} />
                      ))}
                    </Select>
                  </Column>

                  <Column lg={8}>
                    <TextInput
                      id="regulationNumber"
                      name="regulationNumber"
                      labelText={intl.formatMessage({
                        id: 'compliance.standard.form.field.regulationNumber',
                        defaultMessage: 'Regulation Number'
                      })}
                      value={values.regulationNumber}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      invalid={touched.regulationNumber && errors.regulationNumber}
                      invalidText={errors.regulationNumber}
                      placeholder="e.g., PP 22/2021, CFR 40-141"
                    />
                  </Column>

                  <Column lg={8}>
                    <TextInput
                      id="version"
                      name="version"
                      labelText={intl.formatMessage({
                        id: 'compliance.standard.form.field.version',
                        defaultMessage: 'Version'
                      })}
                      value={values.version}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      invalid={touched.version && errors.version}
                      invalidText={errors.version}
                      placeholder="e.g., 1.0, 2021, 4th Edition"
                    />
                  </Column>

                  {/* Date and Status Section */}
                  <Column lg={16} className="compliance-standard-form__section">
                    <h2>
                      <FormattedMessage
                        id="compliance.standard.form.section.dates"
                        defaultMessage="Effective Period & Status"
                      />
                    </h2>
                  </Column>

                  <Column lg={8}>
                    <DatePicker
                      dateFormat="m/d/Y"
                      onChange={(dates) => setFieldValue('effectiveDate', dates[0])}
                      value={values.effectiveDate}
                    >
                      <DatePickerInput
                        id="effectiveDate"
                        labelText={intl.formatMessage({
                          id: 'compliance.standard.form.field.effectiveDate',
                          defaultMessage: 'Effective Date'
                        })}
                        placeholder="mm/dd/yyyy"
                        invalid={touched.effectiveDate && errors.effectiveDate}
                        invalidText={errors.effectiveDate}
                      />
                    </DatePicker>
                  </Column>

                  <Column lg={8}>
                    <DatePicker
                      dateFormat="m/d/Y"
                      onChange={(dates) => setFieldValue('expiryDate', dates[0] || '')}
                      value={values.expiryDate}
                    >
                      <DatePickerInput
                        id="expiryDate"
                        labelText={intl.formatMessage({
                          id: 'compliance.standard.form.field.expiryDate',
                          defaultMessage: 'Expiry Date (Optional)'
                        })}
                        placeholder="mm/dd/yyyy"
                        invalid={touched.expiryDate && errors.expiryDate}
                        invalidText={errors.expiryDate}
                      />
                    </DatePicker>
                  </Column>

                  <Column lg={8}>
                    <Select
                      id="status"
                      name="status"
                      labelText={intl.formatMessage({
                        id: 'compliance.standard.form.field.status',
                        defaultMessage: 'Status'
                      })}
                      value={values.status}
                      onChange={(e) => setFieldValue('status', e.target.value)}
                      invalid={touched.status && errors.status}
                      invalidText={errors.status}
                    >
                      {statusOptions.map(option => (
                        <SelectItem key={option.value} text={option.label} value={option.value} />
                      ))}
                    </Select>
                  </Column>

                  <Column lg={8}>
                    <Select
                      id="countryRegion"
                      name="countryRegion"
                      labelText={intl.formatMessage({
                        id: 'compliance.standard.form.field.countryRegion',
                        defaultMessage: 'Country/Region'
                      })}
                      value={values.countryRegion}
                      onChange={(e) => setFieldValue('countryRegion', e.target.value)}
                      invalid={touched.countryRegion && errors.countryRegion}
                      invalidText={errors.countryRegion}
                    >
                      {countries.map(country => (
                        <SelectItem key={country.value} text={country.label} value={country.value} />
                      ))}
                    </Select>
                  </Column>

                  {/* Additional Details Section */}
                  <Column lg={16} className="compliance-standard-form__section">
                    <h2>
                      <FormattedMessage
                        id="compliance.standard.form.section.details"
                        defaultMessage="Additional Details"
                      />
                    </h2>
                  </Column>

                  <Column lg={16}>
                    <TextArea
                      id="description"
                      name="description"
                      labelText={intl.formatMessage({
                        id: 'compliance.standard.form.field.description',
                        defaultMessage: 'Description'
                      })}
                      value={values.description}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      invalid={touched.description && errors.description}
                      invalidText={errors.description}
                      placeholder={intl.formatMessage({
                        id: 'compliance.standard.form.field.description.placeholder',
                        defaultMessage: 'Provide a brief description of this compliance standard'
                      })}
                      rows={4}
                      maxCount={1000}
                    />
                  </Column>

                  <Column lg={8}>
                    <TextInput
                      id="enforcementAuthority"
                      name="enforcementAuthority"
                      labelText={intl.formatMessage({
                        id: 'compliance.standard.form.field.enforcementAuthority',
                        defaultMessage: 'Enforcement Authority'
                      })}
                      value={values.enforcementAuthority}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      invalid={touched.enforcementAuthority && errors.enforcementAuthority}
                      invalidText={errors.enforcementAuthority}
                      placeholder={intl.formatMessage({
                        id: 'compliance.standard.form.field.enforcementAuthority.placeholder',
                        defaultMessage: 'Authority responsible for enforcement'
                      })}
                    />
                  </Column>

                  {/* Sample Types Section - Multi-select would be implemented as checkbox group */}
                  <Column lg={16} className="compliance-standard-form__section">
                    <h2>
                      <FormattedMessage
                        id="compliance.standard.form.section.sampleTypes"
                        defaultMessage="Applicable Sample Types"
                      />
                    </h2>
                    <div className="compliance-standard-form__sample-types">
                      {sampleTypes.map(sampleType => (
                        <Tag
                          key={sampleType.value}
                          type={values.applicableSampleTypes.includes(sampleType.value) ? 'blue' : 'outline'}
                          onClick={() => {
                            const currentTypes = values.applicableSampleTypes;
                            const newTypes = currentTypes.includes(sampleType.value)
                              ? currentTypes.filter(type => type !== sampleType.value)
                              : [...currentTypes, sampleType.value];
                            setFieldValue('applicableSampleTypes', newTypes);
                          }}
                          style={{ cursor: 'pointer', margin: '0.25rem' }}
                        >
                          {sampleType.label}
                        </Tag>
                      ))}
                    </div>
                    {touched.applicableSampleTypes && errors.applicableSampleTypes && (
                      <div className="cds--form-requirement" style={{ marginTop: '0.5rem' }}>
                        {errors.applicableSampleTypes}
                      </div>
                    )}
                  </Column>
                </Grid>
              </Column>

              {/* Form Actions */}
              <Column lg={16} className="compliance-standard-form__actions">
                <ButtonSet>
                  <Button
                    kind="secondary"
                    renderIcon={Close}
                    onClick={() => {
                      if (onCancel) {
                        onCancel();
                      } else {
                        resetForm();
                      }
                    }}
                    disabled={isSubmitting}
                  >
                    <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
                  </Button>

                  <Button
                    kind="primary"
                    type="submit"
                    renderIcon={Save}
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? (
                      <InlineLoading
                        description={intl.formatMessage({
                          id: 'compliance.standard.form.saving',
                          defaultMessage: 'Saving...'
                        })}
                      />
                    ) : (
                      <FormattedMessage
                        id={isEditing ? 'button.update' : 'button.create'}
                        defaultMessage={isEditing ? 'Update' : 'Create'}
                      />
                    )}
                  </Button>
                </ButtonSet>
              </Column>
            </Form>
          )}
        </Formik>
      )}

      {/* Duplicate Check Modal */}
      <Modal
        open={duplicateCheckModal}
        onRequestClose={() => setDuplicateCheckModal(false)}
        modalHeading={intl.formatMessage({
          id: 'compliance.standard.duplicate.modal.title',
          defaultMessage: 'Duplicate Standard Detected'
        })}
        modalLabel={intl.formatMessage({
          id: 'compliance.standard.duplicate.modal.label',
          defaultMessage: 'Warning'
        })}
        primaryButtonText={intl.formatMessage({
          id: 'button.understood',
          defaultMessage: 'Understood'
        })}
        size="md"
        danger
      >
        <p>
          <FormattedMessage
            id="compliance.standard.duplicate.modal.message"
            defaultMessage="A compliance standard with the same issuing body, regulation number, and version already exists. Please check the existing standards or modify the details to create a unique standard."
          />
        </p>
      </Modal>
    </Grid>
  );
};

export default ComplianceStandardForm;