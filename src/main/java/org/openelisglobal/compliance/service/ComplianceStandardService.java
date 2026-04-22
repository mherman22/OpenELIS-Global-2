package org.openelisglobal.compliance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandardStatus;

/**
 * Service interface for ComplianceStandard operations.
 *
 * Follows OpenELIS service patterns extending BaseObjectService for standard
 * CRUD operations and providing domain-specific business logic for compliance
 * standards management.
 *
 * Constitutional compliance: - Extends BaseObjectService for standardized
 * operations - Declares transaction boundaries at service level - Provides
 * business logic validation methods - Supports FHIR integration requirements
 */
public interface ComplianceStandardService extends BaseObjectService<ComplianceStandard, String> {

    /**
     * Get compliance standard by FHIR UUID
     */
    ComplianceStandard getStandardByFhirId(UUID fhirUuid);

    /**
     * Get compliance standard by natural key (issuing body, regulation number,
     * version)
     */
    ComplianceStandard getStandardByNaturalKey(String issuingBody, String regulationNumber, String version);

    /**
     * Get standards by status
     */
    List<ComplianceStandard> getStandardsByStatus(ComplianceStandardStatus status);

    /**
     * Get active standards applicable to a specific sample type
     */
    List<ComplianceStandard> getActiveStandardsBySampleType(String sampleType);

    /**
     * Get standards by country/region
     */
    List<ComplianceStandard> getStandardsByCountryRegion(String countryRegion);

    /**
     * Get standards by issuing body
     */
    List<ComplianceStandard> getStandardsByIssuingBody(String issuingBody);

    /**
     * Get standards effective within a date range
     */
    List<ComplianceStandard> getStandardsEffectiveInRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get standard with eagerly loaded parameter groups (Constitutional
     * requirement: compile data within transaction)
     */
    ComplianceStandard getStandardWithParameterGroups(String standardId);

    /**
     * Get standard with full hierarchy (groups and thresholds) eagerly loaded
     */
    ComplianceStandard getStandardWithFullHierarchy(String standardId);

    /**
     * Get standards with their parameter group counts
     */
    List<ComplianceStandard> getStandardsWithGroupCounts();

    /**
     * Get paginated list of standards
     */
    List<ComplianceStandard> getPageOfStandards(int startingRecNo);

    /**
     * Get paginated search results
     */
    List<ComplianceStandard> getPagesOfSearchedStandards(int startingRecNo, String searchString);

    /**
     * Get total count of standards
     */
    Integer getTotalStandardCount();

    /**
     * Get total count of searched standards
     */
    Integer getTotalSearchedStandardCount(String searchString);

    /**
     * Check if a duplicate standard exists (business rule validation)
     */
    boolean duplicateStandardExists(ComplianceStandard standard);

    /**
     * Check if standard has linked evaluations (prevents deletion)
     */
    boolean standardHasEvaluations(String standardId);

    /**
     * Check if standard has parameter groups (prevents deletion)
     */
    boolean standardHasParameterGroups(String standardId);

    /**
     * Get standards that are superseded by the given standard
     */
    List<ComplianceStandard> getSupersededStandards(String supersededByStandardId);

    /**
     * Get version history for a regulation
     */
    List<ComplianceStandard> getVersionHistory(String issuingBody, String regulationNumber);

    /**
     * Get compliance standard by regulation number and name
     */
    ComplianceStandard getByRegulationNumberAndName(String regulationNumber, String name);

    /**
     * Search standards by multiple criteria
     */
    List<ComplianceStandard> searchStandards(String name, String issuingBody, String regulationNumber,
            ComplianceStandardStatus status, String countryRegion, String sampleType);

    /**
     * Get standards expiring within specified days
     */
    List<ComplianceStandard> getStandardsExpiringWithinDays(int days);

    /**
     * Get standards for export (minimal data)
     */
    List<ComplianceStandard> getStandardsForExport();

    /**
     * Get standard with eagerly loaded parameter groups (Constitutional requirement: compile data within transaction)
     */
    ComplianceStandard getStandardWithGroups(String standardId);

    /**
     * Supersede a standard by setting its status and linking to replacement
     */
    void supersedseStandard(String oldStandardId, String newStandardId);

    /**
     * Archive a standard (set status to ARCHIVED)
     */
    void archive(String standardId);

    /**
     * Get version for evaluation (constitutional requirement: version-lock semantics)
     */
    String getVersionForEvaluation(String standardId);

    /**
     * Get the latest version of each regulation
     */
    List<ComplianceStandard> getLatestVersionStandards();

    /**
     * Get standards that can be superseded (active standards)
     */
    List<ComplianceStandard> getSupersedableStandards();

    /**
     * Bulk update status for multiple standards
     */
    void bulkUpdateStatus(List<String> standardIds, ComplianceStandardStatus newStatus, String userId);

    /**
     * Get statistics about standards
     */
    List<Object[]> getStandardsStatistics();

    /**
     * Validate standard before save (business rules)
     */
    void validateStandard(ComplianceStandard standard);

    /**
     * Check if standard can be deleted (business rules)
     */
    boolean canDelete(String standardId);

    /**
     * Check if standard can be modified (business rules)
     */
    boolean canModify(String standardId);

    // FHIR R4 integration methods

    /**
     * Get compliance standard by FHIR UUID string (Convenience method for FHIR
     * providers expecting string ID)
     */
    ComplianceStandard getComplianceStandardByFhirId(String fhirIdString);

    /**
     * Get compliance standards by name (supports partial matching)
     */
    List<ComplianceStandard> getComplianceStandardsByName(String name);

    /**
     * Get compliance standard by regulation number
     */
    ComplianceStandard getComplianceStandardByRegulationNumber(String regulationNumber);

    /**
     * Get compliance standards by issuing body
     */
    List<ComplianceStandard> getComplianceStandardsByIssuingBody(String issuingBody);

    /**
     * Get all active compliance standards
     */
    List<ComplianceStandard> getActiveComplianceStandards();
}