# Migration notes: `sending-json-data-to-a-jms-queue`

- Original Mule app: [`../../sending-json-data-to-a-jms-queue`](../../sending-json-data-to-a-jms-queue)
- Mule flows: `json-to-jms.xml` → `json-to-jmsFlow`
- Spring Boot module: `spring-boot-migration/sending-json-data-to-a-jms-queue`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener-config` host `0.0.0.0` port `${http.port}` | embedded Tomcat, `server.port: ${http.port:8081}` | |
| `http:listener path="sales" allowedMethods="POST"` (`json-to-jmsFlow`) | `SalesController#sales` (`@PostMapping("/sales")`) | Spring MVC returns 405 for other methods |
| `byte-array-to-string-transformer` | `new String(body, charset)` in `SalesController` | Charset taken from `Content-Type`; UTF-8 by default, like Mule |
| `jms:activemq-connector brokerURL="tcp://localhost:61616"` | `spring-boot-starter-activemq`, `spring.activemq.broker-url` | |
| `jms:outbound-endpoint queue="sales"` (one-way) | `JmsTemplate#convertAndSend("sales", String)` | String → `TextMessage`, same as Mule's `JMSMessageUtils` |
| `logger level="INFO"` | SLF4J `log.info` | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `brokerURL` (connector attribute) | `spring.activemq.broker-url` (`${activemq.broker-url:...}`) | `tcp://localhost:61616` |
| `queue="sales"` | `jms.queue` (`${jms.sales-queue:sales}`) | `sales` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-munit-message-json` | The IT's `message.json`: 200, payload echoed, one `TextMessage` with the same text on `sales` | `munit` (`SendingJsonDataToJMSQueueIT`) |
| `02-readme-rest-console` | README body (not valid JSON) is forwarded verbatim, never parsed | `documented` (README step 6) |
| `03-valid-json-utf8` | Valid JSON with non-ASCII characters keeps its content | `documented` (flow XML) |
| `04-get-not-allowed` | `allowedMethods=POST`: 405 and no message sent | `documented` (flow XML) |
| `05-unknown-path` | Only `/sales` is bound: 404 and no message sent | `documented` (flow XML) |

## Behavioural deltas

See [README](README.md#behavioural-deltas): error body format for 405/404 only.
