package com.training.service.invoiceextractor.domain.events;

import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.InvoiceData;
import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publisher for extraction-related domain events.
 * Uses Spring's ApplicationEventPublisher to implement the Observer Pattern.
 *
 * <p><b>Design Pattern:</b> Observer Pattern (Publisher/Subject)
 * <p><b>SOLID Principles:</b>
 * <ul>
 *   <li>Single Responsibility: Only publishes events, doesn't handle them</li>
 *   <li>Open/Closed: New event types can be added without modifying this class</li>
 *   <li>Dependency Inversion: Depends on Spring's abstraction</li>
 * </ul>
 *
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Decouples event producers from consumers</li>
 *   <li>Multiple listeners can react to same event</li>
 *   <li>Easy to add new event handlers without modifying existing code</li>
 *   <li>Asynchronous processing support via @Async</li>
 * </ul>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-22
 */
@Component("domainExtractionEventPublisher")
@Slf4j
@RequiredArgsConstructor
public class ExtractionEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publishes an extraction started event.
     *
     * @param extractionKey Unique extraction identifier
     * @param fileName Name of the file being processed
     */
    public void publishExtractionStarted(UUID extractionKey, String fileName) {
        log.debug("Publishing extraction started event: {}", extractionKey);
        ExtractionStartedEvent event = new ExtractionStartedEvent(extractionKey, fileName);
        eventPublisher.publishEvent(event);
    }

    /**
     * Publishes an OCR completed event.
     *
     * @param extractionKey Unique extraction identifier
     * @param ocrResult OCR extraction result
     */
    public void publishOcrCompleted(UUID extractionKey, OcrResult ocrResult) {
        log.debug("Publishing OCR completed event: {} - confidence: {}",
                extractionKey, ocrResult.confidenceScore());
        OcrCompletedEvent event = new OcrCompletedEvent(extractionKey, ocrResult);
        eventPublisher.publishEvent(event);
    }

    /**
     * Publishes an LLM completed event.
     *
     * @param extractionKey Unique extraction identifier
     * @param invoiceData Extracted invoice data
     */
    public void publishLlmCompleted(UUID extractionKey, InvoiceData invoiceData) {
        log.debug("Publishing LLM completed event: {} - confidence: {}",
                extractionKey, invoiceData.confidence());
        LlmCompletedEvent event = new LlmCompletedEvent(extractionKey, invoiceData);
        eventPublisher.publishEvent(event);
    }

    /**
     * Publishes an invoice saved event.
     *
     * @param extractionKey Unique extraction identifier
     * @param invoiceKey Unique invoice identifier
     */
    public void publishInvoiceSaved(UUID extractionKey, UUID invoiceKey) {
        log.debug("Publishing invoice saved event: {} - invoice: {}", extractionKey, invoiceKey);
        InvoiceSavedEvent event = new InvoiceSavedEvent(extractionKey, invoiceKey);
        eventPublisher.publishEvent(event);
    }

    /**
     * Publishes an extraction completed event.
     *
     * @param extractionKey Unique extraction identifier
     * @param invoiceKey Unique invoice identifier
     * @param confidenceScore Overall confidence score
     */
    public void publishExtractionCompleted(UUID extractionKey, UUID invoiceKey, double confidenceScore) {
        log.debug("Publishing extraction completed event: {} - confidence: {}",
                extractionKey, confidenceScore);
        ExtractionCompletedEvent event = new ExtractionCompletedEvent(
                extractionKey,
                invoiceKey,
                confidenceScore
        );
        eventPublisher.publishEvent(event);
    }

    /**
     * Publishes an extraction failed event.
     *
     * @param extractionKey Unique extraction identifier
     * @param errorMessage Error description
     */
    public void publishExtractionFailed(UUID extractionKey, String errorMessage) {
        log.debug("Publishing extraction failed event: {} - error: {}", extractionKey, errorMessage);
        ExtractionFailedEvent event = new ExtractionFailedEvent(extractionKey, errorMessage);
        eventPublisher.publishEvent(event);
    }

    /**
     * Publishes an extraction failed event with error code.
     *
     * @param extractionKey Unique extraction identifier
     * @param errorMessage Error description
     * @param errorCode Error code
     */
    public void publishExtractionFailed(UUID extractionKey, String errorMessage, String errorCode) {
        log.debug("Publishing extraction failed event: {} - error: {} ({})",
                extractionKey, errorMessage, errorCode);
        ExtractionFailedEvent event = new ExtractionFailedEvent(extractionKey, errorMessage, errorCode);
        eventPublisher.publishEvent(event);
    }

    /**
     * Publishes a generic extraction event.
     * Useful for custom event types or event forwarding.
     *
     * @param event The event to publish
     */
    public void publishEvent(ExtractionEvent event) {
        log.debug("Publishing extraction event: {} - type: {}",
                event.extractionKey(), event.eventType());
        eventPublisher.publishEvent(event);
    }
}
