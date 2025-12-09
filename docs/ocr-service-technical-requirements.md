# OCR Service Technical Requirements

**Project:** Invoice Extractor - OCR Service
**Document Type:** OCR Integration Technical Requirements
**Version:** 1.0
**Date:** 2025-12-08
**Related Documents:**
- [Technical Requirements Document](technical-requirements-document.md)
- [Backend Technical Acceptance Criteria](technical-acceptance-criteria.md)

---

## 1. Overview

The OCR (Optical Character Recognition) Service is responsible for extracting text and structured data from invoice documents (PDF and images). This service acts as an adapter in the hexagonal architecture, implementing the extraction logic using external OCR providers.

### 1.1 Responsibilities

- Extract raw text from PDF and image files
- Parse invoice-specific fields (invoice number, amount, client name, address)
- Return structured extraction results with confidence scores
- Handle OCR failures and retries
- Log extraction metadata for auditing

### 1.2 Technology Options

| OCR Provider | Pros | Cons | Cost | Recommendation |
|--------------|------|------|------|----------------|
| **Tesseract OCR** | Free, open-source, offline | Lower accuracy, slower | Free | ✅ **Best for training/learning** |
| **Google Cloud Vision API** | High accuracy, fast, supports many formats | Requires Google Cloud account | Pay-per-use ($1.50/1000 requests) | Good for production |
| **AWS Textract** | Excellent for forms/invoices, extracts key-value pairs | AWS dependency | Pay-per-use ($1.50/1000 pages) | Best for production |
| **Azure Computer Vision** | Good accuracy, integrated with Azure | Azure dependency | Pay-per-use | Alternative option |

**Recommendation for this project:** Start with **Tesseract OCR** for development/training, then migrate to **AWS Textract** for production due to its invoice-specific capabilities.

---

## 2. Architecture

### 2.1 Package Structure

```
src/main/java/com/training/service/invoiceextractor/
├── adapter/
│   └── outbound/
│       └── ocr/
│           ├── IOcrService.java                    # Port interface
│           ├── TesseractOcrService.java           # Tesseract implementation
│           ├── AwsTextractOcrService.java         # AWS Textract implementation (future)
│           ├── model/
│           │   ├── OcrResult.java                 # Raw OCR output
│           │   └── InvoiceExtractionResult.java   # Parsed invoice data
│           └── parser/
│               └── InvoiceDataParser.java         # Text parsing logic
├── domain/
│   └── service/
│       ├── IExtractionService.java                # Domain service interface
│       └── ExtractionService.java                 # Domain service implementation
```

### 2.2 Data Flow

```
1. Controller receives file upload
   ↓
2. ExtractionService (Domain) orchestrates extraction
   ↓
3. IOcrService (Port) defines contract
   ↓
4. TesseractOcrService (Adapter) implements OCR
   ↓
5. InvoiceDataParser parses raw text into structured data
   ↓
6. Result stored in database via Repository Service
   ↓
7. Response returned to client
```

---

## 3. OCR Service Interface (Port)

### 3.1 Interface Definition

```java
// src/app/adapter/outbound/ocr/IOcrService.java
package com.training.service.invoiceextractor.adapter.outbound.ocr;

import com.training.service.invoiceextractor.adapter.outbound.ocr.model.InvoiceExtractionResult;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for OCR services.
 * Implementations can use different OCR providers (Tesseract, AWS Textract, Google Vision, etc.)
 */
public interface IOcrService {

    /**
     * Extract invoice data from file
     *
     * @param fileData Binary file data (PDF or image)
     * @param fileName Original file name
     * @param fileType MIME type (application/pdf, image/png, etc.)
     * @return CompletableFuture with extraction result
     */
    CompletableFuture<InvoiceExtractionResult> extractInvoiceData(
        byte[] fileData,
        String fileName,
        String fileType
    );

    /**
     * Check if OCR service is available/healthy
     *
     * @return CompletableFuture with health status
     */
    CompletableFuture<Boolean> isServiceAvailable();

    /**
     * Get OCR provider name
     *
     * @return Provider name (e.g., "Tesseract", "AWS Textract")
     */
    String getProviderName();
}
```

