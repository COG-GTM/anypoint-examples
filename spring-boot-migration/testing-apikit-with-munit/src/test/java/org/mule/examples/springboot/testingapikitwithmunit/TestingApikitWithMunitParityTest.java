package org.mule.examples.springboot.testingapikitwithmunit;

import org.mule.examples.springboot.parity.AbstractParityTest;

class TestingApikitWithMunitParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/testing-apikit-with-munit";
    }
}
