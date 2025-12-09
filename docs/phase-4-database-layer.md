# Phase 4: Database Layer (Outbound Adapter) - COMPLETED

## Overview
Esta fase implementa la capa de persistencia del servicio usando JPA y PostgreSQL, siguiendo el patrón de arquitectura hexagonal.

**Status:** ✅ COMPLETED
**Compilation:** ✅ BUILD SUCCESS (19 source files)
**Duration:** ~4 hours

---

## Archivos Creados

### 1. JPA Entities (adapter/outbound/database/v1_0/entity/)

#### **Invoice.java**
- **Purpose:** Entidad JPA para la tabla `tb_invoice`
- **Annotations:** `@Entity`, `@Table(schema = "invoicedata")`, `@Data`, `@Builder`
- **Key Features:**
  - Mapeo completo de los 4 campos extraídos: invoice_number, invoice_amount, client_name, client_address
  - Campo `isDeleted` para soft delete
  - Callbacks `@PrePersist` y `@PreUpdate` para manejo de timestamps
  - Generación automática de UUID
- **Location:** [Invoice.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/entity/Invoice.java)

#### **ExtractionMetadata.java**
- **Purpose:** Entidad JPA para la tabla `tb_extraction_metadata`
- **Annotations:** `@Entity`, `@Table(schema = "invoicedata")`, `@Data`, `@Builder`
- **Key Features:**
  - Campo JSONB (`extraction_data`) con `@JdbcTypeCode(SqlTypes.JSON)`
  - Soporte para metadata de extracción OCR
  - Soft delete support
- **Location:** [ExtractionMetadata.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/entity/ExtractionMetadata.java)

---

### 2. Spring Data JPA Repositories (adapter/outbound/database/v1_0/repository/)

#### **InvoiceRepository.java**
- **Extends:** `JpaRepository<Invoice, Long>`
- **Métodos Implementados:**
  - `findByInvoiceKey(UUID)` - Buscar por business key
  - `findByInvoiceKeyAndIsDeletedFalse(UUID)` - Buscar activos
  - `findByIsDeletedFalse()` - Listar todos los activos
  - `findByInvoiceNumberAndIsDeletedFalse(String)` - Buscar por número
  - `findByClientNameContainingIgnoreCaseAndIsDeletedFalse(String)` - Buscar por nombre de cliente
  - `findByStatusAndIsDeletedFalse(String)` - Buscar por status
  - `existsByInvoiceKeyAndIsDeletedFalse(UUID)` - Verificar existencia
- **Location:** [InvoiceRepository.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/InvoiceRepository.java)

#### **ExtractionMetadataRepository.java**
- **Extends:** `JpaRepository<ExtractionMetadata, Long>`
- **Métodos Implementados:**
  - `findByExtractionKey(UUID)`
  - `findByExtractionKeyAndIsDeletedFalse(UUID)`
  - `findByInvoiceKeyAndIsDeletedFalse(UUID)`
  - `findByExtractionStatusAndIsDeletedFalse(String)`
  - `findByIsDeletedFalse()`
  - `findByConfidenceScoreLessThanAndIsDeletedFalse(Double)`
- **Location:** [ExtractionMetadataRepository.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/ExtractionMetadataRepository.java)

---

### 3. Repository Service Interfaces - Ports (adapter/outbound/database/v1_0/repository/)

#### **IInvoiceRepositoryService.java**
- **Purpose:** Contrato entre dominio y persistencia (Port)
- **Key Features:**
  - Todos los métodos retornan `CompletableFuture<T>` para operaciones async
  - Trabaja con `InvoiceModel` (dominio), no con entidades JPA
  - Operaciones: find, save, update, softDelete, restore
- **Location:** [IInvoiceRepositoryService.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/IInvoiceRepositoryService.java)

#### **IExtractionMetadataRepositoryService.java**
- **Purpose:** Contrato para metadata de extracción (Port)
- **Key Features:**
  - Async operations con `CompletableFuture`
  - Trabaja con `ExtractionMetadataModel`
  - Operaciones: find, save, update, softDelete, findLowConfidence
- **Location:** [IExtractionMetadataRepositoryService.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/IExtractionMetadataRepositoryService.java)

