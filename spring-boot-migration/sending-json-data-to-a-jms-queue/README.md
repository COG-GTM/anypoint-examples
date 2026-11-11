# sending-json-data-to-a-jms-queue (Spring Boot 3)

Spring Boot migration of the Mule 3.8 example [`sending-json-data-to-a-jms-queue`](../../sending-json-data-to-a-jms-queue).

`POST /sales` takes the raw request body, converts it to a string and sends it as a JMS `TextMessage` to the
ActiveMQ queue `sales`. The response is `200` with the same string as body (the Mule flow's final payload).
Any other method on `/sales` returns `405`; any other path returns `404`.

## Run

```bash
docker run -d -p 61616:61616 -p 8161:8161 apache/activemq-classic:5.18.3
cd spring-boot-migration && mvn -pl sending-json-data-to-a-jms-queue -am spring-boot:run
curl -X POST http://localhost:8081/sales -d '{"ITEM_ID"= 001, "ITEM_NAME" = "Shirt", "QTY" = 1, "PRICE" = 20}'
```

Then check the `sales` queue at <http://localhost:8161/admin/queues.jsp>.

| Property | Default | Mule equivalent |
|---|---|---|
| `http.port` | `8081` | `http.port` in `mule-app.properties` |
| `activemq.broker-url` | `tcp://localhost:61616` | `activemq-connector brokerURL` |
| `activemq.username` / `activemq.password` | empty | (none; Mule used an anonymous connection) |
| `jms.sales-queue` | `sales` | `jms:outbound-endpoint queue="sales"` |

## Parity tests

`SendingJsonDataToAJmsQueueParityTest` (extends `AbstractParityTest`) starts ActiveMQ Classic with Testcontainers,
sends each fixture in `src/test/resources/parity/sending-json-data-to-a-jms-queue/` to the Spring Boot endpoint,
checks the HTTP status and body, and then reads queue `sales` to check the message count, type and body
(`expectedMessages`, compared with `MessagingParityAssertions`).

```bash
cd spring-boot-migration && mvn -B -pl sending-json-data-to-a-jms-queue -am verify
```

## Behavioural deltas

- **405/404 response bodies**: the fixtures assert only the status code. Spring Boot returns its standard JSON error
  body, while the Mule HTTP listener returned a short plain-text message. Clients that only check status codes see
  no difference.
- **Response `Content-Type`**: the response echoes the request `Content-Type`. Mule 3.8 carried the inbound MIME type
  through the flow in the same way, but no fixture asserts it because none of the Mule sources document it.
