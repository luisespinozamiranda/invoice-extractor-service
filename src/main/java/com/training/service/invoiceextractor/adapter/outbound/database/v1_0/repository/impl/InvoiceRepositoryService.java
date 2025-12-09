package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.impl;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.entity.Invoice;
import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IInvoiceRepositoryService;
import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.InvoiceRepository;
import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.impl.mappers.InvoiceMapper;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Repository service implementation for invoice data persistence.
 *
 * <p>This service acts as an outbound adapter in the hexagonal architecture,
 * translating domain operations into database operations through JPA repositories.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Translate domain models to JPA entities and vice versa</li>
 *   <li>Execute database operations asynchronously</li>
 *   <li>Map database exceptions to domain exceptions</li>
 *   <li>Maintain data integrity and consistency</li>
 * </ul>
 *
 * <p><b>Architecture:</b> Outbound Adapter (Hexagonal Architecture)
 * <p><b>Persistence:</b> PostgreSQL via Spring Data JPA
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see IInvoiceRepositoryService
 * @see InvoiceRepository
 * @see Invoice
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceRepositoryService implements IInvoiceRepositoryService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<InvoiceModel> findByInvoiceKey(UUID invoiceKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding invoice by key: {}", invoiceKey);
                Optional<Invoice> invoiceOptional = invoiceRepository.findByInvoiceKeyAndIsDeletedFalse(invoiceKey);

                if (invoiceOptional.isEmpty()) {
                    log.warn("Invoice not found with key: {}", invoiceKey);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.INVOICE_NOT_FOUND,
                            "Invoice not found with key: " + invoiceKey
                    );
                }

                InvoiceModel model = invoiceMapper.entityToModel(invoiceOptional);
                log.debug("Successfully found invoice: {}", invoiceKey);
                return model;

            } catch (InvoiceExtractorServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error finding invoice by key: {}", invoiceKey, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding invoice: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<List<InvoiceModel>> findAllActive() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding all active invoices");
                List<Invoice> invoices = invoiceRepository.findByIsDeletedFalse();

                List<InvoiceModel> models = invoices.stream()
                        .map(invoiceMapper::entityToModel)
                        .collect(Collectors.toList());

                log.debug("Found {} active invoices", models.size());
                return models;

            } catch (Exception e) {
                log.error("Error finding all active invoices", e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding active invoices: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<List<InvoiceModel>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding all invoices including deleted");
                List<Invoice> invoices = invoiceRepository.findAll();

                List<InvoiceModel> models = invoices.stream()
                        .map(invoiceMapper::entityToModel)
                        .collect(Collectors.toList());

                log.debug("Found {} total invoices", models.size());
                return models;

            } catch (Exception e) {
                log.error("Error finding all invoices", e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding all invoices: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional
    public CompletableFuture<InvoiceModel> save(InvoiceModel invoiceModel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Saving new invoice: {}", invoiceModel.invoiceNumber());
                Invoice entity = invoiceMapper.modelToEntity(invoiceModel);
                Invoice savedEntity = invoiceRepository.save(entity);

                InvoiceModel savedModel = invoiceMapper.entityToModel(savedEntity);
                log.info("Successfully saved invoice with key: {}", savedModel.invoiceKey());
                return savedModel;

            } catch (Exception e) {
                log.error("Error saving invoice: {}", invoiceModel.invoiceNumber(), e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error saving invoice: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional
    public CompletableFuture<InvoiceModel> update(UUID invoiceKey, InvoiceModel invoiceModel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Updating invoice: {}", invoiceKey);
                Optional<Invoice> existingEntityOptional = invoiceRepository.findByInvoiceKeyAndIsDeletedFalse(invoiceKey);

                if (existingEntityOptional.isEmpty()) {
                    log.warn("Invoice not found for update: {}", invoiceKey);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.INVOICE_NOT_FOUND,
                            "Invoice not found with key: " + invoiceKey
                    );
                }

                Invoice existingEntity = existingEntityOptional.get();
                invoiceMapper.updateEntityFromModel(existingEntity, invoiceModel);
                Invoice updatedEntity = invoiceRepository.save(existingEntity);

                InvoiceModel updatedModel = invoiceMapper.entityToModel(updatedEntity);
                log.info("Successfully updated invoice: {}", invoiceKey);
                return updatedModel;

            } catch (InvoiceExtractorServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error updating invoice: {}", invoiceKey, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error updating invoice: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional
    public CompletableFuture<Void> softDelete(UUID invoiceKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Soft deleting invoice: {}", invoiceKey);
                Optional<Invoice> invoiceOptional = invoiceRepository.findByInvoiceKeyAndIsDeletedFalse(invoiceKey);

                if (invoiceOptional.isEmpty()) {
                    log.warn("Invoice not found for deletion: {}", invoiceKey);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.INVOICE_NOT_FOUND,
                            "Invoice not found with key: " + invoiceKey
                    );
                }

                Invoice invoice = invoiceOptional.get();
                invoice.setIsDeleted(true);
                invoice.setUpdatedAt(LocalDateTime.now());
                invoiceRepository.save(invoice);

                log.info("Successfully soft deleted invoice: {}", invoiceKey);

            } catch (InvoiceExtractorServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error soft deleting invoice: {}", invoiceKey, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error deleting invoice: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional
    public CompletableFuture<InvoiceModel> restore(UUID invoiceKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Restoring invoice: {}", invoiceKey);
                Optional<Invoice> invoiceOptional = invoiceRepository.findByInvoiceKey(invoiceKey);

                if (invoiceOptional.isEmpty()) {
                    log.warn("Invoice not found for restoration: {}", invoiceKey);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.INVOICE_NOT_FOUND,
                            "Invoice not found with key: " + invoiceKey
                    );
                }

                Invoice invoice = invoiceOptional.get();
                invoice.setIsDeleted(false);
                invoice.setUpdatedAt(LocalDateTime.now());
                Invoice restoredEntity = invoiceRepository.save(invoice);

                InvoiceModel restoredModel = invoiceMapper.entityToModel(restoredEntity);
                log.info("Successfully restored invoice: {}", invoiceKey);
                return restoredModel;

            } catch (InvoiceExtractorServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error restoring invoice: {}", invoiceKey, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error restoring invoice: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Boolean> existsByInvoiceKey(UUID invoiceKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Checking if invoice exists: {}", invoiceKey);
                boolean exists = invoiceRepository.existsByInvoiceKeyAndIsDeletedFalse(invoiceKey);
                log.debug("Invoice {} exists: {}", invoiceKey, exists);
                return exists;

            } catch (Exception e) {
                log.error("Error checking invoice existence: {}", invoiceKey, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error checking invoice existence: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<List<InvoiceModel>> findByInvoiceNumber(String invoiceNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding invoices by invoice number: {}", invoiceNumber);
                List<Invoice> invoices = invoiceRepository.findByInvoiceNumberAndIsDeletedFalse(invoiceNumber);

                List<InvoiceModel> models = invoices.stream()
                        .map(invoiceMapper::entityToModel)
                        .collect(Collectors.toList());

                log.debug("Found {} invoices with invoice number: {}", models.size(), invoiceNumber);
                return models;

            } catch (Exception e) {
                log.error("Error finding invoices by invoice number: {}", invoiceNumber, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding invoices by invoice number: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<List<InvoiceModel>> findByClientName(String clientName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding invoices by client name: {}", clientName);
                List<Invoice> invoices = invoiceRepository.findByClientNameContainingIgnoreCaseAndIsDeletedFalse(clientName);

                List<InvoiceModel> models = invoices.stream()
                        .map(invoiceMapper::entityToModel)
                        .collect(Collectors.toList());

                log.debug("Found {} invoices with client name containing: {}", models.size(), clientName);
                return models;

            } catch (Exception e) {
                log.error("Error finding invoices by client name: {}", clientName, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding invoices by client name: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<List<InvoiceModel>> findByStatus(String status) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding invoices by status: {}", status);
                List<Invoice> invoices = invoiceRepository.findByStatusAndIsDeletedFalse(status);

                List<InvoiceModel> models = invoices.stream()
                        .map(invoiceMapper::entityToModel)
                        .collect(Collectors.toList());

                log.debug("Found {} invoices with status: {}", models.size(), status);
                return models;

            } catch (Exception e) {
                log.error("Error finding invoices by status: {}", status, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding invoices by status: " + e.getMessage()
                );
            }
        });
    }
}
