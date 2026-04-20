# Compliance Module FHIR R4 Integration Guide

## Overview

The OpenELIS Global compliance module provides comprehensive FHIR R4 integration following established OpenELIS patterns. This document outlines the FHIR resource mappings, extensions, and usage patterns for regulatory compliance data exchange.

## FHIR Resource Mappings

### ComplianceStandard → FHIR Measure

OpenELIS compliance standards are mapped to FHIR Measure resources, which represent evaluation criteria and quality measures.

| OpenELIS Field | FHIR Measure Property | Notes |
|---|---|---|
| `fhirUuid` | `id` | FHIR resource identifier |
| `name` | `title` | Standard name |
| `version` | `version` | Standard version |
| `issuingBody` | `publisher` | Organization that issued the standard |
| `description` | `description` | Standard description |
| `regulationNumber` | `identifier[0].value` | Primary identifier with system |
| `effectiveDate`/`expiryDate` | `effectivePeriod` | Period when standard is active |
| `countryRegion` | `jurisdiction` | Geographic scope |
| `status` | `status` | Publication status mapping |

**FHIR Status Mappings:**
- `ACTIVE` → `active`
- `DRAFT` → `draft`
- `SUPERSEDED`/`ARCHIVED` → `retired`
- `SUSPENDED` → `unknown`

### ComplianceEvaluation → FHIR MeasureReport

OpenELIS compliance evaluations are mapped to FHIR MeasureReport resources, representing results of applying measures.

| OpenELIS Field | FHIR MeasureReport Property | Notes |
|---|---|---|
| `fhirUuid` | `id` | FHIR resource identifier |
| `complianceStandard.fhirUuid` | `measure` | Reference to Measure |
| `sampleId` | `subject` | Reference to Specimen |
| `evaluationDate` | `date`, `period` | Evaluation timing |
| `status` | `status` | Evaluation status |
| `overallCompliance` | `group[0].measureScore` | Overall compliance percentage |
| `totalParameters` | `group[0].population[measure-population].count` | Total parameters evaluated |
| `compliantParameters` | `group[0].population[compliant].count` | Compliant parameter count |

**FHIR Status Mappings:**
- `COMPLIANT`/`NON_COMPLIANT` → `complete`
- `WARNING`/`PENDING` → `pending`

## FHIR Extensions

### Extension URLs

Base URL: `http://openelis.org/fhir/extension/`

| Extension | Usage | Description |
|---|---|---|
| `enforcement-authority` | Measure | Authority responsible for enforcement |
| `applicable-sample-types` | Measure | Sample types where standard applies |
| `evaluation-status` | MeasureReport | Detailed evaluation status |
| `evaluation-notes` | MeasureReport | Evaluation notes/comments |
| `threshold-definition` | Measure.group.population | Parameter threshold details |
| `evaluation-result` | MeasureReport.group.stratifier.stratum | Individual parameter result |
| `compliance-statistics` | Bundle.meta | Compliance summary statistics |

### Extension Examples

#### Enforcement Authority Extension
```json
{
  "url": "http://openelis.org/fhir/extension/enforcement-authority",
  "valueString": "Ministry of Health"
}
```

#### Applicable Sample Types Extension
```json
{
  "url": "http://openelis.org/fhir/extension/applicable-sample-types",
  "extension": [
    {
      "url": "sample-type",
      "valueString": "Water"
    },
    {
      "url": "sample-type",
      "valueString": "Wastewater"
    }
  ]
}
```

#### Threshold Definition Extension
```json
{
  "url": "http://openelis.org/fhir/extension/threshold-definition",
  "extension": [
    {
      "url": "parameter-name",
      "valueString": "pH"
    },
    {
      "url": "threshold-type",
      "valueString": "RANGE"
    },
    {
      "url": "min-value",
      "valueDecimal": 6.5
    },
    {
      "url": "max-value",
      "valueDecimal": 8.5
    },
    {
      "url": "unit",
      "valueString": "pH Units"
    },
    {
      "url": "criticality",
      "valueString": "HIGH"
    }
  ]
}
```

## FHIR Identifiers

