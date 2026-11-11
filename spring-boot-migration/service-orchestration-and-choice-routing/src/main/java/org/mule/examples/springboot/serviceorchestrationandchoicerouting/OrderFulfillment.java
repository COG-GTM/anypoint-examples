package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The {@code samsungOrder} and {@code inhouseOrder} flows. */
@Service
public class OrderFulfillment {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillment.class);

    private final SamsungClient samsung;
    private final PriceClient prices;
    private final OrderRepository repository;

    public OrderFulfillment(SamsungClient samsung, PriceClient prices, OrderRepository repository) {
        this.samsung = samsung;
        this.prices = prices;
        this.repository = repository;
    }

    /** OrderItem → OrderRequest{name, quantity} → purchase; any failure → REJECTED receipt with totalPrice 0. */
    public Model.PurchaseReceipt samsungOrder(Model.OrderItem item) {
        try {
            SamsungClient.OrderResponse r = samsung.purchase(item.name, item.quantity);
            return new Model.PurchaseReceipt(r.id(), Model.Status.valueOf(r.result()), Float.parseFloat(r.price()));
        } catch (RuntimeException e) {
            log.info("Samsung order rejected: {}", e.getMessage());
            return new Model.PurchaseReceipt(null, Model.Status.REJECTED, 0);
        }
    }

    /** Cached price lookup, insert into orders, ACCEPTED receipt with totalPrice = price * quantity. */
    @Transactional
    public Model.PurchaseReceipt inhouseOrder(Model.OrderItem item) {
        int price = Integer.parseInt(prices.price(item.productId).trim());
        repository.insertOrder(item, price);
        return new Model.PurchaseReceipt(null, Model.Status.ACCEPTED, Float.valueOf(price * item.quantity));
    }

    /** sessionVars.totalValue: the sum of every receipt's totalPrice. */
    public static float totalValue(Model.Order order) {
        float total = 0;
        for (Model.OrderItem item : order.orderItems()) {
            if (item.purchaseReceipt != null) {
                total += item.purchaseReceipt.totalPrice();
            }
        }
        return total;
    }
}