### 3.2 Extraction Result Models

```java
// src/app/adapter/outbound/ocr/model/OcrResult.java
package com.training.service.invoiceextractor.adapter.outbound.ocr.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Raw OCR output containing extracted text
 */
@Data
@Builder
public class OcrResult {
    private String extractedText;
    private Double confidenceScore;  // 0.0 to 1.0
    private String ocrEngine;
    private LocalDateTime extractionTimestamp;
    private Integer pageCount;
    private String errorMessage;
}
```

```java
// src/app/adapter/outbound/ocr/model/InvoiceExtractionResult.java
package com.training.service.invoiceextractor.adapter.outbound.ocr.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Parsed invoice data extracted from OCR text
 */
@Data
@Builder
public class InvoiceExtractionResult {
    // Required fields
    private String invoiceNumber;
    private BigDecimal invoiceAmount;
    private String clientName;
    private String clientAddress;

    // Optional fields
    private String vendorName;
    private String vendorAddress;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String currency;

    // Metadata
    private Double confidenceScore;
    private String ocrEngine;
    private LocalDateTime extractionTimestamp;
    private String rawText;  // Full extracted text for debugging
    private Boolean hasErrors;
    private String errorMessage;

    // Field-level confidence
    private Double invoiceNumberConfidence;
    private Double invoiceAmountConfidence;
    private Double clientNameConfidence;
    private Double clientAddressConfidence;
}
```

---

## 4. Tesseract OCR Implementation

### 4.1 Maven Dependencies

Add to `pom.xml`:

```xml
<!-- Tesseract OCR -->
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.9.0</version>
</dependency>

<!-- Apache PDFBox for PDF handling -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- Apache Commons IO -->
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.15.1</version>
</dependency>
```

### 4.2 Tesseract Configuration

```java
// src/app/configuration/TesseractConfiguration.java
package com.training.service.invoiceextractor.configuration;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TesseractConfiguration {

    @Value("${ocr.tesseract.data-path:/usr/share/tesseract-ocr/5/tessdata}")
    private String tesseractDataPath;

    @Value("${ocr.tesseract.language:eng}")
    private String tesseractLanguage;

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractDataPath);
        tesseract.setLanguage(tesseractLanguage);
        tesseract.setPageSegMode(1);  // Automatic page segmentation with OSD
        tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
        return tesseract;
    }
}
```

### 4.3 application.properties Configuration

```properties
# OCR Configuration
ocr.tesseract.data-path=C:/Program Files/Tesseract-OCR/tessdata
ocr.tesseract.language=eng
ocr.extraction.timeout=30000
ocr.extraction.retry.max-attempts=3
ocr.extraction.retry.backoff-delay=2000

# Resilience4j CircuitBreaker for OCR
resilience4j.circuitbreaker.instances.ocrService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.ocrService.wait-duration-in-open-state=60s
resilience4j.circuitbreaker.instances.ocrService.sliding-window-size=10

# Resilience4j Retry for OCR
resilience4j.retry.instances.ocrService.max-attempts=3
resilience4j.retry.instances.ocrService.wait-duration=2s
resilience4j.retry.instances.ocrService.exponential-backoff-multiplier=2
```

### 4.4 Tesseract Service Implementation

