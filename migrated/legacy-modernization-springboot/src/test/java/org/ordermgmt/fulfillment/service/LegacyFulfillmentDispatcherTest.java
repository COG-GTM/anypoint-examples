package org.ordermgmt.fulfillment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ordermgmt.fulfillment.SoapFixtures;
import org.ordermgmt.fulfillment.legacy.LegacyFulfillmentGateway;

class LegacyFulfillmentDispatcherTest {

    private final LegacyFulfillmentGateway gateway = mock(LegacyFulfillmentGateway.class);
    private final LegacyFulfillmentDispatcher dispatcher =
            new LegacyFulfillmentDispatcher(new ShippingOrderCsvTransformer(), gateway);

    @Test
    void sendsTheCsvRenderingOfTheOrderToTheLegacySystem() {
        dispatcher.dispatch(SoapFixtures.sampleShippingOrder());

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(gateway).send(payload.capture());
        assertThat(payload.getValue()).startsWith("MSKU,QTY,").contains("77 Geary St Level 4");
    }

    @Test
    void swallowsLegacySystemFailuresJustLikeTheMuleAsyncScope() {
        doThrow(new IllegalStateException("legacy system down")).when(gateway).send(anyString());

        assertThatCode(() -> dispatcher.dispatch(SoapFixtures.sampleShippingOrder())).doesNotThrowAnyException();
    }
}
