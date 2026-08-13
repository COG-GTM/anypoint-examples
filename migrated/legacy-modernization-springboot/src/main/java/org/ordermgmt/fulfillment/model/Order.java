package org.ordermgmt.fulfillment.model;

import java.util.List;

/**
 * Order, mirroring {@code org.ordermgmt.Order} of the Mule application.
 */
public record Order(List<OrderItem> orderItemList) {

    public Order {
        orderItemList = orderItemList == null ? List.of() : List.copyOf(orderItemList);
    }
}
