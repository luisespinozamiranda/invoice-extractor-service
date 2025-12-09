# Implementation Plan - Invoice Extractor Service

**Project:** Invoice Extractor Service
**Document Type:** Step-by-Step Implementation Plan
**Version:** 1.1
**Date:** 2025-12-09
**Target Completion:** December 22, 2025
**Last Update:** Added Phase 5.5 - LLM Integration (Groq API)

---

## Overview

This document provides a detailed, step-by-step implementation plan for building the Invoice Extractor Service. Each phase is designed to be completed sequentially, with clear deliverables and verification steps.

**Total Estimated Time:** 68 hours (8.5 working days)
**Includes:** LLM integration with Groq API for intelligent invoice extraction

---

## Implementation Phases

```
Phase 1: Foundation & Database Setup        [6 hours]  ✅ COMPLETED
Phase 2: Error Handling Infrastructure      [3 hours]  ✅ COMPLETED
Phase 3: Domain Layer Implementation        [8 hours]  ✅ COMPLETED
Phase 4: Database Layer (Outbound Adapter)  [10 hours] ✅ COMPLETED
Phase 5: OCR Service Integration            [12 hours] ✅ COMPLETED
Phase 5.5: LLM Integration (Groq API)       [4 hours]  ✅ COMPLETED
Phase 6: REST Layer (Inbound Adapter)       [8 hours]  ✅ COMPLETED
Phase 7: Testing & Validation               [9 hours]  ✅ COMPLETED
Phase 8: Frontend Implementation            [14 hours] ⏳ PENDING
Phase 9: Integration & Deployment           [4 hours]  ⏳ PENDING
```

---

## Phase 1: Foundation & Database Setup ✅ COMPLETED

**Status:** ✅ **COMPLETED**
**Duration:** 6 hours
**Deliverables:**
- [x] Maven project structure created
- [x] Spring Boot application runs
- [x] PostgreSQL connection working
- [x] Dependencies configured (Lombok, Resilience4j, Swagger, Testing)

### Already Completed Files:
1. ✅ `pom.xml` - Maven configuration with all dependencies
2. ✅ `InvoiceExtractorServiceApplication.java` - Main application class
3. ✅ `application.properties` - Database and app configuration
4. ✅ Project documentation (TRD, TAC, OCR requirements, etc.)

### Verification:
```bash
# Run application
mvn spring-boot:run

# Expected: Application starts on port 8080
# Expected: Database connection successful
# Expected: Swagger UI available at http://localhost:8080/invoice-extractor-service/swagger-ui.html
```

---

## Phase 2: Error Handling Infrastructure ⏳ NEXT PHASE

**Status:** ⏳ **READY TO START**
**Duration:** 3 hours
**Goal:** Create centralized error handling with custom exceptions and error codes

### 2.1 Create Error Codes Enum (30 minutes)

**File:** `src/main/java/com/training/service/invoiceextractor/utils/error/ErrorCodes.java`

**Implementation:**
```java
package com.training.service.invoiceextractor.utils.error;

import lombok.Getter;

@Getter
public enum ErrorCodes {
    // File validation errors
    INVALID_FILE_TYPE("INV-001", "Invalid file type provided. Accepted types: PDF, PNG, JPG, JPEG"),
    FILE_TOO_LARGE("INV-002", "File size exceeds maximum limit of 10 MB"),
    FILE_NOT_READABLE("INV-003", "The uploaded file could not be read or is corrupted"),

    // OCR errors
    OCR_SERVICE_UNAVAILABLE("INV-004", "OCR service is currently unavailable"),
    EXTRACTION_FAILED("INV-005", "Failed to extract invoice data from file"),
    OCR_TIMEOUT("INV-006", "OCR processing timed out after 30 seconds"),

    // Data errors
    INVOICE_NOT_FOUND("INV-007", "Invoice not found with the provided key"),
    VENDOR_NOT_FOUND("INV-008", "Vendor not found with the provided key"),
    EXTRACTION_METADATA_NOT_FOUND("INV-009", "Extraction metadata not found"),

    // Database errors
    DATABASE_ERROR("INV-010", "Database operation failed"),
    DUPLICATE_INVOICE("INV-011", "Invoice with this number already exists"),

    // Request validation errors
    INVALID_REQUEST("INV-012", "Invalid request payload"),
    MISSING_REQUIRED_FIELD("INV-013", "Required field is missing: {field}"),
    INVALID_UUID_FORMAT("INV-014", "Invalid UUID format provided"),

    // General errors
    INTERNAL_ERROR("INV-999", "An unexpected internal error occurred");

    private final String code;
    private final String message;

    ErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getFormattedMessage(Object... args) {
        return String.format(message, args);
    }
}
```

