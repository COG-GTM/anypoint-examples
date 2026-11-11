package org.mule.examples.springboot.sendingjsondatatoajmsqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Flow {@code json-to-jmsFlow}: HTTP listener (POST /sales) → byte-array-to-string → JMS outbound (queue "sales")
 * → logger. The flow's final payload (the string sent to the queue) is the HTTP response.
 */
@RestController
public class SalesController {

    private static final Logger log = LoggerFactory.getLogger(SalesController.class);

    private final JmsTemplate jmsTemplate;
    private final String queue;

    public SalesController(JmsTemplate jmsTemplate, @Value("${jms.queue}") String queue) {
        this.jmsTemplate = jmsTemplate;
        this.queue = queue;
    }

    @PostMapping(path = "/sales")
    public ResponseEntity<String> sales(@RequestBody(required = false) byte[] body,
                                        @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType) {
        String payload = body == null ? "" : new String(body, charset(contentType));
        jmsTemplate.convertAndSend(queue, payload);
        log.info("Sent to JMS queue {}: {}", queue, payload);
        ResponseEntity.BodyBuilder ok = ResponseEntity.ok();
        if (contentType != null) {
            ok.header(HttpHeaders.CONTENT_TYPE, contentType);
        }
        return ok.body(payload);
    }

    private static Charset charset(String contentType) {
        try {
            if (contentType != null) {
                Charset cs = MediaType.parseMediaType(contentType).getCharset();
                if (cs != null) {
                    return cs;
                }
            }
        } catch (RuntimeException ignored) {
            // invalid Content-Type: fall back to Mule's default encoding
        }
        return StandardCharsets.UTF_8;
    }
}
