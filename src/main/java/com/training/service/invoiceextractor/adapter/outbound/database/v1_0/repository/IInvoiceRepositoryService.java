package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository;

import com.training.service.invoiceextractor.domain.model.InvoiceModel;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository service interface (Port) for invoice persistence operations.
 * Defines the contract between domain layer and database layer.
 * All operations are async and return CompletableFuture.
 */
public interface IInvoiceRepositoryService {

    /**
     * Find invoice by its unique business key.
     *
     * @param invoiceKey UUID of the invoice
     * @return CompletableFuture with InvoiceModel if found
     */
    CompletableFuture<InvoiceModel> findByInvoiceKey(UUID invoiceKey);

    /**
     * Find all active invoices (not deleted).
     *
     * @return CompletableFuture with list of InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> findAllActive();

    /**
     * Find all invoices including deleted ones.
     *
     * @return CompletableFuture with list of all InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> findAll();

    /**
     * Save a new invoice.
     *
     * @param invoiceModel Invoice to save
     * @return CompletableFuture with saved InvoiceModel
     */
    CompletableFuture<InvoiceModel> save(InvoiceModel invoiceModel);

    /**
     * Update an existing invoice.
     *
     * @param invoiceKey   UUID of the invoice to update
     * @param invoiceModel Updated invoice data
     * @return CompletableFuture with updated InvoiceModel
     */
    CompletableFuture<InvoiceModel> update(UUID invoiceKey, InvoiceModel invoiceModel);

    /**
     * Soft delete an invoice by setting is_deleted flag to true.
     *
     * @param invoiceKey UUID of the invoice to delete
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> softDelete(UUID invoiceKey);

    /**
     * Restore a soft-deleted invoice by setting is_deleted flag to false.
     *
     * @param invoiceKey UUID of the invoice to restore
     * @return CompletableFuture with restored InvoiceModel
     */
    CompletableFuture<InvoiceModel> restore(UUID invoiceKey);

    /**
     * Check if an invoice exists and is not deleted.
     *
     * @param invoiceKey UUID of the invoice
     * @return CompletableFuture<Boolean> true if exists and not deleted
     */
    CompletableFuture<Boolean> existsByInvoiceKey(UUID invoiceKey);

    /**
     * Find invoices by invoice number.
     *
     * @param invoiceNumber Invoice number to search
     * @return CompletableFuture with list of matching InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoices by client name (partial match, case-insensitive).
     *
     * @param clientName Client name to search
     * @return CompletableFuture with list of matching InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> findByClientName(String clientName);

    /**
     * Find invoices by status.
     *
     * @param status Invoice status
     * @return CompletableFuture with list of matching InvoiceModels
     */
    CompletableFuture<List<InvoiceModel>> findByStatus(String status);
}