### 2.2 Create Custom Exception Class (30 minutes)

**File:** `src/main/java/com/training/service/invoiceextractor/utils/error/InvoiceExtractorServiceException.java`

**Implementation:**
```java
package com.training.service.invoiceextractor.utils.error;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class InvoiceExtractorServiceException extends RuntimeException {

    private final ErrorCodes errorCode;
    private final Map<String, Object> details;

    public InvoiceExtractorServiceException(ErrorCodes errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public InvoiceExtractorServiceException(ErrorCodes errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public InvoiceExtractorServiceException(ErrorCodes errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public InvoiceExtractorServiceException(ErrorCodes errorCode, Map<String, Object> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details != null ? details : new HashMap<>();
    }

    public InvoiceExtractorServiceException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
```

### 2.3 Create Error Response DTO (30 minutes)

**File:** `src/main/java/com/training/service/invoiceextractor/adapter/inbound/rest/v1_0/dto/ErrorResponseV1_0.java`

**Implementation:**
```java
package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseV1_0 {
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, Object> details;
}
```

### 2.4 Create Global Exception Handler (90 minutes)

**File:** `src/main/java/com/training/service/invoiceextractor/configuration/GlobalExceptionHandler.java`

**Implementation:**
```java
package com.training.service.invoiceextractor.configuration;

import com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto.ErrorResponseV1_0;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvoiceExtractorServiceException.class)
    public ResponseEntity<ErrorResponseV1_0> handleInvoiceExtractorException(
        InvoiceExtractorServiceException ex
    ) {
        log.error("InvoiceExtractorServiceException: {} - {}", ex.getErrorCode().getCode(), ex.getMessage(), ex);

        ErrorResponseV1_0 errorResponse = ErrorResponseV1_0.builder()
            .errorCode(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .details(ex.getDetails())
            .build();

        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseV1_0> handleMaxUploadSizeExceeded(
        MaxUploadSizeExceededException ex
    ) {
        log.error("File size exceeded: {}", ex.getMessage());

        ErrorResponseV1_0 errorResponse = ErrorResponseV1_0.builder()
            .errorCode(ErrorCodes.FILE_TOO_LARGE.getCode())
            .message(ErrorCodes.FILE_TOO_LARGE.getMessage())
            .timestamp(LocalDateTime.now())
            .details(new HashMap<>())
            .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseV1_0> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ErrorResponseV1_0 errorResponse = ErrorResponseV1_0.builder()
            .errorCode(ErrorCodes.INTERNAL_ERROR.getCode())
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .details(new HashMap<>())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private HttpStatus determineHttpStatus(ErrorCodes errorCode) {
        return switch (errorCode) {
            case INVALID_FILE_TYPE, FILE_TOO_LARGE, FILE_NOT_READABLE,
                 INVALID_REQUEST, MISSING_REQUIRED_FIELD, INVALID_UUID_FORMAT -> HttpStatus.BAD_REQUEST;
            case INVOICE_NOT_FOUND, VENDOR_NOT_FOUND, EXTRACTION_METADATA_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OCR_SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DUPLICATE_INVOICE -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
```

### Verification (Phase 2):
```bash
# Compile project
mvn clean compile

# Expected: No compilation errors
# Expected: All error handling classes created
# Expected: GlobalExceptionHandler registered as @RestControllerAdvice
```

---

## Phase 3: Domain Layer Implementation

**Status:** ⏹️ **PENDING**
**Duration:** 8 hours
**Goal:** Implement domain models and business logic services

