package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.controller;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.ExtractionMetadataV1_0;
import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.ExtractionResponseV1_0;
import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service.IExtractionControllerServiceV1_0;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for invoice extraction operations (API v1.0).
 *
 * <p>This controller exposes RESTful endpoints for invoice extraction management,
 * following REST best practices and HTTP semantics.
 *
 * <p><b>Base Path:</b> {@code /api/v1.0/extractions}
 *
 * <p><b>Endpoints:</b>
 * <ul>
 *   <li>POST / - Upload and extract invoice from file</li>
 *   <li>GET /{extraction_key} - Get extraction metadata by key</li>
 * </ul>
 *
 * <p><b>Architecture:</b> REST Layer (Inbound Adapter)
 * <p><b>API Version:</b> 1.0
 * <p><b>Async:</b> All operations are non-blocking using {@link CompletableFuture}
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see IExtractionControllerServiceV1_0
 * @see ExtractionResponseV1_0
 * @see ExtractionMetadataV1_0
 */
@RestController
@RequestMapping("/api/v1.0/extractions")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Extraction API", description = "Endpoints for extracting invoice data from files using OCR")
public class ExtractionControllerV1_0 {

    private final IExtractionControllerServiceV1_0 extractionControllerService;

    @Async
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Extract invoice from file", description = "Upload a PDF or image file to extract invoice data using Tesseract OCR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invoice extracted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or file format"),
            @ApiResponse(responseCode = "500", description = "Extraction failed")
    })
    public CompletableFuture<ResponseEntity<ExtractionResponseV1_0>> extractInvoice(
            @Parameter(description = "Invoice file (PDF or image)") @RequestParam("file") MultipartFile file) {
        log.info("REST Request: POST /api/v1.0/extractions - File: {}", file.getOriginalFilename());
        return extractionControllerService.extractInvoice(file);
    }

    @Async
    @GetMapping("/{extraction_key}")
    @Operation(summary = "Get extraction metadata", description = "Retrieve extraction metadata by extraction key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Extraction metadata found"),
            @ApiResponse(responseCode = "404", description = "Extraction metadata not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<ExtractionMetadataV1_0>> getExtractionMetadata(
            @Parameter(description = "Extraction unique key") @PathVariable("extraction_key") UUID extractionKey) {
        log.info("REST Request: GET /api/v1.0/extractions/{}", extractionKey);
        return extractionControllerService.getExtractionMetadata(extractionKey);
    }
}
