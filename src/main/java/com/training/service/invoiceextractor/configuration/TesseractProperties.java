package com.training.service.invoiceextractor.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Tesseract OCR service.
 * Binds properties with prefix "ocr.tesseract" from application.properties/yml.
 */
@Configuration
@ConfigurationProperties(prefix = "ocr.tesseract")
@Getter
@Setter
public class TesseractProperties {

    /**
     * Path to Tesseract training data directory (tessdata).
     */
    private String datapath = "tessdata";

    /**
     * Language for OCR (e.g., eng, spa, fra).
     */
    private String language = "eng";

    /**
     * DPI for PDF rendering (higher = better quality, slower processing).
     */
    private int dpi = 300;

    /**
     * Enable image enhancement preprocessing.
     */
    private boolean enhance = false;

    /**
     * OCR Engine Mode (0=Legacy, 1=Neural nets LSTM, 2=Legacy+LSTM, 3=Default).
     */
    private int oem = 1;

    /**
     * Page Segmentation Mode (0-13, 3=Fully automatic page segmentation).
     */
    private int psm = 3;

    /**
     * Character whitelist for OCR (empty = all characters allowed).
     */
    private String charWhitelist = "";

    /**
     * Preserve interword spaces (1=yes, 0=no).
     */
    private String preserveInterwordSpaces = "1";

    /**
     * Whether OCR is enabled globally.
     */
    private boolean enabled = true;
}
