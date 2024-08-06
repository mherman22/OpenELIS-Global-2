package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.model.api.annotation.SearchParamDefinition;

public class FhirConstants {

    public static final String PATIENT = "Patient";

    public static final String IHE_PIX_OPERATION = "$ihe-pix";

    @SearchParamDefinition(name = "sourceIdentifier", path = "Patient.sourceIdentifier", description = "A patient identifier used to find cross-matching identifiers in client registry", type = "token")
	public static final String SOURCE_IDENTIFIER = "sourceIdentifier";

    @SearchParamDefinition(name = "targetSystem", path = "Patient.targetSystem", description = "Assigning Authorities for the Patient Identifier Domains from which the returned identifiers shall be selected", type = "token")
	public static final String TARGET_SYSTEM = "targetSystem";

}
