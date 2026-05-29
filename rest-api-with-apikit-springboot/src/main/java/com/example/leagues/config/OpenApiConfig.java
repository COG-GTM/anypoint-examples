package com.example.leagues.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI metadata.
 *
 * <p>Swagger UI (served at {@code /swagger-ui.html}) is the modern equivalent
 * of the MuleSoft APIkit console, providing interactive documentation for the
 * API that was previously described by {@code leagues.raml}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI leaguesOpenApi() {
        return new OpenAPI().info(new Info()
                .title("La Liga API")
                .version("1.0")
                .description("Spring Boot migration of the MuleSoft \"REST API with APIkit\" "
                        + "example. Add, remove and retrieve information about teams, "
                        + "standings and fixtures in a league tournament."));
    }
}
