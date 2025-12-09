package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response DTO for REST API (v1.0)
 * Used by GlobalExceptionHandler to return consistent error messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Application-specific error code (e.g., "INV-007")
     */
    @JsonProperty("error_code")
    private String errorCode;

    /**
     * Human-readable error message
     */
    @JsonProperty("message")
    private String message;

    /**
     * Timestamp when the error occurred
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * API path where the error occurred (e.g., "/api/v1.0/invoices/123")
     */
    @JsonProperty("path")
    private String path;

    /**
     * HTTP status code
     */
    @JsonProperty("status")
    private Integer status;

    /**
     * Additional error details (optional)
     */
    @JsonProperty("details")
    private Map<String, Object> details;
}
