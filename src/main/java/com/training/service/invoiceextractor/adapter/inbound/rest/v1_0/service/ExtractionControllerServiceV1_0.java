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
    public CompletableFuture<ResponseEntity<ExtractionMetadataV1_0>> extractInvoice(MultipartFile file) {
        log.debug("Extracting invoice from file: {}", file.getOriginalFilename());

        try {
            byte[] fileData = file.getBytes();
            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType();

            return extractionService.extractAndSaveInvoice(fileData, fileName, fileType)
                    .thenApply(metadataModel -> {
                        ExtractionMetadataV1_0 metadataDto = extractionMetadataMapper.modelToDto(metadataModel);
                        return ResponseEntity.status(HttpStatus.CREATED).body(metadataDto);
                    });
            // Async exceptions handled by GlobalExceptionHandler
        } catch (Exception ex) {
            // Sync exceptions (file read errors) are wrapped and re-thrown
            log.error("Error reading file data: {}", file.getOriginalFilename(), ex);
            throw new RuntimeException("Failed to read file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public CompletableFuture<ResponseEntity<ExtractionMetadataV1_0>> getExtractionByInvoice(UUID invoiceKey) {
        log.debug("Getting extraction by invoice key: {}", invoiceKey);

        return extractionService.getExtractionsByInvoice(invoiceKey)
                .thenApply(metadataModels -> {
                    // Since relationship is 1:1, get the first (and only) extraction
                    if (metadataModels.isEmpty()) {
                        throw new RuntimeException("No extraction found for invoice: " + invoiceKey);
                    }
                    ExtractionMetadataV1_0 dto = extractionMetadataMapper.modelToDto(metadataModels.get(0));
                    return ResponseEntity.ok(dto);
                });
    }
}
