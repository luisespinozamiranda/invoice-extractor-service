package com.training.service.invoiceextractor.adapter.outbound.llm.v1_0;

import java.util.concurrent.CompletableFuture;

/**
 * Port interface for LLM-based invoice data extraction.
 * <p>
 * This interface defines the contract for extracting structured invoice data
 * from raw OCR text using Large Language Models (LLMs).
 * <p>
 * The implementation is provider-agnostic, allowing different LLM providers
 * (Groq, OpenAI, Claude, etc.) to be swapped without changing domain logic.
 * <p>
 * Implementations should:
 * - Send OCR text to LLM with structured prompts
 * - Parse LLM response into InvoiceData
 * - Handle errors gracefully with fallback strategies
 * - Return confidence scores for quality assessment
 */
public interface ILlmExtractionService {

    /**
     * Extract structured invoice data from OCR text using LLM.
     *
     * @param ocrText Raw text extracted from invoice by OCR
     * @return CompletableFuture with InvoiceData containing extracted fields
     */
    CompletableFuture<InvoiceData> extractInvoiceData(String ocrText);

    /**
     * Check if the LLM service is available and configured.
     *
     * @return true if LLM service is ready to use, false otherwise
     */
    boolean isAvailable();

    /**
     * Get the name of the LLM provider being used.
     *
     * @return Provider name (e.g., "Groq", "OpenAI", "Claude")
     */
    String getProviderName();
}
