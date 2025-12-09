package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Domain service interface defining business operations for invoice extraction management.
 *
 * <p>This interface represents the port in hexagonal architecture, defining the contract
 * for invoice extraction business logic without exposing implementation details. It orchestrates
 * the complete OCR extraction workflow and manages extraction metadata.
 *
 * <p><b>Design Pattern:</b> Port (Hexagonal Architecture)
 * <p><b>Async Contract:</b> All methods return {@link CompletableFuture} for non-blocking execution
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see ExtractionMetadataModel
 * @see ExtractionService
 */
public interface IExtractionService {

    /**
     * Extract invoice data from an uploaded file and save it.
     * This method orchestrates the complete extraction workflow:
     * 1. Call OCR service to extract data
     * 2. Parse extracted text into structured invoice data
     * 3. Create invoice in database
     * 4. Save extraction metadata
     *
     * @param fileData Binary file data (PDF or image)
     * @param fileName Original file name
     * @param fileType MIME type (application/pdf, image/png, etc.)
     * @return CompletableFuture with ExtractionMetadataModel
     */
    CompletableFuture<ExtractionMetadataModel> extractAndSaveInvoice(
            byte[] fileData,
            String fileName,
            String fileType
    );

    /**
     * Get extraction metadata by extraction key.
     *
     * @param extractionKey UUID of the extraction
     * @return CompletableFuture with ExtractionMetadataModel
     */
    CompletableFuture<ExtractionMetadataModel> getExtractionMetadata(UUID extractionKey);

    /**
     * Get all extraction metadata for a specific invoice.
     *
     * @param invoiceKey UUID of the invoice
     * @return CompletableFuture with list of ExtractionMetadataModels
     */
    CompletableFuture<List<ExtractionMetadataModel>> getExtractionsByInvoice(UUID invoiceKey);

    /**
     * Get all extractions by status.
     *
     * @param status Extraction status (PROCESSING, COMPLETED, FAILED)
     * @return CompletableFuture with list of ExtractionMetadataModels
     */
    CompletableFuture<List<ExtractionMetadataModel>> getExtractionsByStatus(String status);

    /**
     * Retry a failed extraction.
     * Re-processes the original file if still available.
     *
     * @param extractionKey UUID of the failed extraction
     * @return CompletableFuture with new ExtractionMetadataModel
     */
    CompletableFuture<ExtractionMetadataModel> retryExtraction(UUID extractionKey);

    /**
     * Get all active extraction metadata (not deleted).
     *
     * @return CompletableFuture with list of ExtractionMetadataModels
     */
    CompletableFuture<List<ExtractionMetadataModel>> getAllExtractions();

    /**
     * Get extraction metadata with low confidence scores.
     * Useful for identifying extractions that may need manual review.
     *
     * @param confidenceThreshold Minimum confidence threshold (0.0 to 1.0)
     * @return CompletableFuture with list of low-confidence extractions
     */
    CompletableFuture<List<ExtractionMetadataModel>> getLowConfidenceExtractions(double confidenceThreshold);

    /**
     * Soft delete extraction metadata (logical deletion).
     *
     * @param extractionKey UUID of the extraction to delete
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> deleteExtraction(UUID extractionKey);

    /**
     * Validate file before extraction.
     * Checks file type, size, and readability.
     *
     * @param fileData Binary file data
     * @param fileName File name
     * @param fileType MIME type
     * @return CompletableFuture<Boolean> true if file is valid
     */
    CompletableFuture<Boolean> validateFile(byte[] fileData, String fileName, String fileType);
}
