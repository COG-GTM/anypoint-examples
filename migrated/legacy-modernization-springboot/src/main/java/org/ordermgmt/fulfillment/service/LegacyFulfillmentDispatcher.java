package org.ordermgmt.fulfillment.service;

import org.ordermgmt.fulfillment.legacy.LegacyFulfillmentGateway;
import org.ordermgmt.fulfillment.model.ShippingOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Replaces the Mule {@code <async>} scope "Async - Legacy Fulfillment Service": it transforms the
 * shipping order to CSV and pushes it to the legacy system off the request thread. As in Mule, a
 * failure here never reaches the SOAP caller — the caller has already been acknowledged.
 */
@Service
public class LegacyFulfillmentDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LegacyFulfillmentDispatcher.class);

    private final ShippingOrderCsvTransformer transformer;
    private final LegacyFulfillmentGateway gateway;

    public LegacyFulfillmentDispatcher(ShippingOrderCsvTransformer transformer, LegacyFulfillmentGateway gateway) {
        this.transformer = transformer;
        this.gateway = gateway;
    }

    @Async("legacyFulfillmentExecutor")
    public void dispatch(ShippingOrder shippingOrder) {
        try {
            gateway.send(transformer.toCsv(shippingOrder));
        } catch (RuntimeException e) {
            log.error("Unable to deliver shipping order {} to the legacy fulfillment system",
                    shippingOrder == null ? null : shippingOrder.shippingId(), e);
        }
    }
}