---

### 4. Entity-Domain Mappers (adapter/outbound/database/v1_0/repository/impl/mappers/)

#### **InvoiceMapper.java**
- **Purpose:** Conversión bidireccional entre `Invoice` (entity) y `InvoiceModel` (domain)
- **Métodos:**
  - `entityToModel(Optional<Invoice>)` - Entity → Model
  - `entityToModel(Invoice)` - Entity → Model (non-optional)
  - `modelToEntity(Optional<InvoiceModel>)` - Model → Entity
  - `modelToEntity(InvoiceModel)` - Model → Entity (non-optional)
  - `updateEntityFromModel(Invoice, InvoiceModel)` - Update existing entity
- **Key Features:**
  - Null safety usando Optional
  - Custom exceptions para errores
- **Location:** [InvoiceMapper.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/impl/mappers/InvoiceMapper.java)

#### **ExtractionMetadataMapper.java**
- **Purpose:** Conversión bidireccional para extraction metadata
- **Key Features:**
  - Manejo de conversión BigDecimal ↔ Double para confidence score
  - Null safety
  - Métodos similares a InvoiceMapper
- **Location:** [ExtractionMetadataMapper.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/impl/mappers/ExtractionMetadataMapper.java)

---

### 5. Repository Service Implementations - Adapters (adapter/outbound/database/v1_0/repository/impl/)

#### **InvoiceRepositoryService.java**
- **Implements:** `IInvoiceRepositoryService`
- **Annotations:** `@Service`, `@Slf4j`, `@Transactional`
- **Dependencies:** `InvoiceRepository`, `InvoiceMapper`
- **Key Features:**
  - Todas las operaciones son async usando `CompletableFuture.supplyAsync()`
  - Transactional support (`@Transactional(readOnly = true)` para queries)
  - Comprehensive error handling y logging
  - Soft delete implementation (update `isDeleted` flag)
- **Métodos Implementados:** 11 métodos (find, save, update, softDelete, restore, etc.)
- **Location:** [InvoiceRepositoryService.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/impl/InvoiceRepositoryService.java)

#### **ExtractionMetadataRepositoryService.java**
- **Implements:** `IExtractionMetadataRepositoryService`
- **Annotations:** `@Service`, `@Slf4j`, `@Transactional`
- **Dependencies:** `ExtractionMetadataRepository`, `ExtractionMetadataMapper`
- **Key Features:**
  - Async operations
  - Transactional support
  - Error handling y logging
  - Soft delete support
- **Métodos Implementados:** 8 métodos
- **Location:** [ExtractionMetadataRepositoryService.java](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/impl/ExtractionMetadataRepositoryService.java)

---

## Patrón de Arquitectura Implementado

```
┌─────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                              │
│  ┌──────────────────┐         ┌─────────────────────────┐  │
│  │  InvoiceModel    │         │  IInvoiceService        │  │
│  │  (Java Record)   │         │  (Domain Service)       │  │
│  └──────────────────┘         └─────────────────────────┘  │
│                                         ↓                    │
│                                 Uses Port Interface          │
│                     ┌───────────────────────────────────┐   │
│                     │  IInvoiceRepositoryService (PORT) │   │
│                     └───────────────────────────────────┘   │
└─────────────────────────────────────────┬───────────────────┘
                                          │
                            Implementado por Adapter
                                          ↓
┌─────────────────────────────────────────────────────────────┐
│                 ADAPTER LAYER (Outbound)                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  InvoiceRepositoryService (ADAPTER)                  │  │
│  │  - Implements: IInvoiceRepositoryService             │  │
│  │  - Uses: InvoiceRepository, InvoiceMapper           │  │
│  │  - Converts: InvoiceModel ↔ Invoice                 │  │
│  │  - Async: CompletableFuture operations              │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  InvoiceMapper (Bidirectional Conversion)            │  │
│  │  - entityToModel(): Invoice → InvoiceModel           │  │
│  │  - modelToEntity(): InvoiceModel → Invoice           │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  InvoiceRepository (Spring Data JPA)                 │  │
│  │  - Extends: JpaRepository<Invoice, Long>            │  │
│  │  - Custom query methods with is_deleted filter      │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Invoice (JPA Entity)                                │  │
│  │  - @Entity, @Table(schema = "invoicedata")          │  │
│  │  - Maps to: tb_invoice                              │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────┬───────────────────┘
                                          │
                                          ↓
                                    PostgreSQL DB
                               (invoicedata.tb_invoice)
```

