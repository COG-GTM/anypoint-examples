# Migration notes: `xml-only-soap-webservice`

- Original Mule app: [`../../xml-only-soap-webservice`](../../xml-only-soap-webservice)
- Mule flows: `Hospital_Admissions_SOA.xml` → `admitPatientService`, sub-flows `upsertPatient`, `invokePatientService`, `createEpisode`, `invokeEHRService`; `mocks.xml` → `PatientService`, `EHRService`
- Spring Boot module: `spring-boot-migration/xml-only-soap-webservice`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener-config port="${http.port}"` | `server.port: ${http.port:8081}` | |
| `http:listener path="AdmissionService" / "PatientService" / "EHRService"` | `WebServiceConfiguration#messageDispatcherServlet` (3 exact mappings) | Boot's `WebServicesAutoConfiguration` excluded |
| `cxf:proxy-service payload="envelope"` (Admission) | Spring-WS `MessageDispatcherServlet` + `AdmissionServiceEndpoint` (`@PayloadRoot admitSubject`) | empty `soap:Header` stripped (`EmptyHeaderRemover`) |
| `cxf:proxy-service payload="body"` (Patient/EHR) | `PatientServiceEndpoint`, `EhrServiceEndpoint` | |
| `set-variable operation = xpath('fn:local-name(/*)')` + `choice` | one `@PayloadRoot` method per operation | |
| `mulexml:namespace-manager` | `Hospital.MSG` / `Hospital.MOD` | |
| `dw` `admitSubject to upsertPatient` | `AdmissionServiceEndpoint#admitSubject` (Subject children copied) | |
| `cxf:proxy-client payload="body"` + `http:request POST PatientService/EHRService` | `HospitalServiceClient` (`WebServiceTemplate` to own port) | |
| `dw` `upsertPatientResponse to createEpisode` | `AdmissionServiceEndpoint#admitSubject` | |
| `dw` `createEpisodeResponse to admitSubjectResponse` (`as :date`, `+ \|P5D\|`, Bill) | `AdmissionServiceEndpoint#admitSubject`, `asDate(..).plusDays(5)` | |
| `dw` upsertPatientResponse (`now as :string`) / getPatientResponse | `PatientServiceEndpoint` | |
| `dw` createEpisodeResponse / findEpisodesResponse | `EhrServiceEndpoint` | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `HTTP_Request_Configuration` host `0.0.0.0` port `${http.port}` | `hospital.services.base-url` | blank = `http://localhost:<server port>` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-admit-subject` | IT envelope → full orchestration → admitSubjectResponse (Episode + Bill) | documented |
| `02-patient-upsert` | upsertPatient → PatientId | documented |
| `03-patient-get` | getPatient → Donald Duck patient | documented |
| `04-ehr-create-episode` | createEpisode → Episode echoing PatientId | documented |
| `05-ehr-find-episodes` | findEpisodes → Episode with PatientId | documented |

## Behavioural deltas

See [README](README.md#behavioural-deltas): Bill names follow DataWeave, unknown operations are faults, no `?wsdl`, timestamp format.
