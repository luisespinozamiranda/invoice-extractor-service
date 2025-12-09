# Architecture Diagram - Invoice Extractor Service

**Project:** Invoice Extractor Service
**Document Type:** Architecture & Interaction Diagrams
**Version:** 1.1
**Date:** 2025-12-09

---

## 1. High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          USER BROWSER                                    │
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │                    Angular Frontend                             │    │
│  │                    (Port: 4200)                                 │    │
│  │                                                                  │    │
│  │  ┌──────────────────┐         ┌──────────────────┐            │    │
│  │  │ Invoice Upload   │         │ Invoice List     │            │    │
│  │  │ Component        │         │ Component        │            │    │
│  │  └──────────────────┘         └──────────────────┘            │    │
│  │           │                             │                       │    │
│  │           └─────────────┬───────────────┘                       │    │
│  │                         │                                        │    │
│  │                  ┌──────▼───────┐                               │    │
│  │                  │ Invoice      │                               │    │
│  │                  │ Service      │                               │    │
│  │                  └──────────────┘                               │    │
│  └────────────────────────│──────────────────────────────────────┘    │
└────────────────────────────│───────────────────────────────────────────┘
                             │
                             │ HTTP/REST API
                             │ (JSON)
                             │
┌────────────────────────────▼───────────────────────────────────────────┐
│                    Spring Boot Backend                                  │
│              (Port: 8080, Context: /invoice-extractor-service)          │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                    INBOUND ADAPTERS (REST)                       │  │
│  │                                                                   │  │
│  │  ┌────────────────────┐      ┌────────────────────┐            │  │
│  │  │ Invoice            │      │ Extraction         │            │  │
│  │  │ Controller V1.0    │      │ Controller V1.0    │            │  │
│  │  └─────────┬──────────┘      └─────────┬──────────┘            │  │
│  │            │                            │                        │  │
│  │            │                            │                        │  │
│  │  ┌─────────▼──────────┐      ┌─────────▼──────────┐            │  │
│  │  │ Invoice            │      │ Extraction         │            │  │
│  │  │ Controller Service │      │ Controller Service │            │  │
│  │  └─────────┬──────────┘      └─────────┬──────────┘            │  │
│  └────────────│──────────────────────────┬│──────────────────────┘  │
│               │                           ││                          │
│  ┌────────────▼───────────────────────────▼▼────────────────────┐   │
│  │                      DOMAIN LAYER                             │   │
│  │                                                                │   │
│  │  ┌─────────────────┐        ┌──────────────────┐            │   │
│  │  │ Invoice         │        │ Extraction       │            │   │
│  │  │ Service         │◄───────┤ Service          │            │   │
│  │  └────────┬────────┘        └────────┬─────────┘            │   │
│  │           │                           │                       │   │
│  │  ┌────────▼──────────────────────────▼─────┐                │   │
│  │  │      Domain Models (Records)             │                │   │
│  │  │  - InvoiceModel                          │                │   │
│  │  │  - ExtractionMetadataModel               │                │   │
│  │  └──────────────────────────────────────────┘                │   │
│  └────────────┬───────────────────────────┬──────────────────────┘  │
│               │                           │                          │
│  ┌─────────┬──────────────┬──────────────────────┬──────────────┐  │
│           │              │                      │              │  │
│  ┌────────▼───────┐ ┌───▼──────────┐ ┌────────▼─────────┐ ┌─▼────┐│
│  │  DATABASE      │ │  LLM ADAPTER │ │  OCR ADAPTER     │ │ ...  ││
│  │  ADAPTER       │ │  (Outbound)  │ │  (Outbound)      │ │      ││
│  │  (Outbound)    │ │              │ │                  │ │      ││
│  │ ┌────────────┐ │ │ ┌──────────┐ │ │ ┌──────────────┐ │ │      ││
│  │ │Repository  │ │ │ │ILlmExtract│ │ │ │IOcrService   │ │ │      ││
│  │ │Service     │ │ │ │Service    │ │ │ │(Port)        │ │ │      ││
│  │ └─────┬──────┘ │ │ │(Port)     │ │ │ └──────┬───────┘ │ │      ││
│  │       │        │ │ └─────┬─────┘ │ │        │         │ │      ││
│  │ ┌─────▼──────┐ │ │ ┌─────▼─────┐ │ │ ┌──────▼───────┐ │ │      ││
│  │ │JPA Repo    │ │ │ │GroqLlm    │ │ │ │Tesseract     │ │ │      ││
│  │ │- Invoice   │ │ │ │Service    │ │ │ │OcrService    │ │ │      ││
│  │ │- Vendor    │ │ │ │(Adapter)  │ │ │ │              │ │ │      ││
│  │ └─────┬──────┘ │ │ └─────┬─────┘ │ │ │┌────────────┐│ │ │      ││
│  │       │        │ │       │       │ │ ││Invoice Data││ │ │      ││
│  │ ┌─────▼──────┐ │ │       │       │ │ ││Parser      ││ │ │      ││
│  │ │JPA Entity  │ │ │       │       │ │ │└────────────┘│ │ │      ││
│  │ │- Invoice   │ │ │       │       │ │ │┌────────────┐│ │ │      ││
│  │ │- Vendor    │ │ │       │       │ │ ││Tesseract   ││ │ │      ││
│  │ │- Extract   │ │ │       │       │ │ ││OCR Engine  ││ │ │      ││
│  │ │  Metadata  │ │ │       │       │ │ │└────────────┘│ │ │      ││
│  │ └────────────┘ │ │       │       │ │ └──────────────┘ │ │      ││
│  └────────┬───────┘ └───────┼───────┘ └────────┬─────────┘ └──────┘│
│           │                 │                  │                    │
└───────────┼─────────────────┼──────────────────┼────────────────────┘
            │                 │                  │
   ┌────────▼────────┐ ┌──────▼──────┐  ┌───────▼──────┐
   │  PostgreSQL     │ │  Groq API   │  │  Tesseract   │
   │  Database       │ │  (Llama 3.1)│  │  OCR Engine  │
   │  (Render.com)   │ │  Free Tier  │  │              │
   └─────────────────┘ └─────────────┘  └──────────────┘
