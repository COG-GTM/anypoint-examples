# Spring Boot 3 migration of the Anypoint (Mule 3.8) examples

This directory holds Spring Boot 3 / Java 17 versions of the 56 Mule ESB 3.8 example apps in this repository.
Each one is an independent Maven module that keeps the original app's external behaviour (HTTP routes, status codes,
payloads, queues, files, emails, downstream calls). Every module comes with **parity tests** that send the same inputs
to the original flow's behaviour and to the new Spring Boot code, then assert the results are equivalent.

The Mule apps themselves stay where they are; the root `pom.xml` (the Mule build) does not include this directory.

## Build

```bash
cd spring-boot-migration
mvn verify                       # builds parity-test-support and every migrated module present
mvn -pl hello-world -am verify   # a single module
```

Requirements: JDK 17+, Maven 3.9+, and Docker for modules that use Testcontainers (MySQL, ActiveMQ, RabbitMQ, MongoDB,
GreenMail, LDAP, SFTP).

## Layout

| Path | Purpose |
|---|---|
| `pom.xml` | Parent: `spring-boot-starter-parent` 3.3.4, Java 17, managed versions (WireMock, springdoc, MapStruct, Testcontainers), one auto-activated profile per app module |
| `parity-test-support/` | Shared parity harness (see [its README](parity-test-support/README.md)) and a sample harness test |
| `CONVENTIONS.md` | Module layout, naming, `application.yml` conventions, fixture format, definition of done |
| `MIGRATION-NOTES.template.md` | Template every module copies to `<module>/MIGRATION-NOTES.md` |
| `<mule-app-name>/` | One migrated module per Mule app, same directory name as the original |

**Adding a module never edits `pom.xml`.** The parent declares a profile per app that activates when
`spring-boot-migration/<app>/pom.xml` exists, so parallel tickets don't conflict on the module list.

## Tickets

| Ticket | Scope | Apps |
|---|---|---|
| 0 | Shared scaffold (this directory, `parity-test-support`) | – |
| 1 | REST API apps -> Spring Web + springdoc | 6 |
| 2 | SOAP & proxy apps -> Spring WS / WebClient | 5 |
| 3 | Messaging apps -> Spring JMS / AMQP | 5 |
| 4 | Salesforce apps -> Salesforce REST/Bulk API + Spring Batch | 6 |
| 5 | ERP/SaaS connectors -> typed WebClient service clients | 5 |
| 6 | Routing & transformation apps -> Spring Integration | 10 |
| 7 | File, email & legacy protocol apps -> Spring Integration adapters | 9 |
| 8 | Security & OAuth apps -> Spring Security | 3 |
| 9 | Database, batch & misc apps -> Spring Data / Spring Batch | 7 |

## Mule app → Spring Boot module

