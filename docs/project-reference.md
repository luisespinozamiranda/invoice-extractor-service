# Project Reference - Invoice Extractor Service

## Información General del Proyecto

**Nombre:** invoice-extractor-service
**Tipo:** Proyecto de práctica - Microservicio REST
**Arquitectura:** Hexagonal (Ports & Adapters)
**Objetivo:** Replicar patrones de arquitectura hexagonal del proyecto risk-data-state-service
**Lenguaje:** Java 17
**Framework:** Spring Boot 3.1.2
**Build Tool:** Maven
**Base de Datos:** PostgreSQL

---

## Proyecto de Referencia

**Path:** `C:\Users\lespinoza\Documents\Projects\Sunburst\Risk\risk-data-state-service`
**Propósito:** Servir como plantilla arquitectónica (NO copiar lógica de negocio)

### Archivos Clave de Referencia:
- `pom.xml` - Estructura de dependencias
- `RiskDataStateServiceApplication.java` - Configuración de Spring Boot
- `ApplicationProperties.java` - Configuración personalizada
- `ErrorCodes.java` - Manejo de errores
- `RiskDataStateServiceException.java` - Excepciones personalizadas
- **Domain Layer:**
  - `ApplicantInfoModel.java` - Modelo de dominio (Java Record)
  - `IApplicantInfoService.java` - Interface de servicio
  - `ApplicantInfoService.java` - Implementación de servicio
- **Outbound Adapters:**
  - `ApplicationInformation.java` - Entidad JPA con JSONB
  - `ApplicationInformationRepository.java` - Spring Data JPA
  - `ApplicationInformationRepositoryService.java` - Servicio de repositorio
  - `StringConverterV1_0.java` - AttributeConverter para JSON
  - `RiskModelMapper.java` - Mapper Entity-Domain
- **Inbound Adapters:**
  - `ApplicantInfoControllerV1_0.java` - REST Controller
  - `ApplicantControllerServiceV1_0.java` - Controller Service
  - `IRiskModelMapperV1_0.java` - Mapper DTO-Domain

---

## Stack Tecnológico

### Core
- **Java:** 17
- **Spring Boot:** 3.1.2
- **Maven:** Build & Dependency Management

### Spring Boot Starters
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - Persistencia
- `spring-boot-starter-test` - Testing

### Database
- **PostgreSQL** (runtime)
- **Driver:** org.postgresql:postgresql

### Utilities & Libraries
- **Lombok** (optional) - Reduce boilerplate code
- **SpringDoc OpenAPI 2.2.0** - Swagger/OpenAPI documentation

### Resilience & Fault Tolerance
- **Resilience4j 2.1.0:**
  - `resilience4j-spring-boot3` - Spring Boot integration
  - `resilience4j-circuitbreaker` - Circuit Breaker pattern
  - `resilience4j-retry` - Retry mechanism
  - `resilience4j-ratelimiter` - Rate limiting

### Testing
- **JUnit Jupiter** - Testing framework
- **Mockito Core** - Mocking framework
- **Mockito JUnit Jupiter** - Mockito + JUnit integration

---

## Configuración del Proyecto

### Server
- **Puerto:** 8080
- **Context Path:** `/invoice-extractor-service`

### Base de Datos
- **URL:** `jdbc:postgresql://localhost:5432/invoicedb`
- **Usuario:** `postgres`
- **Password:** `postgres`
- **Driver:** `org.postgresql.Driver`

### JPA/Hibernate
- **DDL Auto:** `validate` (NO auto-create/update schema)
- **Show SQL:** `true` (logs SQL queries)
- **Format SQL:** `true` (pretty print SQL)
- **Open-in-View:** `false` (evita lazy loading fuera de transacción)

### Swagger/OpenAPI
- **API Docs JSON:** `/api-docs`
- **Swagger UI:** `/swagger-ui.html`
- **URL Completa:** `http://localhost:8080/invoice-extractor-service/swagger-ui.html`
- **Ordenamiento:** Métodos HTTP y tags alfabéticamente

### Logging
- **Spring:** INFO
- **Application Package:** DEBUG (`com.training.service.invoiceextractor`)

---

## Estructura de Paquetes (Hexagonal Architecture)