```

---

## 2. Request Flow Diagrams

### 2.1 Upload Invoice Flow

```
┌─────────┐         ┌─────────┐         ┌──────────┐         ┌─────────┐         ┌──────────┐
│ User    │         │ Angular │         │ Spring   │         │ OCR     │         │PostgreSQL│
│ Browser │         │Frontend │         │ Backend  │         │ Service │         │ Database │
└────┬────┘         └────┬────┘         └────┬─────┘         └────┬────┘         └────┬─────┘
     │                   │                    │                    │                    │
     │ 1. Select File    │                    │                    │                    │
     │──────────────────>│                    │                    │                    │
     │                   │                    │                    │                    │
     │                   │ 2. Validate File   │                    │                    │
     │                   │   (type, size)     │                    │                    │
     │                   │                    │                    │                    │
     │ 3. Click Upload   │                    │                    │                    │
     │──────────────────>│                    │                    │                    │
     │                   │                    │                    │                    │
     │                   │ 4. POST /api/v1.0/invoices/upload      │                    │
     │                   │    (multipart/form-data)               │                    │
     │                   │───────────────────>│                    │                    │
     │                   │                    │                    │                    │
     │                   │                    │ 5. Controller receives request          │
     │                   │                    │    @PostMapping("/upload")              │
     │                   │                    │                    │                    │
     │                   │                    │ 6. ControllerService                    │
     │                   │                    │    converts DTO                         │
     │                   │                    │                    │                    │
     │                   │                    │ 7. ExtractionService                    │
     │                   │                    │    .extractAndSaveInvoice()             │
     │                   │                    │                    │                    │
     │                   │                    │ 8. Call OCR Service│                    │
     │                   │                    │───────────────────>│                    │
     │                   │                    │                    │                    │
     │                   │                    │                    │ 9. Convert PDF     │
     │                   │                    │                    │    to Image        │
     │                   │                    │                    │                    │
     │                   │                    │                    │ 10. Tesseract.doOCR│
     │                   │                    │                    │     (extract text) │
     │                   │                    │                    │                    │
     │                   │                    │ 11. Return OCR Text│                    │
     │                   │                    │<───────────────────│                    │
     │                   │                    │  OcrResult         │                    │
     │                   │                    │                    │                    │
     │                   │                    │ 12. Call LLM Service (Groq API)         │
     │                   │                    │    extractInvoiceData(ocrText)          │
     │                   │                    │                    │                    │
     │                   │                    │                    │ 13. Groq API Call  │
     │                   │                    │                    │    Llama 3.1 70B   │
     │                   │                    │                    │    (JSON Mode)     │
     │                   │                    │                    │                    │
     │                   │                    │ 14. Return InvoiceData                  │
     │                   │                    │    (with Optional fields)               │
     │                   │                    │    OR fallback to regex                 │
     │                   │                    │                    │                    │
     │                   │                    │ 15. InvoiceService.createInvoice()      │
     │                   │                    │                    │                    │
     │                   │                    │ 14. RepositoryService                   │
     │                   │                    │     .save()        │                    │
     │                   │                    │────────────────────────────────────────>│
     │                   │                    │                    │                    │
     │                   │                    │                    │    15. INSERT INTO │
     │                   │                    │                    │        tb_invoice  │
     │                   │                    │                    │                    │
     │                   │                    │ 16. Saved Invoice  │                    │
     │                   │                    │<────────────────────────────────────────│
     │                   │                    │                    │                    │
     │                   │ 17. Return Response│                    │                    │
     │                   │    200 OK          │                    │                    │
     │                   │<───────────────────│                    │                    │
     │                   │  {extractionKey,   │                    │                    │
     │                   │   fileName,status} │                    │                    │
     │                   │                    │                    │                    │
     │ 18. Show Success  │                    │                    │                    │
     │    Notification   │                    │                    │                    │
     │<──────────────────│                    │                    │                    │
     │                   │                    │                    │                    │
     │                   │ 19. Refresh List   │                    │                    │
     │                   │    (auto-trigger)  │                    │                    │
     │                   │                    │                    │                    │
