package org.mule.examples.springboot.sapdataretrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The Mule {@code sap:connector} attributes ({@code sap.jco.*} in mule-app.properties) plus the URL of the HTTP
 * gateway that executes the BAPIs.
 */
@ConfigurationProperties(prefix = "sap")
public record SapProperties(String gatewayBaseUrl, Jco jco) {

    public record Jco(String ashost, String client, String lang, String passwd, String sysnr, String user) {
    }
}
