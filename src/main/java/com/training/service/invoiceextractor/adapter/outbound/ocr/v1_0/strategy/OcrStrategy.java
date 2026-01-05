package com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.strategy;

import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;

import java.util.concurrent.CompletableFuture;

/**
 * Strategy interface for OCR operations.
 * Allows multiple OCR engine implementations to be used interchangeably.
 *
 * <p>This interface follows the Strategy Pattern, enabling the system to:
 * <ul>
 *   <li>Support multiple OCR engines (Tesseract, Google Vision, AWS Textract, etc.)</li>
 *   <li>Switch between OCR providers without modifying client code</li>
 *   <li>Add new OCR engines without changing existing implementations</li>
 * </ul>
 *
 * <p><b>Design Pattern:</b> Strategy Pattern
 * <p><b>SOLID Principles:</b> Open/Closed Principle, Dependency Inversion Principle
 *
 * @see OcrStrategyContext
 * @see TesseractOcrStrategy
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-22
 */
public interface OcrStrategy {

    /**
     * Extracts text from file data using the OCR engine.
     *
     * @param fileData Binary file data
     * @param fileName Original file name
     * @param fileType MIME type of the file (e.g., "application/pdf", "image/png")
     * @return CompletableFuture containing the OCR result
     */
    CompletableFuture<OcrResult> extractText(byte[] fileData, String fileName, String fileType);

    /**
     * Checks if this strategy supports the given file type.
     *
     * @param fileType MIME type to check
     * @return true if this strategy can process the file type, false otherwise
     */
    boolean supports(String fileType);

    /**
     * Returns the name and version of the OCR engine.
     *
     * @return Engine name with version (e.g., "Tesseract 5.3.0")
     */
    String getEngineName();

    /**
     * Returns the priority of this strategy.
     * Higher priority strategies are preferred when multiple strategies support the same file type.
     *
     * @return Priority value (higher = more priority)
     */
    default int getPriority() {
        return 0;
    }
}
