package org.ordermgmt.fulfillment.service;

import org.ordermgmt.fulfillment.model.ShippingOrder;
import org.ordermgmt.fulfillment.model.ShippingOrderConfirmation;
import org.springframework.stereotype.Service;

/**
 * Replaces the {@code <component class="org.ordermgmt.FulfillmentImpl"/>} message processor:
 * it acknowledges the order to the caller and hands the order to the legacy fulfillment
 * dispatcher, which runs outside the request/response exchange exactly like the Mule
 * {@code <async>} scope.
 */
@Service
public class FulfillmentService {

    private final LegacyFulfillmentDispatcher dispatcher;

    public FulfillmentService(LegacyFulfillmentDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public ShippingOrderConfirmation putShippingOrder(ShippingOrder shippingOrder) {
        ShippingOrderConfirmation confirmation = new ShippingOrderConfirmation(shippingOrder, true);
        dispatcher.dispatch(shippingOrder);
        return confirmation;
    }
}