```java
// src/app/adapter/outbound/ocr/TesseractOcrService.java
package com.training.service.invoiceextractor.adapter.outbound.ocr;

import com.training.service.invoiceextractor.adapter.outbound.ocr.model.InvoiceExtractionResult;
import com.training.service.invoiceextractor.adapter.outbound.ocr.model.OcrResult;
import com.training.service.invoiceextractor.adapter.outbound.ocr.parser.InvoiceDataParser;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TesseractOcrService implements IOcrService {

    private final Tesseract tesseract;
    private final InvoiceDataParser invoiceDataParser;

    @Autowired
    public TesseractOcrService(Tesseract tesseract, InvoiceDataParser invoiceDataParser) {
        this.tesseract = tesseract;
        this.invoiceDataParser = invoiceDataParser;
    }

    @Override
    @Async
    @CircuitBreaker(name = "ocrService", fallbackMethod = "extractInvoiceDataFallback")
    @Retry(name = "ocrService")
    public CompletableFuture<InvoiceExtractionResult> extractInvoiceData(
        byte[] fileData,
        String fileName,
        String fileType
    ) {
        log.info("Starting OCR extraction for file: {}, type: {}", fileName, fileType);

        try {
            // Step 1: Extract raw text using Tesseract
            OcrResult ocrResult = performOcr(fileData, fileType);

            if (ocrResult.getExtractedText() == null || ocrResult.getExtractedText().isEmpty()) {
                throw new InvoiceExtractorServiceException(
                    ErrorCodes.EXTRACTION_FAILED,
                    "No text could be extracted from the file"
                );
            }

            log.debug("OCR extraction complete. Text length: {} characters",
                     ocrResult.getExtractedText().length());

            // Step 2: Parse invoice data from raw text
            InvoiceExtractionResult result = invoiceDataParser.parseInvoiceData(
                ocrResult.getExtractedText(),
                ocrResult.getConfidenceScore(),
                ocrResult.getOcrEngine()
            );

            log.info("Invoice extraction successful for file: {}. Invoice #: {}, Amount: {}",
                    fileName, result.getInvoiceNumber(), result.getInvoiceAmount());

            return CompletableFuture.completedFuture(result);

        } catch (TesseractException e) {
            log.error("Tesseract OCR failed for file: {}", fileName, e);
            throw new InvoiceExtractorServiceException(
                ErrorCodes.OCR_SERVICE_UNAVAILABLE,
                "OCR processing failed: " + e.getMessage()
            );
        } catch (IOException e) {
            log.error("Failed to read file: {}", fileName, e);
            throw new InvoiceExtractorServiceException(
                ErrorCodes.INVALID_FILE_TYPE,
                "File could not be read: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error during OCR extraction for file: {}", fileName, e);
            throw new InvoiceExtractorServiceException(
                ErrorCodes.EXTRACTION_FAILED,
                "Extraction failed: " + e.getMessage()
            );
        }
    }

    /**
     * Perform OCR on file based on type
     */
    private OcrResult performOcr(byte[] fileData, String fileType) throws IOException, TesseractException {
        BufferedImage image;

        if (fileType.equals("application/pdf")) {
            image = convertPdfToImage(fileData);
        } else {
            image = ImageIO.read(new ByteArrayInputStream(fileData));
        }

        if (image == null) {
            throw new IOException("Failed to convert file to image");
        }

        // Perform OCR
        String extractedText = tesseract.doOCR(image);

        // Tesseract doesn't provide confidence per extraction, estimate based on text quality
        double confidence = estimateConfidence(extractedText);

        return OcrResult.builder()
            .extractedText(extractedText)
            .confidenceScore(confidence)
            .ocrEngine("Tesseract 5.x")
            .extractionTimestamp(LocalDateTime.now())
            .pageCount(1)
            .build();
    }

    /**
     * Convert PDF first page to image for OCR
     */
    private BufferedImage convertPdfToImage(byte[] pdfData) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFRenderer renderer = new PDFRenderer(document);
            // Render first page at 300 DPI for better OCR accuracy
            return renderer.renderImageWithDPI(0, 300);
        }
    }

    /**
     * Estimate confidence based on text characteristics
     * This is a simplified heuristic since Tesseract doesn't provide per-extraction confidence
     */
    private double estimateConfidence(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        // Simple heuristic: ratio of alphanumeric characters to total characters
        long alphanumericCount = text.chars()
            .filter(Character::isLetterOrDigit)
            .count();

        double ratio = (double) alphanumericCount / text.length();

        // Normalize to 0.5 - 0.95 range
        return 0.5 + (ratio * 0.45);
    }

    @Override
    public CompletableFuture<Boolean> isServiceAvailable() {
        try {
            // Simple test to check if Tesseract is available
            BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            tesseract.doOCR(testImage);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Tesseract service health check failed", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public String getProviderName() {
        return "Tesseract OCR";
    }

    /**
     * Fallback method for CircuitBreaker
     */
    public CompletableFuture<InvoiceExtractionResult> extractInvoiceDataFallback(
        byte[] fileData,
        String fileName,
        String fileType,
        Exception e
    ) {
        log.error("OCR service circuit breaker triggered for file: {}", fileName, e);

        InvoiceExtractionResult fallbackResult = InvoiceExtractionResult.builder()
            .invoiceNumber("EXTRACTION_FAILED")
            .clientName("EXTRACTION_FAILED")
            .clientAddress("EXTRACTION_FAILED")
            .hasErrors(true)
            .errorMessage("OCR service unavailable. Please try again later.")
            .extractionTimestamp(LocalDateTime.now())
            .build();

        return CompletableFuture.completedFuture(fallbackResult);
    }
}
```

