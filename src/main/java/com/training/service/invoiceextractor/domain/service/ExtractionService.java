package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IExtractionMetadataRepositoryService;
import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IInvoiceRepositoryService;
import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.ILlmExtractionService;
import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.InvoiceData;
import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.IOcrService;
import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;
import com.training.service.invoiceextractor.domain.events.ExtractionEventPublisher;
import com.training.service.invoiceextractor.domain.factory.ExtractionMetadataFactory;
import com.training.service.invoiceextractor.domain.factory.InvoiceFactory;
import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Domain service implementation for invoice extraction operations.
 * Orchestrates OCR extraction and invoice creation workflow.
 *
 * <p><b>Refactored to use Design Patterns:</b>
 * <ul>
 *   <li>Factory Pattern: Uses InvoiceFactory and ExtractionMetadataFactory</li>
 *   <li>Observer Pattern: Publishes domain events via ExtractionEventPublisher</li>
 *   <li>Strategy Pattern: Delegates OCR to IOcrService (which uses OcrStrategyContext)</li>
 * </ul>
 *
 * @version 2.0
 * @since 2025-12-22
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractionService implements IExtractionService {

    private final IInvoiceRepositoryService invoiceRepositoryService;
    private final IExtractionMetadataRepositoryService extractionMetadataRepositoryService;
    private final IOcrService ocrService;
    private final ILlmExtractionService llmExtractionService;
    private final ExtractionEventPublisher eventPublisher;

    // Design Pattern: Factory Pattern
    private final InvoiceFactory invoiceFactory;
    private final ExtractionMetadataFactory extractionMetadataFactory;

    private static final double LOW_CONFIDENCE_THRESHOLD = 0.7;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    @Override
    public CompletableFuture<ExtractionMetadataModel> extractAndSaveInvoice(
            byte[] fileData,
            String fileName,
            String fileType
    ) {
        log.debug("Domain: Extracting and saving invoice from file: {}", fileName);

        return CompletableFuture.supplyAsync(() -> {
            // Factory Pattern: Create initial extraction metadata
            ExtractionMetadataModel initialMetadata = extractionMetadataFactory.createProcessing(fileName);

            try {
                saveInitialMetadata(initialMetadata);

                // Observer Pattern: Publish extraction started event
                eventPublisher.publishExtractionStarted(initialMetadata.extractionKey(), fileName);

                // Strategy Pattern: OCR extraction (delegates to OcrStrategyContext)
                OcrResult ocrResult = performOcrExtraction(fileData, fileName, fileType);

                // Observer Pattern: Publish OCR completed event
                eventPublisher.publishOcrCompleted(initialMetadata.extractionKey(), ocrResult);

                // Factory Pattern: Parse and create invoice from LLM data
                InvoiceModel invoice = extractInvoiceFromOcrResult(
                        ocrResult.extractedText(),
                        fileName,
                        initialMetadata.extractionKey()
                );

                // Save invoice to database
                InvoiceModel savedInvoice = saveInvoice(invoice);

                // Observer Pattern: Publish invoice saved event
                eventPublisher.publishInvoiceSaved(initialMetadata.extractionKey(), savedInvoice.invoiceKey());

                // Factory Pattern: Create completed metadata
                ExtractionMetadataModel completedMetadata = createCompletedMetadata(
                        initialMetadata,
                        savedInvoice,
                        ocrResult
                );

                // Update metadata in database
                ExtractionMetadataModel finalMetadata = updateMetadata(
                        initialMetadata.extractionKey(),
                        completedMetadata
                );

                // Observer Pattern: Publish extraction completed event
                eventPublisher.publishExtractionCompleted(
                        initialMetadata.extractionKey(),
                        savedInvoice.invoiceKey(),
                        ocrResult.confidenceScore()
                );

                return finalMetadata;

            } catch (Exception ex) {
                return handleExtractionFailure(initialMetadata, fileName, ex);
            }
        });
    }

    /**
     * Saves initial extraction metadata to database.
     * Uses timeout to prevent indefinite blocking.
     */
    private void saveInitialMetadata(ExtractionMetadataModel metadata) {
        try {
            extractionMetadataRepositoryService.save(metadata).get(30, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.DATABASE_ERROR,
                    "Timeout saving initial metadata after 30 seconds",
                    ex
            );
        } catch (Exception ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.DATABASE_ERROR,
                    "Failed to save initial metadata: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Performs OCR extraction with validation.
     * Strategy Pattern: Delegates to IOcrService (which uses OcrStrategyContext).
     * Uses 60-second timeout for OCR processing.
     */
    private OcrResult performOcrExtraction(byte[] fileData, String fileName, String fileType) {
        OcrResult ocrResult;
        try {
            ocrResult = ocrService.extractText(fileData, fileName, fileType).get(60, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.OCR_TIMEOUT,
                    "OCR processing timed out after 60 seconds",
                    ex
            );
        } catch (Exception ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.OCR_SERVICE_UNAVAILABLE,
                    "OCR extraction failed: " + ex.getMessage(),
                    ex
            );
        }

        if (ocrResult.isEmpty()) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.EXTRACTION_FAILED,
                    "OCR extraction returned empty text"
            );
        }

        return ocrResult;
    }

    /**
     * Extracts invoice data from OCR text using LLM.
     * Factory Pattern: Uses InvoiceFactory to create invoice model.
     * Uses 30-second timeout for LLM API calls.
     */
    private InvoiceModel extractInvoiceFromOcrResult(
            String extractedText,
            String fileName,
            UUID extractionKey
    ) {
        validateLlmAvailability();

        try {
            InvoiceData llmData = llmExtractionService.extractInvoiceData(extractedText).get(30, TimeUnit.SECONDS);

            // Observer Pattern: Publish LLM completed event
            eventPublisher.publishLlmCompleted(extractionKey, llmData);

            validateLlmData(llmData);

            // Factory Pattern: Create invoice from LLM data
            return invoiceFactory.createFromLlmData(llmData, fileName);

        } catch (Exception ex) {
            log.error("LLM extraction failed: {}", ex.getMessage());
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.EXTRACTION_FAILED,
                    "LLM extraction failed: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Validates that LLM service is available.
     */
    private void validateLlmAvailability() {
        if (!llmExtractionService.isAvailable()) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.LLM_SERVICE_UNAVAILABLE,
                    "LLM service is not available for invoice data extraction"
            );
        }
    }

    /**
     * Validates LLM extracted data.
     */
    private void validateLlmData(InvoiceData llmData) {
        if (!llmData.isValid()) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.LLM_INVALID_RESPONSE,
                    "LLM extraction returned invalid data"
            );
        }

        log.info("LLM extraction successful with confidence: {}", llmData.confidence());
    }

    /**
     * Saves invoice to database.
     * Uses timeout to prevent indefinite blocking.
     */
    private InvoiceModel saveInvoice(InvoiceModel invoice) {
        try {
            return invoiceRepositoryService.save(invoice).get(30, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.DATABASE_ERROR,
                    "Timeout saving invoice after 30 seconds",
                    ex
            );
        } catch (Exception ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.DATABASE_ERROR,
                    "Failed to save invoice: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Creates completed extraction metadata.
     * Factory Pattern: Uses ExtractionMetadataFactory.
     */
    private ExtractionMetadataModel createCompletedMetadata(
            ExtractionMetadataModel initialMetadata,
            InvoiceModel savedInvoice,
            OcrResult ocrResult
    ) {
        return extractionMetadataFactory.createCompleted(
                initialMetadata.extractionKey(),
                savedInvoice.invoiceKey(),
                initialMetadata.sourceFileName(),
                ocrResult
        );
    }

    /**
     * Updates extraction metadata in database.
     * Uses timeout to prevent indefinite blocking.
     */
    private ExtractionMetadataModel updateMetadata(
            UUID extractionKey,
            ExtractionMetadataModel metadata
    ) {
        try {
            return extractionMetadataRepositoryService.update(extractionKey, metadata).get(30, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.DATABASE_ERROR,
                    "Timeout updating extraction metadata after 30 seconds",
                    ex
            );
        } catch (Exception ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.DATABASE_ERROR,
                    "Failed to update extraction metadata: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Handles extraction failure by creating failed metadata and publishing event.
     * Factory Pattern: Uses ExtractionMetadataFactory to create failed metadata.
     */
    private ExtractionMetadataModel handleExtractionFailure(
            ExtractionMetadataModel initialMetadata,
            String fileName,
            Exception ex
    ) {
        log.error("Error during extraction: {}", fileName, ex);

        // Observer Pattern: Publish extraction failed event
        eventPublisher.publishExtractionFailed(initialMetadata.extractionKey(), ex.getMessage());

        // Factory Pattern: Create failed extraction metadata
        ExtractionMetadataModel failedMetadata = extractionMetadataFactory.createFailed(
                initialMetadata.extractionKey(),
                fileName,
                ex.getMessage()
        );

        try {
            extractionMetadataRepositoryService.save(failedMetadata).get(30, TimeUnit.SECONDS);
        } catch (Exception saveEx) {
            log.error("Failed to save failed metadata: {}", saveEx.getMessage());
            // Don't throw here, we already have an exception to throw
        }

        throw new InvoiceExtractorServiceException(
                ErrorCodes.EXTRACTION_FAILED,
                fileName + ": " + ex.getMessage(),
                ex
        );
    }

    @Override
    public CompletableFuture<ExtractionMetadataModel> getExtractionMetadata(UUID extractionKey) {
        log.debug("Domain: Getting extraction metadata by key: {}", extractionKey);

        return extractionMetadataRepositoryService.findByExtractionKey(extractionKey)
                .exceptionally(ex -> {
                    log.error("Error getting extraction metadata: {}", extractionKey, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.EXTRACTION_NOT_FOUND,
                            "Extraction metadata not found with key: " + extractionKey
                    );
                });
    }

    @Override
    public CompletableFuture<List<ExtractionMetadataModel>> getExtractionsByInvoice(UUID invoiceKey) {
        log.debug("Domain: Getting extractions by invoice key: {}", invoiceKey);

        return extractionMetadataRepositoryService.findByInvoiceKey(invoiceKey)
                .exceptionally(ex -> {
                    log.error("Error getting extractions by invoice: {}", invoiceKey, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to retrieve extractions"
                    );
                });
    }

    @Override
    public CompletableFuture<List<ExtractionMetadataModel>> getExtractionsByStatus(String status) {
        log.debug("Domain: Getting extractions by status: {}", status);

        return extractionMetadataRepositoryService.findByStatus(status)
                .exceptionally(ex -> {
                    log.error("Error getting extractions by status: {}", status, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to retrieve extractions by status"
                    );
                });
    }

    @Override
    public CompletableFuture<ExtractionMetadataModel> retryExtraction(UUID extractionKey) {
        log.debug("Domain: Retrying extraction: {}", extractionKey);

        return extractionMetadataRepositoryService.findByExtractionKey(extractionKey)
                .thenCompose(metadata -> {
                    if (metadata == null) {
                        throw new InvoiceExtractorServiceException(
                                ErrorCodes.EXTRACTION_NOT_FOUND,
                                "Extraction metadata not found with key: " + extractionKey
                        );
                    }

                    // Mark as processing for retry
                    log.info("Marking extraction for retry: {}", extractionKey);

                    // Create new processing metadata to indicate retry is needed
                    ExtractionMetadataModel retryMetadata = ExtractionMetadataModel.createProcessing(
                            metadata.sourceFileName()
                    );

                    // Update the status to processing
                    return extractionMetadataRepositoryService.save(retryMetadata)
                            .thenApply(saved -> {
                                log.info("Extraction marked for retry: {}", extractionKey);
                                return saved;
                            });
                })
                .exceptionally(ex -> {
                    log.error("Error retrying extraction: {}", extractionKey, ex);
                    if (ex.getCause() instanceof InvoiceExtractorServiceException) {
                        throw (InvoiceExtractorServiceException) ex.getCause();
                    }
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.EXTRACTION_FAILED,
                            "Retry failed: " + ex.getMessage()
                    );
                });
    }

    @Override
    public CompletableFuture<List<ExtractionMetadataModel>> getAllExtractions() {
        log.debug("Domain: Getting all extractions");

        return extractionMetadataRepositoryService.findAllActive()
                .exceptionally(ex -> {
                    log.error("Error getting all extractions", ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to retrieve extractions"
                    );
                });
    }

    @Override
    public CompletableFuture<List<ExtractionMetadataModel>> getLowConfidenceExtractions(double confidenceThreshold) {
        log.debug("Domain: Getting low confidence extractions below threshold: {}", confidenceThreshold);

        return extractionMetadataRepositoryService.findLowConfidenceExtractions(confidenceThreshold)
                .exceptionally(ex -> {
                    log.error("Error getting low confidence extractions", ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to retrieve low confidence extractions"
                    );
                });
    }

    @Override
    public CompletableFuture<Void> deleteExtraction(UUID extractionKey) {
        log.debug("Domain: Soft deleting extraction: {}", extractionKey);

        return extractionMetadataRepositoryService.softDelete(extractionKey)
                .exceptionally(ex -> {
                    log.error("Error deleting extraction: {}", extractionKey, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to delete extraction"
                    );
                });
    }

    @Override
    public CompletableFuture<Boolean> validateFile(byte[] fileData, String fileName, String fileType) {
        log.debug("Domain: Validating file: {}", fileName);

        return CompletableFuture.supplyAsync(() -> {
            // Validate file is not empty
            if (fileData == null || fileData.length == 0) {
                log.warn("File is empty: {}", fileName);
                return false;
            }

            // Validate file size (max 10MB)
            if (fileData.length > MAX_FILE_SIZE_BYTES) {
                log.warn("File too large: {} ({} bytes, max: {} bytes)",
                        fileName, fileData.length, MAX_FILE_SIZE_BYTES);
                return false;
            }

            // Validate file type (delegating to OCR service for accuracy)
            try {
                Boolean isSupported = ocrService.isFormatSupported(fileType).get(10, TimeUnit.SECONDS);
                if (!isSupported) {
                    log.warn("Unsupported file type: {} ({})", fileName, fileType);
                    return false;
                }
            } catch (TimeoutException ex) {
                log.error("Timeout checking file type support for: {}", fileType, ex);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.OCR_SERVICE_UNAVAILABLE,
                        "Timeout validating file type after 10 seconds",
                        ex
                );
            } catch (Exception ex) {
                log.error("Error checking file type support for: {}", fileType, ex);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.INVALID_FILE_TYPE,
                        "Failed to validate file type: " + ex.getMessage(),
                        ex
                );
            }

            return true;
        });
    }

}
