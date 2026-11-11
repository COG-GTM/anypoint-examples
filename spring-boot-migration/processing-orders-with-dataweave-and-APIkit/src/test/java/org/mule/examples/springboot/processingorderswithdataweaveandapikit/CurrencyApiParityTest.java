package org.mule.examples.springboot.processingorderswithdataweaveandapikit;

import org.mule.examples.springboot.parity.AbstractParityTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class CurrencyApiParityTest extends AbstractParityTest {

    @DynamicPropertySource
    static void dirs(DynamicPropertyRegistry registry) {
        OrdersTestDirectories.register(registry);
    }

    @Override
    protected String fixtureDirectory() {
        return "parity/processing-orders-with-dataweave-and-APIkit";
    }
}
