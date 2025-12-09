package com.training.service.invoiceextractor.adapter.outbound.storage.v1_0.impl;

import com.training.service.invoiceextractor.adapter.outbound.storage.v1_0.IFileStorageService;
import com.training.service.invoiceextractor.adapter.outbound.storage.v1_0.StoredFile;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Local file system storage implementation.
 * Stores files in a configured directory on the local file system.
 *
 * <p><b>Architecture:</b> Outbound Adapter (Hexagonal Architecture)
 * <p><b>Storage:</b> Local file system
 *
 * <p><b>File Naming Convention:</b>
 * Files are stored as: {UUID}.{originalExtension}
 * Metadata is stored as: {UUID}.meta (JSON format with fileName and fileType)
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-09
 */
@Service
@Slf4j
public class LocalFileStorageService implements IFileStorageService {

    @Value("${storage.upload.directory:./uploads}")
    private String uploadDirectory;

    private Path storagePath;

    @PostConstruct
    public void init() {
        storagePath = Paths.get(uploadDirectory).toAbsolutePath().normalize();

        try {
            Files.createDirectories(storagePath);
            log.info("File storage initialized at: {}", storagePath);
        } catch (IOException ex) {
            log.error("Failed to create storage directory: {}", storagePath, ex);
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INTERNAL_ERROR,
                    "Failed to initialize file storage"
            );
        }
    }

    @Override
    public CompletableFuture<UUID> storeFile(byte[] fileData, String fileName, String fileType) {
        return CompletableFuture.supplyAsync(() -> {
            UUID fileKey = UUID.randomUUID();

            try {
                // Extract file extension from original filename
                String extension = getFileExtension(fileName);
                String storedFileName = fileKey.toString() + (extension.isEmpty() ? "" : "." + extension);
                Path filePath = storagePath.resolve(storedFileName);

                // Write file data
                Files.write(filePath, fileData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // Write metadata
                String metadata = String.format("{\"fileName\":\"%s\",\"fileType\":\"%s\",\"storedAt\":\"%s\"}",
                        fileName, fileType, LocalDateTime.now());
                Path metaPath = storagePath.resolve(fileKey.toString() + ".meta");
                Files.writeString(metaPath, metadata, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                log.debug("File stored: {} -> {}", fileName, storedFileName);
                return fileKey;

            } catch (IOException ex) {
                log.error("Failed to store file: {}", fileName, ex);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.INTERNAL_ERROR,
                        "Failed to store file: " + ex.getMessage()
                );
            }
        });
    }

    @Override
    public CompletableFuture<StoredFile> retrieveFile(UUID fileKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Read metadata first
                Path metaPath = storagePath.resolve(fileKey.toString() + ".meta");
                if (!Files.exists(metaPath)) {
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.FILE_NOT_FOUND,
                            "File metadata not found: " + fileKey
                    );
                }

                String metadata = Files.readString(metaPath);
                String fileName = extractJsonValue(metadata, "fileName");
                String fileType = extractJsonValue(metadata, "fileType");
                String storedAtStr = extractJsonValue(metadata, "storedAt");

                // Find file with any extension
                String extension = getFileExtension(fileName);
                String storedFileName = fileKey.toString() + (extension.isEmpty() ? "" : "." + extension);
                Path filePath = storagePath.resolve(storedFileName);

                if (!Files.exists(filePath)) {
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.FILE_NOT_FOUND,
                            "File not found: " + fileKey
                    );
                }

                byte[] fileData = Files.readAllBytes(filePath);

                log.debug("File retrieved: {} ({})", fileName, fileKey);

                return new StoredFile(
                        fileKey,
                        fileData,
                        fileName,
                        fileType,
                        fileData.length,
                        LocalDateTime.parse(storedAtStr)
                );

            } catch (IOException ex) {
                log.error("Failed to retrieve file: {}", fileKey, ex);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.INTERNAL_ERROR,
                        "Failed to retrieve file: " + ex.getMessage()
                );
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteFile(UUID fileKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path metaPath = storagePath.resolve(fileKey.toString() + ".meta");

                if (!Files.exists(metaPath)) {
                    log.warn("File not found for deletion: {}", fileKey);
                    return false;
                }

                // Read metadata to get extension
                String metadata = Files.readString(metaPath);
                String fileName = extractJsonValue(metadata, "fileName");
                String extension = getFileExtension(fileName);
                String storedFileName = fileKey.toString() + (extension.isEmpty() ? "" : "." + extension);

                Path filePath = storagePath.resolve(storedFileName);

                // Delete both file and metadata
                boolean fileDeleted = Files.deleteIfExists(filePath);
                boolean metaDeleted = Files.deleteIfExists(metaPath);

                log.debug("File deleted: {} (file: {}, meta: {})", fileKey, fileDeleted, metaDeleted);
                return fileDeleted || metaDeleted;

            } catch (IOException ex) {
                log.error("Failed to delete file: {}", fileKey, ex);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.INTERNAL_ERROR,
                        "Failed to delete file: " + ex.getMessage()
                );
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> fileExists(UUID fileKey) {
        return CompletableFuture.supplyAsync(() -> {
            Path metaPath = storagePath.resolve(fileKey.toString() + ".meta");
            return Files.exists(metaPath);
        });
    }

    /**
     * Extract file extension from filename.
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * Simple JSON value extraction (for minimal metadata parsing).
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            return "";
        }
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        return json.substring(startIndex, endIndex);
    }
}
