# proxying-a-rest-api (Spring Boot)

Spring Boot 3 migration of the Mule 3.8 example [`proxying-a-rest-api`](../../proxying-a-rest-api): a transparent
proxy that replays every inbound HTTP request (any method, any path under `/`, query string, body and application
headers) to the Box API (`https://api.box.com`) and returns the API's body and headers.

Implementation: one Spring MVC catch-all handler (`RestApiProxyController`, `@RequestMapping("/**")`) forwarding with
`WebClient` (Reactor Netty, redirects followed). Header copying/removal is the Mule `copy-headers` sub-flow
(`MuleHeaderFilter`).

## Run

```bash
cd spring-boot-migration
mvn -B -pl proxying-a-rest-api -am verify                 # build + parity tests
java -jar proxying-a-rest-api/target/proxying-a-rest-api-1.0.0-SNAPSHOT.jar --http.port=8081
curl -H "Authorization: Bearer <box-token>" http://localhost:8081/2.0/folders/0
```

| Property | Default | Meaning |
|---|---|---|
| `http.port` | `8081` | Listener port (Mule `http.port`) |
| `proxy.target.base-url` | `https://api.box.com` | Proxied API (Mule `HTTP_Request_Configuration` host/port/protocol) |
| `keystore.path` / `keystore.key` / `keystore.password` | blank | Optional client key store (Mule `tls:key-store`); blank path = none |

## Parity tests

`ProxyingARestApiParityTest` (`AbstractWireMockParityTest`) replays `src/test/resources/parity/proxying-a-rest-api/*.json`;
WireMock stands in for `api.box.com`, so no Box account, OAuth dance or PhantomJS is needed (the original
`ProxyingRestApiIT` needed all three). Every fixture asserts both the response and the outbound request
(method, path, query, forwarded/removed headers, body).

## Behavioural deltas

- **Box API stubbed in tests.** The original IT logged into Box with real OAuth credentials through PhantomJS; the
  parity tests use WireMock with Box-shaped payloads instead. Runtime behaviour is unchanged (the real Box URL is the default).
- **HTTP/2 upgrade headers are not forwarded.** Besides the headers Mule's `copy-headers` removes (`Host`,
  `Content-Length`, `http.*`, `MULE_*`, `Connection`, `Transfer-Encoding`) the Spring proxy also drops `Upgrade` and
  `HTTP2-Settings`. Mule 3.8 would have copied them, but without the (removed) `Connection: Upgrade` they make the
  request malformed for HTTP/1.1 servers (Jetty answers 400). Callers that don't send h2c upgrade requests see no difference.
- **Error body text.** For an API status >= 400 both answer 500 (Mule's default `0..399` success range fails the flow).
  The Spring body is `Response code <n> mapped as failure.` as `text/plain`; Mule's default exception strategy body
  wording is not documented, so the fixture only asserts the status.
