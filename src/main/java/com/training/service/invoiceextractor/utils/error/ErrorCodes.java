package com.training.service.invoiceextractor.utils.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of error codes for the Invoice Extractor Service.
 * Each error code includes a unique identifier, descriptive message, and corresponding HTTP status.
 *
 * Benefits of this approach:
 * - Single source of truth: HTTP status is defined alongside the error code
 * - Follows Open/Closed Principle: adding new errors doesn't require modifying other classes
 * - Type-safe: compiler ensures HTTP status is always provided
 * - Self-documenting: clear relationship between business error and HTTP response
 */
@Getter
public enum ErrorCodes {
    // File validation errors (400 Bad Request)
    INVALID_FILE_TYPE("INV-001", "Invalid file type provided. Accepted types: PDF, PNG, JPG, JPEG", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("INV-002", "File size exceeds maximum limit of 10 MB", HttpStatus.BAD_REQUEST),
    FILE_NOT_READABLE("INV-003", "The uploaded file could not be read or is corrupted", HttpStatus.BAD_REQUEST),

    // OCR errors
    OCR_SERVICE_UNAVAILABLE("INV-004", "OCR service is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    EXTRACTION_FAILED("INV-005", "Failed to extract invoice data from file", HttpStatus.INTERNAL_SERVER_ERROR),
    OCR_TIMEOUT("INV-006", "OCR processing timed out after 30 seconds", HttpStatus.REQUEST_TIMEOUT),

    // Data errors (404 Not Found)
    INVOICE_NOT_FOUND("INV-007", "Invoice not found with the provided key", HttpStatus.NOT_FOUND),
    VENDOR_NOT_FOUND("INV-008", "Vendor not found with the provided key", HttpStatus.NOT_FOUND),
    EXTRACTION_NOT_FOUND("INV-009", "Extraction metadata not found", HttpStatus.NOT_FOUND),
    EXTRACTION_METADATA_NOT_FOUND("INV-009", "Extraction metadata not found", HttpStatus.NOT_FOUND),

    // Database errors (500 Internal Server Error)
    DATABASE_ERROR("INV-010", "Database operation failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // Conflict errors (409 Conflict)
    DUPLICATE_INVOICE("INV-011", "Invoice with this number already exists", HttpStatus.CONFLICT),

    // Request validation errors (400 Bad Request)
    INVALID_REQUEST("INV-012", "Invalid request payload", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("INV-013", "Required field is missing: {field}", HttpStatus.BAD_REQUEST),
    INVALID_UUID_FORMAT("INV-014", "Invalid UUID format provided", HttpStatus.BAD_REQUEST),

    // General errors (500 Internal Server Error)
    INTERNAL_ERROR("INV-999", "An unexpected internal error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCodes(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * Returns a formatted message with placeholders replaced by actual values.
     *
     * @param args Arguments to replace placeholders in the message
     * @return Formatted error message
     */
    public String getFormattedMessage(Object... args) {
        return String.format(message, args);
    }
}
