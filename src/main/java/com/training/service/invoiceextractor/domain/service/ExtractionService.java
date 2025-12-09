package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IExtractionMetadataRepositoryService;
import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IInvoiceRepositoryService;
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

                // TODO: Integrate with Tesseract OCR adapter to extract text from fileData
                // For now, we'll simulate extraction with placeholder data
                String extractedText = simulateOcrExtraction(fileName);

                // Parse extracted text into invoice data
                InvoiceModel invoice = parseInvoiceFromText(extractedText, fileName);

                // Save the invoice
                InvoiceModel savedInvoice = invoiceRepositoryService.save(invoice).join();

                // Calculate confidence score (simplified - would be from OCR engine)
                double confidenceScore = calculateConfidenceScore(extractedText);

                // Create completed extraction metadata
                ExtractionMetadataModel completedMetadata = ExtractionMetadataModel.createCompleted(
                        initialMetadata.extractionKey(),
                        savedInvoice.invoiceKey(),
                        fileName,
                        confidenceScore,
                        "Tesseract 5.x", // OCR engine
                        extractedText // Raw extraction data
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
                            extractionKey.toString()
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
                                extractionKey.toString()
                        );
                    }

                    // TODO: Re-extract from original file if available
                    // For now, return the existing metadata
                    log.warn("Retry extraction not fully implemented - returning existing metadata");
                    return CompletableFuture.completedFuture(metadata);
                })
                .exceptionally(ex -> {
                    log.error("Error retrying extraction: {}", extractionKey, ex);
                    if (ex.getCause() instanceof InvoiceExtractorServiceException) {
                        throw (InvoiceExtractorServiceException) ex.getCause();
                    }
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.EXTRACTION_FAILED,
                            "Retry failed"
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
     * Simulate OCR extraction (placeholder until Tesseract integration is complete)
     */
    private String simulateOcrExtraction(String fileName) {
        log.debug("Simulating OCR extraction for: {}", fileName);

        // Return simulated invoice text
        return """
                INVOICE #INV-2024-001

                Bill To: ACME Corporation
                123 Main Street

                Total Amount: $1,250.00

                Date: 2024-12-08
                """;
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
     * Calculate confidence score (simplified - would come from OCR engine)
     */
    private double calculateConfidenceScore(String extractedText) {
        // Simple heuristic: if we found key fields, confidence is higher
        double score = 0.5;

        if (INVOICE_NUMBER_PATTERN.matcher(extractedText).find()) score += 0.2;
        if (AMOUNT_PATTERN.matcher(extractedText).find()) score += 0.2;
        if (CLIENT_NAME_PATTERN.matcher(extractedText).find()) score += 0.1;

        return Math.min(score, 1.0);
    }
}
