package org.mule.examples.springboot.parity.sample;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

/**
 * Sample parity suite: every fixture in {@code parity/sample} is replayed against the Spring Boot
 * app; downstream CRM calls are served and verified by WireMock. Add
 * {@code -Dparity.mule.base-url=http://localhost:8081} to also compare against a live Mule runtime.
 */
class SampleParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/sample";
    }
}
