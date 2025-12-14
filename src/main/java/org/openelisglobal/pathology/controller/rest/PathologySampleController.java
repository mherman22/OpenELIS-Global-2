package org.openelisglobal.pathology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.pathology.service.PathologySampleRegistrationService;
import org.openelisglobal.pathology.valueholder.PathologyEnums.SampleCategory;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/pathology/samples")
public class PathologySampleController extends BaseRestController {

    @Autowired
    private PathologySampleRegistrationService pathologySampleRegistrationService;

    @Autowired
    private SampleItemService sampleItemService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathologySampleRegistration> registerSample(
            @Valid @RequestBody SampleRegistrationRequest request, HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            PathologySampleRegistration registration = convertToEntity(request, sysUserId);
            PathologySampleRegistration savedRegistration = pathologySampleRegistrationService
                    .registerSample(registration);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedRegistration);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PathologySampleRegistration>> searchSamples(
            @RequestParam(required = false) SampleCategory category, @RequestParam(required = false) String studyId,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
        try {
            List<PathologySampleRegistration> registrations = pathologySampleRegistrationService.searchSamples(category,
                    studyId, offset, limit);
            return ResponseEntity.ok(registrations);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/category", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PathologySampleRegistration>> getSamplesByCategory(
            @RequestParam SampleCategory category) {
        try {
            List<PathologySampleRegistration> registrations = pathologySampleRegistrationService
                    .findByCategory(category);
            return ResponseEntity.ok(registrations);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private PathologySampleRegistration convertToEntity(SampleRegistrationRequest request, String sysUserId) {
        PathologySampleRegistration registration = new PathologySampleRegistration();

        // Get sample item
        SampleItem sampleItem = sampleItemService.get(request.getSampleItemId());
        if (sampleItem == null) {
            throw new IllegalArgumentException("Sample item not found: " + request.getSampleItemId());
        }
        registration.setSampleItem(sampleItem);

        // Set category and category-specific fields
        registration.setCategory(request.getCategory());

        if (request.getCategory() == SampleCategory.CLINICAL) {
            registration.setPatientId(request.getPatientId());
            registration.setRequestingClinician(request.getRequestingClinician());
            registration.setClinicalDetails(request.getClinicalDetails());
            registration.setSpecimenSite(request.getSpecimenSite());
        } else if (request.getCategory() == SampleCategory.RESEARCH) {
            registration.setStudyId(request.getStudyId());
            registration.setPiName(request.getPiName());
            registration.setParticipantId(request.getParticipantId());
            registration.setEthicalApprovalRef(request.getEthicalApprovalRef());
        }

        // Set common fields
        registration.setSampleSource(request.getSampleSource());

        if (request.getReceivingDate() != null && !request.getReceivingDate().isEmpty()) {
            registration.setReceivingDate(Timestamp.from(Instant.parse(request.getReceivingDate())));
        }

        registration.setReceivingStaffId(request.getReceivingStaffId());

        return registration;
    }

    @Setter
    @Getter
    public static class SampleRegistrationRequest {

        @jakarta.validation.constraints.NotNull
        @SafeHtml
        private String sampleItemId;

        @jakarta.validation.constraints.NotNull
        private SampleCategory category;

        // Clinical specimen fields
        @SafeHtml
        private String patientId;

        @SafeHtml
        private String requestingClinician;

        @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
        private String clinicalDetails;

        @SafeHtml
        private String specimenSite;

        // Research specimen fields
        @SafeHtml
        private String studyId;

        @SafeHtml
        private String piName;

        @SafeHtml
        private String participantId;

        @SafeHtml
        private String ethicalApprovalRef;

        // Common fields
        @SafeHtml
        private String sampleSource = "Alert Hospital";

        private String receivingDate;

        private Integer receivingStaffId;
    }
}
