package com.sports.tracker.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI documentation.
 * <p>
 * This class sets up the OpenAPI specification using Springdoc for the Sports Tracker microservice.
 * It includes basic metadata and a link to the project repository.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sportsTrackerOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Sports Tracker API")
                                .description("API documentation for the Sports Tracker microservice.")
                                .version("v1.0.0")
                                .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://github.com/swepsa/sports-tracker"));
    }

}