### 3.1 Create Domain Models (2 hours)

#### File 1: InvoiceModel.java (30 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/model/InvoiceModel.java`

```java
package com.training.service.invoiceextractor.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceModel(
    UUID invoiceKey,
    String invoiceNumber,
    UUID vendorKey,
    LocalDate issueDate,
    LocalDate dueDate,
    BigDecimal totalAmount,
    String currency,
    String status,
    String originalFileName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

#### File 2: VendorModel.java (30 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/model/VendorModel.java`

```java
package com.training.service.invoiceextractor.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record VendorModel(
    UUID vendorKey,
    String vendorName,
    String taxId,
    String address,
    String city,
    String state,
    String zipCode,
    String country,
    String contactEmail,
    String contactPhone,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

#### File 3: ExtractionMetadataModel.java (30 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/model/ExtractionMetadataModel.java`

```java
package com.training.service.invoiceextractor.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExtractionMetadataModel(
    UUID extractionKey,
    UUID invoiceKey,
    String sourceFileName,
    LocalDateTime extractionTimestamp,
    String extractionStatus,
    Double confidenceScore,
    String ocrEngine,
    String extractionData,
    String errorMessage,
    LocalDateTime createdAt
) {}
```

#### File 4: InvoiceLineItemModel.java (30 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/model/InvoiceLineItemModel.java`

```java
package com.training.service.invoiceextractor.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceLineItemModel(
    UUID lineItemKey,
    UUID invoiceKey,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    String category,
    LocalDateTime createdAt
) {}
```

### 3.2 Create Domain Service Interfaces (2 hours)

#### File 5: IInvoiceService.java (45 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/service/IInvoiceService.java`

```java
package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.domain.model.InvoiceModel;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IInvoiceService {
    CompletableFuture<InvoiceModel> getInvoiceByKey(UUID invoiceKey);
    CompletableFuture<List<InvoiceModel>> getAllInvoices();
    CompletableFuture<InvoiceModel> createInvoice(InvoiceModel invoice);
    CompletableFuture<InvoiceModel> updateInvoice(UUID invoiceKey, InvoiceModel invoice);
    CompletableFuture<Void> deleteInvoice(UUID invoiceKey);
}
```

#### File 6: IVendorService.java (45 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/service/IVendorService.java`

```java
package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.domain.model.VendorModel;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IVendorService {
    CompletableFuture<VendorModel> getVendorByKey(UUID vendorKey);
    CompletableFuture<List<VendorModel>> getAllVendors();
    CompletableFuture<VendorModel> createVendor(VendorModel vendor);
    CompletableFuture<VendorModel> updateVendor(UUID vendorKey, VendorModel vendor);
}
```

#### File 7: IExtractionService.java (30 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/service/IExtractionService.java`

```java
package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.domain.model.ExtractionMetadataModel;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IExtractionService {
    CompletableFuture<ExtractionMetadataModel> extractAndSaveInvoice(
        byte[] fileData,
        String fileName,
        String fileType
    );

    CompletableFuture<ExtractionMetadataModel> getExtractionMetadata(UUID extractionKey);

    CompletableFuture<ExtractionMetadataModel> retryExtraction(UUID extractionKey);
}
```

### 3.3 Implement Domain Services (4 hours)

#### File 8: InvoiceService.java (90 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/service/InvoiceService.java`

**Note:** Implementation requires repository service interface (created in Phase 4)
**Placeholder for now - will be implemented after Phase 4**

#### File 9: VendorService.java (60 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/service/VendorService.java`

**Placeholder for now - will be implemented after Phase 4**

#### File 10: ExtractionService.java (90 minutes)
**Location:** `src/main/java/com/training/service/invoiceextractor/domain/service/ExtractionService.java`

**Placeholder for now - will be implemented after Phase 5 (OCR integration)**

### Verification (Phase 3):
```bash
mvn clean compile

# Expected: All domain models compile
# Expected: All service interfaces compile
# Expected: No implementation errors
```

---

## Phase 4: Database Layer (Outbound Adapter)

**Status:** ⏹️ **PENDING**
**Duration:** 10 hours
**Goal:** Implement JPA entities, repositories, and repository services

