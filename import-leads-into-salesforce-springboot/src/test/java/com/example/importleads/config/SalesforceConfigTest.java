package com.example.importleads.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SalesforceConfigTest {

    @Test
    void getPasswordWithToken_bothSet_returnsConcatenated() {
        SalesforceConfig config = new SalesforceConfig();
        config.setPassword("mypassword");
        config.setSecurityToken("mytoken");
        
        assertEquals("mypasswordmytoken", config.getPasswordWithToken());
    }

    @Test
    void getPasswordWithToken_tokenNull_returnsPasswordOnly() {
        SalesforceConfig config = new SalesforceConfig();
        config.setPassword("mypassword");
        config.setSecurityToken(null);
        
        assertEquals("mypassword", config.getPasswordWithToken());
    }

    @Test
    void defaultLoginUrl_isLoginSalesforce() {
        SalesforceConfig config = new SalesforceConfig();
        
        assertEquals("https://login.salesforce.com", config.getLoginUrl());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        SalesforceConfig config = new SalesforceConfig();
        
        config.setUsername("user@test.com");
        config.setPassword("password123");
        config.setSecurityToken("token456");
        config.setLoginUrl("https://test.salesforce.com");
        
        assertEquals("user@test.com", config.getUsername());
        assertEquals("password123", config.getPassword());
        assertEquals("token456", config.getSecurityToken());
        assertEquals("https://test.salesforce.com", config.getLoginUrl());
    }
}
