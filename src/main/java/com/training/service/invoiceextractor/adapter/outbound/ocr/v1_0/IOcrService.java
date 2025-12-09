package com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0;

import java.util.concurrent.CompletableFuture;

/**
 * Outbound port for OCR operations.
 * This interface defines the contract for extracting text from binary files using OCR technology.
 *
 * <p><b>Architecture:</b> Outbound Port (Hexagonal Architecture)
 * <p><b>Layer:</b> Adapter Layer
 * <p><b>Purpose:</b> Abstract OCR engine operations from domain logic
 *
 * <p>Implementations should:
 * <ul>
 *   <li>Support PDF and image formats (PNG, JPG, TIFF)</li>
 *   <li>Return confidence scores for extracted text</li>
 *   <li>Handle OCR failures gracefully</li>
 *   <li>Be non-blocking using {@link CompletableFuture}</li>
 * </ul>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-09
 */
public interface IOcrService {

    /**
     * Extract text from binary file data using OCR.
     *
     * @param fileData Binary content of the file (PDF or image)
     * @param fileName Original file name (used for logging and format detection)
     * @param fileType MIME type of the file (e.g., "application/pdf", "image/png")
     * @return CompletableFuture with OcrResult containing extracted text and metadata
     * @throws com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException
     *         if OCR extraction fails
     */
    CompletableFuture<OcrResult> extractText(byte[] fileData, String fileName, String fileType);

    /**
     * Validates if the file format is supported by the OCR engine.
     *
     * @param fileType MIME type of the file
     * @return CompletableFuture with true if format is supported, false otherwise
     */
    CompletableFuture<Boolean> isFormatSupported(String fileType);

    /**
     * Get the name/version of the OCR engine being used.
     *
     * @return Engine name and version (e.g., "Tesseract 5.3.0")
     */
    String getEngineName();
}
