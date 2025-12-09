# JavaDoc Templates - Invoice Extractor Service

Este documento contiene templates de JavaDoc para todas las capas del proyecto siguiendo las mejores prácticas de documentación Java.

---

## 1. Domain Layer - Service Classes

### Template para Clase de Servicio:

```java
/**
 * Domain service implementation for [ENTITY] business logic.
 *
 * <p>This service encapsulates all business rules and operations related to [ENTITY] management.
 * It acts as the core business logic layer in the hexagonal architecture, isolated from
 * infrastructure concerns.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Execute [ENTITY] business operations (CRUD)</li>
 *   <li>Validate business rules before persistence</li>
 *   <li>Transform exceptions into domain-specific errors</li>
 *   <li>Coordinate with repository services for data persistence</li>
 * </ul>
 *
 * <p><b>Architecture:</b> Domain Layer (Hexagonal Architecture)
 * <p><b>Thread Safety:</b> All methods are thread-safe and return {@link CompletableFuture}
 * for asynchronous processing.
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see I[ENTITY]Service
 * @see [ENTITY]Model
 * @see I[ENTITY]RepositoryService
 */
```

### Template para Métodos de Servicio:

#### Método GET:
```java
/**
 * Retrieves [ENTITY_DESCRIPTION] by [CRITERIA].
 *
 * <p>This method performs an asynchronous lookup of [ENTITY] using [CRITERIA].
 * If the [ENTITY] is not found or an error occurs, it throws a domain exception.
 *
 * @param [paramName] [param description], must not be null
 * @return a {@link CompletableFuture} containing the [ENTITY] model if found
 * @throws InvoiceExtractorServiceException with {@link ErrorCodes#[ERROR_CODE]}
 *         if the [ENTITY] does not exist or an error occurs during retrieval
 * @see [ENTITY]Model
 */
```

#### Método CREATE:
```java
/**
 * Creates a new [ENTITY] in the system.
 *
 * <p>This method validates the [ENTITY] data and persists it asynchronously.
 * Business rules are enforced before persistence.
 *
 * @param [entity] the [ENTITY] model to create, must not be null
 * @return a {@link CompletableFuture} containing the created [ENTITY] with generated key
 * @throws InvoiceExtractorServiceException with {@link ErrorCodes#DATABASE_ERROR}
 *         if the [ENTITY] cannot be created
 * @see [ENTITY]Model
 */
```

#### Método UPDATE:
```java
/**
 * Updates an existing [ENTITY] identified by [KEY].
 *
 * <p>This method first verifies the [ENTITY] exists, then applies the updates
 * while preserving immutable fields (createdAt, [ENTITY]Key).
 *
 * @param [key] the unique identifier of the [ENTITY] to update, must not be null
 * @param [entity] the updated [ENTITY] data, must not be null
 * @return a {@link CompletableFuture} containing the updated [ENTITY]
 * @throws InvoiceExtractorServiceException with {@link ErrorCodes#[ENTITY]_NOT_FOUND}
 *         if the [ENTITY] does not exist
 * @throws InvoiceExtractorServiceException with {@link ErrorCodes#DATABASE_ERROR}
 *         if the update operation fails
 * @see [ENTITY]Model
 */
```

#### Método DELETE:
```java
/**
 * Soft deletes an [ENTITY] by marking it as deleted.
 *
 * <p>This method performs a logical deletion rather than physical removal,
 * allowing for potential restoration.
 *
 * @param [key] the unique identifier of the [ENTITY] to delete, must not be null
 * @return a {@link CompletableFuture} that completes when deletion is successful
 * @throws InvoiceExtractorServiceException with {@link ErrorCodes#DATABASE_ERROR}
 *         if the deletion operation fails
 * @see #restore[ENTITY](UUID)
 */
```

---

## 2. Domain Layer - Interface Definitions

```java
/**
 * Domain service interface defining business operations for [ENTITY] management.
 *
 * <p>This interface represents the port in hexagonal architecture, defining the contract
 * for [ENTITY] business logic without exposing implementation details.
 *
 * <p><b>Design Pattern:</b> Port (Hexagonal Architecture)
 * <p><b>Async Contract:</b> All methods return {@link CompletableFuture} for non-blocking execution
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see [ENTITY]Model
 */
```

---

## 3. Outbound Adapters - Repository Service Implementation

### Template para Clase:

```java
/**
 * Repository service implementation for [ENTITY] data persistence.
 *
 * <p>This service acts as an outbound adapter in the hexagonal architecture,
 * translating domain operations into database operations through JPA repositories.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Translate domain models to JPA entities and vice versa</li>
 *   <li>Execute database operations asynchronously</li>
 *   <li>Map database exceptions to domain exceptions</li>
 *   <li>Maintain data integrity and consistency</li>
 * </ul>
 *
 * <p><b>Architecture:</b> Outbound Adapter (Hexagonal Architecture)
 * <p><b>Persistence:</b> PostgreSQL via Spring Data JPA
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see I[ENTITY]RepositoryService
 * @see [ENTITY]Repository
 * @see [ENTITY]
 */
```

