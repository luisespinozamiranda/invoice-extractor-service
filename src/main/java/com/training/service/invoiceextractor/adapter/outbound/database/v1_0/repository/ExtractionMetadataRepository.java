package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.entity.ExtractionMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for ExtractionMetadata entity.
 * Provides database access for extraction metadata operations.
 */
@Repository
public interface ExtractionMetadataRepository extends JpaRepository<ExtractionMetadata, Long> {

    /**
     * Find extraction metadata by its unique business key.
     *
     * @param extractionKey UUID of the extraction
     * @return Optional containing ExtractionMetadata if found
     */
    Optional<ExtractionMetadata> findByExtractionKey(UUID extractionKey);

    /**
     * Find extraction metadata by extraction key that is not deleted.
     *
     * @param extractionKey UUID of the extraction
     * @return Optional containing ExtractionMetadata if found and not deleted
     */
    Optional<ExtractionMetadata> findByExtractionKeyAndIsDeletedFalse(UUID extractionKey);

    /**
     * Find all extraction metadata for a specific invoice.
     *
     * @param invoiceKey UUID of the invoice
     * @return List of extraction metadata for the invoice
     */
    List<ExtractionMetadata> findByInvoiceKeyAndIsDeletedFalse(UUID invoiceKey);

    /**
     * Find all extractions by status.
     *
     * @param status Extraction status (PROCESSING, COMPLETED, FAILED)
     * @return List of extraction metadata with matching status
     */
    List<ExtractionMetadata> findByExtractionStatusAndIsDeletedFalse(String status);

    /**
     * Find all active extraction metadata (not deleted).
     *
     * @return List of active extraction metadata
     */
    List<ExtractionMetadata> findByIsDeletedFalse();

    /**
     * Find extractions with confidence score below threshold.
     *
     * @param threshold Confidence threshold
     * @return List of low-confidence extractions
     */
    List<ExtractionMetadata> findByConfidenceScoreLessThanAndIsDeletedFalse(Double threshold);
}
