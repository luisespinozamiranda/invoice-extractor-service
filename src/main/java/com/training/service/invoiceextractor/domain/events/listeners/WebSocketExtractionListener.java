package com.training.service.invoiceextractor.domain.events.listeners;

import com.training.service.invoiceextractor.adapter.inbound.websocket.dto.ExtractionEventV1_0;
import com.training.service.invoiceextractor.domain.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener that broadcasts extraction events via WebSocket.
 * Implements the Observer Pattern as a concrete observer.
 *
 * <p><b>Design Pattern:</b> Observer Pattern (Concrete Observer)
 * <p><b>SOLID Principles:</b>
 * <ul>
 *   <li>Single Responsibility: Only handles WebSocket broadcasting</li>
 *   <li>Open/Closed: New event types can be handled by adding methods</li>
 * </ul>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-22
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketExtractionListener {

    private final SimpMessagingTemplate messagingTemplate;
    private static final String TOPIC_PREFIX = "/topic/extraction/";

    /**
     * Handles extraction started events.
     */
    @Async
    @EventListener
    public void handleExtractionStarted(ExtractionStartedEvent event) {
        log.info("WebSocket: Broadcasting extraction started - {}", event.extractionKey());

        ExtractionEventV1_0 wsEvent = ExtractionEventV1_0.builder()
                .type("EXTRACTION_STARTED")
                .extractionKey(event.extractionKey())
                .status("PROCESSING")
                .progress(0)
                .message("Extraction started for file: " + event.fileName())
                .timestamp(event.timestamp())
                .build();

        broadcastEvent(event.extractionKey(), wsEvent);
    }

    /**
     * Handles OCR completed events.
     */
    @Async
    @EventListener
    public void handleOcrCompleted(OcrCompletedEvent event) {
        log.info("WebSocket: Broadcasting OCR completed - {} (confidence: {})",
                event.extractionKey(), event.confidenceScore());

        String message = String.format("OCR completed with %.2f%% confidence (%d pages)",
                event.confidenceScore() * 100,
                event.pageCount());

        ExtractionEventV1_0 wsEvent = ExtractionEventV1_0.builder()
                .type("OCR_COMPLETED")
                .extractionKey(event.extractionKey())
                .status("PROCESSING")
                .progress(40)
                .message(message)
                .timestamp(event.timestamp())
                .metadata(java.util.Map.of(
                        "pageCount", event.pageCount(),
                        "confidenceScore", event.confidenceScore()
                ))
                .build();

        broadcastEvent(event.extractionKey(), wsEvent);
    }

    /**
     * Handles LLM completed events.
     */
    @Async
    @EventListener
    public void handleLlmCompleted(LlmCompletedEvent event) {
        log.info("WebSocket: Broadcasting LLM completed - {} (confidence: {})",
                event.extractionKey(), event.confidence());

        String message = String.format("Invoice data extracted with %.2f%% confidence",
                event.confidence() * 100);

        ExtractionEventV1_0 wsEvent = ExtractionEventV1_0.builder()
                .type("LLM_EXTRACTION_COMPLETED")
                .extractionKey(event.extractionKey())
                .status("PROCESSING")
                .progress(70)
                .message(message)
                .timestamp(event.timestamp())
                .metadata(java.util.Map.of("confidence", event.confidence()))
                .build();

        broadcastEvent(event.extractionKey(), wsEvent);
    }

    /**
     * Handles invoice saved events.
     */
    @Async
    @EventListener
    public void handleInvoiceSaved(InvoiceSavedEvent event) {
        log.info("WebSocket: Broadcasting invoice saved - {} (invoice: {})",
                event.extractionKey(), event.invoiceKey());

        ExtractionEventV1_0 wsEvent = ExtractionEventV1_0.builder()
                .type("INVOICE_SAVED")
                .extractionKey(event.extractionKey())
                .status("PROCESSING")
                .progress(90)
                .message("Invoice saved with key: " + event.invoiceKey())
                .timestamp(event.timestamp())
                .metadata(java.util.Map.of("invoiceKey", event.invoiceKey().toString()))
                .build();

        broadcastEvent(event.extractionKey(), wsEvent);
    }

    /**
     * Handles extraction completed events.
     */
    @Async
    @EventListener
    public void handleExtractionCompleted(ExtractionCompletedEvent event) {
        log.info("WebSocket: Broadcasting extraction completed - {} (confidence: {})",
                event.extractionKey(), event.confidenceScore());

        String message = String.format("Extraction completed successfully with %.2f%% confidence",
                event.confidenceScore() * 100);

        ExtractionEventV1_0 wsEvent = ExtractionEventV1_0.builder()
                .type("EXTRACTION_COMPLETED")
                .extractionKey(event.extractionKey())
                .status("SUCCESS")
                .progress(100)
                .message(message)
                .timestamp(event.timestamp())
                .metadata(java.util.Map.of(
                        "invoiceKey", event.invoiceKey().toString(),
                        "confidenceScore", event.confidenceScore()
                ))
                .build();

        broadcastEvent(event.extractionKey(), wsEvent);
    }

    /**
     * Handles extraction failed events.
     */
    @Async
    @EventListener
    public void handleExtractionFailed(ExtractionFailedEvent event) {
        log.error("WebSocket: Broadcasting extraction failed - {} (error: {})",
                event.extractionKey(), event.errorMessage());

        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("errorMessage", event.errorMessage());
        if (event.errorCode() != null) {
            metadata.put("errorCode", event.errorCode());
        }

        ExtractionEventV1_0 wsEvent = ExtractionEventV1_0.builder()
                .type("EXTRACTION_FAILED")
                .extractionKey(event.extractionKey())
                .status("FAILED")
                .progress(0)
                .message("Extraction failed: " + event.errorMessage())
                .timestamp(event.timestamp())
                .metadata(metadata)
                .build();

        broadcastEvent(event.extractionKey(), wsEvent);
    }

    /**
     * Broadcasts an event to the WebSocket topic.
     */
    private void broadcastEvent(java.util.UUID extractionKey, ExtractionEventV1_0 event) {
        try {
            String destination = TOPIC_PREFIX + extractionKey;
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Event broadcasted to WebSocket topic: {}", destination);
        } catch (Exception ex) {
            log.error("Failed to broadcast event via WebSocket: {}", extractionKey, ex);
        }
    }
}
