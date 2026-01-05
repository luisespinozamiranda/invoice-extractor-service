package com.training.service.invoiceextractor.configuration;

import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.ILlmExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class for LLM (Large Language Model) extraction service.
 * Validates LLM configuration on application startup.
 */
@Configuration
@Slf4j
public class LlmConfiguration {

    private final ILlmExtractionService llmExtractionService;
    private final GroqProperties groqProperties;

    public LlmConfiguration(
            ILlmExtractionService llmExtractionService,
            GroqProperties groqProperties
    ) {
        this.llmExtractionService = llmExtractionService;
        this.groqProperties = groqProperties;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (groqProperties.isEnabled()) {
            if (llmExtractionService.isAvailable()) {
                log.info("✓ LLM extraction service is enabled and available: {}",
                        llmExtractionService.getProviderName());
            } else {
                log.warn("⚠ LLM extraction is enabled but service is not available. " +
                        "Check API key configuration.");
                log.warn("  Falling back to regex-based extraction.");
            }
        } else {
            log.info("LLM extraction is disabled. Using regex-based extraction.");
        }
    }
}
