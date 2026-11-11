package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import java.util.List;

/** Ports of com.mulesoft.se.orders (Order, OrderPerson, OrderItem, PurchaseReceipt, Status). */
public final class Model {

    private Model() {
    }

    public enum Status { ACCEPTED, REJECTED }

    public record OrderPerson(String address, String firstName, String lastName) {
    }

    /** {@code id} is null for receipts without one (in-house and rejected), so the element is omitted. */
    public record PurchaseReceipt(String id, Status status, float totalPrice) {
    }

    public static class OrderItem {
        public String productId;
        public String name;
        public String manufacturer;
        public int quantity;
        public PurchaseReceipt purchaseReceipt;

        public OrderItem(String productId, String name, String manufacturer, int quantity) {
            this.productId = productId;
            this.name = name;
            this.manufacturer = manufacturer;
            this.quantity = quantity;
        }
    }

    public record Order(String orderId, OrderPerson customer, List<OrderItem> orderItems) {
    }
}
