package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IExtractionMetadataRepositoryService;
import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IInvoiceRepositoryService;
import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.IOcrService;
import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;
import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain service implementation for invoice extraction operations.
 * Orchestrates OCR extraction and invoice creation workflow.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractionService implements IExtractionService {

    private final IInvoiceRepositoryService invoiceRepositoryService;
    private final IExtractionMetadataRepositoryService extractionMetadataRepositoryService;
    private final IOcrService ocrService;

    // Regex patterns for invoice field extraction
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("(?i)invoice\\s*#?:?\\s*([A-Z0-9-]+)", Pattern.MULTILINE);
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(?:total|amount|balance)\\s*:?\\s*\\$?([0-9,]+\\.?[0-9]*)", Pattern.MULTILINE);
    private static final Pattern CLIENT_NAME_PATTERN = Pattern.compile("(?i)(?:bill\\s*to|client|customer)\\s*:?\\s*([A-Za-z\\s]+)", Pattern.MULTILINE);

    @Override
    public CompletableFuture<ExtractionMetadataModel> extractAndSaveInvoice(
            byte[] fileData,
            String fileName,
            String fileType
    ) {
        log.debug("Domain: Extracting and saving invoice from file: {}", fileName);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create initial extraction metadata
                ExtractionMetadataModel initialMetadata = ExtractionMetadataModel.createProcessing(fileName);

                // Save initial processing state
                extractionMetadataRepositoryService.save(initialMetadata).join();

                // Extract text using OCR adapter
                OcrResult ocrResult = ocrService.extractText(fileData, fileName, fileType).join();

                if (ocrResult.isEmpty()) {
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.EXTRACTION_FAILED,
                            "OCR extraction returned empty text"
                    );
                }

                String extractedText = ocrResult.extractedText();

                // Parse extracted text into invoice data
                InvoiceModel invoice = parseInvoiceFromText(extractedText, fileName);

                // Save the invoice
                InvoiceModel savedInvoice = invoiceRepositoryService.save(invoice).join();

                // Create completed extraction metadata
                // Wrap text in JSON format for PostgreSQL jsonb column
                String extractionDataJson = wrapTextAsJson(extractedText);

                ExtractionMetadataModel completedMetadata = ExtractionMetadataModel.createCompleted(
                        initialMetadata.extractionKey(),
                        savedInvoice.invoiceKey(),
                        fileName,
                        ocrResult.confidenceScore(),
                        ocrResult.engineVersion(),
                        extractionDataJson // JSON-formatted extraction data
                );

                // Save completed metadata
                return extractionMetadataRepositoryService.save(completedMetadata).join();

            } catch (Exception ex) {
                log.error("Error during extraction: {}", fileName, ex);

                // Create failed extraction metadata
                ExtractionMetadataModel failedMetadata = ExtractionMetadataModel.createFailed(
                        UUID.randomUUID(),
                        fileName,
                        ex.getMessage()
                );

                extractionMetadataRepositoryService.save(failedMetadata).join();

                throw new InvoiceExtractorServiceException(
                        ErrorCodes.EXTRACTION_FAILED,
                        fileName + ": " + ex.getMessage()
                );
            }
        });
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
            // Check file size (max 10MB)
            if (fileData == null || fileData.length == 0) {
                log.warn("File is empty: {}", fileName);
                return false;
            }

            if (fileData.length > 10 * 1024 * 1024) {
                log.warn("File too large: {} ({} bytes)", fileName, fileData.length);
                return false;
            }

            // Check file type
            if (fileType == null || (!fileType.contains("pdf") && !fileType.contains("image"))) {
                log.warn("Invalid file type: {} ({})", fileName, fileType);
                return false;
            }

            return true;
        });
    }

    /**
     * Parse invoice data from extracted text using regex patterns
     */
    private InvoiceModel parseInvoiceFromText(String extractedText, String fileName) {
        String invoiceNumber = extractField(extractedText, INVOICE_NUMBER_PATTERN, "UNKNOWN");
        String amountStr = extractField(extractedText, AMOUNT_PATTERN, "0.00");
        String clientName = extractField(extractedText, CLIENT_NAME_PATTERN, "Unknown Client");

        // Clean amount string and parse
        BigDecimal amount = parseAmount(amountStr);

        return InvoiceModel.create(
                invoiceNumber,
                amount,
                clientName.trim(),
                null, // clientAddress - would extract from text
                "USD",
                InvoiceModel.STATUS_EXTRACTED,
                fileName
        );
    }

    /**
     * Extract field from text using regex pattern
     */
    private String extractField(String text, Pattern pattern, String defaultValue) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find() && matcher.groupCount() >= 1) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }

    /**
     * Parse amount string to BigDecimal
     */
    private BigDecimal parseAmount(String amountStr) {
        try {
            String cleaned = amountStr.replaceAll("[^0-9.]", "");
            return new BigDecimal(cleaned);
        } catch (Exception ex) {
            log.warn("Failed to parse amount: {}", amountStr);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Wrap extracted text in JSON format for PostgreSQL jsonb column.
     * Escapes quotes and newlines to create valid JSON.
     *
     * @param text Raw text extracted by OCR
     * @return JSON string in format: {"text": "escaped content", "length": 123}
     */
    private String wrapTextAsJson(String text) {
        if (text == null) {
            return "{\"text\":\"\",\"length\":0}";
        }

        // Escape special JSON characters
        String escapedText = text
                .replace("\\", "\\\\")  // Backslash must be first
                .replace("\"", "\\\"")  // Escape quotes
                .replace("\n", "\\n")   // Escape newlines
                .replace("\r", "\\r")   // Escape carriage returns
                .replace("\t", "\\t");  // Escape tabs

        return String.format("{\"text\":\"%s\",\"length\":%d}", escapedText, text.length());
    }
}
