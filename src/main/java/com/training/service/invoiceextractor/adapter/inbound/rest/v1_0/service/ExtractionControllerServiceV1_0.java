package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.ExtractionMetadataV1_0;
import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.ExtractionResponseV1_0;
import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.InvoiceV1_0;
import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service.mappers.IExtractionMetadataMapperV1_0;
import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service.mappers.IInvoiceMapperV1_0;
import com.training.service.invoiceextractor.domain.service.IExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller Service implementation for Extraction operations (API v1.0)
 * Inbound adapter that translates between REST DTOs and domain models.
 * Exception handling is delegated to GlobalExceptionHandler.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractionControllerServiceV1_0 implements IExtractionControllerServiceV1_0 {

    private final IExtractionService extractionService;
    private final IInvoiceMapperV1_0 invoiceMapper;
    private final IExtractionMetadataMapperV1_0 extractionMetadataMapper;

    @Override
    public CompletableFuture<ResponseEntity<ExtractionResponseV1_0>> extractInvoice(MultipartFile file) {
        log.debug("Extracting invoice from file: {}", file.getOriginalFilename());

        try {
            byte[] fileData = file.getBytes();
            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType();

            return extractionService.extractAndSaveInvoice(fileData, fileName, fileType)
                    .thenApply(metadataModel -> {
                        ExtractionMetadataV1_0 metadataDto = extractionMetadataMapper.modelToDto(metadataModel);

                        ExtractionResponseV1_0 response = ExtractionResponseV1_0.builder()
                                .invoice(null) // Would need to fetch separately using metadataModel.invoiceKey()
                                .extractionMetadata(metadataDto)
                                .message("Invoice extracted successfully")
                                .build();

                        return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    });
            // Async exceptions handled by GlobalExceptionHandler
        } catch (Exception ex) {
            // Sync exceptions (file read errors) are wrapped and re-thrown
            log.error("Error reading file data: {}", file.getOriginalFilename(), ex);
            throw new RuntimeException("Failed to read file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public CompletableFuture<ResponseEntity<ExtractionMetadataV1_0>> getExtractionMetadata(UUID extractionKey) {
        log.debug("Getting extraction metadata by key: {}", extractionKey);

        return extractionService.getExtractionMetadata(extractionKey)
                .thenApply(metadataModel -> {
                    ExtractionMetadataV1_0 dto = extractionMetadataMapper.modelToDto(metadataModel);
                    return ResponseEntity.ok(dto);
                });
    }
}
