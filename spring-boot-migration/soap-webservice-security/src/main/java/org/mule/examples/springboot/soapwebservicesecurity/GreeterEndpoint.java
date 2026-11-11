package org.mule.examples.springboot.soapwebservicesecurity;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Mule {@code cxf:jaxws-service serviceClass="Greeter"} + {@code component GreeterService}: the JAX-WS wrapped
 * document/literal {@code greet} operation ({@code @WebParam(name="name")}, {@code @WebResult(name="name")}).
 * Served on every path; WS-Security is applied per path by {@link WebServiceConfiguration}.
 */
@Endpoint
class GreeterEndpoint {

    /** JAX-WS default target namespace of {@code com.mulesoft.mule.example.security.Greeter}. */
    static final String NAMESPACE = "http://security.example.mule.mulesoft.com/";

    @PayloadRoot(namespace = NAMESPACE, localPart = "greet")
    @ResponsePayload
    Element greet(@RequestPayload Element request) {
        String name = null;
        for (Node n = request.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && "name".equals(e.getLocalName())) {
                name = e.getTextContent();
            }
        }
        Document doc = newDocument();
        Element response = doc.createElementNS(NAMESPACE, "ns2:greetResponse");
        Element result = doc.createElementNS(null, "name");
        result.setTextContent(greet(name));
        response.appendChild(result);
        doc.appendChild(response);
        return response;
    }

    /** {@code GreeterService#greet}. */
    static String greet(String name) {
        return "Hello " + name;
    }

    private static Document newDocument() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
