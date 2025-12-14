package org.openelisglobal.pathology.valueholder;

/**
 * Consolidated enumerations for pathology module. Contains all enum types used
 * across pathology entities.
 */
public class PathologyEnums {

    /** Sample category: Clinical diagnostic vs Research specimens */
    public enum SampleCategory {
        CLINICAL, RESEARCH
    }

    /** Quality control inspection types */
    public enum QCType {
        INITIAL_INSPECTION, BLOCK_QC, SLIDE_QC, STAIN_QC
    }

    /** Quality control pass/fail status */
    public enum QCStatus {
        PASS, FAIL
    }

    /** Actions taken when QC fails */
    public enum QCAction {
        REPROCESS, RECOLLECT, ESCALATE, DOCUMENT_ONLY
    }

    /** Test/assay types performed in pathology */
    public enum TestType {
        HISTOCHEMISTRY, IMMUNOHISTOCHEMISTRY, MOLECULAR, CYTOLOGY
    }

    /** Stain quality assessment levels */
    public enum StainQuality {
        EXCELLENT, GOOD, ACCEPTABLE, POOR, UNACCEPTABLE
    }

    /** Control validation status (positive/negative controls) */
    public enum ControlStatus {
        PASS, FAIL, NOT_TESTED
    }

    /** Reference document types (SOPs, protocols) */
    public enum DocumentType {
        SOP, PROTOCOL, GUIDELINE, FORM
    }

    /** Project access roles for research samples */
    public enum AccessRole {
        COORDINATOR, TECHNICIAN, PATHOLOGIST
    }

    // Private constructor to prevent instantiation
    private PathologyEnums() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
