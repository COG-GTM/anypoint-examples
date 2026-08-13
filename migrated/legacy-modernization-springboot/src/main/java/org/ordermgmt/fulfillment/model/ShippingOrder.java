package org.ordermgmt.fulfillment.model;

/**
 * Shipping order, mirroring {@code org.ordermgmt.ShippingOrder} of the Mule application.
 */
public record ShippingOrder(
        String shippingId,
        Address billingAddress,
        Address shippingAddress,
        Order order) {
}
