package com.training.service.invoiceextractor.domain.events;

import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.InvoiceData;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when LLM extraction completes successfully.
 *
 * @param extractionKey Unique extraction identifier
 * @param invoiceData Extracted invoice data from LLM
 * @param timestamp When the LLM extraction completed
 */
public record LlmCompletedEvent(
        UUID extractionKey,
        InvoiceData invoiceData,
        LocalDateTime timestamp
) implements ExtractionEvent {

    public LlmCompletedEvent(UUID extractionKey, InvoiceData invoiceData) {
        this(extractionKey, invoiceData, LocalDateTime.now());
    }

    @Override
    public ExtractionEventType eventType() {
        return ExtractionEventType.LLM_COMPLETED;
    }

    public double confidence() {
        return invoiceData.confidence();
    }

    public boolean isValid() {
        return invoiceData.isValid();
    }
}