### System URLs

- **Compliance Standards:** `http://openelis.org/compliance/regulation-number`
- **Issuing Bodies:** `http://openelis.org/compliance/issuing-body`
- **Parameter Groups:** `http://openelis.org/compliance/parameter-group`
- **Threshold Parameters:** `http://openelis.org/compliance/threshold-parameter`
- **Evaluation Groups:** `http://openelis.org/compliance/evaluation-group`
- **Result Parameters:** `http://openelis.org/compliance/result-parameter`
- **Result Status:** `http://openelis.org/compliance/result-status`

### Identifier Examples

#### Primary Standard Identifier
```json
{
  "use": "official",
  "system": "http://openelis.org/compliance/regulation-number",
  "value": "PP-22-2021"
}
```

#### Secondary Standard Identifier
```json
{
  "use": "secondary",
  "system": "http://openelis.org/compliance/issuing-body",
  "value": "Indonesia Ministry of Environment/Baku Mutu Air Limbah"
}
```

## FHIR Profiles

### ComplianceStandard Profile
**Profile URL:** `http://openelis.org/fhir/StructureDefinition/ComplianceStandard`

**Base Resource:** `Measure`

**Key Constraints:**
- `title` is required (standard name)
- `publisher` is required (issuing body)
- `identifier` must include regulation number
- `type` must be "outcome"
- Must include applicable sample types extension

### ComplianceEvaluation Profile
**Profile URL:** `http://openelis.org/fhir/StructureDefinition/ComplianceEvaluation`

**Base Resource:** `MeasureReport`

**Key Constraints:**
- `measure` reference is required
- `type` must be "individual"
- `status` is required
- Must include overall compliance group
- Subject should reference Specimen

## FHIR REST API Endpoints

### ComplianceStandard Provider (Measure)

- `GET /fhir/Measure/{id}` - Read specific standard
- `POST /fhir/Measure` - Create new standard
- `PUT /fhir/Measure/{id}` - Update existing standard
- `DELETE /fhir/Measure/{id}` - Archive standard
- `GET /fhir/Measure?title={name}` - Search by name
- `GET /fhir/Measure?identifier={regulation}` - Search by regulation
- `GET /fhir/Measure?publisher={body}` - Search by issuing body
- `GET /fhir/Measure?status={status}` - Search by status

### ComplianceEvaluation Provider (MeasureReport)

- `GET /fhir/MeasureReport/{id}` - Read specific evaluation
- `POST /fhir/MeasureReport` - Create new evaluation
- `PUT /fhir/MeasureReport/{id}` - Update existing evaluation
- `GET /fhir/MeasureReport?measure={standardId}` - Search by standard
- `GET /fhir/MeasureReport?subject={sampleId}` - Search by sample
- `GET /fhir/MeasureReport?date={period}` - Search by date range
- `GET /fhir/MeasureReport?status={status}` - Search by status
- `GET /fhir/MeasureReport?measure={standardId}&_summary=count` - Get statistics

## Integration Patterns

### Automatic FHIR Sync

The compliance module uses JPA lifecycle hooks to automatically sync entities to FHIR:

```java
@PostPersist
public void onPostPersist() {
    // Automatically sync new entities to FHIR server
}

@PostUpdate
public void onPostUpdate() {
    // Automatically sync updates to FHIR server
}
```

### Async Processing

FHIR operations are performed asynchronously to avoid blocking transactional operations:

```java
@Async
@Transactional(readOnly = true)
public void syncToFhir(ComplianceStandard standard, boolean isCreate) {
    // Async FHIR sync implementation
}
```

### Error Handling

FHIR sync errors are logged but do not fail the primary transaction:

```java
try {
    fhirTransform.syncToFhir(entity, isCreate);
} catch (Exception e) {
    LogEvent.logError("FHIR sync failed but continuing: " + e.getMessage());
    // Primary operation continues
}
```

## Configuration

### Required Properties

```properties
# FHIR server configuration
org.openelisglobal.fhirstore.uri=http://localhost:8080/fhir
org.openelisglobal.fhirstore.username=openelis
org.openelisglobal.fhirstore.password=password

# OpenELIS FHIR system identifier
org.openelisglobal.oe.fhir.system=http://openelis-global.org
```

