package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.impl.mappers;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.entity.ExtractionMetadata;
import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mapper for converting between ExtractionMetadata JPA entity and ExtractionMetadataModel domain model.
 * Handles bidirectional transformation with null safety using Optional.
 */
@Component
public class ExtractionMetadataMapper {

    /**
     * Convert ExtractionMetadata entity to ExtractionMetadataModel domain model.
     *
     * @param entityOptional Optional containing ExtractionMetadata entity
     * @return ExtractionMetadataModel if entity is present, null otherwise
     */
    public ExtractionMetadataModel entityToModel(Optional<ExtractionMetadata> entityOptional) {
        if (entityOptional.isEmpty()) {
            return null;
        }

        ExtractionMetadata entity = entityOptional.get();

        return new ExtractionMetadataModel(
                entity.getExtractionKey(),
                entity.getInvoiceKey(),
                entity.getSourceFileName(),
                entity.getExtractionTimestamp(),
                entity.getExtractionStatus(),
                entity.getConfidenceScore() != null ? entity.getConfidenceScore().doubleValue() : null,
                entity.getOcrEngine(),
                entity.getExtractionData(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }

    /**
     * Convert ExtractionMetadata entity to ExtractionMetadataModel domain model (non-optional version).
     *
     * @param entity ExtractionMetadata entity
     * @return ExtractionMetadataModel
     */
    public ExtractionMetadataModel entityToModel(ExtractionMetadata entity) {
        if (entity == null) {
            return null;
        }

        return new ExtractionMetadataModel(
                entity.getExtractionKey(),
                entity.getInvoiceKey(),
                entity.getSourceFileName(),
                entity.getExtractionTimestamp(),
                entity.getExtractionStatus(),
                entity.getConfidenceScore() != null ? entity.getConfidenceScore().doubleValue() : null,
                entity.getOcrEngine(),
                entity.getExtractionData(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }

    /**
     * Convert ExtractionMetadataModel domain model to ExtractionMetadata entity.
     *
     * @param modelOptional Optional containing ExtractionMetadataModel
     * @return ExtractionMetadata entity if model is present
     * @throws InvoiceExtractorServiceException if model is not present
     */
    public ExtractionMetadata modelToEntity(Optional<ExtractionMetadataModel> modelOptional) {
        if (modelOptional.isEmpty()) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.EXTRACTION_NOT_FOUND,
                    "Cannot convert null ExtractionMetadataModel to entity"
            );
        }

        ExtractionMetadataModel model = modelOptional.get();

        return ExtractionMetadata.builder()
                .extractionKey(model.extractionKey())
                .invoiceKey(model.invoiceKey())
                .sourceFileName(model.sourceFileName())
                .extractionTimestamp(model.extractionTimestamp())
                .extractionStatus(model.extractionStatus())
                .confidenceScore(model.confidenceScore() != null ?
                        java.math.BigDecimal.valueOf(model.confidenceScore()) : null)
                .ocrEngine(model.ocrEngine())
                .extractionData(model.extractionData())
                .errorMessage(model.errorMessage())
                .createdAt(model.createdAt())
                .isDeleted(false) // Always false for new entities
                .build();
    }

    /**
     * Convert ExtractionMetadataModel domain model to ExtractionMetadata entity (non-optional version).
     *
     * @param model ExtractionMetadataModel
     * @return ExtractionMetadata entity
     */
    public ExtractionMetadata modelToEntity(ExtractionMetadataModel model) {
        if (model == null) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.EXTRACTION_NOT_FOUND,
                    "Cannot convert null ExtractionMetadataModel to entity"
            );
        }

        return ExtractionMetadata.builder()
                .extractionKey(model.extractionKey())
                .invoiceKey(model.invoiceKey())
                .sourceFileName(model.sourceFileName())
                .extractionTimestamp(model.extractionTimestamp())
                .extractionStatus(model.extractionStatus())
                .confidenceScore(model.confidenceScore() != null ?
                        java.math.BigDecimal.valueOf(model.confidenceScore()) : null)
                .ocrEngine(model.ocrEngine())
                .extractionData(model.extractionData())
                .errorMessage(model.errorMessage())
                .createdAt(model.createdAt())
                .isDeleted(false)
                .build();
    }

    /**
     * Update existing entity with data from model.
     *
     * @param entity Existing entity to update
     * @param model  Model with new data
     */
    public void updateEntityFromModel(ExtractionMetadata entity, ExtractionMetadataModel model) {
        if (entity == null || model == null) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INTERNAL_ERROR,
                    "Cannot update entity: entity or model is null"
            );
        }

        entity.setInvoiceKey(model.invoiceKey());
        entity.setSourceFileName(model.sourceFileName());
        entity.setExtractionTimestamp(model.extractionTimestamp());
        entity.setExtractionStatus(model.extractionStatus());
        entity.setConfidenceScore(model.confidenceScore() != null ?
                java.math.BigDecimal.valueOf(model.confidenceScore()) : null);
        entity.setOcrEngine(model.ocrEngine());
        entity.setExtractionData(model.extractionData());
        entity.setErrorMessage(model.errorMessage());
    }
}
