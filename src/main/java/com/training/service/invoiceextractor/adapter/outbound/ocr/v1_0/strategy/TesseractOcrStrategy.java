package com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.strategy;

import com.training.service.invoiceextractor.adapter.outbound.ocr.v1_0.OcrResult;
import com.training.service.invoiceextractor.configuration.TesseractProperties;
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
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tesseract OCR strategy implementation.
 * Implements the Strategy pattern for OCR operations using Tesseract engine.
 *
 * <p><b>Design Pattern:</b> Strategy Pattern (Concrete Strategy)
 * <p><b>Architecture:</b> Outbound Adapter (Hexagonal Architecture)
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-22
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TesseractOcrStrategy implements OcrStrategy {

    private static final String ENGINE_NAME = "Tesseract";
    private static final List<String> SUPPORTED_IMAGE_TYPES = List.of(
            "image/png", "image/jpeg", "image/jpg", "image/tiff"
    );
    private static final String PDF_TYPE = "application/pdf";
    private static final int HISTOGRAM_SIZE = 256;
    private static final double BASE_CONFIDENCE = 0.5;
    private static final int MIN_WORD_COUNT_LOW = 10;
    private static final int MIN_WORD_COUNT_HIGH = 50;
    private static final double MAX_SPECIAL_CHAR_RATIO = 0.3;

    // Color constants for binarization (ARGB format)
    private static final int WHITE_PIXEL_ARGB = 0xFFFFFFFF;  // White: Alpha=255, R=255, G=255, B=255
    private static final int BLACK_PIXEL_ARGB = 0xFF000000;  // Black: Alpha=255, R=0, G=0, B=0
    private static final int GRAYSCALE_MASK = 0xFF;          // Mask to extract grayscale value

    private final TesseractProperties properties;

    @Override
    public CompletableFuture<OcrResult> extractText(byte[] fileData, String fileName, String fileType) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                ITesseract tesseract = createTesseractInstance();

                if (PDF_TYPE.equalsIgnoreCase(fileType)) {
                    return extractFromPdf(tesseract, fileData, fileName, startTime);
                } else if (isSupportedImageType(fileType)) {
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
                        "OCR extraction failed: " + ex.getMessage(),
                        ex
                );
            }
        });
    }

    @Override
    public boolean supports(String fileType) {
        return PDF_TYPE.equalsIgnoreCase(fileType) || isSupportedImageType(fileType);
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME + " " + detectTesseractVersion();
    }

    @Override
    public int getPriority() {
        return 10; // Default priority
    }

    private boolean isSupportedImageType(String fileType) {
        return SUPPORTED_IMAGE_TYPES.stream().anyMatch(type -> type.equalsIgnoreCase(fileType));
    }

    private ITesseract createTesseractInstance() {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(properties.getDatapath());
        tesseract.setLanguage(properties.getLanguage());
        tesseract.setOcrEngineMode(properties.getOem());
        tesseract.setPageSegMode(properties.getPsm());

        if (properties.getCharWhitelist() != null && !properties.getCharWhitelist().isEmpty()) {
            tesseract.setVariable("tessedit_char_whitelist", properties.getCharWhitelist());
        }

        tesseract.setVariable("preserve_interword_spaces", properties.getPreserveInterwordSpaces());

        return tesseract;
    }

    private OcrResult extractFromImage(ITesseract tesseract, byte[] imageData, String fileName, long startTime) {
        try {
            BufferedImage image = readImage(imageData, fileName);

            if (properties.isEnhance()) {
                image = enhanceImageForOcr(image);
            }

            String extractedText = performOcr(tesseract, image, fileName);
            double confidence = calculateConfidence(extractedText);
            long processingTime = System.currentTimeMillis() - startTime;

            return OcrResult.success(
                    extractedText,
                    confidence,
                    1,
                    processingTime,
                    getEngineName(),
                    properties.getLanguage()
            );

        } catch (TesseractException ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.EXTRACTION_FAILED,
                    "Tesseract OCR failed: " + ex.getMessage(),
                    ex
            );
        } catch (IOException ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INVALID_FILE_FORMAT,
                    "Invalid image format: " + ex.getMessage(),
                    ex
            );
        }
    }

    private OcrResult extractFromPdf(ITesseract tesseract, byte[] pdfData, String fileName, long startTime) {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            int pageCount = document.getNumberOfPages();
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            StringBuilder allText = new StringBuilder();
            List<Double> pageConfidences = new ArrayList<>();

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage pageImage = renderPdfPage(pdfRenderer, pageIndex);

                if (properties.isEnhance()) {
                    pageImage = enhanceImageForOcr(pageImage);
                }

                String pageText = performOcr(tesseract, pageImage, fileName);
                appendPageText(allText, pageText, pageIndex, pageCount);
                pageConfidences.add(calculateConfidence(pageText));
            }

            double averageConfidence = calculateAverageConfidence(pageConfidences);
            long processingTime = System.currentTimeMillis() - startTime;

            return OcrResult.success(
                    allText.toString(),
                    averageConfidence,
                    pageCount,
                    processingTime,
                    getEngineName(),
                    properties.getLanguage()
            );

        } catch (TesseractException ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.EXTRACTION_FAILED,
                    "Tesseract OCR failed: " + ex.getMessage(),
                    ex
            );
        } catch (IOException ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INVALID_FILE_FORMAT,
                    "Invalid PDF format: " + ex.getMessage(),
                    ex
            );
        }
    }

    private BufferedImage readImage(byte[] imageData, String fileName) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));

        if (image == null) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.INVALID_FILE_FORMAT,
                    "Failed to read image: " + fileName
            );
        }

        return image;
    }

    private String performOcr(ITesseract tesseract, BufferedImage image, String fileName) throws TesseractException {
        return tesseract.doOCR(image);
    }

    private BufferedImage renderPdfPage(PDFRenderer renderer, int pageIndex) throws IOException {
        return renderer.renderImageWithDPI(pageIndex, properties.getDpi());
    }

    private void appendPageText(StringBuilder allText, String pageText, int pageIndex, int totalPages) {
        allText.append(pageText);
        if (pageIndex < totalPages - 1) {
            allText.append("\n\n=== PAGE ").append(pageIndex + 2).append(" ===\n\n");
        }
    }

    private double calculateAverageConfidence(List<Double> confidences) {
        return confidences.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double calculateConfidence(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        double score = BASE_CONFIDENCE;
        int wordCount = text.split("\\s+").length;

        if (wordCount > MIN_WORD_COUNT_LOW) score += 0.1;
        if (wordCount > MIN_WORD_COUNT_HIGH) score += 0.1;
        if (text.matches(".*[A-Za-z].*")) score += 0.1;
        if (text.matches(".*\\d.*")) score += 0.1;

        double specialCharRatio = calculateSpecialCharRatio(text);
        if (specialCharRatio > MAX_SPECIAL_CHAR_RATIO) score -= 0.2;

        return Math.max(0.0, Math.min(1.0, score));
    }

    private double calculateSpecialCharRatio(String text) {
        long specialCharCount = text.chars()
                .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                .count();
        return (double) specialCharCount / text.length();
    }

    private BufferedImage enhanceImageForOcr(BufferedImage original) {
        BufferedImage grayscale = convertToGrayscale(original);
        int threshold = calculateOtsuThreshold(grayscale);
        return applyBinarization(grayscale, threshold);
    }

    private BufferedImage convertToGrayscale(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        return grayscale;
    }

    private BufferedImage applyBinarization(BufferedImage grayscale, int threshold) {
        int width = grayscale.getWidth();
        int height = grayscale.getHeight();
        BufferedImage binarizedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int yCoordinate = 0; yCoordinate < height; yCoordinate++) {
            for (int xCoordinate = 0; xCoordinate < width; xCoordinate++) {
                int grayscaleValue = grayscale.getRGB(xCoordinate, yCoordinate) & GRAYSCALE_MASK;
                int binarizedPixel = (grayscaleValue > threshold) ? WHITE_PIXEL_ARGB : BLACK_PIXEL_ARGB;
                binarizedImage.setRGB(xCoordinate, yCoordinate, binarizedPixel);
            }
        }

        return binarizedImage;
    }

    /**
     * Calculate optimal threshold using Otsu's method.
     * Otsu's algorithm finds the threshold that minimizes intra-class variance
     * (or equivalently, maximizes inter-class variance) between foreground and background.
     *
     * @param image Grayscale image to analyze
     * @return Optimal threshold value (0-255)
     */
    private int calculateOtsuThreshold(BufferedImage image) {
        int[] histogram = buildHistogram(image);
        int totalPixels = image.getWidth() * image.getHeight();

        double totalWeightedSum = calculateHistogramSum(histogram);
        double backgroundWeightedSum = 0.0;
        int backgroundPixelCount = 0;

        double maximumVariance = 0.0;
        int optimalThreshold = 0;

        // Iterate through all possible threshold values
        for (int thresholdCandidate = 0; thresholdCandidate < HISTOGRAM_SIZE; thresholdCandidate++) {
            backgroundPixelCount += histogram[thresholdCandidate];

            // Skip if no background pixels yet
            if (backgroundPixelCount == 0) {
                continue;
            }

            int foregroundPixelCount = totalPixels - backgroundPixelCount;

            // Stop if no foreground pixels remain
            if (foregroundPixelCount == 0) {
                break;
            }

            // Calculate weighted sums and means
            backgroundWeightedSum += thresholdCandidate * histogram[thresholdCandidate];
            double backgroundMean = backgroundWeightedSum / backgroundPixelCount;
            double foregroundMean = (totalWeightedSum - backgroundWeightedSum) / foregroundPixelCount;

            // Calculate between-class variance
            double meanDifference = backgroundMean - foregroundMean;
            double betweenClassVariance = (double) backgroundPixelCount * foregroundPixelCount * meanDifference * meanDifference;

            // Update threshold if this variance is higher
            if (betweenClassVariance > maximumVariance) {
                maximumVariance = betweenClassVariance;
                optimalThreshold = thresholdCandidate;
            }
        }

        return optimalThreshold;
    }

    private int[] buildHistogram(BufferedImage image) {
        int[] histogram = new int[HISTOGRAM_SIZE];
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        for (int yCoordinate = 0; yCoordinate < imageHeight; yCoordinate++) {
            for (int xCoordinate = 0; xCoordinate < imageWidth; xCoordinate++) {
                int grayscaleValue = image.getRGB(xCoordinate, yCoordinate) & GRAYSCALE_MASK;
                histogram[grayscaleValue]++;
            }
        }

        return histogram;
    }

    private double calculateHistogramSum(int[] histogram) {
        double sum = 0;
        for (int i = 0; i < HISTOGRAM_SIZE; i++) {
            sum += i * histogram[i];
        }
        return sum;
    }

    private String detectTesseractVersion() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("tesseract", "--version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String firstLine = reader.readLine();
                if (firstLine != null && firstLine.contains("tesseract")) {
                    String[] parts = firstLine.split("\\s+");
                    if (parts.length >= 2) {
                        return parts[1];
                    }
                }
            }

            process.waitFor();
        } catch (Exception ex) {
            log.warn("Could not determine Tesseract version: {}", ex.getMessage());
        }

        return "unknown";
    }
}