---

## 5. Invoice Data Parser

### 5.1 Parser Implementation

```java
// src/app/adapter/outbound/ocr/parser/InvoiceDataParser.java
package com.training.service.invoiceextractor.adapter.outbound.ocr.parser;

import com.training.service.invoiceextractor.adapter.outbound.ocr.model.InvoiceExtractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw OCR text to extract structured invoice data
 */
@Component
@Slf4j
public class InvoiceDataParser {

    // Regular expression patterns for invoice fields
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile(
        "(?:Invoice\\s*(?:#|No\\.?|Number)?\\s*:?\\s*)([A-Z0-9-]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?:Total|Amount|Sum)\\s*(?:Due)?\\s*:?\\s*\\$?\\s*([0-9,]+\\.?[0-9]{0,2})",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CLIENT_NAME_PATTERN = Pattern.compile(
        "(?:Bill\\s*To|Client|Customer|Sold\\s*To)\\s*:?\\s*\\n?\\s*([A-Za-z0-9\\s&.,'-]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(?:Date|Invoice\\s*Date|Issue\\s*Date)\\s*:?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DUE_DATE_PATTERN = Pattern.compile(
        "(?:Due\\s*Date|Payment\\s*Due)\\s*:?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Parse invoice data from raw OCR text
     */
    public InvoiceExtractionResult parseInvoiceData(
        String rawText,
        Double ocrConfidence,
        String ocrEngine
    ) {
        log.debug("Parsing invoice data from text of length: {}", rawText.length());

        // Extract individual fields
        String invoiceNumber = extractInvoiceNumber(rawText);
        BigDecimal invoiceAmount = extractInvoiceAmount(rawText);
        String clientName = extractClientName(rawText);
        String clientAddress = extractClientAddress(rawText, clientName);
        LocalDate issueDate = extractDate(rawText, DATE_PATTERN);
        LocalDate dueDate = extractDate(rawText, DUE_DATE_PATTERN);

        // Calculate field-level confidence (simplified)
        double invoiceNumberConfidence = invoiceNumber.equals("UNKNOWN") ? 0.0 : 0.85;
        double amountConfidence = invoiceAmount.equals(BigDecimal.ZERO) ? 0.0 : 0.90;
        double clientNameConfidence = clientName.equals("UNKNOWN") ? 0.0 : 0.80;
        double addressConfidence = clientAddress.equals("UNKNOWN") ? 0.0 : 0.75;

        boolean hasErrors = invoiceNumber.equals("UNKNOWN") ||
                           invoiceAmount.equals(BigDecimal.ZERO) ||
                           clientName.equals("UNKNOWN");

        return InvoiceExtractionResult.builder()
            .invoiceNumber(invoiceNumber)
            .invoiceAmount(invoiceAmount)
            .clientName(clientName)
            .clientAddress(clientAddress)
            .issueDate(issueDate)
            .dueDate(dueDate)
            .currency("USD")
            .confidenceScore(ocrConfidence)
            .ocrEngine(ocrEngine)
            .extractionTimestamp(LocalDateTime.now())
            .rawText(rawText)
            .hasErrors(hasErrors)
            .invoiceNumberConfidence(invoiceNumberConfidence)
            .invoiceAmountConfidence(amountConfidence)
            .clientNameConfidence(clientNameConfidence)
            .clientAddressConfidence(addressConfidence)
            .build();
    }

    /**
     * Extract invoice number from text
     */
    private String extractInvoiceNumber(String text) {
        Matcher matcher = INVOICE_NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            String invoiceNumber = matcher.group(1).trim();
            log.debug("Extracted invoice number: {}", invoiceNumber);
            return invoiceNumber;
        }
        log.warn("Invoice number not found in text");
        return "UNKNOWN";
    }

    /**
     * Extract invoice amount from text
     */
    private BigDecimal extractInvoiceAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            String amountStr = matcher.group(1).replaceAll(",", "");
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                log.debug("Extracted invoice amount: {}", amount);
                return amount;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse amount: {}", amountStr);
            }
        }
        log.warn("Invoice amount not found in text");
        return BigDecimal.ZERO;
    }

    /**
     * Extract client name from text
     */
    private String extractClientName(String text) {
        Matcher matcher = CLIENT_NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            String clientName = matcher.group(1).trim();
            // Clean up: take only first line if multiple lines captured
            clientName = clientName.split("\\n")[0].trim();
            log.debug("Extracted client name: {}", clientName);
            return clientName;
        }
        log.warn("Client name not found in text");
        return "UNKNOWN";
    }

    /**
     * Extract client address from text
     * Looks for lines following the client name
     */
    private String extractClientAddress(String text, String clientName) {
        if (clientName.equals("UNKNOWN")) {
            return "UNKNOWN";
        }

        // Find client name in text and extract following lines
        int nameIndex = text.indexOf(clientName);
        if (nameIndex != -1) {
            String afterName = text.substring(nameIndex + clientName.length());
            String[] lines = afterName.split("\\n");

            StringBuilder address = new StringBuilder();
            for (int i = 0; i < Math.min(3, lines.length); i++) {
                String line = lines[i].trim();
                if (!line.isEmpty() &&
                    !line.toLowerCase().matches(".*(?:invoice|date|amount|total).*")) {
                    if (address.length() > 0) {
                        address.append(", ");
                    }
                    address.append(line);
                }
            }

            if (address.length() > 0) {
                String finalAddress = address.toString();
                log.debug("Extracted client address: {}", finalAddress);
                return finalAddress;
            }
        }

        log.warn("Client address not found in text");
        return "UNKNOWN";
    }

    /**
     * Extract date from text using pattern
     */
    private LocalDate extractDate(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            try {
                // Try common date formats
                DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("MM-dd-yyyy"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
                };

                for (DateTimeFormatter formatter : formatters) {
                    try {
                        LocalDate date = LocalDate.parse(dateStr, formatter);
                        log.debug("Extracted date: {}", date);
                        return date;
                    } catch (DateTimeParseException ignored) {
                        // Try next format
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse date: {}", dateStr);
            }
        }
        return null;
    }
}
```

