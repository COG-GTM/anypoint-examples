package org.mule.examples.springboot.getcustomerlistfromnetsuite;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The Mule {@code netsuite:config} attributes ({@code netsuite.*} in mule-app.properties) plus the endpoint URL. */
@ConfigurationProperties(prefix = "netsuite")
public record NetSuiteProperties(String baseUrl, String email, String password, String account, String roleId) {
}