### 4.1 Create Database Schema Script (1 hour)

**File:** `src/main/resources/db/schema.sql`

```sql
-- Create schema
CREATE SCHEMA IF NOT EXISTS invoicedata;

-- Create vendor table
CREATE TABLE IF NOT EXISTS invoicedata.tb_vendor (
    id BIGSERIAL PRIMARY KEY,
    vendor_key UUID NOT NULL UNIQUE,
    vendor_name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(50),
    zip_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'USA',
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create invoice table
CREATE TABLE IF NOT EXISTS invoicedata.tb_invoice (
    id BIGSERIAL PRIMARY KEY,
    invoice_key UUID NOT NULL UNIQUE,
    invoice_number VARCHAR(100) NOT NULL,
    vendor_key UUID,
    issue_date DATE,
    due_date DATE,
    total_amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vendor FOREIGN KEY (vendor_key)
        REFERENCES invoicedata.tb_vendor(vendor_key)
);

-- Create line item table
CREATE TABLE IF NOT EXISTS invoicedata.tb_invoice_line_item (
    id BIGSERIAL PRIMARY KEY,
    line_item_key UUID NOT NULL UNIQUE,
    invoice_key UUID NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    total_price DECIMAL(15, 2) NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice FOREIGN KEY (invoice_key)
        REFERENCES invoicedata.tb_invoice(invoice_key) ON DELETE CASCADE
);

-- Create extraction metadata table
CREATE TABLE IF NOT EXISTS invoicedata.tb_extraction_metadata (
    id BIGSERIAL PRIMARY KEY,
    extraction_key UUID NOT NULL UNIQUE,
    invoice_key UUID,
    source_file_name VARCHAR(255) NOT NULL,
    extraction_timestamp TIMESTAMP NOT NULL,
    extraction_status VARCHAR(50) NOT NULL,
    confidence_score DECIMAL(3, 2),
    ocr_engine VARCHAR(100),
    extraction_data JSONB,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_extraction_invoice FOREIGN KEY (invoice_key)
        REFERENCES invoicedata.tb_invoice(invoice_key) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_vendor_key ON invoicedata.tb_vendor(vendor_key);
CREATE INDEX idx_vendor_name ON invoicedata.tb_vendor(vendor_name);

CREATE INDEX idx_invoice_key ON invoicedata.tb_invoice(invoice_key);
CREATE INDEX idx_invoice_number ON invoicedata.tb_invoice(invoice_number);
CREATE INDEX idx_invoice_created_at ON invoicedata.tb_invoice(created_at DESC);

CREATE INDEX idx_line_item_invoice_key ON invoicedata.tb_invoice_line_item(invoice_key);

CREATE INDEX idx_extraction_key ON invoicedata.tb_extraction_metadata(extraction_key);
CREATE INDEX idx_extraction_invoice_key ON invoicedata.tb_extraction_metadata(invoice_key);
CREATE INDEX idx_extraction_status ON invoicedata.tb_extraction_metadata(extraction_status);
```

**Execute manually in PostgreSQL:**
```bash
# Connect to PostgreSQL (Render.com)
psql -h dpg-d4pfk8khg0os73ar3c70-a.virginia-postgres.render.com -U lespinoza -d postgresql_invoice_service

# Run schema script
\i src/main/resources/db/schema.sql
```

### 4.2 Create JPA Entities (3 hours)

**Files to create:** (Implementation details in technical-acceptance-criteria.md)
1. `Invoice.java` (45 min)
2. `Vendor.java` (45 min)
3. `InvoiceLineItem.java` (45 min)
4. `ExtractionMetadata.java` (45 min)

### 4.3 Create JPA Repositories (1 hour)

**Files to create:**
1. `InvoiceRepository.java` (15 min)
2. `VendorRepository.java` (15 min)
3. `InvoiceLineItemRepository.java` (15 min)
4. `ExtractionMetadataRepository.java` (15 min)

### 4.4 Create Repository Service Interfaces (1 hour)

