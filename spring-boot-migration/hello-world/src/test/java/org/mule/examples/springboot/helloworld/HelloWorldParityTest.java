package org.mule.examples.springboot.helloworld;

import org.mule.examples.springboot.parity.AbstractParityTest;

class HelloWorldParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/hello-world";
    }
}
