package org.mule.examples.springboot.netsuitedataretrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The Mule {@code netsuite:config-login-authentication} attributes ({@code nets.*}) plus the endpoint URL. */
@ConfigurationProperties(prefix = "nets")
public record NetSuiteProperties(String baseUrl, String email, String password, String account, String roleId,
                                 String applicationId) {
}