**Files to create:**
1. `IInvoiceRepositoryService.java` (15 min)
2. `IVendorRepositoryService.java` (15 min)
3. `IInvoiceLineItemRepositoryService.java` (15 min)
4. `IExtractionMetadataRepositoryService.java` (15 min)

### 4.5 Implement Repository Services (3 hours)

**Files to create:**
1. `InvoiceRepositoryService.java` (45 min)
2. `VendorRepositoryService.java` (45 min)
3. `InvoiceLineItemRepositoryService.java` (45 min)
4. `ExtractionMetadataRepositoryService.java` (45 min)

### 4.6 Create Entity Mappers (1 hour)

**Files to create:**
1. `InvoiceMapper.java` (15 min)
2. `VendorMapper.java` (15 min)
3. `InvoiceLineItemMapper.java` (15 min)
4. `ExtractionMetadataMapper.java` (15 min)

### Verification (Phase 4):
```bash
# Run application
mvn spring-boot:run

# Expected: JPA entities mapped correctly
# Expected: Database tables accessible
# Expected: Repository services autowired successfully
```

---

## Phase 5: OCR Service Integration

**Status:** ⏹️ **PENDING**
**Duration:** 12 hours
**Goal:** Integrate Tesseract OCR for invoice data extraction

### 5.1 Install Tesseract OCR (1 hour)

**Windows:**
```bash
# Download installer from: https://github.com/UB-Mannheim/tesseract/wiki
# Install to: C:\Program Files\Tesseract-OCR
# Add to PATH

# Update application.properties
ocr.tesseract.data-path=C:/Program Files/Tesseract-OCR/tessdata
```

**Verify installation:**
```bash
tesseract --version
# Expected: tesseract 5.x.x
```

### 5.2 Add OCR Dependencies (30 minutes)

Update `pom.xml`:
```xml
<!-- Tesseract OCR -->
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.9.0</version>
</dependency>

<!-- Apache PDFBox -->
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

### 5.3 Create OCR Models (1 hour)

**Files to create:**
1. `OcrResult.java` (30 min)
2. `InvoiceExtractionResult.java` (30 min)

### 5.4 Create OCR Service Interface (30 minutes)

**File:** `IOcrService.java`

### 5.5 Implement Tesseract Configuration (1 hour)

**File:** `TesseractConfiguration.java`

### 5.6 Implement Invoice Data Parser (3 hours)

**File:** `InvoiceDataParser.java`
- Regex patterns for invoice number (45 min)
- Regex patterns for amount (45 min)
- Regex patterns for client name (45 min)
- Address parsing logic (45 min)

### 5.7 Implement Tesseract OCR Service (4 hours)

**File:** `TesseractOcrService.java`
- PDF to image conversion (60 min)
- OCR execution (60 min)
- Result processing (60 min)
- Error handling and retry logic (60 min)

### 5.8 Complete Extraction Service Implementation (1 hour)

**File:** `ExtractionService.java` (from Phase 3)
- Integrate OCR service calls
- Map extraction results to domain models
- Orchestrate save operations

### Verification (Phase 5):
```bash
# Create test invoice PDF
# Place in: src/test/resources/test-invoice.pdf

# Run unit tests
mvn test -Dtest=TesseractOcrServiceTest

# Expected: OCR extracts text from PDF
# Expected: Parser extracts invoice fields
# Expected: All tests pass
```

---

## Phase 5.5: LLM Integration (Groq API) ✅ COMPLETED

**Status:** ✅ **COMPLETED**
**Duration:** 4 hours
**Goal:** Integrate Groq LLM for intelligent invoice data extraction with regex fallback

### 5.5.1 Add OkHttp Dependency (15 minutes)

**File:** `pom.xml`

Added HTTP client for API calls:
```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

### 5.5.2 Create InvoiceData DTO with Optional Pattern (45 minutes)

**File:** `InvoiceData.java`

Created DTO using Optional<T> for null-safe field handling:
- Optional<String> invoiceNumber
- Optional<BigDecimal> amount
- Optional<String> clientName
- Optional<String> clientAddress
- Static factory method with null filtering
- isValid() method for data validation

### 5.5.3 Create ILlmExtractionService Port Interface (30 minutes)

