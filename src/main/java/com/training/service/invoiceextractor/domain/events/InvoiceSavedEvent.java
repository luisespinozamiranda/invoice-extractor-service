package com.training.service.invoiceextractor.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an invoice is successfully saved.
 *
 * @param extractionKey Unique extraction identifier
 * @param invoiceKey Unique invoice identifier
 * @param timestamp When the invoice was saved
 */
public record InvoiceSavedEvent(
        UUID extractionKey,
        UUID invoiceKey,
        LocalDateTime timestamp
) implements ExtractionEvent {

    public InvoiceSavedEvent(UUID extractionKey, UUID invoiceKey) {
        this(extractionKey, invoiceKey, LocalDateTime.now());
    }

    @Override
    public ExtractionEventType eventType() {
        return ExtractionEventType.INVOICE_SAVED;
    }
}
