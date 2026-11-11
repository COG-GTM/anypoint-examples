package org.mule.examples.springboot.soapwebservicesecurity;

import org.apache.wss4j.common.crypto.Crypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Optional;

/** Mule flow {@code SecurityClients} ({@code service-clients.xml}): {@code GET /client?clientType=..&name=..}. */
@RestController
class SecurityClientController {

    private static final Logger log = LoggerFactory.getLogger(SecurityClientController.class);
    private static final MediaType TEXT_PLAIN = MediaType.TEXT_PLAIN;

    private final Crypto crypto;
    private final Environment environment;
    private final String servicesBaseUrl;

    SecurityClientController(Crypto wssCrypto, Environment environment,
                             @Value("${security.services.base-url:}") String servicesBaseUrl) {
        this.crypto = wssCrypto;
        this.environment = environment;
        this.servicesBaseUrl = servicesBaseUrl;
    }

    @GetMapping("/client")
    ResponseEntity<String> client(@RequestParam(required = false) String name,
                                  @RequestParam(required = false) String clientType) {
        String payload;
        try {
            payload = SecurityClients.forType(clientType, crypto)
                    .map(c -> greet(c, name))
                    .orElse("Client type is not supported");
        } catch (RuntimeException e) {
            // catch-exception-strategy
            log.warn("Client call failed", e);
            payload = "There has been an Error processing the request";
        }
        return ResponseEntity.ok().contentType(TEXT_PLAIN).body(payload);
    }

    /** {@code cxf:jaxws-client operation="greet"} + {@code http:request POST <path>}. */
    private String greet(SecurityClients.Client client, String name) {
        WebServiceTemplate template = new WebServiceTemplate();
        client.interceptor().ifPresent(i -> template.setInterceptors(new ClientInterceptor[] {i}));
        String request = "<ns2:greet xmlns:ns2=\"" + GreeterEndpoint.NAMESPACE + "\"><name>"
                + escape(Optional.ofNullable(name).orElse("")) + "</name></ns2:greet>";
        DOMResult result = new DOMResult();
        template.sendSourceAndReceiveToResult(base() + "/" + client.path(), new StreamSource(new StringReader(request)), result);
        org.w3c.dom.Node node = result.getNode();
        return node instanceof org.w3c.dom.Document d ? d.getDocumentElement().getTextContent() : node.getTextContent();
    }

    private String base() {
        if (!servicesBaseUrl.isBlank()) {
            return servicesBaseUrl;
        }
        return "http://localhost:" + environment.getRequiredProperty("local.server.port") + "/services";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
