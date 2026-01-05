package com.training.service.invoiceextractor.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * Configuration class for OCR (Optical Character Recognition) settings.
 * Validates Tesseract installation and configuration on application startup.
 */
@Configuration
@Slf4j
public class OcrConfiguration {

    private static final int MIN_DPI = 72;
    private static final int MAX_DPI = 600;
    private static final int RECOMMENDED_DPI = 300;
    private static final String TESSDATA_DOWNLOAD_URL = "https://github.com/tesseract-ocr/tessdata";

    private final TesseractProperties properties;

    public OcrConfiguration(TesseractProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates OCR configuration on application startup.
     * Logs warnings if Tesseract is not properly configured but doesn't prevent startup.
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("=== OCR Configuration ===");
        log.info("Tesseract Data Path: {}", properties.getDatapath());
        log.info("Tesseract Language: {}", properties.getLanguage());
        log.info("PDF Rendering DPI: {}", properties.getDpi());
        log.info("Image Enhancement: {}", properties.isEnhance());
        log.info("OCR Engine Mode: {}", properties.getOem());
        log.info("Page Segmentation Mode: {}", properties.getPsm());

        // Check if tessdata directory exists
        File tessdataDir = new File(properties.getDatapath());
        if (!tessdataDir.exists()) {
            log.warn("Tesseract data path does not exist: {}", properties.getDatapath());
            log.warn("Download language files from: {}", TESSDATA_DOWNLOAD_URL);
            log.warn("OCR extraction may fail until tessdata is properly configured");
        } else {
            log.info("Tesseract data path found: {}", tessdataDir.getAbsolutePath());

            // Check if language file exists
            String languageFile = properties.getLanguage() + ".traineddata";
            File langFile = new File(tessdataDir, languageFile);
            if (!langFile.exists()) {
                log.warn("Language file not found: {}", languageFile);
                log.warn("Download from: {}/blob/main/{}", TESSDATA_DOWNLOAD_URL, languageFile);
            } else {
                log.info("Language file found: {}", langFile.getName());
            }
        }

        // Validate DPI
        int dpi = properties.getDpi();
        if (dpi < MIN_DPI || dpi > MAX_DPI) {
            log.warn("PDF rendering DPI ({}) is outside recommended range [{}-{}]",
                    dpi, MIN_DPI, MAX_DPI);
            log.warn("Recommended: {} DPI for balance between quality and performance", RECOMMENDED_DPI);
        }

        log.info("========================");
    }

    /**
     * Check if OCR is properly configured and enabled.
     *
     * @return true if OCR can be used
     */
    public boolean isOcrAvailable() {
        File tessdataDir = new File(properties.getDatapath());
        if (!tessdataDir.exists()) {
            return false;
        }

        String languageFile = properties.getLanguage() + ".traineddata";
        File langFile = new File(tessdataDir, languageFile);
        return langFile.exists();
    }
}
