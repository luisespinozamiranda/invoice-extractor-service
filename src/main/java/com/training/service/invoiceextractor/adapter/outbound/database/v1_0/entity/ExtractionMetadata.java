package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for tb_extraction_metadata table.
 * Stores OCR extraction metadata and results.
 */
@Entity
@Table(name = "tb_extraction_metadata", schema = "invoicedata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractionMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "extraction_key", nullable = false, unique = true, columnDefinition = "UUID")
    private UUID extractionKey;

    @Column(name = "invoice_key", columnDefinition = "UUID")
    private UUID invoiceKey;

    @Column(name = "source_file_name", nullable = false, length = 255)
    private String sourceFileName;

    @Column(name = "extraction_timestamp", nullable = false)
    private LocalDateTime extractionTimestamp;

    @Column(name = "extraction_status", nullable = false, length = 50)
    private String extractionStatus;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "ocr_engine", length = 100)
    private String ocrEngine;

    /**
     * JSONB column for storing raw extraction data.
     * PostgreSQL-specific type for efficient JSON storage and querying.
     */
    @Column(name = "extraction_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String extractionData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * Pre-persist callback to set timestamps and generate UUID.
     */
    @PrePersist
    protected void onCreate() {
        if (extractionKey == null) {
            extractionKey = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (extractionTimestamp == null) {
            extractionTimestamp = LocalDateTime.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}
