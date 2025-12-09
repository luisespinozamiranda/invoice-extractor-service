# Technical Acceptance Criteria (TAC)

**Project:** AI-Assisted Invoice Extraction Service
**Document Type:** Technical Acceptance Criteria
**Version:** 1.0
**Date:** 2025-12-08
**Related Documents:**
- [Business Acceptance Criteria](requirements/business-acceptance-criteria.md)
- [Technical Requirements Document](technical-requirements-document.md)

---

## 1. File Upload Endpoint

### API Specification

**Endpoint:** `POST /api/v1.0/invoices/upload`

**Request:**
```http
POST /invoice-extractor-service/api/v1.0/invoices/upload
Content-Type: multipart/form-data

file: [binary file data]
```

**Accepted File Types:**
- `application/pdf`
- `image/png`
- `image/jpg`
- `image/jpeg`

**Maximum File Size:** 10 MB

**Response - Success (200 OK):**
```json
{
  "extractionKey": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "fileName": "invoice_2024_001.pdf",
  "status": "PROCESSING",
  "uploadedAt": "2025-12-08T10:30:00Z"
}
```

**Response - Invalid File Type (400 Bad Request):**
```json
{
  "errorCode": "INVALID_FILE_TYPE",
  "message": "File type not supported. Accepted types: PDF, PNG, JPG, JPEG",
  "timestamp": "2025-12-08T10:30:00Z"
}
```

**Response - File Too Large (413 Payload Too Large):**
```json
{
  "errorCode": "FILE_TOO_LARGE",
  "message": "File size exceeds maximum allowed size of 10 MB",
  "timestamp": "2025-12-08T10:30:00Z"
}
```

### Technical Validation Requirements

✅ **File Type Validation:**
- Must validate file MIME type, not just extension
- Reject files with mismatched extension and content type
- Implementation: Use Apache Tika or similar library for content type detection

✅ **File Size Validation:**
- Enforce 10 MB limit at application level
- Configuration property: `spring.servlet.multipart.max-file-size=10MB`

✅ **File Storage:**
- Temporarily store uploaded file in memory or temp directory
- Clean up temp files after processing (max 24 hours retention)

---

## 2. Automatic Data Extraction

### Processing Pipeline

**OCR Service Interface:**
```java
public interface IOcrService {
    CompletableFuture<ExtractionResultModel> extractInvoiceData(byte[] fileData, String fileName);
}
```

**Extraction Result Model:**
```java
public record ExtractionResultModel(
    String invoiceNumber,
    BigDecimal invoiceAmount,
    String clientName,
    String clientAddress,
    Double confidenceScore,
    String ocrEngine,
    LocalDateTime extractedAt
) {}
```

### Data Extraction Requirements

✅ **Invoice Number Extraction:**
- Pattern matching: `INV-\d+`, `Invoice #\d+`, `Invoice No: \d+`
- Must handle various formats (alphanumeric)
- Fallback: "UNKNOWN" if not found

✅ **Invoice Amount Extraction:**
- Pattern matching for currency amounts: `\$?\d+\.?\d{0,2}`
- Support for formats: `$1,450.75`, `1450.75`, `USD 1450.75`
- Decimal precision: 2 decimal places
- Data type: `BigDecimal` for accuracy

✅ **Client Name Extraction:**
- Search for keywords: "Bill To:", "Client:", "Customer:", "Sold To:"
- Extract next line or adjacent text
- Maximum length: 255 characters
- Fallback: "UNKNOWN" if not found

✅ **Client Address Extraction:**
- Extract multi-line address following client name
- Pattern: Street, City, State, ZIP
- Maximum length: 500 characters
- Fallback: "UNKNOWN" if not found

✅ **Processing Time:**
- Must complete extraction within 30 seconds
- Implementation: `@Async` with timeout configuration
- Timeout handling: Return partial results with error flag

✅ **Confidence Score:**
- OCR engine must provide confidence score (0.0 to 1.0)
- Log extractions with confidence < 0.7 for review
- Store confidence score in extraction metadata

---

## 3. JSON Response Structure

### Invoice API Endpoints

**Get Invoice by Key:**

**Endpoint:** `GET /api/v1.0/invoices/{invoiceKey}`

