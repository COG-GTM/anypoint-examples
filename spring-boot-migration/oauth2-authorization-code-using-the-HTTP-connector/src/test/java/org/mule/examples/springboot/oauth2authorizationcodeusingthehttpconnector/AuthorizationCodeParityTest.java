package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import org.junit.jupiter.api.BeforeEach;
import org.mule.examples.springboot.parity.AbstractWireMockParityTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Fixture-driven parity for the authorization-code client. The fixtures are one grant flow in file
 * order: redirects (01, 03, 04), failures before any token (02, 05, 06, 07), the successful code
 * exchange (08) and the API calls that use the stored token (09, 10). Mule keeps the token in
 * memory too, so the store is emptied once before the run.
 */
class AuthorizationCodeParityTest extends AbstractWireMockParityTest {

    @Autowired
    BoxAuthorizationCodeClient oauth;

    @BeforeEach
    void startWithoutTokens() {
        oauth.forgetTokens();
    }

    @Override
    protected String fixtureDirectory() {
        return "parity/oauth2-authorization-code-using-the-HTTP-connector";
    }
}
