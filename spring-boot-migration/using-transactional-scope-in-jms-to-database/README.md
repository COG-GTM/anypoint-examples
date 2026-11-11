# using-transactional-scope-in-jms-to-database (Spring Boot 3)

Spring Boot migration of the Mule 3.8 example
[`using-transactional-scope-in-jms-to-database`](../../using-transactional-scope-in-jms-to-database).

`OrderListener` consumes XML orders from ActiveMQ queue `in` in a transacted JMS session and binds each one to `Order`
(`<order><itemId/><itemUnits/><customerId/></order>`). It then calls `OrderTransactionalScope#process`, which is
`@Transactional(rollbackFor = Exception.class)` on a `DataSourceTransactionManager`. That method inserts the row into
MySQL `orders`, runs the `TestComponent` step (throws `MyException`, as in Mule), and would send the result to queue
`out`. The send to `out` joins the listener's JMS session.

The exception rolls back the insert (JDBC transaction) and the `out` send plus the consume (JMS session), so ActiveMQ
redelivers the message. On the 4th delivery the message has been redelivered 3 times, which exceeds
`maxRedelivery=2`. The listener then logs the Mule `MessageRedeliveredException` message and consumes the message
without running the scope. `orders` stays empty.

The JMS and JDBC transactions are coordinated best-effort 1PC (JDBC commits first, then the JMS session), not
through XA/JTA. That is also how Mule's `ee:multi-transactional` behaves, and it is the "chained transaction manager"
pattern without the deprecated `ChainedTransactionManager` class.

## Run

```bash
docker run -d -p 61616:61616 apache/activemq-classic:5.18.3
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=<choose one> -e MYSQL_DATABASE=test mysql:5.7
cd spring-boot-migration && mvn -pl using-transactional-scope-in-jms-to-database -am spring-boot:run \
  -Dspring-boot.run.arguments="--jdbc.url=jdbc:mysql://localhost:3306/test?user=root&password=<pwd>&useSSL=false&allowPublicKeyRetrieval=true --orders.init-schema=always"
```

Send the README's `<order>` XML to queue `in` (ActiveMQ console, `send.jsp`). The log shows three rollbacks and then
the max-redelivery error, and `select count(*) from orders` stays `0`.

| Property | Default | Mule equivalent |
|---|---|---|
| `activemq.broker-url` | `tcp://localhost:61616` | `jms:activemq-connector` default |
| `jdbc.url` (+ optional `jdbc.username` / `jdbc.password`) | `jdbc:mysql://localhost:3306/test` | `db:generic-config url="${jdbc.url}"` |
| `orders.init-schema` | `never` | (none: Mule required running `order.sql` by hand) |
| `jms.in-queue` / `jms.out-queue` | `in` / `out` | inbound / outbound `queue` |
| `jms.max-redelivery` | `2` | `jms:activemq-connector maxRedelivery="2"` |
| `orders.throw-after-insert` | `true` | `org.mule.examples.TestComponent` (always throws) |

## Parity tests

Both test classes start ActiveMQ Classic and MySQL 5.7 with Testcontainers. `jdbc.url` carries the credentials, as in
the Mule IT. Each test sends the fixture's XML to `in` and then checks:

- the sequence of `JMSXDeliveryCount` values and outcomes;
- the committed rows of `orders`;
- how many inserts actually ran, using the delta of `orders` `AUTO_INCREMENT`, which InnoDB does not roll back. This
  shows that the inserts happened and were rolled back, rather than skipped;
- the messages on `out`, `in` and `ActiveMQ.DLQ`.

| Test class | Fixtures | Config |
|---|---|---|
| `RollbackParityTest` | `rollback/` (2) | as shipped |
| `CommitPathParityTest` | `commit-path/` (1) | `orders.throw-after-insert=false` |

```bash
cd spring-boot-migration && mvn -B -pl using-transactional-scope-in-jms-to-database -am verify
```

## Behavioural deltas

- **`orders.throw-after-insert` switch (new).** The Mule app always throws after the insert, and so does the default
  config here. The switch exists so that `CommitPathParityTest` can prove the commit side of the same transaction
  (row committed and `ObjectMessage(1)` on `out` together). Leave it at `true` to match Mule.
- **Optional schema init.** `orders.init-schema=always` runs an idempotent copy of `order.sql`. The default is
  `never`, as in Mule.
- **Max-redelivery handling.** Spring has no `MessageRedeliveredException`, so `OrderListener` makes the same
  redelivery-count check itself, logs Mule's message at ERROR, and consumes the message.
