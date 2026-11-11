# Migration notes: `proxying-a-rest-api`

- Original Mule app: [`../../proxying-a-rest-api`](../../proxying-a-rest-api)
- Mule flows: `proxying-a-rest-api.xml` → `rest-api-proxy`, sub-flow `copy-headers`
- Spring Boot module: `spring-boot-migration/proxying-a-rest-api`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener-config port="${http.port}"` | `server.port: ${http.port:8081}` | |
| `http:listener path="/*"` (`rest-api-proxy`) | `RestApiProxyController#proxy` (`@RequestMapping("/**")`, all methods) | |
| `flow-ref copy-headers` (before request) | `MuleHeaderFilter.isCopied` applied to servlet request headers | |
| `copy-properties propertyName="*"` | copy all headers | |
| `remove-property Host / Content-Length / http.* / MULE_* / Connection / transfer-encoding` | `MuleHeaderFilter.REMOVED` + `http.` / `mule_` prefix check (case-insensitive) | also drops `Upgrade`, `HTTP2-Settings` (delta) |
| `http:request-config host="api.box.com" port="443" protocol="HTTPS"` | `ProxyClientConfiguration#proxyWebClient` (`proxy.target.base-url`) | Reactor Netty, `followRedirect(true)` = Mule default |
| `tls:context` / `tls:key-store` | optional `KeyManagerFactory` from `keystore.path` / `keystore.key` / `keystore.password` | |
| `http:request method=#[http.method] path=#[http.request.path] parseResponse="false"` | `WebClient.method(..).uri(requestURI + queryString)`, body as `byte[]` | raw bytes, no parsing |
| `http:query-params expression=#[http.query.params]` | raw query string appended | |
| default `success-status-code-validator` (0..399) | status >= 400 → 500 `Response code <n> mapped as failure.` | |
| `flow-ref copy-headers` (after request) | `MuleHeaderFilter.copy(upstream headers)`; `http.status` removed → listener default 200 | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| request config host/port/protocol | `proxy.target.base-url` | `https://api.box.com` |
| `keystore.key` | `keystore.key` | blank |
| `keystore.password` | `keystore.password` | blank |
| (hard-coded `keystore.jks`) | `keystore.path` | blank (no key store) |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-get-forwards-method-path-query-and-headers` | method/path/query/app headers forwarded, `MULE_*` dropped, API JSON + custom header returned | documented |
| `02-post-body-forwarded-created-becomes-200` | body forwarded verbatim; API 201 becomes 200 (`http.status` removed) | documented |
| `03-delete-no-content-becomes-empty-200` | DELETE forwarded; 204 becomes empty 200 | documented |
| `04-api-error-fails-flow-with-500` | API 404 fails the flow → 500 | documented |
| `05-redirect-followed` | 302 from API followed, final content returned | documented |

## Behavioural deltas

See [README](README.md#behavioural-deltas): Box stubbed in tests; `Upgrade`/`HTTP2-Settings` not forwarded; error body text.