---

## 6. Domain Service Integration

### 6.1 Extraction Service Interface

```java
// src/app/domain/service/IExtractionService.java
package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IExtractionService {

    /**
     * Extract invoice data from uploaded file
     */
    CompletableFuture<ExtractionMetadataModel> extractAndSaveInvoice(
        byte[] fileData,
        String fileName,
        String fileType
    );

    /**
     * Get extraction metadata by key
     */
    CompletableFuture<ExtractionMetadataModel> getExtractionMetadata(UUID extractionKey);

    /**
     * Retry failed extraction
     */
    CompletableFuture<ExtractionMetadataModel> retryExtraction(UUID extractionKey);
}
```

### 6.2 Extraction Service Implementation

```java
// src/app/domain/service/ExtractionService.java
package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.adapter.outbound.ocr.IOcrService;
import com.training.service.invoiceextractor.adapter.outbound.ocr.model.InvoiceExtractionResult;
import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ExtractionService implements IExtractionService {

    private final IOcrService ocrService;
    private final IInvoiceService invoiceService;
    // private final IExtractionMetadataRepositoryService extractionMetadataRepository;

    @Autowired
    public ExtractionService(
        IOcrService ocrService,
        IInvoiceService invoiceService
    ) {
        this.ocrService = ocrService;
        this.invoiceService = invoiceService;
    }

    @Override
    @Async
    public CompletableFuture<ExtractionMetadataModel> extractAndSaveInvoice(
        byte[] fileData,
        String fileName,
        String fileType
    ) {
        log.info("Starting extraction process for file: {}", fileName);

        UUID extractionKey = UUID.randomUUID();

        return ocrService.extractInvoiceData(fileData, fileName, fileType)
            .thenCompose(extractionResult -> {
                // Create invoice from extraction result
                InvoiceModel invoice = mapToInvoiceModel(extractionResult, fileName);

                // Save invoice to database
                return invoiceService.createInvoice(invoice)
                    .thenApply(savedInvoice -> {
                        // Create extraction metadata
                        return createExtractionMetadata(
                            extractionKey,
                            savedInvoice,
                            extractionResult,
                            fileName
                        );
                    });
            })
            .exceptionally(e -> {
                log.error("Extraction failed for file: {}", fileName, e);
                return createFailedExtractionMetadata(extractionKey, fileName, e);
            });
    }

    @Override
    public CompletableFuture<ExtractionMetadataModel> getExtractionMetadata(UUID extractionKey) {
        // TODO: Implement retrieval from extraction metadata repository
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ExtractionMetadataModel> retryExtraction(UUID extractionKey) {
        // TODO: Implement retry logic
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Map OCR extraction result to invoice domain model
     */
    private InvoiceModel mapToInvoiceModel(
        InvoiceExtractionResult extractionResult,
        String fileName
    ) {
        return new InvoiceModel(
            UUID.randomUUID(),
            extractionResult.getInvoiceNumber(),
            null, // vendorKey - to be implemented
            extractionResult.getIssueDate(),
            extractionResult.getDueDate(),
            extractionResult.getInvoiceAmount() != null
                ? extractionResult.getInvoiceAmount()
                : BigDecimal.ZERO,
            extractionResult.getCurrency() != null
                ? extractionResult.getCurrency()
                : "USD",
            extractionResult.getHasErrors() ? "EXTRACTION_FAILED" : "EXTRACTED",
            fileName,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    /**
     * Create extraction metadata for successful extraction
     */
    private ExtractionMetadataModel createExtractionMetadata(
        UUID extractionKey,
        InvoiceModel invoice,
        InvoiceExtractionResult extractionResult,
        String fileName
    ) {
        return new ExtractionMetadataModel(
            extractionKey,
            invoice.invoiceKey(),
            fileName,
            extractionResult.getExtractionTimestamp(),
            "COMPLETED",
            extractionResult.getConfidenceScore(),
            extractionResult.getOcrEngine(),
            extractionResult.getRawText(),
            null,
            LocalDateTime.now()
        );
    }

    /**
     * Create extraction metadata for failed extraction
     */
    private ExtractionMetadataModel createFailedExtractionMetadata(
        UUID extractionKey,
        String fileName,
        Throwable error
    ) {
        return new ExtractionMetadataModel(
            extractionKey,
            null,
            fileName,
            LocalDateTime.now(),
            "FAILED",
            0.0,
            "N/A",
            null,
            error.getMessage(),
            LocalDateTime.now()
        );
    }
}
```

