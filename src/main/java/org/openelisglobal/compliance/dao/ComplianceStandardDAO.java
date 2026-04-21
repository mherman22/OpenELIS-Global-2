package org.openelisglobal.compliance.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandardStatus;

/**
 * DAO interface for ComplianceStandard entity operations.
 *
 * Follows OpenELIS DAO patterns with proper exception handling and
 * constitutional compliance for transaction management.
 */
public interface ComplianceStandardDAO extends BaseDAO<ComplianceStandard, String> {

    /**
     * Retrieve all compliance standards
     */
    List<ComplianceStandard> getAllStandards() throws LIMSRuntimeException;

    /**
     * Retrieve compliance standard by FHIR UUID
     */
    ComplianceStandard getStandardByFhirId(UUID fhirUuid) throws LIMSRuntimeException;

    /**
     * Retrieve compliance standard by natural key (issuing body, regulation number, version)
     */
    ComplianceStandard getStandardByNaturalKey(String issuingBody, String regulationNumber, String version)
            throws LIMSRuntimeException;

    /**
     * Retrieve standards by status
     */
    List<ComplianceStandard> getStandardsByStatus(ComplianceStandardStatus status) throws LIMSRuntimeException;

    /**
     * Retrieve active standards by sample type
     */
    List<ComplianceStandard> getActiveStandardsBySampleType(String sampleType) throws LIMSRuntimeException;

    /**
     * Retrieve standards by country/region
     */
    List<ComplianceStandard> getStandardsByCountryRegion(String countryRegion) throws LIMSRuntimeException;

    /**
     * Retrieve standards by issuing body
     */
    List<ComplianceStandard> getStandardsByIssuingBody(String issuingBody) throws LIMSRuntimeException;

    /**
     * Retrieve standards effective within date range
     */
    List<ComplianceStandard> getStandardsEffectiveInRange(LocalDate startDate, LocalDate endDate)
            throws LIMSRuntimeException;

    /**
     * Retrieve standard with eagerly loaded parameter groups
     * (Constitutional requirement: services compile data within transaction)
     */
    ComplianceStandard getStandardWithParameterGroups(String standardId) throws LIMSRuntimeException;

    /**
     * Retrieve standard with full hierarchy (groups and thresholds)
     */
    ComplianceStandard getStandardWithFullHierarchy(String standardId) throws LIMSRuntimeException;

    /**
     * Retrieve standards with their parameter group counts
     */
    List<ComplianceStandard> getStandardsWithGroupCounts() throws LIMSRuntimeException;

    /**
     * Get paginated standards list
     */
    List<ComplianceStandard> getPageOfStandards(int startingRecNo) throws LIMSRuntimeException;

    /**
     * Get paginated search results for standards
     */
    List<ComplianceStandard> getPagesOfSearchedStandards(int startingRecNo, String searchString)
            throws LIMSRuntimeException;

    /**
     * Get total count of standards
     */
    Integer getTotalStandardCount() throws LIMSRuntimeException;

    /**
     * Get total count of searched standards
     */
    Integer getTotalSearchedStandardCount(String searchString) throws LIMSRuntimeException;

    /**
     * Check if a standard with the same natural key already exists
     */
    boolean duplicateStandardExists(ComplianceStandard standard) throws LIMSRuntimeException;

    /**
     * Check if a standard has linked evaluations (prevents deletion)
     */
    boolean standardHasEvaluations(String standardId) throws LIMSRuntimeException;

    /**
     * Check if a standard has linked parameter groups (prevents deletion)
     */
    boolean standardHasParameterGroups(String standardId) throws LIMSRuntimeException;

    /**
     * Get standards that are superseded by the given standard
     */
    List<ComplianceStandard> getSupersededStandards(String supersededByStandardId) throws LIMSRuntimeException;

    /**
     * Get version history for a regulation (all versions of same regulation)
     */
    List<ComplianceStandard> getVersionHistory(String issuingBody, String regulationNumber)
            throws LIMSRuntimeException;

    /**
     * Search standards by multiple criteria
     */
    List<ComplianceStandard> searchStandards(String name, String issuingBody, String regulationNumber,
                                           ComplianceStandardStatus status, String countryRegion,
                                           String sampleType) throws LIMSRuntimeException;

    /**
     * Get standards that will expire within the specified number of days
     */
    List<ComplianceStandard> getStandardsExpiringWithinDays(int days) throws LIMSRuntimeException;

    /**
     * Get standards for CSV export with minimal data
     */
    List<ComplianceStandard> getStandardsForExport() throws LIMSRuntimeException;

    /**
     * Get the latest version of standards for each regulation
     */
    List<ComplianceStandard> getLatestVersionStandards() throws LIMSRuntimeException;

    /**
     * Get standards that can be superseded (active standards)
     */
    List<ComplianceStandard> getSupersedableStandards() throws LIMSRuntimeException;

    /**
     * Bulk update status for multiple standards
     */
    void bulkUpdateStatus(List<String> standardIds, ComplianceStandardStatus newStatus, String userId)
            throws LIMSRuntimeException;

    /**
     * Get compliance standards summary statistics
     */
    List<Object[]> getStandardsStatistics() throws LIMSRuntimeException;

    /**
     * Get compliance standard by regulation number and name
     */
    ComplianceStandard getByRegulationNumberAndName(String regulationNumber, String name) throws LIMSRuntimeException;
}