package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryAttemptsTest {

    @Test
    void keepsOnlyTheMostRecentDeliveries() {
        DeliveryAttempts attempts = new DeliveryAttempts();
        for (int i = 1; i <= DeliveryAttempts.CAPACITY + 5; i++) {
            attempts.record("id-" + i, 1, DeliveryAttempts.Outcome.COMMITTED);
        }
        assertThat(attempts.all()).hasSize(DeliveryAttempts.CAPACITY);
        assertThat(attempts.all().get(0).messageId()).isEqualTo("id-6");
        assertThat(attempts.all().get(DeliveryAttempts.CAPACITY - 1).messageId()).isEqualTo("id-" + (DeliveryAttempts.CAPACITY + 5));
    }
}