---

## 7. Testing

### 7.1 Unit Tests for OCR Service

```java
// src/test/java/com/training/service/invoiceextractor/adapter/outbound/ocr/TesseractOcrServiceTest.java
package com.training.service.invoiceextractor.adapter.outbound.ocr;

import com.training.service.invoiceextractor.adapter.outbound.ocr.model.InvoiceExtractionResult;
import com.training.service.invoiceextractor.adapter.outbound.ocr.parser.InvoiceDataParser;
import net.sourceforge.tess4j.Tesseract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TesseractOcrServiceTest {

    @Mock
    private Tesseract tesseract;

    @Mock
    private InvoiceDataParser invoiceDataParser;

    private TesseractOcrService ocrService;

    @BeforeEach
    void setUp() {
        ocrService = new TesseractOcrService(tesseract, invoiceDataParser);
    }

    @Test
    void testExtractInvoiceData_Success() throws Exception {
        // Arrange
        byte[] testFile = "test data".getBytes();
        String fileName = "test-invoice.pdf";
        String fileType = "application/pdf";

        String mockOcrText = """
            Invoice #INV-12345
            Total: $1,450.75
            Bill To: ACME Corp
            123 Main St, Salt Lake City, UT
            """;

        when(tesseract.doOCR(any())).thenReturn(mockOcrText);

        InvoiceExtractionResult mockResult = InvoiceExtractionResult.builder()
            .invoiceNumber("INV-12345")
            .invoiceAmount(new BigDecimal("1450.75"))
            .clientName("ACME Corp")
            .clientAddress("123 Main St, Salt Lake City, UT")
            .build();

        when(invoiceDataParser.parseInvoiceData(anyString(), anyDouble(), anyString()))
            .thenReturn(mockResult);

        // Act
        CompletableFuture<InvoiceExtractionResult> result =
            ocrService.extractInvoiceData(testFile, fileName, fileType);

        // Assert
        assertNotNull(result);
        InvoiceExtractionResult extractionResult = result.get();
        assertEquals("INV-12345", extractionResult.getInvoiceNumber());
        assertEquals("ACME Corp", extractionResult.getClientName());

        verify(tesseract, times(1)).doOCR(any());
        verify(invoiceDataParser, times(1)).parseInvoiceData(anyString(), anyDouble(), anyString());
    }

    @Test
    void testIsServiceAvailable() {
        // Act
        CompletableFuture<Boolean> result = ocrService.isServiceAvailable();

        // Assert
        assertNotNull(result);
    }

    @Test
    void testGetProviderName() {
        // Act
        String providerName = ocrService.getProviderName();

        // Assert
        assertEquals("Tesseract OCR", providerName);
    }
}
```

