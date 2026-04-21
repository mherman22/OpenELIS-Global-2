package org.openelisglobal.compliance.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.ComplianceEvaluation;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;

/**
 * Service interface for ComplianceEvaluation operations.
 *
 * Follows OpenELIS service patterns extending BaseObjectService for standard
 * CRUD operations and providing domain-specific business logic for compliance
 * evaluation management.
 *
 * Constitutional compliance: - Extends BaseObjectService for standardized
 * operations - Declares transaction boundaries at service level - Provides
 * business logic validation methods - Supports FHIR integration requirements
 */
public interface ComplianceEvaluationService extends BaseObjectService<ComplianceEvaluation, String> {

    /**
     * Get compliance evaluation by FHIR UUID
     */
    ComplianceEvaluation getEvaluationByFhirId(UUID fhirUuid);

    /**
     * Get evaluations by compliance standard
     */
    List<ComplianceEvaluation> getEvaluationsByComplianceStandard(String standardId);

    /**
     * Get evaluations by sample ID
     */
    List<ComplianceEvaluation> getEvaluationsBySampleId(String sampleId);

    /**
     * Get evaluations by status
     */
    List<ComplianceEvaluation> getEvaluationsByStatus(EvaluationStatus status);

    /**
     * Get evaluations within date range
     */
    List<ComplianceEvaluation> getEvaluationsInRange(Date startDate, Date endDate);

    /**
     * Get evaluations since a specific date
     */
    List<ComplianceEvaluation> getEvaluationsSince(Date sinceDate);

    /**
     * Get evaluation with eagerly loaded results (Constitutional requirement:
     * compile data within transaction)
     */
    ComplianceEvaluation getEvaluationWithResults(String evaluationId);

    /**
     * Get paginated list of evaluations
     */
    List<ComplianceEvaluation> getPageOfEvaluations(int startingRecNo);

    /**
     * Get paginated search results
     */
    List<ComplianceEvaluation> getPagesOfSearchedEvaluations(int startingRecNo, String searchString);

    /**
     * Get total count of evaluations
     */
    Integer getTotalEvaluationCount();

    /**
     * Get total count of searched evaluations
     */
    Integer getTotalSearchedEvaluationCount(String searchString);

    /**
     * Search evaluations by multiple criteria
     */
    List<ComplianceEvaluation> searchEvaluations(String sampleId, String standardId, EvaluationStatus status,
            Date startDate, Date endDate);

    /**
     * Get evaluations for export (minimal data)
     */
    List<ComplianceEvaluation> getEvaluationsForExport();

    /**
     * Get evaluation statistics
     */
    List<Object[]> getEvaluationStatistics();

    /**
     * Get compliance rate for a standard
     */
    Double getComplianceRate(String standardId);

    /**
     * Get compliance trend over time
     */
    List<Object[]> getComplianceTrend(String standardId, Date startDate, Date endDate);

    /**
     * Validate evaluation before save (business rules)
     */
    void validateEvaluation(ComplianceEvaluation evaluation);

    /**
     * Check if evaluation can be deleted (business rules)
     */
    boolean canDelete(String evaluationId);

    /**
     * Check if evaluation can be modified (business rules)
     */
    boolean canModify(String evaluationId);

    /**
     * Mark evaluation as reviewed
     */
    void markAsReviewed(String evaluationId, String reviewedBy);

    /**
     * Recalculate evaluation results
     */
    void recalculateEvaluation(String evaluationId);

    // FHIR R4 integration methods

    /**
     * Get compliance evaluation by FHIR UUID string (Convenience method for FHIR
     * providers expecting string ID)
     */
    ComplianceEvaluation getComplianceEvaluationByFhirId(String fhirIdString);

    /**
     * Get evaluations by compliance standard FHIR ID
     */
    List<ComplianceEvaluation> getEvaluationsByComplianceStandardFhirId(String standardFhirId);

    /**
     * Get recent evaluations (default last 30 days)
     */
    List<ComplianceEvaluation> getRecentEvaluations();

    /**
     * Get evaluations by sample identifier
     */
    List<ComplianceEvaluation> getEvaluationsBySampleIdentifier(String sampleIdentifier);
}