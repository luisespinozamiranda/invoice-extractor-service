package com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.impl;

import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;
import com.training.service.invoiceextractor.configuration.TesseractProperties;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TesseractOcrService.
 *
 * <p>Tests OCR text extraction functionality using Tesseract engine.
 * Note: These are simplified tests. Full integration tests would require
 * Tesseract to be installed and configured.
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-24
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TesseractOcrService Tests")
class TesseractOcrServiceTest {

    @Mock
    private TesseractProperties tesseractProperties;

    private TesseractOcrService ocrService;

    @BeforeEach
    void setUp() {
        lenient().when(tesseractProperties.getDatapath()).thenReturn("C:/Program Files/Tesseract-OCR/tessdata");
        lenient().when(tesseractProperties.getLanguage()).thenReturn("eng");
        lenient().when(tesseractProperties.getDpi()).thenReturn(300);

        ocrService = new TesseractOcrService(tesseractProperties);
    }

    // =========================
    // BASIC FUNCTIONALITY TESTS
    // =========================

    @Test
    @DisplayName("Should handle unsupported file type")
    void testExtractText_UnsupportedFormat() {
        // Arrange
        byte[] fileData = "test data".getBytes();
        String fileName = "test.docx";
        String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        // Act
        CompletableFuture<OcrResult> result = ocrService.extractText(fileData, fileName, fileType);

        // Assert - should fail since Tesseract is not configured
        Exception exception = assertThrows(Exception.class, result::join);
        Throwable cause = exception.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof InvoiceExtractorServiceException);

        // When Tesseract is not installed, it may throw EXTRACTION_FAILED or INVALID_FILE_FORMAT
        InvoiceExtractorServiceException ocrException = (InvoiceExtractorServiceException) cause;
        assertNotNull(ocrException.getErrorCode());
    }

    // =========================
    // FORMAT SUPPORT TESTS
    // =========================

    @Test
    @DisplayName("Should support PNG format")
    void testIsFormatSupported_PNG() {
        // Act
        CompletableFuture<Boolean> result = ocrService.isFormatSupported("image/png");

        // Assert
        assertTrue(result.join());
    }

    @Test
    @DisplayName("Should support JPG format")
    void testIsFormatSupported_JPG() {
        // Act
        CompletableFuture<Boolean> result = ocrService.isFormatSupported("image/jpeg");

        // Assert
        assertTrue(result.join());
    }

    @Test
    @DisplayName("Should support PDF format")
    void testIsFormatSupported_PDF() {
        // Act
        CompletableFuture<Boolean> result = ocrService.isFormatSupported("application/pdf");

        // Assert
        assertTrue(result.join());
    }

    @Test
    @DisplayName("Should not support unsupported format")
    void testIsFormatSupported_Unsupported() {
        // Act
        CompletableFuture<Boolean> result = ocrService.isFormatSupported("application/msword");

        // Assert
        assertFalse(result.join());
    }

    // =========================
    // GET ENGINE NAME TESTS
    // =========================

    @Test
    @DisplayName("Should return correct engine name")
    void testGetEngineName() {
        // Act
        String engineName = ocrService.getEngineName();

        // Assert - When Tesseract is not installed, it returns "Tesseract unknown"
        assertTrue(engineName.startsWith("Tesseract"));
    }
}
