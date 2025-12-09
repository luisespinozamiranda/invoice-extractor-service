package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.InvoiceV1_0;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller Service interface for Invoice operations (API v1.0)
 * Inbound adapter port for REST layer
 */
public interface IInvoiceControllerServiceV1_0 {

    /**
     * Get invoice by key
     *
     * @param invoiceKey The invoice key
     * @return CompletableFuture with ResponseEntity containing invoice DTO
     */
    CompletableFuture<ResponseEntity<InvoiceV1_0>> getInvoiceByKey(UUID invoiceKey);

    /**
     * Get all invoices
     *
     * @return CompletableFuture with ResponseEntity containing list of invoice DTOs
     */
    CompletableFuture<ResponseEntity<List<InvoiceV1_0>>> getAllInvoices();

    /**
     * Create a new invoice
     *
     * @param invoiceDto The invoice DTO
     * @return CompletableFuture with ResponseEntity containing created invoice DTO
     */
    CompletableFuture<ResponseEntity<InvoiceV1_0>> createInvoice(InvoiceV1_0 invoiceDto);

    /**
     * Update an existing invoice
     *
     * @param invoiceKey The invoice key
     * @param invoiceDto The updated invoice DTO
     * @return CompletableFuture with ResponseEntity containing updated invoice DTO
     */
    CompletableFuture<ResponseEntity<InvoiceV1_0>> updateInvoice(UUID invoiceKey, InvoiceV1_0 invoiceDto);

    /**
     * Delete invoice (soft delete)
     *
     * @param invoiceKey The invoice key
     * @return CompletableFuture with ResponseEntity void
     */
    CompletableFuture<ResponseEntity<Void>> deleteInvoice(UUID invoiceKey);

    /**
     * Search invoices by client name
     *
     * @param clientName The client name to search for
     * @return CompletableFuture with ResponseEntity containing list of invoice DTOs
     */
    CompletableFuture<ResponseEntity<List<InvoiceV1_0>>> searchByClientName(String clientName);
}
