package com.training.service.invoiceextractor.configuration;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.ErrorResponseV1_0;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Invoice Extractor Service.
 * Catches all exceptions thrown by controllers and converts them to standardized error responses.
 *
 * Best Practices Implemented:
 * - Handles both sync and async (CompletableFuture) exceptions
 * - Provides detailed error responses with consistent format
 * - Logs all errors with appropriate context
 * - Maps business exceptions to proper HTTP status codes
 * - Handles Spring validation errors with field-level details
 * - Includes request path in error responses for better debugging
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle custom InvoiceExtractorServiceException.
     * Maps exception to appropriate HTTP status code and error response.
     *
     * @param ex The custom exception
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(InvoiceExtractorServiceException.class)
    public ResponseEntity<ErrorResponseV1_0> handleInvoiceExtractorException(
            InvoiceExtractorServiceException ex,
            WebRequest request
    ) {
        String path = getRequestPath(request);
        log.error("InvoiceExtractorServiceException at {}: {} - {}",
                path,
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                ex);

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                ex.getDetails()
        );

        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(errorResponse);
    }

    /**
     * Handle CompletionException (thrown by CompletableFuture operations).
     * Unwraps the cause and delegates to appropriate handler.
     *
     * @param ex The CompletionException
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ErrorResponseV1_0> handleCompletionException(
            CompletionException ex,
            WebRequest request
    ) {
        Throwable cause = ex.getCause();

        // If the cause is our custom exception, handle it
        if (cause instanceof InvoiceExtractorServiceException) {
            return handleInvoiceExtractorException((InvoiceExtractorServiceException) cause, request);
        }

        // Otherwise, handle as generic exception
        log.error("CompletionException with unexpected cause at {}", getRequestPath(request), ex);
        Exception exceptionToHandle = (cause instanceof Exception)
            ? (Exception) cause
            : new RuntimeException("Unexpected error", cause);
        return handleGenericException(exceptionToHandle, request);
    }

    /**
     * Handle Bean Validation errors (@Valid, @Validated).
     * Returns detailed field-level validation errors.
     *
     * @param ex The MethodArgumentNotValidException
     * @param request The web request
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseV1_0> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        log.warn("Validation failed at {}: {} errors",
                getRequestPath(request),
                ex.getBindingResult().getErrorCount());

        Map<String, Object> validationErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(error.getField(), error.getDefaultMessage());
        }

        String message = String.format("Validation failed: %d error(s)",
                ex.getBindingResult().getErrorCount());

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.INVALID_REQUEST.getCode(),
                message,
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle missing request parameters.
     *
     * @param ex The MissingServletRequestParameterException
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseV1_0> handleMissingParameter(
            MissingServletRequestParameterException ex,
            WebRequest request
    ) {
        log.warn("Missing parameter at {}: {}", getRequestPath(request), ex.getParameterName());

        Map<String, Object> details = new HashMap<>();
        details.put("parameter", ex.getParameterName());
        details.put("type", ex.getParameterType());

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.MISSING_REQUIRED_FIELD.getCode(),
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle method argument type mismatch (e.g., passing String when UUID expected).
     *
     * @param ex The MethodArgumentTypeMismatchException
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseV1_0> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request
    ) {
        log.warn("Type mismatch at {}: {} cannot be converted to {}",
                getRequestPath(request),
                ex.getValue(),
                ex.getRequiredType());

        Map<String, Object> details = new HashMap<>();
        details.put("parameter", ex.getName());
        details.put("providedValue", ex.getValue());
        details.put("expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.INVALID_REQUEST.getCode(),
                message,
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle malformed JSON requests.
     *
     * @param ex The HttpMessageNotReadableException
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseV1_0> handleMalformedJson(
            HttpMessageNotReadableException ex,
            WebRequest request
    ) {
        log.error("Malformed JSON at {}", getRequestPath(request), ex);

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.INVALID_REQUEST.getCode(),
                "Malformed JSON request body",
                new HashMap<>()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle unsupported HTTP method.
     *
     * @param ex The HttpRequestMethodNotSupportedException
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseV1_0> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request
    ) {
        log.warn("HTTP method not supported at {}: {}", getRequestPath(request), ex.getMethod());

        Map<String, Object> details = new HashMap<>();
        details.put("method", ex.getMethod());
        details.put("supportedMethods", ex.getSupportedHttpMethods());

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.INVALID_REQUEST.getCode(),
                String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
                details
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * Handle unsupported media type.
     *
     * @param ex The HttpMediaTypeNotSupportedException
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseV1_0> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            WebRequest request
    ) {
        log.warn("Media type not supported at {}: {}", getRequestPath(request), ex.getContentType());

        Map<String, Object> details = new HashMap<>();
        details.put("providedType", ex.getContentType());
        details.put("supportedTypes", ex.getSupportedMediaTypes());

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.INVALID_REQUEST.getCode(),
                "Unsupported media type. Please check Content-Type header.",
                details
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    /**
     * Handle file upload size exceeded exception.
     *
     * @param ex The MaxUploadSizeExceededException
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseV1_0> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            WebRequest request
    ) {
        log.error("File size exceeded at {}: {}", getRequestPath(request), ex.getMessage());

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.FILE_TOO_LARGE.getCode(),
                ErrorCodes.FILE_TOO_LARGE.getMessage(),
                new HashMap<>()
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * Handle IllegalArgumentException (typically from invalid input).
     *
     * @param ex The IllegalArgumentException
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseV1_0> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        log.error("IllegalArgumentException at {}: {}", getRequestPath(request), ex.getMessage(), ex);

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.INVALID_REQUEST.getCode(),
                ex.getMessage(),
                new HashMap<>()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle NullPointerException (should be avoided, but catch as safety net).
     *
     * @param ex The NullPointerException
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponseV1_0> handleNullPointerException(
            NullPointerException ex,
            WebRequest request
    ) {
        log.error("NullPointerException at {} - this should be handled properly in code",
                getRequestPath(request), ex);

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.INTERNAL_ERROR.getCode(),
                "An internal error occurred due to missing data",
                new HashMap<>()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle all other uncaught exceptions.
     * This is the catch-all handler for any exception not explicitly handled above.
     *
     * @param ex The generic exception
     * @param request The web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseV1_0> handleGenericException(
            Exception ex,
            WebRequest request
    ) {
        log.error("Unexpected error at {}", getRequestPath(request), ex);

        ErrorResponseV1_0 errorResponse = buildErrorResponse(
                ErrorCodes.INTERNAL_ERROR.getCode(),
                "An unexpected error occurred",
                new HashMap<>()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Build a standardized error response.
     *
     * @param errorCode Error code
     * @param message Error message
     * @param details Additional error details
     * @return ErrorResponseV1_0 object
     */
    private ErrorResponseV1_0 buildErrorResponse(String errorCode, String message, Map<String, Object> details) {
        return ErrorResponseV1_0.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .details(details != null ? details : new HashMap<>())
                .build();
    }

    /**
     * Extract request path from WebRequest.
     *
     * @param request The web request
     * @return Request path or "unknown" if not available
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return "unknown";
    }
}