| Mule 3.8 app | Spring Boot module | Ticket | Status |
|---|---|---|---|
| [`rest-api-with-apikit`](../rest-api-with-apikit) | [`spring-boot-migration/rest-api-with-apikit`](./rest-api-with-apikit) | 1 | Planned |
| [`processing-orders-with-dataweave-and-APIkit`](../processing-orders-with-dataweave-and-APIkit) | [`spring-boot-migration/processing-orders-with-dataweave-and-APIkit`](./processing-orders-with-dataweave-and-APIkit) | 1 | Planned |
| [`exposing-a-restful-resource-using-the-HTTP-connector`](../exposing-a-restful-resource-using-the-HTTP-connector) | [`spring-boot-migration/exposing-a-restful-resource-using-the-HTTP-connector`](./exposing-a-restful-resource-using-the-HTTP-connector) | 1 | Planned |
| [`http-request-response-with-logger`](../http-request-response-with-logger) | [`spring-boot-migration/http-request-response-with-logger`](./http-request-response-with-logger) | 1 | Planned |
| [`http-multipart-request`](../http-multipart-request) | [`spring-boot-migration/http-multipart-request`](./http-multipart-request) | 1 | Planned |
| [`login-form-using-the-http-connector`](../login-form-using-the-http-connector) | [`spring-boot-migration/login-form-using-the-http-connector`](./login-form-using-the-http-connector) | 1 | Planned |
| [`xml-only-soap-webservice`](../xml-only-soap-webservice) | [`spring-boot-migration/xml-only-soap-webservice`](./xml-only-soap-webservice) | 2 | Planned |
| [`soap-webservice-security`](../soap-webservice-security) | [`spring-boot-migration/soap-webservice-security`](./soap-webservice-security) | 2 | Planned |
| [`web-service-consumer`](../web-service-consumer) | [`spring-boot-migration/web-service-consumer`](./web-service-consumer) | 2 | Planned |
| [`proxying-a-soap-api`](../proxying-a-soap-api) | [`spring-boot-migration/proxying-a-soap-api`](./proxying-a-soap-api) | 2 | Planned |
| [`proxying-a-rest-api`](../proxying-a-rest-api) | [`spring-boot-migration/proxying-a-rest-api`](./proxying-a-rest-api) | 2 | Planned |
| [`sending-json-data-to-a-jms-queue`](../sending-json-data-to-a-jms-queue) | [`spring-boot-migration/sending-json-data-to-a-jms-queue`](./sending-json-data-to-a-jms-queue) | 3 | Planned |
| [`sending-json-data-to-a-amqp-queue`](../sending-json-data-to-a-amqp-queue) | [`spring-boot-migration/sending-json-data-to-a-amqp-queue`](./sending-json-data-to-a-amqp-queue) | 3 | Planned |
| [`jms-message-rollback-and-redelivery`](../jms-message-rollback-and-redelivery) | [`spring-boot-migration/jms-message-rollback-and-redelivery`](./jms-message-rollback-and-redelivery) | 3 | Planned |
| [`using-transactional-scope-in-jms-to-database`](../using-transactional-scope-in-jms-to-database) | [`spring-boot-migration/using-transactional-scope-in-jms-to-database`](./using-transactional-scope-in-jms-to-database) | 3 | Planned |
| [`websphere-mq`](../websphere-mq) | [`spring-boot-migration/websphere-mq`](./websphere-mq) | 3 | Planned |
| [`authenticating-salesforce-using-oauth2`](../authenticating-salesforce-using-oauth2) | [`spring-boot-migration/authenticating-salesforce-using-oauth2`](./authenticating-salesforce-using-oauth2) | 4 | Planned |
| [`salesforce-data-retrieval`](../salesforce-data-retrieval) | [`spring-boot-migration/salesforce-data-retrieval`](./salesforce-data-retrieval) | 4 | Planned |
| [`import-contacts-into-salesforce`](../import-contacts-into-salesforce) | [`spring-boot-migration/import-contacts-into-salesforce`](./import-contacts-into-salesforce) | 4 | Planned |
| [`import-leads-into-salesforce`](../import-leads-into-salesforce) | [`spring-boot-migration/import-leads-into-salesforce`](./import-leads-into-salesforce) | 4 | Planned |
| [`salesforce-data-synchronization-using-watermarking-and-batch-processing`](../salesforce-data-synchronization-using-watermarking-and-batch-processing) | [`spring-boot-migration/salesforce-data-synchronization-using-watermarking-and-batch-processing`](./salesforce-data-synchronization-using-watermarking-and-batch-processing) | 4 | Planned |
| [`salesforce-to-MySQL-DB-using-Batch-Processing`](../salesforce-to-MySQL-DB-using-Batch-Processing) | [`spring-boot-migration/salesforce-to-MySQL-DB-using-Batch-Processing`](./salesforce-to-MySQL-DB-using-Batch-Processing) | 4 | Planned |
| [`get-customer-list-from-netsuite`](../get-customer-list-from-netsuite) | [`spring-boot-migration/get-customer-list-from-netsuite`](./get-customer-list-from-netsuite) | 5 | Planned |
| [`netsuite-data-retrieval`](../netsuite-data-retrieval) | [`spring-boot-migration/netsuite-data-retrieval`](./netsuite-data-retrieval) | 5 | Planned |
| [`import-contacts-into-ms-dynamics`](../import-contacts-into-ms-dynamics) | [`spring-boot-migration/import-contacts-into-ms-dynamics`](./import-contacts-into-ms-dynamics) | 5 | Planned |
| [`adding-a-new-customer-to-workday-revenue-management`](../adding-a-new-customer-to-workday-revenue-management) | [`spring-boot-migration/adding-a-new-customer-to-workday-revenue-management`](./adding-a-new-customer-to-workday-revenue-management) | 5 | Planned |
| [`sap-data-retrieval`](../sap-data-retrieval) | [`spring-boot-migration/sap-data-retrieval`](./sap-data-retrieval) | 5 | Planned |
| [`content-based-routing`](../content-based-routing) | [`spring-boot-migration/content-based-routing`](./content-based-routing) | 6 | Planned |
| [`scatter-gather-flow-control`](../scatter-gather-flow-control) | [`spring-boot-migration/scatter-gather-flow-control`](./scatter-gather-flow-control) | 6 | Planned |
| [`foreach-processing-and-choice-routing`](../foreach-processing-and-choice-routing) | [`spring-boot-migration/foreach-processing-and-choice-routing`](./foreach-processing-and-choice-routing) | 6 | Planned |
| [`service-orchestration-and-choice-routing`](../service-orchestration-and-choice-routing) | [`spring-boot-migration/service-orchestration-and-choice-routing`](./service-orchestration-and-choice-routing) | 6 | Planned |
| [`filtering-a-message`](../filtering-a-message) | [`spring-boot-migration/filtering-a-message`](./filtering-a-message) | 6 | Planned |
| [`dataweave-with-flowreflookup`](../dataweave-with-flowreflookup) | [`spring-boot-migration/dataweave-with-flowreflookup`](./dataweave-with-flowreflookup) | 6 | Planned |
| [`cache-scope-with-fibonacci`](../cache-scope-with-fibonacci) | [`spring-boot-migration/cache-scope-with-fibonacci`](./cache-scope-with-fibonacci) | 6 | Planned |
| [`mule-expression-language-basics`](../mule-expression-language-basics) | [`spring-boot-migration/mule-expression-language-basics`](./mule-expression-language-basics) | 6 | Planned |
| [`addition-using-javascript-transformer`](../addition-using-javascript-transformer) | [`spring-boot-migration/addition-using-javascript-transformer`](./addition-using-javascript-transformer) | 6 | Planned |
| [`mule-component-bindings`](../mule-component-bindings) | [`spring-boot-migration/mule-component-bindings`](./mule-component-bindings) | 6 | Planned |
| [`upload-to-ftp-after-converting-json-to-xml`](../upload-to-ftp-after-converting-json-to-xml) | [`spring-boot-migration/upload-to-ftp-after-converting-json-to-xml`](./upload-to-ftp-after-converting-json-to-xml) | 7 | Planned |
| [`importing-a-CSV-file-into-Mongo-DB`](../importing-a-CSV-file-into-Mongo-DB) | [`spring-boot-migration/importing-a-CSV-file-into-Mongo-DB`](./importing-a-CSV-file-into-Mongo-DB) | 7 | Planned |
| [`importing-a-csv-file-into-ms-sharepoint`](../importing-a-csv-file-into-ms-sharepoint) | [`spring-boot-migration/importing-a-csv-file-into-ms-sharepoint`](./importing-a-csv-file-into-ms-sharepoint) | 7 | Planned |
| [`importing-an-email-attachment-using-the-IMAP-connector`](../importing-an-email-attachment-using-the-IMAP-connector) | [`spring-boot-migration/importing-an-email-attachment-using-the-IMAP-connector`](./importing-an-email-attachment-using-the-IMAP-connector) | 7 | Planned |
| [`importing-an-email-attachment-using-the-POP3-connector`](../importing-an-email-attachment-using-the-POP3-connector) | [`spring-boot-migration/importing-an-email-attachment-using-the-POP3-connector`](./importing-an-email-attachment-using-the-POP3-connector) | 7 | Planned |
| [`sending-a-csv-file-through-email-using-smtp`](../sending-a-csv-file-through-email-using-smtp) | [`spring-boot-migration/sending-a-csv-file-through-email-using-smtp`](./sending-a-csv-file-through-email-using-smtp) | 7 | Planned |
| [`querying-a-db-and-attaching-results-to-an-email`](../querying-a-db-and-attaching-results-to-an-email) | [`spring-boot-migration/querying-a-db-and-attaching-results-to-an-email`](./querying-a-db-and-attaching-results-to-an-email) | 7 | Planned |
| [`extracting-data-from-LDAP-directory`](../extracting-data-from-LDAP-directory) | [`spring-boot-migration/extracting-data-from-LDAP-directory`](./extracting-data-from-LDAP-directory) | 7 | Planned |
| [`document-integration-using-the-cmis-connector`](../document-integration-using-the-cmis-connector) | [`spring-boot-migration/document-integration-using-the-cmis-connector`](./document-integration-using-the-cmis-connector) | 7 | Planned |
| [`oauth2-authorization-code-using-the-HTTP-connector`](../oauth2-authorization-code-using-the-HTTP-connector) | [`spring-boot-migration/oauth2-authorization-code-using-the-HTTP-connector`](./oauth2-authorization-code-using-the-HTTP-connector) | 8 | Planned |
| [`oauth2-client-credentials-using-the-HTTP-connector`](../oauth2-client-credentials-using-the-HTTP-connector) | [`spring-boot-migration/oauth2-client-credentials-using-the-HTTP-connector`](./oauth2-client-credentials-using-the-HTTP-connector) | 8 | Planned |
| [`http-oauth-provider`](../http-oauth-provider) | [`spring-boot-migration/http-oauth-provider`](./http-oauth-provider) | 8 | Planned |
| [`querying-a-mysql-database`](../querying-a-mysql-database) | [`spring-boot-migration/querying-a-mysql-database`](./querying-a-mysql-database) | 9 | Planned |
| [`legacy-modernization`](../legacy-modernization) | [`spring-boot-migration/legacy-modernization`](./legacy-modernization) | 9 | Planned |
| [`track-a-custom-business-event`](../track-a-custom-business-event) | [`spring-boot-migration/track-a-custom-business-event`](./track-a-custom-business-event) | 9 | Planned |
| [`implementing-a-choice-exception-strategy`](../implementing-a-choice-exception-strategy) | [`spring-boot-migration/implementing-a-choice-exception-strategy`](./implementing-a-choice-exception-strategy) | 9 | Planned |
| [`hello-world`](../hello-world) | [`spring-boot-migration/hello-world`](./hello-world) | 9 | Planned |
| [`munit-short-tutorial`](../munit-short-tutorial) | [`spring-boot-migration/munit-short-tutorial`](./munit-short-tutorial) | 9 | Planned |
| [`testing-apikit-with-munit`](../testing-apikit-with-munit) | [`spring-boot-migration/testing-apikit-with-munit`](./testing-apikit-with-munit) | 9 | Planned |

Ticket PRs update the **Status** column for their apps (`Migrated`, or `Migrated (deltas)` when the module README lists
behavioural differences).