**File:** `ILlmExtractionService.java`

Provider-agnostic interface:
```java
public interface ILlmExtractionService {
    CompletableFuture<InvoiceData> extractInvoiceData(String ocrText);
    boolean isAvailable();
    String getProviderName();
}
```

### 5.5.4 Implement GroqLlmService Adapter (2 hours)

**File:** `GroqLlmService.java`

Key features implemented:
- HTTP POST to Groq API endpoint
- Llama 3.1 70B model configuration
- JSON response format enforcement
- Low temperature (0.1) for factual extraction
- Structured prompt engineering
- Null-safe JSON parsing
- Returns empty Optionals for missing fields

### 5.5.5 Create LLM Configuration Class (30 minutes)

**File:** `LlmConfiguration.java`

Configuration with validation:
- @PostConstruct validation
- Logs LLM availability on startup
- Dependency injection setup

### 5.5.6 Update ExtractionService with LLM-First Strategy (1 hour)

**File:** `ExtractionService.java`

Implemented dual extraction strategy:
1. Try LLM extraction first (Groq API)
2. If LLM fails or disabled → Fallback to regex
3. Unwrap Optional<T> values with .orElse() defaults

### 5.5.7 Configure Environment Variables (15 minutes)

Updated configuration files:
- `application.properties` - Production config with env vars
- `application-local.properties` - Local config with API key
- `.env` - Docker environment variables

### Verification (Phase 5.5):
```bash
# Build project
mvn clean compile

# Check logs for LLM initialization
# Expected: "✓ LLM extraction service is enabled and available: Groq (Llama 3.1 70B)"

# All tests pass
mvn test
```

**Completed Features:**
- ✅ Hexagonal architecture with port/adapter pattern
- ✅ Provider-agnostic design (easy to swap LLM providers)
- ✅ Optional<T> pattern for explicit null handling
- ✅ Automatic fallback to regex if LLM fails
- ✅ Free Groq API integration
- ✅ Fast extraction (2-5 seconds via Groq)
- ✅ High accuracy with context understanding

---

## Phase 6: REST Layer (Inbound Adapter)

**Status:** ⏹️ **PENDING**
**Duration:** 8 hours
**Goal:** Implement REST controllers and DTOs

### 6.1 Create REST DTOs (2 hours)

**Files to create:**
1. `InvoiceV1_0.java` (30 min)
2. `VendorV1_0.java` (30 min)
3. `ExtractionRequestV1_0.java` (30 min)
4. `ExtractionResponseV1_0.java` (30 min)

### 6.2 Create DTO Mappers (1 hour)

**Files to create:**
1. `IInvoiceMapperV1_0.java` (15 min)
2. `IVendorMapperV1_0.java` (15 min)
3. `IExtractionMapperV1_0.java` (15 min)
4. Mapper implementations (15 min)

### 6.3 Create Controller Service Interfaces (1 hour)

**Files to create:**
1. `IInvoiceControllerServiceV1_0.java` (20 min)
2. `IVendorControllerServiceV1_0.java` (20 min)
3. `IExtractionControllerServiceV1_0.java` (20 min)

### 6.4 Implement Controller Services (2 hours)

**Files to create:**
1. `InvoiceControllerServiceV1_0.java` (40 min)
2. `VendorControllerServiceV1_0.java` (40 min)
3. `ExtractionControllerServiceV1_0.java` (40 min)

### 6.5 Create REST Controllers (2 hours)

**Files to create:**
1. `InvoiceControllerV1_0.java` (40 min)
2. `VendorControllerV1_0.java` (40 min)
3. `ExtractionControllerV1_0.java` (40 min)

### Verification (Phase 6):
```bash
# Run application
mvn spring-boot:run

# Access Swagger UI
http://localhost:8080/invoice-extractor-service/swagger-ui.html

# Expected: All endpoints visible in Swagger
# Expected: Test upload endpoint with sample PDF
# Expected: Get invoices endpoint returns list
```

---

## Phase 7: Testing & Validation

**Status:** ⏹️ **PENDING**
**Duration:** 9 hours
**Goal:** Comprehensive testing of all layers

