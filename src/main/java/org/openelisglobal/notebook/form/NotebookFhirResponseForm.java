package org.openelisglobal.notebook.form;

import java.util.HashMap;
import java.util.Map;

public class NotebookFhirResponseForm {

    private String status;
    private String specimenId;
    private Map<String, Object> responses = new HashMap<>();

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSpecimenId() {
        return specimenId;
    }

    public void setSpecimenId(String specimenId) {
        this.specimenId = specimenId;
    }

    public Map<String, Object> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, Object> responses) {
        this.responses = responses;
    }
}
