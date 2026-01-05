package com.training.service.invoiceextractor.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an extraction process fails.
 *
 * @param extractionKey Unique extraction identifier
 * @param errorMessage Error description
 * @param errorCode Error code (optional)
 * @param timestamp When the extraction failed
 */
public record ExtractionFailedEvent(
        UUID extractionKey,
        String errorMessage,
        String errorCode,
        LocalDateTime timestamp
) implements ExtractionEvent {

    public ExtractionFailedEvent(UUID extractionKey, String errorMessage) {
        this(extractionKey, errorMessage, null, LocalDateTime.now());
    }

    public ExtractionFailedEvent(UUID extractionKey, String errorMessage, String errorCode) {
        this(extractionKey, errorMessage, errorCode, LocalDateTime.now());
    }

    @Override
    public ExtractionEventType eventType() {
        return ExtractionEventType.EXTRACTION_FAILED;
    }
}
