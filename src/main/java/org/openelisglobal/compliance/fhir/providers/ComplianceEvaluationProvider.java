package org.openelisglobal.compliance.fhir.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.fhir.ComplianceFhirTransform;
import org.openelisglobal.compliance.service.ComplianceEvaluationService;
import org.openelisglobal.compliance.service.EvaluationResultService;
import org.openelisglobal.compliance.valueholder.ComplianceEvaluation;
import org.openelisglobal.compliance.valueholder.EvaluationResult;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.fhir.providers.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR Resource Provider for ComplianceEvaluation (mapped to FHIR
 * MeasureReport) Provides REST API endpoints for accessing compliance
 * evaluation results
 *
 * Endpoints: - GET /fhir/MeasureReport/{id} - Read specific evaluation - POST
 * /fhir/MeasureReport - Create new evaluation (usually system-generated) - PUT
 * /fhir/MeasureReport/{id} - Update existing evaluation - GET
 * /fhir/MeasureReport?measure={standard}&subject={sample}&date={period} -
 * Search evaluations
 */
@Component
public class ComplianceEvaluationProvider implements IResourceProvider {

    @Autowired
    private ComplianceFhirTransform fhirTransform;

    @Autowired
    private ComplianceEvaluationService complianceEvaluationService;

    @Autowired
    private EvaluationResultService evaluationResultService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return MeasureReport.class;
    }

    /**
     * READ: GET /fhir/MeasureReport/{id} Retrieve a specific compliance evaluation
     * by FHIR UUID
     */
    @Read
    public MeasureReport readComplianceEvaluation(@IdParam IdType theId) {
        String method = "Read";
        try {
            FhirProviderUtils.validateIdParam(theId, "MeasureReport", this.getClass().getSimpleName(), method);

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Reading ComplianceEvaluation with FHIR ID: " + theId.getIdPart());

            ComplianceEvaluation evaluation = complianceEvaluationService
                    .getComplianceEvaluationByFhirId(theId.getIdPart());

            if (evaluation == null) {
                throw new ResourceNotFoundException(
                        "ComplianceEvaluation not found with FHIR ID: " + theId.getIdPart());
            }

            // Get associated evaluation results
            List<EvaluationResult> results = evaluationResultService.getResultsByEvaluationId(evaluation.getId());

            // Transform to FHIR MeasureReport
            MeasureReport fhirReport = fhirTransform.transformToFhirMeasureReport(evaluation);

            // Add detailed results
            if (results != null && !results.isEmpty()) {
                fhirTransform.addEvaluationResultsToReport(fhirReport, results);
            }

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Successfully retrieved ComplianceEvaluation for sample: " + evaluation.getSampleId());

            return fhirReport;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while reading ComplianceEvaluation: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while reading ComplianceEvaluation", e);
        }
    }

    /**
     * CREATE: POST /fhir/MeasureReport Create a new compliance evaluation from FHIR
     * MeasureReport resource Note: Usually evaluations are system-generated, but
     * this supports external submissions
     */
    @Create
    public MethodOutcome createComplianceEvaluation(@ResourceParam MeasureReport fhirReport, HttpServletRequest request)
            throws FhirLocalPersistingException {

        String method = "create";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR CREATE request for ComplianceEvaluation");

        try {
            if (fhirReport == null) {
                throw new InvalidRequestException("MeasureReport resource cannot be null");
            }

            // Validate required fields
            if (!fhirReport.hasMeasure()) {
                throw new InvalidRequestException("MeasureReport must reference a Measure");
            }

            if (!fhirReport.hasStatus()) {
                throw new InvalidRequestException("MeasureReport status is required");
            }

            // Generate FHIR UUID if not provided
            if (!fhirReport.hasId()) {
                fhirReport.setId(UUID.randomUUID().toString());
            }

            // Transform FHIR MeasureReport to ComplianceEvaluation
            ComplianceEvaluation evaluation = transformToComplianceEvaluation(fhirReport);

            // Set system user ID
            evaluation.setSysUserId(FhirProviderUtils.getSysUserId(request));

            // Save to database
            ComplianceEvaluation savedEvaluation = complianceEvaluationService.save(evaluation);

            if (savedEvaluation == null) {
                throw new InternalErrorException("Failed to save ComplianceEvaluation");
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), method, "Created ComplianceEvaluation for sample: "
                    + savedEvaluation.getSampleId() + " with ID: " + savedEvaluation.getId());

            // Transform back to FHIR and return
            MeasureReport response = fhirTransform.transformToFhirMeasureReport(savedEvaluation);

            return FhirProviderUtils.buildCreateOutcome(response);

        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error creating ComplianceEvaluation: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while creating ComplianceEvaluation", e);
        }
    }

    /**
     * UPDATE: PUT /fhir/MeasureReport/{id} Update an existing compliance evaluation
     */
    @Update
    public MethodOutcome updateComplianceEvaluation(@IdParam IdType theId, @ResourceParam MeasureReport fhirReport,
            HttpServletRequest request) throws FhirLocalPersistingException {

        String method = "update";
        try {
            FhirProviderUtils.validateIdParam(theId, "MeasureReport", this.getClass().getSimpleName(), method);

            if (fhirReport == null) {
                throw new InvalidRequestException("MeasureReport resource cannot be null");
            }

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Updating ComplianceEvaluation with FHIR ID: " + theId.getIdPart());

            // Find existing evaluation
            ComplianceEvaluation existingEvaluation = complianceEvaluationService
                    .getComplianceEvaluationByFhirId(theId.getIdPart());

            if (existingEvaluation == null) {
                throw new ResourceNotFoundException(
                        "ComplianceEvaluation not found with FHIR ID: " + theId.getIdPart());
            }

            // Transform FHIR updates to entity
            ComplianceEvaluation updatedEvaluation = transformToComplianceEvaluation(fhirReport);
            updatedEvaluation.setId(existingEvaluation.getId()); // Preserve database ID
            updatedEvaluation.setFhirUuid(existingEvaluation.getFhirUuid()); // Preserve FHIR UUID

            // Set system user ID
            updatedEvaluation.setSysUserId(FhirProviderUtils.getSysUserId(request));

            // Save updates
            ComplianceEvaluation savedEvaluation = complianceEvaluationService.update(updatedEvaluation);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Updated ComplianceEvaluation for sample: " + savedEvaluation.getSampleId());

            // Transform back to FHIR and return
            MeasureReport response = fhirTransform.transformToFhirMeasureReport(savedEvaluation);

            return FhirProviderUtils.buildUpdateOutcome(response);

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error updating ComplianceEvaluation: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while updating ComplianceEvaluation", e);
        }
    }

    /**
     * SEARCH: GET
     * /fhir/MeasureReport?measure={standard}&subject={sample}&date={period}&status={status}
     * Search compliance evaluations by various criteria
     */
    @Search
    public Bundle searchComplianceEvaluations(@OptionalParam(name = "measure") ReferenceParam measure,
            @OptionalParam(name = "subject") ReferenceParam subject,
            @OptionalParam(name = "date") DateRangeParam dateRange, @OptionalParam(name = "status") TokenParam status,
            @OptionalParam(name = "identifier") TokenParam identifier) {

        String method = "search";
        try {
            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Searching ComplianceEvaluations with parameters - measure: " + measure + ", subject: " + subject
                            + ", date: " + dateRange + ", status: " + status);

            List<ComplianceEvaluation> evaluations = new ArrayList<>();

            if (measure != null) {
                // Search by compliance standard (Measure reference)
                String measureId = extractIdFromReference(measure.getValue());
                evaluations.addAll(complianceEvaluationService.getEvaluationsByComplianceStandardFhirId(measureId));
            } else if (subject != null) {
                // Search by sample/specimen (subject reference)
                String sampleId = extractIdFromReference(subject.getValue());
                evaluations.addAll(complianceEvaluationService.getEvaluationsBySampleId(sampleId));
            } else if (identifier != null) {
                // Search by sample identifier
                evaluations.addAll(complianceEvaluationService.getEvaluationsBySampleId(identifier.getValue()));
            } else {
                // Default: get recent evaluations (last 30 days)
                Date thirtyDaysAgo = new Date(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000));
                evaluations.addAll(complianceEvaluationService.getEvaluationsSince(thirtyDaysAgo));
            }

            // Filter by date range if specified
            if (dateRange != null && !evaluations.isEmpty()) {
                Date lowerBound = dateRange.getLowerBound() != null
                        ? dateRange.getLowerBound().getValue()
                        : null;
                Date upperBound = dateRange.getUpperBound() != null
                        ? dateRange.getUpperBound().getValue()
                        : null;

                evaluations.removeIf(evaluation -> {
                    Date evalDate = evaluation.getEvaluatedDate();
                    if (evalDate == null)
                        return true;

                    if (lowerBound != null && evalDate.before(lowerBound))
                        return true;
                    if (upperBound != null && evalDate.after(upperBound))
                        return true;

                    return false;
                });
            }

            // Filter by status if specified
            if (status != null && !evaluations.isEmpty()) {
                String statusValue = status.getValue().toUpperCase();
                evaluations.removeIf(evaluation -> !evaluation.getStatus().toString().equals(statusValue));
            }

            // Create Bundle
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.SEARCHSET);
            bundle.setTotal(evaluations.size());

            // Transform entities to FHIR resources
            for (ComplianceEvaluation evaluation : evaluations) {
                try {
                    // Get associated results
                    List<EvaluationResult> results = evaluationResultService
                            .getResultsByEvaluationId(evaluation.getId());

                    MeasureReport report = fhirTransform.transformToFhirMeasureReport(evaluation);

                    // Add detailed results if available
                    if (results != null && !results.isEmpty()) {
                        fhirTransform.addEvaluationResultsToReport(report, results);
                    }

                    Bundle.BundleEntryComponent entry = bundle.addEntry();
                    entry.setResource(report);
                    entry.setFullUrl("MeasureReport/" + report.getId());
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getSimpleName(), method,
                            "Error transforming ComplianceEvaluation to FHIR: " + e.getMessage());
                    // Continue with other evaluations
                }
            }

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Search completed, returning " + bundle.getEntry().size() + " results");

            return bundle;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error searching ComplianceEvaluations: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while searching ComplianceEvaluations", e);
        }
    }

    /**
     * GET /fhir/MeasureReport?measure={standardId}&_summary=count Get compliance
     * statistics for a specific standard
     */
    @Search
    public Bundle getComplianceStatistics(@OptionalParam(name = "measure") ReferenceParam measure,
            @OptionalParam(name = "_summary") StringParam summary) {

        String method = "getComplianceStatistics";
        try {
            if (measure == null || summary == null || !"count".equals(summary.getValue())) {
                throw new InvalidRequestException("This operation requires measure parameter and _summary=count");
            }

            String measureId = extractIdFromReference(measure.getValue());
            List<ComplianceEvaluation> evaluations = complianceEvaluationService
                    .getEvaluationsByComplianceStandardFhirId(measureId);

            // Calculate statistics
            long totalEvaluations = evaluations.size();
            long compliantCount = evaluations.stream()
                    .mapToLong(e -> EvaluationStatus.COMPLIANT.equals(e.getStatus()) ? 1 : 0).sum();
            long nonCompliantCount = evaluations.stream()
                    .mapToLong(e -> EvaluationStatus.NON_COMPLIANT.equals(e.getStatus()) ? 1 : 0).sum();
            long warningCount = evaluations.stream()
                    .mapToLong(e -> EvaluationStatus.WARNING.equals(e.getStatus()) ? 1 : 0).sum();

            // Create summary Bundle
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.SEARCHSET);
            bundle.setTotal((int) totalEvaluations);

            // Add statistics as Bundle metadata (extensions)
            org.hl7.fhir.r4.model.Extension statisticsExt = new org.hl7.fhir.r4.model.Extension("http://openelis.org/fhir/extension/compliance-statistics");
            statisticsExt.addExtension("total", new org.hl7.fhir.r4.model.IntegerType((int) totalEvaluations));
            statisticsExt.addExtension("compliant", new org.hl7.fhir.r4.model.IntegerType((int) compliantCount));
            statisticsExt.addExtension("non-compliant", new org.hl7.fhir.r4.model.IntegerType((int) nonCompliantCount));
            statisticsExt.addExtension("warning", new org.hl7.fhir.r4.model.IntegerType((int) warningCount));
            bundle.getMeta().addExtension(statisticsExt);

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Statistics for standard " + measureId + ": Total=" + totalEvaluations + ", Compliant="
                            + compliantCount + ", Non-Compliant=" + nonCompliantCount + ", Warning=" + warningCount);

            return bundle;

        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error getting compliance statistics: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while getting compliance statistics", e);
        }
    }

    // Private helper methods

    /**
     * Transform FHIR MeasureReport to ComplianceEvaluation entity
     */
    private ComplianceEvaluation transformToComplianceEvaluation(MeasureReport fhirReport) {
        ComplianceEvaluation evaluation = new ComplianceEvaluation();

        // Set FHIR UUID
        if (fhirReport.hasId()) {
            try {
                evaluation.setFhirUuid(UUID.fromString(fhirReport.getId()));
            } catch (IllegalArgumentException e) {
                evaluation.setFhirUuid(UUID.randomUUID());
            }
        } else {
            evaluation.setFhirUuid(UUID.randomUUID());
        }

        // Extract measure reference (compliance standard)
        if (fhirReport.hasMeasure()) {
            String measureId = extractIdFromReference(fhirReport.getMeasure());
            // Would need to look up ComplianceStandard by FHIR ID
            // evaluation.setComplianceStandardId(complianceStandardService.getIdByFhirId(measureId));
        }

        // Extract subject reference (sample)
        if (fhirReport.hasSubject()) {
            String sampleId = extractIdFromReference(fhirReport.getSubject().getReference());
            evaluation.setSampleId(sampleId);
        }

        // Set evaluation date
        if (fhirReport.hasDate()) {
            evaluation.setEvaluatedDate(fhirReport.getDate());
        }

        // Set status
        if (fhirReport.hasStatus()) {
            switch (fhirReport.getStatus()) {
            case COMPLETE:
                evaluation.setStatus(EvaluationStatus.COMPLIANT); // Default to compliant for complete
                break;
            case PENDING:
                evaluation.setStatus(EvaluationStatus.PENDING);
                break;
            default:
                evaluation.setStatus(EvaluationStatus.PENDING);
            }
        }

        // Extract compliance metrics from groups
        if (fhirReport.hasGroup()) {
            for (MeasureReport.MeasureReportGroupComponent group : fhirReport.getGroup()) {
                if (group.hasMeasureScore() && group.getMeasureScore().hasValue()) {
                    // Assume overall compliance percentage
                    double percentage = group.getMeasureScore().getValue().doubleValue();
                    // Set status based on percentage thresholds
                    if (percentage >= 95.0) {
                        evaluation.setStatus(EvaluationStatus.COMPLIANT);
                    } else if (percentage >= 80.0) {
                        evaluation.setStatus(EvaluationStatus.WARNING);
                    } else {
                        evaluation.setStatus(EvaluationStatus.NON_COMPLIANT);
                    }
                }

                // Extract population counts
                if (group.hasPopulation()) {
                    for (MeasureReport.MeasureReportGroupPopulationComponent pop : group.getPopulation()) {
                        if (pop.hasCode() && pop.getCode().hasCoding()) {
                            String popCode = pop.getCode().getCodingFirstRep().getCode();
                            if ("measure-population".equals(popCode) && pop.hasCount()) {
                                // Note: setTotalParameters method not implemented
                                // evaluation.setTotalParameters(pop.getCount());
                            } else if ("compliant".equals(popCode) && pop.hasCount()) {
                                // Note: setCompliantParameters method not implemented
                                // evaluation.setCompliantParameters(pop.getCount());
                            }
                        }
                    }
                }
            }
        }

        // Extract notes from extensions
        if (fhirReport.hasExtension()) {
            for (org.hl7.fhir.r4.model.Extension extension : fhirReport.getExtension()) {
                if (extension.getUrl().contains("evaluation-notes") && extension.hasValue()
                        && extension.getValue() instanceof org.hl7.fhir.r4.model.StringType) {
                    evaluation.setEvaluationNotes(((org.hl7.fhir.r4.model.StringType) extension.getValue()).getValue());
                    break;
                }
            }
        }

        return evaluation;
    }

    /**
     * Extract ID part from FHIR reference (e.g., "Measure/123" -> "123")
     */
    private String extractIdFromReference(String reference) {
        if (reference == null)
            return null;
        if (reference.contains("/")) {
            return reference.substring(reference.lastIndexOf("/") + 1);
        }
        return reference;
    }
}