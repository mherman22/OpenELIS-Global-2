package org.openelisglobal.inventory.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for creating inventory items (equipment, reagents,
 * cartridges) from CSV files.
 * <p>
 * This handler processes CSV files to create inventory item entries, which can
 * then have lots associated with them.
 * <p>
 * CSV Format: name,description,item_type,category,manufacturer,catalog_number,
 * model_number,serial_number,ahri_tag,project_name,storage_requirements,units,
 * calibration_required,compatible_analyzers,equipment_condition,installation_date,
 * last_service_date,last_maintenance_date,current_location,active
 *
 * Example: Applied Biosystems QuantStudio 3,Real-Time PCR System for
 * quantitative PCR applications,CARTRIDGE,PCR Equipment,Applied
 * Biosystems,A28567, QuantStudio-3,QS3-2024-001,AHRI-PCR-001,Central
 * Laboratory,Room Temperature, units,Y,96-well
 * plates,functional,2023-06-15,2024-11-20,2024-12-01, PCR Laboratory - Room
 * 201,Y
 */
@Component
@Transactional
public class InventoryItemConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private InventoryItemDAO inventoryItemDAO;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String getDomainName() {
        return "inventory-items";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 300; // Load after storage locations (100-104) but before higher-level configs
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (GenericValidator.isBlankOrNull(headerLine)) {
            throw new IllegalArgumentException("Inventory item configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);

        int nameIndex = findColumnIndex(headers, "name");
        int descriptionIndex = findColumnIndex(headers, "description");
        int itemTypeIndex = findColumnIndex(headers, "item_type");
        int categoryIndex = findColumnIndex(headers, "category");
        int manufacturerIndex = findColumnIndex(headers, "manufacturer");
        int catalogNumberIndex = findColumnIndex(headers, "catalog_number");
        int modelNumberIndex = findColumnIndex(headers, "model_number");
        int serialNumberIndex = findColumnIndex(headers, "serial_number");
        int ahriTagIndex = findColumnIndex(headers, "ahri_tag");
        int projectNameIndex = findColumnIndex(headers, "project_name");
        int storageRequirementsIndex = findColumnIndex(headers, "storage_requirements");
        int unitsIndex = findColumnIndex(headers, "units");
        int stabilityAfterOpeningIndex = findColumnIndex(headers, "stability_after_opening");
        int concentrationIndex = findColumnIndex(headers, "concentration");
        int calibrationRequiredIndex = findColumnIndex(headers, "calibration_required");
        int compatibleAnalyzersIndex = findColumnIndex(headers, "compatible_analyzers");
        int equipmentConditionIndex = findColumnIndex(headers, "equipment_condition");
        int installationDateIndex = findColumnIndex(headers, "installation_date");
        int lastServiceDateIndex = findColumnIndex(headers, "last_service_date");
        int lastMaintenanceDateIndex = findColumnIndex(headers, "last_maintenance_date");
        int currentLocationIndex = findColumnIndex(headers, "current_location");
        int activeIndex = findColumnIndex(headers, "active");

        if (nameIndex == -1) {
            throw new IllegalArgumentException(
                    "Inventory item configuration file " + fileName + " must have a 'name' column");
        }
        if (itemTypeIndex == -1) {
            throw new IllegalArgumentException(
                    "Inventory item configuration file " + fileName + " must have an 'item_type' column");
        }

        String line;
        int lineNumber = 1;
        int processedCount = 0;
        int skippedCount = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.trim();

            // Skip empty lines and comments
            if (GenericValidator.isBlankOrNull(line) || line.startsWith("#")) {
                continue;
            }

            try {
                boolean processed = processInventoryItemLine(line, nameIndex, descriptionIndex, itemTypeIndex,
                        categoryIndex, manufacturerIndex, catalogNumberIndex, modelNumberIndex, serialNumberIndex,
                        ahriTagIndex, projectNameIndex, storageRequirementsIndex, unitsIndex,
                        stabilityAfterOpeningIndex, concentrationIndex, calibrationRequiredIndex,
                        compatibleAnalyzersIndex, equipmentConditionIndex, installationDateIndex, lastServiceDateIndex,
                        lastMaintenanceDateIndex, currentLocationIndex, activeIndex, fileName, lineNumber);
                if (processed) {
                    processedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Inventory item configuration processing completed for " + fileName + ". Processed: " + processedCount
                        + ", Skipped: " + skippedCount);
    }

    private boolean processInventoryItemLine(String line, int nameIndex, int descriptionIndex, int itemTypeIndex,
            int categoryIndex, int manufacturerIndex, int catalogNumberIndex, int modelNumberIndex,
            int serialNumberIndex, int ahriTagIndex, int projectNameIndex, int storageRequirementsIndex, int unitsIndex,
            int stabilityAfterOpeningIndex, int concentrationIndex, int calibrationRequiredIndex,
            int compatibleAnalyzersIndex, int equipmentConditionIndex, int installationDateIndex,
            int lastServiceDateIndex, int lastMaintenanceDateIndex, int currentLocationIndex, int activeIndex,
            String fileName, int lineNumber) {

        String[] values = parseCsvLine(line);

        String name = getValueOrEmpty(values, nameIndex);
        String description = getValueOrEmpty(values, descriptionIndex);
        String itemTypeStr = getValueOrEmpty(values, itemTypeIndex);
        String category = getValueOrEmpty(values, categoryIndex);
        String manufacturer = getValueOrEmpty(values, manufacturerIndex);
        String catalogNumber = getValueOrEmpty(values, catalogNumberIndex);
        String modelNumber = getValueOrEmpty(values, modelNumberIndex);
        String serialNumber = getValueOrEmpty(values, serialNumberIndex);
        String ahriTag = getValueOrEmpty(values, ahriTagIndex);
        String projectName = getValueOrEmpty(values, projectNameIndex);
        String storageRequirements = getValueOrEmpty(values, storageRequirementsIndex);
        String units = getValueOrEmpty(values, unitsIndex);
        String stabilityAfterOpeningStr = getValueOrEmpty(values, stabilityAfterOpeningIndex);
        String concentration = getValueOrEmpty(values, concentrationIndex);
        String calibrationRequired = getValueOrEmpty(values, calibrationRequiredIndex);
        String compatibleAnalyzers = getValueOrEmpty(values, compatibleAnalyzersIndex);
        String equipmentCondition = getValueOrEmpty(values, equipmentConditionIndex);
        String installationDate = getValueOrEmpty(values, installationDateIndex);
        String lastServiceDate = getValueOrEmpty(values, lastServiceDateIndex);
        String lastMaintenanceDate = getValueOrEmpty(values, lastMaintenanceDateIndex);
        String currentLocation = getValueOrEmpty(values, currentLocationIndex);
        String activeStr = getValueOrEmpty(values, activeIndex);

        // Validate required fields
        if (GenericValidator.isBlankOrNull(name)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryItemLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing item name");
            return false;
        }

        if (GenericValidator.isBlankOrNull(itemTypeStr)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryItemLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing item type");
            return false;
        }

        // Parse item type
        ItemType itemType;
        try {
            itemType = ItemType.valueOf(itemTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryItemLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": invalid item type '" + itemTypeStr + "'");
            return false;
        }

        // Default units if not provided
        if (GenericValidator.isBlankOrNull(units)) {
            units = "units";
        }

        // Parse active flag (default to true)
        Boolean active = true;
        if (!GenericValidator.isBlankOrNull(activeStr)) {
            active = Boolean.parseBoolean(activeStr.toLowerCase());
        }

        // Check if item with this name already exists
        List<InventoryItem> existingItems = inventoryItemDAO.getAll();
        for (InventoryItem existing : existingItems) {
            if (existing.getName().equalsIgnoreCase(name)) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "processInventoryItemLine",
                        "Inventory item with name '" + name + "' already exists. Skipping line " + lineNumber);
                return false;
            }
        }

        // Create new inventory item
        try {
            InventoryItem item = new InventoryItem();
            item.setFhirUuid(UUID.randomUUID());
            item.setName(name);
            item.setDescription(description);
            item.setItemType(itemType);
            item.setCategory(category);
            item.setManufacturer(manufacturer);
            item.setCatalogNumber(catalogNumber);
            item.setStorageRequirements(storageRequirements);
            item.setUnits(units);
            item.setIsActive("Y".equalsIgnoreCase(activeStr) ? "Y" : "N");
            item.setSysUserId("1"); // System user
            item.setProjectName(projectName); // Set project name for all types

            // Reagent-specific fields (REAGENT type)
            if (itemType == ItemType.REAGENT) {
                if (!GenericValidator.isBlankOrNull(concentration)) {
                    item.setConcentration(concentration);
                }
                if (!GenericValidator.isBlankOrNull(stabilityAfterOpeningStr)) {
                    try {
                        item.setStabilityAfterOpening(Integer.parseInt(stabilityAfterOpeningStr));
                    } catch (NumberFormatException e) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryItemLine",
                                "Invalid stability value '" + stabilityAfterOpeningStr + "'. Using default 30 days");
                        item.setStabilityAfterOpening(30);
                    }
                }
            }

