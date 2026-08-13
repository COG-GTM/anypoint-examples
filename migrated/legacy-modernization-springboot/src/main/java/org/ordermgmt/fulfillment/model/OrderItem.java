package org.ordermgmt.fulfillment.model;

/**
 * Single line of an order, mirroring {@code org.ordermgmt.OrderItem} of the Mule application.
 */
public record OrderItem(String merchantSKU, int quantity) {
}