```

**Timeline:** ~30 seconds total (steps 8-12 take 20-25 seconds)

---

### 2.2 Get Invoice List Flow

```
┌─────────┐         ┌─────────┐         ┌──────────┐         ┌──────────┐
│ User    │         │ Angular │         │ Spring   │         │PostgreSQL│
│ Browser │         │Frontend │         │ Backend  │         │ Database │
└────┬────┘         └────┬────┘         └────┬─────┘         └────┬─────┘
     │                   │                    │                    │
     │ 1. Navigate to    │                    │                    │
     │    /invoices      │                    │                    │
     │──────────────────>│                    │                    │
     │                   │                    │                    │
     │                   │ 2. GET /api/v1.0/invoices?             │
     │                   │    page=0&size=20&sort=createdAt,desc  │
     │                   │───────────────────>│                    │
     │                   │                    │                    │
     │                   │                    │ 3. Controller      │
     │                   │                    │    @GetMapping()   │
     │                   │                    │                    │
     │                   │                    │ 4. InvoiceService  │
     │                   │                    │    .getAllInvoices()│
     │                   │                    │                    │
     │                   │                    │ 5. RepositoryService│
     │                   │                    │    .findAll()      │
     │                   │                    │───────────────────>│
     │                   │                    │                    │
     │                   │                    │                    │ 6. SELECT *
     │                   │                    │                    │    FROM tb_invoice
     │                   │                    │                    │    ORDER BY created_at
     │                   │                    │                    │    LIMIT 20 OFFSET 0
     │                   │                    │                    │
     │                   │                    │ 7. List<Invoice>   │
     │                   │                    │<───────────────────│
     │                   │                    │                    │
     │                   │                    │ 8. Map Entities    │
     │                   │                    │    to Models       │
     │                   │                    │                    │
     │                   │ 9. Return Response │                    │
     │                   │    200 OK          │                    │
     │                   │<───────────────────│                    │
     │                   │  {content:[...],   │                    │
     │                   │   page:0,size:20}  │                    │
     │                   │                    │                    │
     │ 10. Display Table │                    │                    │
     │<──────────────────│                    │                    │
     │   with invoices   │                    │                    │
```

**Timeline:** < 1 second

---

## 3. Component Interaction Details

### 3.1 Frontend → Backend Communication

```
┌──────────────────────────────────────────────────────────────┐
│                    HTTP Communication                         │
└──────────────────────────────────────────────────────────────┘

Request:
--------
POST http://localhost:8080/invoice-extractor-service/api/v1.0/invoices/upload
Content-Type: multipart/form-data
Headers:
  - Accept: application/json

Body:
  file: [binary data]

Response:
---------
Status: 200 OK
Content-Type: application/json

{
  "extractionKey": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "fileName": "invoice_2024_001.pdf",
  "status": "PROCESSING",
  "uploadedAt": "2025-12-08T10:30:00Z"
}
```

### 3.2 Backend → OCR & LLM Service Interaction

```
┌─────────────────────────────────────────────────────────────┐
│              Internal Java Method Calls                      │
└─────────────────────────────────────────────────────────────┘