### Optional Properties

```properties
# Client registry integration (optional)
org.openelisglobal.crserver.uri=http://localhost:3000

# Remote FHIR servers for federation
org.openelisglobal.remote.source.uri=http://remote-fhir.example.com/fhir
```

## Usage Examples

### Creating a Compliance Standard via FHIR

```http
POST /fhir/Measure HTTP/1.1
Content-Type: application/fhir+json

{
  "resourceType": "Measure",
  "title": "Indonesian Water Quality Standard PP-22-2021",
  "publisher": "Indonesia Ministry of Environment",
  "version": "2021",
  "status": "active",
  "type": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/measure-type",
          "code": "outcome",
          "display": "Outcome"
        }
      ]
    }
  ],
  "identifier": [
    {
      "use": "official",
      "system": "http://openelis.org/compliance/regulation-number",
      "value": "PP-22-2021"
    }
  ],
  "jurisdiction": [
    {
      "coding": [
        {
          "system": "urn:iso:std:iso:3166",
          "code": "ID"
        }
      ]
    }
  ],
  "extension": [
    {
      "url": "http://openelis.org/fhir/extension/enforcement-authority",
      "valueString": "Ministry of Environment and Forestry"
    }
  ]
}
```

### Searching for Evaluations

```http
GET /fhir/MeasureReport?measure=Measure/uuid-123&date=ge2024-01-01&status=complete HTTP/1.1
```

### Getting Compliance Statistics

```http
GET /fhir/MeasureReport?measure=Measure/uuid-123&_summary=count HTTP/1.1
```

## Security Considerations

1. **Authentication:** All FHIR endpoints require authentication via OpenELIS security framework
2. **Authorization:** Access controlled by OpenELIS role-based permissions
3. **Data Validation:** All incoming FHIR resources are validated against profiles
4. **Audit Trail:** All FHIR operations are logged in OpenELIS audit system
5. **Rate Limiting:** FHIR endpoints respect OpenELIS rate limiting policies

## Performance Considerations

1. **Async Sync:** FHIR operations are asynchronous to avoid blocking
2. **Batch Operations:** Multiple resources can be created/updated in single transaction
3. **Caching:** FHIR client connections are pooled and cached
4. **Pagination:** Search results are paginated to manage memory usage
5. **Indexing:** Database indexes support efficient FHIR ID lookups

## Monitoring and Debugging

### Log Categories

- `org.openelisglobal.compliance.fhir` - FHIR integration logs
- `org.openelisglobal.dataexchange.fhir` - Core FHIR infrastructure
- `ca.uhn.fhir.rest.client` - HAPI FHIR client logs

### Common Issues

1. **FHIR Server Connectivity:** Check `fhirstore.uri` configuration
2. **UUID Validation:** Ensure FHIR IDs are valid UUIDs
3. **Extension Validation:** Verify custom extensions follow schema
4. **Resource References:** Check that referenced resources exist

### Troubleshooting Commands

```bash
# Test FHIR server connectivity
curl -H "Accept: application/fhir+json" http://localhost:8080/fhir/metadata

# Validate FHIR resource
curl -X POST -H "Content-Type: application/fhir+json" \
     -d @measure.json http://localhost:8080/fhir/Measure/$validate

# Search compliance measures
curl -H "Accept: application/fhir+json" \
     "http://localhost:8080/fhir/Measure?publisher=Indonesia%20Ministry"
```

## Future Enhancements

1. **FHIR Bulk Export:** Support for bulk data export via FHIR Bulk API
2. **Real-time Subscriptions:** FHIR Subscription support for real-time updates
3. **Advanced Search:** Support for additional FHIR search parameters
4. **GraphQL Integration:** FHIR GraphQL endpoint for flexible queries
5. **Multi-tenant Support:** FHIR resource partitioning for multi-tenant deployments

This integration follows HL7 FHIR R4 standards and OpenELIS architectural patterns to provide robust, interoperable compliance data exchange.