package org.mule.examples.springboot.authenticatingsalesforceusingoauth2;

import org.mule.examples.springboot.authenticatingsalesforceusingoauth2.salesforce.SalesforceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class SalesforceConfig {

    @Bean
    SalesforceClient salesforceClient(RestClient.Builder builder, SalesforceOAuth oauth,
                                      @Value("${sfdc.api-version}") String apiVersion) {
        return new SalesforceClient(builder.build(), oauth, apiVersion);
    }
}
