package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

/**
 * Samsung SOAP service and price service are WireMock stubs. Fixtures run in file order against one context, so the
 * price cache and the populated tables carry over (as in one running Mule app).
 */
class ServiceOrchestrationAndChoiceRoutingParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/service-orchestration-and-choice-routing";
    }
}
