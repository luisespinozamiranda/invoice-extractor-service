package com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0;

import java.time.LocalDateTime;

/**
 * Result object containing OCR extraction data and metadata.
 * Immutable record representing the outcome of an OCR text extraction operation.
 *
 * <p><b>Architecture:</b> Data Transfer Object (Adapter Layer)
 * <p><b>Immutability:</b> All fields are final (Java record)
 *
 * @param extractedText    The full text extracted from the document
 * @param confidenceScore  Overall confidence score (0.0 to 1.0) of the extraction quality
 * @param pageCount        Number of pages processed
 * @param processingTimeMs Time taken for extraction in milliseconds
 * @param extractionTime   Timestamp when extraction was performed
 * @param engineVersion    OCR engine name and version used
 * @param language         Detected or configured language (e.g., "eng", "spa")
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-09
 */
public record OcrResult(
        String extractedText,
        double confidenceScore,
        int pageCount,
        long processingTimeMs,
        LocalDateTime extractionTime,
        String engineVersion,
        String language
) {

    /**
     * Factory method for successful OCR extraction.
     *
     * @param extractedText    Text extracted from document
     * @param confidenceScore  Confidence score (0.0 to 1.0)
     * @param pageCount        Number of pages processed
     * @param processingTimeMs Processing duration in milliseconds
     * @param engineVersion    OCR engine identification
     * @param language         Document language
     * @return New OcrResult instance
     */
    public static OcrResult success(
            String extractedText,
            double confidenceScore,
            int pageCount,
            long processingTimeMs,
            String engineVersion,
            String language
    ) {
        return new OcrResult(
                extractedText,
                Math.max(0.0, Math.min(1.0, confidenceScore)), // Clamp to [0.0, 1.0]
                pageCount,
                processingTimeMs,
                LocalDateTime.now(),
                engineVersion,
                language
        );
    }

    /**
     * Check if extraction has acceptable confidence.
     *
     * @param threshold Minimum acceptable confidence (0.0 to 1.0)
     * @return true if confidence meets or exceeds threshold
     */
    public boolean hasAcceptableConfidence(double threshold) {
        return confidenceScore >= threshold;
    }

    /**
     * Check if extracted text is empty or blank.
     *
     * @return true if no text was extracted
     */
    public boolean isEmpty() {
        return extractedText == null || extractedText.isBlank();
    }
}
