package com.disasterreliefnet.disaster.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI disasterServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("DisasterReliefNet :: Disaster Service API")
                .description("API documentation for Disaster incidents, geospatial tagging, and severity updates")
                .version("1.0.0")
                .contact(new Contact().name("DisasterReliefNet Platform Team").email("platform@disasterreliefnet.org"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
