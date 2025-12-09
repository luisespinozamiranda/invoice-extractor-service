package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Invoice entity.
 * Provides database access for invoice operations.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Find invoice by its unique business key.
     *
     * @param invoiceKey UUID of the invoice
     * @return Optional containing Invoice if found
     */
    Optional<Invoice> findByInvoiceKey(UUID invoiceKey);

    /**
     * Find invoice by invoice key that is not deleted.
     *
     * @param invoiceKey UUID of the invoice
     * @return Optional containing Invoice if found and not deleted
     */
    Optional<Invoice> findByInvoiceKeyAndIsDeletedFalse(UUID invoiceKey);

    /**
     * Find all active invoices (not deleted).
     *
     * @return List of active invoices
     */
    List<Invoice> findByIsDeletedFalse();

    /**
     * Find all invoices including deleted ones.
     *
     * @return List of all invoices
     */
    List<Invoice> findAll();

    /**
     * Find invoices by invoice number (exact match).
     *
     * @param invoiceNumber Invoice number to search
     * @return List of invoices with matching invoice number
     */
    List<Invoice> findByInvoiceNumberAndIsDeletedFalse(String invoiceNumber);

    /**
     * Find invoices by client name (partial match, case-insensitive).
     *
     * @param clientName Client name to search (partial match)
     * @return List of invoices with matching client name
     */
    List<Invoice> findByClientNameContainingIgnoreCaseAndIsDeletedFalse(String clientName);

    /**
     * Find invoices by status.
     *
     * @param status Invoice status to filter
     * @return List of invoices with matching status
     */
    List<Invoice> findByStatusAndIsDeletedFalse(String status);

    /**
     * Check if an invoice exists by invoice key and is not deleted.
     *
     * @param invoiceKey UUID of the invoice
     * @return true if invoice exists and is not deleted
     */
    boolean existsByInvoiceKeyAndIsDeletedFalse(UUID invoiceKey);
}
