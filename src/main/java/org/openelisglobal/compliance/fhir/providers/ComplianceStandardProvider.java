package org.openelisglobal.compliance.fhir.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.fhir.ComplianceFhirTransform;
import org.openelisglobal.compliance.service.ComplianceStandardService;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.fhir.providers.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR Resource Provider for ComplianceStandard (mapped to FHIR Measure)
 * Provides REST API endpoints following HAPI FHIR patterns
 *
 * Endpoints: - GET /fhir/Measure/{id} - Read specific compliance standard -
 * POST /fhir/Measure - Create new compliance standard - PUT /fhir/Measure/{id}
 * - Update existing compliance standard - DELETE /fhir/Measure/{id} - Delete
 * compliance standard - GET /fhir/Measure?title={name}&identifier={regulation}
 * - Search compliance standards
 */
@Component
public class ComplianceStandardProvider implements IResourceProvider {

    @Autowired
    private ComplianceFhirTransform fhirTransform;

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Measure.class;
    }

    /**
     * READ: GET /fhir/Measure/{id} Retrieve a specific compliance standard by FHIR
     * UUID
     */
    @Read
    public Measure readComplianceStandard(@IdParam IdType theId) {
        String method = "Read";
        try {
            FhirProviderUtils.validateIdParam(theId, "Measure", this.getClass().getSimpleName(), method);

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Reading ComplianceStandard with FHIR ID: " + theId.getIdPart());

            ComplianceStandard standard = complianceStandardService.getComplianceStandardByFhirId(theId.getIdPart());

            if (standard == null) {
                throw new ResourceNotFoundException("ComplianceStandard not found with FHIR ID: " + theId.getIdPart());
            }

            Measure fhirMeasure = fhirTransform.transformToFhirMeasure(standard);

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Successfully retrieved ComplianceStandard: " + standard.getName());

            return fhirMeasure;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while reading ComplianceStandard: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while reading ComplianceStandard", e);
        }
    }

    /**
     * CREATE: POST /fhir/Measure Create a new compliance standard from FHIR Measure
     * resource
     */
    @Create
    public MethodOutcome createComplianceStandard(@ResourceParam Measure fhirMeasure, HttpServletRequest request)
            throws FhirLocalPersistingException {

        String method = "create";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR CREATE request for ComplianceStandard");

        try {
            if (fhirMeasure == null) {
                throw new InvalidRequestException("Measure resource cannot be null");
            }

            // Validate required fields
            if (!fhirMeasure.hasTitle()) {
                throw new InvalidRequestException("Measure title (standard name) is required");
            }

            if (!fhirMeasure.hasPublisher()) {
                throw new InvalidRequestException("Measure publisher (issuing body) is required");
            }

            // Generate FHIR UUID if not provided
            if (!fhirMeasure.hasId()) {
                fhirMeasure.setId(UUID.randomUUID().toString());
            }

            // Transform FHIR Measure to ComplianceStandard
            ComplianceStandard standard = transformToComplianceStandard(fhirMeasure);

            // Set system user ID
            standard.setSysUserId(FhirProviderUtils.getSysUserId(request));

            // Save to database
            ComplianceStandard savedStandard = complianceStandardService.save(standard);

            if (savedStandard == null) {
                throw new InternalErrorException("Failed to save ComplianceStandard");
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Created ComplianceStandard: " + savedStandard.getName() + " with ID: " + savedStandard.getId());

            // Transform back to FHIR and return
            Measure response = fhirTransform.transformToFhirMeasure(savedStandard);

            return FhirProviderUtils.buildCreateOutcome(response);

        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error creating ComplianceStandard: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while creating ComplianceStandard", e);
        }
    }

    /**
     * UPDATE: PUT /fhir/Measure/{id} Update an existing compliance standard
     */
    @Update
    public MethodOutcome updateComplianceStandard(@IdParam IdType theId, @ResourceParam Measure fhirMeasure,
            HttpServletRequest request) throws FhirLocalPersistingException {

        String method = "update";
        try {
            FhirProviderUtils.validateIdParam(theId, "Measure", this.getClass().getSimpleName(), method);

            if (fhirMeasure == null) {
                throw new InvalidRequestException("Measure resource cannot be null");
            }

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Updating ComplianceStandard with FHIR ID: " + theId.getIdPart());

            // Find existing standard
            ComplianceStandard existingStandard = complianceStandardService
                    .getComplianceStandardByFhirId(theId.getIdPart());

            if (existingStandard == null) {
                throw new ResourceNotFoundException("ComplianceStandard not found with FHIR ID: " + theId.getIdPart());
            }

            // Transform FHIR updates to entity
            ComplianceStandard updatedStandard = transformToComplianceStandard(fhirMeasure);
            updatedStandard.setId(existingStandard.getId()); // Preserve database ID
            updatedStandard.setFhirUuid(existingStandard.getFhirUuid()); // Preserve FHIR UUID

            // Set system user ID
            updatedStandard.setSysUserId(FhirProviderUtils.getSysUserId(request));

            // Save updates
            ComplianceStandard savedStandard = complianceStandardService.update(updatedStandard);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Updated ComplianceStandard: " + savedStandard.getName());

            // Transform back to FHIR and return
            Measure response = fhirTransform.transformToFhirMeasure(savedStandard);

            return FhirProviderUtils.buildUpdateOutcome(response);

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error updating ComplianceStandard: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while updating ComplianceStandard", e);
        }
    }

    /**
     * DELETE: DELETE /fhir/Measure/{id} Delete (archive) a compliance standard
     */
    @Delete
    public MethodOutcome deleteComplianceStandard(@IdParam IdType theId) {
        String method = "delete";
        try {
            FhirProviderUtils.validateIdParam(theId, "Measure", this.getClass().getSimpleName(), method);

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Deleting ComplianceStandard with FHIR ID: " + theId.getIdPart());

            ComplianceStandard standard = complianceStandardService.getComplianceStandardByFhirId(theId.getIdPart());

            if (standard == null) {
                throw new ResourceNotFoundException("ComplianceStandard not found with FHIR ID: " + theId.getIdPart());
            }

            // Archive instead of hard delete (following OpenELIS patterns)
            complianceStandardService.archive(standard.getId());

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Archived ComplianceStandard: " + standard.getName());

            return FhirProviderUtils.buildDeleteOutcome(theId, "Measure");

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error deleting ComplianceStandard: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while deleting ComplianceStandard", e);
        }
    }

    /**
     * SEARCH: GET
     * /fhir/Measure?title={name}&identifier={regulation}&status={status} Search
     * compliance standards by various criteria
     */
    @Search
    public Bundle searchComplianceStandards(@OptionalParam(name = "title") StringParam title,
            @OptionalParam(name = "identifier") TokenParam identifier,
            @OptionalParam(name = "publisher") StringParam publisher,
            @OptionalParam(name = "status") TokenParam status) {

        String method = "search";
        try {
            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Searching ComplianceStandards with parameters - title: " + title + ", identifier: " + identifier
                            + ", publisher: " + publisher + ", status: " + status);

            List<ComplianceStandard> standards = new ArrayList<>();

            if (title != null) {
                // Search by name
                standards.addAll(complianceStandardService.getComplianceStandardsByName(title.getValue()));
            } else if (identifier != null) {
                // Search by regulation number
                ComplianceStandard standard = complianceStandardService
                        .getComplianceStandardByRegulationNumber(identifier.getValue());
                if (standard != null) {
                    standards.add(standard);
                }
            } else if (publisher != null) {
                // Search by issuing body
                standards.addAll(complianceStandardService.getComplianceStandardsByIssuingBody(publisher.getValue()));
            } else {
                // Return all active standards (with pagination in real implementation)
                standards.addAll(complianceStandardService.getActiveComplianceStandards());
            }

            // Filter by status if specified
            if (status != null && !standards.isEmpty()) {
                String statusValue = status.getValue().toUpperCase();
                standards.removeIf(standard -> !standard.getStatus().toString().equals(statusValue));
            }

            // Create Bundle
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.SEARCHSET);
            bundle.setTotal(standards.size());

            // Transform entities to FHIR resources
            for (ComplianceStandard standard : standards) {
                try {
                    Measure measure = fhirTransform.transformToFhirMeasure(standard);
                    Bundle.BundleEntryComponent entry = bundle.addEntry();
                    entry.setResource(measure);
                    entry.setFullUrl("Measure/" + measure.getId());
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getSimpleName(), method,
                            "Error transforming ComplianceStandard to FHIR: " + e.getMessage());
                    // Continue with other standards
                }
            }

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Search completed, returning " + bundle.getEntry().size() + " results");

            return bundle;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error searching ComplianceStandards: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while searching ComplianceStandards", e);
        }
    }

    // Private helper methods

    /**
     * Transform FHIR Measure to ComplianceStandard entity
     */
    private ComplianceStandard transformToComplianceStandard(Measure fhirMeasure) {
        ComplianceStandard standard = new ComplianceStandard();

        // Set FHIR UUID
        if (fhirMeasure.hasId()) {
            try {
                standard.setFhirUuid(UUID.fromString(fhirMeasure.getId()));
            } catch (IllegalArgumentException e) {
                // Generate new UUID if provided ID is not valid UUID
                standard.setFhirUuid(UUID.randomUUID());
            }
        } else {
            standard.setFhirUuid(UUID.randomUUID());
        }

        // Basic fields
        if (fhirMeasure.hasTitle()) {
            standard.setName(fhirMeasure.getTitle());
        }

        if (fhirMeasure.hasPublisher()) {
            standard.setIssuingBody(fhirMeasure.getPublisher());
        }

        if (fhirMeasure.hasVersion()) {
            standard.setVersion(fhirMeasure.getVersion());
        }

        if (fhirMeasure.hasDescription()) {
            standard.setDescription(fhirMeasure.getDescription());
        }

        // Extract regulation number from identifier
        if (fhirMeasure.hasIdentifier()) {
            for (org.hl7.fhir.r4.model.Identifier identifier : fhirMeasure.getIdentifier()) {
                if (identifier.hasSystem() && identifier.getSystem().contains("regulation-number")
                        && identifier.hasValue()) {
                    standard.setRegulationNumber(identifier.getValue());
                    break;
                }
            }
        }

        // Set status
        if (fhirMeasure.hasStatus()) {
            switch (fhirMeasure.getStatus()) {
            case ACTIVE:
                standard.setStatus(org.openelisglobal.compliance.valueholder.ComplianceStandardStatus.ACTIVE);
                break;
            case DRAFT:
                standard.setStatus(org.openelisglobal.compliance.valueholder.ComplianceStandardStatus.DRAFT);
                break;
            case RETIRED:
                standard.setStatus(org.openelisglobal.compliance.valueholder.ComplianceStandardStatus.ARCHIVED);
                break;
            case UNKNOWN:
                standard.setStatus(org.openelisglobal.compliance.valueholder.ComplianceStandardStatus.SUSPENDED);
                break;
            default:
                standard.setStatus(org.openelisglobal.compliance.valueholder.ComplianceStandardStatus.DRAFT);
            }
        }

        // Extract effective dates
        if (fhirMeasure.hasEffectivePeriod()) {
            org.hl7.fhir.r4.model.Period period = fhirMeasure.getEffectivePeriod();
            if (period.hasStart()) {
                standard.setEffectiveDate(new java.sql.Date(period.getStart().getTime()).toLocalDate());
            }
            if (period.hasEnd()) {
                standard.setExpiryDate(new java.sql.Date(period.getEnd().getTime()).toLocalDate());
            }
        }

        // Extract jurisdiction (country/region)
        if (fhirMeasure.hasJurisdiction()) {
            for (org.hl7.fhir.r4.model.CodeableConcept jurisdiction : fhirMeasure.getJurisdiction()) {
                if (jurisdiction.hasCoding()) {
                    for (org.hl7.fhir.r4.model.Coding coding : jurisdiction.getCoding()) {
                        if (coding.hasCode()) {
                            standard.setCountryRegion(coding.getCode());
                            break;
                        }
                    }
                }
                if (standard.getCountryRegion() != null)
                    break;
            }
        }

        // Extract extensions
        if (fhirMeasure.hasExtension()) {
            for (org.hl7.fhir.r4.model.Extension extension : fhirMeasure.getExtension()) {
                if (extension.getUrl().contains("enforcement-authority") && extension.hasValue()
                        && extension.getValue() instanceof org.hl7.fhir.r4.model.StringType) {
                    standard.setEnforcementAuthority(
                            ((org.hl7.fhir.r4.model.StringType) extension.getValue()).getValue());
                }
            }
        }

        return standard;
    }
}