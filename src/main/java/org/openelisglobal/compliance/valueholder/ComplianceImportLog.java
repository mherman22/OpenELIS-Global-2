package org.openelisglobal.compliance.valueholder;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.annotations.Type;
import org.openelisglobal.common.util.ValidationHelper;
import org.openelisglobal.common.util.validator.SafeHtml;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;
import org.openelisglobal.spring.util.SpringContext;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ComplianceImportLog entity for tracking compliance standard import operations.
 *
 * Provides audit trail functionality for CSV imports, manual imports, and other
 * data import operations related to compliance standards.
 *
 * Constitutional compliance:
 * - Extends BaseObject for audit trail support
 * - Includes FHIR UUID for interoperability
 * - Uses JPA annotations (no XML mappings)
 * - Implements validation annotations
 * - FHIR R4 integration hooks via @PostPersist/@PostUpdate
 */
@Entity
@Table(name = "compliance_import_log")
public class ComplianceImportLog extends BaseObject<String> implements SimpleBaseEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "compliance_import_log_generator", sequenceName = "compliance_import_log_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compliance_import_log_generator")
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    @Column(name = "id")
    private String id;

    @NotNull
    @Type(type = "uuid-char")
    @Column(name = "fhir_uuid", unique = true, nullable = false)
    private UUID fhirUuid;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", nullable = false)
    private ComplianceImportType importType;

    @NotBlank(message = "Import source is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "import_source", nullable = false)
    private String importSource; // File path, URL, or source identifier

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", nullable = false)
    private ComplianceImportStatus importStatus;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "import_start_time", nullable = false)
    private LocalDateTime importStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "import_end_time")
    private LocalDateTime importEndTime;

    @NotNull
    @Column(name = "records_processed", nullable = false)
    private Integer recordsProcessed = 0;

    @NotNull
    @Column(name = "records_successful", nullable = false)
    private Integer recordsSuccessful = 0;

    @NotNull
    @Column(name = "records_failed", nullable = false)
    private Integer recordsFailed = 0;

    @NotNull
    @Column(name = "records_skipped", nullable = false)
    private Integer recordsSkipped = 0;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "import_summary", columnDefinition = "TEXT")
    private String importSummary;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize; // Size in bytes

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "file_checksum")
    private String fileChecksum; // SHA-256 checksum for file integrity

    @NotBlank(message = "Initiated by user ID is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy; // User ID who started the import

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "correlation_id")
    private String correlationId; // For tracking related operations

    public ComplianceImportLog() {
        super();
        generateFhirUuid();
        this.importStartTime = LocalDateTime.now();
        this.importStatus = ComplianceImportStatus.STARTED;
    }

    public ComplianceImportLog(ComplianceImportType importType, String importSource, String initiatedBy) {
        this();
        this.importType = importType;
        this.importSource = importSource;
        this.initiatedBy = initiatedBy;
    }

    private void generateFhirUuid() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Getters and Setters

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public ComplianceImportType getImportType() {
        return importType;
    }

    public void setImportType(ComplianceImportType importType) {
        this.importType = importType;
    }

    public String getImportSource() {
        return importSource;
    }

    public void setImportSource(String importSource) {
        this.importSource = importSource;
    }

    public ComplianceImportStatus getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(ComplianceImportStatus importStatus) {
        this.importStatus = importStatus;
    }

    public LocalDateTime getImportStartTime() {
        return importStartTime;
    }

    public void setImportStartTime(LocalDateTime importStartTime) {
        this.importStartTime = importStartTime;
    }

    public LocalDateTime getImportEndTime() {
        return importEndTime;
    }

    public void setImportEndTime(LocalDateTime importEndTime) {
        this.importEndTime = importEndTime;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public Integer getRecordsSuccessful() {
        return recordsSuccessful;
    }

    public void setRecordsSuccessful(Integer recordsSuccessful) {
        this.recordsSuccessful = recordsSuccessful;
    }

    public Integer getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(Integer recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public Integer getRecordsSkipped() {
        return recordsSkipped;
    }

    public void setRecordsSkipped(Integer recordsSkipped) {
        this.recordsSkipped = recordsSkipped;
    }

    public String getImportSummary() {
        return importSummary;
    }

    public void setImportSummary(String importSummary) {
        this.importSummary = importSummary;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    // Helper methods

    /**
     * Mark the import as completed successfully
     */
    public void markCompleted() {
        this.importStatus = ComplianceImportStatus.COMPLETED;
        this.importEndTime = LocalDateTime.now();
    }

    /**
     * Mark the import as failed with error details
     */
    public void markFailed(String errorDetails) {
        this.importStatus = ComplianceImportStatus.FAILED;
        this.importEndTime = LocalDateTime.now();
        this.errorDetails = errorDetails;
    }

    /**
     * Mark the import as completed with warnings
     */
    public void markCompletedWithWarnings(String warnings) {
        this.importStatus = ComplianceImportStatus.COMPLETED_WITH_WARNINGS;
        this.importEndTime = LocalDateTime.now();
        this.errorDetails = warnings;
    }

    /**
     * Calculate import duration in seconds
     */
    public Long getDurationSeconds() {
        if (importStartTime == null) {
            return null;
        }

        LocalDateTime endTime = importEndTime != null ? importEndTime : LocalDateTime.now();
        return java.time.Duration.between(importStartTime, endTime).getSeconds();
    }

    /**
     * Get success rate as percentage
     */
    public Double getSuccessRate() {
        if (recordsProcessed == null || recordsProcessed == 0) {
            return 0.0;
        }
        return (recordsSuccessful.doubleValue() / recordsProcessed.doubleValue()) * 100.0;
    }

    /**
     * Check if import is still in progress
     */
    public boolean isInProgress() {
        return ComplianceImportStatus.STARTED.equals(importStatus) ||
               ComplianceImportStatus.IN_PROGRESS.equals(importStatus);
    }

    /**
     * Check if import completed successfully (with or without warnings)
     */
    public boolean isCompleted() {
        return ComplianceImportStatus.COMPLETED.equals(importStatus) ||
               ComplianceImportStatus.COMPLETED_WITH_WARNINGS.equals(importStatus);
    }

    /**
     * Check if import failed
     */
    public boolean isFailed() {
        return ComplianceImportStatus.FAILED.equals(importStatus);
    }

    @Override
    public String toString() {
        return "ComplianceImportLog{" +
                "id='" + id + '\'' +
                ", importType=" + importType +
                ", importSource='" + importSource + '\'' +
                ", importStatus=" + importStatus +
                ", recordsProcessed=" + recordsProcessed +
                ", recordsSuccessful=" + recordsSuccessful +
                ", recordsFailed=" + recordsFailed +
                '}';
    }

    /**
     * FHIR R4 integration - automatically sync to FHIR server on entity creation
     * Following established OpenELIS patterns for async FHIR persistence
     */
    @PostPersist
    public void onPostPersist() {
        try {
            org.openelisglobal.compliance.fhir.ComplianceFhirTransform fhirTransform =
                SpringContext.getBean(org.openelisglobal.compliance.fhir.ComplianceFhirTransform.class);
            if (fhirTransform != null) {
                fhirTransform.syncComplianceImportLogToFhir(this, true);
            }
        } catch (Exception e) {
            // Log error but don't fail transaction
            org.openelisglobal.common.log.LogEvent.logError(
                "ComplianceImportLog", "onPostPersist",
                "Failed to sync to FHIR on create: " + e.getMessage());
        }
    }

    /**
     * FHIR R4 integration - automatically sync to FHIR server on entity update
     */
    @PostUpdate
    public void onPostUpdate() {
        try {
            org.openelisglobal.compliance.fhir.ComplianceFhirTransform fhirTransform =
                SpringContext.getBean(org.openelisglobal.compliance.fhir.ComplianceFhirTransform.class);
            if (fhirTransform != null) {
                fhirTransform.syncComplianceImportLogToFhir(this, false);
            }
        } catch (Exception e) {
            // Log error but don't fail transaction
            org.openelisglobal.common.log.LogEvent.logError(
                "ComplianceImportLog", "onPostUpdate",
                "Failed to sync to FHIR on update: " + e.getMessage());
        }
    }

    /**
     * Get FHIR UUID as string for FHIR resource ID
     */
    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
    }
}