**Response (200 OK):**
```json
{
  "invoiceKey": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "invoiceNumber": "INV-12345",
  "invoiceAmount": 1450.75,
  "clientName": "ACME Corp",
  "clientAddress": "123 Main St, Salt Lake City, UT 84101",
  "vendorKey": "7ab12c34-1234-5678-9abc-def012345678",
  "issueDate": "2025-12-01",
  "dueDate": "2025-12-31",
  "currency": "USD",
  "status": "EXTRACTED",
  "createdAt": "2025-12-08T10:30:00Z",
  "updatedAt": "2025-12-08T10:30:15Z"
}
```

**Response - Not Found (404 Not Found):**
```json
{
  "errorCode": "INVOICE_NOT_FOUND",
  "message": "Invoice with key 3fa85f64-5717-4562-b3fc-2c963f66afa6 not found",
  "timestamp": "2025-12-08T10:30:00Z"
}
```

**Get All Invoices:**

**Endpoint:** `GET /api/v1.0/invoices`

**Query Parameters (Optional):**
- `page` (int, default: 0)
- `size` (int, default: 20)
- `sort` (string, default: "createdAt,desc")

**Response (200 OK):**
```json
{
  "content": [
    {
      "invoiceKey": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "invoiceNumber": "INV-12345",
      "invoiceAmount": 1450.75,
      "clientName": "ACME Corp",
      "clientAddress": "123 Main St, Salt Lake City, UT 84101",
      "status": "EXTRACTED",
      "createdAt": "2025-12-08T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### JSON Validation Requirements

✅ **Data Type Correctness:**
- `invoiceAmount`: Must be numeric (JSON number type)
- `invoiceKey`, `vendorKey`: Must be valid UUID format
- Dates: Must be ISO 8601 format (`yyyy-MM-dd'T'HH:mm:ss'Z'`)
- Strings: Must be properly escaped

✅ **HTTP Status Codes:**
- `200 OK`: Successful retrieval
- `201 Created`: Successful creation
- `400 Bad Request`: Invalid request payload
- `404 Not Found`: Resource not found
- `413 Payload Too Large`: File size exceeded
- `500 Internal Server Error`: Server-side errors
- `503 Service Unavailable`: OCR service unavailable

✅ **Error Response Format:**
All error responses must follow this structure:
```json
{
  "errorCode": "ERROR_CODE_ENUM",
  "message": "Human-readable error message",
  "timestamp": "2025-12-08T10:30:00Z",
  "details": {}  // Optional additional context
}
```

---

## 4. Database Storage

### Database Schema

**Schema Name:** `invoicedata`

**Table: `tb_invoice`**

```sql
CREATE TABLE invoicedata.tb_invoice (
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
    CONSTRAINT fk_vendor FOREIGN KEY (vendor_key) REFERENCES invoicedata.tb_vendor(vendor_key)
);

CREATE INDEX idx_invoice_key ON invoicedata.tb_invoice(invoice_key);
CREATE INDEX idx_invoice_number ON invoicedata.tb_invoice(invoice_number);
CREATE INDEX idx_created_at ON invoicedata.tb_invoice(created_at DESC);
```

**Table: `tb_invoice_line_item`**

```sql
CREATE TABLE invoicedata.tb_invoice_line_item (
    id BIGSERIAL PRIMARY KEY,
    line_item_key UUID NOT NULL UNIQUE,
    invoice_key UUID NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    total_price DECIMAL(15, 2) NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice FOREIGN KEY (invoice_key) REFERENCES invoicedata.tb_invoice(invoice_key) ON DELETE CASCADE
);

