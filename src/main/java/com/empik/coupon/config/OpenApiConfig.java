package com.empik.coupon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI couponServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Coupon Service API")
                .version("v1")
                .description("REST API for managing and redeeming discount coupons")
                .contact(new Contact().name("Empik recruitment task")));
    }
}
