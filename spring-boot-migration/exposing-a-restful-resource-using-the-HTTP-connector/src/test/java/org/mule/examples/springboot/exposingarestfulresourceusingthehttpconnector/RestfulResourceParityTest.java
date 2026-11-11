package org.mule.examples.springboot.exposingarestfulresourceusingthehttpconnector;

import org.mule.examples.springboot.parity.AbstractParityTest;

/** Fixtures are stateful and run in file-name order against one in-memory store, like one running Mule app. */
class RestfulResourceParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/exposing-a-restful-resource-using-the-HTTP-connector";
    }
}