### Template para Métodos:

```java
/**
 * Finds [ENTITY_DESCRIPTION] by [CRITERIA] from the database.
 *
 * <p>Executes an asynchronous query to retrieve the [ENTITY] entity,
 * converts it to a domain model, and returns the result.
 *
 * @param [param] the [param description] to search for
 * @return a {@link CompletableFuture} containing the [ENTITY] model if found
 * @throws InvoiceExtractorServiceException if database operation fails
 */
```

---

## 4. Inbound Adapters - Controller Service Implementation

### Template para Clase:

```java
/**
 * Controller service implementation for [ENTITY] REST operations (API v1.0).
 *
 * <p>This service acts as an inbound adapter in the hexagonal architecture,
 * translating REST DTOs to domain models and vice versa.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Transform REST DTOs to domain models</li>
 *   <li>Transform domain models to REST DTOs</li>
 *   <li>Delegate business logic to domain services</li>
 *   <li>Construct appropriate HTTP responses</li>
 * </ul>
 *
 * <p><b>Architecture:</b> Inbound Adapter (Hexagonal Architecture)
 * <p><b>Exception Handling:</b> Delegated to {@link GlobalExceptionHandler}
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see I[ENTITY]ControllerServiceV1_0
 * @see [ENTITY]V1_0
 * @see I[ENTITY]Service
 */
```

### Template para Métodos:

```java
/**
 * Retrieves [ENTITY_DESCRIPTION] by [CRITERIA] and returns as REST DTO.
 *
 * <p>This method delegates to the domain service, then transforms the result
 * into a versioned REST DTO wrapped in a ResponseEntity.
 *
 * @param [param] the [param description]
 * @return a {@link CompletableFuture} containing {@link ResponseEntity} with [ENTITY] DTO
 * @see [ENTITY]V1_0
 * @see I[ENTITY]Service#[methodName]
 */
```

---

## 5. REST Controllers

### Template para Clase:

```java
/**
 * REST Controller for [ENTITY] operations (API v1.0).
 *
 * <p>This controller exposes RESTful endpoints for [ENTITY] management,
 * following REST best practices and HTTP semantics.
 *
 * <p><b>Base Path:</b> {@code /api/v1.0/[entities]}
 *
 * <p><b>Endpoints:</b>
 * <ul>
 *   <li>GET /{key} - Retrieve [ENTITY] by key</li>
 *   <li>GET / - List all [entities]</li>
 *   <li>POST / - Create new [ENTITY]</li>
 *   <li>PUT /{key} - Update existing [ENTITY]</li>
 *   <li>DELETE /{key} - Delete [ENTITY]</li>
 * </ul>
 *
 * <p><b>Architecture:</b> REST Layer (Inbound Adapter)
 * <p><b>API Version:</b> 1.0
 * <p><b>Async:</b> All operations are non-blocking using {@link CompletableFuture}
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see I[ENTITY]ControllerServiceV1_0
 * @see [ENTITY]V1_0
 */
```

### Template para Métodos:

```java
/**
 * [HTTP_METHOD] [Description of operation].
 *
 * <p><b>HTTP Method:</b> [GET/POST/PUT/DELETE]
 * <p><b>Path:</b> {@code [path]}
 * <p><b>Status:</b> [200 OK / 201 CREATED / 204 NO_CONTENT]
 *
 * @param [param] [description]
 * @return {@link CompletableFuture} containing {@link ResponseEntity} with [response description]
 * @see I[ENTITY]ControllerServiceV1_0#[methodName]
 */
```

**Ejemplo completo:**
```java
/**
 * GET Retrieves an invoice by its unique key.
 *
 * <p><b>HTTP Method:</b> GET
 * <p><b>Path:</b> {@code /api/v1.0/invoices/{invoice_key}}
 * <p><b>Status:</b> 200 OK if found, 404 NOT_FOUND otherwise
 *
 * @param invoiceKey the UUID of the invoice to retrieve
 * @return {@link CompletableFuture} containing {@link ResponseEntity} with invoice DTO
 * @see IInvoiceControllerServiceV1_0#getInvoiceByKey(UUID)
 */
```

---

## 6. Domain Models (Java Records)

```java
/**
 * Domain model representing [ENTITY_DESCRIPTION].
 *
 * <p>This immutable record encapsulates all [ENTITY] data within the domain layer.
 * As a Java record, it automatically provides equals(), hashCode(), and toString().
 *
 * <p><b>Immutability:</b> All fields are final and the record is immutable
 * <p><b>Layer:</b> Domain (Hexagonal Architecture)
 *
 * @param [field1] [description of field1]
 * @param [field2] [description of field2]
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 */
```

---

