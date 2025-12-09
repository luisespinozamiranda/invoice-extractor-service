package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IInvoiceRepositoryService;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Domain service implementation for invoice business logic.
 *
 * <p>This service encapsulates all business rules and operations related to invoice management.
 * It acts as the core business logic layer in the hexagonal architecture, isolated from
 * infrastructure concerns.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Execute invoice business operations (CRUD)</li>
 *   <li>Validate business rules before persistence</li>
 *   <li>Transform exceptions into domain-specific errors</li>
 *   <li>Coordinate with repository services for data persistence</li>
 * </ul>
 *
 * <p><b>Architecture:</b> Domain Layer (Hexagonal Architecture)
 * <p><b>Thread Safety:</b> All methods are thread-safe and return {@link CompletableFuture}
 * for asynchronous processing.
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see IInvoiceService
 * @see InvoiceModel
 * @see IInvoiceRepositoryService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService implements IInvoiceService {

    private final IInvoiceRepositoryService invoiceRepositoryService;

    /**
     * Retrieves an invoice by its unique key.
     *
     * <p>This method performs an asynchronous lookup of an invoice using its UUID key.
     * If the invoice is not found or an error occurs, it throws a domain exception.
     *
     * @param invoiceKey the unique identifier of the invoice, must not be null
     * @return a {@link CompletableFuture} containing the invoice model if found
     * @throws InvoiceExtractorServiceException with {@link ErrorCodes#INVOICE_NOT_FOUND}
     *         if the invoice does not exist or an error occurs during retrieval
     * @see InvoiceModel
     */
    @Override
    public CompletableFuture<InvoiceModel> getInvoiceByKey(UUID invoiceKey) {
        log.debug("Domain: Getting invoice by key: {}", invoiceKey);

        return invoiceRepositoryService.findByInvoiceKey(invoiceKey)
                .exceptionally(ex -> {
                    log.error("Error getting invoice by key: {}", invoiceKey, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.INVOICE_NOT_FOUND,
                            "Invoice not found with key: " + invoiceKey
                    );
                });
    }

    @Override
    public CompletableFuture<List<InvoiceModel>> getAllInvoices() {
        log.debug("Domain: Getting all active invoices");

        return invoiceRepositoryService.findAllActive()
                .exceptionally(ex -> {
                    log.error("Error getting all invoices", ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to retrieve invoices"
                    );
                });
    }

    @Override
    public CompletableFuture<List<InvoiceModel>> getAllInvoicesIncludingDeleted() {
        log.debug("Domain: Getting all invoices including deleted");

        return invoiceRepositoryService.findAll()
                .exceptionally(ex -> {
                    log.error("Error getting all invoices including deleted", ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to retrieve all invoices"
                    );
                });
    }

    @Override
    public CompletableFuture<InvoiceModel> createInvoice(InvoiceModel invoice) {
        log.debug("Domain: Creating invoice: {}", invoice.invoiceNumber());

        return invoiceRepositoryService.save(invoice)
                .exceptionally(ex -> {
                    log.error("Error creating invoice: {}", invoice.invoiceNumber(), ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to create invoice"
                    );
                });
    }

    @Override
    public CompletableFuture<InvoiceModel> updateInvoice(UUID invoiceKey, InvoiceModel invoice) {
        log.debug("Domain: Updating invoice with key: {}", invoiceKey);

        return invoiceRepositoryService.findByInvoiceKey(invoiceKey)
                .thenCompose(existingInvoice -> {
                    if (existingInvoice == null) {
                        throw new InvoiceExtractorServiceException(
                                ErrorCodes.INVOICE_NOT_FOUND,
                                invoiceKey.toString()
                        );
                    }

                    // Create updated invoice with existing key
                    InvoiceModel updatedInvoice = new InvoiceModel(
                            invoiceKey, // Keep existing key
                            invoice.invoiceNumber(),
                            invoice.invoiceAmount(),
                            invoice.clientName(),
                            invoice.clientAddress(),
                            invoice.currency(),
                            invoice.status(),
                            invoice.originalFileName(),
                            existingInvoice.createdAt(), // Keep original createdAt
                            java.time.LocalDateTime.now() // Update updatedAt
                    );

                    return invoiceRepositoryService.save(updatedInvoice);
                })
                .exceptionally(ex -> {
                    log.error("Error updating invoice with key: {}", invoiceKey, ex);
                    if (ex.getCause() instanceof InvoiceExtractorServiceException) {
                        throw (InvoiceExtractorServiceException) ex.getCause();
                    }
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to update invoice"
                    );
                });
    }

    @Override
    public CompletableFuture<Void> deleteInvoice(UUID invoiceKey) {
        log.debug("Domain: Soft deleting invoice with key: {}", invoiceKey);

        return invoiceRepositoryService.softDelete(invoiceKey)
                .exceptionally(ex -> {
                    log.error("Error deleting invoice with key: {}", invoiceKey, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to delete invoice"
                    );
                });
    }

    @Override
    public CompletableFuture<InvoiceModel> restoreInvoice(UUID invoiceKey) {
        log.debug("Domain: Restoring invoice with key: {}", invoiceKey);

        return invoiceRepositoryService.restore(invoiceKey)
                .exceptionally(ex -> {
                    log.error("Error restoring invoice with key: {}", invoiceKey, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to restore invoice"
                    );
                });
    }

    @Override
    public CompletableFuture<Boolean> invoiceExists(UUID invoiceKey) {
        log.debug("Domain: Checking if invoice exists: {}", invoiceKey);

        return invoiceRepositoryService.existsByInvoiceKey(invoiceKey)
                .exceptionally(ex -> {
                    log.error("Error checking invoice existence: {}", invoiceKey, ex);
                    return false;
                });
    }

    @Override
    public CompletableFuture<List<InvoiceModel>> findByInvoiceNumber(String invoiceNumber) {
        log.debug("Domain: Finding invoices by number: {}", invoiceNumber);

        return invoiceRepositoryService.findByInvoiceNumber(invoiceNumber)
                .exceptionally(ex -> {
                    log.error("Error finding invoices by number: {}", invoiceNumber, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to search invoices by number"
                    );
                });
    }

    @Override
    public CompletableFuture<List<InvoiceModel>> findByClientName(String clientName) {
        log.debug("Domain: Finding invoices by client name: {}", clientName);

        return invoiceRepositoryService.findByClientName(clientName)
                .exceptionally(ex -> {
                    log.error("Error finding invoices by client name: {}", clientName, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to search invoices by client name"
                    );
                });
    }

    @Override
    public CompletableFuture<List<InvoiceModel>> getInvoicesByStatus(String status) {
        log.debug("Domain: Getting invoices by status: {}", status);

        return invoiceRepositoryService.findByStatus(status)
                .exceptionally(ex -> {
                    log.error("Error getting invoices by status: {}", status, ex);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.DATABASE_ERROR,
                            "Failed to get invoices by status"
                    );
                });
    }
}
