package org.mule.examples.springboot.proxyingarestapi;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

/** Replays {@code parity/proxying-a-rest-api} fixtures; WireMock stands in for api.box.com. */
class ProxyingARestApiParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/proxying-a-rest-api";
    }
}
