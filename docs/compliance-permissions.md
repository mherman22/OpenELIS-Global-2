# Compliance Module - Granular Permissions

This document describes the granular permission system implemented for the Compliance Standards module in OpenELIS Global.

## Overview

The compliance module implements role-based access control with four distinct permission levels, providing fine-grained control over compliance-related operations.

## Permission Roles

### 1. ADMIN (Legacy - Full Access)
- **Description**: System administrators with full access to all OpenELIS functionality
- **Compliance Access**: Complete access to all compliance operations
- **Usage**: Existing role maintained for backward compatibility

### 2. COMPLIANCE_ADMIN (Compliance Administrator)
- **Description**: Specialized role for compliance management
- **Permissions**:
  - Create, edit, and delete compliance standards
  - Archive compliance standards
  - Manage test-compliance standard associations
  - View all compliance data
  - Import compliance standards from CSV
  - Configure compliance settings
- **Use Cases**:
  - Laboratory compliance officers
  - Quality assurance managers
  - Senior laboratory staff responsible for regulatory compliance

### 3. COMPLIANCE_USER (Compliance User)
- **Description**: Standard compliance user role for day-to-day operations
- **Permissions**:
  - View compliance standards (active standards only)
  - Associate tests with compliance standards
  - Modify test-compliance associations
  - View compliance reports
  - Cannot delete or archive compliance standards
- **Use Cases**:
  - Laboratory technicians
  - Test coordinators
  - Staff responsible for test setup and configuration

### 4. COMPLIANCE_VIEWER (Compliance Viewer)
- **Description**: Read-only access to compliance information
- **Permissions**:
  - View compliance standards
  - View test-compliance associations
  - Generate compliance reports
  - Cannot modify any compliance data
- **Use Cases**:
  - Auditors
  - Inspectors
  - Read-only users requiring compliance information
  - Reporting staff

## Permission Matrix

| Operation | ADMIN | COMPLIANCE_ADMIN | COMPLIANCE_USER | COMPLIANCE_VIEWER |
|-----------|-------|------------------|-----------------|-------------------|
| **Compliance Standards Management** |
| View compliance standards | ✅ | ✅ | ✅ | ✅ |
| Create compliance standards | ✅ | ✅ | ❌ | ❌ |
| Edit compliance standards | ✅ | ✅ | ❌ | ❌ |
| Delete compliance standards | ✅ | ✅ | ❌ | ❌ |
| Archive compliance standards | ✅ | ✅ | ❌ | ❌ |
| **Test-Compliance Associations** |
| View test associations | ✅ | ✅ | ✅ | ✅ |
| Create test associations | ✅ | ✅ | ✅ | ❌ |
| Modify test associations | ✅ | ✅ | ✅ | ❌ |
| Remove test associations | ✅ | ✅ | ✅ | ❌ |
| **Data Import/Export** |
| Import from CSV | ✅ | ✅ | ❌ | ❌ |
| Export compliance data | ✅ | ✅ | ✅ | ✅ |
| **System Configuration** |
| Configure CSV seeding | ✅ | ✅ | ❌ | ❌ |
| Manage compliance settings | ✅ | ✅ | ❌ | ❌ |

## API Endpoint Permissions

### ComplianceStandardMenuRestController
- **GET /rest/ComplianceStandardMenu**: All roles (ADMIN, COMPLIANCE_ADMIN, COMPLIANCE_USER, COMPLIANCE_VIEWER)
- **GET /rest/compliance-sample-types**: All roles (ADMIN, COMPLIANCE_ADMIN, COMPLIANCE_USER, COMPLIANCE_VIEWER)
- **POST /rest/DeleteComplianceStandard**: Admin only (ADMIN, COMPLIANCE_ADMIN)
- **POST /rest/ArchiveComplianceStandard**: Admin only (ADMIN, COMPLIANCE_ADMIN)

### TestComplianceStandardRestController
- **GET /rest/test/{testId}/compliance-standards**: All roles (ADMIN, COMPLIANCE_ADMIN, COMPLIANCE_USER, COMPLIANCE_VIEWER)
- **GET /rest/available-compliance-standards**: All roles (ADMIN, COMPLIANCE_ADMIN, COMPLIANCE_USER, COMPLIANCE_VIEWER)
- **POST /rest/test/{testId}/compliance-standards**: Write access (ADMIN, COMPLIANCE_ADMIN, COMPLIANCE_USER)
- **POST /rest/test/{testId}/compliance-standards/{standardId}/remove**: Write access (ADMIN, COMPLIANCE_ADMIN, COMPLIANCE_USER)
- **POST /rest/test/{testId}/compliance-standards/bulk-update**: Write access (ADMIN, COMPLIANCE_ADMIN, COMPLIANCE_USER)

## Implementation Details

### Role Configuration
Roles are implemented using Spring Security's `@PreAuthorize` annotation:

```java
// Class-level permission (minimum required)
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER')")

// Method-level permission (more specific)
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN', 'COMPLIANCE_USER', 'COMPLIANCE_VIEWER')")

// Admin-only operations
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_ADMIN')")
```

### Role Hierarchy
The roles follow a hierarchical model where higher-level roles inherit permissions from lower levels:

```
ADMIN (full system access)
└── COMPLIANCE_ADMIN (full compliance access)
    └── COMPLIANCE_USER (standard compliance operations)
        └── COMPLIANCE_VIEWER (read-only compliance access)
```

## Setting Up Roles

### Database Configuration
Roles must be configured in the OpenELIS database in the appropriate role tables. Contact your system administrator to add the new compliance roles:

1. `COMPLIANCE_ADMIN`
2. `COMPLIANCE_USER`
3. `COMPLIANCE_VIEWER`

### User Assignment
System administrators can assign these roles to users through the OpenELIS user management interface, allowing multiple roles per user as needed.

## Best Practices

### Role Assignment Guidelines
1. **COMPLIANCE_ADMIN**: Assign to senior staff responsible for regulatory compliance
2. **COMPLIANCE_USER**: Assign to laboratory technicians and test coordinators
3. **COMPLIANCE_VIEWER**: Assign to auditors, inspectors, and read-only users

### Security Considerations
1. Regularly review role assignments
2. Apply principle of least privilege
3. Audit compliance-related operations
4. Monitor access to sensitive compliance data

### Migration from ADMIN Role
Existing users with ADMIN role will maintain full access. Consider migrating appropriate users to the new granular roles for better security and access control.

## Troubleshooting

### Access Denied Errors
If users receive access denied errors:
1. Verify the user has the appropriate compliance role assigned
2. Check that the role is active and properly configured
3. Ensure the role names match exactly (case-sensitive)

### Role Assignment Issues
Contact your system administrator if:
- New compliance roles are not available in user management
- Role assignments are not taking effect
- Users with appropriate roles still cannot access compliance features

## Support

For technical support with compliance permissions:
- Review OpenELIS documentation
- Contact your system administrator
- Check application logs for permission-related errors
- Verify Spring Security configuration