1. ExtractionService (Domain)
   └─> ocrService.extractText(fileData, fileName, fileType)
       │
       ├─> TesseractOcrService (Adapter)
       │   │
       │   ├─> Convert PDF to BufferedImage (PDFBox)
       │   │   └─> PDDocument.load(pdfData)
       │   │       └─> PDFRenderer.renderImageWithDPI(0, 300)
       │   │
       │   └─> Perform OCR
       │       └─> tesseract.doOCR(bufferedImage)
       │           └─> Returns: OcrResult (raw text + confidence)
       │
       └─> Returns: CompletableFuture<OcrResult>

2. ExtractionService continues with LLM extraction
   └─> llmExtractionService.extractInvoiceData(ocrText)
       │
       ├─> GroqLlmService (Adapter)
       │   │
       │   ├─> Build extraction prompt (structured)
       │   │   └─> Instructs LLM to extract fields as JSON
       │   │
       │   ├─> Call Groq API (HTTP POST via OkHttp)
       │   │   └─> POST https://api.groq.com/openai/v1/chat/completions
       │   │       ├─> Model: llama-3.1-70b-versatile
       │   │       ├─> Temperature: 0.1 (factual)
       │   │       └─> Response Format: json_object (forced JSON)
       │   │
       │   └─> Parse JSON response
       │       └─> Extract fields: invoice_number, amount, client_name, etc.
       │           └─> Wrap in Optional<T> (null-safe)
       │               └─> Returns: InvoiceData (with Optionals)
       │
       └─> Returns: CompletableFuture<InvoiceData>

       **If LLM fails or is disabled:**
       └─> Fallback to Regex Pattern Matching
           ├─> Extract invoice number (regex)
           ├─> Extract amount (regex)
           ├─> Extract client name (regex)
           └─> Returns: InvoiceData (default values)

3. ExtractionService continues
   └─> Create InvoiceModel from InvoiceData
       └─> Unwrap Optional fields with .orElse(defaults)
           └─> invoiceRepositoryService.save(invoiceModel)
               └─> invoiceRepository.save(invoiceEntity)
                   └─> PostgreSQL INSERT
```

### 3.3 Backend → Database Interaction

```
┌─────────────────────────────────────────────────────────────┐
│              JPA/Hibernate → PostgreSQL                      │
└─────────────────────────────────────────────────────────────┘

1. Domain Service calls Repository Service
   invoiceRepositoryService.save(invoiceModel)

2. Repository Service maps Model to Entity
   Invoice entity = invoiceMapper.modelToEntity(invoiceModel)

3. Repository Service calls JPA Repository
   invoiceRepository.save(entity)

4. Hibernate generates SQL
   INSERT INTO invoicedata.tb_invoice (
       invoice_key,
       invoice_number,
       total_amount,
       client_name,
       client_address,
       original_file_name,
       status,
       created_at,
       updated_at
   ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)

5. PostgreSQL executes INSERT
   Returns generated ID

6. JPA/Hibernate returns managed entity
   Repository Service maps Entity back to Model
   Returns InvoiceModel to Domain Service
```

---

## 4. Hexagonal Architecture Layers

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HEXAGONAL ARCHITECTURE                       │
│                        (Ports & Adapters Pattern)                    │
└─────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────┐
                    │   INBOUND ADAPTERS      │
                    │   (User Interface)      │
                    │                         │
                    │  - REST Controllers     │
                    │  - Controller Services  │
                    │  - DTOs & Mappers       │
                    └───────────┬─────────────┘
                                │
                                │ Inbound Port
                                │ (Service Interface)
                                │
        ┌───────────────────────▼────────────────────────┐
        │                 DOMAIN LAYER                    │
        │            (Business Logic Core)                │
        │                                                 │
        │  ┌─────────────────────────────────────────┐  │
        │  │        Domain Services                   │  │
        │  │  - IInvoiceService                       │  │
        │  │  - InvoiceService                        │  │
        │  │  - IExtractionService                    │  │
        │  │  - ExtractionService                     │  │
        │  └─────────────────────────────────────────┘  │
        │                                                 │
        │  ┌─────────────────────────────────────────┐  │
        │  │        Domain Models (Records)           │  │
        │  │  - InvoiceModel                          │  │
        │  │  - VendorModel                           │  │
        │  │  - ExtractionMetadataModel               │  │
        │  └─────────────────────────────────────────┘  │
        │                                                 │
        └────┬─────────────────┬─────────────────┬─────────┘
             │                 │                 │
             │ Outbound Port   │ Outbound Port   │ Outbound Port
             │ (Repository)    │ (LLM Interface) │ (OCR Interface)
             │                 │                 │
  ┌──────────▼─────────┐ ┌─────▼──────────┐ ┌──▼────────────────┐
  │  OUTBOUND ADAPTER  │ │  OUTBOUND      │ │  OUTBOUND ADAPTER │
  │  (Database)        │ │  ADAPTER (LLM) │ │  (OCR Service)    │
  │                    │ │                │ │                   │
  │  - Repository      │ │  - ILlmExtract │ │  - IOcrService    │
  │    Services        │ │    Service     │ │  - TesseractOcr   │
  │  - JPA Repos       │ │  - GroqLlm     │ │    Service        │
  │  - Entities        │ │    Service     │ │                   │
  │  - Entity Mappers  │ │  - InvoiceData │ │                   │
  └─────────┬──────────┘ └───────┬────────┘ └─────────┬─────────┘
            │                    │                     │
            │                    │                     │
     ┌──────▼────────┐  ┌────────▼─────────┐  ┌───────▼────────┐
     │  PostgreSQL   │  │  Groq API        │  │  Tesseract     │
     │  Database     │  │  Llama 3.1 70B   │  │  OCR Engine    │
     └───────────────┘  └──────────────────┘  └────────────────┘
```