            // Equipment-specific fields (CARTRIDGE type)
            if (itemType == ItemType.CARTRIDGE) {
                item.setCompatibleAnalyzers(compatibleAnalyzers);
                item.setModelNumber(modelNumber);
                item.setSerialNumber(serialNumber);
                item.setAhriTag(ahriTag);
                item.setProjectName(projectName);
                item.setCalibrationRequired("Y".equalsIgnoreCase(calibrationRequired) ? "Y" : "N");
                item.setEquipmentCondition(equipmentCondition);
                item.setCurrentLocation(currentLocation);

                // Parse dates if provided
                if (!GenericValidator.isBlankOrNull(installationDate)) {
                    item.setInstallationDate(parseTimestamp(installationDate));
                }
                if (!GenericValidator.isBlankOrNull(lastServiceDate)) {
                    item.setLastServiceDate(parseTimestamp(lastServiceDate));
                }
                if (!GenericValidator.isBlankOrNull(lastMaintenanceDate)) {
                    item.setLastMaintenanceDate(parseTimestamp(lastMaintenanceDate));
                }
            }

            inventoryItemDAO.insert(item);

            LogEvent.logInfo(this.getClass().getSimpleName(), "processInventoryItemLine",
                    "Successfully created inventory item '" + name + "' of type '" + itemType + "'");
            return true;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processInventoryItemLine",
                    "Failed to create inventory item '" + name + "': " + e.getMessage());
            return false;
        }
    }

    private LocalDateTime parseTimestamp(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr + "T00:00:00");
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "parseTimestamp",
                    "Failed to parse date: " + dateStr + ". Using current timestamp.");
            return LocalDateTime.now();
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());
        return values.toArray(new String[0]);
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }
}
