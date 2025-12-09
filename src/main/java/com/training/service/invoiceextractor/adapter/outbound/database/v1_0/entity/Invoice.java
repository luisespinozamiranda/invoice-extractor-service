package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for tb_invoice table.
 * Stores invoice information extracted from documents.
 *
 * Extracted fields:
 * - Invoice number
 * - Invoice amount
 * - Client name
 * - Client address
 */
@Entity
@Table(name = "tb_invoice", schema = "invoicedata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "invoice_key", nullable = false, unique = true, columnDefinition = "UUID")
    private UUID invoiceKey;

    // Extracted fields from invoice document
    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal invoiceAmount;

    @Column(name = "client_name", nullable = false, length = 255)
    private String clientName;

    @Column(name = "client_address", columnDefinition = "TEXT")
    private String clientAddress;

    // Additional metadata
    @Builder.Default
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * Pre-persist callback to set timestamps and generate UUID.
     */
    @PrePersist
    protected void onCreate() {
        if (invoiceKey == null) {
            invoiceKey = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    /**
     * Pre-update callback to update timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
