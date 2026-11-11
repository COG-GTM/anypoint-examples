package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import org.springframework.http.MediaType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Element;

/**
 * {@code samsungWebServiceClient}: cxf:jaxws-client (SamsungServiceService, operation purchase) + http:request POST
 * {@code samsung/orders}. The {@code http.status=200} message filter becomes the status check.
 */
@Component
public class SamsungClient {

    public static final String SAMSUNG_NS = "http://samsung.se.mulesoft.com/";

    public record OrderResponse(String id, String price, String result) {
    }

    private final RestClient rest;

    private final Environment env;

    /** The base URL is resolved per call so it can point at this application's own (random) port. */
    public SamsungClient(RestClient.Builder builder, Environment env) {
        this.rest = builder.build();
        this.env = env;
    }

    public OrderResponse purchase(String name, int quantity) {
        StringBuilder b = new StringBuilder("<soap:Envelope xmlns:soap=\"" + OrderSoap.SOAP_NS + "\"><soap:Body>")
                .append("<ns2:purchase xmlns:ns2=\"" + SAMSUNG_NS + "\"><orderRequest>");
        OrderSoap.el(b, "name", name);
        OrderSoap.el(b, "quantity", Integer.toString(quantity));
        b.append("</orderRequest></ns2:purchase></soap:Body></soap:Envelope>");
        String[] res = rest.post().uri(env.getRequiredProperty("samsung.base-url") + "/samsung/orders").contentType(MediaType.TEXT_XML).header("SOAPAction", "\"\"")
                .body(b.toString())
                .exchange((req, r) -> new String[] {String.valueOf(r.getStatusCode().value()),
                        new String(r.getBody().readAllBytes())});
        if (!"200".equals(res[0])) {
            throw new IllegalStateException("Message has been rejected by filter: http.status=" + res[0]);
        }
        Element response = OrderSoap.child(
                OrderSoap.first(OrderSoap.parseXml(res[1]).getDocumentElement(), "purchaseResponse"), "orderResponse");
        if (response == null) {
            throw new IllegalStateException("No purchaseResponse/orderResponse in Samsung reply");
        }
        return new OrderResponse(OrderSoap.text(response, "id"), OrderSoap.text(response, "price"),
                OrderSoap.text(response, "result"));
    }
}
