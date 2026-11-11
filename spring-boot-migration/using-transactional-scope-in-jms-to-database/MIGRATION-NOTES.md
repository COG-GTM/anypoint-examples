# Migration notes: `using-transactional-scope-in-jms-to-database`

- Original Mule app: [`../../using-transactional-scope-in-jms-to-database`](../../using-transactional-scope-in-jms-to-database)
- Mule flows: `transactions.xml` → `transactionsFlow1`
- Spring Boot module: `spring-boot-migration/using-transactional-scope-in-jms-to-database`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `jms:activemq-connector maxRedelivery="2"` | `spring-boot-starter-activemq`; `jms.max-redelivery` check in `OrderListener` | |
| `db:generic-config url="${jdbc.url}"` (MySQL) | `spring.datasource.url: ${jdbc.url}`, `mysql-connector-j` | |
| `jms:inbound-endpoint queue="in"` (`processingStrategy="synchronous"`) | `@JmsListener` on `transactedListenerFactory` (session-transacted, concurrency 1) | |
| `mulexml:xml-to-object-transformer` (XStream, alias `order`) | Jackson `XmlMapper` → `Order` record (`@JacksonXmlRootElement("order")`) | |
| `logger message="XXXXXXX order: #[payload]"` | `log.info("XXXXXXX order: {}", order)` | `Order#toString` kept |
| `ee:multi-transactional action="ALWAYS_BEGIN"` | `OrderTransactionalScope#process` `@Transactional(rollbackFor = Exception.class)` + transacted listener session | Best-effort 1PC across JDBC and JMS, like Mule's multi-transaction (not XA) |
| `db:insert` parameterized query | `JdbcClient` `insert into orders(item_id,item_units,customer_id) values (?, ?, ?)` | |
| `component class="org.mule.examples.TestComponent"` | `throw new MyException("exception customized")` (switch `orders.throw-after-insert`) | |
| `jms:outbound-endpoint queue="out"` + `JOIN_IF_POSSIBLE` | `JmsTemplate#convertAndSend("out", Integer)`, which joins the exposed listener session | Update count → `ObjectMessage`, like Mule |
| Rollback / redelivery by ActiveMQ | Exception rethrown → session rollback → ActiveMQ redelivery (default policy) | |
| `MessageRedeliveredException` | `MessageRedeliveredException`, logged by `OrderListener`; message consumed | |
| `order.sql` | `schema.sql` (`CREATE TABLE IF NOT EXISTS`), opt-in via `orders.init-schema` | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `jdbc.url` | `spring.datasource.url` (`${jdbc.url:...}`) | `jdbc:mysql://localhost:3306/test` |
| connector `maxRedelivery` | `jms.max-redelivery` | `2` |
| (broker URL, connector default) | `spring.activemq.broker-url` (`${activemq.broker-url:...}`) | `tcp://localhost:61616` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `rollback/01-readme-order-rolled-back` | README/IT order: 3 rolled-back deliveries (3 inserts executed and undone), 4th delivery exceeds maxRedelivery=2 and is consumed; `orders` empty (IT `count(*)=0`); nothing on `out`/`in`/DLQ | `documented` (README steps 5-6) + IT |
| `rollback/02-other-order-rolled-back` | Same outcome for different order values | `documented` |
| `commit-path/01-scope-commits-without-error` | Without the throwing step: one row committed and `ObjectMessage` `1` on `out`, in one transaction | `documented` (flow XML) |

## Behavioural deltas

See [README](README.md#behavioural-deltas): the `orders.throw-after-insert` test switch, the opt-in schema init, and the in-listener max-redelivery check.
