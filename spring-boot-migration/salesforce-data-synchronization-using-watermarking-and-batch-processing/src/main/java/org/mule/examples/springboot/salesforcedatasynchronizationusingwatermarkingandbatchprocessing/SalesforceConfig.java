package org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing;

import org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing.salesforce.SalesforceClient;
import org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing.salesforce.SalesforcePasswordSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** The Mule {@code sfdc:config name="Salesforce"} global element. */
@Configuration(proxyBeanMethods = false)
class SalesforceConfig {

    @Bean
    SalesforcePasswordSession salesforceSession(RestClient.Builder builder,
                                                @Value("${sfdc.login-url}") String loginUrl,
                                                @Value("${sfdc.consumerKey}") String consumerKey,
                                                @Value("${sfdc.consumerSecret}") String consumerSecret,
                                                @Value("${sfdc.user}") String user,
                                                @Value("${sfdc.password}") String password,
                                                @Value("${sfdc.securityToken}") String securityToken) {
        return new SalesforcePasswordSession(builder.build(), loginUrl, consumerKey, consumerSecret, user, password, securityToken);
    }

    @Bean
    SalesforceClient salesforceClient(RestClient.Builder builder, SalesforcePasswordSession session,
                                      @Value("${sfdc.api-version}") String apiVersion) {
        return new SalesforceClient(builder.build(), session, apiVersion);
    }
}
