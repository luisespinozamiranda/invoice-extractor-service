package com.training.service.invoiceextractor.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * Configuration class for OCR (Optical Character Recognition) settings.
 * Validates Tesseract installation and configuration on application startup.
 *
 * <p><b>Configuration Properties:</b>
 * <ul>
 *   <li>{@code ocr.tesseract.datapath} - Path to Tesseract training data (tessdata)</li>
 *   <li>{@code ocr.tesseract.language} - Language for OCR (e.g., eng, spa, fra)</li>
 *   <li>{@code ocr.tesseract.dpi} - DPI for PDF rendering (higher = better quality)</li>
 * </ul>
 *
 * <p><b>Tesseract Setup Requirements:</b>
 * <ol>
 *   <li>Install Tesseract OCR binary on the system</li>
 *   <li>Download language training data files (.traineddata)</li>
 *   <li>Configure datapath to point to tessdata directory</li>
 * </ol>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-09
 * @see com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.impl.TesseractOcrService
 */
@Configuration
@Slf4j
public class OcrConfiguration {

    @Value("${ocr.tesseract.datapath:tessdata}")
    private String tesseractDataPath;

    @Value("${ocr.tesseract.language:eng}")
    private String tesseractLanguage;

    @Value("${ocr.tesseract.dpi:300}")
    private int pdfRenderingDpi;

    @Value("${ocr.enabled:true}")
    private boolean ocrEnabled;

    /**
     * Validates OCR configuration on application startup.
     * Logs warnings if Tesseract is not properly configured but doesn't prevent startup.
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("=== OCR Configuration ===");
        log.info("OCR Enabled: {}", ocrEnabled);
        log.info("Tesseract Data Path: {}", tesseractDataPath);
        log.info("Tesseract Language: {}", tesseractLanguage);
        log.info("PDF Rendering DPI: {}", pdfRenderingDpi);

        if (!ocrEnabled) {
            log.warn("OCR is DISABLED - extraction will use simulation mode");
            return;
        }

        // Check if tessdata directory exists
        File tessdataDir = new File(tesseractDataPath);
        if (!tessdataDir.exists()) {
            log.warn("Tesseract data path does not exist: {}", tesseractDataPath);
            log.warn("Download language files from: https://github.com/tesseract-ocr/tessdata");
            log.warn("OCR extraction may fail until tessdata is properly configured");
        } else {
            log.info("Tesseract data path found: {}", tessdataDir.getAbsolutePath());

            // Check if language file exists
            String languageFile = tesseractLanguage + ".traineddata";
            File langFile = new File(tessdataDir, languageFile);
            if (!langFile.exists()) {
                log.warn("Language file not found: {}", languageFile);
                log.warn("Download from: https://github.com/tesseract-ocr/tessdata/blob/main/{}", languageFile);
            } else {
                log.info("Language file found: {}", langFile.getName());
            }
        }

        // Validate DPI
        if (pdfRenderingDpi < 72 || pdfRenderingDpi > 600) {
            log.warn("PDF rendering DPI ({}) is outside recommended range [72-600]", pdfRenderingDpi);
            log.warn("Recommended: 300 DPI for balance between quality and performance");
        }

        log.info("========================");
    }

    /**
     * Check if OCR is properly configured and enabled.
     *
     * @return true if OCR can be used
     */
    public boolean isOcrAvailable() {
        if (!ocrEnabled) {
            return false;
        }

        File tessdataDir = new File(tesseractDataPath);
        if (!tessdataDir.exists()) {
            return false;
        }

        String languageFile = tesseractLanguage + ".traineddata";
        File langFile = new File(tessdataDir, languageFile);
        return langFile.exists();
    }
}
