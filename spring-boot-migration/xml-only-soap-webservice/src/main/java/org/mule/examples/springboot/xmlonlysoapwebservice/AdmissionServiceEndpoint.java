package org.mule.examples.springboot.xmlonlysoapwebservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mule.examples.springboot.xmlonlysoapwebservice.Hospital.MSG;

/**
 * Mule flow {@code admitPatientService} ({@code Hospital_Admissions_SOA.xml}): {@code upsertPatient} sub-flow →
 * {@code createEpisode} sub-flow → DataWeave {@code createEpisodeResponse to admitSubjectResponse}.
 */
@Endpoint
class AdmissionServiceEndpoint {

    private static final Logger log = LoggerFactory.getLogger(AdmissionServiceEndpoint.class);

    private final HospitalServiceClient services;

    AdmissionServiceEndpoint(HospitalServiceClient services) {
        this.services = services;
    }

    @PayloadRoot(namespace = MSG, localPart = "admitSubject")
    @ResponsePayload
    Element admitSubject(@RequestPayload Element admitSubject) {
        // admitSubject to upsertPatient: ns0#upsertPatient: { ns1#Subject: payload.Envelope.Body.admitSubject.Subject }
        Element upsert = Hospital.root("upsertPatient");
        Element subject = Hospital.model(upsert, "Subject");
        Hospital.child(admitSubject, "Subject").ifPresent(s -> copyChildren(s, subject));
        Element upsertResponse = services.patientService(upsert);

        // upsertPatientResponse to createEpisode: ns1#PatientId: payload.upsertPatientResponse.ns1#PatientId
        Element createEpisode = Hospital.root("createEpisode");
        Hospital.model(createEpisode, "PatientId").setTextContent(Hospital.text(upsertResponse, "PatientId"));
        Element episodeResponse = services.ehrService(createEpisode);
        Element episode = Hospital.child(episodeResponse, "Episode").orElseThrow();
        log.debug("Created episode {}", Hospital.text(episode, "episodeId"));

        // createEpisodeResponse to admitSubjectResponse
        Element response = Hospital.root("admitSubjectResponse");
        Element out = Hospital.model(response, "Episode");
        Hospital.plain(out, "episodeId", Hospital.text(episode, "episodeId"));
        Hospital.model(out, "PatientId").setTextContent(Hospital.text(episode, "PatientId"));
        Hospital.plain(out, "admission", Hospital.text(episode, "admission"));
        String start = Hospital.text(episode, "startDate");
        if (start != null) {
            Hospital.plain(out, "startDate", asDate(start).toString());
        }
        String end = Hospital.text(episode, "endDate");
        if (end != null) {
            Hospital.plain(out, "endDate", asDate(end).plusDays(5).toString());
        }
        Hospital.plain(out, "care", Hospital.text(episode, "care"));
        // ns1#"Bill " (the trailing space in the DataWeave key is not part of the XML element name)
        Element bill = Hospital.model(response, "Bill");
        Hospital.plain(bill, "costPerNight", "100");
        Hospital.plain(bill, "initialStateEstimate", "5");
        Hospital.plain(bill, "runningTotal", "500");
        Hospital.plain(bill, "status", "ADMITTED");
        return response;
    }

    /** DataWeave {@code as :date} on the EHR service's {@code now} datetime. */
    private static LocalDate asDate(String value) {
        return value.length() > 10 ? OffsetDateTime.parse(value, Hospital.DW_DATETIME).toLocalDate() : LocalDate.parse(value);
    }

    private static void copyChildren(Element from, Element to) {
        for (Node n = from.getFirstChild(); n != null; n = n.getNextSibling()) {
            to.appendChild(to.getOwnerDocument().importNode(n, true));
        }
    }
}
