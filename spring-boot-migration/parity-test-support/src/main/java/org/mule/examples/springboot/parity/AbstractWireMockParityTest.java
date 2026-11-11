package org.mule.examples.springboot.parity;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * {@link AbstractParityTest} plus one shared WireMock server standing in for every downstream
 * system. Its URL is exposed as {@code parity.downstream.base-url}; map your client properties to it
 * in {@code application-parity.yml}, e.g. {@code salesforce.base-url: ${parity.downstream.base-url}}.
 * Before each case WireMock is reset and loaded with the fixture's {@code downstream} stubs; after
 * the Spring Boot call the fixture's {@code expectedOutbound} requests are verified.
 */
public abstract class AbstractWireMockParityTest extends AbstractParityTest {

    public static final String DOWNSTREAM_BASE_URL_PROPERTY = "parity.downstream.base-url";

    protected static final WireMockParitySupport DOWNSTREAM = new WireMockParitySupport().start();

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) {
        registry.add(DOWNSTREAM_BASE_URL_PROPERTY, DOWNSTREAM::baseUrl);
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        DOWNSTREAM.reset();
        DOWNSTREAM.stub(fixture.downstream());
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        ParityAssertions.fail(fixture, "outbound side effects", DOWNSTREAM.verifyOutbound(fixture.expectedOutbound()), spring);
    }
}
