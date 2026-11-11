package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code orderService} (POST /orders SOAP), {@code databaseInitialisation} (GET /populate) and the three AJAX
 * channels of mule-config.xml ({@code /orders/request}, {@code /orders/soap}, {@code /orders/manufacturers}) as
 * plain HTTP endpoints.
 */
@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static final MediaType TEXT_XML_UTF8 = MediaType.parseMediaType("text/xml;charset=UTF-8");

    private final OrderOrchestrationFlowConfig.OrderServiceGateway gateway;
    private final OrderRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderController(OrderOrchestrationFlowConfig.OrderServiceGateway gateway, OrderRepository repository) {
        this.gateway = gateway;
        this.repository = repository;
    }

    @PostMapping(path = "/orders", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> processOrder(@RequestBody String soap) {
        try {
            return ResponseEntity.ok().contentType(TEXT_XML_UTF8).body(OrderSoap.renderResponse(process(OrderSoap.parseRequest(soap))));
        } catch (RuntimeException e) {
            log.info("Error occurred: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(TEXT_XML_UTF8)
                    .body(OrderSoap.fault(String.valueOf(e.getMessage())));
        }
    }

    @GetMapping(path = "/populate", produces = MediaType.TEXT_PLAIN_VALUE)
    public String populate() {
        try {
            repository.createTables();
            return "db populated";
        } catch (RuntimeException e) {
            return "table already populated";
        }
    }

    /** {@code orderRequest}: JSON order → SOAP orderService → JSON {orderId, customer, orderItems}. */
    @PostMapping(path = "/orders/request", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode orderRequest(@RequestBody String json) throws Exception {
        JsonNode in = mapper.readTree(json);
        JsonNode c = in.path("customer");
        List<Model.OrderItem> items = new ArrayList<>();
        for (JsonNode wrapper : in.path("orderItems")) {
            JsonNode i = wrapper.path("item");
            items.add(new Model.OrderItem(i.path("productId").asText(), i.path("name").asText(),
                    i.path("manufacturer").asText(), Integer.parseInt(i.path("quantity").asText().trim())));
        }
        Model.Order order = process(new Model.Order(in.path("orderId").asText(),
                new Model.OrderPerson(c.path("address").asText(), c.path("firstName").asText(),
                        c.path("lastName").asText()), items));
        ObjectNode out = mapper.createObjectNode().put("orderId", order.orderId());
        out.putObject("customer").put("address", order.customer().address())
                .put("firstName", order.customer().firstName()).put("lastName", order.customer().lastName());
        ArrayNode outItems = out.putArray("orderItems");
        for (Model.OrderItem item : order.orderItems()) {
            ObjectNode o = outItems.addObject().putObject("item").put("manufacturer", item.manufacturer)
                    .put("name", item.name).put("productId", item.productId);
            ObjectNode r = o.putObject("purchaseReceipt");
            if (item.purchaseReceipt.id() != null) {
                r.put("id", item.purchaseReceipt.id());
            }
            r.put("status", item.purchaseReceipt.status().name()).put("totalPrice", item.purchaseReceipt.totalPrice());
            o.put("quantity", Integer.toString(item.quantity));
        }
        return out;
    }

    /** {@code orderProxy}: the raw SOAP envelope is passed to orderService and its reply returned as a string. */
    @PostMapping(path = "/orders/soap", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> orderProxy(@RequestBody String soap) {
        return processOrder(soap);
    }

    @GetMapping(path = "/orders/manufacturers", produces = MediaType.APPLICATION_JSON_VALUE)
    public String manufacturers() {
        return "[\"Samsung\",\"Philips\",\"Sony\"]";
    }

    private Model.Order process(Model.Order order) {
        return order.orderItems().isEmpty() ? order : gateway.processOrder(order);
    }
}
