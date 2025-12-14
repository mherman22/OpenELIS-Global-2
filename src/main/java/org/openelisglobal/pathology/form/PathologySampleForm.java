package org.openelisglobal.pathology.form;

import jakarta.validation.constraints.NotNull;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.pathology.valueholder.PathologyEnums.SampleCategory;
import org.openelisglobal.validation.annotations.SafeHtml;

public class PathologySampleForm extends BaseForm {

    private static final long serialVersionUID = 1L;

    private Integer id;

    @NotNull
    @SafeHtml
    private String sampleItemId;

    @NotNull
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

    private String receivingDate; // ISO 8601 format string

    private Integer receivingStaffId;

    public PathologySampleForm() {
        setFormName("pathologySampleForm");
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public SampleCategory getCategory() {
        return category;
    }

    public void setCategory(SampleCategory category) {
        this.category = category;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getRequestingClinician() {
        return requestingClinician;
    }

    public void setRequestingClinician(String requestingClinician) {
        this.requestingClinician = requestingClinician;
    }

    public String getClinicalDetails() {
        return clinicalDetails;
    }

    public void setClinicalDetails(String clinicalDetails) {
        this.clinicalDetails = clinicalDetails;
    }

    public String getSpecimenSite() {
        return specimenSite;
    }

    public void setSpecimenSite(String specimenSite) {
        this.specimenSite = specimenSite;
    }

    public String getStudyId() {
        return studyId;
    }

    public void setStudyId(String studyId) {
        this.studyId = studyId;
    }

    public String getPiName() {
        return piName;
    }

    public void setPiName(String piName) {
        this.piName = piName;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getEthicalApprovalRef() {
        return ethicalApprovalRef;
    }

    public void setEthicalApprovalRef(String ethicalApprovalRef) {
        this.ethicalApprovalRef = ethicalApprovalRef;
    }

    public String getSampleSource() {
        return sampleSource;
    }

    public void setSampleSource(String sampleSource) {
        this.sampleSource = sampleSource;
    }

    public String getReceivingDate() {
        return receivingDate;
    }

    public void setReceivingDate(String receivingDate) {
        this.receivingDate = receivingDate;
    }

    public Integer getReceivingStaffId() {
        return receivingStaffId;
    }

    public void setReceivingStaffId(Integer receivingStaffId) {
        this.receivingStaffId = receivingStaffId;
    }
}
