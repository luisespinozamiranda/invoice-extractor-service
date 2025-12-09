package com.training.service.invoiceextractor.adapter.outbound.storage.v1_0;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Record representing a stored file with its metadata.
 * Immutable object returned when retrieving files from storage.
 *
 * <p><b>Architecture:</b> Data Transfer Object (Adapter Layer)
 * <p><b>Immutability:</b> All fields are final (Java record)
 *
 * @param fileKey      Unique identifier for the stored file
 * @param fileData     Binary content of the file
 * @param fileName     Original file name
 * @param fileType     MIME type of the file
 * @param fileSize     Size of the file in bytes
 * @param storedAt     Timestamp when file was stored
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-09
 */
public record StoredFile(
        UUID fileKey,
        byte[] fileData,
        String fileName,
        String fileType,
        long fileSize,
        LocalDateTime storedAt
) {
    /**
     * Factory method for creating a StoredFile.
     *
     * @param fileKey  Unique identifier
     * @param fileData Binary content
     * @param fileName Original name
     * @param fileType MIME type
     * @return New StoredFile instance
     */
    public static StoredFile of(UUID fileKey, byte[] fileData, String fileName, String fileType) {
        return new StoredFile(
                fileKey,
                fileData,
                fileName,
                fileType,
                fileData != null ? fileData.length : 0L,
                LocalDateTime.now()
        );
    }
}
