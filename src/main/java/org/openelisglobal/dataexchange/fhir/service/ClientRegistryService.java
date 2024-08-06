package org.openelisglobal.dataexchange.fhir.service;

import java.util.List;
import org.hl7.fhir.r4.model.Patient;

public interface ClientRegistryService {
    List<Patient> getCRPatients(String sourceIdentifier, String sourceIdentifierSystem, List<String> extraTargetSystems);
}
