package com.training.service.invoiceextractor.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Groq LLM service.
 * Binds properties with prefix "llm.groq" from application.properties/yml.
 */
@Configuration
@ConfigurationProperties(prefix = "llm.groq")
@Getter
@Setter
public class GroqProperties {

    /**
     * Groq API key for authentication.
     */
    private String apiKey;

    /**
     * Groq model to use for extraction (e.g., llama-3.1-70b-versatile).
     */
    private String model = "llama-3.1-70b-versatile";

    /**
     * Whether LLM extraction is enabled.
     */
    private boolean enabled = false;

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Temperature for LLM responses (0.0 to 1.0).
     */
    private double temperature = 0.1;

    /**
     * Maximum tokens in LLM response.
     */
    private int maxTokens = 2048;
}
