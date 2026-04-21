package org.openelisglobal.compliance.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.dao.ComplianceThresholdDAO;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ComplianceThresholdService following OpenELIS patterns.
 *
 * Constitutional compliance:
 * - Extends AuditableBaseObjectServiceImpl for audit trail support
 * - Uses @Transactional annotations for data integrity
 * - Implements proper exception handling
 * - Validates business rules before persistence
 */
@Service
public class ComplianceThresholdServiceImpl extends AuditableBaseObjectServiceImpl<ComplianceThreshold, String>
        implements ComplianceThresholdService {

    @Autowired
    protected ComplianceThresholdDAO baseObjectDAO;

    ComplianceThresholdServiceImpl() {
        super(ComplianceThreshold.class);
    }

    @Override
    protected ComplianceThresholdDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByGroupId(String groupId) {
        return getBaseObjectDAO().getThresholdsByGroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByParameterCode(String parameterCode) {
        return getBaseObjectDAO().getThresholdsByParameterCode(parameterCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean evaluateThreshold(String thresholdId, BigDecimal testedValue) {
        ComplianceThreshold threshold = get(thresholdId);
        if (threshold == null || testedValue == null) {
            return false;
        }

        switch (threshold.getThresholdType()) {
            case MINIMUM:
                return testedValue.compareTo(threshold.getMinValue()) >= 0;
            case MAXIMUM:
                return testedValue.compareTo(threshold.getMaxValue()) <= 0;
            case RANGE:
                return testedValue.compareTo(threshold.getMinValue()) >= 0
                    && testedValue.compareTo(threshold.getMaxValue()) <= 0;
            case EXACT:
                return testedValue.compareTo(threshold.getTargetValue()) == 0;
            default:
                return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceThreshold getThresholdWithEvaluations(String thresholdId) {
        return getBaseObjectDAO().getThresholdWithEvaluations(thresholdId);
    }

    @Override
    @Transactional
    public List<String> bulkImport(String groupId, List<ComplianceThreshold> thresholds) {
        List<String> savedIds = new ArrayList<>();

        for (ComplianceThreshold threshold : thresholds) {
            // Validate uniqueness within group
            if (parameterExistsInGroup(groupId, threshold.getParameterCode())) {
                throw new LIMSRuntimeException("Parameter " + threshold.getParameterCode() + " already exists in group");
            }

            // Set group and sort order
            threshold.setGroup(null); // Will be set by group ID
            if (threshold.getSortOrder() == null) {
                Integer maxOrder = getBaseObjectDAO().getMaxSortOrderForGroup(groupId);
                threshold.setSortOrder(maxOrder + 1);
            }

            // Validate business rules
            validateThreshold(threshold);

            ComplianceThreshold saved = save(threshold);
            savedIds.add(saved.getId());
        }

        return savedIds;
    }

    @Override
    @Transactional
    public void reorderThresholds(String groupId, String[] newOrderIds) {
        getBaseObjectDAO().updateSortOrders(groupId, Arrays.asList(newOrderIds));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean parameterExistsInGroup(String groupId, String parameterCode) {
        return getBaseObjectDAO().parameterExistsInGroup(groupId, parameterCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByGroupIds(List<String> groupIds) {
        return getBaseObjectDAO().getThresholdsByGroupIds(groupIds);
    }

    @Override
    @Transactional
    public void deleteThresholdsByGroupId(String groupId) {
        getBaseObjectDAO().deleteThresholdsByGroupId(groupId);
    }

    @Override
    @Transactional
    public ComplianceThreshold save(ComplianceThreshold threshold) {
        validateThreshold(threshold);
        return super.save(threshold);
    }

    @Override
    @Transactional
    public ComplianceThreshold update(ComplianceThreshold threshold) {
        validateThreshold(threshold);
        return super.update(threshold);
    }

    @Override
    @Transactional
    public void delete(ComplianceThreshold threshold) {
        // Check for linked evaluations
        if (getBaseObjectDAO().hasLinkedEvaluations(threshold.getId())) {
            throw new LIMSRuntimeException("Cannot delete threshold with linked evaluations");
        }
        super.delete(threshold);
    }

    /**
     * Validate business rules for threshold values
     */
    private void validateThreshold(ComplianceThreshold threshold) {
        if (threshold.getThresholdType() == ThresholdType.RANGE) {
            if (threshold.getMinValue() != null && threshold.getMaxValue() != null) {
                if (threshold.getMinValue().compareTo(threshold.getMaxValue()) > 0) {
                    throw new LIMSRuntimeException("Minimum value cannot exceed maximum value");
                }
            }
        }

        if (threshold.getThresholdType() == ThresholdType.MINIMUM && threshold.getMinValue() == null) {
            throw new LIMSRuntimeException("Minimum value is required for MINIMUM threshold type");
        }

        if (threshold.getThresholdType() == ThresholdType.MAXIMUM && threshold.getMaxValue() == null) {
            throw new LIMSRuntimeException("Maximum value is required for MAXIMUM threshold type");
        }

        if (threshold.getThresholdType() == ThresholdType.EXACT && threshold.getTargetValue() == null) {
            throw new LIMSRuntimeException("Target value is required for EXACT threshold type");
        }
    }
}