## 7. DTOs (Data Transfer Objects)

```java
/**
 * REST DTO for [ENTITY] representation (API v1.0).
 *
 * <p>This DTO is used for REST API communication, using snake_case JSON serialization.
 * It represents the external contract of the [ENTITY] resource.
 *
 * <p><b>JSON Format:</b> snake_case (e.g., invoice_number)
 * <p><b>API Version:</b> 1.0
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see [ENTITY]Model
 */
```

---

## 8. Exceptions

```java
/**
 * Custom exception for [ENTITY] service errors.
 *
 * <p>This exception wraps all errors that occur within the [ENTITY] service,
 * providing structured error codes and messages for client communication.
 *
 * <p><b>Error Codes:</b> See {@link ErrorCodes} for all possible error scenarios
 * <p><b>HTTP Mapping:</b> Automatically mapped to appropriate HTTP status by
 * {@link GlobalExceptionHandler}
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see ErrorCodes
 * @see GlobalExceptionHandler
 */
```

---

## 9. Configuration Classes

```java
/**
 * [Configuration description].
 *
 * <p>This configuration class sets up [what it configures] for the application.
 *
 * <p><b>Spring Profile:</b> [profile or "all profiles"]
 * <p><b>Configuration Type:</b> [Bean definitions / Property bindings / etc.]
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 */
```

---

## 10. Mappers

```java
/**
 * Mapper for converting between [SOURCE] and [TARGET].
 *
 * <p>This mapper provides bidirectional conversion between [layer1] and [layer2],
 * maintaining proper separation of concerns in the hexagonal architecture.
 *
 * <p><b>Conversions:</b>
 * <ul>
 *   <li>[SOURCE] → [TARGET]</li>
 *   <li>[TARGET] → [SOURCE]</li>
 * </ul>
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see [SOURCE]
 * @see [TARGET]
 */
```

---

## Mejores Prácticas Aplicadas

### ✅ Estructura Completa
- **Resumen**: Breve descripción de una línea
- **Descripción detallada**: Párrafo con contexto adicional
- **Responsabilidades**: Lista de responsabilidades clave
- **Metadatos**: @author, @version, @since
- **Enlaces**: @see para clases relacionadas

### ✅ Tags JavaDoc Estándar
- `@param` - Descripción de parámetros
- `@return` - Descripción del valor de retorno
- `@throws` - Excepciones que puede lanzar
- `@see` - Referencias a clases/métodos relacionados
- `@since` - Versión de introducción
- `@deprecated` - Si aplica
- `@author` - Autor del código
- `@version` - Versión actual

### ✅ HTML Tags Permitidos
- `<p>` - Párrafos
- `<b>` - Negrita
- `<ul>`, `<li>` - Listas
- `<code>` - Código inline
- `{@link}` - Enlaces a otras clases
- `{@code}` - Código formateado

### ✅ Contexto Arquitectónico
Cada clase documenta su rol en la arquitectura hexagonal:
- **Domain Layer**: Lógica de negocio
- **Inbound Adapter**: REST → Domain
- **Outbound Adapter**: Domain → Database
- **Port**: Interfaces que definen contratos

---

## Ejemplo Completo: InvoiceService

```java
/**
 * Domain service implementation for invoice business logic.
 *
 * <p>This service encapsulates all business rules and operations related to invoice management.
 * It acts as the core business logic layer in the hexagonal architecture, isolated from
 * infrastructure concerns.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Execute invoice business operations (CRUD)</li>
 *   <li>Validate business rules before persistence</li>
 *   <li>Transform exceptions into domain-specific errors</li>
 *   <li>Coordinate with repository services for data persistence</li>
 * </ul>
 *
 * <p><b>Architecture:</b> Domain Layer (Hexagonal Architecture)
 * <p><b>Thread Safety:</b> All methods are thread-safe and return {@link CompletableFuture}
 * for asynchronous processing.
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-08
 * @see IInvoiceService
 * @see InvoiceModel
 * @see IInvoiceRepositoryService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService implements IInvoiceService {

    /**
     * Retrieves an invoice by its unique key.
     *
     * <p>This method performs an asynchronous lookup of an invoice using its UUID key.
     * If the invoice is not found or an error occurs, it throws a domain exception.
     *
     * @param invoiceKey the unique identifier of the invoice, must not be null
     * @return a {@link CompletableFuture} containing the invoice model if found
     * @throws InvoiceExtractorServiceException with {@link ErrorCodes#INVOICE_NOT_FOUND}
     *         if the invoice does not exist or an error occurs during retrieval
     * @see InvoiceModel
     */
    @Override
    public CompletableFuture<InvoiceModel> getInvoiceByKey(UUID invoiceKey) {
        // Implementation
    }
}
```

---

**Fecha:** 2025-12-09
**Autor:** Luis Espinoza
**Estado:** ✅ Template Completo para Todas las Capas
