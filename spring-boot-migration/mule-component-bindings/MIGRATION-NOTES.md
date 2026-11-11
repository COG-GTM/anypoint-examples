# Migration notes: `mule-component-bindings`

- Original Mule app: [`../../mule-component-bindings`](../../mule-component-bindings)
- Mule flows: `mule-component-bindings.xml` → `StockServiceFlow`, `TwitterFlow`, `mule-component-bindingsFlow1`
- Spring Boot module: `spring-boot-migration/mule-component-bindings`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/api/*"` + `jersey:resources` (`StockStatsResourceImpl`) | `StockStatsController` + `StockStatsService` | Validation, paging and error mapping ported line for line. |
| `<binding interface="TwitterService" method="search">` → `vm://twitterFlowRequest` | `@MessagingGateway TwitterFlowGateway` exposed as the `TwitterService` bean (`TwitterBindingFlowConfig`) |  |
| `TwitterFlow` (TwitterRequestProcessor → twitter:search → TwitterResponseProcessor) | `twitterFlow` (`prepare` → `TwitterClient.search`) | Same query/since/until. |
| `StocklyticsStockService`, `Sentiment140SentimentService` | Same-named `@Component`s using `RestClient` | Typed seams; WireMock in tests. |
| `mule-component-bindingsFlow1` (`/`, fixed AAPL search) | `StockStatsController#aaplTweets` |  |
| `WebApplicationException(status, message)` | `@ExceptionHandler` → status + message body |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| listener port 8180 | `server.port` (`${http.port:8180}`) | 8180 |
| `twitter.*` keys | `twitter.base-url`, `twitter.bearer-token` | env vars |
| `stockstats.stocklyticsApiKey` | `stockstats.stocklytics.api-key` | env var |
| `stockstats.sentiment140ApiKey` | `stockstats.sentiment140.app-id` | env var |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-no-tweets.json` | Stock found, first Twitter page empty: one binding call, empty classify, zero social stats. | `documented` |
| `02-twitter-binding-pages-past-1000.json` | TwitterFlow's twitter:search only passes query/since/until, so every page returns the same 100 tweets: 11 binding calls (1100 > 1000) set moreTweets, the list is cut to 1000, and only the last occurrence of each tweet id receives a sentiment (idToTweet map); unclassified tweets count as positive. | `documented` |
| `03-invalid-stock-symbol.json` | Stocklytics reports no data -> InvalidStockSymbol -> 400 'Invalid stock symbol <stock>'; Twitter is not called. | `documented` |
| `04-stocklytics-error-is-500.json` | Non-200 from Stocklytics -> RuntimeException message returned with 500. | `documented` |
| `05-sentiment-error-is-500.json` | Non-200 from Sentiment140 -> 500 with the exception message. | `documented` |
| `06-missing-stock.json` | Blank stock -> 400 before any service call. | `documented` |
| `07-missing-date.json` | Blank date -> 400. | `documented` |
| `08-short-date.json` | Date length != 10 -> 400. | `documented` |
| `09-unparseable-date.json` | SimpleDateFormat(yyyy-MM-dd) parse failure (a / where - is expected) -> 400. Lenient parsing accepts e.g. 26-10-2012, so that is not a failure case. | `documented` |

Other tests: `AaplSearchFlowTest`, `MuleComponentBindingsParityTest`, `StockStatsTableTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

- The fixed AAPL search (Mule port 8081, `/`) runs on the same port as the API (8180). It returns the tweets as JSON, while Mule returned the raw Java list.
- The Twitter connector became Twitter API v1.1 `search/tweets.json` with an app-only bearer token. As in Mule, no paging parameters are sent (the connector ignored `rpp`/`page`), so every page returns the same results and the loop runs until more than 1000 tweets are collected.
- The Mule Stocklytics/Sentiment140 clients used `response.getEntity().toString()` and `Entity.text(bulkRequest)`, which never sent or read real JSON. The Spring clients send and read the JSON those services document. Fixtures use that contract.
