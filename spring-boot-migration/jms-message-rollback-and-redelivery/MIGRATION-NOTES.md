# Migration notes: `jms-message-rollback-and-redelivery`

- Original Mule app: [`../../jms-message-rollback-and-redelivery`](../../jms-message-rollback-and-redelivery)
- Mule flows: `jms-redelivery.xml` → `JMSRedeliver`
- Spring Boot module: `spring-boot-migration/jms-message-rollback-and-redelivery`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `spring:bean org.apache.activemq.RedeliveryPolicy` | `JmsConfig#redeliveryPolicyCustomizer` (`ActiveMQConnectionFactoryCustomizer`) | Values in `activemq.redelivery-policy.*` |
| `spring:bean ActiveMQConnectionFactory amqFactory` | `spring-boot-starter-activemq`, `spring.activemq.broker-url` | |
| `jms:activemq-connector maxRedelivery="5" persistentDelivery="true"` | `jms.max-redelivery` check in `RedeliveryListener`; `spring.jms.template.delivery-mode: persistent` | |
| `jms:inbound-endpoint queue="in"` + `jms:transaction action="ALWAYS_BEGIN"` | `@JmsListener` on `transactedListenerFactory` (`sessionTransacted=true`, concurrency 1) | |
| `choice` `JMSXDeliveryCount==5` | `if (deliveryCount == successDeliveryCount)` | |
| `test:component exceptionToThrow="org.exceptions.MyException"` | `throw new MyException(...)` | |
| `jms:outbound-endpoint topic="topic1"` + `ALWAYS_JOIN` | `JmsTemplate#convertAndSend(topic, ...)` from the listener thread, which joins the exposed listener session | Committed or rolled back together with the consume |
| `rollback-exception-strategy when causedBy(MyException)` | rethrow → container rolls the session back → ActiveMQ redelivers | |
| `catch-exception-strategy when !causedBy(MyException)` | catch, log, return normally → commit | |
| Mule `MessageRedeliveredException` | `MessageRedeliveredException` thrown by `RedeliveryListener` | |
| Broker DLQ after the policy is exhausted | ActiveMQ `ActiveMQ.DLQ` (no change) | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `brokerURL` | `spring.activemq.broker-url` (`${activemq.broker-url:...}`) | `failover:(tcp://localhost:61616)` |
| connector `maxRedelivery` | `jms.max-redelivery` | `5` |
| `RedeliveryPolicy.*` | `activemq.redelivery-policy.*` | 5 / 5000 / 2000 / false / 2 |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `redelivery/01-munit-message123` | `Message123`: deliveries 1-4 roll back, 5 commits; one `TextMessage` on `topic1`; gaps ≥5 s/2 s/2 s/2 s; nothing left on `in` or the DLQ | `munit` + `documented` (README steps 6-7) |
| `redelivery/02-json-sales-payload` | Same path for a JSON body, which is forwarded unchanged | `documented` |
| `max-redelivery-exceeded/01-readme-step8-no-maxredelivery` | `maxRedelivery=0`: delivery 1 rolls back, delivery 2 is committed after the max-redelivery error, nothing published | `documented` (README step 8) |
| `dead-letter/01-broker-redeliveries-exhausted` | Broker policy exhausted first: 4 rollbacks, then the message moves unchanged to `ActiveMQ.DLQ` | `documented` (ActiveMQ RedeliveryPolicy) |

## Behavioural deltas

See [README](README.md#behavioural-deltas): the broker redelivery policy is used instead of Spring Retry (by design), and the max-redelivery log message has no payload-type suffix.
