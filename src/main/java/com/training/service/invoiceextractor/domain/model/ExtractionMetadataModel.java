package com.training.service.invoiceextractor.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing extraction metadata.
 * Contains information about the OCR extraction process and results.
 */
public record ExtractionMetadataModel(
        UUID extractionKey,
        UUID invoiceKey,
        String sourceFileName,
        LocalDateTime extractionTimestamp,
        String extractionStatus,
        Double confidenceScore,
        String ocrEngine,
        String extractionData,
        String errorMessage,
        LocalDateTime createdAt
) {
    /**
     * Validates that required fields are not null.
     */
    public ExtractionMetadataModel {
        if (extractionKey == null) {
            throw new IllegalArgumentException("Extraction key cannot be null");
        }
        if (sourceFileName == null || sourceFileName.isBlank()) {
            throw new IllegalArgumentException("Source file name cannot be null or blank");
        }
        if (extractionStatus == null || extractionStatus.isBlank()) {
            throw new IllegalArgumentException("Extraction status cannot be null or blank");
        }
    }

    /**
     * Extraction status constants.
     */
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PARTIAL = "PARTIAL";

    /**
     * Create a new extraction metadata for a processing state.
     */
    public static ExtractionMetadataModel createProcessing(
            String sourceFileName
    ) {
        return new ExtractionMetadataModel(
                UUID.randomUUID(),
                null, // Invoice not yet created
                sourceFileName,
                LocalDateTime.now(),
                STATUS_PROCESSING,
                null,
                null,
                null,
                null,
                LocalDateTime.now()
        );
    }

    /**
     * Create a completed extraction metadata.
     */
    public static ExtractionMetadataModel createCompleted(
            UUID extractionKey,
            UUID invoiceKey,
            String sourceFileName,
            Double confidenceScore,
            String ocrEngine,
            String extractionData
    ) {
        return new ExtractionMetadataModel(
                extractionKey,
                invoiceKey,
                sourceFileName,
                LocalDateTime.now(),
                STATUS_COMPLETED,
                confidenceScore,
                ocrEngine,
                extractionData,
                null,
                LocalDateTime.now()
        );
    }

    /**
     * Create a failed extraction metadata.
     */
    public static ExtractionMetadataModel createFailed(
            UUID extractionKey,
            String sourceFileName,
            String errorMessage
    ) {
        return new ExtractionMetadataModel(
                extractionKey,
                null,
                sourceFileName,
                LocalDateTime.now(),
                STATUS_FAILED,
                0.0,
                null,
                null,
                errorMessage,
                LocalDateTime.now()
        );
    }

    /**
     * Check if extraction was successful.
     */
    public boolean isSuccessful() {
        return STATUS_COMPLETED.equals(extractionStatus);
    }

    /**
     * Check if extraction failed.
     */
    public boolean isFailed() {
        return STATUS_FAILED.equals(extractionStatus);
    }

    /**
     * Check if extraction is still processing.
     */
    public boolean isProcessing() {
        return STATUS_PROCESSING.equals(extractionStatus);
    }

    /**
     * Check if confidence score is above threshold.
     */
    public boolean hasHighConfidence(double threshold) {
        return confidenceScore != null && confidenceScore >= threshold;
    }
}
