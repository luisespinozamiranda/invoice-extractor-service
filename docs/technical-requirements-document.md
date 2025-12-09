# Technical Requirements Document
# Invoice Extractor Service

> **AI-Assisted Document**: This TRD was generated with AI assistance to define technical requirements for the Invoice Extraction application.

---

## Document Metadata

| Field | Value |
|-------|-------|
| **TRD ID** | INVOICE-EXT-001 |
| **Type** | Feature - New Application |
| **Impact Level** | High - Complete new system |
| **Created** | 2025-12-05 |
| **Author** | Luis Espinoza |
| **Deadline** | 2025-12-22 |
| **Status** | Draft |

---

## 1. Overview

### Summary
Build an end-to-end invoice extraction application using Angular (frontend), Java Spring Boot (backend), and PostgreSQL (database) that leverages LLM (Groq Llama 3.1 70B) and OCR (Tesseract) to automatically extract structured data (invoice number, amount, client name, client address) from uploaded PDF or image files. The system uses intelligent LLM-based extraction with regex fallback for reliability.

### Driver
- [x] Product/Business (Training exercise with business value demonstration)
- [x] Engineering (Learning AI-assisted development and hexagonal architecture)

### Business Value
- **Training Value**: Demonstrate ability to use AI tools for planning, design, and implementation
- **Practical Value**: Automate manual invoice data entry, reducing processing time from ~5 minutes to ~30 seconds per invoice
- **Portfolio Value**: Showcase full-stack development with modern architecture patterns

### Related Work
- **Training Program**: Performance Improvement Plan - Invoice Extraction Exercise
- **Deadline**: December 22, 2025
- **Related Documentation**:
  - `docs/requirements/project-description.md`
  - `docs/requirements/business-acceptance-criteria.md`
  - `docs/requirements/ai-usage-expectations.md`
- **Reference Architecture**: `risk-data-state-service` (hexagonal architecture pattern)

---

## 2. Technical Scope

### Components Affected

**New Services/Modules to Create:**

1. **Backend (Java Spring Boot)**
   - `invoice-extractor-service` - Main Spring Boot application
   - LLM integration module (Groq API adapter)
   - OCR integration module (Tesseract)
   - Invoice processing pipeline (with LLM-first, regex fallback)
   - REST API layer
   - Database persistence layer

2. **Frontend (Angular)**
   - `invoice-extractor-ui` - Angular application
   - File upload component
   - Invoice table component
   - API service layer

3. **Database (PostgreSQL)**
   - Invoice storage schema
   - Metadata tables

### Scope Boundaries

