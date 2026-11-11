package org.mule.examples.springboot.proxyingasoapapi;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;

/**
 * Mule flow {@code main}: {@code http:listener path="/"} → {@code cxf:proxy-service payload="envelope"}
 * → {@code copy-headers} → {@code cxf:proxy-client payload="envelope"} → {@code http:request method="POST"
 * path="shop/ShopService"} → {@code copy-headers}. The envelope is forwarded byte-for-byte.
 */
@RestController
class SoapApiProxyController {

    private static final Logger log = LoggerFactory.getLogger(SoapApiProxyController.class);
    static final MediaType SOAP11 = MediaType.parseMediaType("text/xml;charset=UTF-8");

    private final WebClient shopService;
    private final String path;

    SoapApiProxyController(WebClient shopServiceClient, @Value("${soap.target.path}") String path) {
        this.shopService = shopServiceClient;
        this.path = path;
    }

    @PostMapping(path = "/", consumes = MediaType.ALL_VALUE)
    ResponseEntity<byte[]> proxy(HttpServletRequest request, @RequestBody(required = false) byte[] envelope) {
        String invalid = SoapEnvelopes.invalidReason(envelope);
        if (invalid != null) {
            log.warn("Rejected request: {}", invalid);
            return fault("Client", invalid);
        }

        HttpHeaders outbound = new HttpHeaders();
        for (String name : Collections.list(request.getHeaderNames())) {
            if (MuleHeaderFilter.isCopied(name)) {
                outbound.addAll(name, Collections.list(request.getHeaders(name)));
            }
        }
        ResponseEntity<byte[]> upstream = shopService.post()
                .uri(path)
                .headers(h -> h.addAll(outbound))
                .bodyValue(envelope)
                .exchangeToMono(r -> r.toEntity(byte[].class))
                .block();
        if (upstream == null) {
            return fault("Server", "No response from " + path);
        }
        int status = upstream.getStatusCode().value();
        if (status >= 400) {
            // http:request default success range is 0..399; the failure is turned into a fault by cxf:proxy-service.
            log.error("Response code {} mapped as failure.", status);
            return fault("Server", "Response code " + status + " mapped as failure.");
        }
        HttpHeaders headers = MuleHeaderFilter.copy(upstream.getHeaders());
        MediaType upstreamType = upstream.getHeaders().getContentType();
        headers.setContentType(upstreamType != null && upstreamType.getCharset() != null
                ? new MediaType(SOAP11, upstreamType.getCharset()) : SOAP11);
        return ResponseEntity.ok().headers(headers).body(upstream.getBody() == null ? new byte[0] : upstream.getBody());
    }

    private static ResponseEntity<byte[]> fault(String code, String reason) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(SOAP11)
                .body(SoapEnvelopes.fault(code, reason));
    }
}
