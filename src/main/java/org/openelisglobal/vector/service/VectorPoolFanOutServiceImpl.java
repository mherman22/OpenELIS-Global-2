package org.openelisglobal.vector.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link VectorPoolFanOutService} implementation. See the interface
 * Javadoc for behavior contract; this class is the persist-side mechanic (build
 * {@code N} child SampleItems, copy collection metadata from the parent, insert
 * via {@link SampleItemService}). All {@code @Transactional} boundaries live
 * here so the interface stays declaration-only and easy to mock from tests.
 */
@Service
public class VectorPoolFanOutServiceImpl implements VectorPoolFanOutService {

    @Autowired
    private SampleItemService sampleItemService;

    @Override
    @Transactional
    public List<SampleItem> fanOut(SampleItem parent, int poolCount, String sysUserId) {
        if (parent == null || parent.getId() == null) {
            return Collections.emptyList();
        }
        if (poolCount <= 1) {
            return Collections.emptyList();
        }
        Sample parentSample = parent.getSample();
        String enteredStatusId = SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.Entered);

        List<SampleItem> created = new ArrayList<>(poolCount);
        for (int i = 1; i <= poolCount; i++) {
            try {
                SampleItem child = new SampleItem();
                child.setSample(parentSample);
                child.setTypeOfSample(parent.getTypeOfSample());
                child.setSourceOfSample(parent.getSourceOfSample());
                child.setUnitOfMeasure(parent.getUnitOfMeasure());
                child.setStatusId(enteredStatusId);
                child.setSortOrder(String.valueOf(i));
                child.setParentSampleItem(parent);
                // Each child represents one organism. quantity is a Double on
                // SampleItem (legacy) — set to 1.0 to make the per-specimen
                // semantic explicit.
                child.setQuantity(1.0);
                child.setSysUserId(sysUserId);
                child.setCollectionDate(parent.getCollectionDate());
                child.setReceivedDate(parent.getReceivedDate());
                child.setCollector(parent.getCollector());
                child.setCollectionConditions(parent.getCollectionConditions());
                child.setCollectionMethod(parent.getCollectionMethod());
                child.setSampleTemperature(parent.getSampleTemperature());
                child.setSpecimenOrigin(parent.getSpecimenOrigin());
                // FHIR resource identity — each child gets its own UUID so
                // it can be referenced individually in outbound exports.
                child.setFhirUuid(UUID.randomUUID());

                String childId = sampleItemService.insert(child);
                // Re-fetch so the returned reference is the managed entity
                // with its generated id (insert returns the id String, not
                // the entity).
                SampleItem persisted = sampleItemService.get(childId);
                created.add(persisted != null ? persisted : child);
            } catch (RuntimeException e) {
                LogEvent.logError(this.getClass().getName(), "fanOut", "Failed to create child SampleItem #" + i
                        + " for parent " + parent.getId() + ": " + e.getMessage());
                throw e;
            }
        }
        LogEvent.logInfo(this.getClass().getName(), "fanOut",
                "Vector pool fan-out: created " + created.size() + " child SampleItems under parent " + parent.getId()
                        + " (sample " + (parentSample != null ? parentSample.getId() : "?") + ")");
        return created;
    }

    @Override
    @Transactional
    public List<SampleItem> fanOutAll(List<SampleItem> parents, int poolCount, String sysUserId) {
        if (parents == null || parents.isEmpty() || poolCount <= 1) {
            return Collections.emptyList();
        }
        List<SampleItem> all = new ArrayList<>();
        for (SampleItem parent : parents) {
            all.addAll(fanOut(parent, poolCount, sysUserId));
        }
        return all;
    }
}
