package com.training.service.invoiceextractor.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an extraction process starts.
 *
 * @param extractionKey Unique extraction identifier
 * @param fileName Name of the file being processed
 * @param timestamp When the extraction started
 */
public record ExtractionStartedEvent(
        UUID extractionKey,
        String fileName,
        LocalDateTime timestamp
) implements ExtractionEvent {

    public ExtractionStartedEvent(UUID extractionKey, String fileName) {
        this(extractionKey, fileName, LocalDateTime.now());
    }

    @Override
    public ExtractionEventType eventType() {
        return ExtractionEventType.EXTRACTION_STARTED;
    }
}