CREATE INDEX idx_line_item_invoice_key ON invoicedata.tb_invoice_line_item(invoice_key);
```

**Table: `tb_vendor`**

```sql
CREATE TABLE invoicedata.tb_vendor (
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

CREATE INDEX idx_vendor_key ON invoicedata.tb_vendor(vendor_key);
CREATE INDEX idx_vendor_name ON invoicedata.tb_vendor(vendor_name);
```

**Table: `tb_extraction_metadata`**

```sql
CREATE TABLE invoicedata.tb_extraction_metadata (
    id BIGSERIAL PRIMARY KEY,
    extraction_key UUID NOT NULL UNIQUE,
    invoice_key UUID NOT NULL,
    source_file_name VARCHAR(255) NOT NULL,
    extraction_timestamp TIMESTAMP NOT NULL,
    extraction_status VARCHAR(50) NOT NULL,
    confidence_score DECIMAL(3, 2),
    ocr_engine VARCHAR(100),
    extraction_data JSONB,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_extraction_invoice FOREIGN KEY (invoice_key) REFERENCES invoicedata.tb_invoice(invoice_key) ON DELETE CASCADE
);

CREATE INDEX idx_extraction_key ON invoicedata.tb_extraction_metadata(extraction_key);
CREATE INDEX idx_extraction_invoice_key ON invoicedata.tb_extraction_metadata(invoice_key);
CREATE INDEX idx_extraction_status ON invoicedata.tb_extraction_metadata(extraction_status);
```

### JPA Entity Requirements

✅ **Entity Structure:**
- Use `@Entity` and `@Table` annotations with schema specification
- Primary key: `@Id` with `@GeneratedValue(strategy = GenerationType.SEQUENCE)`
- UUID fields: Use `@Column(columnDefinition = "UUID")` with `@Type(UUIDCharType.class)`
- Timestamps: Use `@CreationTimestamp` and `@UpdateTimestamp` from Hibernate

✅ **JSONB Support:**
- Use custom `@Converter` for JSONB columns
- Store as `String` in entity, convert to `JsonNode` for database
- Example: `@Convert(converter = JsonConverterV1_0.class)`

✅ **Hibernate Configuration:**
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
```

### Repository Interface Requirements

✅ **Spring Data JPA Repositories:**
```java
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Invoice findByInvoiceKey(UUID invoiceKey);
    List<Invoice> findAllByOrderByCreatedAtDesc();
}

public interface ExtractionMetadataRepository extends JpaRepository<ExtractionMetadata, Long> {
    ExtractionMetadata findByExtractionKey(UUID extractionKey);
    List<ExtractionMetadata> findByInvoiceKey(UUID invoiceKey);
}
```

✅ **Data Persistence Requirements:**
- All invoice data must persist after application restart
- Transaction management: Use `@Transactional` on service methods
- Rollback on exceptions to maintain data consistency
- Audit timestamps (created_at, updated_at) auto-populated

---

## 5. UI Display

### Frontend Requirements

**Technology Stack:**
- Angular 16+ (or latest stable)
- Angular Material for UI components
- RxJS for async operations

### Invoice List Component

**Component:** `InvoiceListComponent`

**API Integration:**
```typescript
export interface InvoiceDto {
  invoiceKey: string;
  invoiceNumber: string;
  invoiceAmount: number;
  clientName: string;
  clientAddress: string;
  status: string;
  createdAt: string;
}

@Injectable()
export class InvoiceService {
  getInvoices(): Observable<InvoiceDto[]> {
    return this.http.get<InvoiceDto[]>(`${API_BASE_URL}/api/v1.0/invoices`);
  }
}
```

**Table Columns:**
1. **Invoice Number** - Display as text, sortable
2. **Invoice Amount** - Display with currency format (`$1,450.75`)
3. **Client Name** - Display as text, truncate if > 50 chars
4. **Client Address** - Display as text, truncate if > 75 chars
5. **Processed Date/Time** - Display in local timezone (`MM/DD/YYYY HH:mm`)

**Table Features:**
- ✅ Pagination (20 records per page)
- ✅ Sorting by any column
- ✅ Auto-refresh after new invoice upload
- ✅ Loading spinner during data fetch
- ✅ Empty state message if no invoices

**Responsive Design:**
- ✅ Mobile view: Stack columns vertically
- ✅ Tablet view: Show essential columns only
- ✅ Desktop view: Show all columns

### Invoice Upload Component

**Component:** `InvoiceUploadComponent`

**Upload Interface:**
```typescript
export class InvoiceUploadComponent {
  uploadFile(file: File): Observable<ExtractionResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ExtractionResponse>(
      `${API_BASE_URL}/api/v1.0/invoices/upload`,
      formData
    );
  }
}
```

**UI Requirements:**
- ✅ File input with drag-and-drop support
- ✅ File type validation (client-side)
- ✅ File size validation (client-side)
- ✅ Upload progress indicator
- ✅ Success notification with extracted data preview
- ✅ Error notification with clear message

**User Feedback:**
- Success: "Invoice uploaded successfully! Extraction complete."
- Processing: "Processing invoice... Please wait."
- Error: Display specific error message from API response

### Data Formatting Requirements

✅ **Currency Formatting:**
```typescript
formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(amount);
}
// Output: "$1,450.75"
```

✅ **Date/Time Formatting:**
```typescript
formatDateTime(isoDate: string): string {
  return new Date(isoDate).toLocaleString('en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}
// Output: "12/08/2025 10:30 AM"
```

---

## 6. Error Handling

### Backend Error Handling

**Custom Exception Class:**
```java
@Getter
public class InvoiceExtractorServiceException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public InvoiceExtractorServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
}
```

**Error Codes Enumeration:**
```java
public enum ErrorCode {
    INVALID_FILE_TYPE("INV-001", "Invalid file type provided"),
    FILE_TOO_LARGE("INV-002", "File size exceeds maximum limit"),
    OCR_SERVICE_UNAVAILABLE("INV-003", "OCR service is currently unavailable"),
    EXTRACTION_FAILED("INV-004", "Failed to extract invoice data"),
    INVOICE_NOT_FOUND("INV-005", "Invoice not found"),
    DATABASE_ERROR("INV-006", "Database operation failed"),
    INVALID_REQUEST("INV-007", "Invalid request payload");

    private final String code;
    private final String message;
}
```

**Global Exception Handler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvoiceExtractorServiceException.class)
    public ResponseEntity<ErrorResponse> handleInvoiceExtractorException(
        InvoiceExtractorServiceException ex
    ) {
        ErrorResponse response = new ErrorResponse(
            ex.getErrorCode().getCode(),
            ex.getMessage(),
            LocalDateTime.now(),
            ex.getDetails()
        );
        return ResponseEntity.status(determineHttpStatus(ex.getErrorCode()))
                             .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Log full stack trace
        log.error("Unexpected error occurred", ex);

        ErrorResponse response = new ErrorResponse(
            "INV-999",
            "An unexpected error occurred",
            LocalDateTime.now(),
            Map.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(response);
    }
}
```

### Error Handling Requirements

✅ **Unreadable Files:**
- Catch: `IOException`, `InvalidFormatException`
- Log: Full exception with file name and size
- User Message: "The uploaded file could not be read. Please ensure the file is not corrupted."
- HTTP Status: 400 Bad Request

✅ **OCR Failures:**
- Catch: OCR service timeout, connection errors
- Retry: 3 attempts with exponential backoff (Resilience4j)
- Fallback: Return partial data with status "EXTRACTION_FAILED"
- User Message: "Invoice extraction failed. Please try again or contact support."
- HTTP Status: 500 Internal Server Error

✅ **Database Errors:**
- Catch: `DataAccessException`, `SQLException`
- Log: Full stack trace with sanitized query
- User Message: "Failed to save invoice data. Please try again."
- HTTP Status: 500 Internal Server Error

✅ **Application Stability:**
- All exceptions caught and handled gracefully
- No uncaught exceptions that crash the application
- Circuit breaker pattern for external OCR service calls
- Health check endpoint: `GET /actuator/health`

### Logging Requirements

✅ **Structured Logging:**
```java
@Slf4j
public class InvoiceService {
    public CompletableFuture<InvoiceModel> createInvoice(InvoiceModel invoice) {
        log.info("Creating invoice: invoiceNumber={}, amount={}",
                 invoice.invoiceNumber(), invoice.totalAmount());

        try {
            // Business logic
            log.debug("Invoice created successfully: invoiceKey={}", result.invoiceKey());
        } catch (Exception e) {
            log.error("Failed to create invoice: invoiceNumber={}, error={}",
                      invoice.invoiceNumber(), e.getMessage(), e);
            throw e;
        }
    }
}
```

✅ **Log Levels:**
- `ERROR`: Exceptions, failed operations, OCR failures
- `WARN`: Low confidence extractions, retry attempts
- `INFO`: Successful operations, invoice creation/retrieval
- `DEBUG`: Request/response payloads, SQL queries (if enabled)

✅ **Log Format:**
```
2025-12-08 10:30:15.123 INFO  [http-nio-8080-exec-1] c.t.s.i.domain.service.InvoiceService : Creating invoice: invoiceNumber=INV-12345, amount=1450.75
```

---

## Definition of Done Checklist

### Backend Implementation

- [ ] `POST /api/v1.0/invoices/upload` endpoint accepts PDF/PNG/JPG/JPEG files
- [ ] File type validation rejects unsupported formats with 400 error
- [ ] File size validation enforces 10 MB limit
- [ ] OCR service extracts invoice number, amount, client name, address
- [ ] Extraction completes within 30 seconds or returns timeout error
- [ ] `GET /api/v1.0/invoices/{invoiceKey}` returns invoice in JSON format
- [ ] `GET /api/v1.0/invoices` returns paginated list of invoices
- [ ] All responses follow documented JSON schema
- [ ] HTTP status codes match specification (200, 400, 404, 500, 503)
- [ ] All error responses include errorCode, message, and timestamp

### Database Implementation

- [ ] PostgreSQL schema `invoicedata` created
- [ ] Tables `tb_invoice`, `tb_vendor`, `tb_invoice_line_item`, `tb_extraction_metadata` created
- [ ] All indexes on UUID keys and foreign keys created
- [ ] JPA entities correctly map to database tables
- [ ] JSONB converter works for `extraction_data` column
- [ ] Timestamps (created_at, updated_at) auto-populate
- [ ] Data persists after application restart
- [ ] Transaction rollback works on errors

### Frontend Implementation

- [ ] Invoice upload component with drag-and-drop UI implemented
- [ ] File type validation on client side
- [ ] Upload progress indicator displays during processing
- [ ] Invoice list table displays 5 columns (number, amount, client name, address, date)
- [ ] Currency formatting displays as `$1,450.75`
- [ ] Date formatting displays in local timezone
- [ ] Table updates automatically after successful upload
- [ ] Pagination works (20 records per page)
- [ ] Responsive design works on mobile, tablet, desktop

### Error Handling Implementation

- [ ] Custom exception class `InvoiceExtractorServiceException` created
- [ ] Error codes enum with all error types defined
- [ ] Global exception handler catches and formats all errors
- [ ] Unreadable file errors display user-friendly message
- [ ] OCR failures retry 3 times with exponential backoff
- [ ] Application remains stable after any error (no crashes)
- [ ] All errors logged with sufficient detail
- [ ] Structured logging with INFO, DEBUG, ERROR levels implemented

### Testing

- [ ] Unit tests for domain services (InvoiceService, ExtractionService)
- [ ] Unit tests for repository services
- [ ] Integration tests for REST endpoints
- [ ] Integration tests for database operations
- [ ] Mock OCR service for testing extraction logic
- [ ] Error handling tests (invalid files, OCR failures, DB errors)
- [ ] All tests pass (`mvn test`)

### Documentation

- [ ] Swagger UI accessible at `/swagger-ui.html`
- [ ] All API endpoints documented in Swagger
- [ ] README.md includes setup and run instructions
- [ ] Database schema diagram included
- [ ] Environment variables documented

---

## Non-Functional Requirements

### Performance

- **Response Time:**
  - File upload endpoint: < 2 seconds (excluding extraction)
  - Invoice retrieval: < 500ms
  - Invoice list: < 1 second

- **Throughput:**
  - Support 10 concurrent uploads
  - Support 100 concurrent read requests

### Security

- **Input Validation:**
  - Sanitize all user inputs
  - Validate file content, not just extension
  - Prevent path traversal attacks in file handling

- **Data Protection:**
  - Use HTTPS for all API calls (production)
  - No sensitive data in logs (mask file content)

### Reliability

- **Fault Tolerance:**
  - Circuit breaker on OCR service calls (open after 5 failures)
  - Retry with exponential backoff (3 attempts)
  - Graceful degradation if OCR unavailable

- **Data Integrity:**
  - Transactional operations for data consistency
  - Foreign key constraints enforced
  - Unique constraints on UUID keys

### Monitoring

- **Health Checks:**
  - `/actuator/health` endpoint returns UP/DOWN status
  - Database connectivity check
  - OCR service connectivity check

- **Metrics:**
  - Count of successful/failed extractions
  - Average extraction time
  - API endpoint response times

---

**Document Status:** Draft
**Approval Pending:** Technical Lead
**Last Updated:** 2025-12-08