```
com.training.service.invoiceextractor/
├── adapter/
│   ├── inbound/
│   │   └── rest/v1_0/
│   │       ├── controller/          # REST Controllers
│   │       ├── dto/                 # REST DTOs
│   │       └── service/             # Controller Services (Inbound Adapters)
│   │           └── mappers/         # DTO ↔ Domain Mappers
│   └── outbound/
│       └── database/v1_0/
│           ├── entity/              # JPA Entities
│           ├── repository/          # Spring Data JPA Repositories
│           │   └── impl/            # Repository Service Implementations
│           │       └── mappers/     # Entity ↔ Domain Mappers
│           └── converter/           # JPA AttributeConverters
├── domain/
│   ├── model/                       # Domain Models (Java Records)
│   └── service/                     # Domain Services (Business Logic)
│       └── [Interface + Implementation]
├── configuration/                   # Spring Configuration Classes
└── utils/
    └── error/                       # Custom Exceptions & Error Codes
```

---

## Patrones de Arquitectura Implementados

### 1. Hexagonal Architecture (Ports & Adapters)
- **Domain Layer:** Lógica de negocio aislada
- **Inbound Adapters:** REST Controllers → Controller Services
- **Outbound Adapters:** Repository Services → JPA Repositories

### 2. Repository Pattern
- **Port (Interface):** `IInvoiceRepositoryService` en domain
- **Adapter (Implementation):** `InvoiceRepositoryService` en infrastructure
- **Spring Data JPA:** `InvoiceRepository extends JpaRepository`

### 3. Dependency Injection
- **Constructor-based injection** en todos los servicios
- **Programming to interfaces** (no implementaciones)

### 4. Mapper Pattern
- **Inbound:** DTO ↔ Domain Model
- **Outbound:** Domain Model ↔ Entity
- **Bidireccional:** Transformaciones en ambas direcciones

### 5. Async/Non-Blocking
- Todos los servicios retornan `CompletableFuture<T>`
- Controllers usan `@Async`
- Error handling con `exceptionally()`

### 6. Versioning
- API versionada: `/api/v1.0/...`
- Permite múltiples versiones en paralelo

---

## Convenciones de Nombres

### Packages
- `adapter.inbound.rest.v1_0` - REST adapters (versionados)
- `adapter.outbound.database.v1_0` - Database adapters (versionados)
- `domain.model` - Domain models
- `domain.service` - Domain services

### Clases
- **Interfaces de Servicio:** Prefijo `I` → `IInvoiceService`
- **Implementaciones:** Sin sufijo → `InvoiceService`
- **Controllers:** Sufijo `ControllerV1_0` → `InvoiceControllerV1_0`
- **Controller Services:** Sufijo `ControllerServiceV1_0`
- **DTOs:** Sufijo `V1_0` → `InvoiceV1_0`
- **Entities:** Sin sufijo → `Invoice`
- **Models (Domain):** Sufijo `Model` → `InvoiceModel`
- **Repositories (JPA):** Sufijo `Repository` → `InvoiceRepository`
- **Repository Services:** Sufijo `RepositoryService` → `InvoiceRepositoryService`
- **Mappers:** Sufijo `Mapper` → `InvoiceMapper`

### Métodos
- **CRUD Operations:**
  - `getByKey(UUID key)`
  - `getAll()`
  - `create(Model model)`
  - `update(UUID key, Model model)`
  - `delete(UUID key)`
- **Async:** Return `CompletableFuture<T>`

---

## Domain Model (Ejemplo: Invoice)

### Entidades Principales
1. **Invoice** - Factura principal
2. **InvoiceLineItem** - Líneas de la factura
3. **Vendor** - Proveedor/Vendedor
4. **ExtractionMetadata** - Metadata de extracción OCR

### Características de Domain Models
- **Java Records** (inmutables)
- **UUIDs** para keys de negocio
- **Sin anotaciones de infraestructura** (JPA, JSON, etc.)
- **Pure business logic**

---

## Database Schema

### Schema Name
`invoicedata`

### Tablas Esperadas (para futuras fases)
- `tb_invoice`
- `tb_invoice_line_item`
- `tb_vendor`
- `tb_extraction_metadata`

### Naming Conventions
- **Tables:** Prefijo `tb_` → `tb_invoice`
- **Columns:** snake_case → `invoice_key`, `created_at`
- **Sequences:** Sufijo `_seq` → `tb_invoice_id_seq`

### Columnas Comunes
- `id` (BIGINT, SEQUENCE) - Primary Key técnica
- `[entity]_key` (UUID) - Business Key
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### JSONB Columns
- PostgreSQL JSONB para datos flexibles
- Usar `@JdbcTypeCode(SqlTypes.JSON)` en JPA
- Converter: `StringConverterV1_0`

---

## Request Flow (Ejemplo)

