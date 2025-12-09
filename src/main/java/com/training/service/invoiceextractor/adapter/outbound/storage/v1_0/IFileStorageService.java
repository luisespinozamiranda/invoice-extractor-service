package com.training.service.invoiceextractor.adapter.outbound.storage.v1_0;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Outbound port for file storage operations.
 * This interface defines the contract for storing and retrieving files.
 *
 * <p><b>Architecture:</b> Outbound Port (Hexagonal Architecture)
 * <p><b>Layer:</b> Adapter Layer
 * <p><b>Purpose:</b> Abstract file system operations from domain logic
 *
 * <p>Implementations should:
 * <ul>
 *   <li>Store files securely with unique identifiers</li>
 *   <li>Support retrieval of stored files</li>
 *   <li>Handle file cleanup and deletion</li>
 *   <li>Be non-blocking using {@link CompletableFuture}</li>
 * </ul>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-09
 */
public interface IFileStorageService {

    /**
     * Store a file and return its storage key.
     *
     * @param fileData Binary content of the file
     * @param fileName Original file name
     * @param fileType MIME type of the file
     * @return CompletableFuture with UUID key for the stored file
     * @throws com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException
     *         if storage fails
     */
    CompletableFuture<UUID> storeFile(byte[] fileData, String fileName, String fileType);

    /**
     * Retrieve a stored file by its key.
     *
     * @param fileKey UUID key of the stored file
     * @return CompletableFuture with StoredFile containing file data and metadata
     * @throws com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException
     *         if file not found or retrieval fails
     */
    CompletableFuture<StoredFile> retrieveFile(UUID fileKey);

    /**
     * Delete a stored file by its key.
     *
     * @param fileKey UUID key of the file to delete
     * @return CompletableFuture with true if deleted, false if not found
     */
    CompletableFuture<Boolean> deleteFile(UUID fileKey);

    /**
     * Check if a file exists in storage.
     *
     * @param fileKey UUID key of the file
     * @return CompletableFuture with true if file exists
     */
    CompletableFuture<Boolean> fileExists(UUID fileKey);
}