---

## Características Clave Implementadas

### ✅ 1. Arquitectura Hexagonal (Ports & Adapters)
- **Port:** `IInvoiceRepositoryService` define el contrato
- **Adapter:** `InvoiceRepositoryService` implementa el contrato
- **Beneficio:** El dominio no depende de JPA o PostgreSQL

### ✅ 2. Soft Delete Pattern
- Campo `is_deleted` en todas las entidades
- Queries filtran automáticamente por `is_deleted = FALSE`
- Métodos `softDelete()` y `restore()` para manejo lógico

### ✅ 3. Async Operations
- Todos los métodos retornan `CompletableFuture<T>`
- Uso de `CompletableFuture.supplyAsync()` y `runAsync()`
- Non-blocking operations

### ✅ 4. Transactional Support
- `@Transactional(readOnly = true)` para queries
- `@Transactional` para writes
- Optimización de performance

### ✅ 5. Comprehensive Error Handling
- Custom exceptions: `InvoiceExtractorServiceException`
- Error codes específicos: `INVOICE_NOT_FOUND`, `EXTRACTION_NOT_FOUND`, `DATABASE_ERROR`
- Logging con SLF4J en todos los puntos críticos

### ✅ 6. Entity-Domain Separation
- **JPA Entity:** `Invoice` (infraestructura)
- **Domain Model:** `InvoiceModel` (negocio)
- **Mapper:** Conversión bidireccional con null safety

---

## Compilación y Warnings

### ✅ Build Status
```
BUILD SUCCESS
Total time:  3.760 s
19 source files compiled
```

### ✅ Lombok Improvements Applied
**Refactorings completed:**

1. **Entity Classes** - Added `@Builder.Default` annotation:
   - [Invoice.java:53](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/entity/Invoice.java#L53) - `currency` field
   - [Invoice.java:70](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/entity/Invoice.java#L70) - `isDeleted` field
   - [ExtractionMetadata.java:67](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/entity/ExtractionMetadata.java#L67) - `isDeleted` field

2. **Repository Services** - Replaced manual constructors with `@RequiredArgsConstructor`:
   - [InvoiceRepositoryService.java:29](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/impl/InvoiceRepositoryService.java#L29)
   - [ExtractionMetadataRepositoryService.java:28](../src/main/java/com/training/service/invoiceextractor/adapter/outbound/database/v1_0/repository/impl/ExtractionMetadataRepositoryService.java#L28)

**Benefits:**
- Eliminated Lombok builder warnings
- Reduced boilerplate code (removed manual constructors and `@Autowired` annotations)
- Default values now work correctly with builder pattern
- Cleaner, more concise code following Lombok best practices

---

## Next Steps: Phase 5 - REST Layer (Inbound Adapter)

Ahora que la capa de persistencia está completa, el siguiente paso es crear los endpoints REST:

### **5.1 Create REST DTOs** (adapter/inbound/rest/v1_0/dto/)
- `InvoiceV1_0.java` - DTO para requests/responses
- `ExtractionRequestV1_0.java` - DTO para upload de archivos
- `ExtractionResponseV1_0.java` - DTO para resultados de extracción

### **5.2 Create DTO-Domain Mappers**
- `IInvoiceMapperV1_0.java` - Conversión DTO ↔ Domain Model

### **5.3 Create Controller Service Interfaces**
- `IInvoiceControllerServiceV1_0.java` - Contratos para controllers

### **5.4 Implement Controller Services**
- `InvoiceControllerServiceV1_0.java` - Lógica de adaptación REST → Domain

### **5.5 Create REST Controllers**
- `InvoiceControllerV1_0.java` - Endpoints REST con `@RestController`
- `ExtractionControllerV1_0.java` - Endpoints para extracción OCR

---

**Última actualización:** 2025-12-08
**Phase Status:** ✅ COMPLETED
**Next Phase:** Phase 5 - REST Layer
