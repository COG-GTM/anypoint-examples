package org.mule.examples.springboot.additionusingjavascripttransformer;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

class AdditionUsingJavascriptTransformerParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/addition-using-javascript-transformer";
    }
}
