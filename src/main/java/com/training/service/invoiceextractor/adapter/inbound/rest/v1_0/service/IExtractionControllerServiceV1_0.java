package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.ExtractionMetadataV1_0;
import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.ExtractionResponseV1_0;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller Service interface for Extraction operations (API v1.0)
 * Inbound adapter port for REST layer
 */
public interface IExtractionControllerServiceV1_0 {

    /**
     * Extract invoice data from uploaded file
     *
     * @param file The uploaded file (PDF or image)
     * @return CompletableFuture with ResponseEntity containing extraction metadata
     */
    CompletableFuture<ResponseEntity<ExtractionMetadataV1_0>> extractInvoice(MultipartFile file);

    /**
     * Get extraction metadata by invoice key
     *
     * @param invoiceKey The invoice key
     * @return CompletableFuture with ResponseEntity containing extraction metadata DTO
     */
    CompletableFuture<ResponseEntity<ExtractionMetadataV1_0>> getExtractionByInvoice(UUID invoiceKey);
}
