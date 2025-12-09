package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.InvoiceV1_0;
import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.service.mappers.IInvoiceMapperV1_0;
import com.training.service.invoiceextractor.domain.service.IInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Controller service implementation for invoice REST operations (API v1.0).
 *
 * <p>This service acts as an inbound adapter in the hexagonal architecture,
 * translating REST DTOs to domain models and vice versa.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Transform REST DTOs to domain models</li>
 *   <li>Transform domain models to REST DTOs</li>
 *   <li>Delegate business logic to domain services</li>
 *   <li>Construct appropriate HTTP responses</li>
 * </ul>
 *
 * <p><b>Architecture:</b> Inbound Adapter (Hexagonal Architecture)
 * <p><b>Exception Handling:</b> Delegated to {@link GlobalExceptionHandler}
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see IInvoiceControllerServiceV1_0
 * @see InvoiceV1_0
 * @see IInvoiceService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceControllerServiceV1_0 implements IInvoiceControllerServiceV1_0 {

    private final IInvoiceService invoiceService;
    private final IInvoiceMapperV1_0 invoiceMapper;

    @Override
    public CompletableFuture<ResponseEntity<InvoiceV1_0>> getInvoiceByKey(UUID invoiceKey) {
        log.debug("Getting invoice by key: {}", invoiceKey);

        return invoiceService.getInvoiceByKey(invoiceKey)
                .thenApply(invoiceModel -> {
                    InvoiceV1_0 dto = invoiceMapper.modelToDto(invoiceModel);
                    return ResponseEntity.ok(dto);
                });
        // Exception handling is done by GlobalExceptionHandler
    }

    @Override
    public CompletableFuture<ResponseEntity<List<InvoiceV1_0>>> getAllInvoices() {
        log.debug("Getting all invoices");

        return invoiceService.getAllInvoices()
                .thenApply(invoiceModels -> {
                    List<InvoiceV1_0> dtos = invoiceModels.stream()
                            .map(invoiceMapper::modelToDto)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(dtos);
                });
    }

    @Override
    public CompletableFuture<ResponseEntity<InvoiceV1_0>> createInvoice(InvoiceV1_0 invoiceDto) {
        log.debug("Creating invoice: {}", invoiceDto.getInvoiceNumber());

        return invoiceService.createInvoice(invoiceMapper.dtoToModel(invoiceDto))
                .thenApply(invoiceModel -> {
                    InvoiceV1_0 dto = invoiceMapper.modelToDto(invoiceModel);
                    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
                });
    }

    @Override
    public CompletableFuture<ResponseEntity<InvoiceV1_0>> updateInvoice(UUID invoiceKey, InvoiceV1_0 invoiceDto) {
        log.debug("Updating invoice with key: {}", invoiceKey);

        return invoiceService.updateInvoice(invoiceKey, invoiceMapper.dtoToModel(invoiceDto))
                .thenApply(invoiceModel -> {
                    InvoiceV1_0 dto = invoiceMapper.modelToDto(invoiceModel);
                    return ResponseEntity.ok(dto);
                });
    }

    @Override
    public CompletableFuture<ResponseEntity<Void>> deleteInvoice(UUID invoiceKey) {
        log.debug("Deleting invoice with key: {}", invoiceKey);

        return invoiceService.deleteInvoice(invoiceKey)
                .thenApply(v -> ResponseEntity.noContent().<Void>build());
    }

    @Override
    public CompletableFuture<ResponseEntity<List<InvoiceV1_0>>> searchByClientName(String clientName) {
        log.debug("Searching invoices by client name: {}", clientName);

        return invoiceService.findByClientName(clientName)
                .thenApply(invoiceModels -> {
                    List<InvoiceV1_0> dtos = invoiceModels.stream()
                            .map(invoiceMapper::modelToDto)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(dtos);
                });
    }
}