**What's IN Scope:**
- ✅ File upload (PDF, PNG, JPG, JPEG)
- ✅ LLM-powered intelligent extraction of 4 fields (invoice #, amount, client name, address)
- ✅ OCR text extraction using Tesseract
- ✅ Dual extraction strategy (LLM-first with regex fallback)
- ✅ Optional<T> pattern for null-safe field handling
- ✅ JSON response format
- ✅ PostgreSQL storage of extracted data
- ✅ Angular UI with table display
- ✅ Basic error handling
- ✅ Technical documentation (TAC, TRD)
- ✅ Hexagonal architecture implementation
- ✅ RESTful API with Swagger documentation
- ✅ Unit and integration tests

**What's OUT of Scope:**
- ❌ User authentication/authorization
- ❌ Invoice validation against business rules
- ❌ Invoice editing after extraction
- ❌ Bulk file upload (multiple files at once)
- ❌ Export to CSV/Excel
- ❌ Email notifications
- ❌ Multi-language support
- ❌ Production deployment infrastructure
- ❌ Advanced OCR training/customization

**Stop Conditions:**
- If LLM + OCR accuracy < 70% → Reassess extraction approach or providers
- If processing time > 60 seconds per invoice → Optimize or use async processing
- If Groq API rate limits become problematic → Consider paid tier or alternative provider
- If scope expands beyond 8 core features → Create new TRD

### Breaking Changes
**Breaking Change**: No (New application, no existing API to break)

---

## 3. Current State Analysis

### Existing Behavior
**Current State**: No existing system. Manual invoice processing is performed by:
- Human data entry from paper/PDF invoices
- Average time: 5-10 minutes per invoice
- Error rate: ~5% (typos, misreads)

### Problems/Gaps
1. **Manual Process**: Time-consuming and error-prone
2. **No Automation**: No AI/OCR integration for document processing
3. **No Digital Record**: Invoices stored as files, not structured data
4. **No Search/Filter**: Cannot easily query invoice data

### Code Quality Assessment
**Starting Point**: Clean slate
- Technical debt level: **None** (new project)
- Test coverage: **Target: 80%+**
- Complexity: **Medium** (OCR integration adds complexity)

---

## 4. Proposed Solution

### Technical Approach

**Architecture**: Hexagonal (Ports & Adapters) Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Angular Frontend                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Upload Comp  │  │ Table Comp   │  │ API Service  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │ HTTP REST API
┌─────────────────────────────────────────────────────────────┐
│               Spring Boot Backend (Java 17)                 │
│  ┌───────────────────────────────────────────────────────┐ │
│  │              Inbound Adapters (REST)                  │ │
│  │  ┌────────────────┐  ┌────────────────┐              │ │
│  │  │ InvoiceCtrl    │  │ ControllerSvc  │              │ │
│  │  └────────────────┘  └────────────────┘              │ │
│  └───────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────┐ │
│  │                  Domain Layer                         │ │
│  │  ┌────────────┐  ┌─────────────┐  ┌──────────────┐  │ │
│  │  │ InvoiceSvc │  │ OcrService  │  │ Models       │  │ │
│  │  └────────────┘  └─────────────┘  └──────────────┘  │ │
│  └───────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────┐ │
│  │            Outbound Adapters (Database)               │ │
│  │  ┌────────────────┐  ┌────────────────┐              │ │
│  │  │ RepoService    │  │ JPA Repository │              │ │
│  │  └────────────────┘  └────────────────┘              │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                   PostgreSQL Database                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ tb_invoice   │  │ tb_metadata  │  │ Audit Tables │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

**Request Flow:**
1. User uploads file via Angular UI
2. Angular sends multipart/form-data to REST endpoint
3. REST Controller receives file → Controller Service
4. Controller Service → Domain Service (InvoiceService)
5. Domain Service → OCR Service (extracts text)
6. OCR Service uses external API (Tesseract/Google Vision/AWS Textract)
7. Parsing logic extracts 4 fields from OCR text
8. Domain Service → Repository Service → JPA Repository
9. Data persisted to PostgreSQL
10. Response flows back through layers
11. Angular displays results in table

### Design Decisions

| Decision | Rationale | Alternatives Considered |
|----------|-----------|------------------------|
| **Hexagonal Architecture** | Clean separation of concerns, testable, matches reference architecture | Layered architecture (less flexible), MVC (too coupled) |
| **Spring Boot 3.1.2** | Matches reference project, mature ecosystem, Java 17 support | Spring Boot 2.x (older), Quarkus (steeper learning curve) |
| **Java Records for Domain Models** | Immutability, less boilerplate, modern Java | POJOs with Lombok, traditional classes |
| **CompletableFuture for Async** | Non-blocking I/O, better scalability | Synchronous (blocking), Reactive (more complex) |
| **PostgreSQL JSONB** | Flexible schema for OCR metadata | Separate tables (more rigid), NoSQL (adds complexity) |
| **Angular** | Required by spec, rich ecosystem | React (not specified), Vue (not specified) |
| **Tesseract OCR** | Free, open-source, local processing | Google Vision ($$), AWS Textract ($$), Azure Form Recognizer ($$) |
| **Groq API (Llama 3.1 70B)** | Free, fast, intelligent extraction | OpenAI GPT-4 ($$), Claude ($$), local models (slower) |
| **SpringDoc OpenAPI** | Auto-generates Swagger UI, Spring Boot 3 compatible | Springfox (deprecated for Boot 3) |

### Architectural Patterns
- [x] Hexagonal Architecture (Ports & Adapters)
- [x] Repository Pattern
- [x] Dependency Injection (Constructor-based)
- [x] Adapter Pattern (REST DTOs ↔ Domain Models ↔ Entities)
- [x] Async/Non-Blocking with CompletableFuture
- [x] Mapper Pattern (bidirectional transformations)
- [ ] Decorator Pattern
- [ ] Strategy Pattern (could be used for multiple OCR providers)

### Code Examples

**Domain Model (Java Record):**
```java
public record InvoiceModel(
    UUID invoiceKey,
    String invoiceNumber,
    BigDecimal invoiceAmount,
    String clientName,
    String clientAddress,
    String originalFileName,
    LocalDateTime processedAt,
    ExtractionStatus status
) {}
```

**REST Controller:**
```java
@RestController
@RequestMapping("/api/v1.0")
@Tag(name = "Invoice", description = "Invoice extraction endpoints")
public class InvoiceControllerV1_0 {

    private final IInvoiceControllerServiceV1_0 controllerService;

    @Async
    @PostMapping(value = "/invoices/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Extract invoice data from uploaded file")
    public CompletableFuture<ResponseEntity<InvoiceV1_0>> extractInvoice(
        @RequestParam("file") MultipartFile file) {
        return controllerService.extractInvoice(file);
    }

    @Async
    @GetMapping("/invoices")
    @Operation(summary = "Get all processed invoices")
    public CompletableFuture<ResponseEntity<List<InvoiceV1_0>>> getAllInvoices() {
        return controllerService.getAllInvoices();
    }
}
```

**OCR Service Interface:**
```java
public interface IOcrService {
    CompletableFuture<OcrResult> extractText(byte[] fileData, String fileType);
    InvoiceModel parseInvoiceData(String ocrText);
}
```

---

## 5. Implementation Plan

### Effort Estimate

| Task Category | Hours Range | Most Likely | Confidence | Owner |
|---------------|-------------|-------------|------------|-------|
| **Backend Development** | | | | |
| - Project setup & config | 1-2 | 1.5 | High | Luis E. |
| - Domain layer | 3-5 | 4 | High | Luis E. |
| - OCR integration | 5-8 | 6 | Medium | Luis E. |
| - Repository layer | 3-4 | 3.5 | High | Luis E. |
| - REST API layer | 3-5 | 4 | High | Luis E. |
| - Error handling | 2-3 | 2.5 | High | Luis E. |
| **Frontend Development** | | | | |
| - Angular project setup | 1-2 | 1.5 | High | Luis E. |
| - Upload component | 2-4 | 3 | Medium | Luis E. |
| - Table component | 2-3 | 2.5 | High | Luis E. |
| - API service integration | 2-3 | 2.5 | High | Luis E. |
| - Styling & UX | 2-4 | 3 | Medium | Luis E. |
| **Database** | | | | |
| - Schema design | 1-2 | 1.5 | High | Luis E. |
| - Migration scripts | 1-2 | 1.5 | High | Luis E. |
| **Testing** | | | | |
| - Unit tests (Backend) | 4-6 | 5 | Medium | Luis E. |
| - Integration tests | 3-5 | 4 | Medium | Luis E. |
| - E2E tests (Frontend) | 2-4 | 3 | Low | Luis E. |
| **Documentation** | | | | |
| - TAC document | 2-3 | 2.5 | High | Luis E. |
| - TRD document | 3-4 | 3.5 | High | Luis E. |
| - API documentation | 1-2 | 1.5 | High | Luis E. |
| - README & setup guides | 1-2 | 1.5 | High | Luis E. |
| **Code Review & Refinement** | 3-5 | 4 | Medium | Luis E. |
| **Buffer for unknowns** | 5-10 | 7 | Low | - |
| **TOTAL** | **48-80** | **64** | **Medium** | - |

**Estimation Confidence**: Medium (60-70%)

**Estimation Notes:**
- OCR integration is the biggest unknown - accuracy and parsing logic may require iteration
- Frontend estimate assumes basic Angular knowledge, may take longer if learning curve
- Testing estimate conservative - may be faster with AI-generated test scaffolding
- Buffer included for OCR provider selection, unexpected API changes, learning curve

### Phases

#### Phase 1: Foundation & Setup (5-8 hours)
**Goal**: Working Spring Boot + Angular apps with database connection
**Duration**: 5-8 hours
**Tasks**:
- [x] Create Spring Boot project with hexagonal architecture
- [x] Configure PostgreSQL connection
- [x] Setup Angular project
- [ ] Create database schema
- [ ] Setup CI/CD pipeline (optional)

#### Phase 2: Backend Core Implementation (15-20 hours)
**Goal**: Working REST API with OCR extraction
**Duration**: 15-20 hours
**Tasks**:
- [ ] Implement domain models (InvoiceModel, etc.)
- [ ] Create domain services (IInvoiceService, IOcrService)
- [ ] Integrate OCR provider (Tesseract/Google Vision)
- [ ] Implement parsing logic for 4 fields
- [ ] Create repository layer (JPA entities, repositories)
- [ ] Implement REST controllers
- [ ] Add error handling and validation

#### Phase 3: Frontend Implementation (10-14 hours)
**Goal**: Working Angular UI with file upload and table display
**Duration**: 10-14 hours
**Tasks**:
- [ ] Create file upload component with validation
- [ ] Implement API service for backend communication
- [ ] Create invoice table component
- [ ] Add error handling and user feedback
- [ ] Style UI with CSS/Material Design
- [ ] Add loading states and progress indicators

#### Phase 4: Testing & Quality (9-15 hours)
**Goal**: Comprehensive test coverage and quality assurance
**Duration**: 9-15 hours
**Tasks**:
- [ ] Write unit tests for domain services
- [ ] Write unit tests for repository layer
- [ ] Write integration tests for REST API
- [ ] Write E2E tests for critical user flows
- [ ] Perform manual QA testing
- [ ] Fix bugs and issues

#### Phase 5: Documentation & Review (7-10 hours)
**Goal**: Complete technical documentation
**Duration**: 7-10 hours
**Tasks**:
- [ ] Complete TAC document
- [x] Complete TRD document (in progress)
- [ ] Update API documentation (Swagger)
- [ ] Write README with setup instructions
- [ ] Document AI usage throughout project
- [ ] Code review and refinements

#### Phase 6: Buffer & Polish (5-10 hours)
**Goal**: Handle unknowns and polish
**Duration**: 5-10 hours
**Tasks**:
- [ ] Handle unexpected issues
- [ ] Improve OCR accuracy if needed
- [ ] Performance optimization
- [ ] Final QA and bug fixes

### Dependencies

**Blocking Dependencies:**
- ✅ PostgreSQL database available (Render.com)
- ⏳ OCR provider selection (Tesseract vs Cloud API)
- ⏳ Java 17 JDK installed
- ⏳ Node.js and Angular CLI installed
- ⏳ Maven installed

**Non-Blocking Dependencies:**
- Swagger UI configuration (can proceed without)
- CI/CD pipeline (can deploy manually)
- Advanced error handling (can add incrementally)

---

## 6. Compatibility & Risk Analysis

### Compatibility Impact

**API Compatibility:**
- Breaking changes: **No** (new API)
- Deprecations: **None**
- Migration required: **No**

**Behavioral Compatibility:**
- Changes to business logic: **N/A** (new system)
- Changes to error handling: **N/A**
- Changes to timing/performance: **N/A**

**Data Compatibility:**
- Schema changes: **N/A** (new schema)
- Data migration needed: **No**
- Backward compatible: **N/A**

**Dependency Compatibility:**
- Library upgrades: **None** (new dependencies)
- Internal API changes: **None**

### Risk Assessment

| Risk | Probability | Impact | Severity | Mitigation |
|------|-------------|--------|----------|------------|
| OCR accuracy < 70% | Medium | High | **High** | Test multiple OCR providers, implement manual correction UI |
| Processing time > 60s | Low | Medium | **Medium** | Implement async processing with job queue |
| Learning curve for new tech | High | Medium | **High** | Use AI assistance, allocate buffer time |
| Missed deadline (Dec 22) | Medium | High | **Critical** | Prioritize core features, cut nice-to-haves |
| Database connection issues | Low | High | **Medium** | Test early, have backup local PostgreSQL |
| File upload size limits | Medium | Low | **Low** | Implement file size validation (max 10MB) |
| Angular knowledge gaps | Medium | Medium | **Medium** | Use AI for code generation, follow tutorials |
| Security vulnerabilities | Low | High | **Medium** | Input validation, file type checking, no PII logging |

**Risk Categories Assessed:**
- ✅ Technical risks (complexity, unknowns)
- ✅ Performance risks (latency, throughput)
- ✅ Security risks (file upload vulnerabilities)
- ✅ Operational risks (deployment, monitoring)
- ✅ Schedule risks (deadline pressure)

### Mitigation Strategies

**Risk 1: OCR Accuracy < 70%**
- **Prevention**:
  - Research and test multiple OCR providers before committing
  - Use sample invoices to benchmark accuracy
  - Consider using AI (GPT-4 Vision) for structured extraction
- **Detection**:
  - Log extraction confidence scores
  - Manual review of first 10 extractions
- **Response**:
  - Switch to cloud OCR provider (Google Vision, AWS Textract)
  - Implement manual correction UI
  - Add "Review" status for low-confidence extractions

**Risk 2: Missed Deadline**
- **Prevention**:
  - Work backwards from Dec 22 deadline
  - Complete Phase 1-2 by Dec 10 (check-in point)
  - Daily progress tracking
- **Detection**:
  - Weekly time tracking vs estimate
  - Red flag if >20% over estimate in any phase
- **Response**:
  - Cut nice-to-have features (advanced error handling, styling)
  - Focus on core 6 acceptance criteria
  - Request scope reduction if necessary

**Risk 3: Learning Curve Impact**
- **Prevention**:
  - Use AI to generate boilerplate code
  - Follow reference architecture closely
  - Allocate 10 hours buffer for learning
- **Detection**:
  - Track time spent on "learning" vs "implementing"
  - If >30% time is learning → need help
- **Response**:
  - Request Tech Lead guidance
  - Focus on working code over perfect code
  - Document learnings for next time

---

## 7. Testing & Validation Strategy

### Pre-Testing: Test Suite Health Check

**Current Test Suite Status:**
- Date checked: 2025-12-05
- Total tests: 0 (new project)
- Currently passing: N/A
- Flaky tests: None

**Baseline Agreement:**
- Starting pass rate: 100% (all new tests must pass)
- Success criteria: Maintain 100% pass rate, achieve 80%+ coverage

### Test Strategy

**Scope**: All components and integrations

#### Unit Tests

**New Tests Required:**

**Backend:**
| Test Case | Type | Success Scenario | Failure Scenario |
|-----------|------|------------------|------------------|
| `InvoiceService.processInvoice()` | Unit | Returns InvoiceModel with all 4 fields | Throws exception on OCR failure |
| `OcrService.extractText()` | Unit | Returns OCR text from valid file | Throws exception on invalid file |
| `OcrService.parseInvoiceData()` | Unit | Parses 4 fields from OCR text | Handles missing fields gracefully |
| `InvoiceRepositoryService.save()` | Unit | Saves invoice to DB | Handles DB connection errors |
| `InvoiceMapper.entityToModel()` | Unit | Maps entity to model | Handles null values |
| `InvoiceControllerServiceV1_0` | Unit | Maps DTO to model and calls service | Validates input and handles errors |

**Frontend:**
| Test Case | Type | Success Scenario | Failure Scenario |
|-----------|------|------------------|------------------|
| `UploadComponent.onFileSelect()` | Unit | Accepts valid file types | Rejects invalid file types |
| `InvoiceService.uploadFile()` | Unit | Calls API with multipart data | Handles network errors |
| `InvoiceTableComponent.loadInvoices()` | Unit | Displays invoice list | Shows error message on failure |

**Coverage Target**: 80% for all backend code, 70% for frontend

#### Integration Tests

**New Tests Required:**

| Integration Point | Success Scenario | Failure Scenario |
|-------------------|------------------|------------------|
| REST API → Domain Service | POST /invoices/extract returns 200 with JSON | Returns 400 on invalid file, 500 on server error |
| Domain Service → OCR Service | OCR extracts text successfully | Handles OCR API timeout |
| Domain Service → Repository | Invoice persisted to PostgreSQL | Handles DB constraint violations |
| Repository → Database | JPA saves and retrieves entities | Handles connection pool exhaustion |
| Angular → Backend API | File upload completes successfully | Shows user-friendly error on failure |

#### End-to-End Tests

**Scenarios to Test:**

1. **Scenario**: Happy Path - Upload and Extract Invoice
   - **Success**: User uploads valid PDF → Extraction completes → Invoice displayed in table
   - **Failure**: Clear error message if any step fails

2. **Scenario**: Invalid File Type
   - **Success**: User uploads .txt file → Immediate validation error
   - **Failure**: Should not reach backend

3. **Scenario**: OCR Extraction Failure
   - **Success**: Backend gracefully handles OCR failure → Returns 500 with error message
   - **Failure**: UI shows "Extraction failed, please try again"

### Test Data Requirements

- **Mock Invoice PDFs**:
  - 3-5 sample invoices with varying formats
  - Include edge cases (blurry, rotated, multi-page)
- **Mock OCR Responses**:
  - Successful extraction text
  - Low-confidence extraction
  - Empty/garbled text
- **Test Database**:
  - Separate PostgreSQL schema for testing
  - Seed data for table display tests

### Performance Testing

**Baseline Metrics:**
- Current latency: N/A (new system)
- Target processing time: < 30 seconds per invoice

**Acceptance Criteria:**
- OCR processing: < 20 seconds
- Database save: < 1 second
- Total end-to-end: < 30 seconds
- Concurrent uploads: Support 5 simultaneous uploads

**Performance Test Scenarios:**
- Single file upload (PDF, 1MB)
- Large file upload (PDF, 10MB)
- Multiple concurrent uploads (5 users)
- 100 invoice table load test

### Acceptance Criteria

**Must-Have (Blocking):**
- [ ] File upload accepts PDF, PNG, JPG, JPEG
- [ ] OCR extracts invoice number, amount, client name, client address
- [ ] Extracted data saved to PostgreSQL
- [ ] JSON response returns all 4 fields
- [ ] Angular table displays all processed invoices
- [ ] Error handling for invalid files and OCR failures
- [ ] All new tests pass (80%+ coverage)
- [ ] API documented with Swagger
- [ ] TAC and TRD documents complete

**Nice-to-Have (Non-Blocking):**
- [ ] Drag-and-drop file upload
- [ ] Real-time processing status
- [ ] Search/filter on invoice table
- [ ] Export invoice data to CSV
- [ ] Multiple file upload
- [ ] Invoice thumbnail preview

---

## 8. Deployment & Operations

### Deployment Strategy

**Approach**: Manual Deployment (for training exercise)

**Rollout Plan:**
1. Deploy PostgreSQL database (Render.com) - **DONE**
2. Deploy Spring Boot backend to Render/Heroku
3. Deploy Angular frontend to Vercel/Netlify
4. Configure CORS and API URLs
5. Test end-to-end in production
6. Submit to Technical Lead for evaluation

**Deployment Checklist:**
- [ ] Database schema created
- [ ] Environment variables configured
- [ ] Backend deployed and running
- [ ] Frontend deployed and running
- [ ] CORS configured correctly
- [ ] Swagger UI accessible
- [ ] End-to-end test successful
- [ ] README with deployment instructions

### Rollback Strategy

**Rollback Triggers:**
- Application crashes on startup
- Database connection failures
- OCR service unreachable
- Critical bugs preventing use

**Rollback Procedure:**
1. Revert to previous Git commit
2. Redeploy previous version
3. Verify system operational
4. Investigate and fix issue offline

**Rollback Time**: ~15 minutes (Git revert + redeploy)

### Monitoring & Observability

#### Critical Metrics (Always Monitor)

**Application Metrics:**
```yaml
# Error rate
- metric: http_error_rate
  threshold: > 10%
  window: 5m
  severity: high

# Processing time
- metric: invoice_processing_duration_seconds
  threshold: > 60s
  window: 1m
  severity: medium

# OCR success rate
- metric: ocr_extraction_success_rate
  threshold: < 70%
  window: 10m
  severity: high
```

**Business Metrics:**
```yaml
# Invoices processed
- metric: invoices_processed_total
  description: Count of successfully processed invoices

# Extraction failures
- metric: extraction_failures_total
  description: Count of failed OCR extractions
```

#### Alerts

**Critical Alerts:**
- Application down (HTTP health check fails)
- Database connection lost
- OCR service unavailable

**Warning Alerts:**
- Processing time > 45s (approaching threshold)
- OCR success rate < 80% (degrading quality)
- Error rate > 5% (above normal)

#### Dashboards

**Required Dashboard Elements:**
- Total invoices processed (daily/weekly)
- Average processing time
- OCR success rate
- Error rate breakdown by type
- Database connection pool status

---

## 9. Security & Compliance

### Security Considerations

**Authentication/Authorization:**
- Changes to auth logic: **No** (no auth for training exercise)
- Public endpoints: **Yes** (for simplicity)
- Note: Production would require authentication

**Data Protection:**
- PII handling: Invoice data contains PII (client name, address)
- Logging: **MUST NOT log PII** - only log invoice keys/IDs
- Encryption: Files encrypted in transit (HTTPS)
- Storage: PostgreSQL data at rest encryption (via Render)

**Input Validation:**
- User input validated: **Yes**
  - File type validation (whitelist: PDF, PNG, JPG, JPEG)
  - File size limit (max 10MB)
  - Filename sanitization
- SQL injection prevention: Using JPA with parameterized queries
- XSS prevention: Angular auto-escapes output
- File upload vulnerabilities:
  - Content-Type validation
  - Virus scanning (nice-to-have)
  - Filename length limits

**Security Checklist:**
- [ ] File type whitelist enforced
- [ ] File size limit enforced
- [ ] No PII in application logs
- [ ] HTTPS for all communications
- [ ] Input validation on all endpoints
- [ ] Error messages don't leak sensitive info

### Compliance Impact

- GDPR impact: **Potential** - Storing client names and addresses (PII)
  - Mitigation: Add data retention policy, allow data deletion
- SOC2 impact: **No** (training exercise, non-production)
- Other compliance: **None**

---

## 10. Documentation

### Code Documentation

- [ ] Javadoc for all public methods
- [ ] README.md with setup instructions
- [ ] Architecture diagram in docs/
- [ ] OpenAPI/Swagger documentation (auto-generated)
- [ ] Database schema documentation

### Operational Documentation

- [ ] Deployment guide
- [ ] Troubleshooting guide
- [ ] Configuration guide
- [ ] API usage examples

### Knowledge Transfer

**Who needs to be informed:**
- [x] Technical Lead (for evaluation)
- [ ] Future developers (via documentation)

---

## 11. Approval & Sign-off

### Required Approvals

**Impact Level: High** (>10 days estimated, new system, critical learning exercise)
- [ ] Tech Lead: [Name] - Approval by: 2025-12-08
- [ ] Self-review complete: 2025-12-05
- **Timeout**: Must have explicit approval before implementation

### Approval Status

| Reviewer | Role | Status | Date | Comments |
|----------|------|--------|------|----------|
| Luis Espinoza | Author | Self-Approved | 2025-12-05 | Initial draft complete |
| [Tech Lead] | Reviewer | Pending | - | - |

---

## 12. Post-Implementation Review

> **IMPORTANT**: To be completed after December 22, 2025 delivery

### Deployment Results

**Deployment Date**: [TBD]
**Deployment Duration**: [TBD]
**Issues During Deployment**: [TBD]

### Validation Results

**Acceptance Criteria Met:**
- [ ] All 6 business acceptance criteria validated
- [ ] TAC and TRD reviewed by Technical Lead
- [ ] Application demo completed

### Production Metrics (First 48 Hours)

[To be filled after deployment]

### Estimation Accuracy

**Planned vs Actual:**
- Estimated effort: 64 hours (most likely)
- Actual effort: [TBD]
- Variance: [TBD]

### Lessons Learned

[To be filled after completion]

---

## 13. References

### Technical References
- Project Reference: `docs/project-reference.md`
- Business Requirements: `docs/requirements/business-acceptance-criteria.md`
- AI Usage Guide: `docs/requirements/ai-usage-expectations.md`
- Reference Architecture: `C:\Users\lespinoza\Documents\Projects\Sunburst\Risk\risk-data-state-service`

### Related Documents
- TAC Document: [To be created]
- Implementation Plan: `C:\Users\lespinoza\.claude\plans\snug-popping-sparkle.md`

---

## Appendix

### Technology Stack Details

**Backend:**
- Java 17
- Spring Boot 3.1.2
- Spring Data JPA
- PostgreSQL Driver
- Lombok
- SpringDoc OpenAPI 2.2.0
- Resilience4j 2.1.0
- JUnit 5 + Mockito

**Frontend:**
- Angular 17+
- TypeScript
- RxJS
- Angular Material (optional)

**Database:**
- PostgreSQL 15+
- Hosted on Render.com

**OCR/AI:**
- Option 1: Tesseract OCR (free, local)
- Option 2: Google Cloud Vision API (paid)
- Option 3: AWS Textract (paid)
- Option 4: OpenAI GPT-4 Vision (paid, most accurate)

### AI Tools Used

**Documentation Generation:**
- Claude 3.5 Sonnet - TRD generation from template
- Prompts: "Generate TRD for invoice extraction app using provided template"

**Code Generation (Planned):**
- Claude/ChatGPT for boilerplate code
- GitHub Copilot for in-IDE assistance

### Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-05 | Luis Espinoza | Initial TRD created using AI-assisted template |

---

**Document Status**: Draft - Pending Technical Lead Approval
**Next Review**: 2025-12-08
**Template Version**: 1.0 (Claude-Optimized)
