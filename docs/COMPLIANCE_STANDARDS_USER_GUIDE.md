# Compliance Standards Administration - User Guide

## Overview

The **Compliance Standards Administration** feature enables laboratory administrators to manage regulatory compliance standards for environmental and vector testing. This module ensures that laboratory tests meet specific regulatory requirements from various issuing bodies and jurisdictions.

## Navigation

To access Compliance Standards Administration:

1. Log into OpenELIS as an administrator
2. Navigate to **Admin** → **Test Management** → **Compliance Standards**

## Key Features

### 📋 Standards Management
- **View Standards**: Browse all compliance standards with detailed information
- **Import Standards**: Bulk import standards from CSV files
- **Export Standards**: Export compliance data for reporting
- **Search & Filter**: Find standards by name, issuing body, or regulation number

### 🔗 Test Integration
- **Test Assignment**: Link compliance standards to specific laboratory tests
- **Mandatory vs Optional**: Configure whether compliance is required or optional
- **Parameter Mapping**: Specify which test parameters apply to each standard

### 📊 Compliance Tracking
- **Status Monitoring**: Track compliance status (Compliant, Non-Compliant, Pending)
- **Real-time Updates**: Automatic evaluation of test results against standards
- **Audit Trail**: Complete history of compliance activities

## Getting Started

### 1. Importing Compliance Standards

**Step 1:** Prepare your CSV file with the following columns:
- `name` - Standard name
- `regulationNumber` - Official regulation identifier
- `issuingBody` - Organization that issued the standard
- `version` - Standard version
- `effectiveDate` - When the standard becomes effective (YYYY-MM-DD)
- `status` - ACTIVE, DRAFT, SUPERSEDED, ARCHIVED, or SUSPENDED
- `countryRegion` - Applicable jurisdiction
- `description` - Detailed description of the standard

**Step 2:** Import the standards:
1. Click **Import Standards**
2. Select your CSV file
3. Review the preview and validation results
4. Configure import options:
   - ✅ **Update existing standards** - Overwrite existing records
   - ✅ **Skip rows with errors** - Continue import despite validation errors
5. Click **Import** to complete

### 2. Assigning Standards to Tests

**Step 1:** Navigate to Test Catalog
1. Go to **Admin** → **Test Management** → **Test Catalog**
2. Find your test and click **Edit**
3. Select the **Compliance** tab

**Step 2:** Assign compliance standards:
1. Click **Assign Standard**
2. Select the applicable compliance standard
3. Configure settings:
   - **Mandatory Compliance**: Check if compliance is required
   - **Applicable Parameters**: Specify which test parameters this standard covers
4. Click **Assign**

### 3. Monitoring Compliance

**Compliance Status Indicators:**
- 🟢 **Compliant** - Test results meet all requirements
- 🔴 **Non-Compliant** - Test results fail to meet standards
- ⚪ **Pending Evaluation** - Results are being evaluated

**To check compliance status:**
1. Go to **Results** → **Validation**
2. Look for compliance status indicators in the results table
3. Click on any non-compliant result for details

## Supported Standards

The system supports various regulatory frameworks:

### Environmental Testing
- **WHO Guidelines** - Water quality standards
- **EPA Standards** - Environmental protection regulations
- **ISO Standards** - International testing protocols

### Vector Surveillance
- **PP No. 22/2021** - Indonesian health regulations
- **PP No. 41/1999** - Vector control standards
- **Regional Standards** - Local health authority requirements

## CSV Import Template

```csv
name,regulationNumber,issuingBody,version,effectiveDate,status,countryRegion,description
WHO Water Quality Guidelines,WHO/SDE/WSH/03.04,World Health Organization,4.0,2017-01-01,ACTIVE,Global,Drinking water quality guidelines
EPA Clean Water Act,40 CFR 141,US EPA,2023.1,2023-01-01,ACTIVE,United States,National primary drinking water regulations
ISO 17025 Testing,ISO/IEC 17025:2017,ISO,2017,2017-11-01,ACTIVE,International,General requirements for testing laboratories
```

## Best Practices

### 🎯 Standards Management
- **Regular Updates**: Keep standards current with latest regulations
- **Version Control**: Track version changes for audit purposes
- **Documentation**: Maintain clear descriptions for each standard

### 🔧 Test Configuration
- **Parameter Mapping**: Be specific about which parameters each standard covers
- **Mandatory Assignment**: Only mark critical standards as mandatory
- **Regular Review**: Periodically review test-standard assignments

### 📈 Monitoring & Reporting
- **Daily Checks**: Monitor compliance dashboard for non-compliant results
- **Trend Analysis**: Export data regularly for compliance reporting
- **Corrective Actions**: Document actions taken for non-compliant results

## Troubleshooting

### Common Import Issues

**Problem**: CSV import fails with validation errors
**Solution**:
- Check date format (use YYYY-MM-DD)
- Verify status values are: ACTIVE, DRAFT, SUPERSEDED, ARCHIVED, or SUSPENDED
- Ensure regulation numbers are unique

**Problem**: Standards not appearing in test assignment
**Solution**:
- Verify standards have ACTIVE status
- Check that standards are appropriate for the test type
- Refresh the page and try again

### Compliance Evaluation Issues

**Problem**: Results stuck in "Pending Evaluation"
**Solution**:
- Verify test parameters match standard requirements
- Check that evaluation rules are properly configured
- Contact system administrator if issue persists

## Feature Configuration

### Admin Settings
- **Feature Toggle**: `compliance.module.enabled=true`
- **Evaluation Frequency**: Configure automatic evaluation schedule
- **Notification Settings**: Set up alerts for non-compliant results

### User Permissions
- **View**: Read-only access to compliance data
- **Manage**: Create, edit, and delete standards
- **Admin**: Full access including system configuration

## Integration with Other Modules

### Laboratory Information System
- **Results Integration**: Automatic compliance evaluation on result entry
- **Quality Control**: Integration with QC rules and control charts
- **Audit Trail**: Compliance activities logged in system audit trail

### Reporting
- **Standard Reports**: Compliance summary reports
- **Custom Reports**: Export data for regulatory submissions
- **Dashboard**: Real-time compliance metrics

## Support and Training

### Getting Help
- **System Administrator**: Contact your IT team for technical issues
- **User Training**: Request training sessions for new features
- **Documentation**: Refer to this guide for common procedures

### Updates and Maintenance
- **Regular Updates**: Standards database updated quarterly
- **System Maintenance**: Scheduled during off-peak hours
- **Backup**: Compliance data included in regular system backups

---

**Version**: 1.0
**Last Updated**: April 2026
**Feature**: OGC-528 S-01 - Compliance Standards Administration

For additional support, contact your laboratory administrator or IT support team.
