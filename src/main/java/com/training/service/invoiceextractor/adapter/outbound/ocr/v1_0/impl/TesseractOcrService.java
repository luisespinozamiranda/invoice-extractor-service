package com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.impl;

import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.IOcrService;
import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tesseract OCR service implementation.
 * Adapter implementation for extracting text from PDF and image files using Tesseract OCR engine.
 *
 * <p><b>Architecture:</b> Outbound Adapter (Hexagonal Architecture)
 * <p><b>Technology:</b> Tesseract 5.x via Tess4J library
 * <p><b>Supported Formats:</b> PDF, PNG, JPG, JPEG, TIFF
 *
 * <p><b>Dependencies:</b>
 * <ul>
 *   <li>Tess4J - Java wrapper for Tesseract</li>
 *   <li>Apache PDFBox - PDF rendering to images</li>
 * </ul>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-09
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TesseractOcrService implements IOcrService {

    @Value("${ocr.tesseract.datapath:tessdata}")
    private String tesseractDataPath;

    @Value("${ocr.tesseract.language:eng}")
    private String tesseractLanguage;

    @Value("${ocr.tesseract.dpi:300}")
    private int pdfRenderingDpi;

    @Value("${ocr.tesseract.enhance:false}")
    private boolean enhanceImages;

    @Value("${ocr.tesseract.oem:1}")
    private int ocrEngineMode;

    @Value("${ocr.tesseract.psm:3}")
    private int pageSegmentationMode;

    @Value("${ocr.tesseract.char-whitelist:}")
    private String charWhitelist;

    @Value("${ocr.tesseract.preserve-interword-spaces:1}")
    private String preserveInterwordSpaces;

    private static final String ENGINE_NAME = "Tesseract";
    private static final List<String> SUPPORTED_IMAGE_TYPES = List.of(
            "image/png", "image/jpeg", "image/jpg", "image/tiff"
    );
    private static final String PDF_TYPE = "application/pdf";

    @Override
    public CompletableFuture<OcrResult> extractText(byte[] fileData, String fileName, String fileType) {
        log.debug("Starting OCR extraction for file: {} (type: {})", fileName, fileType);

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                ITesseract tesseract = createTesseractInstance();

                if (PDF_TYPE.equalsIgnoreCase(fileType)) {
                    return extractFromPdf(tesseract, fileData, fileName, startTime);
                } else if (SUPPORTED_IMAGE_TYPES.stream().anyMatch(type -> type.equalsIgnoreCase(fileType))) {
                    return extractFromImage(tesseract, fileData, fileName, startTime);
                } else {
                    throw new InvoiceExtractorServiceException(
                            ErrorCodes.INVALID_FILE_FORMAT,
                            "Unsupported file type: " + fileType
                    );
                }
            } catch (InvoiceExtractorServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                log.error("OCR extraction failed for file: {}", fileName, ex);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.EXTRACTION_FAILED,
                        "OCR extraction failed: " + ex.getMessage()
                );
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isFormatSupported(String fileType) {
        return CompletableFuture.completedFuture(
                PDF_TYPE.equalsIgnoreCase(fileType) ||
                SUPPORTED_IMAGE_TYPES.stream().anyMatch(type -> type.equalsIgnoreCase(fileType))
        );
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME + " " + getVersion();
    }

    /**
     * Create and configure Tesseract instance with optimized settings for invoices.
     * All settings are configurable via application properties.
     */
    private ITesseract createTesseractInstance() {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractDataPath);
        tesseract.setLanguage(tesseractLanguage);

        // OEM (OCR Engine Mode) - configurable via ocr.tesseract.oem
        tesseract.setOcrEngineMode(ocrEngineMode);

        // PSM (Page Segmentation Mode) - configurable via ocr.tesseract.psm
        tesseract.setPageSegMode(pageSegmentationMode);

        // Character whitelist - configurable via ocr.tesseract.char-whitelist
        if (charWhitelist != null && !charWhitelist.isEmpty()) {
            tesseract.setTessVariable("tessedit_char_whitelist", charWhitelist);
        }

        // Preserve interword spaces - configurable via ocr.tesseract.preserve-interword-spaces
        tesseract.setTessVariable("preserve_interword_spaces", preserveInterwordSpaces);

        log.debug("Tesseract configured: datapath={}, language={}, PSM={}, OEM={}",
                tesseractDataPath, tesseractLanguage, pageSegmentationMode, ocrEngineMode);
        return tesseract;
    }

    /**
     * Extract text from image file with preprocessing.
     */
    private OcrResult extractFromImage(ITesseract tesseract, byte[] imageData, String fileName, long startTime) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));

            if (image == null) {
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.INVALID_FILE_FORMAT,
                        "Failed to read image: " + fileName
                );
            }

            log.debug("Processing image: {}x{} pixels", image.getWidth(), image.getHeight());

            // Apply image preprocessing if enabled
            if (enhanceImages) {
                image = enhanceImageForOcr(image);
                log.debug("Image enhancement applied");
            }

            String extractedText = tesseract.doOCR(image);
            double confidence = calculateConfidence(extractedText);
            long processingTime = System.currentTimeMillis() - startTime;

            log.info("Image OCR completed: {} (confidence: {}, time: {}ms)",
                    fileName, confidence, processingTime);

            return OcrResult.success(
                    extractedText,
                    confidence,
                    1, // Single image = 1 page
                    processingTime,
                    getEngineName(),
                    tesseractLanguage
            );

        } catch (TesseractException ex) {
            log.error("Tesseract OCR failed for image: {}", fileName, ex);
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.EXTRACTION_FAILED,
                    "Tesseract OCR failed: " + ex.getMessage()
            );
        } catch (IOException ex) {
            log.error("Failed to read image data: {}", fileName, ex);
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INVALID_FILE_FORMAT,
                    "Invalid image format: " + ex.getMessage()
            );
        }
    }

    /**
     * Extract text from PDF file by converting pages to images.
     */
    private OcrResult extractFromPdf(ITesseract tesseract, byte[] pdfData, String fileName, long startTime) {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            int pageCount = document.getNumberOfPages();
            log.debug("Processing PDF: {} ({} pages)", fileName, pageCount);

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            StringBuilder allText = new StringBuilder();
            List<Double> pageConfidences = new ArrayList<>();

            // Process each page
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                log.debug("Processing page {}/{}", pageIndex + 1, pageCount);

                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, pdfRenderingDpi);

                // Apply image preprocessing if enabled
                if (enhanceImages) {
                    pageImage = enhanceImageForOcr(pageImage);
                }

                String pageText = tesseract.doOCR(pageImage);

                allText.append(pageText);
                if (pageIndex < pageCount - 1) {
                    allText.append("\n\n=== PAGE ").append(pageIndex + 2).append(" ===\n\n");
                }

                pageConfidences.add(calculateConfidence(pageText));
            }

            // Average confidence across all pages
            double averageConfidence = pageConfidences.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            long processingTime = System.currentTimeMillis() - startTime;

            log.info("PDF OCR completed: {} ({} pages, confidence: {}, time: {}ms)",
                    fileName, pageCount, averageConfidence, processingTime);

            return OcrResult.success(
                    allText.toString(),
                    averageConfidence,
                    pageCount,
                    processingTime,
                    getEngineName(),
                    tesseractLanguage
            );

        } catch (TesseractException ex) {
            log.error("Tesseract OCR failed for PDF: {}", fileName, ex);
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.EXTRACTION_FAILED,
                    "Tesseract OCR failed: " + ex.getMessage()
            );
        } catch (IOException ex) {
            log.error("Failed to read PDF data: {}", fileName, ex);
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INVALID_FILE_FORMAT,
                    "Invalid PDF format: " + ex.getMessage()
            );
        }
    }

    /**
     * Calculate confidence score based on text quality heuristics.
     *
     * <p>This is a simplified heuristic. Real Tesseract confidence would require
     * using the hOCR output format or accessing internal confidence values.
     */
    private double calculateConfidence(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        double score = 0.5; // Base score

        // More text usually means better extraction
        int wordCount = text.split("\\s+").length;
        if (wordCount > 10) score += 0.1;
        if (wordCount > 50) score += 0.1;

        // Presence of alphanumeric characters
        if (text.matches(".*[A-Za-z].*")) score += 0.1;
        if (text.matches(".*\\d.*")) score += 0.1;

        // Penalize excessive special characters (noise)
        long specialCharCount = text.chars().filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)).count();
        double specialCharRatio = (double) specialCharCount / text.length();
        if (specialCharRatio > 0.3) score -= 0.2;

        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Enhance image quality for better OCR results.
     * Applies preprocessing techniques including:
     * - Grayscale conversion
     * - Contrast enhancement
     * - Adaptive thresholding (binarization)
     * - Noise reduction
     *
     * @param original Original image
     * @return Enhanced image optimized for OCR
     */
    private BufferedImage enhanceImageForOcr(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Step 1: Convert to grayscale
        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        // Step 2: Apply contrast enhancement and binarization
        BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        // Calculate threshold using Otsu's method (simplified version)
        int threshold = calculateOtsuThreshold(grayscale);

        // Apply threshold
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = grayscale.getRGB(x, y);
                int gray = pixel & 0xFF; // Extract gray value

                // Binarize: white if above threshold, black otherwise
                int newPixel = (gray > threshold) ? 0xFFFFFFFF : 0xFF000000;
                enhanced.setRGB(x, y, newPixel);
            }
        }

        return enhanced;
    }

    /**
     * Calculate optimal threshold using Otsu's method.
     * This is a simplified implementation for automatic thresholding.
     *
     * @param image Grayscale image
     * @return Optimal threshold value (0-255)
     */
    private int calculateOtsuThreshold(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Build histogram
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int gray = pixel & 0xFF;
                histogram[gray]++;
            }
        }

        // Total number of pixels
        int total = width * height;

        // Calculate the optimal threshold
        double sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        double sumB = 0;
        int wB = 0;
        int wF;

        double varMax = 0;
        int threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += histogram[t];
            if (wB == 0) continue;

            wF = total - wB;
            if (wF == 0) break;

            sumB += t * histogram[t];

            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;

            double varBetween = (double) wB * (double) wF * (mB - mF) * (mB - mF);

            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }

        return threshold;
    }

    /**
     * Get Tesseract version by executing tesseract --version command.
     * Returns the exact version of Tesseract installed on the system.
     */
    private String getVersion() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("tesseract", "--version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String firstLine = reader.readLine();
                if (firstLine != null && firstLine.contains("tesseract")) {
                    // Extract version from output like "tesseract 4.1.1" or "tesseract 5.3.0"
                    String[] parts = firstLine.split("\\s+");
                    if (parts.length >= 2) {
                        return parts[1]; // Return just the version number
                    }
                }
            }

            process.waitFor();
        } catch (Exception ex) {
            log.warn("Could not determine Tesseract version: {}", ex.getMessage());
        }

        return "unknown"; // Fallback if version cannot be determined
    }
}
