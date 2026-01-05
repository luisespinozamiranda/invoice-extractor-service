package com.training.service.invoiceextractor.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when the entire extraction process completes successfully.
 *
 * @param extractionKey Unique extraction identifier
 * @param invoiceKey Unique invoice identifier
 * @param confidenceScore Overall confidence score
 * @param timestamp When the extraction completed
 */
public record ExtractionCompletedEvent(
        UUID extractionKey,
        UUID invoiceKey,
        double confidenceScore,
        LocalDateTime timestamp
) implements ExtractionEvent {

    public ExtractionCompletedEvent(UUID extractionKey, UUID invoiceKey, double confidenceScore) {
        this(extractionKey, invoiceKey, confidenceScore, LocalDateTime.now());
    }

    @Override
    public ExtractionEventType eventType() {
        return ExtractionEventType.EXTRACTION_COMPLETED;
    }
}
