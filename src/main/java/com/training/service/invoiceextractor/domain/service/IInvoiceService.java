package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.domain.model.InvoiceModel;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Domain service interface defining business operations for invoice management.
 *
 * <p>This interface represents the port in hexagonal architecture, defining the contract
 * for invoice business logic without exposing implementation details.
 *
 * <p><b>Design Pattern:</b> Port (Hexagonal Architecture)
 * <p><b>Async Contract:</b> All methods return {@link CompletableFuture} for non-blocking execution
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see InvoiceModel
 * @see InvoiceService
 */
public interface IInvoiceService {

    /**
     * Retrieve an invoice by its unique key.
     *
     * @param invoiceKey UUID of the invoice
     * @return CompletableFuture with InvoiceModel
     */
    CompletableFuture<InvoiceModel> getInvoiceByKey(UUID invoiceKey);

    /**
     * Retrieve all active invoices (not deleted).
     *
     * @return CompletableFuture with list of InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> getAllInvoices();

    /**
     * Retrieve all invoices including deleted ones.
     *
     * @return CompletableFuture with list of all InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> getAllInvoicesIncludingDeleted();

    /**
     * Create a new invoice.
     *
     * @param invoice InvoiceModel to create
     * @return CompletableFuture with created InvoiceModel
     */
    CompletableFuture<InvoiceModel> createInvoice(InvoiceModel invoice);

    /**
     * Update an existing invoice.
     *
     * @param invoiceKey UUID of the invoice to update
     * @param invoice    Updated invoice data
     * @return CompletableFuture with updated InvoiceModel
     */
    CompletableFuture<InvoiceModel> updateInvoice(UUID invoiceKey, InvoiceModel invoice);

    /**
     * Soft delete an invoice (logical deletion).
     * Sets is_deleted flag to TRUE instead of physically removing the record.
     *
     * @param invoiceKey UUID of the invoice to delete
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> deleteInvoice(UUID invoiceKey);

    /**
     * Restore a soft-deleted invoice.
     * Sets is_deleted flag to FALSE to reactivate the invoice.
     *
     * @param invoiceKey UUID of the invoice to restore
     * @return CompletableFuture<InvoiceModel> with restored invoice
     */
    CompletableFuture<InvoiceModel> restoreInvoice(UUID invoiceKey);

    /**
     * Check if an invoice exists (not deleted).
     *
     * @param invoiceKey UUID of the invoice
     * @return CompletableFuture<Boolean> true if exists and not deleted
     */
    CompletableFuture<Boolean> invoiceExists(UUID invoiceKey);

    /**
     * Search invoices by invoice number.
     *
     * @param invoiceNumber Invoice number to search
     * @return CompletableFuture with list of matching InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> findByInvoiceNumber(String invoiceNumber);

    /**
     * Search invoices by client name (partial match, case-insensitive).
     *
     * @param clientName Client name to search
     * @return CompletableFuture with list of matching InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> findByClientName(String clientName);

    /**
     * Get invoices by status.
     *
     * @param status Invoice status (PROCESSING, EXTRACTED, EXTRACTION_FAILED)
     * @return CompletableFuture with list of InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> getInvoicesByStatus(String status);
}
