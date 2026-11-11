package org.mule.examples.springboot.webserviceconsumer;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

/** Replays {@code parity/web-service-consumer} fixtures; WireMock stands in for tshirt-service.cloudhub.io. */
class WebServiceConsumerParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/web-service-consumer";
    }
}
