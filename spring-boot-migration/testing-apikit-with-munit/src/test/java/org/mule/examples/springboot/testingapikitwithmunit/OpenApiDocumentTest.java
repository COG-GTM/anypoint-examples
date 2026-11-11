package org.mule.examples.springboot.testingapikitwithmunit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocumentTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void documentsEveryRamlMethodOfMunitResource() {
        JsonNode munit = rest.getForObject("/v3/api-docs", JsonNode.class).path("paths").path("/api/munit");

        assertThat(munit.path("get").path("responses").has("200")).isTrue();
        assertThat(munit.path("post").path("responses").has("201")).isTrue();
        assertThat(munit.path("put").path("responses").has("201")).isTrue();
        assertThat(munit.path("delete").path("responses").has("200")).isTrue();
    }
}
