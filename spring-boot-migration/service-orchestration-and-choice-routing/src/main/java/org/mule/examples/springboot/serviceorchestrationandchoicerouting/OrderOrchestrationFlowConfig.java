package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;

/**
 * {@code orderService}: foreach orderItems → choice (manufacturer == 'Samsung' → samsungOrder, otherwise
 * inhouseOrder) enriching each item's purchaseReceipt, then a one-way {@code audit} hand-off. The foreach is a
 * splitter + router + aggregator; the one-way VM endpoint is a wire tap onto an executor channel.
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class OrderOrchestrationFlowConfig {

    public static final String INPUT = "orderServiceFlow.input";
    public static final String AUDIT = "audit";
    static final String ORDER = "order";
    public static final String SAMSUNG = "Samsung";

    private static final Logger log = LoggerFactory.getLogger(OrderOrchestrationFlowConfig.class);

    @MessagingGateway(defaultRequestChannel = INPUT)
    public interface OrderServiceGateway {
        Model.Order processOrder(Model.Order order);
    }

    /** The choice: Samsung items go to samsungOrder, every other manufacturer to inhouseOrder (case-sensitive). */
    public static boolean isSamsung(Model.OrderItem item) {
        return SAMSUNG.equals(item.manufacturer);
    }

    @Bean
    public IntegrationFlow orderServiceFlow(OrderFulfillment fulfillment) {
        return f -> f
                .enrichHeaders(h -> h.headerFunction(ORDER, m -> m.getPayload()))
                .split(Model.Order.class, Model.Order::orderItems)
                .<Model.OrderItem, Boolean>route(OrderOrchestrationFlowConfig::isSamsung, r -> r
                        .subFlowMapping(true, sf -> sf.<Model.OrderItem>handle((item, h) -> {
                            item.purchaseReceipt = fulfillment.samsungOrder(item);
                            return item;
                        }))
                        .subFlowMapping(false, sf -> sf.<Model.OrderItem>handle((item, h) -> {
                            item.purchaseReceipt = fulfillment.inhouseOrder(item);
                            return item;
                        })))
                .aggregate()
                .handle((p, h) -> h.get(ORDER, Model.Order.class))
                .wireTap(AUDIT);
    }

    @Bean(name = AUDIT)
    public MessageChannel auditChannel() {
        return MessageChannels.executor(Executors.newSingleThreadExecutor()).getObject();
    }

    /** {@code auditService}: insert into order_audits (orderId, totalValue). */
    @Bean
    public IntegrationFlow auditServiceFlow(OrderRepository repository) {
        return IntegrationFlow.from(AUDIT)
                .<Model.Order>handle((order, h) -> {
                    try {
                        repository.insertAudit(order.orderId(), OrderFulfillment.totalValue(order));
                    } catch (RuntimeException e) {
                        log.info("Error occurred: {}", e.getMessage());
                    }
                    return null;
                })
                .get();
    }
}