### 7.1 Unit Tests - Domain Services (2 hours)

**Files to create:**
1. `InvoiceServiceTest.java` (40 min)
2. `VendorServiceTest.java` (40 min)
3. `ExtractionServiceTest.java` (40 min)

### 7.2 Unit Tests - Repository Services (2 hours)

**Files to create:**
1. `InvoiceRepositoryServiceTest.java` (40 min)
2. `VendorRepositoryServiceTest.java` (40 min)
3. `ExtractionMetadataRepositoryServiceTest.java` (40 min)

### 7.3 Unit Tests - OCR Service (2 hours)

**Files to create:**
1. `TesseractOcrServiceTest.java` (60 min)
2. `InvoiceDataParserTest.java` (60 min)

### 7.4 Integration Tests - REST Endpoints (2 hours)

**Files to create:**
1. `InvoiceControllerIntegrationTest.java` (40 min)
2. `ExtractionControllerIntegrationTest.java` (40 min)
3. Test data setup (40 min)

### 7.5 End-to-End Testing (1 hour)

- Manual testing of complete flow
- Test with 5 different invoice formats
- Verify data persistence

### Verification (Phase 7):
```bash
# Run all tests
mvn test

# Expected: > 80% code coverage
# Expected: All unit tests pass
# Expected: All integration tests pass

# Generate coverage report
mvn jacoco:report
```

---

## Phase 8: Frontend Implementation

**Status:** ⏹️ **PENDING**
**Duration:** 14 hours
**Goal:** Build Angular frontend application

### 8.1 Setup Angular Project (1 hour)

```bash
# Create new Angular project
cd C:\Users\lespinoza\Documents\Projects\Training
ng new invoice-extractor-frontend --routing --style=scss

cd invoice-extractor-frontend

# Install dependencies
npm install @angular/material @angular/cdk
npm install ngx-toastr
npm install date-fns
```

### 8.2 Configure Environment (30 minutes)

**Files to create:**
1. `environment.ts`
2. `environment.prod.ts`

### 8.3 Create Core Services (3 hours)

**Files to create:**
1. `invoice.service.ts` (60 min)
2. `file-upload.service.ts` (45 min)
3. `http-error.interceptor.ts` (45 min)
4. Models (30 min)

### 8.4 Create Shared Components (2 hours)

**Files to create:**
1. `loading-spinner.component.ts` (30 min)
2. `currency-format.pipe.ts` (30 min)
3. `date-format.pipe.ts` (30 min)
4. Styles (30 min)

### 8.5 Implement Invoice Upload Component (3 hours)

**Files to create:**
1. `invoice-upload.component.ts` (90 min)
2. `invoice-upload.component.html` (60 min)
3. `invoice-upload.component.scss` (30 min)

### 8.6 Implement Invoice List Component (3 hours)

**Files to create:**
1. `invoice-list.component.ts` (90 min)
2. `invoice-list.component.html` (60 min)
3. `invoice-list.component.scss` (30 min)

### 8.7 Configure Routing & App Module (1 hour)

**Files to update:**
1. `app.routes.ts`
2. `app.component.html`
3. `app.config.ts`

### 8.8 Testing & Styling (30 minutes)

- Test all components
- Verify responsive design
- Apply Material theme

### Verification (Phase 8):
```bash
# Start frontend
ng serve

# Access application
http://localhost:4200

# Expected: Upload interface visible
# Expected: File upload works
# Expected: Invoice list displays data
# Expected: Responsive on mobile
```

---

## Phase 9: Integration & Deployment

**Status:** ⏹️ **PENDING**
**Duration:** 4 hours
**Goal:** End-to-end integration and deployment preparation

### 9.1 Backend-Frontend Integration (1 hour)

- Configure CORS in Spring Boot
- Test API calls from Angular
- Verify data flow

### 9.2 Create Docker Configuration (1 hour)

**Files to create:**
1. `Dockerfile` (backend)
2. `docker-compose.yml`

### 9.3 Documentation & README (1 hour)

**Files to create:**
1. `README.md` (root)
2. `SETUP.md`
3. `API_DOCUMENTATION.md`