### 7.2 Unit Tests for Invoice Data Parser

```java
// src/test/java/com/training/service/invoiceextractor/adapter/outbound/ocr/parser/InvoiceDataParserTest.java
package com.training.service.invoiceextractor.adapter.outbound.ocr.parser;

import com.training.service.invoiceextractor.adapter.outbound.ocr.model.InvoiceExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceDataParserTest {

    private InvoiceDataParser parser;

    @BeforeEach
    void setUp() {
        parser = new InvoiceDataParser();
    }

    @Test
    void testParseInvoiceData_ValidInvoice() {
        // Arrange
        String rawText = """
            INVOICE
            Invoice #: INV-2024-001
            Date: 12/08/2025

            Bill To:
            ACME Corporation
            123 Main Street
            Salt Lake City, UT 84101

            Total Amount Due: $2,450.75
            Due Date: 01/08/2026
            """;

        // Act
        InvoiceExtractionResult result = parser.parseInvoiceData(rawText, 0.85, "Tesseract");

        // Assert
        assertNotNull(result);
        assertEquals("INV-2024-001", result.getInvoiceNumber());
        assertEquals(new BigDecimal("2450.75"), result.getInvoiceAmount());
        assertEquals("ACME Corporation", result.getClientName());
        assertNotNull(result.getIssueDate());
        assertNotNull(result.getDueDate());
        assertFalse(result.getHasErrors());
    }

    @Test
    void testParseInvoiceData_MissingFields() {
        // Arrange
        String rawText = "Some random text without invoice data";

        // Act
        InvoiceExtractionResult result = parser.parseInvoiceData(rawText, 0.50, "Tesseract");

        // Assert
        assertNotNull(result);
        assertEquals("UNKNOWN", result.getInvoiceNumber());
        assertEquals(BigDecimal.ZERO, result.getInvoiceAmount());
        assertEquals("UNKNOWN", result.getClientName());
        assertTrue(result.getHasErrors());
    }

    @Test
    void testParseInvoiceData_VariousAmountFormats() {
        // Test different amount formats
        String[] testCases = {
            "Total: $1,450.75",
            "Amount Due: 1450.75",
            "Sum: $1450.75"
        };

        for (String testCase : testCases) {
            InvoiceExtractionResult result = parser.parseInvoiceData(testCase, 0.80, "Test");
            assertEquals(new BigDecimal("1450.75"), result.getInvoiceAmount());
        }
    }
}
```

