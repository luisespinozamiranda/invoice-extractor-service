package com.training.service.invoiceextractor.adapter.inbound.websocket.service;

import com.training.service.invoiceextractor.adapter.inbound.websocket.dto.ExtractionEventV1_0;
import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.InvoiceData;
import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for publishing extraction progress events via WebSocket.
 *
 * <p>This service broadcasts real-time extraction events to WebSocket clients
 * subscribed to specific extraction topics. Events are sent via STOMP protocol.
 *
 * <p><b>Event Flow:</b>
 * <ol>
 *   <li>Extraction Started (0%)</li>
 *   <li>OCR Completed (33%)</li>
 *   <li>LLM Extraction Completed (66%)</li>
 *   <li>Invoice Saved (90%)</li>
 *   <li>Extraction Completed (100%) or Extraction Failed</li>
 * </ol>
 *
 * <p><b>Topic Structure:</b> /topic/extraction/{extractionKey}
 *
 * <p><b>Architecture:</b> Service Layer (Inbound Adapter)
 * <p><b>Pattern:</b> Publisher (Event Broadcasting)
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-12
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Publishes an extraction event to the WebSocket topic for a specific extraction.
     *
     * @param extractionKey Unique extraction identifier
     * @param event Event to publish
     */
    public void publishEvent(UUID extractionKey, ExtractionEventV1_0 event) {
        String destination = "/topic/extraction/" + extractionKey;
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Published event {} to {}", event.getType(), destination);
    }

    /**
     * Publishes EXTRACTION_STARTED event (Progress: 0%).
     *
     * @param extractionKey Unique extraction identifier
     * @param fileName Source file name
     */
    public void publishExtractionStarted(UUID extractionKey, String fileName) {
        ExtractionEventV1_0 event = ExtractionEventV1_0.builder()
                .type("EXTRACTION_STARTED")
                .extractionKey(extractionKey)
                .status("PROCESSING")
                .progress(0)
                .message("Starting extraction for: " + fileName)
                .timestamp(LocalDateTime.now())
                .build();
        publishEvent(extractionKey, event);
    }

    /**
     * Publishes OCR_COMPLETED event (Progress: 33%).
     *
     * @param extractionKey Unique extraction identifier
     * @param ocrResult OCR extraction result with text and confidence
     */
    public void publishOcrCompleted(UUID extractionKey, OcrResult ocrResult) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pageCount", ocrResult.pageCount());
        metadata.put("confidenceScore", ocrResult.confidenceScore());
        metadata.put("processingTimeMs", ocrResult.processingTimeMs());

        ExtractionEventV1_0 event = ExtractionEventV1_0.builder()
                .type("OCR_COMPLETED")
                .extractionKey(extractionKey)
                .status("PROCESSING")
                .progress(33)
                .message("OCR extraction completed")
                .timestamp(LocalDateTime.now())
                .metadata(metadata)
                .build();
        publishEvent(extractionKey, event);
    }

    /**
     * Publishes LLM_EXTRACTION_COMPLETED event (Progress: 66%).
     *
     * @param extractionKey Unique extraction identifier
     * @param invoiceData Parsed invoice data from LLM
     */
    public void publishLlmCompleted(UUID extractionKey, InvoiceData invoiceData) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceNumber", invoiceData.invoiceNumber().orElse("N/A"));
        metadata.put("amount", invoiceData.amount().map(BigDecimal::toString).orElse("N/A"));
        metadata.put("clientName", invoiceData.clientName().orElse("N/A"));

        ExtractionEventV1_0 event = ExtractionEventV1_0.builder()
                .type("LLM_EXTRACTION_COMPLETED")
                .extractionKey(extractionKey)
                .status("PROCESSING")
                .progress(66)
                .message("Invoice data extracted by LLM")
                .timestamp(LocalDateTime.now())
                .metadata(metadata)
                .build();
        publishEvent(extractionKey, event);
    }

    /**
     * Publishes INVOICE_SAVED event (Progress: 90%).
     *
     * @param extractionKey Unique extraction identifier
     * @param invoiceKey Saved invoice unique key
     */
    public void publishInvoiceSaved(UUID extractionKey, UUID invoiceKey) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceKey", invoiceKey.toString());

        ExtractionEventV1_0 event = ExtractionEventV1_0.builder()
                .type("INVOICE_SAVED")
                .extractionKey(extractionKey)
                .status("PROCESSING")
                .progress(90)
                .message("Invoice saved to database")
                .timestamp(LocalDateTime.now())
                .metadata(metadata)
                .build();
        publishEvent(extractionKey, event);
    }

    /**
     * Publishes EXTRACTION_COMPLETED event (Progress: 100%).
     *
     * @param extractionKey Unique extraction identifier
     * @param invoiceKey Saved invoice unique key
     * @param confidence Overall extraction confidence score (0.0-1.0)
     */
    public void publishExtractionCompleted(UUID extractionKey, UUID invoiceKey, double confidence) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceKey", invoiceKey.toString());
        metadata.put("confidenceScore", confidence);

        ExtractionEventV1_0 event = ExtractionEventV1_0.builder()
                .type("EXTRACTION_COMPLETED")
                .extractionKey(extractionKey)
                .status("SUCCESS")
                .progress(100)
                .message("Extraction completed successfully")
                .timestamp(LocalDateTime.now())
                .metadata(metadata)
                .build();
        publishEvent(extractionKey, event);
    }

    /**
     * Publishes EXTRACTION_FAILED event (Progress: 0%).
     *
     * @param extractionKey Unique extraction identifier
     * @param errorMessage Error description
     */
    public void publishExtractionFailed(UUID extractionKey, String errorMessage) {
        ExtractionEventV1_0 event = ExtractionEventV1_0.builder()
                .type("EXTRACTION_FAILED")
                .extractionKey(extractionKey)
                .status("FAILED")
                .progress(0)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
        publishEvent(extractionKey, event);
    }
}
