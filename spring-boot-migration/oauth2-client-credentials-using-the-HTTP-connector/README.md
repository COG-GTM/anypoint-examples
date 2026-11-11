# oauth2-client-credentials-using-the-HTTP-connector (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example
[`oauth2-client-credentials-using-the-HTTP-connector`](../../oauth2-client-credentials-using-the-HTTP-connector).
Mule's `oauth2:client-credentials-grant-type` on the Box request config is replaced by **Spring Security OAuth2 Client**
(`ClientRegistration` with `client_credentials`, `AuthorizedClientServiceOAuth2AuthorizedClientManager`).

| Endpoint | Behaviour (same as Mule) |
|---|---|
| `GET/any /oauth` | Logs the payload, then calls Box `GET https://app.box.com/api/oauth2/authorize/` with `Authorization: Bearer <token>` and returns Box's body. |
| `any /FIM/**` | Proxies method, path, query string, body and `Content-Type` to `https://tfim01.demos.ibm.com` and returns the response. |

The token is fetched from `https://app.box.com/api/oauth2/token` when the application starts (form `grant_type=client_credentials`,
client authenticated with HTTP Basic `client_id:client_secret`). When Box answers 401 or 403 the token is fetched again and the request is
sent once more, which is Mule's default `refreshTokenWhen`. Any downstream status of 400 or higher fails the flow with
`500 Response code N mapped as failure.`

## Run

```bash
cd spring-boot-migration
mvn -pl oauth2-client-credentials-using-the-HTTP-connector -am spring-boot:run \
  -Dspring-boot.run.arguments="--oauth.client.id=<box client id> --oauth.client.secret=<box client secret>"
curl http://localhost:8081/oauth
```

Property names are the Mule ones (`oauth.client.id`, `oauth.client.secret`, `client.id`). Box/FIM hosts are `box.base-url`, `box.token-url`
and `fim.base-url`.

## Tests

`mvn -pl oauth2-client-credentials-using-the-HTTP-connector -am verify` runs:

- `ClientCredentialsParityTest`: 5 JSON fixtures in `src/test/resources/parity/oauth2-client-credentials-using-the-HTTP-connector/`, run against
  the Spring app with WireMock playing Box, its token endpoint and the FIM host.
- `ClientCredentialsGrantFlowTest`: the grant flow itself. Checks the token request made at startup (Basic auth, form body),
  refresh-and-resend once on 401, and that a second 401 after the refresh fails the flow.

Box and IBM Tivoli FIM are external SaaS endpoints, so they are stubbed with WireMock (`box.*`/`fim.*` point at it in the `parity` profile).

## Behavioural deltas

1. **TLS context.** Mule's `TLS_Context` (custom trust store and key store) is not carried over. Box and FIM are called with the JVM
   default trust store. Configure `javax.net.ssl.*` or a Spring SSL bundle if you need a custom one.

Same as Mule: there is no expiry-driven refresh (a new token is fetched only at startup and after a 401/403), and a failing startup token
request stops the application (Mule's grant type fails in its start phase).
