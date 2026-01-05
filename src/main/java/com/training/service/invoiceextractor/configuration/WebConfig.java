package com.training.service.invoiceextractor.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for CORS (Cross-Origin Resource Sharing) and WebClient.
 * Allows the Angular frontend to communicate with this backend API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures WebClient.Builder bean for HTTP client operations.
     * Used by services that need to make HTTP requests (e.g., LLM services).
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB buffer for large responses
                .defaultHeader("User-Agent", "Invoice-Extractor-Service/1.0");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost",                   // Docker Nginx frontend (port 80)
                        "http://localhost:4200",              // Angular dev server
                        "https://*.vercel.app",               // Production frontend (Vercel)
                        "https://*.netlify.app"               // Production frontend (Netlify)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
