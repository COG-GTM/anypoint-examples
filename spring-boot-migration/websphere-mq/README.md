# websphere-mq (Spring Boot 3)

Spring Boot migration of the Mule 3.8 EE example [`websphere-mq`](../../websphere-mq).

| Mule flow | Spring Boot |
|---|---|
| Input: AJAX channel `/services/wmqExample/enqueue` → WMQ queue `in` | `POST /services/wmqExample/enqueue` (`InputController#enqueue`), which puts the text on `in` and returns it |
| MessageProcessor: transacted read of `in`, append `" - processed"`, wait 15 s, send to `out` in the same transaction | `MessageProcessor#process` (`@JmsListener` on a session-transacted container) |
| Output: read `out` → AJAX channel `/services/wmqExample/dequeue` | `OutputFlow#output`, which publishes to every `GET /services/wmqExample/dequeue` Server-Sent Events subscriber |
| `docroot/index.html` at `http://localhost:8086/services/wmqExample` | Same page at the same URL, ported from dojo/`mule.js` to `fetch` + `EventSource` |

The WMQ connector becomes IBM's own `com.ibm.mq:mq-jms-spring-boot-starter` (IBM MQ JMS client, client/TCP transport,
like `transportType="CLIENT_MQ_TCPIP"`). No stub is needed: the parity tests run against a real queue manager, the
free IBM MQ Advanced for Developers container image `icr.io/ibm-messaging/mq`.

## Run

```bash
docker run -d -p 1414:1414 -e LICENSE=accept -e MQ_QMGR_NAME=QM1 -e MQ_APP_PASSWORD=<choose one> icr.io/ibm-messaging/mq:9.4.1.0-r1
# create the queues the example needs, as in the original README:
docker exec -i <container> runmqsc QM1 <<'MQSC'
DEFINE QLOCAL('in')
DEFINE QLOCAL('out')
SET AUTHREC PROFILE('in') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(BROWSE,GET,INQ,PUT,DSP)
SET AUTHREC PROFILE('out') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(BROWSE,GET,INQ,PUT,DSP)
MQSC
cd spring-boot-migration && mvn -pl websphere-mq -am spring-boot:run \
  -Dspring-boot.run.arguments="--wmq.username=app --wmq.password=<pwd>"
```

Open <http://localhost:8086/services/wmqExample>, type a message and click **Send**. After 15 s, "&lt;message&gt; -
processed" appears in the table.

| Property | Default | Mule equivalent |
|---|---|---|
| `http.port` | `8086` | `ajax:connector serverUrl` port |
| `wmq.host` / `wmq.port` | `localhost` / `1414` | `wmq:connector hostName` / `port` |
| `wmq.queue` | `QM1` | `wmq:connector queueManager="${wmq.queue}"` |
| `wmq.channel` | `DEV.APP.SVRCONN` | `wmq:connector channel` |
| `wmq.username` / `wmq.password` | empty | `wmq:connector username` / `password` |
| `wmq.in-queue` / `wmq.out-queue` | `in` / `out` | endpoint `queue` attributes |
| `wmq.processor.append-string` / `wait-millis` | `" - processed"` / `15000` | `test:component appendString` / `waitTime` |
| `wmq.output.enabled` | `true` | (Output flow; tests turn it off to inspect `out`) |

## Parity tests

Both test classes start IBM MQ in Testcontainers (`IbmMqContainer`: QM1, `DEV.APP.SVRCONN`, queues `in`/`out`, app
password generated per run). They subscribe to the dequeue SSE stream before sending. Assertions cover the enqueue
response, the dequeued messages (count and exact text), the 15 s processing delay, and what is left on `in` / `out`.

| Test class | Fixtures | Config |
|---|---|---|
| `EndToEndParityTest` | `end-to-end/` (2): browser → `in` → `out` → browser | as shipped |
| `MessageProcessorParityTest` | `message-processor/` (1): `in` → `out` as a `TextMessage` | `wmq.output.enabled=false` |

```bash
cd spring-boot-migration && mvn -B -pl websphere-mq -am verify
```

The first run pulls the IBM MQ image (about 560 MB compressed) from `icr.io`. The developer image is free to use for
development and testing, and the tests accept its licence with `LICENSE=accept`.

## Behavioural deltas

- **AJAX (CometD/Bayeux) transport replaced by HTTP POST + Server-Sent Events.** Mule 3's AJAX connector and its
  `mule.js` client have no Spring equivalent. The channel paths stay the same (`/services/wmqExample/enqueue` and
  `/dequeue`), and so does the page. A client written against the Bayeux protocol (`mule.rpc` / `mule.subscribe`)
  has to switch to `POST` + `EventSource`.
- **No message cache for late subscribers.** `ajax:outbound-endpoint cacheMessages="true"` kept recent messages for
  clients that subscribed later. Here, a dequeued message only goes to the subscribers connected at that moment.
- **Queue setup and credentials.** As in Mule, queues `in`/`out` must exist on the queue manager. The defaults
  target the IBM developer image (`QM1`, `DEV.APP.SVRCONN`). No credentials are stored in the module.
