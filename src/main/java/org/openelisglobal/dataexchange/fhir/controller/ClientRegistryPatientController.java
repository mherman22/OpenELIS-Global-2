package org.openelisglobal.dataexchange.fhir.controller;

import org.hl7.fhir.r4.model.Patient;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.ClientRegistryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cr/patients")
public class ClientRegistryPatientController {

    @Autowired
    private ClientRegistryManager clientRegistryManager;

    @Autowired
    private FhirConfig config;

    /**
     * REST endpoint to get Patient references from external client registry.
     * Example request: GET /clientregistry/patients/$ihe-pix?sourceIdentifier=sourceSystem|1234&targetSystem=system1,system2
     *
     * @param sourceIdentifierParam patient identifier token. If source system is included in token,
     *                              it will be used to override the module-defined source system.
     * @param targetSystemsParam    (optional) Patient assigning authorities (i.e., systems) from which
     *                              the returned identifiers shall be selected.
     * @return List of matching FHIR patients returned by the client registry.
     */
    @GetMapping("/$ihe-pix")
    public ResponseEntity<List<PatientDTO>> getCRPatientById(
            @RequestParam(name = "sourceIdentifier") String sourceIdentifierParam,
            @RequestParam(name = "targetSystem", required = false) List<String> targetSystemsParam) {

        validateSourceIdentifier(sourceIdentifierParam);

        List<String> targetSystems = targetSystemsParam == null
                ? Collections.emptyList()
                : targetSystemsParam.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String sourceIdentifierSystem = getSourceIdentifierSystem(sourceIdentifierParam);

        List<PatientDTO> patients = fetchPatientsFromClientRegistry(sourceIdentifierParam, sourceIdentifierSystem, targetSystems);

        if (patients.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(patients);
    }

    private void validateSourceIdentifier(String sourceIdentifierParam) {
        if (sourceIdentifierParam == null || sourceIdentifierParam.isEmpty()) {
            throw new IllegalArgumentException("sourceIdentifier must be specified");
        }
    }

    private String getSourceIdentifierSystem(String sourceIdentifierParam) {
        boolean userDefinedSourceSystem = sourceIdentifierParam.contains("|");
        String sourceIdentifierSystem = userDefinedSourceSystem
                ? sourceIdentifierParam.split("\\|")[0]
                : config.getClientRegistryDefaultPatientIdentifierSystem();

        if (sourceIdentifierSystem == null || sourceIdentifierSystem.isEmpty()) {
            throw new IllegalArgumentException("ClientRegistry module does not have a default source system assigned " +
                    "via the defaultPatientIdentifierSystem property. Source system must be provided as a token in " +
                    "the sourceIdentifier request param");
        }
        return sourceIdentifierSystem;
    }

    private List<PatientDTO> fetchPatientsFromClientRegistry(String sourceIdentifierParam, String sourceIdentifierSystem, List<String> targetSystems) {
        List<Patient> patients = clientRegistryManager.getPatientService().getCRPatients(
                sourceIdentifierParam.split("\\|")[1], sourceIdentifierSystem, targetSystems);

        return patients.stream()
                .map(PatientDTO::new)
                .collect(Collectors.toList());
    }

    @Getter
    @Setter
    public class PatientDTO {
        private String id;
        private String name;
    
        public PatientDTO(Patient patient) {
            this.id = patient.getIdElement().getIdPart();
            if (!patient.getName().isEmpty()) {
                this.name = patient.getName().get(0).getNameAsSingleString();
            }
        }
    }
}
