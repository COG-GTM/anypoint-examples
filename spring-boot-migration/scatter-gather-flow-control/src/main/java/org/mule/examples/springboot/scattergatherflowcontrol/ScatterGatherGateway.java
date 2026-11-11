package org.mule.examples.springboot.scattergatherflowcontrol;

import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway(defaultRequestChannel = ScatterGatherFlowConfig.INPUT)
public interface ScatterGatherGateway {

    String run(Object trigger);
}