```
HTTP GET /api/v1.0/invoices/{invoice_key}
    ↓
InvoiceControllerV1_0.getInvoiceByKey(UUID)
    @Async, CompletableFuture<ResponseEntity<InvoiceV1_0>>
    ↓
InvoiceControllerServiceV1_0.getInvoiceByKey(UUID)
    - Llama domain service
    - Mapea: InvoiceModel → InvoiceV1_0
    - Wrappea en ResponseEntity
    ↓
IInvoiceService.getInvoiceByKey(UUID)
    CompletableFuture<InvoiceModel>
    ↓
InvoiceService.getInvoiceByKey(UUID)
    - Business logic
    - Llama repository service
    - Error handling
    ↓
IInvoiceRepositoryService.findByInvoiceKey(UUID)
    CompletableFuture<InvoiceModel>
    ↓
InvoiceRepositoryService.findByInvoiceKey(UUID)
    - Llama JPA repository
    - Mapea: Invoice entity → InvoiceModel
    - Exception handling
    ↓
InvoiceRepository.findByInvoiceKey(UUID)
    Spring Data JPA (puede incluir @Query nativa)
    ↓
PostgreSQL Database
```

---

## Error Handling Strategy

### Custom Exception
- `InvoiceExtractorServiceException extends RuntimeException`
- Constructor con error code + message
- Constructor con ErrorCodes enum

### Error Codes Enum
```java
public enum ErrorCodes {
    ERR_INVOICE_NOT_FOUND("Invoice not found: %s", "001"),
    ERR_INVALID_INVOICE_DATA("Invalid invoice data: %s", "002"),
    // ...

    private final String errorCode;
    private final String errorMessage;
}
```

### Usage Pattern
```java
.exceptionally(ex -> {
    throw new InvoiceExtractorServiceException(
        ex, ErrorCodes.ERR_INVOICE_NOT_FOUND, invoiceKey);
});
```

---

## Testing Strategy

### Unit Tests
- **Framework:** JUnit Jupiter
- **Mocking:** Mockito
- **Pattern:** `@ExtendWith(MockitoExtension.class)`
- **Location:** `src/test/java/.../domain/service/`

### Integration Tests
- **Framework:** Spring Boot Test
- **Pattern:** `@SpringBootTest`
- **Database:** H2 in-memory (opcional)

### Test Naming
- `[ClassUnderTest]Test.java` → `InvoiceServiceTest.java`
- Métodos: `should[ExpectedBehavior]When[Condition]()`

---

## Lombok Annotations (Para usar en el futuro)

### Clases
- `@Data` - Getters, setters, toString, equals, hashCode
- `@Builder` - Builder pattern
- `@AllArgsConstructor` / `@NoArgsConstructor`
- `@Slf4j` - Logger instance

### Servicios
```java
@Service
@Slf4j
@RequiredArgsConstructor  // Constructor injection con final fields
public class InvoiceService implements IInvoiceService {
    private final IInvoiceRepositoryService repositoryService;

    @Override
    public CompletableFuture<InvoiceModel> getInvoiceByKey(UUID key) {
        log.debug("Getting invoice by key: {}", key);
        // ...
    }
}
```

---

## Resilience4j Patterns (Para usar en el futuro)

### Circuit Breaker
```java
@CircuitBreaker(name = "invoiceService", fallbackMethod = "fallbackGetInvoice")
public CompletableFuture<InvoiceModel> getInvoiceByKey(UUID key) {
    // ...
}

private CompletableFuture<InvoiceModel> fallbackGetInvoice(UUID key, Exception ex) {
    log.error("Circuit breaker fallback for invoice: {}", key, ex);
    return CompletableFuture.completedFuture(null);
}
```

### Retry
```java
@Retry(name = "invoiceService", fallbackMethod = "fallbackGetInvoice")
public CompletableFuture<InvoiceModel> getInvoiceByKey(UUID key) {
    // ...
}
```

### Rate Limiter
```java
@RateLimiter(name = "invoiceService")
public CompletableFuture<List<InvoiceModel>> getAllInvoices() {
    // ...
}
```

---

## Swagger/OpenAPI Annotations (Para usar en el futuro)

### Controller
```java
@RestController
@RequestMapping("/api/v1.0")
@Tag(name = "Invoice", description = "Invoice management endpoints")
public class InvoiceControllerV1_0 {

    @GetMapping("/invoices/{invoice_key}")
    @Operation(summary = "Get invoice by key", description = "Retrieves an invoice by its unique key")
    @ApiResponse(responseCode = "200", description = "Invoice found")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    public CompletableFuture<ResponseEntity<InvoiceV1_0>> getInvoiceByKey(
            @Parameter(description = "Invoice unique key")
            @PathVariable("invoice_key") UUID invoiceKey) {
        // ...
    }
}
```

