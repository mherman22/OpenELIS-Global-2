package org.openelisglobal.compliance.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.compliance.valueholder.EvaluationResult;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;

/**
 * DAO interface for EvaluationResult entity.
 *
 * Follows OpenELIS DAO patterns: - Extends BaseDAO for standard CRUD operations
 * - Provides domain-specific query methods - Supports relationship queries
 * between evaluation results and their associated evaluations and thresholds
 */
public interface EvaluationResultDAO extends BaseDAO<EvaluationResult, String> {

    /**
     * Get all results associated with a specific evaluation
     *
     * @param evaluationId The evaluation ID
     * @return List of EvaluationResult entities
     */
    List<EvaluationResult> getResultsByEvaluationId(String evaluationId);

    /**
     * Get all results for a specific parameter name
     *
     * @param parameterName The parameter name
     * @return List of EvaluationResult entities
     */
    List<EvaluationResult> getResultsByParameterName(String parameterName);

    /**
     * Get all results with a specific compliance status
     *
     * @param isCompliant The compliance status (true for compliant, false for non-compliant)
     * @return List of EvaluationResult entities
     */
    List<EvaluationResult> getResultsByStatus(Boolean isCompliant);

    /**
     * Get all results associated with a specific threshold
     *
     * @param thresholdId The threshold ID
     * @return List of EvaluationResult entities
     */
    List<EvaluationResult> getResultsByThresholdId(String thresholdId);

    /**
     * Get a specific result by evaluation and parameter
     *
     * @param evaluationId  The evaluation ID
     * @param parameterName The parameter name
     * @return EvaluationResult entity or null if not found
     */
    EvaluationResult getResultByEvaluationAndParameter(String evaluationId, String parameterName);

    /**
     * Delete all results for a specific evaluation
     *
     * @param evaluationId The evaluation ID
     */
    void deleteResultsByEvaluationId(String evaluationId);

    /**
     * Get paginated list of results
     *
     * @param startingRecNo The starting record number for pagination
     * @return List of EvaluationResult entities
     */
    List<EvaluationResult> getPageOfResults(int startingRecNo);

    /**
     * Get total count of results
     *
     * @return Total number of results
     */
    Integer getTotalResultCount();

    /**
     * Search results by multiple criteria
     *
     * @param evaluationId  The evaluation ID (optional)
     * @param parameterName The parameter name (optional)
     * @param isCompliant   The compliance status (optional)
     * @return List of matching EvaluationResult entities
     */
    List<EvaluationResult> searchResults(String evaluationId, String parameterName, Boolean isCompliant);

    /**
     * Get results for export (minimal data)
     *
     * @return List of EvaluationResult entities suitable for export
     */
    List<EvaluationResult> getResultsForExport();

    /**
     * Get result statistics
     *
     * @return List of Object arrays containing statistical data
     */
    List<Object[]> getResultStatistics();

    /**
     * Load data for an EvaluationResult entity
     *
     * @param evaluationResult The entity to load data for
     */
    void getData(EvaluationResult evaluationResult);
}