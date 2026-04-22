package org.openelisglobal.compliance.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.dao.EvaluationResultDAO;
import org.openelisglobal.compliance.valueholder.EvaluationResult;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for EvaluationResult operations.
 *
 * Follows OpenELIS service patterns extending AuditableBaseObjectServiceImpl
 * for standard CRUD operations with proper transaction boundaries and
 * domain-specific business logic.
 *
 * Constitutional compliance: - Extends AuditableBaseObjectServiceImpl<EvaluationResult, String>
 * - Uses @Service annotation for Spring component scanning - @Transactional
 * boundaries at service level (not controller) - Delegates to DAO layer for
 * data access - Implements business logic validation
 */
@Service
public class EvaluationResultServiceImpl extends AuditableBaseObjectServiceImpl<EvaluationResult, String>
        implements EvaluationResultService {

    @Autowired
    protected EvaluationResultDAO baseObjectDAO;

    EvaluationResultServiceImpl() {
        super(EvaluationResult.class);
    }

    @Override
    protected EvaluationResultDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsByEvaluationId(String evaluationId) {
        return baseObjectDAO.getResultsByEvaluationId(evaluationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsByParameterName(String parameterName) {
        return baseObjectDAO.getResultsByParameterName(parameterName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsByStatus(Boolean isCompliant) {
        return baseObjectDAO.getResultsByStatus(isCompliant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsByThresholdId(String thresholdId) {
        return baseObjectDAO.getResultsByThresholdId(thresholdId);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationResult getResultByEvaluationAndParameter(String evaluationId, String parameterName) {
        return baseObjectDAO.getResultByEvaluationAndParameter(evaluationId, parameterName);
    }

    @Override
    @Transactional
    public void saveResultsForEvaluation(String evaluationId, List<EvaluationResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        // Remove existing results for this evaluation
        deleteResultsByEvaluationId(evaluationId);

        // Save new results
        for (EvaluationResult result : results) {
            // Validate before save
            validateResult(result);
            save(result);
        }
    }

    @Override
    @Transactional
    public void deleteResultsByEvaluationId(String evaluationId) {
        baseObjectDAO.deleteResultsByEvaluationId(evaluationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getPageOfResults(int startingRecNo) {
        return baseObjectDAO.getPageOfResults(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalResultCount() {
        return baseObjectDAO.getTotalResultCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> searchResults(String evaluationId, String parameterName, Boolean isCompliant) {
        return baseObjectDAO.searchResults(evaluationId, parameterName, isCompliant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResult> getResultsForExport() {
        return baseObjectDAO.getResultsForExport();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getResultStatistics() {
        return baseObjectDAO.getResultStatistics();
    }

    @Override
    public void validateResult(EvaluationResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Evaluation result cannot be null");
        }

        if (result.getParameterCode() == null || result.getParameterCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter code is required");
        }

        if (result.getEvaluation() == null) {
            throw new IllegalArgumentException("Compliance evaluation is required");
        }

        if (result.getIsCompliant() == null) {
            throw new IllegalArgumentException("Compliance status is required");
        }

        if (result.getVariancePercentage() != null) {
            if (result.getVariancePercentage().compareTo(new java.math.BigDecimal("-999.99")) < 0 ||
                result.getVariancePercentage().compareTo(new java.math.BigDecimal("999.99")) > 0) {
                throw new IllegalArgumentException("Variance percentage must be between -999.99 and 999.99");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelete(String resultId) {
        // Business rule: Results can be deleted if they are not part of a finalized evaluation
        EvaluationResult result = get(resultId);
        if (result != null && result.getEvaluation() != null) {
            // Check if evaluation is finalized - this would need to check evaluation status
            // For now, allow deletion
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canModify(String resultId) {
        // Business rule: Results can be modified if they are not part of a finalized evaluation
        EvaluationResult result = get(resultId);
        if (result != null && result.getEvaluation() != null) {
            // Check if evaluation is finalized - this would need to check evaluation status
            // For now, allow modification
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void recalculateComplianceStatus(String resultId) {
        EvaluationResult result = get(resultId);
        if (result == null) {
            throw new IllegalArgumentException("Result not found for ID: " + resultId);
        }

        // Recalculate compliance status based on threshold comparison
        if (result.getThreshold() != null && result.getTestedValue() != null) {
            // Re-evaluate compliance using the entity's built-in method
            result.reevaluateCompliance();
            update(result);
        }
    }
}