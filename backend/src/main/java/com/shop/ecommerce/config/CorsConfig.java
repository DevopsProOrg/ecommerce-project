package com.shop.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://127.0.0.1:5500,http://localhost:5500}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {

                registry.addMapping("/**")
                        .allowedOrigins(parseAllowedOrigins())
                        .allowedOriginPatterns(parseAllowedOriginPatterns())
                        .allowedMethods("*")
                        .allowedHeaders("*");

            }

        };
    }

    private String[] parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }

    private String[] parseAllowedOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .toArray(String[]::new);
    }
}
