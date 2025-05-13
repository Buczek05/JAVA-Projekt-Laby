package com.example.budget.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI budgetOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Budget Application API")
                        .description("Spring Boot REST API for Budget Management")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Budget Team")
                                .email("contact@budgetapp.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}