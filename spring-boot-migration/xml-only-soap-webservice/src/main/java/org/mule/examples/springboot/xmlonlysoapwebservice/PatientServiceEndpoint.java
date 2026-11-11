package org.mule.examples.springboot.xmlonlysoapwebservice;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Element;

import static org.mule.examples.springboot.xmlonlysoapwebservice.Hospital.MSG;

/** Mule flow {@code PatientService} ({@code mocks.xml}, {@code cxf:proxy-service payload="body"} + choice on the operation). */
@Endpoint
class PatientServiceEndpoint {

    /** {@code when operation == 'upsertPatient'}: {@code ns1#PatientId: now as :string}. */
    @PayloadRoot(namespace = MSG, localPart = "upsertPatient")
    @ResponsePayload
    Element upsertPatient(@RequestPayload Element request) {
        Element response = Hospital.root("upsertPatientResponse");
        Hospital.model(response, "PatientId").setTextContent(Hospital.now());
        return response;
    }

    /** {@code otherwise} branch (the WSDL's only other operation, {@code getPatient}): a fixed Donald Duck patient. */
    @PayloadRoot(namespace = MSG, localPart = "getPatient")
    @ResponsePayload
    Element getPatient(@RequestPayload Element request) {
        Element response = Hospital.root("getPatientResponse");
        Element patient = Hospital.model(response, "Patient");
        String now = Hospital.now();
        Hospital.plain(patient, "patientId", now);
        Hospital.plain(patient, "dateOfBirth", "1930-01-01");
        Hospital.plain(patient, "gender", "Male");
        Hospital.plain(patient, "nationality", "USA");
        Hospital.plain(patient, "address1", "DisneyLand");
        Hospital.plain(patient, "lastName", "Duck");
        Hospital.plain(patient, "firstName", "Donald");
        Hospital.plain(patient, "nationalId", now);
        return response;
    }
}
