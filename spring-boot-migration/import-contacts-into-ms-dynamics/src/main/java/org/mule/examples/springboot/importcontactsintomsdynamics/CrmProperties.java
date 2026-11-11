package org.mule.examples.springboot.importcontactsintomsdynamics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The Mule {@code dynamicscrm:config} ({@code crm.*} in mule-app.properties). {@code baseUrl} is the organisation URL
 * (the Mule {@code crm.service-url} without the {@code /XRMServices/...} suffix); {@code accessToken} is an OAuth 2.0
 * bearer token for the Dynamics Web API.
 */
@ConfigurationProperties(prefix = "crm")
public record CrmProperties(String baseUrl, String apiVersion, String accessToken) {
}
