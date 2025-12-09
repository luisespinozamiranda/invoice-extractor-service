package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.impl.mappers;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.entity.Invoice;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mapper for converting between Invoice JPA entity and InvoiceModel domain model.
 * Handles bidirectional transformation with null safety using Optional.
 */
@Component
public class InvoiceMapper {

    /**
     * Convert Invoice entity to InvoiceModel domain model.
     *
     * @param entityOptional Optional containing Invoice entity
     * @return InvoiceModel if entity is present, null otherwise
     */
    public InvoiceModel entityToModel(Optional<Invoice> entityOptional) {
        if (entityOptional.isEmpty()) {
            return null;
        }

        Invoice entity = entityOptional.get();

        return new InvoiceModel(
                entity.getInvoiceKey(),
                entity.getInvoiceNumber(),
                entity.getInvoiceAmount(),
                entity.getClientName(),
                entity.getClientAddress(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getOriginalFileName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Convert Invoice entity to InvoiceModel domain model (non-optional version).
     *
     * @param entity Invoice entity
     * @return InvoiceModel
     */
    public InvoiceModel entityToModel(Invoice entity) {
        if (entity == null) {
            return null;
        }

        return new InvoiceModel(
                entity.getInvoiceKey(),
                entity.getInvoiceNumber(),
                entity.getInvoiceAmount(),
                entity.getClientName(),
                entity.getClientAddress(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getOriginalFileName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Convert InvoiceModel domain model to Invoice entity.
     *
     * @param modelOptional Optional containing InvoiceModel
     * @return Invoice entity if model is present
     * @throws InvoiceExtractorServiceException if model is not present
     */
    public Invoice modelToEntity(Optional<InvoiceModel> modelOptional) {
        if (modelOptional.isEmpty()) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INVOICE_NOT_FOUND,
                    "Cannot convert null InvoiceModel to entity"
            );
        }

        InvoiceModel model = modelOptional.get();

        return Invoice.builder()
                .invoiceKey(model.invoiceKey())
                .invoiceNumber(model.invoiceNumber())
                .invoiceAmount(model.invoiceAmount())
                .clientName(model.clientName())
                .clientAddress(model.clientAddress())
                .currency(model.currency())
                .status(model.status())
                .originalFileName(model.originalFileName())
                .createdAt(model.createdAt())
                .updatedAt(model.updatedAt())
                .isDeleted(false) // Always false for new entities
                .build();
    }

    /**
     * Convert InvoiceModel domain model to Invoice entity (non-optional version).
     *
     * @param model InvoiceModel
     * @return Invoice entity
     */
    public Invoice modelToEntity(InvoiceModel model) {
        if (model == null) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INVOICE_NOT_FOUND,
                    "Cannot convert null InvoiceModel to entity"
            );
        }

        return Invoice.builder()
                .invoiceKey(model.invoiceKey())
                .invoiceNumber(model.invoiceNumber())
                .invoiceAmount(model.invoiceAmount())
                .clientName(model.clientName())
                .clientAddress(model.clientAddress())
                .currency(model.currency())
                .status(model.status())
                .originalFileName(model.originalFileName())
                .createdAt(model.createdAt())
                .updatedAt(model.updatedAt())
                .isDeleted(false)
                .build();
    }

    /**
     * Update existing entity with data from model.
     *
     * @param entity Existing entity to update
     * @param model  Model with new data
     */
    public void updateEntityFromModel(Invoice entity, InvoiceModel model) {
        if (entity == null || model == null) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INTERNAL_ERROR,
                    "Cannot update entity: entity or model is null"
            );
        }

        entity.setInvoiceNumber(model.invoiceNumber());
        entity.setInvoiceAmount(model.invoiceAmount());
        entity.setClientName(model.clientName());
        entity.setClientAddress(model.clientAddress());
        entity.setCurrency(model.currency());
        entity.setStatus(model.status());
        entity.setOriginalFileName(model.originalFileName());
        // updatedAt will be set automatically by @PreUpdate
    }
}
