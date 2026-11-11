package org.mule.examples.springboot.proxyingasoapapi;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

/** Replays {@code parity/proxying-a-soap-api} fixtures; WireMock stands in for the predic8 ShopService. */
class ProxyingASoapApiParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/proxying-a-soap-api";
    }
}
