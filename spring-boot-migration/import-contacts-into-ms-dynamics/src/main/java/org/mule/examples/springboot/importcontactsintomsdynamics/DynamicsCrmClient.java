package org.mule.examples.springboot.importcontactsintomsdynamics;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Typed replacement for the Mule {@code dynamicscrm:create logicalName="contact"} operation: POSTs the contact
 * attributes to the Dynamics 365 Web API entity set {@code contacts}.
 */
@Component
public class DynamicsCrmClient {

    private final WebClient webClient;
    private final String contactsPath;

    public DynamicsCrmClient(WebClient.Builder builder, CrmProperties properties) {
        WebClient.Builder b = builder.baseUrl(properties.baseUrl())
                .defaultHeader("OData-MaxVersion", "4.0")
                .defaultHeader("OData-Version", "4.0");
        if (StringUtils.hasText(properties.accessToken())) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken());
        }
        this.webClient = b.build();
        this.contactsPath = "/api/data/" + properties.apiVersion() + "/contacts";
    }

    /** Creates the contact and returns the {@code OData-EntityId} of the new record (may be {@code null}). */
    public String createContact(DynamicsContact contact) {
        return webClient.post()
                .uri(contactsPath)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(contact)
                .retrieve()
                .toBodilessEntity()
                .map(r -> r.getHeaders().getFirst("OData-EntityId"))
                .block();
    }
}
