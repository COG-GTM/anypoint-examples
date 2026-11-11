# Migration notes: `cache-scope-with-fibonacci`

- Original Mule app: [`../../cache-scope-with-fibonacci`](../../cache-scope-with-fibonacci)
- Mule flows: `cache-scope-with-fibonacci.xml` → `fibonacciFlow`, `calculateFibonacciFlow`
- Spring Boot module: `spring-boot-migration/cache-scope-with-fibonacci`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/fibonacci"` | `FibonacciController` |  |
| `ee:cache` with `object-store-caching-strategy` (key = `n`) | Spring Cache (`CacheConfig`, cache `fibonacci`) used via the `Cache` API in `FibonacciService` | The `Cache` API is used instead of `@Cacheable` so hits can be told apart and return cost 0, as `FibonacciResponseGenerator` does. |
| `choice` `n < 2` / otherwise, recursive `flow-ref` | Recursive `FibonacciService.fibonacci` |  |
| `nocache` filter on the cache scope | `nocache` query parameter/header skips the cache for that call |  |
| `n >= 20` validation | Controller returns `ERROR: n must be less than 20` |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-first-request-computes-every-value.json` | Cold cache: Fibonacci(10) misses for n = 10..0, so COST is 11 (fixtures run in order against one app instance, like the IT). | `munit` |
| `02-intermediate-value-is-cached.json` | n=5 was cached while computing n=10, so the response generator reports COST 0. | `munit` |
| `03-repeat-request-is-cache-hit.json` | The same n again is served from the cache. | `documented` |
| `04-only-new-values-cost.json` | n=12 misses for 12 and 11 only (10 and 9 are cached): COST = (0 + 0 + 1) + 0 + 1 = 2. | `documented` |
| `05-n-must-be-less-than-20.json` | The choice rejects n >= 20 before the cache scope. | `documented` |
| `06-nocache-parameter-recomputes-children.json` | nocache sends n-1 and n-2 with the nocache property (cache skipped, result not stored); their own children use the cache again. Cached so far: 0..12. n=16 (miss): 15 [nocache] -> 14 (miss: 13 miss = 0+0+1, 12 hit -> 2) + 13 (hit) + 1 = 3; 14 [nocache] -> 13 hit + 12 hit + 1 = 1; COST = 3 + 1 + 1 = 5. | `documented` |
| `07-nocache-header-bypasses-cache.json` | A nocache HTTP header becomes an inbound property, so the cache scope is skipped for the top-level call. Children: 15 was never stored (it ran under nocache), so it misses (14 hit + 13 hit + 1 = 1); 14 hits (0). COST = 1 + 0 + 1 = 2. | `documented` |

Other tests: `CacheScopeWithFibonacciParityTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

- The cache is in memory (`spring.cache.type: simple`), like Mule's default in-memory store. Mule's TTL and expiry settings are not reproduced; entries live as long as the JVM.
