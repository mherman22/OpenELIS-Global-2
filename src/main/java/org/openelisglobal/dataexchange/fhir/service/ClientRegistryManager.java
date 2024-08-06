package org.openelisglobal.dataexchange.fhir.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClientRegistryManager {

    @Autowired
	private ClientRegistryServiceImpl fhirPatientService;

    public enum ClientRegistryTransactionType {
        FHIR,
        HL7
    }

    /**
	 * Determine the appropriate PatientService class based off of the client registry transaction
	 * type configuration
	 * 
	 * @return PatientService class corresponding to the appropriate transaction type supported by
	 *         the client registry
	 * @throws IllegalArgumentException if defined transaction type is unsupported
	 */
	public ClientRegistryService getPatientService() throws IllegalArgumentException {
		try {
			String transactionMethodGlobalProperty = "clientregistry.transactionMethod".toUpperCase();
			
			switch (ClientRegistryTransactionType.valueOf(transactionMethodGlobalProperty)) {
				case FHIR:
					return fhirPatientService;
				case HL7:
					throw new IllegalArgumentException("HL7 transaction type is currently unsupported");
			}
		}
		catch (Exception ignored) {
			
		}
		
		throw new IllegalArgumentException("Unsupported transaction type");
	}
}
