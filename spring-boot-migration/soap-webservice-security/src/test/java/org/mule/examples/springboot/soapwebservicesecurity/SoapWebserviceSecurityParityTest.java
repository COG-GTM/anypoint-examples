package org.mule.examples.springboot.soapwebservicesecurity;

import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.wss4j.common.crypto.Crypto;
import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.context.DefaultMessageContext;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Replays {@code parity/soap-webservice-security} fixtures. WS-Security headers hold timestamps, nonces and
 * signatures, so they cannot be stored verbatim. A fixture names the Mule client config in the
 * {@value #WSS_HEADER} request header, and the plain envelope from the fixture is secured with that WSS4J config
 * (the {@code SecureClient} / {@code service-clients.xml} settings of the Mule app) just before it is sent.
 */
class SoapWebserviceSecurityParityTest extends AbstractParityTest {

    static final String WSS_HEADER = "X-Parity-WSS";

    @Autowired
    private Crypto wssCrypto;

    @Override
    protected String fixtureDirectory() {
        return "parity/soap-webservice-security";
    }

    @Override
    protected void runCase(ParityFixture fixture) {
        ParityFixture.Request request = fixture.request();
        String profile = request.headers() == null ? null : request.headers().get(WSS_HEADER);
        if (profile == null) {
            super.runCase(fixture);
            return;
        }
        Map<String, String> headers = new LinkedHashMap<>(request.headers());
        headers.remove(WSS_HEADER);
        String secured = secure(request.body().asText(), interceptor(profile));
        ParityFixture.Request securedRequest = new ParityFixture.Request(request.method(), request.path(), headers,
                TextNode.valueOf(secured), null);
        super.runCase(new ParityFixture(fixture.name(), fixture.description(), fixture.source(), securedRequest,
                fixture.expected(), fixture.downstream(), fixture.expectedOutbound()));
    }

    private Wss4jSecurityInterceptor interceptor(String profile) {
        return switch (profile) {
            // SoapWebserviceSecurityIT#testUsernameTokenWrongPasswordError (WrongPasswordCallback)
            case "usernameTokenWrongPassword" -> SecurityClients.usernameToken("UsernameToken Timestamp", "wrongpassword");
            // SoapWebserviceSecurityIT#testUsernameTokenUnsignedError
            case "usernameTokenUnsigned" -> SecurityClients.usernameToken("UsernameToken Timestamp", "secret");
            // SoapWebserviceSecurityIT#testSamlTokenWrongIssuer (wrong-saml.properties)
            case "samlTokenWrongIssuer" -> SecurityClients.samlToken("www.example.com", wssCrypto);
            default -> SecurityClients.forType(profile, wssCrypto).flatMap(SecurityClients.Client::interceptor)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown WS-Security profile " + profile));
        };
    }

    private static String secure(String envelope, Wss4jSecurityInterceptor interceptor) {
        try {
            SaajSoapMessageFactory factory = new SaajSoapMessageFactory();
            factory.afterPropertiesSet();
            var message = factory.createWebServiceMessage(new ByteArrayInputStream(envelope.getBytes(StandardCharsets.UTF_8)));
            interceptor.afterPropertiesSet();
            interceptor.handleRequest(new DefaultMessageContext(message, factory));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            message.writeTo(out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not secure envelope", e);
        }
    }
}
