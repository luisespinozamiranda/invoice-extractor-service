package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service.mappers;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.ExtractionMetadataV1_0;
import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between ExtractionMetadataV1_0 DTOs and ExtractionMetadataModel domain objects
 */
@Component
public class IExtractionMetadataMapperV1_0 {

    /**
     * Converts ExtractionMetadataV1_0 DTO to ExtractionMetadataModel domain object
     *
     * @param dto The DTO to convert
     * @return The domain model
     */
    public ExtractionMetadataModel dtoToModel(ExtractionMetadataV1_0 dto) {
        if (dto == null) {
            return null;
        }

        return new ExtractionMetadataModel(
                dto.getExtractionKey(),
                dto.getInvoiceKey(),
                dto.getSourceFileName(),
                dto.getExtractionTimestamp(),
                dto.getExtractionStatus(),
                dto.getConfidenceScore(),
                dto.getOcrEngine(),
                dto.getExtractionData(),
                dto.getErrorMessage(),
                dto.getCreatedAt()
        );
    }

    /**
     * Converts ExtractionMetadataModel domain object to ExtractionMetadataV1_0 DTO
     *
     * @param model The domain model to convert
     * @return The DTO
     */
    public ExtractionMetadataV1_0 modelToDto(ExtractionMetadataModel model) {
        if (model == null) {
            return null;
        }

        return ExtractionMetadataV1_0.builder()
                .extractionKey(model.extractionKey())
                .invoiceKey(model.invoiceKey())
                .sourceFileName(model.sourceFileName())
                .extractionTimestamp(model.extractionTimestamp())
                .extractionStatus(model.extractionStatus())
                .confidenceScore(model.confidenceScore())
                .ocrEngine(model.ocrEngine())
                .extractionData(model.extractionData())
                .errorMessage(model.errorMessage())
                .createdAt(model.createdAt())
                .updatedAt(null) // Domain model doesn't have updatedAt
                .isDeleted(false) // Domain model doesn't have isDeleted
                .build();
    }
}
