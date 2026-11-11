# Migration notes: `sending-json-data-to-a-amqp-queue`

- Original Mule app: [`../../sending-json-data-to-a-amqp-queue`](../../sending-json-data-to-a-amqp-queue)
- Mule flows: `json-to-rabbitmq.xml` → `json-to-rabbitmqFlow`
- Spring Boot module: `spring-boot-migration/sending-json-data-to-a-amqp-queue`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/" allowedMethods="POST"` | `SalesController#send` (`@PostMapping("/")`) | |
| `logger message="payload to be sent: #[new String(payload)]"` | `log.info("payload to be sent: {}", ...)` | |
| `amqp:connector` (defaults) | `spring-boot-starter-amqp`, `spring.rabbitmq.*` | |
| `amqp:outbound-endpoint exchangeName="sales_exchange" queueName="sales_queue"` | `RabbitTemplate#sendAndReceive(exchange, routingKey, Message)` | Raw bytes, no conversion |
| `exchange-pattern="request-response" responseTimeout="10000"` | `RabbitTemplate#setReplyTimeout(amqp.response-timeout)` | Direct reply-to |
| Exchange/queue/binding created by hand (README 2-4, IT `setUp`) | `AmqpTopologyConfig` (`TopicExchange`, `Queue`, `Binding` `#`) | Can be disabled |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| connector host/port/user/password | `spring.rabbitmq.*` (`${amqp.host:localhost}`, ...) | localhost:5672 guest |
| `exchangeName` / `queueName` | `amqp.exchange` / `amqp.queue` | `sales_exchange` / `sales_queue` |
| `responseTimeout` | `amqp.response-timeout` | 10000 |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-munit-test-payload` | The IT's `test` payload arrives on `sales_queue` through `sales_exchange` with a `replyTo` | `munit` (`SendingJsonDataToAmqpQueueIT`) |
| `02-readme-sales-json` | README body is published verbatim | `documented` (README 8-9) |
| `03-request-response-reply` | The reply to the request-response call is the HTTP response body | `documented` (flow XML) |
| `04-get-not-allowed` | `allowedMethods=POST`: 405, nothing published | `documented` (flow XML) |

## Behavioural deltas

See [README](README.md#behavioural-deltas).
