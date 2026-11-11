# Migration notes: `salesforce-data-retrieval`

- Original Mule app: [`../../salesforce-data-retrieval`](../../salesforce-data-retrieval)
- Mule flows: `salesforce-id-retrieval.xml` → `showFormFlow`, `salesforceDataRetrievalFlow`
- Spring Boot module: `spring-boot-migration/salesforce-data-retrieval`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path=/ allowedMethods=GET` (`showFormFlow`) | `SalesforceRetrievalController#showForm` (`@GetMapping("/")`) |  |
| `sfdc:describe-global` | `SalesforceClient#describeGlobal` → `GET /sobjects` |  |
| `dw:transform-message` (`div: { option @(value: $.name): $.label }`, `application/xml`) + `object-to-string` | `SalesforceRetrievalController#options` | DataWeave 1.0 XML writer output, including the XML declaration |
| `parse-template location=retrieval/index.html` | `retrieval/index.html` (copied verbatim) with `#[message.payload]` replaced |  |
| `http:listener path=/ allowedMethods=POST` (`salesforceDataRetrievalFlow`) | `SalesforceRetrievalController#retrieve` (`@PostMapping("/")`, form params) |  |
| MEL query expression in `sfdc:query-all` | `SalesforceRetrievalController#soql` | Same concatenation; missing `field` fails, other missing values become `null` |
| `sfdc:query-all` | `SalesforceClient#queryAll` → `GET /queryAll?q=...` |  |
| `foreach` + `byte-array-to-string-transformer` + `set-variable #[flowVars.response + payload]` + final `byte-array-to-string-transformer` | `MuleText#map` / `MuleText#collection` | Reproduces Mule's `ObjectToString` → `MapUtils.toString` / `CollectionUtils.toString(c, 50)` (checked against mule-core 3.8.0) |
| `logger "Result: #[payload]"` | SLF4J `Result: ...` (INFO) |  |
| `catch-exception-strategy` → `set-payload "Invalid Salesforce query."` | `try/catch` in `retrieve` → 200 `Invalid Salesforce query.` |  |
| `sfdc:config` (username / password / securityToken, connection pooling) | `salesforce.SalesforcePasswordSession` (OAuth2 username-password flow, cached session) + `SalesforceConfig` | Re-login once on 401 |
| Salesforce connector operations | `salesforce.SalesforceClient` (Spring `RestClient`) | Typed REST client; queries follow `nextRecordsUrl` |
| Connector record maps | `salesforce.SalesforceRecords#toMuleMap` | `HashMap` + `type`, SOAP date format |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `sfdc.username` | `sfdc.username` | blank |
| `sfdc.password` | `sfdc.password` | blank |
| `sfdc.securityToken` | `sfdc.securityToken` | blank |
| – | `sfdc.consumerKey` / `sfdc.consumerSecret` (new: Connected App) | blank |
| `sfdc:config url` (SOAP login endpoint) | `sfdc.login-url` | `https://login.salesforce.com` |
| – | `sfdc.api-version` | `61.0` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-get-form-lists-sobject-types` | GET / runs describe-global and renders retrieval/index.html with the DataWeave XML <option> list (value=name, text=label, XML declaration included, as in example.xml) in place of #[message.payload]. | documented |
| `02-post-query-users-by-name` | The IT's form (object=user, field=email, searchKey=name, searchValue=mule) builds "SELECT id, name ,email from user where name like '%mule%'" and returns every record; reply.txt lines (Email=..., Id=..., Name=...) are all contained. | documented |
| `03-post-empty-field-no-results` | An empty Field adds nothing to the select list (note the double space the MEL concatenation leaves: "SELECT id, name  from Contact where Name like '%young%'"); no matching records gives an empty list, rendered as []. | documented |
| `04-post-invalid-query-is-caught` | Selecting Name twice (README: 'choosing these fields will result in an invalid query') makes Salesforce reject the SOQL; the catch-exception-strategy answers 200 'Invalid Salesforce query.'. | documented |
| `05-post-without-field-param-is-caught` | Without a 'field' form parameter, payload.field is null and payload.field.isEmpty() fails while the query is being built, so Salesforce is never called (no downstream request at all) and the catch-exception-strategy answers 'Invalid Salesforce query.'. | documented |
| `06-post-more-than-50-records-is-truncated` | 51 matching records: Mule's ObjectToString renders a collection with CollectionUtils.toString(collection, 50), i.e. | documented |

## Behavioural deltas

See [README.md](README.md#behavioural-deltas).
