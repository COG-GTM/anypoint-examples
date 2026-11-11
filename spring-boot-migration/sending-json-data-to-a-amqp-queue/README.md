# sending-json-data-to-a-amqp-queue (Spring Boot 3)

Spring Boot migration of the Mule 3.8 example [`sending-json-data-to-a-amqp-queue`](../../sending-json-data-to-a-amqp-queue).

`POST /` publishes the raw request body to the RabbitMQ exchange `sales_exchange`, which routes it to `sales_queue`.
Like the Mule `amqp:outbound-endpoint exchange-pattern="request-response"`, the publisher then waits up to
`amqp.responseTimeout` (10 s) for a reply. If a consumer replies, the reply body is the HTTP response body.
If nobody replies, the response is `200` with an empty body. Other methods on `/` return `405`.

## Run

```bash
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
cd spring-boot-migration && mvn -pl sending-json-data-to-a-amqp-queue -am spring-boot:run
curl -X POST http://localhost:8081/ -d '{"ITEM_ID"= 001, "ITEM_NAME" = "Shirt", "QTY" = 1, "PRICE" = 20}'
```

Then open `sales_queue` in the management console (<http://localhost:15672>, guest/guest) and use **Get messages**.

| Property | Default | Mule equivalent |
|---|---|---|
| `http.port` | `8081` | `http.port` |
| `amqp.host` / `amqp.port` / `amqp.username` / `amqp.password` / `amqp.virtualHost` | `localhost` / `5672` / `guest` / `guest` / `/` | `amqp:connector` defaults |
| `amqp.exchangeName` / `amqp.queueName` | `sales_exchange` / `sales_queue` | `exchangeName` / `queueName` endpoint attributes |
| `amqp.routingKey` | `sales_queue` | (Mule derived it from the endpoint) |
| `amqp.responseTimeout` | `10000` | `responseTimeout="10000"` |
| `amqp.declareTopology` | `true` | (README steps 2-4 created them by hand) |

## Parity tests

`SendingJsonDataToAAmqpQueueParityTest` starts RabbitMQ with Testcontainers and sends each fixture in
`src/test/resources/parity/sending-json-data-to-a-amqp-queue/` to the Spring Boot endpoint. It then reads
`sales_queue` and checks the message count, body, the exchange it came through, and the `replyTo` set by the
request-response call. One fixture starts a test replier on `sales_queue` to show that the reply becomes the HTTP
response body. The parity profile shortens `amqp.responseTimeout` to 2 s so the no-reply cases finish quickly.

## Behavioural deltas

- **Topology declaration**: on startup the app declares a durable topic exchange `sales_exchange`, a durable queue
  `sales_queue` and a `#` binding, matching what the Mule IT set up. The Mule app expected you to create these by
  hand (README steps 2-4). Set `amqp.declareTopology=false` to turn this off.
- **Reply queue**: Spring AMQP waits for replies on RabbitMQ direct reply-to (`amq.rabbitmq.reply-to`). The Mule
  transport used a temporary reply queue. Either way a replier simply publishes to the message's `replyTo`.
- **Timeout response body**: when no reply arrives, Spring returns `200` with an empty body. Mule returned the flow
  result after the timeout. The fixtures do not assert that body (`IGNORE`) because the Mule sources don't document it.
- **405 body**: Spring Boot JSON error body instead of Mule's plain text. Only the status is asserted.
