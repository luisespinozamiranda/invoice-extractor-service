package com.training.service.invoiceextractor.domain.factory;

import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;
import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Factory for creating ExtractionMetadataModel instances.
 * Centralizes extraction metadata creation logic.
 *
 * <p><b>Design Pattern:</b> Factory Pattern
 * <p><b>SOLID Principles:</b>
 * <ul>
 *   <li>Single Responsibility: Focused solely on extraction metadata creation</li>
 *   <li>Open/Closed: New creation methods can be added without modifying existing ones</li>
 * </ul>
 *
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Centralized extraction data JSON wrapping logic</li>
 *   <li>Consistent metadata creation across the application</li>
 *   <li>Easy to test and mock</li>
 *   <li>Reduces code duplication</li>
 * </ul>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-22
 */
@Component
@Slf4j
public class ExtractionMetadataFactory {

    /**
     * Creates extraction metadata for a processing state.
     *
     * @param sourceFileName Source file name
     * @return Processing extraction metadata
     */
    public ExtractionMetadataModel createProcessing(String sourceFileName) {
        log.debug("Creating processing extraction metadata for: {}", sourceFileName);

        return ExtractionMetadataModel.createProcessing(sourceFileName);
    }

    /**
     * Creates completed extraction metadata from OCR result.
     *
     * @param extractionKey Extraction key (must match processing metadata)
     * @param invoiceKey Associated invoice key
     * @param sourceFileName Source file name
     * @param ocrResult OCR extraction result
     * @return Completed extraction metadata
     */
    public ExtractionMetadataModel createCompleted(
            UUID extractionKey,
            UUID invoiceKey,
            String sourceFileName,
            OcrResult ocrResult
    ) {
        log.debug("Creating completed extraction metadata for extraction: {}", extractionKey);

        String extractionDataJson = wrapTextAsJson(ocrResult.extractedText());

        return ExtractionMetadataModel.createCompleted(
                extractionKey,
                invoiceKey,
                sourceFileName,
                ocrResult.confidenceScore(),
                ocrResult.engineVersion(),
                extractionDataJson
        );
    }

    /**
     * Creates failed extraction metadata.
     *
     * @param extractionKey Extraction key (must match processing metadata)
     * @param sourceFileName Source file name
     * @param errorMessage Error message
     * @return Failed extraction metadata
     */
    public ExtractionMetadataModel createFailed(
            UUID extractionKey,
            String sourceFileName,
            String errorMessage
    ) {
        log.warn("Creating failed extraction metadata for: {} - {}", sourceFileName, errorMessage);

        return ExtractionMetadataModel.createFailed(
                extractionKey,
                sourceFileName,
                errorMessage
        );
    }

    /**
     * Creates partial extraction metadata.
     * Used when extraction completes but with low confidence or missing fields.
     *
     * @param extractionKey Extraction key
     * @param invoiceKey Associated invoice key (may be null)
     * @param sourceFileName Source file name
     * @param ocrResult OCR extraction result
     * @param warningMessage Warning message explaining what's partial
     * @return Partial extraction metadata
     */
    public ExtractionMetadataModel createPartial(
            UUID extractionKey,
            UUID invoiceKey,
            String sourceFileName,
            OcrResult ocrResult,
            String warningMessage
    ) {
        log.info("Creating partial extraction metadata for: {} - {}", sourceFileName, warningMessage);

        String extractionDataJson = wrapTextAsJson(ocrResult.extractedText());

        return new ExtractionMetadataModel(
                extractionKey,
                invoiceKey,
                sourceFileName,
                java.time.LocalDateTime.now(),
                ExtractionMetadataModel.STATUS_PARTIAL,
                ocrResult.confidenceScore(),
                ocrResult.engineVersion(),
                extractionDataJson,
                warningMessage,
                java.time.LocalDateTime.now()
        );
    }

    /**
     * Wraps extracted text in JSON format for PostgreSQL jsonb column.
     * Escapes special JSON characters to create valid JSON.
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

    /**
     * Determines if an extraction should be marked as partial based on confidence.
     *
     * @param confidenceScore Confidence score from OCR
     * @param threshold Minimum acceptable confidence threshold
     * @return true if confidence is below threshold
     */
    public boolean shouldMarkAsPartial(double confidenceScore, double threshold) {
        return confidenceScore < threshold;
    }
}
