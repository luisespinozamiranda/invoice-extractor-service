package com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.strategy;

import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.IOcrService;
import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Context class that manages OCR strategies.
 * Implements the Strategy Pattern to select the appropriate OCR engine based on file type.
 *
 * <p>This class acts as a facade for all OCR operations, delegating to the appropriate
 * strategy based on:
 * <ul>
 *   <li>File type support</li>
 *   <li>Strategy priority</li>
 * </ul>
 *
 * <p><b>Design Pattern:</b> Strategy Pattern (Context)
 * <p><b>SOLID Principles:</b>
 * <ul>
 *   <li>Open/Closed: New strategies can be added without modifying this class</li>
 *   <li>Dependency Inversion: Depends on OcrStrategy abstraction, not concrete implementations</li>
 * </ul>
 *
 * @see OcrStrategy
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-22
 */
@Service
@Primary
@Slf4j
public class OcrStrategyContext implements IOcrService {

    private final List<OcrStrategy> strategies;

    /**
     * Constructor with dependency injection.
     * Spring automatically injects all beans implementing OcrStrategy.
     *
     * @param strategies List of available OCR strategies
     */
    public OcrStrategyContext(List<OcrStrategy> strategies) {
        this.strategies = strategies;
        log.info("Initialized OcrStrategyContext with {} strategies: {}",
                strategies.size(),
                strategies.stream().map(OcrStrategy::getEngineName).toList());
    }

    @Override
    public CompletableFuture<OcrResult> extractText(byte[] fileData, String fileName, String fileType) {
        log.debug("Selecting OCR strategy for file type: {}", fileType);

        OcrStrategy selectedStrategy = selectStrategy(fileType);

        log.info("Using OCR strategy '{}' for file: {}", selectedStrategy.getEngineName(), fileName);

        return selectedStrategy.extractText(fileData, fileName, fileType);
    }

    @Override
    public CompletableFuture<Boolean> isFormatSupported(String fileType) {
        return CompletableFuture.completedFuture(
                strategies.stream().anyMatch(strategy -> strategy.supports(fileType))
        );
    }

    @Override
    public String getEngineName() {
        return strategies.stream()
                .map(OcrStrategy::getEngineName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("No OCR engines available");
    }

    /**
     * Selects the most appropriate OCR strategy for the given file type.
     * Prioritizes strategies with higher priority values.
     *
     * @param fileType MIME type of the file
     * @return The selected OCR strategy
     * @throws InvoiceExtractorServiceException if no strategy supports the file type
     */
    private OcrStrategy selectStrategy(String fileType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(fileType))
                .max(Comparator.comparingInt(OcrStrategy::getPriority))
                .orElseThrow(() -> {
                    log.error("No OCR strategy found for file type: {}", fileType);
                    return new InvoiceExtractorServiceException(
                            ErrorCodes.INVALID_FILE_TYPE,
                            "Unsupported file type: " + fileType + ". No OCR strategy available."
                    );
                });
    }

    /**
     * Returns all available OCR strategies.
     * Useful for diagnostics and configuration.
     *
     * @return List of registered strategies
     */
    public List<OcrStrategy> getAvailableStrategies() {
        return List.copyOf(strategies);
    }

    /**
     * Returns the number of registered strategies.
     *
     * @return Strategy count
     */
    public int getStrategyCount() {
        return strategies.size();
    }
}
