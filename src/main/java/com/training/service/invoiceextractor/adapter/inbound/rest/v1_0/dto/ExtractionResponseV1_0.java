package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for Extraction Response (API v1.0)
 * Contains both invoice and extraction metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResponseV1_0 {

    @JsonProperty("invoice")
    private InvoiceV1_0 invoice;

    @JsonProperty("extraction_metadata")
    private ExtractionMetadataV1_0 extractionMetadata;

    @JsonProperty("message")
    private String message;
}
