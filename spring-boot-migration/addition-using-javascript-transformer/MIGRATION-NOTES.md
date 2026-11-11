# Migration notes: `addition-using-javascript-transformer`

- Original Mule app: [`../../addition-using-javascript-transformer`](../../addition-using-javascript-transformer)
- Mule flows: `addition-using-javascript-transformer.xml` → `addition-using-javascript-transformerFlow`
- Spring Boot module: `spring-boot-migration/addition-using-javascript-transformer`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/"` | `CalculatorController` |  |
| `scripting:transformer` (JavaScript/Rhino `eval` + `for..in` sum) | `JavaScriptSum` (plain Java) invoked from `AdditionFlowConfig` | Reproduces JS `+` semantics: numbers add as doubles, a string switches to concatenation. |
| `set-payload "Sum is: #[payload]."` | String formatting in `JavaScriptSum` | Rhino number formatting (`3.0`) kept. |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-sum-two-integers.json` | The MUnit/IT input {a:1,b:2}: Rhino sums to the Double 3.0. | `munit` |
| `02-readme-example.json` | README example {a:3,b:4}, logged as 'Sum is: 7.0'. | `documented` |
| `03-sum-all-values-including-decimals.json` | for (var i in list) visits every property; decimals and negatives add numerically. | `documented` |
| `04-empty-object-sums-to-zero.json` | An empty object leaves the initial var sum = 0. | `documented` |
| `05-string-value-concatenates.json` | JavaScript + with a string operand concatenates: 0 + 1 = 1, then 1 + '2' = '12'. | `documented` |

Other tests: `AdditionUsingJavascriptTransformerParityTest`, `JavaScriptSumTableTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

- The Rhino script engine is gone. `JavaScriptSum` emulates the parts of JavaScript the script uses (number addition, string concatenation, `null`/boolean coercion, arrays). Inputs that relied on other JavaScript `eval` features (functions, expressions inside the payload) are rejected as invalid JSON instead of being evaluated.
