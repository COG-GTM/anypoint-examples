# xml-only-soap-webservice (Spring Boot)

Spring Boot 3 migration of the Mule 3.8 example [`xml-only-soap-webservice`](../../xml-only-soap-webservice): a hospital
admission SOAP service built only from XML transformations, with no generated JAXB classes. One listener serves three SOAP 1.1 services:

| Path | Mule flow | Operations |
|---|---|---|
| `/AdmissionService` | `admitPatientService` | `admitSubject` → calls PatientService `upsertPatient`, then EHRService `createEpisode`, returns `admitSubjectResponse` (Episode + Bill) |
| `/PatientService` | `PatientService` (`mocks.xml`) | `upsertPatient`, `getPatient` |
| `/EHRService` | `EHRService` (`mocks.xml`) | `createEpisode`, `findEpisodes` |

Implementation: one Spring-WS `MessageDispatcherServlet` mapped to the three paths. There is one `@Endpoint` class per
service, with `@PayloadRoot` methods that work on DOM `Element` payloads (no JAXB, to stay "XML only"). The Admission
endpoint calls the other two over HTTP on the app's own port with `WebServiceTemplate`, as Mule's
`cxf:proxy-client` + `http:request` did.

## Run

```bash
cd spring-boot-migration
mvn -B -pl xml-only-soap-webservice -am verify
java -jar xml-only-soap-webservice/target/xml-only-soap-webservice-1.0.0-SNAPSHOT.jar --http.port=8081
curl -H 'Content-Type: text/xml' --data @../xml-only-soap-webservice/src/test/resources/message.xml http://localhost:8081/AdmissionService
```

| Property | Default | Meaning |
|---|---|---|
| `http.port` | `8081` | Listener port |
| `hospital.services.base-url` | blank (= `http://localhost:<server port>`) | Where Admission calls PatientService/EHRService (Mule request config `0.0.0.0:${http.port}`) |

## Parity tests

`XmlOnlySoapWebserviceParityTest` (`AbstractParityTest`) sends SOAP envelopes, including the original IT `message.xml`,
to the three services and compares the responses with XMLUnit. Values that Mule generates from `now`
(ids, dates) are matched with `~regex` text nodes. `ApplicationStartupTest` checks that the app starts with production defaults.

## Behavioural deltas

- **Bill element names follow the flow, not `reply.xml`.** The DataWeave writes `initialStateEstimate` (and the key
  `"Bill "`, whose trailing space is not part of the XML element name). The hand-written `src/test/resources/reply.xml`
  in the Mule app says `initialStayEstimate`. The Spring app follows the DataWeave, which is what Mule actually returned.
- **Unknown operations.** Mule's mock flows sent *any* non-`upsertPatient` / non-`createEpisode` body to their
  `otherwise` branch. Spring-WS routes only the operations defined in the WSDLs (`getPatient`, `findEpisodes`), and any
  other payload gets a SOAP fault.
- **No `?wsdl` publishing.** `cxf:proxy-service` served the WSDL at `<path>?wsdl`. The WSDLs/XSDs are kept under
  `src/main/resources/service` but are not published.
- **Timestamp format.** `now` / `now as :string` is written as `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` in the server's zone.
  DataWeave 1.0's exact rendering is not documented, so fixtures match any ISO-8601 timestamp.