---

## Decisiones Técnicas Importantes

| Decisión | Razón | Fecha |
|----------|-------|-------|
| Java 17 + Spring Boot 3.1.2 | Alineado con proyecto de referencia | 2025-12-05 |
| Valores directos en application.properties | Simplificar configuración para práctica | 2025-12-05 |
| Lombok desde el inicio | Reducir boilerplate en servicios y entidades | 2025-12-05 |
| Resilience4j desde el inicio | Aplicar patrones de resiliencia temprano | 2025-12-05 |
| SpringDoc OpenAPI 2.2.0 | Documentación automática de API | 2025-12-05 |
| PostgreSQL JSONB | Flexibilidad en almacenamiento de datos | 2025-12-05 |
| Async con CompletableFuture | Non-blocking I/O, escalabilidad | Por definir |
| JPA hibernate.ddl-auto=validate | Control manual de schema, seguridad | 2025-12-05 |

---

## TODO / Próximas Fases

### FASE 2: Error Handling & Utilities
- [ ] `ErrorCodes.java` enum
- [ ] `InvoiceExtractorServiceException.java`
- [ ] Global exception handler (opcional)

### FASE 3: Domain Layer
- [ ] Domain models (Java Records)
- [ ] Domain service interfaces
- [ ] Domain service implementations

### FASE 4-5: Outbound Adapters
- [ ] JPA Entities
- [ ] JPA AttributeConverter (JSON)
- [ ] Spring Data JPA Repositories
- [ ] Repository Service Interfaces (Ports)
- [ ] Repository Service Implementations
- [ ] Entity-Domain Mappers

### FASE 6-7: Inbound Adapters
- [ ] REST DTOs
- [ ] DTO-Domain Mapper Interfaces
- [ ] Controller Service Interfaces
- [ ] Controller Service Implementations
- [ ] REST Controllers

### FASE 8: Testing & Documentation
- [ ] Unit tests para domain services
- [ ] Integration tests (opcional)
- [ ] README.md
- [ ] Database schema scripts
- [ ] Dockerfile

---

## Scripts Útiles

### Compilar Proyecto
```bash
cd C:\Users\lespinoza\Documents\Projects\Training\invoice-extractor-service
mvn clean install
```

### Ejecutar Proyecto
```bash
mvn spring-boot:run
```

### Ejecutar Tests
```bash
mvn test
```

### Crear Base de Datos
```sql
CREATE DATABASE invoicedb;
```

---

## URLs Importantes

### Aplicación
- **Base URL:** `http://localhost:8080/invoice-extractor-service`
- **Swagger UI:** `http://localhost:8080/invoice-extractor-service/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/invoice-extractor-service/api-docs`

---

## Comandos Git (Para control de versión)

```bash
# Inicializar repositorio
git init

# Agregar archivos
git add .

# Commit
git commit -m "feat: initial project setup with hexagonal architecture"

# Ver estado
git status

# Ver diferencias
git diff
```

---

## Notas Importantes

1. **NO copiar lógica de negocio** del proyecto de referencia
2. **Solo replicar patrones arquitectónicos**
3. **Domain layer sin dependencias** de infraestructura (NO JPA, NO JSON, NO Spring annotations)
4. **Usar Java Records** para domain models (inmutabilidad)
5. **CompletableFuture** en todos los servicios
6. **Constructor injection** siempre
7. **Programming to interfaces** en toda la aplicación
8. **UUID para business keys**, Long/BIGINT para IDs técnicas
9. **Logging con SLF4J** (o Lombok @Slf4j)
10. **Validación en boundaries** (REST controllers, repository services)

---

## Referencias Externas

- **Spring Boot 3.1.2 Docs:** https://docs.spring.io/spring-boot/docs/3.1.2/reference/htmlsingle/
- **Spring Data JPA:** https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
- **Resilience4j:** https://resilience4j.readme.io/
- **SpringDoc OpenAPI:** https://springdoc.org/
- **Lombok:** https://projectlombok.org/
- **PostgreSQL JSONB:** https://www.postgresql.org/docs/current/datatype-json.html

---

**Última actualización:** 2025-12-05
**Plan completo:** `C:\Users\lespinoza\.claude\plans\snug-popping-sparkle.md`
