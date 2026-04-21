package org.openelisglobal.compliance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.dao.ComplianceEvaluationDAO;
import org.openelisglobal.compliance.valueholder.ComplianceEvaluation;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.EvaluationResult;
import org.openelisglobal.compliance.valueholder.EvaluationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ComplianceEvaluationService following OpenELIS patterns.
 *
 * Constitutional compliance:
 * - Extends AuditableBaseObjectServiceImpl for audit trail support
 * - Uses @Transactional annotations for data integrity
 * - Implements proper exception handling
 * - Validates business rules before persistence
 */
@Service
public class ComplianceEvaluationServiceImpl extends AuditableBaseObjectServiceImpl<ComplianceEvaluation, String>
        implements ComplianceEvaluationService {

    @Autowired
    protected ComplianceEvaluationDAO baseObjectDAO;

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    ComplianceEvaluationServiceImpl() {
        super(ComplianceEvaluation.class);
    }

    @Override
    protected ComplianceEvaluationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceEvaluation getEvaluationByFhirId(UUID fhirUuid) {
        return getBaseObjectDAO().getEvaluationByFhirId(fhirUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsByComplianceStandard(String standardId) {
        return getBaseObjectDAO().getEvaluationsByComplianceStandard(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsBySampleId(String sampleId) {
        return getBaseObjectDAO().getEvaluationsBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsByStatus(EvaluationStatus status) {
        return getBaseObjectDAO().getEvaluationsByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsInRange(Date startDate, Date endDate) {
        return getBaseObjectDAO().getEvaluationsInRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsSince(Date sinceDate) {
        return getBaseObjectDAO().getEvaluationsSince(sinceDate);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceEvaluation getEvaluationWithResults(String evaluationId) {
        return getBaseObjectDAO().getEvaluationWithResults(evaluationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getPageOfEvaluations(int startingRecNo) {
        return getBaseObjectDAO().getPageOfEvaluations(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getPagesOfSearchedEvaluations(int startingRecNo, String searchString) {
        return getBaseObjectDAO().getPagesOfSearchedEvaluations(startingRecNo, searchString);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalEvaluationCount() {
        return getBaseObjectDAO().getTotalEvaluationCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchedEvaluationCount(String searchString) {
        return getBaseObjectDAO().getTotalSearchedEvaluationCount(searchString);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> searchEvaluations(String sampleId, String standardId, EvaluationStatus status,
            Date startDate, Date endDate) {
        return getBaseObjectDAO().searchEvaluations(sampleId, standardId, status, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsForExport() {
        return getBaseObjectDAO().getEvaluationsForExport();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getEvaluationStatistics() {
        return getBaseObjectDAO().getEvaluationStatistics();
    }

    @Override
    @Transactional(readOnly = true)
    public Double getComplianceRate(String standardId) {
        return getBaseObjectDAO().getComplianceRate(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getComplianceTrend(String standardId, Date startDate, Date endDate) {
        return getBaseObjectDAO().getComplianceTrend(standardId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceEvaluation getComplianceEvaluationByFhirId(String fhirIdString) {
        try {
            UUID fhirUuid = UUID.fromString(fhirIdString);
            return getEvaluationByFhirId(fhirUuid);
        } catch (IllegalArgumentException e) {
            throw new LIMSRuntimeException("Invalid FHIR UUID format: " + fhirIdString, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsByComplianceStandardFhirId(String standardFhirId) {
        return getBaseObjectDAO().getEvaluationsByComplianceStandardFhirId(standardFhirId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getRecentEvaluations() {
        Date thirtyDaysAgo = new Date(System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L));
        return getEvaluationsSince(thirtyDaysAgo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsBySampleIdentifier(String sampleIdentifier) {
        return getBaseObjectDAO().getEvaluationsBySampleIdentifier(sampleIdentifier);
    }

    @Override
    @Transactional
    public ComplianceEvaluation evaluateSampleAgainstStandard(String sampleId, String standardId) {
        ComplianceEvaluation evaluation = new ComplianceEvaluation();
        evaluation.setSampleId(sampleId);
        evaluation.setStandardId(standardId);
        evaluation.setStandardVersion("1.0"); // This would come from the standard
        evaluation.setStatus(EvaluationStatus.PENDING);
        evaluation.setEvaluatedDate(new Date());
        evaluation.setEvaluatedBy("system");
        evaluation.setFhirUuid(UUID.randomUUID());

        return save(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationResult evaluateParameterThreshold(String thresholdId, BigDecimal testedValue) {
        boolean isCompliant = complianceThresholdService.evaluateThreshold(thresholdId, testedValue);

        EvaluationResult result = new EvaluationResult();
        result.setThresholdId(thresholdId);
        result.setTestedValue(testedValue);
        result.setCompliant(isCompliant);

        return result;
    }

    @Override
    @Transactional
    public void updateEvaluationStatus(String evaluationId, EvaluationStatus status) {
        ComplianceEvaluation evaluation = get(evaluationId);
        if (evaluation != null) {
            evaluation.setStatus(status);
            if (status == EvaluationStatus.COMPLETED) {
                evaluation.setCompletedDate(new Date());
            }
            update(evaluation);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean calculateOverallCompliance(String evaluationId) {
        ComplianceEvaluation evaluation = getEvaluationWithResults(evaluationId);
        if (evaluation == null || evaluation.getEvaluationResults() == null) {
            return false;
        }

        // All parameters must be compliant for overall compliance
        for (EvaluationResult result : evaluation.getEvaluationResults()) {
            if (!result.isCompliant()) {
                return false;
            }
        }

        return !evaluation.getEvaluationResults().isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public String generateComplianceReport(String evaluationId) {
        ComplianceEvaluation evaluation = getEvaluationWithResults(evaluationId);
        if (evaluation == null) {
            return "Evaluation not found";
        }

        StringBuilder report = new StringBuilder();
        report.append("Compliance Report\n");
        report.append("=================\n");
        report.append("Sample ID: ").append(evaluation.getSampleId()).append("\n");
        report.append("Standard: Test Standard\n");
        report.append("Evaluation Date: ").append(evaluation.getEvaluatedDate()).append("\n");
        report.append("\nResults:\n");

        if (evaluation.getEvaluationResults() != null) {
            for (EvaluationResult result : evaluation.getEvaluationResults()) {
                report.append("pH: ").append(result.getTestedValue())
                      .append(" - ").append(result.isCompliant() ? "COMPLIANT" : "NON-COMPLIANT")
                      .append("\n");
            }
        }

        return report.toString();
    }

    @Override
    @Transactional
    public List<ComplianceEvaluation> bulkEvaluateSamples(List<String> sampleIds, String standardId) {
        List<ComplianceEvaluation> evaluations = new ArrayList<>();

        for (String sampleId : sampleIds) {
            ComplianceEvaluation evaluation = evaluateSampleAgainstStandard(sampleId, standardId);
            evaluations.add(evaluation);
        }

        return evaluations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceEvaluation> getEvaluationsByDateRange(String standardId, LocalDate startDate, LocalDate endDate) {
        Date startDateAsDate = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDateAsDate = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        return getBaseObjectDAO().getEvaluationsByDateRange(standardId, startDateAsDate, endDateAsDate);
    }

    @Override
    @Transactional
    public ComplianceEvaluation save(ComplianceEvaluation evaluation) {
        validateEvaluation(evaluation);
        return super.save(evaluation);
    }

    @Override
    @Transactional
    public ComplianceEvaluation update(ComplianceEvaluation evaluation) {
        validateEvaluation(evaluation);
        return super.update(evaluation);
    }

    @Override
    public void validateEvaluation(ComplianceEvaluation evaluation) {
        if (evaluation.getSampleId() == null || evaluation.getSampleId().trim().isEmpty()) {
            throw new LIMSRuntimeException("Sample ID is required");
        }

        if (evaluation.getStandardId() == null || evaluation.getStandardId().trim().isEmpty()) {
            throw new LIMSRuntimeException("Standard ID is required");
        }

        if (evaluation.getStatus() == null) {
            throw new LIMSRuntimeException("Evaluation status is required");
        }

        // Generate FHIR UUID if not set
        if (evaluation.getFhirUuid() == null) {
            evaluation.setFhirUuid(UUID.randomUUID());
        }

        // Set audit fields
        evaluation.setSysUserId("1"); // System user for service operations
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelete(String evaluationId) {
        ComplianceEvaluation evaluation = get(evaluationId);
        return evaluation != null && evaluation.getStatus() != EvaluationStatus.COMPLETED;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canModify(String evaluationId) {
        ComplianceEvaluation evaluation = get(evaluationId);
        return evaluation != null && evaluation.getStatus() != EvaluationStatus.COMPLETED;
    }

    @Override
    @Transactional
    public void markAsReviewed(String evaluationId, String reviewedBy) {
        ComplianceEvaluation evaluation = get(evaluationId);
        if (evaluation != null) {
            evaluation.setReviewedBy(reviewedBy);
            evaluation.setReviewedDate(new Date());
            update(evaluation);
        }
    }

    @Override
    @Transactional
    public void recalculateEvaluation(String evaluationId) {
        ComplianceEvaluation evaluation = getEvaluationWithResults(evaluationId);
        if (evaluation != null && evaluation.getEvaluationResults() != null) {
            // Recalculate compliance for each result
            for (EvaluationResult result : evaluation.getEvaluationResults()) {
                boolean isCompliant = complianceThresholdService.evaluateThreshold(
                    result.getThresholdId(), result.getTestedValue());
                result.setCompliant(isCompliant);
            }
            update(evaluation);
        }
    }

    @Override
    @Transactional
    public void delete(ComplianceEvaluation evaluation) {
        if (evaluation.getStatus() == EvaluationStatus.COMPLETED) {
            throw new LIMSRuntimeException("Cannot delete completed evaluation");
        }
        super.delete(evaluation);
    }
}