**Key Principles:**
- **Domain Layer** has NO dependencies on infrastructure
- **Adapters** depend on Domain (not vice versa)
- **Ports** define contracts (interfaces)
- **Dependency Inversion** - high-level modules don't depend on low-level modules

---

## 5. Technology Stack Mapping

```
┌────────────────────────────────────────────────────────────────┐
│                       TECHNOLOGY STACK                          │
└────────────────────────────────────────────────────────────────┘

Layer                    Technology              Version
────────────────────────────────────────────────────────────────
Frontend                Angular                  16+
                        TypeScript               5.0+
                        Angular Material         16+
                        RxJS                     7+
                        ngx-toastr              (latest)

Backend Framework       Spring Boot              3.1.2
                        Java                     17

REST Layer              Spring Web MVC           3.1.2
                        SpringDoc OpenAPI        2.2.0

Domain Layer            Plain Java (POJOs)       17
                        Java Records             17

LLM Integration         Groq API                 Free Tier
                        Llama 3.1 70B            Latest
                        OkHttp                   4.12.0
                        Jackson (JSON parsing)   2.15.x

OCR Integration         Tesseract OCR            5.x
                        Tess4J                   5.9.0
                        Apache PDFBox            3.0.0

Persistence             Spring Data JPA          3.1.2
                        Hibernate                6.2.x
                        PostgreSQL Driver        42.x

Database                PostgreSQL               15+
                        (Hosted on Render.com)

Resilience              Resilience4j             2.1.0
                        - CircuitBreaker
                        - Retry
                        - RateLimiter

Utilities               Lombok                   (included)
                        Apache Commons IO        2.15.1

Testing                 JUnit 5                  5.9.x
                        Mockito                  5.x
                        Spring Boot Test         3.1.2

Build Tools             Maven                    3.8+
                        Angular CLI              16+
```

---

## 6. Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     DEPLOYMENT TOPOLOGY                          │
└─────────────────────────────────────────────────────────────────┘

                        ┌───────────────┐
                        │   Internet    │
                        └───────┬───────┘
                                │
                        ┌───────▼────────┐
                        │  Load Balancer │
                        │   / Reverse    │
                        │     Proxy      │
                        └───────┬────────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
          ┌─────────▼──────────┐   ┌───────▼──────────┐
          │  Angular Frontend  │   │  Spring Boot     │
          │  (Static Files)    │   │  Backend         │
          │                    │   │  (JAR)           │
          │  Nginx / Apache    │   │                  │
          │  Port: 80/443      │   │  Port: 8080      │
          └────────────────────┘   └────────┬─────────┘
                                            │
                                            │ JDBC
                                            │
                                   ┌────────▼──────────┐
                                   │  PostgreSQL       │
                                   │  Database         │
                                   │  (Render.com)     │
                                   │  Port: 5432       │
                                   └───────────────────┘

Server Requirements:
───────────────────
Frontend Server:
  - Nginx or Apache HTTP Server
  - 1 CPU, 512 MB RAM
  - Static file hosting

Backend Server:
  - JVM 17+
  - 2 CPU, 2 GB RAM (minimum)
  - 4 GB RAM recommended (for OCR processing)
  - Tesseract OCR installed

Database Server:
  - PostgreSQL 15+
  - 2 GB storage minimum
  - Connection pooling configured
