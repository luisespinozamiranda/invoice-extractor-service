package com.training.service.invoiceextractor.adapter.inbound.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket event DTO for real-time extraction progress updates (API v1.0).
 *
 * <p>Represents a single extraction event broadcasted to WebSocket clients.
 * Events are published during various stages of the extraction process.
 *
 * <p><b>Event Types:</b>
 * <ul>
 *   <li>EXTRACTION_STARTED - Extraction process initiated</li>
 *   <li>OCR_COMPLETED - OCR text extraction completed</li>
 *   <li>LLM_EXTRACTION_COMPLETED - LLM data parsing completed</li>
 *   <li>INVOICE_SAVED - Invoice record saved to database</li>
 *   <li>EXTRACTION_COMPLETED - Full extraction process succeeded</li>
 *   <li>EXTRACTION_FAILED - Extraction process failed</li>
 * </ul>
 *
 * <p><b>Architecture:</b> REST DTO Layer (Inbound Adapter)
 * <p><b>API Version:</b> 1.0
 * <p><b>Transport:</b> WebSocket (STOMP over SockJS)
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionEventV1_0 {

    /**
     * Event type identifier.
     * Values: EXTRACTION_STARTED, OCR_COMPLETED, LLM_EXTRACTION_COMPLETED,
     * INVOICE_SAVED, EXTRACTION_COMPLETED, EXTRACTION_FAILED
     */
    @JsonProperty("type")
    private String type;

    /**
     * Unique extraction identifier.
     * Used to correlate events with a specific extraction request.
     */
    @JsonProperty("extraction_key")
    private UUID extractionKey;

    /**
     * Current extraction status.
     * Values: PROCESSING, SUCCESS, FAILED
     */
    @JsonProperty("status")
    private String status;

    /**
     * Progress percentage (0-100).
     * Indicates completion percentage of the extraction process.
     */
    @JsonProperty("progress")
    private Integer progress;

    /**
     * Human-readable event message.
     * Describes what happened in this event.
     */
    @JsonProperty("message")
    private String message;

    /**
     * Event timestamp.
     * When this event was generated.
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Additional event-specific metadata.
     * Contents vary based on event type:
     * - OCR_COMPLETED: pageCount, confidenceScore, processingTimeMs
     * - LLM_EXTRACTION_COMPLETED: invoiceNumber, amount, clientName
     * - EXTRACTION_COMPLETED: invoiceKey, confidenceScore
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}
