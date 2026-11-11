# Migration notes: `extracting-data-from-LDAP-directory`

- Original Mule app: [`../../extracting-data-from-LDAP-directory`](../../extracting-data-from-LDAP-directory)
- Mule flows: `ldap.xml` → `ldapFlow1`
- Spring Boot module: `spring-boot-migration/extracting-data-from-LDAP-directory`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/"` | `LdapController#searchPeople` (`text/plain`) |  |
| `ldap:config authDn/authPassword url="ldap://localhost:${ldap.port}/dc=my-domain,dc=com"` | `spring.ldap.urls/base/username/password` (Spring LDAP `LdapContextSource`) |  |
| `ldap:search baseDn="ou=people" filter="(objectClass=*)"` | `LdapTemplate#search` (`SearchScope.ONELEVEL`, `ContextMapper`) | One level, so `ou=people` itself is not returned, matching the README output |
| `byte-array-to-object` + `collection-splitter` + `logger` + `collection-aggregator` + `logger` | `List<String>` + per-entry and "all users" logs |  |
| `object-to-string-transformer` (`LDAPEntry#toString`) | `LdapEntryFormatter` | `dn:` relative to the base, `name: value` lines, binary values (`userPassword`) as `name:: base64` |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` | 8081 |
| `ldap.dn` / `ldap.password` | same keys → `spring.ldap.username/password` | blank |
| `ldap.port` | `ldap.port` → `spring.ldap.urls` | 389 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-people-listed-as-ldap-entries` | The three people from `ldap.ldif` with every attribute, exactly as the README sample (IT asserts the three dn lines) | documented |

## Behavioural deltas

- **Order of entries and attributes**: Mule printed entries and attributes in whatever order its connector's collections produced, and LDAP itself does not guarantee an order. The Spring module prints them in the order the directory and JNDI return them. Parity is asserted on content, not order: the same DNs, attribute names and values, and the same overall text format.
