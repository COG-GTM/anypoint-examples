package org.mule.examples.springboot.xmlonlysoapwebservice;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Element;

import static org.mule.examples.springboot.xmlonlysoapwebservice.Hospital.MSG;

/** Mule flow {@code EHRService} ({@code mocks.xml}, {@code cxf:proxy-service payload="body"} + choice on the operation). */
@Endpoint
class EhrServiceEndpoint {

    /** {@code when operation == 'createEpisode'}. */
    @PayloadRoot(namespace = MSG, localPart = "createEpisode")
    @ResponsePayload
    Element createEpisode(@RequestPayload Element request) {
        Element response = Hospital.root("createEpisodeResponse");
        Element episode = Hospital.model(response, "Episode");
        String now = Hospital.now();
        Hospital.plain(episode, "episodeId", now);
        Hospital.model(episode, "PatientId").setTextContent(Hospital.text(request, "PatientId"));
        Hospital.plain(episode, "admission", "Elective");
        Hospital.plain(episode, "startDate", now);
        Hospital.plain(episode, "endDate", now);
        Hospital.plain(episode, "care", "Private");
        return response;
    }

    /** {@code otherwise} branch (the WSDL's only other operation, {@code findEpisodes}). */
    @PayloadRoot(namespace = MSG, localPart = "findEpisodes")
    @ResponsePayload
    Element findEpisodes(@RequestPayload Element request) {
        Element response = Hospital.root("findEpisodesResponse");
        Element episode = Hospital.model(response, "Episode");
        Hospital.model(episode, "PatientId").setTextContent(Hospital.text(request, "PatientId"));
        return response;
    }
}
