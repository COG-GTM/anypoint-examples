# Migration notes: `websphere-mq`

- Original Mule app: [`../../websphere-mq`](../../websphere-mq)
- Mule flows: `websphere-mq.xml` → `Input`, `MessageProcessor`, `Output`
- Spring Boot module: `spring-boot-migration/websphere-mq`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `wmq:connector` (`CLIENT_MQ_TCPIP`, host/port/queueManager/channel/username/password) | `com.ibm.mq:mq-jms-spring-boot-starter`, `ibm.mq.*` properties | IBM MQ JMS client |
| `ajax:connector serverUrl="http://0.0.0.0:8086/services/wmqExample" resourceBase=docroot` | embedded Tomcat on `8086`; `static/services/wmqExample/index.html`; `IndexController` | |
| `ajax:inbound-endpoint channel="/services/wmqExample/enqueue"` (Input) | `InputController#enqueue` (`POST`) | Returns the enqueued text |
| `message-properties-transformer delete MULE_REPLYTO` | Not needed (no reply-to is set) | |
| `wmq:outbound-endpoint queue="in"` | `JmsTemplate#convertAndSend("in", text)` | `TextMessage` |
| `wmq:inbound-endpoint queue="in"` + `wmq:transaction ALWAYS_BEGIN` (MessageProcessor) | `MessageProcessor#process`, `@JmsListener` on `transactedListenerFactory` | |
| `test:component appendString=" - processed" waitTime="15000"` | `Thread.sleep(waitMillis)` + append | Configurable |
| `wmq:outbound-endpoint queue="out"` + `JOIN_IF_POSSIBLE` | `JmsTemplate#convertAndSend("out", ...)`, which joins the listener session | Same MQ syncpoint |
| `wmq:inbound-endpoint queue="out"` (Output) | `OutputFlow#output` `@JmsListener` | |
| `ajax:outbound-endpoint channel="/services/wmqExample/dequeue"` | `DequeueChannel` → `GET /services/wmqExample/dequeue` (SSE) | No `cacheMessages` |
| `docroot/index.html` (dojo + `mule.js`) | Same page using `fetch` + `EventSource` | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `wmq.host`, `wmq.port` | `ibm.mq.conn-name: ${wmq.host}(${wmq.port})` | `localhost(1414)` |
| `wmq.queue` (queue manager) | `ibm.mq.queue-manager` | `QM1` |
| `wmq.channel` | `ibm.mq.channel` | `DEV.APP.SVRCONN` |
| `wmq.username`, `wmq.password` | `ibm.mq.user`, `ibm.mq.password` | empty |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `end-to-end/01-readme-browser-roundtrip` | README flow: enqueue returns the text; after ≥15 s exactly one "… - processed" reaches the dequeue subscriber; `in`/`out` empty | `documented` (README, flow XML, index.html) |
| `end-to-end/02-munit-test-payload` | `WebsphereMQIT` payload `test` → `test - processed` | `munit` |
| `message-processor/01-in-to-out-text-message` | Any producer's message on `in` → `TextMessage` "… - processed" on `out` after ≥15 s | `documented` (flow XML) |

## Behavioural deltas

See [README](README.md#behavioural-deltas): the AJAX/Bayeux transport is replaced by HTTP POST + SSE on the same paths, there is no message cache for late subscribers, and the queues still have to be set up on the queue manager.
