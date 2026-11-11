package org.mule.examples.springboot.cachescopewithfibonacci;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

class CacheScopeWithFibonacciParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/cache-scope-with-fibonacci";
    }
}
