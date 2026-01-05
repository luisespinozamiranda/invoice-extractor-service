package com.training.service.invoiceextractor.domain.events.listeners;

import com.training.service.invoiceextractor.domain.events.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event listener that tracks extraction metrics and statistics.
 * Implements the Observer Pattern as a concrete observer.
 *
 * <p><b>Design Pattern:</b> Observer Pattern (Concrete Observer)
 * <p><b>SOLID Principles:</b>
 * <ul>
 *   <li>Single Responsibility: Only handles metrics collection</li>
 *   <li>Open/Closed: New metrics can be added without modifying existing logic</li>
 * </ul>
 *
 * <p>This is a basic implementation. In production, consider using:
 * <ul>
 *   <li>Micrometer for metrics collection</li>
 *   <li>Prometheus for metrics storage</li>
 *   <li>Grafana for metrics visualization</li>
 * </ul>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-22
 */
@Component
@Slf4j
public class MetricsExtractionListener {

    private final AtomicInteger totalExtractions = new AtomicInteger(0);
    private final AtomicInteger successfulExtractions = new AtomicInteger(0);
    private final AtomicInteger failedExtractions = new AtomicInteger(0);
    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);

    private final ConcurrentHashMap<java.util.UUID, LocalDateTime> extractionStartTimes
            = new ConcurrentHashMap<>();

    /**
     * Handles extraction started events to track start time.
     */
    @Async
    @EventListener
    public void handleExtractionStarted(ExtractionStartedEvent event) {
        totalExtractions.incrementAndGet();
        extractionStartTimes.put(event.extractionKey(), event.timestamp());
        log.debug("Metrics: Extraction started - Total: {}", totalExtractions.get());
    }

    /**
     * Handles extraction completed events to track success metrics.
     */
    @Async
    @EventListener
    public void handleExtractionCompleted(ExtractionCompletedEvent event) {
        successfulExtractions.incrementAndGet();
        recordProcessingTime(event.extractionKey(), event.timestamp());

        double successRate = calculateSuccessRate();
        long avgProcessingTime = calculateAverageProcessingTime();

        log.info("Metrics: Extraction completed - Success rate: {:.2f}%, Avg time: {}ms",
                successRate, avgProcessingTime);
    }

    /**
     * Handles extraction failed events to track failure metrics.
     */
    @Async
    @EventListener
    public void handleExtractionFailed(ExtractionFailedEvent event) {
        failedExtractions.incrementAndGet();
        recordProcessingTime(event.extractionKey(), event.timestamp());

        double failureRate = calculateFailureRate();

        log.warn("Metrics: Extraction failed - Failure rate: {:.2f}%", failureRate);
    }

    /**
     * Handles OCR completed events to track OCR-specific metrics.
     */
    @Async
    @EventListener
    public void handleOcrCompleted(OcrCompletedEvent event) {
        log.debug("Metrics: OCR confidence - {} (score: {:.2f})",
                event.extractionKey(), event.confidenceScore());

        // In production, this would record to a metrics system
        // Example: meterRegistry.gauge("ocr.confidence", event.confidenceScore());
    }

    /**
     * Handles LLM completed events to track LLM-specific metrics.
     */
    @Async
    @EventListener
    public void handleLlmCompleted(LlmCompletedEvent event) {
        log.debug("Metrics: LLM confidence - {} (score: {:.2f})",
                event.extractionKey(), event.confidence());

        // In production, this would record to a metrics system
        // Example: meterRegistry.gauge("llm.confidence", event.confidence());
    }

    /**
     * Records the processing time for an extraction.
     */
    private void recordProcessingTime(java.util.UUID extractionKey, LocalDateTime endTime) {
        LocalDateTime startTime = extractionStartTimes.remove(extractionKey);
        if (startTime != null) {
            Duration duration = Duration.between(startTime, endTime);
            long durationMs = duration.toMillis();
            totalProcessingTimeMs.addAndGet(durationMs);

            log.debug("Metrics: Processing time for {} - {}ms", extractionKey, durationMs);
        }
    }

    /**
     * Calculates the success rate percentage.
     */
    private double calculateSuccessRate() {
        int total = successfulExtractions.get() + failedExtractions.get();
        if (total == 0) return 0.0;
        return (successfulExtractions.get() * 100.0) / total;
    }

    /**
     * Calculates the failure rate percentage.
     */
    private double calculateFailureRate() {
        int total = successfulExtractions.get() + failedExtractions.get();
        if (total == 0) return 0.0;
        return (failedExtractions.get() * 100.0) / total;
    }

    /**
     * Calculates the average processing time in milliseconds.
     */
    private long calculateAverageProcessingTime() {
        int completedExtractions = successfulExtractions.get() + failedExtractions.get();
        if (completedExtractions == 0) return 0;
        return totalProcessingTimeMs.get() / completedExtractions;
    }

    /**
     * Returns current metrics as a formatted string.
     * Useful for health checks and monitoring endpoints.
     */
    public String getMetricsSummary() {
        return String.format(
                "Extraction Metrics - Total: %d, Successful: %d, Failed: %d, " +
                "Success Rate: %.2f%%, Avg Time: %dms",
                totalExtractions.get(),
                successfulExtractions.get(),
                failedExtractions.get(),
                calculateSuccessRate(),
                calculateAverageProcessingTime()
        );
    }

    /**
     * Resets all metrics. Use with caution.
     */
    public void resetMetrics() {
        totalExtractions.set(0);
        successfulExtractions.set(0);
        failedExtractions.set(0);
        totalProcessingTimeMs.set(0);
        extractionStartTimes.clear();
        log.info("Metrics: All metrics have been reset");
    }
}
