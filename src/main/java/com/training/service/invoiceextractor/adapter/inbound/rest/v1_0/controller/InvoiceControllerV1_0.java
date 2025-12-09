package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.controller;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.InvoiceV1_0;
import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service.IInvoiceControllerServiceV1_0;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for invoice operations (API v1.0).
 *
 * <p>This controller exposes RESTful endpoints for invoice management,
 * following REST best practices and HTTP semantics.
 *
 * <p><b>Base Path:</b> {@code /api/v1.0/invoices}
 *
 * <p><b>Endpoints:</b>
 * <ul>
 *   <li>GET /{invoice_key} - Retrieve invoice by key</li>
 *   <li>GET / - List all invoices</li>
 *   <li>POST / - Create new invoice</li>
 *   <li>PUT /{invoice_key} - Update existing invoice</li>
 *   <li>DELETE /{invoice_key} - Delete invoice</li>
 *   <li>GET /search - Search invoices by client name</li>
 * </ul>
 *
 * <p><b>Architecture:</b> REST Layer (Inbound Adapter)
 * <p><b>API Version:</b> 1.0
 * <p><b>Async:</b> All operations are non-blocking using {@link CompletableFuture}
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see IInvoiceControllerServiceV1_0
 * @see InvoiceV1_0
 */
@RestController
@RequestMapping("/api/v1.0/invoices")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Invoice API", description = "Endpoints for managing invoices")
public class InvoiceControllerV1_0 {

    private final IInvoiceControllerServiceV1_0 invoiceControllerService;

    @Async
    @GetMapping("/{invoice_key}")
    @Operation(summary = "Get invoice by key", description = "Retrieve an invoice by its unique key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice found"),
            @ApiResponse(responseCode = "404", description = "Invoice not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<InvoiceV1_0>> getInvoiceByKey(
            @Parameter(description = "Invoice unique key") @PathVariable("invoice_key") UUID invoiceKey) {
        log.info("REST Request: GET /api/v1.0/invoices/{}", invoiceKey);
        return invoiceControllerService.getInvoiceByKey(invoiceKey);
    }

    @Async
    @GetMapping
    @Operation(summary = "Get all invoices", description = "Retrieve all invoices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<InvoiceV1_0>>> getAllInvoices() {
        log.info("REST Request: GET /api/v1.0/invoices");
        return invoiceControllerService.getAllInvoices();
    }

    @Async
    @PostMapping
    @Operation(summary = "Create invoice", description = "Create a new invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invoice created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<InvoiceV1_0>> createInvoice(
            @Parameter(description = "Invoice data") @RequestBody InvoiceV1_0 invoiceDto) {
        log.info("REST Request: POST /api/v1.0/invoices - Invoice Number: {}", invoiceDto.getInvoiceNumber());
        return invoiceControllerService.createInvoice(invoiceDto);
    }

    @Async
    @PutMapping("/{invoice_key}")
    @Operation(summary = "Update invoice", description = "Update an existing invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice updated successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<InvoiceV1_0>> updateInvoice(
            @Parameter(description = "Invoice unique key") @PathVariable("invoice_key") UUID invoiceKey,
            @Parameter(description = "Updated invoice data") @RequestBody InvoiceV1_0 invoiceDto) {
        log.info("REST Request: PUT /api/v1.0/invoices/{}", invoiceKey);
        return invoiceControllerService.updateInvoice(invoiceKey, invoiceDto);
    }

    @Async
    @DeleteMapping("/{invoice_key}")
    @Operation(summary = "Delete invoice", description = "Soft delete an invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Invoice deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<Void>> deleteInvoice(
            @Parameter(description = "Invoice unique key") @PathVariable("invoice_key") UUID invoiceKey) {
        log.info("REST Request: DELETE /api/v1.0/invoices/{}", invoiceKey);
        return invoiceControllerService.deleteInvoice(invoiceKey);
    }

    @Async
    @GetMapping("/search")
    @Operation(summary = "Search invoices by client name", description = "Search for invoices by client name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<InvoiceV1_0>>> searchByClientName(
            @Parameter(description = "Client name to search for") @RequestParam("clientName") String clientName) {
        log.info("REST Request: GET /api/v1.0/invoices/search?clientName={}", clientName);
        return invoiceControllerService.searchByClientName(clientName);
    }
}
