# Migration notes: `filtering-a-message`

- Original Mule app: [`../../filtering-a-message`](../../filtering-a-message)
- Mule flows: `filtering-a-message.xml` → `filtering-a-messageFlow`
- Spring Boot module: `spring-boot-migration/filtering-a-message`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/" allowedMethods="POST"` | `FilteringController` (`@PostMapping("/")`) |  |
| `json:json-to-object-transformer returnClass=java.util.HashMap` | Jackson `Map` binding |  |
| `custom-filter` (`FreeMembershipDiscountFilter`) | Spring Integration `filter()` in `FilteringFlowConfig` using `FreeMembershipDiscountFilter.accept` | Predicate ported verbatim. |
| `set-payload "the discount was granted."` | `transform` after the filter | Rejected messages are discarded; the controller returns an empty 200. |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-free-member-discount-granted.json` | Free member, 12 months, 2000 purchases: months >= 12 && purchases >= 1500, so the message passes the custom filter. | `munit` |
| `02-free-member-discount-rejected.json` | Free member, 6 months, 100 purchases: the custom filter rejects the message and the listener answers 200 with an empty body. | `munit` |
| `03-premium-boundary-granted.json` | Premium member at the boundary: months >= 6 && purchases >= 1000. | `documented` |
| `04-premium-too-new-rejected.json` | Premium member with fewer than 6 months is rejected whatever the purchases. | `documented` |
| `05-free-short-membership-high-purchases.json` | Free member, <= 6 months, purchases >= 2500. | `documented` |
| `06-free-mid-membership-below-threshold.json` | Free member, 7..11 months needs 2000; 1999 is rejected. | `documented` |
| `07-get-not-allowed.json` | The listener only allows POST (allowedMethods="POST"), so a GET is answered with 405 Method Not Allowed. | `documented` |

Other tests: `FilteringAMessageParityTest`, `FilteringRoutingTableTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

None.
