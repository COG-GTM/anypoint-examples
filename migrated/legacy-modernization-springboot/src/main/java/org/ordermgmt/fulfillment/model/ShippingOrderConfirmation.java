package org.ordermgmt.fulfillment.model;

/**
 * Confirmation returned to the caller, mirroring {@code org.ordermgmt.ShippingOrderConfirmation}
 * of the Mule application.
 */
public record ShippingOrderConfirmation(ShippingOrder shippingOrder, boolean orderReceivedStatus) {
}
