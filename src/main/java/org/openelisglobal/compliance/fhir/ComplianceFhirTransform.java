package org.openelisglobal.compliance.fhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.valueholder.ComplianceEvaluation;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.EvaluationResult;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FHIR R4 transformation service for compliance module entities Maps OpenELIS
 * compliance entities to FHIR resources following established patterns
 */
@Service
public class ComplianceFhirTransform {

    private static final String OPENELIS_COMPLIANCE_SYSTEM = "http://openelis.org/compliance";
    private static final String COMPLIANCE_PROFILE_BASE = "http://openelis.org/fhir/StructureDefinition/";
    private static final String COMPLIANCE_EXTENSION_BASE = "http://openelis.org/fhir/extension/";

    /**
     * Transform ComplianceStandard to FHIR Measure resource FHIR Measure represents
     * evaluation criteria and quality measures
     */
    public Measure transformToFhirMeasure(ComplianceStandard standard) {
        Measure measure = new Measure();

        // Set FHIR ID from entity UUID
        measure.setId(standard.getFhirUuidAsString());

        // Basic properties
        measure.setTitle(standard.getName());
        measure.setVersion(standard.getVersion());
        measure.setPublisher(standard.getIssuingBody());

        if (standard.getDescription() != null) {
            measure.setDescription(standard.getDescription());
        }

        // Set status based on compliance standard status
        switch (standard.getStatus()) {
        case ACTIVE:
            measure.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
            break;
        case DRAFT:
            measure.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.DRAFT);
            break;
        case SUPERSEDED:
        case ARCHIVED:
            measure.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.RETIRED);
            break;
        case SUSPENDED:
            measure.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.UNKNOWN);
            break;
        default:
            measure.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.DRAFT);
        }

        // Add primary identifier (regulation number)
        Identifier primaryIdentifier = new Identifier();
        primaryIdentifier.setSystem(OPENELIS_COMPLIANCE_SYSTEM + "/regulation-number");
        primaryIdentifier.setValue(standard.getRegulationNumber());
        primaryIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        measure.addIdentifier(primaryIdentifier);

        // Add secondary identifier (issuing body + name)
        Identifier secondaryIdentifier = new Identifier();
        secondaryIdentifier.setSystem(OPENELIS_COMPLIANCE_SYSTEM + "/issuing-body");
        secondaryIdentifier.setValue(standard.getIssuingBody() + "/" + standard.getName());
        secondaryIdentifier.setUse(Identifier.IdentifierUse.SECONDARY);
        measure.addIdentifier(secondaryIdentifier);

        // Set effective period
        if (standard.getEffectiveDate() != null) {
            Period effectivePeriod = new Period();
            effectivePeriod.setStart(java.sql.Date.valueOf(standard.getEffectiveDate()), TemporalPrecisionEnum.DAY);
            if (standard.getExpiryDate() != null) {
                effectivePeriod.setEnd(java.sql.Date.valueOf(standard.getExpiryDate()), TemporalPrecisionEnum.DAY);
            }
            measure.setEffectivePeriod(effectivePeriod);
        }

        // Add jurisdiction (country/region)
        if (standard.getCountryRegion() != null) {
            CodeableConcept jurisdiction = new CodeableConcept();
            Coding jurisdictionCoding = new Coding();
            jurisdictionCoding.setSystem("urn:iso:std:iso:3166");
            jurisdictionCoding.setCode(standard.getCountryRegion());
            jurisdiction.addCoding(jurisdictionCoding);
            measure.addJurisdiction(jurisdiction);
        }

        // Add extensions for compliance-specific fields
        if (standard.getEnforcementAuthority() != null) {
            Extension enforcementExt = new Extension(COMPLIANCE_EXTENSION_BASE + "enforcement-authority");
            enforcementExt.setValue(new StringType(standard.getEnforcementAuthority()));
            measure.addExtension(enforcementExt);
        }

        // Add applicable sample types as extension
        if (standard.getApplicableSampleTypesList() != null && !standard.getApplicableSampleTypesList().isEmpty()) {
            Extension sampleTypesExt = new Extension(COMPLIANCE_EXTENSION_BASE + "applicable-sample-types");
            for (String sampleType : standard.getApplicableSampleTypesList()) {
                Extension sampleTypeExt = new Extension("sample-type");
                sampleTypeExt.setValue(new StringType(sampleType));
                sampleTypesExt.addExtension(sampleTypeExt);
            }
            measure.addExtension(sampleTypesExt);
        }

        // Set measure type
        CodeableConcept measureType = new CodeableConcept();
        Coding measureTypeCoding = new Coding();
        measureTypeCoding.setSystem("http://terminology.hl7.org/CodeSystem/measure-type");
        measureTypeCoding.setCode("outcome"); // Compliance standards measure outcomes
        measureTypeCoding.setDisplay("Outcome");
        measureType.addCoding(measureTypeCoding);
        measure.addType(measureType);

        // Add meta profile
        measure.getMeta().addProfile(COMPLIANCE_PROFILE_BASE + "ComplianceStandard");

        // Add meta tags
        measure.getMeta().addTag(OPENELIS_COMPLIANCE_SYSTEM + "/tag", "regulatory-standard", "Regulatory Standard");

        return measure;
    }

    /**
     * Transform ComplianceEvaluation to FHIR MeasureReport FHIR MeasureReport
     * represents the results of evaluating a measure
     */
    public MeasureReport transformToFhirMeasureReport(ComplianceEvaluation evaluation) {
        MeasureReport report = new MeasureReport();

        // Set FHIR ID from entity UUID
        report.setId(evaluation.getFhirUuidAsString());

        // Set reference to the measure being evaluated
        Reference measureRef = new Reference();
        measureRef.setReference("Measure/" + evaluation.getStandard().getFhirUuidAsString());
        measureRef.setDisplay(evaluation.getStandard().getName());
        report.setMeasure(measureRef.getReference());

        // Set status based on evaluation status
        switch (evaluation.getStatus()) {
        case COMPLIANT:
            report.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
            break;
        case NON_COMPLIANT:
            report.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
            break;
        case WARNING:
            report.setStatus(MeasureReport.MeasureReportStatus.PENDING);
            break;
        case PENDING:
            report.setStatus(MeasureReport.MeasureReportStatus.PENDING);
            break;
        default:
            report.setStatus(MeasureReport.MeasureReportStatus.PENDING);
        }

        // Set report type
        report.setType(MeasureReport.MeasureReportType.INDIVIDUAL);

        // Set evaluation period
        if (evaluation.getEvaluatedDate() != null) {
            Period period = new Period();
            period.setStart(evaluation.getEvaluatedDate(), TemporalPrecisionEnum.DAY);
            period.setEnd(evaluation.getEvaluatedDate(), TemporalPrecisionEnum.DAY);
            report.setPeriod(period);
        }

        // Set date
        if (evaluation.getEvaluatedDate() != null) {
            report.setDate(evaluation.getEvaluatedDate());
        }

        // Add subject reference (sample)
        if (evaluation.getSampleId() != null) {
            Reference subjectRef = new Reference();
            subjectRef.setReference("Specimen/" + evaluation.getSampleId());
            subjectRef.setDisplay("Sample " + evaluation.getSampleId());
            report.setSubject(subjectRef);
        }

        // Add overall compliance score
        if (evaluation.getCompliancePercentage() != null) {
            MeasureReport.MeasureReportGroupComponent group = report.addGroup();

            // Set group identifier
            CodeableConcept groupCode = new CodeableConcept();
            Coding groupCoding = new Coding();
            groupCoding.setSystem(OPENELIS_COMPLIANCE_SYSTEM + "/evaluation-group");
            groupCoding.setCode("overall-compliance");
            groupCoding.setDisplay("Overall Compliance");
            groupCode.addCoding(groupCoding);
            group.setCode(groupCode);

            // Set measure score using existing compliance percentage
            Quantity measureScore = new Quantity();
            measureScore.setValue(evaluation.getCompliancePercentage());
            measureScore.setUnit("%");
            measureScore.setSystem("http://unitsofmeasure.org");
            measureScore.setCode("%");
            group.setMeasureScore(measureScore);
        }

        // Add extensions for compliance-specific fields
        Extension evaluationStatusExt = new Extension(COMPLIANCE_EXTENSION_BASE + "evaluation-status");
        evaluationStatusExt.setValue(new StringType(evaluation.getStatus().toString()));
        report.addExtension(evaluationStatusExt);

        if (evaluation.getEvaluationNotes() != null) {
            Extension notesExt = new Extension(COMPLIANCE_EXTENSION_BASE + "evaluation-notes");
            notesExt.setValue(new StringType(evaluation.getEvaluationNotes()));
            report.addExtension(notesExt);
        }

        // Add meta profile
        report.getMeta().addProfile(COMPLIANCE_PROFILE_BASE + "ComplianceEvaluation");

        // Add meta tags
        report.getMeta().addTag(OPENELIS_COMPLIANCE_SYSTEM + "/tag", "evaluation-report",
                "Compliance Evaluation Report");

        return report;
    }

    /**
     * Add parameter group as Measure.group
     */
    public void addParameterGroupToMeasure(Measure measure, ParameterGroup parameterGroup,
            List<ComplianceThreshold> thresholds) {
        Measure.MeasureGroupComponent group = measure.addGroup();

        // Set group code
        CodeableConcept groupCode = new CodeableConcept();
        Coding groupCoding = new Coding();
        groupCoding.setSystem(OPENELIS_COMPLIANCE_SYSTEM + "/parameter-group");
        groupCoding.setCode(parameterGroup.getId().toString());
        groupCoding.setDisplay(parameterGroup.getName());
        groupCode.addCoding(groupCoding);
        group.setCode(groupCode);

        // Set description
        if (parameterGroup.getDescription() != null) {
            group.setDescription(parameterGroup.getDescription());
        }

        // Add thresholds as group populations
        for (ComplianceThreshold threshold : thresholds) {
            if (threshold.getGroup() != null
                    && threshold.getGroup().getId().equals(parameterGroup.getId())) {

                Measure.MeasureGroupPopulationComponent population = group.addPopulation();

                // Set population code
                CodeableConcept popCode = new CodeableConcept();
                Coding popCoding = new Coding();
                popCoding.setSystem(OPENELIS_COMPLIANCE_SYSTEM + "/threshold-parameter");
                popCoding.setCode(threshold.getDisplayName().replaceAll("\\s+", "-").toLowerCase());
                popCoding.setDisplay(threshold.getDisplayName());
                popCode.addCoding(popCoding);
                population.setCode(popCode);

                // Set criteria expression (threshold definition)
                String criteria = buildThresholdCriteria(threshold);
                population.setCriteria(null); // Would need Expression type in real implementation

                // Add threshold as extension
                Extension thresholdExt = new Extension(COMPLIANCE_EXTENSION_BASE + "threshold-definition");
                thresholdExt.addExtension("parameter-name", new StringType(threshold.getDisplayName()));
                thresholdExt.addExtension("threshold-type", new StringType(threshold.getThresholdType().toString()));
                thresholdExt.addExtension("unit", new StringType(threshold.getUnits()));

                if (threshold.getMinValue() != null) {
                    thresholdExt.addExtension("min-value",
                            new org.hl7.fhir.r4.model.DecimalType(threshold.getMinValue()));
                }
                if (threshold.getMaxValue() != null) {
                    thresholdExt.addExtension("max-value",
                            new org.hl7.fhir.r4.model.DecimalType(threshold.getMaxValue()));
                }
                if (threshold.getTargetValue() != null) {
                    thresholdExt.addExtension("target-value",
                            new org.hl7.fhir.r4.model.DecimalType(threshold.getTargetValue()));
                }

                population.addExtension(thresholdExt);
            }
        }
    }

    /**
     * Add evaluation results to MeasureReport
     */
    public void addEvaluationResultsToReport(MeasureReport report, List<EvaluationResult> results) {
        for (EvaluationResult result : results) {
            // Find or create group for this result
            MeasureReport.MeasureReportGroupComponent group = findOrCreateGroupForResult(report, result);

            // Add stratifier for individual parameter results
            MeasureReport.MeasureReportGroupStratifierComponent stratifier = group.addStratifier();

            // Set stratifier code
            CodeableConcept stratifierCode = new CodeableConcept();
            Coding stratifierCoding = new Coding();
            stratifierCoding.setSystem(OPENELIS_COMPLIANCE_SYSTEM + "/result-parameter");
            stratifierCoding.setCode(result.getThreshold().getDisplayName().replaceAll("\\s+", "-").toLowerCase());
            stratifierCoding.setDisplay(result.getThreshold().getDisplayName());
            stratifierCode.addCoding(stratifierCoding);
            stratifier.addCode(stratifierCode);

            // Add stratum for the result
            MeasureReport.StratifierGroupComponent stratum = stratifier.addStratum();

            // Set stratum value (actual result)
            CodeableConcept stratumValue = new CodeableConcept();
            Coding stratumCoding = new Coding();
            stratumCoding.setSystem(OPENELIS_COMPLIANCE_SYSTEM + "/result-status");
            String complianceStatus = result.getIsCompliant() ? "compliant" : "non-compliant";
            stratumCoding.setCode(complianceStatus);
            stratumCoding.setDisplay(complianceStatus);
            stratumValue.addCoding(stratumCoding);
            stratum.setValue(stratumValue);

            // Set measure score (actual value)
            if (result.getTestedValue() != null) {
                Quantity measureScore = new Quantity();
                measureScore.setValue(result.getTestedValue());
                if (result.getUnits() != null) {
                    measureScore.setUnit(result.getUnits());
                    measureScore.setSystem("http://unitsofmeasure.org");
                    measureScore.setCode(result.getUnits());
                }
                stratum.setMeasureScore(measureScore);
            }

            // Add extensions for detailed result information
            Extension resultExt = new Extension(COMPLIANCE_EXTENSION_BASE + "evaluation-result");
            resultExt.addExtension("parameter-name", new StringType(result.getThreshold().getDisplayName()));
            resultExt.addExtension("actual-value", new org.hl7.fhir.r4.model.DecimalType(result.getTestedValue()));
            resultExt.addExtension("threshold-type", new StringType(result.getThreshold().getThresholdType().toString()));
            resultExt.addExtension("compliance-status", new StringType(result.getIsCompliant() ? "compliant" : "non-compliant"));

            if (result.getResultNotes() != null) {
                resultExt.addExtension("notes", new StringType(result.getResultNotes()));
            }

            stratum.addExtension(resultExt);
        }
    }

    /**
     * Async sync to FHIR store - ComplianceStandard
     */
    @Async
    @Transactional(readOnly = true)
    public void syncComplianceStandardToFhir(ComplianceStandard standard, boolean isCreate) {
        try {
            Measure measure = transformToFhirMeasure(standard);
            persistMeasure(measure, isCreate);
        } catch (Exception e) {
            LogEvent.logError("ComplianceFhirTransform", "syncComplianceStandardToFhir",
                    "Error syncing ComplianceStandard to FHIR: " + e.getMessage());
        }
    }

    /**
     * Async sync to FHIR store - ComplianceEvaluation
     */
    @Async
    @Transactional(readOnly = true)
    public void syncComplianceEvaluationToFhir(ComplianceEvaluation evaluation, boolean isCreate) {
        try {
            MeasureReport report = transformToFhirMeasureReport(evaluation);
            persistMeasureReport(report, isCreate);
        } catch (Exception e) {
            LogEvent.logError("ComplianceFhirTransform", "syncComplianceEvaluationToFhir",
                    "Error syncing ComplianceEvaluation to FHIR: " + e.getMessage());
        }
    }

    // Private helper methods

    private String buildThresholdCriteria(ComplianceThreshold threshold) {
        StringBuilder criteria = new StringBuilder();
        criteria.append(threshold.getDisplayName()).append(" ");

        switch (threshold.getThresholdType()) {
        case MAXIMUM:
            criteria.append("<=").append(threshold.getMaxValue());
            break;
        case MINIMUM:
            criteria.append(">=").append(threshold.getMinValue());
            break;
        case RANGE:
            criteria.append(">=").append(threshold.getMinValue()).append(" AND <=").append(threshold.getMaxValue());
            break;
        case EXACT:
            criteria.append("=").append(threshold.getTargetValue());
            break;
        }

        criteria.append(" ").append(threshold.getUnits());
        return criteria.toString();
    }

    private MeasureReport.MeasureReportGroupComponent findOrCreateGroupForResult(MeasureReport report,
            EvaluationResult result) {

        // Look for existing group
        for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
            if (group.hasCode() && group.getCode().hasCoding()) {
                for (Coding coding : group.getCode().getCoding()) {
                    if (coding.getDisplay().equals("Parameter Results")) {
                        return group;
                    }
                }
            }
        }

        // Create new group for parameter results
        MeasureReport.MeasureReportGroupComponent group = report.addGroup();
        CodeableConcept groupCode = new CodeableConcept();
        Coding groupCoding = new Coding();
        groupCoding.setSystem(OPENELIS_COMPLIANCE_SYSTEM + "/evaluation-group");
        groupCoding.setCode("parameter-results");
        groupCoding.setDisplay("Parameter Results");
        groupCode.addCoding(groupCoding);
        group.setCode(groupCode);

        return group;
    }

    private void persistMeasure(Measure measure, boolean isCreate) throws FhirLocalPersistingException {
        try {
            FhirPersistanceService fhirService = SpringContext.getBean(FhirPersistanceService.class);
            Map<String, Resource> resourceMap = new HashMap<>();
            String measureId = measure.getIdElement().getIdPart();
            resourceMap.put(measureId != null ? measureId : UUID.randomUUID().toString(), measure);

            if (isCreate) {
                fhirService.createFhirResourcesInFhirStore(resourceMap);
            } else {
                fhirService.updateFhirResourcesInFhirStore(resourceMap);
            }
        } catch (Exception e) {
            LogEvent.logError("ComplianceFhirTransform", "persistMeasure",
                    "Error persisting Measure to FHIR server: " + e.getMessage());
            throw new FhirLocalPersistingException(e);
        }
    }

    private void persistMeasureReport(MeasureReport report, boolean isCreate) throws FhirLocalPersistingException {
        try {
            FhirPersistanceService fhirService = SpringContext.getBean(FhirPersistanceService.class);
            Map<String, Resource> resourceMap = new HashMap<>();
            String reportId = report.getIdElement().getIdPart();
            resourceMap.put(reportId != null ? reportId : UUID.randomUUID().toString(), report);

            if (isCreate) {
                fhirService.createFhirResourcesInFhirStore(resourceMap);
            } else {
                fhirService.updateFhirResourcesInFhirStore(resourceMap);
            }
        } catch (Exception e) {
            LogEvent.logError("ComplianceFhirTransform", "persistMeasureReport",
                    "Error persisting MeasureReport to FHIR server: " + e.getMessage());
            throw new FhirLocalPersistingException(e);
        }
    }
}