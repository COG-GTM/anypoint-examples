# jms-message-rollback-and-redelivery (Spring Boot 3)

Spring Boot migration of the Mule 3.8 example [`jms-message-rollback-and-redelivery`](../../jms-message-rollback-and-redelivery).

`RedeliveryListener` consumes queue `in` in a transacted JMS session (`@JmsListener` on a session-transacted
`DefaultJmsListenerContainerFactory`). Deliveries 1-4 throw `MyException`, which rolls the session back so that
ActiveMQ redelivers the message (redelivery policy: initial delay 5 s, then 2 s, no exponential back-off, at most 5
redeliveries). Delivery 5 (`JMSXDeliveryCount == 5`) logs "Transaction without errors" and publishes the message to
topic `topic1` in the same transaction as the consume. Errors that are not caused by `MyException` (for example the
connector-level `maxRedelivery` check) are logged and the transaction is committed, like the Mule
`catch-exception-strategy`.

## Run

```bash
docker run -d -p 61616:61616 -p 8161:8161 apache/activemq-classic:5.18.3
cd spring-boot-migration && mvn -pl jms-message-rollback-and-redelivery -am spring-boot:run
```

Send any message to queue `in` from <http://localhost:8161/admin/send.jsp> (admin/admin). After about 11 s, the
"Messages Enqueued" count for `topic1` goes up by 1.

| Property | Default | Mule equivalent |
|---|---|---|
| `activemq.broker-url` | `failover:(tcp://localhost:61616)` | `amqFactory` `brokerURL` |
| `jms.in-queue` / `jms.topic` | `in` / `topic1` | inbound `queue="in"`, outbound `topic="topic1"` |
| `jms.max-redelivery` | `5` (`-1` disables the check) | `jms:activemq-connector maxRedelivery="5"` |
| `jms.success-delivery-count` | `5` | `choice` `JMSXDeliveryCount==5` |
| `activemq.redelivery-policy.*` | `5` / `5000` / `2000` / `false` / `2` | `redeliveryPolicy` Spring bean |

## Parity tests

All three test classes start ActiveMQ Classic with Testcontainers, send each fixture's message to `in`, and then
check four things: the sequence of `JMSXDeliveryCount` values and outcomes the listener saw (`ROLLED_BACK`,
`COMMITTED`, `COMMITTED_AFTER_ERROR`), the minimum delay between redeliveries, what reached `topic1` (through a live
topic subscriber), and what is left on `in` and `ActiveMQ.DLQ`.

| Test class | Fixtures | Config |
|---|---|---|
| `RedeliveryParityTest` | `redelivery/` (2) | as shipped |
| `MaxRedeliveryExceededParityTest` | `max-redelivery-exceeded/` (1) | `jms.max-redelivery=0` (README step 8) |
| `DeadLetterParityTest` | `dead-letter/` (1) | broker gives up after 3 redeliveries → `ActiveMQ.DLQ` |

```bash
cd spring-boot-migration && mvn -B -pl jms-message-rollback-and-redelivery -am verify
```

## Behavioural deltas

- **Redelivery uses the broker's redelivery policy plus its DLQ, not in-process Spring Retry.** The ticket mapped
  "rollback/redelivery → Spring retry + DLQ". The Mule flow depends on `JMSXDeliveryCount` and on the ActiveMQ
  `RedeliveryPolicy` delays, and a Spring `RetryTemplate` retries inside one delivery, so it would change both. The
  ActiveMQ redelivery policy and `ActiveMQ.DLQ` keep the observable behaviour: 5 deliveries, 5 s and then 2 s gaps,
  and dead-lettering once the policy is exhausted (`DeadLetterParityTest`).
- **Mule `maxRedelivery` check.** Mule's JMS connector throws `MessageRedeliveredException` when the redelivery count
  exceeds `maxRedelivery`. Spring has no equivalent, so `RedeliveryListener` makes the same check itself. The log
  message follows Mule's wording but leaves out the payload type suffix.
