package org.mule.examples.springboot.xmlonlysoapwebservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

/**
 * Mule sub-flows {@code invokePatientService} / {@code invokeEHRService}: {@code cxf:proxy-client payload="body"} +
 * {@code http:request method="POST" path="PatientService|EHRService"} against the app's own listener.
 */
@Component
class HospitalServiceClient {

    private final WebServiceTemplate template = new WebServiceTemplate();
    private final Environment environment;
    private final String baseUrl;

    HospitalServiceClient(Environment environment, @Value("${hospital.services.base-url:}") String baseUrl) {
        this.environment = environment;
        this.baseUrl = baseUrl;
    }

    Element patientService(Element request) {
        return call("/PatientService", request);
    }

    Element ehrService(Element request) {
        return call("/EHRService", request);
    }

    private Element call(String path, Element request) {
        DOMResult result = new DOMResult();
        template.sendSourceAndReceiveToResult(base() + path, new DOMSource(request), message -> {
            if (message instanceof SaajSoapMessage saaj) {
                try {
                    saaj.getSaajMessage().getSOAPHeader().detachNode();
                } catch (jakarta.xml.soap.SOAPException e) {
                    throw new IllegalStateException(e);
                }
            }
        }, result);
        Node node = result.getNode();
        return node instanceof Document d ? d.getDocumentElement() : (Element) node.getFirstChild();
    }

    private String base() {
        if (!baseUrl.isBlank()) {
            return baseUrl;
        }
        return "http://localhost:" + environment.getRequiredProperty("local.server.port");
    }
}
