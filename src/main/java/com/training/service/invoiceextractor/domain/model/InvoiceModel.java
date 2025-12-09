package com.training.service.invoiceextractor.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing an invoice with extracted information.
 * Immutable record for invoice data in the domain layer.
 *
 * Extracted fields:
 * - Invoice number
 * - Invoice amount
 * - Client name
 * - Client address
 */
public record InvoiceModel(
        UUID invoiceKey,
        String invoiceNumber,
        BigDecimal invoiceAmount,
        String clientName,
        String clientAddress,

        // Additional metadata
        String currency,
        String status,
        String originalFileName,

        // Audit fields
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Validates that required fields are not null.
     */
    public InvoiceModel {
        if (invoiceKey == null) {
            throw new IllegalArgumentException("Invoice key cannot be null");
        }
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new IllegalArgumentException("Invoice number cannot be null or blank");
        }
        if (invoiceAmount == null) {
            throw new IllegalArgumentException("Invoice amount cannot be null");
        }
        if (clientName == null || clientName.isBlank()) {
            throw new IllegalArgumentException("Client name cannot be null or blank");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be null or blank");
        }
    }

    /**
     * Invoice status constants.
     */
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_EXTRACTED = "EXTRACTED";
    public static final String STATUS_EXTRACTION_FAILED = "EXTRACTION_FAILED";
    public static final String STATUS_PENDING = "PENDING";

    /**
     * Create a new invoice with generated UUID and timestamps.
     */
    public static InvoiceModel create(
            String invoiceNumber,
            BigDecimal invoiceAmount,
            String clientName,
            String clientAddress,
            String currency,
            String status,
            String originalFileName
    ) {
        return new InvoiceModel(
                UUID.randomUUID(),
                invoiceNumber,
                invoiceAmount,
                clientName,
                clientAddress,
                currency != null ? currency : "USD",
                status,
                originalFileName,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    /**
     * Create a copy with updated status.
     */
    public InvoiceModel withStatus(String newStatus) {
        return new InvoiceModel(
                invoiceKey,
                invoiceNumber,
                invoiceAmount,
                clientName,
                clientAddress,
                currency,
                newStatus,
                originalFileName,
                createdAt,
                LocalDateTime.now()
        );
    }

    /**
     * Create a copy with updated amount.
     */
    public InvoiceModel withAmount(BigDecimal newAmount) {
        return new InvoiceModel(
                invoiceKey,
                invoiceNumber,
                newAmount,
                clientName,
                clientAddress,
                currency,
                status,
                originalFileName,
                createdAt,
                LocalDateTime.now()
        );
    }

    /**
     * Create a copy with updated client information.
     */
    public InvoiceModel withClientInfo(String newClientName, String newClientAddress) {
        return new InvoiceModel(
                invoiceKey,
                invoiceNumber,
                invoiceAmount,
                newClientName,
                newClientAddress,
                currency,
                status,
                originalFileName,
                createdAt,
                LocalDateTime.now()
        );
    }

    /**
     * Check if the invoice is in processing state.
     */
    public boolean isProcessing() {
        return STATUS_PROCESSING.equals(status);
    }

    /**
     * Check if the invoice has been successfully extracted.
     */
    public boolean isExtracted() {
        return STATUS_EXTRACTED.equals(status);
    }

    /**
     * Check if the extraction failed.
     */
    public boolean isExtractionFailed() {
        return STATUS_EXTRACTION_FAILED.equals(status);
    }
}
