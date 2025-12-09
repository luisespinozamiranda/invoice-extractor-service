package com.training.service.invoiceextractor.utils.error;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom exception class for Invoice Extractor Service.
 * Wraps ErrorCodes and provides additional context through details map.
 */
@Getter
public class InvoiceExtractorServiceException extends RuntimeException {

    private final ErrorCodes errorCode;
    private final Map<String, Object> details;

    /**
     * Constructor with error code only.
     *
     * @param errorCode The error code enum
     */
    public InvoiceExtractorServiceException(ErrorCodes errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    /**
     * Constructor with error code and custom message.
     *
     * @param errorCode     The error code enum
     * @param customMessage Custom error message (overrides default)
     */
    public InvoiceExtractorServiceException(ErrorCodes errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    /**
     * Constructor with error code, custom message, and cause.
     *
     * @param errorCode     The error code enum
     * @param customMessage Custom error message
     * @param cause         The root cause exception
     */
    public InvoiceExtractorServiceException(ErrorCodes errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    /**
     * Constructor with error code and details map.
     *
     * @param errorCode The error code enum
     * @param details   Additional context details
     */
    public InvoiceExtractorServiceException(ErrorCodes errorCode, Map<String, Object> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details != null ? details : new HashMap<>();
    }

    /**
     * Add a detail entry to the exception context.
     *
     * @param key   Detail key
     * @param value Detail value
     * @return This exception instance (for method chaining)
     */
    public InvoiceExtractorServiceException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    /**
     * Check if the exception has any additional details.
     *
     * @return true if details map is not empty
     */
    public boolean hasDetails() {
        return !details.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("InvoiceExtractorServiceException{");
        sb.append("errorCode=").append(errorCode.getCode());
        sb.append(", message='").append(getMessage()).append('\'');
        if (hasDetails()) {
            sb.append(", details=").append(details);
        }
        sb.append('}');
        return sb.toString();
    }
}
