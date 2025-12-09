package com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.impl;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.entity.ExtractionMetadata;
import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.ExtractionMetadataRepository;
import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IExtractionMetadataRepositoryService;
import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.impl.mappers.ExtractionMetadataMapper;
import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Repository service implementation for extraction metadata data persistence.
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
 * @see IExtractionMetadataRepositoryService
 * @see ExtractionMetadataRepository
 * @see ExtractionMetadata
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractionMetadataRepositoryService implements IExtractionMetadataRepositoryService {

    private final ExtractionMetadataRepository extractionMetadataRepository;
    private final ExtractionMetadataMapper extractionMetadataMapper;

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<ExtractionMetadataModel> findByExtractionKey(UUID extractionKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding extraction metadata by key: {}", extractionKey);
                Optional<ExtractionMetadata> metadataOptional =
                        extractionMetadataRepository.findByExtractionKeyAndIsDeletedFalse(extractionKey);

                if (metadataOptional.isEmpty()) {
                    log.warn("Extraction metadata not found with key: {}", extractionKey);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.EXTRACTION_NOT_FOUND,
                            "Extraction metadata not found with key: " + extractionKey
                    );
                }

                ExtractionMetadataModel model = extractionMetadataMapper.entityToModel(metadataOptional);
                log.debug("Successfully found extraction metadata: {}", extractionKey);
                return model;

            } catch (InvoiceExtractorServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error finding extraction metadata by key: {}", extractionKey, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding extraction metadata: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<List<ExtractionMetadataModel>> findByInvoiceKey(UUID invoiceKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding extraction metadata by invoice key: {}", invoiceKey);
                List<ExtractionMetadata> metadataList =
                        extractionMetadataRepository.findByInvoiceKeyAndIsDeletedFalse(invoiceKey);

                List<ExtractionMetadataModel> models = metadataList.stream()
                        .map(extractionMetadataMapper::entityToModel)
                        .collect(Collectors.toList());

                log.debug("Found {} extraction metadata for invoice: {}", models.size(), invoiceKey);
                return models;

            } catch (Exception e) {
                log.error("Error finding extraction metadata by invoice key: {}", invoiceKey, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding extraction metadata by invoice key: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<List<ExtractionMetadataModel>> findByStatus(String status) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding extraction metadata by status: {}", status);
                List<ExtractionMetadata> metadataList =
                        extractionMetadataRepository.findByExtractionStatusAndIsDeletedFalse(status);

                List<ExtractionMetadataModel> models = metadataList.stream()
                        .map(extractionMetadataMapper::entityToModel)
                        .collect(Collectors.toList());

                log.debug("Found {} extraction metadata with status: {}", models.size(), status);
                return models;

            } catch (Exception e) {
                log.error("Error finding extraction metadata by status: {}", status, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding extraction metadata by status: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<List<ExtractionMetadataModel>> findAllActive() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding all active extraction metadata");
                List<ExtractionMetadata> metadataList =
                        extractionMetadataRepository.findByIsDeletedFalse();

                List<ExtractionMetadataModel> models = metadataList.stream()
                        .map(extractionMetadataMapper::entityToModel)
                        .collect(Collectors.toList());

                log.debug("Found {} active extraction metadata", models.size());
                return models;

            } catch (Exception e) {
                log.error("Error finding all active extraction metadata", e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding active extraction metadata: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional
    public CompletableFuture<ExtractionMetadataModel> save(ExtractionMetadataModel extractionMetadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Saving new extraction metadata for file: {}", extractionMetadata.sourceFileName());
                ExtractionMetadata entity = extractionMetadataMapper.modelToEntity(extractionMetadata);
                ExtractionMetadata savedEntity = extractionMetadataRepository.save(entity);

                ExtractionMetadataModel savedModel = extractionMetadataMapper.entityToModel(savedEntity);
                log.info("Successfully saved extraction metadata with key: {}", savedModel.extractionKey());
                return savedModel;

            } catch (Exception e) {
                log.error("Error saving extraction metadata for file: {}", extractionMetadata.sourceFileName(), e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error saving extraction metadata: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional
    public CompletableFuture<ExtractionMetadataModel> update(UUID extractionKey, ExtractionMetadataModel extractionMetadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Updating extraction metadata: {}", extractionKey);
                Optional<ExtractionMetadata> existingEntityOptional =
                        extractionMetadataRepository.findByExtractionKeyAndIsDeletedFalse(extractionKey);

                if (existingEntityOptional.isEmpty()) {
                    log.warn("Extraction metadata not found for update: {}", extractionKey);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.EXTRACTION_NOT_FOUND,
                            "Extraction metadata not found with key: " + extractionKey
                    );
                }

                ExtractionMetadata existingEntity = existingEntityOptional.get();
                extractionMetadataMapper.updateEntityFromModel(existingEntity, extractionMetadata);
                ExtractionMetadata updatedEntity = extractionMetadataRepository.save(existingEntity);

                ExtractionMetadataModel updatedModel = extractionMetadataMapper.entityToModel(updatedEntity);
                log.info("Successfully updated extraction metadata: {}", extractionKey);
                return updatedModel;

            } catch (InvoiceExtractorServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error updating extraction metadata: {}", extractionKey, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error updating extraction metadata: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional
    public CompletableFuture<Void> softDelete(UUID extractionKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Soft deleting extraction metadata: {}", extractionKey);
                Optional<ExtractionMetadata> metadataOptional =
                        extractionMetadataRepository.findByExtractionKeyAndIsDeletedFalse(extractionKey);

                if (metadataOptional.isEmpty()) {
                    log.warn("Extraction metadata not found for deletion: {}", extractionKey);
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.EXTRACTION_NOT_FOUND,
                            "Extraction metadata not found with key: " + extractionKey
                    );
                }

                ExtractionMetadata metadata = metadataOptional.get();
                metadata.setIsDeleted(true);
                extractionMetadataRepository.save(metadata);

                log.info("Successfully soft deleted extraction metadata: {}", extractionKey);

            } catch (InvoiceExtractorServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error soft deleting extraction metadata: {}", extractionKey, e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error deleting extraction metadata: " + e.getMessage()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<List<ExtractionMetadataModel>> findLowConfidenceExtractions(double confidenceThreshold) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding extraction metadata with confidence below: {}", confidenceThreshold);
                List<ExtractionMetadata> metadataList =
                        extractionMetadataRepository.findByConfidenceScoreLessThanAndIsDeletedFalse(confidenceThreshold);

                List<ExtractionMetadataModel> models = metadataList.stream()
                        .map(extractionMetadataMapper::entityToModel)
                        .collect(Collectors.toList());

                log.debug("Found {} low-confidence extractions below threshold: {}", models.size(), confidenceThreshold);
                return models;

            } catch (Exception e) {
                log.error("Error finding low-confidence extractions", e);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.DATABASE_ERROR,
                        "Error finding low-confidence extractions: " + e.getMessage()
                );
            }
        });
    }
}
