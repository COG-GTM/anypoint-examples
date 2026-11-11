package org.mule.examples.springboot.importcontactsintosalesforce.salesforce;

/** An access token and the instance it is valid for (OAuth2 {@code access_token} / {@code instance_url}). */
public record SalesforceToken(String accessToken, String instanceUrl, String refreshToken) {

    public SalesforceToken(String accessToken, String instanceUrl) {
        this(accessToken, instanceUrl, null);
    }
}
