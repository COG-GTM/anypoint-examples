# mule-component-bindings (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`mule-component-bindings`](../../mule-component-bindings).

REST `GET /api/stockStats?stock=..&date=..` returns a stock's closing figures plus Twitter sentiment counts. `TwitterService` is a component binding sent through a separate Twitter flow.

## Run

```bash
cd spring-boot-migration
mvn -B -pl mule-component-bindings -am verify          # build + parity tests
mvn -pl mule-component-bindings spring-boot:run        # then: GET http://localhost:8180/api/stockStats?stock=AAPL&date=2012-10-26
```

## Tests

- Parity fixtures: `src/test/resources/parity/mule-component-bindings/` (9 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `AaplSearchFlowTest`, `MuleComponentBindingsParityTest`, `StockStatsTableTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- The fixed AAPL search (Mule port 8081, `/`) runs on the same port as the API (8180). It returns the tweets as JSON, while Mule returned the raw Java list.
- The Twitter connector became Twitter API v1.1 `search/tweets.json` with an app-only bearer token. As in Mule, no paging parameters are sent (the connector ignored `rpp`/`page`), so every page returns the same results and the loop runs until more than 1000 tweets are collected.
- The Mule Stocklytics/Sentiment140 clients used `response.getEntity().toString()` and `Entity.text(bulkRequest)`, which never sent or read real JSON. The Spring clients send and read the JSON those services document. Fixtures use that contract.
