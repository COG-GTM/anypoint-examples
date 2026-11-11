package org.mule.examples.springboot.scattergatherflowcontrol;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code http:listener path="/scatterGather"} (any method); the reply is the payload left by outboundFlow. */
@RestController
public class ScatterGatherController {

    private final ScatterGatherGateway gateway;

    public ScatterGatherController(ScatterGatherGateway gateway) {
        this.gateway = gateway;
    }

    @RequestMapping(path = "/scatterGather", produces = MediaType.TEXT_PLAIN_VALUE)
    public String scatterGather() {
        return gateway.run("/scatterGather");
    }
}