---

## 8. Installation & Setup

### 8.1 Install Tesseract OCR

**Windows:**
```bash
# Download installer from: https://github.com/UB-Mannheim/tesseract/wiki
# Install to: C:\Program Files\Tesseract-OCR

# Add to system PATH:
setx PATH "%PATH%;C:\Program Files\Tesseract-OCR"

# Download language data:
# Included in installer (tessdata folder)
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install tesseract-ocr
sudo apt-get install libtesseract-dev

# Install language data
sudo apt-get install tesseract-ocr-eng
```

**macOS:**
```bash
brew install tesseract
```

### 8.2 Verify Installation

```bash
tesseract --version

# Should output:
# tesseract 5.x.x
```

---

## 9. Future Enhancements

### 9.1 AWS Textract Integration (Production)

```java
// Future implementation for production
@Service
@Profile("production")
public class AwsTextractOcrService implements IOcrService {

    private final AmazonTextract textractClient;

    @Override
    public CompletableFuture<InvoiceExtractionResult> extractInvoiceData(
        byte[] fileData,
        String fileName,
        String fileType
    ) {
        // Use AWS Textract AnalyzeExpense API
        // Automatically extracts invoice key-value pairs
        // Returns structured data without regex parsing
    }
}
```

### 9.2 Machine Learning Improvements

- Train custom model on invoice-specific data
- Use confidence thresholds to trigger manual review
- Implement feedback loop for continuous improvement

### 9.3 Multi-Language Support

- Add language detection
- Support invoices in Spanish, French, German, etc.
- Configure Tesseract with multiple language packs

---

## 10. Definition of Done - OCR Service

### Implementation
- [ ] IOcrService interface defined
- [ ] TesseractOcrService implementation complete
- [ ] InvoiceDataParser with regex patterns implemented
- [ ] Resilience4j CircuitBreaker and Retry configured
- [ ] Tesseract OCR installed and configured
- [ ] PDF to image conversion working

### Extraction Capabilities
- [ ] Extract invoice number (various formats)
- [ ] Extract invoice amount (with currency)
- [ ] Extract client name
- [ ] Extract client address (multi-line)
- [ ] Extract issue date and due date
- [ ] Return confidence scores for each field

### Error Handling
- [ ] Handle OCR failures gracefully
- [ ] Retry mechanism with exponential backoff
- [ ] Circuit breaker opens after failures
- [ ] Fallback method returns partial data
- [ ] All errors logged with context

### Testing
- [ ] Unit tests for TesseractOcrService (>80% coverage)
- [ ] Unit tests for InvoiceDataParser (>85% coverage)
- [ ] Integration tests with sample invoices
- [ ] Test various invoice formats (PDF, PNG, JPG)
- [ ] Test error scenarios (corrupt files, unreadable text)

### Performance
- [ ] Extraction completes within 30 seconds
- [ ] PDF conversion doesn't exceed memory limits
- [ ] Async processing doesn't block main thread

### Documentation
- [ ] Installation guide for Tesseract
- [ ] Regex patterns documented
- [ ] Confidence scoring explained
- [ ] Fallback behavior documented

---

**Document Status:** Draft
**Approval Pending:** Technical Lead
**Last Updated:** 2025-12-08
