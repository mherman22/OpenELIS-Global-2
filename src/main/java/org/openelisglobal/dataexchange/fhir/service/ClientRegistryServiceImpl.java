package org.openelisglobal.dataexchange.fhir.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInputAndPartialOutput;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;

@Service
public class ClientRegistryServiceImpl implements ClientRegistryService {

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private FhirConfig fhirConfig;

    /**
     * Get patient identifiers from an external client registry's $ihe-pix implementation.
     * Use the returned identifiers to then request a matching Patient bundle from the client registry.
     *
     * @param sourceIdentifier        the identifier to search for
     * @param sourceIdentifierSystem  the system of the source identifier
     * @param targetSystems           the target systems to search within
     * @return a list of matching Patient resources
     */
    @Override
    public List<Patient> getCRPatients(String sourceIdentifier, String sourceIdentifierSystem, List<String> targetSystems) {
        // Get patient identifiers from the external FHIR $ihe-pix endpoint
        List<String> crIdentifiers = fetchPatientIdentifiers(sourceIdentifier, sourceIdentifierSystem, targetSystems);
        
        if (crIdentifiers.isEmpty()) {
            return Collections.emptyList();
        }

        // Get matching Patient resources from the external client registry
        Bundle patientBundle = fetchPatientBundle(crIdentifiers);
        
        return parseCRPatientSearchResults(patientBundle);
    }

    /**
     * Fetch patient identifiers from the external FHIR $ihe-pix endpoint.
     *
     * @param sourceIdentifier       the identifier to search for
     * @param sourceIdentifierSystem the system of the source identifier
     * @param targetSystems          the target systems to search within
     * @return a list of patient identifiers
     */
    private List<String> fetchPatientIdentifiers(String sourceIdentifier, String sourceIdentifierSystem, List<String> targetSystems) {
        IOperationUntypedWithInputAndPartialOutput<Parameters> identifiersRequest = fhirUtil.getFhirClient(
            fhirConfig.getClientRegistryServerUrl(),
            fhirConfig.getClientRegistryUserName(),
            fhirConfig.getClientRegistryPassword())
            .operation()
            .onType(FhirConstants.PATIENT)
            .named(FhirConstants.IHE_PIX_OPERATION)
            .withSearchParameter(Parameters.class, FhirConstants.SOURCE_IDENTIFIER, new TokenParam(sourceIdentifierSystem, sourceIdentifier));

        if (!targetSystems.isEmpty()) {
            identifiersRequest.andSearchParameter(FhirConstants.TARGET_SYSTEM, new StringParam(String.join(",", targetSystems)));
        }

        Parameters crMatchingParams = identifiersRequest.useHttpGet().execute();
        return crMatchingParams.getParameter().stream()
                .filter(param -> Objects.equals(param.getName(), "targetId"))
                .map(param -> param.getValue().toString())
                .collect(Collectors.toList());
    }

    /**
     * Fetch matching Patient resources from the external client registry.
     *
     * @param crIdentifiers the identifiers to search for
     * @return a Bundle of matching Patient resources
     */
    private Bundle fetchPatientBundle(List<String> crIdentifiers) {
        return fhirUtil.getFhirClient(
            fhirConfig.getClientRegistryServerUrl(),
            fhirConfig.getClientRegistryUserName(),
            fhirConfig.getClientRegistryPassword())
            .search()
            .forResource(Patient.class)
            .where(new StringClientParam(Patient.SP_RES_ID).matches().values(crIdentifiers))
            .returnBundle(Bundle.class)
            .execute();
    }

    /**
     * Filter and parse out FHIR patients from Client Registry Patient Search results.
     *
     * @param patientBundle the Bundle of Patient resources
     * @return a list of Patient resources
     */
    private List<Patient> parseCRPatientSearchResults(Bundle patientBundle) {
        return patientBundle.getEntry().stream()
                .filter(entry -> entry.hasResource() && entry.getResource() instanceof Patient)
                .map(entry -> (Patient) entry.getResource())
                .collect(Collectors.toList());
    }
}
