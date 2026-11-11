package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Database side effects of inhouseOrder (insert into orders) and the asynchronous auditService (insert into
 * order_audits with the order total), running against the bundled Samsung and price services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:derby:memory:sideEffectsDB;create=true")
class OrderSideEffectsTest {

    @Autowired
    TestRestTemplate http;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void inhouseItemsAreStoredAndOrderTotalIsAudited() throws Exception {
        assertThat(http.getForObject("/populate", String.class)).isEqualTo("db populated");
        String soap = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ord=\"http://orders.se.mulesoft.com/\"><soapenv:Body><ord:processOrder><order>"
                + "<orderId>77</orderId><customer><address>a</address><firstName>f</firstName><lastName>l</lastName>"
                + "</customer><orderItems>"
                + "<item><manufacturer>Samsung</manufacturer><name>s-1</name><productId>AX02</productId><quantity>2</quantity></item>"
                + "<item><manufacturer>Philips</manufacturer><name>P3123</name><productId>23</productId><quantity>10</quantity></item>"
                + "</orderItems></order></ord:processOrder></soapenv:Body></soapenv:Envelope>";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        String reply = http.postForObject("/orders", new HttpEntity<>(soap, headers), String.class);
        assertThat(reply).contains("<totalPrice>5100.0</totalPrice>").contains("<totalPrice>1590.0</totalPrice>");

        List<Map<String, Object>> orders = jdbc.queryForList(
                "select product_id, name, manufacturer, quantity, price from orders");
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0)).containsEntry("PRODUCT_ID", "23").containsEntry("MANUFACTURER", "Philips")
                .containsEntry("QUANTITY", 10).containsEntry("PRICE", 159);

        List<Map<String, Object>> audits = List.of();
        for (int i = 0; i < 50 && audits.isEmpty(); i++) {
            audits = jdbc.queryForList("select order_id, total_value from order_audits");
            Thread.sleep(100);
        }
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0)).containsEntry("ORDER_ID", "77").containsEntry("TOTAL_VALUE", 6690);

        String empty = soap.replaceAll("<orderId>77</orderId>", "<orderId>78</orderId>")
                .replaceAll("<orderItems>.*</orderItems>", "<orderItems/>");
        assertThat(http.postForObject("/orders", new HttpEntity<>(empty, headers), String.class))
                .contains("<orderId>78</orderId>");
        List<Map<String, Object>> emptyAudit = List.of();
        for (int i = 0; i < 50 && emptyAudit.isEmpty(); i++) {
            emptyAudit = jdbc.queryForList("select total_value from order_audits where order_id = '78'");
            Thread.sleep(100);
        }
        assertThat(emptyAudit).hasSize(1);
        assertThat(emptyAudit.get(0)).containsEntry("TOTAL_VALUE", 0);
        assertThat(jdbc.queryForObject("select count(*) from orders", Integer.class)).isEqualTo(1);
    }
}
