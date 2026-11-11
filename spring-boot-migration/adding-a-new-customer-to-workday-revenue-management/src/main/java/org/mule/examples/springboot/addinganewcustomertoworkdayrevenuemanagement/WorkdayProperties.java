package org.mule.examples.springboot.addinganewcustomertoworkdayrevenuemanagement;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The Mule {@code wd-connector:config} ({@code wday.*} in mule-app.properties) plus the endpoint and API version. */
@ConfigurationProperties(prefix = "wday")
public record WorkdayProperties(String baseUrl, String hostname, String tenant, String user, String password,
                                String version) {
}
