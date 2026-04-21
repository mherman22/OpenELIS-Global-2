package org.openelisglobal.compliance.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.dao.ComplianceStandardDAO;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandardStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for ComplianceStandard operations.
 *
 * Follows OpenELIS service patterns extending AuditableBaseObjectServiceImpl
 * for standard CRUD operations with proper transaction boundaries and
 * domain-specific business logic.
 *
 * Constitutional compliance: - Extends
 * AuditableBaseObjectServiceImpl<ComplianceStandard, String> - Uses @Service
 * annotation for Spring component scanning - @Transactional boundaries at
 * service level (not controller) - Delegates to DAO layer for data access -
 * Implements business logic validation
 */
@Service
public class ComplianceStandardServiceImpl extends AuditableBaseObjectServiceImpl<ComplianceStandard, String>
        implements ComplianceStandardService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    protected ComplianceStandardDAO baseObjectDAO;

    ComplianceStandardServiceImpl() {
        super(ComplianceStandard.class);
    }

    @Override
    protected ComplianceStandardDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    // ================== FHIR Integration Methods ==================

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getStandardByFhirId(UUID fhirUuid) {
        return baseObjectDAO.getStandardByFhirId(fhirUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getComplianceStandardByFhirId(String fhirIdString) {
        try {
            UUID fhirUuid = UUID.fromString(fhirIdString);
            return getStandardByFhirId(fhirUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ================== Business Logic Methods ==================

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getStandardByNaturalKey(String issuingBody, String regulationNumber, String version) {
        return baseObjectDAO.getStandardByNaturalKey(issuingBody, regulationNumber, version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsByStatus(ComplianceStandardStatus status) {
        return baseObjectDAO.getStandardsByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getActiveStandardsBySampleType(String sampleType) {
        return baseObjectDAO.getActiveStandardsBySampleType(sampleType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsByCountryRegion(String countryRegion) {
        return baseObjectDAO.getStandardsByCountryRegion(countryRegion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsByIssuingBody(String issuingBody) {
        return baseObjectDAO.getStandardsByIssuingBody(issuingBody);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsEffectiveInRange(LocalDate startDate, LocalDate endDate) {
        return baseObjectDAO.getStandardsEffectiveInRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getStandardWithParameterGroups(String standardId) {
        return baseObjectDAO.getStandardWithParameterGroups(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getStandardWithFullHierarchy(String standardId) {
        return baseObjectDAO.getStandardWithFullHierarchy(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsWithGroupCounts() {
        return baseObjectDAO.getStandardsWithGroupCounts();
    }

    // ================== Pagination Methods ==================

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getPageOfStandards(int startingRecNo) {
        return baseObjectDAO.getPageOfStandards(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getPagesOfSearchedStandards(int startingRecNo, String searchString) {
        return baseObjectDAO.getPagesOfSearchedStandards(startingRecNo, searchString);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalStandardCount() {
        return baseObjectDAO.getTotalStandardCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchedStandardCount(String searchString) {
        return baseObjectDAO.getTotalSearchedStandardCount(searchString);
    }

    // ================== Business Rule Validation ==================

    @Override
    @Transactional(readOnly = true)
    public boolean duplicateStandardExists(ComplianceStandard standard) {
        return baseObjectDAO.duplicateStandardExists(standard);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean standardHasEvaluations(String standardId) {
        return baseObjectDAO.standardHasEvaluations(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean standardHasParameterGroups(String standardId) {
        return baseObjectDAO.standardHasParameterGroups(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getSupersededStandards(String supersededByStandardId) {
        return baseObjectDAO.getSupersededStandards(supersededByStandardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getVersionHistory(String issuingBody, String regulationNumber) {
        return baseObjectDAO.getVersionHistory(issuingBody, regulationNumber);
    }

    // ================== Search and Query Methods ==================

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> searchStandards(String name, String issuingBody, String regulationNumber,
            ComplianceStandardStatus status, String countryRegion, String sampleType) {
        return baseObjectDAO.searchStandards(name, issuingBody, regulationNumber, status, countryRegion, sampleType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsExpiringWithinDays(int days) {
        return baseObjectDAO.getStandardsExpiringWithinDays(days);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsForExport() {
        return baseObjectDAO.getStandardsForExport();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getLatestVersionStandards() {
        return baseObjectDAO.getLatestVersionStandards();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getSupersedableStandards() {
        return baseObjectDAO.getSupersedableStandards();
    }

    // ================== Additional Service Methods ==================

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getComplianceStandardsByName(String name) {
        return baseObjectDAO.getComplianceStandardsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getComplianceStandardByRegulationNumber(String regulationNumber) {
        return baseObjectDAO.getComplianceStandardByRegulationNumber(regulationNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getComplianceStandardsByIssuingBody(String issuingBody) {
        return getStandardsByIssuingBody(issuingBody);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getActiveComplianceStandards() {
        return getStandardsByStatus(ComplianceStandardStatus.ACTIVE);
    }

    // ================== Business Logic Operations ==================

    @Override
    @Transactional
    public void supersedseStandard(String oldStandardId, String newStandardId) {
        baseObjectDAO.supersedseStandard(oldStandardId, newStandardId);
    }

    @Override
    @Transactional
    public void archive(String standardId) {
        ComplianceStandard standard = get(standardId);
        if (standard != null) {
            standard.setStatus(ComplianceStandardStatus.ARCHIVED);
            save(standard);
        }
    }

    @Override
    @Transactional
    public void bulkUpdateStatus(List<String> standardIds, ComplianceStandardStatus newStatus, String userId) {
        for (String standardId : standardIds) {
            ComplianceStandard standard = get(standardId);
            if (standard != null) {
                standard.setStatus(newStatus);
                standard.setSysUserId(userId);
                save(standard);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getVersionForEvaluation(String standardId) {
        return baseObjectDAO.getVersionForEvaluation(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getStandardsStatistics() {
        return baseObjectDAO.getStandardsStatistics();
    }

    // ================== Validation Methods ==================

    @Override
    public void validateStandard(ComplianceStandard standard) {
        if (standard == null) {
            throw new IllegalArgumentException("Compliance standard cannot be null");
        }

        if (standard.getName() == null || standard.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Standard name is required");
        }

        if (standard.getVersion() == null || standard.getVersion().trim().isEmpty()) {
            throw new IllegalArgumentException("Version is required");
        }

        if (standard.getIssuingBody() == null || standard.getIssuingBody().trim().isEmpty()) {
            throw new IllegalArgumentException("Issuing body is required");
        }

        if (standard.getRegulationNumber() == null || standard.getRegulationNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Regulation number is required");
        }

        if (standard.getCountryRegion() == null || standard.getCountryRegion().trim().isEmpty()) {
            throw new IllegalArgumentException("Country/region is required");
        }

        if (standard.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }

        // Check for duplicates
        if (duplicateStandardExists(standard)) {
            throw new LIMSDuplicateRecordException(
                    "A compliance standard with this issuing body, regulation number, and version already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelete(String standardId) {
        return !standardHasEvaluations(standardId) && !standardHasParameterGroups(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canModify(String standardId) {
        // Standards with linked evaluations should be versioned rather than modified
        return !standardHasEvaluations(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getByRegulationNumberAndName(String regulationNumber, String name) {
        return getBaseObjectDAO().getByRegulationNumberAndName(regulationNumber, name);
    }
}