### 9.4 Final Testing & Demo (1 hour)

- Complete end-to-end test
- Prepare demo invoice samples
- Document known issues

### Verification (Phase 9):
```bash
# Build backend
mvn clean package

# Build frontend
ng build --configuration production

# Run Docker containers
docker-compose up

# Expected: Both services running
# Expected: Complete flow works end-to-end
```

---

## Daily Implementation Schedule

### Week 1 (Dec 9-13)

**Monday:** Phase 2 (Error Handling) ✅ 3 hours
**Tuesday:** Phase 3 (Domain Layer) ✅ 8 hours
**Wednesday:** Phase 4 Part 1 (Database Schema + Entities) ✅ 5 hours
**Thursday:** Phase 4 Part 2 (Repositories + Services) ✅ 5 hours
**Friday:** Phase 5 Part 1 (OCR Setup + Models) ✅ 5 hours

### Week 2 (Dec 16-20)

**Monday:** Phase 5 Part 2 (OCR Implementation) ✅ 7 hours
**Tuesday:** Phase 6 (REST Layer) ✅ 8 hours
**Wednesday:** Phase 7 (Testing) ✅ 9 hours
**Thursday:** Phase 8 Part 1 (Frontend Setup + Services) ✅ 7 hours
**Friday:** Phase 8 Part 2 (Frontend Components) ✅ 7 hours

### Week 3 (Dec 21-22)

**Saturday:** Phase 9 (Integration & Deployment) ✅ 4 hours
**Sunday:** Buffer / Final polish

---

## Progress Tracking

### Completion Checklist

#### Phase 1: Foundation ✅
- [x] Project structure created
- [x] Dependencies configured
- [x] Database connection working
- [x] Documentation complete

#### Phase 2: Error Handling ⏳
- [ ] ErrorCodes enum created
- [ ] Custom exception created
- [ ] Error response DTO created
- [ ] Global exception handler implemented

#### Phase 3: Domain Layer ⏹️
- [ ] 4 domain models created
- [ ] 3 service interfaces created
- [ ] Service implementations (pending Phase 4)

#### Phase 4: Database Layer ⏹️
- [ ] Database schema created
- [ ] 4 JPA entities created
- [ ] 4 JPA repositories created
- [ ] 4 repository services created
- [ ] 4 entity mappers created

#### Phase 5: OCR Service ⏹️
- [ ] Tesseract installed
- [ ] OCR dependencies added
- [ ] IOcrService interface created
- [ ] InvoiceDataParser implemented
- [ ] TesseractOcrService implemented
- [ ] ExtractionService completed

#### Phase 6: REST Layer ⏹️
- [ ] 4 REST DTOs created
- [ ] 3 DTO mappers created
- [ ] 3 controller services created
- [ ] 3 REST controllers created
- [ ] Swagger UI accessible

#### Phase 7: Testing ⏹️
- [ ] Domain service tests (3 files)
- [ ] Repository service tests (3 files)
- [ ] OCR service tests (2 files)
- [ ] Integration tests (2 files)
- [ ] > 80% code coverage achieved

#### Phase 8: Frontend ⏹️
- [ ] Angular project setup
- [ ] Core services (3 files)
- [ ] Shared components (3 files)
- [ ] Upload component complete
- [ ] List component complete
- [ ] Routing configured

#### Phase 9: Integration ⏹️
- [ ] Backend-frontend integration
- [ ] Docker configuration
- [ ] Documentation complete
- [ ] Final testing passed

---

## Next Steps

**IMMEDIATE ACTION:** Start Phase 2 - Error Handling Infrastructure

**Command to execute:**
```bash
# 1. Create error package structure
mkdir -p src/main/java/com/training/service/invoiceextractor/utils/error

# 2. Begin with ErrorCodes.java
# (See Phase 2.1 implementation above)
```

**Ready to proceed?** Reply "start phase 2" and I'll begin implementing the error handling infrastructure.

---

**Document Version:** 1.0
**Last Updated:** 2025-12-08
**Total Estimated Time:** 64 hours
**Deadline:** December 22, 2025
