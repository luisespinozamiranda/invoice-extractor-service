package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository;

import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository service interface (Port) for extraction metadata persistence operations.
 * Defines the contract between domain layer and database layer.
 * All operations are async and return CompletableFuture.
 */
public interface IExtractionMetadataRepositoryService {

    /**
     * Find extraction metadata by its unique business key.
     *
     * @param extractionKey UUID of the extraction
     * @return CompletableFuture with ExtractionMetadataModel if found
     */
    CompletableFuture<ExtractionMetadataModel> findByExtractionKey(UUID extractionKey);

    /**
     * Find all extraction metadata for a specific invoice.
     *
     * @param invoiceKey UUID of the invoice
     * @return CompletableFuture with list of ExtractionMetadataModels
     */
    CompletableFuture<List<ExtractionMetadataModel>> findByInvoiceKey(UUID invoiceKey);

    /**
     * Find all extractions by status.
     *
     * @param status Extraction status (PROCESSING, COMPLETED, FAILED)
     * @return CompletableFuture with list of ExtractionMetadataModels
     */
    CompletableFuture<List<ExtractionMetadataModel>> findByStatus(String status);

    /**
     * Find all active extraction metadata (not deleted).
     *
     * @return CompletableFuture with list of ExtractionMetadataModels
     */
    CompletableFuture<List<ExtractionMetadataModel>> findAllActive();

    /**
     * Save a new extraction metadata.
     *
     * @param extractionMetadata Extraction metadata to save
     * @return CompletableFuture with saved ExtractionMetadataModel
     */
    CompletableFuture<ExtractionMetadataModel> save(ExtractionMetadataModel extractionMetadata);

    /**
     * Update an existing extraction metadata.
     *
     * @param extractionKey      UUID of the extraction to update
     * @param extractionMetadata Updated extraction metadata
     * @return CompletableFuture with updated ExtractionMetadataModel
     */
    CompletableFuture<ExtractionMetadataModel> update(UUID extractionKey, ExtractionMetadataModel extractionMetadata);

    /**
     * Soft delete extraction metadata by setting is_deleted flag to true.
     *
     * @param extractionKey UUID of the extraction to delete
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> softDelete(UUID extractionKey);

    /**
     * Find extractions with low confidence scores.
     *
     * @param confidenceThreshold Confidence threshold (0.0 to 1.0)
     * @return CompletableFuture with list of low-confidence extractions
     */
    CompletableFuture<List<ExtractionMetadataModel>> findLowConfidenceExtractions(double confidenceThreshold);
}
