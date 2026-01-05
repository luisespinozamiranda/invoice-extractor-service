package com.training.service.invoiceextractor.domain.factory;

import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.InvoiceData;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Factory for creating InvoiceModel instances.
 * Centralizes invoice creation logic and provides various factory methods.
 *
 * <p><b>Design Pattern:</b> Factory Pattern
 * <p><b>SOLID Principles:</b>
 * <ul>
 *   <li>Single Responsibility: Focused solely on invoice creation logic</li>
 *   <li>Open/Closed: New creation methods can be added without modifying existing ones</li>
 * </ul>
 *
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Centralized validation and default value logic</li>
 *   <li>Consistent invoice creation across the application</li>
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
public class InvoiceFactory {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final String UNKNOWN_CLIENT = "Unknown Client";
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;

    /**
     * Creates an invoice from LLM-extracted data.
     * Applies validation and default values for missing fields.
     *
     * @param llmData LLM extraction result
     * @param fileName Original file name
     * @return Created invoice model
     */
    public InvoiceModel createFromLlmData(InvoiceData llmData, String fileName) {
        log.debug("Creating invoice from LLM data for file: {}", fileName);

        String invoiceNumber = llmData.invoiceNumber()
                .filter(this::isValidInvoiceNumber)
                .orElseGet(() -> generateInvoiceNumber(fileName));

        BigDecimal amount = llmData.amount()
                .filter(this::isValidAmount)
                .orElse(ZERO_AMOUNT);

        String clientName = llmData.clientName()
                .filter(this::isValidClientName)
                .orElse(UNKNOWN_CLIENT);

        String clientAddress = llmData.clientAddress().orElse(null);

        String currency = normalizeCurrency(llmData.currency());

        return InvoiceModel.create(
                invoiceNumber,
                amount,
                clientName,
                clientAddress,
                currency,
                InvoiceModel.STATUS_EXTRACTED,
                fileName
        );
    }

    /**
     * Creates a processing invoice placeholder.
     * Used when invoice extraction is in progress but not yet completed.
     *
     * @param fileName Original file name
     * @return Processing invoice model
     */
    public InvoiceModel createProcessing(String fileName) {
        log.debug("Creating processing invoice for file: {}", fileName);

        return InvoiceModel.create(
                generateInvoiceNumber(fileName),
                ZERO_AMOUNT,
                UNKNOWN_CLIENT,
                null,
                DEFAULT_CURRENCY,
                InvoiceModel.STATUS_PROCESSING,
                fileName
        );
    }

    /**
     * Creates a failed invoice.
     * Used when extraction fails but we need to track the attempt.
     *
     * @param fileName Original file name
     * @param errorMessage Error message
     * @return Failed invoice model
     */
    public InvoiceModel createFailed(String fileName, String errorMessage) {
        log.warn("Creating failed invoice for file: {} - {}", fileName, errorMessage);

        return InvoiceModel.create(
                generateInvoiceNumber(fileName) + "-FAILED",
                ZERO_AMOUNT,
                UNKNOWN_CLIENT,
                null,
                DEFAULT_CURRENCY,
                InvoiceModel.STATUS_EXTRACTION_FAILED,
                fileName
        );
    }

    /**
     * Creates an invoice from manual input data.
     * Used for manual invoice creation or correction.
     *
     * @param invoiceNumber Invoice number
     * @param amount Invoice amount
     * @param clientName Client name
     * @param clientAddress Client address (optional)
     * @param currency Currency code
     * @param fileName Original file name
     * @return Created invoice model
     */
    public InvoiceModel createManual(
            String invoiceNumber,
            BigDecimal amount,
            String clientName,
            String clientAddress,
            String currency,
            String fileName
    ) {
        log.debug("Creating manual invoice: {}", invoiceNumber);

        return InvoiceModel.create(
                invoiceNumber,
                amount,
                clientName,
                clientAddress,
                normalizeCurrency(currency),
                InvoiceModel.STATUS_EXTRACTED,
                fileName
        );
    }

    /**
     * Generates a unique invoice number based on file name and UUID.
     *
     * @param fileName Original file name
     * @return Generated invoice number
     */
    private String generateInvoiceNumber(String fileName) {
        String sanitizedFileName = sanitizeFileName(fileName);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("INV-%s-%s", sanitizedFileName, uniqueId);
    }

    /**
     * Sanitizes file name for use in invoice number.
     *
     * @param fileName Original file name
     * @return Sanitized file name
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "UNKNOWN";
        }

        // Remove file extension
        String nameWithoutExt = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;

        // Keep only alphanumeric characters and convert to uppercase
        return nameWithoutExt
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase()
                .substring(0, Math.min(nameWithoutExt.length(), 10));
    }

    /**
     * Validates invoice number.
     *
     * @param invoiceNumber Invoice number to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidInvoiceNumber(String invoiceNumber) {
        return invoiceNumber != null
                && !invoiceNumber.isBlank()
                && !invoiceNumber.equalsIgnoreCase("null")
                && !invoiceNumber.equalsIgnoreCase("unknown")
                && invoiceNumber.length() >= 3;
    }

    /**
     * Validates amount.
     *
     * @param amount Amount to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Validates client name.
     *
     * @param clientName Client name to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidClientName(String clientName) {
        return clientName != null
                && !clientName.isBlank()
                && !clientName.equalsIgnoreCase("null")
                && !clientName.equalsIgnoreCase("unknown")
                && clientName.length() >= 2;
    }

    /**
     * Normalizes currency code to uppercase 3-letter ISO format.
     *
     * @param currency Currency code
     * @return Normalized currency code
     */
    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }

        String normalized = currency.trim().toUpperCase();

        // Basic ISO 4217 validation (3 letters)
        if (normalized.matches("[A-Z]{3}")) {
            return normalized;
        }

        log.warn("Invalid currency code '{}', using default: {}", currency, DEFAULT_CURRENCY);
        return DEFAULT_CURRENCY;
    }
}
