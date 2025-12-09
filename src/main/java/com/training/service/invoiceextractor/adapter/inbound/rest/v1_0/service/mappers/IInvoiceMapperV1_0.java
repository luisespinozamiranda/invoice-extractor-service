package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service.mappers;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.InvoiceV1_0;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between InvoiceV1_0 DTOs and InvoiceModel domain objects
 */
@Component
public class IInvoiceMapperV1_0 {

    /**
     * Converts InvoiceV1_0 DTO to InvoiceModel domain object
     *
     * @param dto The DTO to convert
     * @return The domain model
     */
    public InvoiceModel dtoToModel(InvoiceV1_0 dto) {
        if (dto == null) {
            return null;
        }

        return new InvoiceModel(
                dto.getInvoiceKey(),
                dto.getInvoiceNumber(),
                dto.getInvoiceAmount(),
                dto.getClientName(),
                dto.getClientAddress(),
                dto.getCurrency() != null ? dto.getCurrency() : "USD",
                dto.getStatus() != null ? dto.getStatus() : InvoiceModel.STATUS_PENDING,
                null, // originalFileName - not part of DTO
                dto.getCreatedAt(),
                dto.getUpdatedAt()
        );
    }

    /**
     * Converts InvoiceModel domain object to InvoiceV1_0 DTO
     *
     * @param model The domain model to convert
     * @return The DTO
     */
    public InvoiceV1_0 modelToDto(InvoiceModel model) {
        if (model == null) {
            return null;
        }

        return InvoiceV1_0.builder()
                .invoiceKey(model.invoiceKey())
                .invoiceNumber(model.invoiceNumber())
                .invoiceAmount(model.invoiceAmount())
                .clientName(model.clientName())
                .clientAddress(model.clientAddress())
                .currency(model.currency())
                .status(model.status())
                .notes(null) // Domain model doesn't have notes
                .issueDate(null) // Domain model doesn't have issueDate
                .dueDate(null) // Domain model doesn't have dueDate
                .createdAt(model.createdAt())
                .updatedAt(model.updatedAt())
                .isDeleted(false) // Domain model doesn't have isDeleted
                .build();
    }
}
