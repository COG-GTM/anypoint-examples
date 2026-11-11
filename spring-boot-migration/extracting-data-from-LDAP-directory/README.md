# Extracting data from an LDAP directory (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`extracting-data-from-LDAP-directory`](../../extracting-data-from-LDAP-directory).

`GET /` searches `ou=people` (one level, `(objectClass=*)`) under `dc=my-domain,dc=com` and returns the entries as text, in the form of the Mule connector's `LDAPEntry` list (`[dn: ...\n...\n\n, dn: ...]`).

## Run

```bash
cd spring-boot-migration/extracting-data-from-LDAP-directory
mvn spring-boot:run -Dldap.dn=cn=Manager,dc=my-domain,dc=com -Dldap.password=root -Dldap.port=389   # then: curl http://localhost:8081/
```

## Tests

```bash
cd spring-boot-migration
mvn -pl extracting-data-from-LDAP-directory -am verify     # needs Docker for Testcontainers-based modules
```

`LdapParityTest` uses Spring Boot's embedded UnboundID directory (the Mule IT starts the same in-memory server) loaded with the Mule project's `ldap.ldif` and bound as `cn=Manager`/`root`. Status and headers go through `ParityComparator`. The body is checked for the list shape and compared as a set of entries, each a set of attribute lines (see deltas). `LdapEntryFormatterTest` covers the entry format.

Fixtures live in `src/test/resources/parity/extracting-data-from-LDAP-directory/`. Each fixture's `expected` (and any side-effect block) holds the **Mule** app's behaviour, and `source.reference` cites where it comes from. See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- **Order of entries and attributes**: Mule printed entries and attributes in whatever order its connector's collections produced, and LDAP itself does not guarantee an order. The Spring module prints them in the order the directory and JNDI return them. Parity is asserted on content, not order: the same DNs, attribute names and values, and the same overall text format.
