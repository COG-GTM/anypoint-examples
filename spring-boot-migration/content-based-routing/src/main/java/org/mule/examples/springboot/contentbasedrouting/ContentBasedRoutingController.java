package org.mule.examples.springboot.contentbasedrouting;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code http:listener path="/"} (any method) of {@code content-based-routingFlow}. */
@RestController
public class ContentBasedRoutingController {

    static final String FAVICON = "/favicon.ico";

    private final GreetingGateway gateway;

    public ContentBasedRoutingController(GreetingGateway gateway) {
        this.gateway = gateway;
    }

    @RequestMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> greet(@RequestParam(name = "language", required = false) String language,
                                        @RequestBody(required = false) String body) {
        // expression-filter #[payload != '/favicon.ico']: a filtered message ends the flow with an empty 200.
        if (FAVICON.equals(body)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(gateway.greet(new GreetingRequest(language)).getPayload());
    }
}
