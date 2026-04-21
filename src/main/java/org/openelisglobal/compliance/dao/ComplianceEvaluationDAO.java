package org.openelisglobal.compliance.dao;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ComplianceEvaluation;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;

/**
 * DAO interface for ComplianceEvaluation entity operations.
 *
 * Follows OpenELIS DAO patterns extending BaseDAO for standard CRUD operations.
 * Provides domain-specific query methods for evaluation management.
 */
public interface ComplianceEvaluationDAO extends BaseDAO<ComplianceEvaluation, String> {

    /**
     * Get compliance evaluation by FHIR UUID
     */
    ComplianceEvaluation getEvaluationByFhirId(UUID fhirUuid) throws LIMSRuntimeException;

    /**
     * Get evaluations by compliance standard
     */
    List<ComplianceEvaluation> getEvaluationsByComplianceStandard(String standardId) throws LIMSRuntimeException;

    /**
     * Get evaluations by sample ID
     */
    List<ComplianceEvaluation> getEvaluationsBySampleId(String sampleId) throws LIMSRuntimeException;

    /**
     * Get evaluations by status
     */
    List<ComplianceEvaluation> getEvaluationsByStatus(EvaluationStatus status) throws LIMSRuntimeException;

    /**
     * Get evaluations within date range
     */
    List<ComplianceEvaluation> getEvaluationsInRange(Date startDate, Date endDate) throws LIMSRuntimeException;

    /**
     * Get evaluations since a specific date
     */
    List<ComplianceEvaluation> getEvaluationsSince(Date sinceDate) throws LIMSRuntimeException;

    /**
     * Get evaluation with eagerly loaded results
     */
    ComplianceEvaluation getEvaluationWithResults(String evaluationId) throws LIMSRuntimeException;

    /**
     * Get paginated list of evaluations
     */
    List<ComplianceEvaluation> getPageOfEvaluations(int startingRecNo) throws LIMSRuntimeException;

    /**
     * Get paginated search results
     */
    List<ComplianceEvaluation> getPagesOfSearchedEvaluations(int startingRecNo, String searchString) throws LIMSRuntimeException;

    /**
     * Get total count of evaluations
     */
    Integer getTotalEvaluationCount() throws LIMSRuntimeException;

    /**
     * Get total count of searched evaluations
     */
    Integer getTotalSearchedEvaluationCount(String searchString) throws LIMSRuntimeException;

    /**
     * Search evaluations by multiple criteria
     */
    List<ComplianceEvaluation> searchEvaluations(String sampleId, String standardId, EvaluationStatus status,
            Date startDate, Date endDate) throws LIMSRuntimeException;

    /**
     * Get evaluations for export (minimal data)
     */
    List<ComplianceEvaluation> getEvaluationsForExport() throws LIMSRuntimeException;

    /**
     * Get evaluation statistics
     */
    List<Object[]> getEvaluationStatistics() throws LIMSRuntimeException;

    /**
     * Get compliance rate for a standard
     */
    Double getComplianceRate(String standardId) throws LIMSRuntimeException;

    /**
     * Get compliance trend over time
     */
    List<Object[]> getComplianceTrend(String standardId, Date startDate, Date endDate) throws LIMSRuntimeException;

    /**
     * Get evaluations by compliance standard FHIR ID
     */
    List<ComplianceEvaluation> getEvaluationsByComplianceStandardFhirId(String standardFhirId) throws LIMSRuntimeException;

    /**
     * Get evaluations by sample identifier
     */
    List<ComplianceEvaluation> getEvaluationsBySampleIdentifier(String sampleIdentifier) throws LIMSRuntimeException;

    /**
     * Get evaluations by date range for a specific standard
     */
    List<ComplianceEvaluation> getEvaluationsByDateRange(String standardId, Date startDate, Date endDate) throws LIMSRuntimeException;
}