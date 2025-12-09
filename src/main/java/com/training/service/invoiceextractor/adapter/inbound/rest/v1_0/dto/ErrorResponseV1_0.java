package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response DTO for REST API v1.0.
 * Returned by all endpoints when an error occurs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseV1_0 {

    /**
     * Unique error code (e.g., "INV-001")
     */
    private String errorCode;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;

    /**
     * Additional context details (optional)
     */
    private Map<String, Object> details;

    /**
     * Create error response with code and message only.
     *
     * @param errorCode Error code
     * @param message   Error message
     * @return ErrorResponseV1_0 instance
     */
    public static ErrorResponseV1_0 of(String errorCode, String message) {
        return ErrorResponseV1_0.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response with code, message, and details.
     *
     * @param errorCode Error code
     * @param message   Error message
     * @param details   Additional details
     * @return ErrorResponseV1_0 instance
     */
    public static ErrorResponseV1_0 of(String errorCode, String message, Map<String, Object> details) {
        return ErrorResponseV1_0.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();
    }
}
