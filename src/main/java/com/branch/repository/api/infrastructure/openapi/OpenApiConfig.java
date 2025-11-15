package com.branch.repository.api.infrastructure.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI repositoryApiOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Repository API")
                .description("API for retrieving GitHub developer profiles and repositories")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Branch")
                    .url("https://branch.io")))
            .servers(List.of(
                new Server()
                    .url("https://localhost:8443")
                    .description("Local HTTPS Server"),
                new Server()
                    .url("http://localhost:8080")
                    .description("Local HTTP Server (redirects to HTTPS)")
            ));
    }
}