```

---

## 7. Data Flow Summary

### Upload Invoice Complete Flow:

```
1. USER UPLOADS FILE
   └─> Angular validates (client-side)
       └─> HTTP POST to backend
           └─> Spring Controller receives
               └─> Controller Service converts DTO
                   └─> Extraction Service orchestrates
                       ├─> OCR Service extracts raw text
                       │   ├─> PDF → Image conversion
                       │   ├─> Tesseract OCR
                       │   └─> Returns OcrResult (text + confidence)
                       │
                       ├─> LLM Service extracts structured data
                       │   ├─> Build extraction prompt
                       │   ├─> Call Groq API (Llama 3.1 70B)
                       │   ├─> Parse JSON response
                       │   └─> Returns InvoiceData (Optional fields)
                       │   └─> Fallback to regex if LLM fails
                       │
                       └─> Invoice Service saves
                           └─> Repository Service persists
                               └─> JPA/Hibernate INSERT
                                   └─> PostgreSQL stores
                                       └─> Returns saved entity
                                           └─> Maps to Model
                                               └─> Returns to Domain
                                                   └─> Returns to Controller
                                                       └─> JSON response
                                                           └─> Angular displays success
```

**Total Time:** ~30-35 seconds
- Frontend validation: < 100ms
- File upload: 1-2 seconds (network)
- Backend processing: 1-2 seconds
- OCR extraction: 20-25 seconds
- LLM extraction: 2-5 seconds (Groq API call)
- Database save: < 500ms
- Response to frontend: < 500ms

---

## 8. Error Handling Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    ERROR PROPAGATION                          │
└──────────────────────────────────────────────────────────────┘

Error Origin                 Error Handler              Response
─────────────────────────────────────────────────────────────────
OCR Failure                 Resilience4j Retry          → 3 retries
  (Tesseract crash)         ↓ (if all fail)               with backoff
                            CircuitBreaker              → Fallback result
                            ↓                             with error flag
                            Domain Service              → Logs error
                            ↓
                            Controller                  → 500 Internal Error
                            ↓
                            Global Exception Handler    → JSON error response
                            ↓
                            HTTP Response               → {errorCode, message}
                            ↓
                            Angular HTTP Interceptor    → Catches error
                            ↓
                            Toastr Service              → Shows notification
                            ↓
                            User sees error message     → "OCR failed, try again"

Database Failure            Spring @Transactional       → Rollback
  (Connection lost)         ↓
                            Custom Exception            → Wraps SQLException
                            ↓
                            Global Exception Handler    → 500 Internal Error
                            ↓
                            Angular displays error      → "Failed to save"

Invalid File Type           Controller Validation       → 400 Bad Request
  (user uploads .txt)       ↓
                            Angular HTTP Interceptor    → Catches 400
                            ↓
                            Toastr notification         → "Invalid file type"
```

---

## 9. Async Processing Flow

```
┌──────────────────────────────────────────────────────────────┐
│              ASYNCHRONOUS OPERATION PATTERN                   │
└──────────────────────────────────────────────────────────────┘

Thread Model:
─────────────

HTTP Request Thread (Tomcat)
  │
  ├─> Controller receives request
  │   └─> @Async annotation triggers new thread
  │
  └─> Returns CompletableFuture<ResponseEntity>
      │
      ├─> Tomcat thread released (non-blocking)
      │
      └─> Async Executor Thread Pool
          │
          ├─> Domain Service executes
          │   └─> OCR Service (async)
          │       └─> Tesseract processing (blocking)
          │           ├─> PDF conversion
          │           ├─> OCR extraction (20-25 sec)
          │           └─> Text parsing
          │
          └─> CompletableFuture resolves
              └─> Response sent to client

Benefits:
- Non-blocking HTTP threads
- Better resource utilization
- Can handle more concurrent requests
- Long-running OCR doesn't block other operations
```

---

**Document Version:** 1.1
**Last Updated:** 2025-12-09
**Changes in v1.1:**
- Added LLM integration (Groq API with Llama 3.1 70B)
- Updated extraction flow to include intelligent data extraction
- Added Optional pattern for null-safe field handling
- Updated architecture diagrams with LLM adapter

**Related Documents:**
- [Technical Requirements Document](technical-requirements-document.md)
- [Backend Technical Acceptance Criteria](technical-acceptance-criteria.md)
- [Frontend Technical Requirements](frontend-technical-requirements.md)
- [OCR Service Technical Requirements](ocr-service-technical-requirements.md)
