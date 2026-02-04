package com.example.importleads;

import com.example.importleads.service.SalesforceService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "salesforce.username=test@test.com",
    "salesforce.password=testpassword",
    "salesforce.security-token=testtoken",
    "salesforce.login-url=https://test.salesforce.com",
    "file.polling.input-directory=target/test-input",
    "file.polling.output-directory=target/test-output"
})
class ImportLeadsApplicationTests {

    @MockBean
    private SalesforceService salesforceService;

    @Test
    void contextLoads() {
    }
}
