package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST DTO for Extraction Metadata (API v1.0)
 * Represents OCR extraction metadata in REST API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionMetadataV1_0 {

    @JsonProperty("extraction_key")
    private UUID extractionKey;

    @JsonProperty("invoice_key")
    private UUID invoiceKey;

    @JsonProperty("source_file_name")
    private String sourceFileName;

    @JsonProperty("extraction_timestamp")
    private LocalDateTime extractionTimestamp;

    @JsonProperty("extraction_status")
    private String extractionStatus;

    @JsonProperty("confidence_score")
    private Double confidenceScore;

    @JsonProperty("ocr_engine")
    private String ocrEngine;

    @JsonProperty("extraction_data")
    private String extractionData;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("is_deleted")
    private Boolean isDeleted;
}
