package com.disasterreliefnet.donor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI donorServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("DisasterReliefNet :: Donor Service API")
                .description("API documentation for donor self-registration, pledges, itemized aid lines, impact metrics, and tax receipts")
                .version("1.0.0")
                .contact(new Contact().name("DisasterReliefNet Platform Team").email("platform@disasterreliefnet.org"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
