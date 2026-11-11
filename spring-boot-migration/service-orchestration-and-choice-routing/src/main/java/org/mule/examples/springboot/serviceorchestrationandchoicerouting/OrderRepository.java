package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Generic_Database_Configuration (embedded Derby): databaseInitialisation DDL and the two db:insert operations. */
@Repository
public class OrderRepository {

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createTables() {
        jdbc.execute("CREATE TABLE orders (i int generated always as identity, product_id varchar(256), "
                + "name varchar(256), manufacturer varchar(256), quantity integer, price integer)");
        jdbc.execute("CREATE TABLE order_audits (i int generated always as identity, order_id varchar(256), "
                + "total_value integer)");
    }

    public void insertOrder(Model.OrderItem item, int price) {
        jdbc.update("insert into orders (product_id, name, manufacturer, quantity, price) values (?, ?, ?, ?, ?)",
                item.productId, item.name, item.manufacturer, item.quantity, price);
    }

    public void insertAudit(String orderId, float totalValue) {
        jdbc.update("insert into order_audits values(default, ?, ?)", orderId, (int) totalValue);
    }
}
