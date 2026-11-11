package org.mule.examples.springboot.httprequestresponsewithlogger;

import org.mule.examples.springboot.parity.AbstractParityTest;

class EchoParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/http-request-response-with-logger";
    }
}
