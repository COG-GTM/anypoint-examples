# Migration notes: `<mule-app-name>`

- Original Mule app: [`../../<mule-app-name>`](../../<mule-app-name>)
- Mule flows: `<file>.xml` → `<flow names>`
- Spring Boot module: `spring-boot-migration/<mule-app-name>`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/..."` (`<flow>`) | `XController#method` (`@GetMapping("/...")`) | |
| | | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| | | |

## Behavioural deltas

None. <!-- or list each difference, why, and its impact on callers -->
