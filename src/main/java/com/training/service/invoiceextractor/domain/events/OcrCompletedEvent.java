package com.training.service.invoiceextractor.domain.events;

import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when OCR extraction completes successfully.
 *
 * @param extractionKey Unique extraction identifier
 * @param ocrResult OCR extraction result
 * @param timestamp When the OCR completed
 */
public record OcrCompletedEvent(
        UUID extractionKey,
        OcrResult ocrResult,
        LocalDateTime timestamp
) implements ExtractionEvent {

    public OcrCompletedEvent(UUID extractionKey, OcrResult ocrResult) {
        this(extractionKey, ocrResult, LocalDateTime.now());
    }

    @Override
    public ExtractionEventType eventType() {
        return ExtractionEventType.OCR_COMPLETED;
    }

    public double confidenceScore() {
        return ocrResult.confidenceScore();
    }

    public int pageCount() {
        return ocrResult.pageCount();
    }

    public String engineVersion() {
        return ocrResult.engineVersion();
    }
}
