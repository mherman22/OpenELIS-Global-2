package org.openelisglobal.compliance.valueholder;

/**
 * Enumeration of compliance standard import types.
 *
 * Defines the various methods and sources for importing compliance standards
 * into the OpenELIS system, providing categorization for audit trail purposes.
 */
public enum ComplianceImportType {

    /**
     * CSV file import - standards imported from CSV files
     */
    CSV_FILE("CSV File Import"),

    /**
     * Manual entry - standards entered manually through the UI
     */
    MANUAL_ENTRY("Manual Entry"),

    /**
     * Liquibase migration - standards loaded via database migrations
     */
    LIQUIBASE_MIGRATION("Liquibase Migration"),

    /**
     * API import - standards imported via REST API
     */
    API_IMPORT("API Import"),

    /**
     * Bulk upload - multiple standards uploaded at once
     */
    BULK_UPLOAD("Bulk Upload"),

    /**
     * System initialization - standards loaded during system setup
     */
    SYSTEM_INITIALIZATION("System Initialization"),

    /**
     * External system sync - standards synchronized from external systems
     */
    EXTERNAL_SYNC("External System Sync"),

    /**
     * Data migration - standards migrated from legacy systems
     */
    DATA_MIGRATION("Data Migration");

    private final String displayName;

    ComplianceImportType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get the display name for UI purposes
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get enum value from display name
     */
    public static ComplianceImportType fromDisplayName(String displayName) {
        for (ComplianceImportType type : values()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}