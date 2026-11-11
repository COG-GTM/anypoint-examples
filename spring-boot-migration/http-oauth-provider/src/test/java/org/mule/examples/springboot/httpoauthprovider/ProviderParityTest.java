package org.mule.examples.springboot.httpoauthprovider;

import org.mule.examples.springboot.parity.AbstractParityTest;

/** Static fixtures of the provider: login page, login → code redirect, redirectFlow, rejected resource calls. */
class ProviderParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/http-oauth-provider";
    }
}
