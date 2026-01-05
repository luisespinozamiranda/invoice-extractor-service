package com.training.service.invoiceextractor.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base sealed interface for all extraction-related events.
 * Uses Java 17 sealed interfaces to define a closed hierarchy of event types.
 *
 * <p><b>Design Pattern:</b> Observer Pattern (Event)
 * <p><b>SOLID Principles:</b>
 * <ul>
 *   <li>Open/Closed: Sealed hierarchy allows specific event types while preventing unauthorized extensions</li>
 *   <li>Liskov Substitution: All implementations can be used polymorphically</li>
 * </ul>
 *
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Type-safe event handling</li>
 *   <li>Pattern matching support in switch expressions</li>
 *   <li>Compiler-enforced exhaustiveness checks</li>
 *   <li>Clear event taxonomy</li>
 * </ul>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-22
 */
public sealed interface ExtractionEvent permits
        ExtractionStartedEvent,
        OcrCompletedEvent,
        LlmCompletedEvent,
        InvoiceSavedEvent,
        ExtractionCompletedEvent,
        ExtractionFailedEvent {

    /**
     * Returns the extraction key associated with this event.
     *
     * @return Extraction key
     */
    UUID extractionKey();

    /**
     * Returns the timestamp when this event occurred.
     *
     * @return Event timestamp
     */
    LocalDateTime timestamp();

    /**
     * Returns the type of this event.
     *
     * @return Event type
     */
    ExtractionEventType eventType();

    /**
     * Enumeration of extraction event types.
     */
    enum ExtractionEventType {
        EXTRACTION_STARTED,
        OCR_COMPLETED,
        LLM_COMPLETED,
        INVOICE_SAVED,
        EXTRACTION_COMPLETED,
        EXTRACTION_FAILED
    }
}
