package org.openelisglobal.inventory.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for creating inventory lots from CSV files.
 * <p>
 * This handler processes CSV files to create inventory lot entries, which
 * represent actual stock with quantities, expiry dates, and lot numbers.
 * <p>
 * CSV Format: item_name,category,lot_number,initial_quantity,unit_size,
 * expiration_date,qc_status,status
 *
 * Example: MgCl2 10xPCR rxn buffer,Sequencing
 * primers,Ref-Y02028,1.0,300ul,2026-12-31,PENDING,ACTIVE
 *
 * Note: item_name + category must match an existing InventoryItem (loaded by
 * InventoryItemConfigurationHandler first)
 */
@Component
@Transactional
public class InventoryLotConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    @Autowired
    private InventoryItemService inventoryItemService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String getDomainName() {
        return "inventory-lots";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 301; // Load after inventory-items (300)
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (GenericValidator.isBlankOrNull(headerLine)) {
            throw new IllegalArgumentException("Inventory lot configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);

        int itemNameIndex = findColumnIndex(headers, "item_name");
        int categoryIndex = findColumnIndex(headers, "category");
        int lotNumberIndex = findColumnIndex(headers, "lot_number");
        int initialQuantityIndex = findColumnIndex(headers, "initial_quantity");
        int unitSizeIndex = findColumnIndex(headers, "unit_size");
        int expirationDateIndex = findColumnIndex(headers, "expiration_date");
        int qcStatusIndex = findColumnIndex(headers, "qc_status");
        int statusIndex = findColumnIndex(headers, "status");

        if (itemNameIndex == -1) {
            throw new IllegalArgumentException(
                    "Inventory lot configuration file " + fileName + " must have an 'item_name' column");
        }
        if (lotNumberIndex == -1) {
            throw new IllegalArgumentException(
                    "Inventory lot configuration file " + fileName + " must have a 'lot_number' column");
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
                boolean processed = processInventoryLotLine(line, itemNameIndex, categoryIndex, lotNumberIndex,
                        initialQuantityIndex, unitSizeIndex, expirationDateIndex, qcStatusIndex, statusIndex, fileName,
                        lineNumber);
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
                "Inventory lot configuration processing completed for " + fileName + ". Processed: " + processedCount
                        + ", Skipped: " + skippedCount);
    }

    private boolean processInventoryLotLine(String line, int itemNameIndex, int categoryIndex, int lotNumberIndex,
            int initialQuantityIndex, int unitSizeIndex, int expirationDateIndex, int qcStatusIndex, int statusIndex,
            String fileName, int lineNumber) {

        String[] values = parseCsvLine(line);

        String itemName = getValueOrEmpty(values, itemNameIndex);
        String category = getValueOrEmpty(values, categoryIndex);
        String lotNumber = getValueOrEmpty(values, lotNumberIndex);
        String initialQuantityStr = getValueOrEmpty(values, initialQuantityIndex);
        String unitSize = getValueOrEmpty(values, unitSizeIndex);
        String expirationDate = getValueOrEmpty(values, expirationDateIndex);
        String qcStatusStr = getValueOrEmpty(values, qcStatusIndex);
        String statusStr = getValueOrEmpty(values, statusIndex);

        // Validate required fields
        if (GenericValidator.isBlankOrNull(itemName)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryLotLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing item name");
            return false;
        }

        if (GenericValidator.isBlankOrNull(lotNumber)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryLotLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing lot number");
            return false;
        }

        // Find the inventory item by name (and optionally category)
        InventoryItem inventoryItem = findInventoryItem(itemName, category);
        if (inventoryItem == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryLotLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": inventory item '" + itemName
                            + "' not found. Ensure inventory-items are loaded first.");
            return false;
        }

        // Check if lot with this number already exists for this item
        List<InventoryLot> existingLots = inventoryLotDAO.getAll();
        for (InventoryLot existing : existingLots) {
            if (existing.getInventoryItem().getId().equals(inventoryItem.getId())
                    && existing.getLotNumber().equalsIgnoreCase(lotNumber)) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "processInventoryLotLine", "Lot with number '"
                        + lotNumber + "' for item '" + itemName + "' already exists. Skipping line " + lineNumber);
                return false;
            }
        }

        // Parse quantity (default to 1.0)
        Double initialQuantity = 1.0;
        if (!GenericValidator.isBlankOrNull(initialQuantityStr)) {
            try {
                initialQuantity = Double.parseDouble(initialQuantityStr);
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryLotLine",
                        "Invalid quantity '" + initialQuantityStr + "' on line " + lineNumber + ". Using 1.0");
                initialQuantity = 1.0;
            }
        }

        // Default unit size
        if (GenericValidator.isBlankOrNull(unitSize)) {
            unitSize = "1 unit";
        }

        // Parse QC status (default to PENDING)
        QCStatus qcStatus = QCStatus.PENDING;
        if (!GenericValidator.isBlankOrNull(qcStatusStr)) {
            try {
                qcStatus = QCStatus.valueOf(qcStatusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryLotLine",
                        "Invalid QC status '" + qcStatusStr + "' on line " + lineNumber + ". Using PENDING");
            }
        }

        // Parse lot status (default to ACTIVE)
        LotStatus lotStatus = LotStatus.ACTIVE;
        if (!GenericValidator.isBlankOrNull(statusStr)) {
            try {
                lotStatus = LotStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processInventoryLotLine",
                        "Invalid status '" + statusStr + "' on line " + lineNumber + ". Using ACTIVE");
            }
        }

        // Create new inventory lot
        try {
            InventoryLot lot = new InventoryLot();
            lot.setFhirUuid(UUID.randomUUID());
            lot.setInventoryItem(inventoryItem);
            lot.setLotNumber(lotNumber);
            lot.setInitialQuantity(initialQuantity);
            lot.setCurrentQuantity(initialQuantity); // Start with full quantity
            lot.setUnitSize(unitSize);
            lot.setQcStatus(qcStatus);
            lot.setStatus(lotStatus);
            lot.setSysUserId("1"); // System user

            // Parse expiration date if provided
            if (!GenericValidator.isBlankOrNull(expirationDate)) {
                lot.setExpirationDate(parseTimestamp(expirationDate));
            }

            inventoryLotDAO.insert(lot);

            LogEvent.logInfo(this.getClass().getSimpleName(), "processInventoryLotLine",
                    "Successfully created lot '" + lotNumber + "' for item '" + itemName + "'");
            return true;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processInventoryLotLine",
                    "Failed to create lot '" + lotNumber + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Find inventory item by name and optionally category. Prefers exact match with
     * category, falls back to name-only match.
     */
    private InventoryItem findInventoryItem(String itemName, String category) {
        List<InventoryItem> allItems = inventoryItemService.getAll();

        // First try: exact match on name AND category
        if (!GenericValidator.isBlankOrNull(category)) {
            for (InventoryItem item : allItems) {
                if (item.getName().equalsIgnoreCase(itemName)
                        && (item.getCategory() != null && item.getCategory().equalsIgnoreCase(category))) {
                    return item;
                }
            }
        }

        // Second try: match on name only
        for (InventoryItem item : allItems) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }

        return null;
    }

    private Timestamp parseTimestamp(String dateStr) {
        try {
            LocalDate localDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            return Timestamp.valueOf(localDate.atStartOfDay());
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "parseTimestamp",
                    "Failed to parse date: " + dateStr + ". Using default expiry (1 year from now)");
            return Timestamp.valueOf(LocalDate.now().plusYears(1).atStartOfDay());
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
