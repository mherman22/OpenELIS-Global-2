package org.openelisglobal.compliance.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.EvaluationResult;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;

/**
 * Service interface for EvaluationResult operations.
 *
 * Follows OpenELIS service patterns extending BaseObjectService for standard
 * CRUD operations and providing domain-specific business logic for evaluation
 * result management.
 *
 * Constitutional compliance: - Extends BaseObjectService for standardized
 * operations - Declares transaction boundaries at service level - Provides
 * business logic validation methods
 */
public interface EvaluationResultService extends BaseObjectService<EvaluationResult, String> {

    /**
     * Get results by evaluation ID
     */
    List<EvaluationResult> getResultsByEvaluationId(String evaluationId);

    /**
     * Get results by parameter name
     */
    List<EvaluationResult> getResultsByParameterName(String parameterName);

    /**
     * Get results by status
     */
    List<EvaluationResult> getResultsByStatus(EvaluationStatus status);

    /**
     * Get results by threshold ID
     */
    List<EvaluationResult> getResultsByThresholdId(String thresholdId);

    /**
     * Get results for specific evaluation and parameter
     */
    EvaluationResult getResultByEvaluationAndParameter(String evaluationId, String parameterName);

    /**
     * Bulk save results for an evaluation
     */
    void saveResultsForEvaluation(String evaluationId, List<EvaluationResult> results);

    /**
     * Delete results by evaluation ID
     */
    void deleteResultsByEvaluationId(String evaluationId);

    /**
     * Get paginated list of results
     */
    List<EvaluationResult> getPageOfResults(int startingRecNo);

    /**
     * Get total count of results
     */
    Integer getTotalResultCount();

    /**
     * Search results by multiple criteria
     */
    List<EvaluationResult> searchResults(String evaluationId, String parameterName, EvaluationStatus status);

    /**
     * Get results for export
     */
    List<EvaluationResult> getResultsForExport();

    /**
     * Get result statistics
     */
    List<Object[]> getResultStatistics();

    /**
     * Validate result before save (business rules)
     */
    void validateResult(EvaluationResult result);

    /**
     * Check if result can be deleted (business rules)
     */
    boolean canDelete(String resultId);

    /**
     * Check if result can be modified (business rules)
     */
    boolean canModify(String resultId);

    /**
     * Recalculate compliance status for result
     */
    void recalculateComplianceStatus(String resultId);
}