package com.training.service.invoiceextractor.configuration;

import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.ILlmExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final boolean llmEnabled;

    public LlmConfiguration(
            ILlmExtractionService llmExtractionService,
            @Value("${llm.groq.enabled:false}") boolean llmEnabled
    ) {
        this.llmExtractionService = llmExtractionService;
        this.llmEnabled = llmEnabled;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (llmEnabled) {
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
