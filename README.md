# Invoice Extractor Service

A microservice for extracting invoice data from PDF and image files using Tesseract OCR, Groq LLM, and hexagonal architecture.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Tesseract](https://img.shields.io/badge/Tesseract-5.x-red)
![Groq](https://img.shields.io/badge/Groq-Llama%203.1-purple)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)

---

## Features

- **Intelligent LLM Extraction**: Uses Groq's Llama 3.1 70B model for accurate invoice data extraction
- **OCR Processing**: Tesseract OCR for text extraction from PDF and images
- **Dual Extraction Strategy**: LLM-first approach with regex fallback for reliability
- **Hexagonal Architecture**: Clean separation of concerns with ports and adapters
- **Provider-Agnostic Design**: Easy to switch between LLM providers (Groq, OpenAI, etc.)
- **Optional Pattern**: Explicit null handling for missing invoice fields
- **Async Operations**: Non-blocking operations with CompletableFuture
- **RESTful API**: OpenAPI 3.0 documented endpoints
- **Docker Support**: Containerized deployment with Docker Compose
- **Database Persistence**: PostgreSQL with JPA/Hibernate
- **Health Checks**: Spring Boot Actuator for monitoring

---

## Extracted Fields

The service extracts the following fields from invoices using LLM intelligence:

- **Invoice Number**: Document/invoice identifier
- **Invoice Amount**: Total amount with currency
- **Client Name**: Bill To / Customer name
- **Client Address**: Full customer address
- **Currency**: Currency code (USD, EUR, MXN, etc.)
- **Confidence Score**: LLM extraction confidence (0.0 - 1.0)

Fields use `Optional<T>` pattern - returns `null` when extraction is not possible.

---

## Tech Stack

### Core
- **Java 17**
- **Spring Boot 3.1.2**
- **Spring Data JPA**
- **PostgreSQL 15**

### LLM & AI Processing
- **Groq API** (Llama 3.1 70B model)
- **OkHttp 4.12.0** (HTTP client for API calls)
- **Jackson** (JSON parsing)

### OCR & File Processing
- **Tesseract 5.x** (via Tess4j)
- **Apache PDFBox** (PDF handling)

### Infrastructure
- **Docker & Docker Compose**
- **Lombok** (boilerplate reduction)
- **SpringDoc OpenAPI** (API documentation)
- **Resilience4j** (fault tolerance)

---

## Quick Start with Docker

### Prerequisites
- Docker Desktop or Docker Engine 20.10+
- Docker Compose 2.0+

### Run with Docker Compose

```bash
# Clone repository
cd invoice-extractor-service

# Start all services (API + PostgreSQL)
docker-compose up --build

# Or run in background
docker-compose up --build -d
```

### Access Services

- **Swagger UI**: http://localhost:8080/invoice-extractor-service/swagger-ui.html
- **API Health**: http://localhost:8080/invoice-extractor-service/actuator/health
- **PostgreSQL**: localhost:5432 (user: `postgres`, pass: `postgres123`)

### Stop Services

```bash
# Stop containers
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## Local Development Setup

### Prerequisites
- **Java 17** JDK
- **Maven 3.8+**
- **PostgreSQL 15+**
- **Tesseract OCR** ([Download](https://github.com/tesseract-ocr/tesseract))

### Install Tesseract

**Windows**:
```powershell
# Download from: https://github.com/UB-Mannheim/tesseract/wiki
# Or via Chocolatey:
choco install tesseract
```

**macOS**:
```bash
brew install tesseract tesseract-lang
```

**Linux (Ubuntu/Debian)**:
```bash
sudo apt-get update
sudo apt-get install tesseract-ocr tesseract-ocr-eng tesseract-ocr-spa
```

### Database Setup

```sql
-- Create database
CREATE DATABASE invoicedb;

-- Run schema
\i src/main/resources/db/schema.sql

-- Load sample data (optional)
\i src/main/resources/db/sample-data.sql
```

### Application Configuration

Edit `src/main/resources/application-local.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/invoicedb
spring.datasource.username=your_username
spring.datasource.password=your_password

# Tesseract (update path)
ocr.tesseract.datapath=C:/Program Files/Tesseract-OCR/tessdata
ocr.tesseract.language=eng
ocr.tesseract.dpi=300

# Groq LLM Configuration
llm.groq.enabled=true
llm.groq.api-key=your_groq_api_key_here
llm.groq.model=llama-3.1-70b-versatile
```

**Get a free Groq API key**: https://console.groq.com/keys

### Build and Run

```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run

# Or run JAR
java -jar target/invoice-extractor-service-0.0.1-SNAPSHOT.jar
```

---

## Project Structure

```
invoice-extractor-service/
├── src/
│   ├── main/
│   │   ├── java/com/training/service/invoiceextractor/
│   │   │   ├── adapter/
│   │   │   │   ├── inbound/rest/v1_0/        # REST Controllers
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── service/
│   │   │   │   └── outbound/
│   │   │   │       ├── database/v1_0/         # JPA Repositories
│   │   │   │       │   ├── entity/
│   │   │   │       │   ├── repository/
│   │   │   │       │   └── converter/
│   │   │   │       ├── llm/v1_0/              # LLM Integration
│   │   │   │       │   ├── impl/              # Groq Implementation
│   │   │   │       │   ├── InvoiceData.java
│   │   │   │       │   └── ILlmExtractionService.java
│   │   │   │       └── ocr/v1_0/              # Tesseract OCR
│   │   │   ├── domain/
│   │   │   │   ├── model/                     # Domain Models
│   │   │   │   └── service/                   # Business Logic
│   │   │   ├── configuration/                 # Spring Configuration
│   │   │   └── utils/
│   │   │       └── error/                     # Error Handling
│   │   └── resources/
│   │       ├── db/
│   │       │   ├── schema.sql
│   │       │   └── sample-data.sql
│   │       ├── application.properties
│   │       └── application-docker.properties
│   └── test/                                  # Unit Tests
├── docs/                                      # Documentation
│   ├── phase-4-database-layer.md
│   └── docker-setup.md
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── pom.xml
└── README.md
```

---

## API Documentation

### Swagger UI

Access interactive API documentation at:
```
http://localhost:8080/invoice-extractor-service/swagger-ui.html
```

### Main Endpoints

**Upload and Extract Invoice**:
```http
POST /api/v1.0/invoices/extract
Content-Type: multipart/form-data

file: [invoice.pdf or invoice.png]
```

**Get Invoice by Key**:
```http
GET /api/v1.0/invoices/{invoice_key}
```

**List All Invoices**:
```http
GET /api/v1.0/invoices
```

**Search by Client Name**:
```http
GET /api/v1.0/invoices/search?clientName=ACME
```

---

## Architecture

### Hexagonal Architecture (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────────┐
│                     REST LAYER (Inbound)                     │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Controllers → Controller Services → DTO Mappers       │ │
│  └────────────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Domain Models (Records) ← Extraction Service          │ │
│  │  (InvoiceModel, ExtractionMetadataModel)               │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────┬───────────────────────────────────────────┘
                  │
      ┌───────────┴───────────┬────────────────┐
      ↓                       ↓                ↓
┌──────────────┐    ┌──────────────────┐    ┌──────────────┐
│  DATABASE    │    │  LLM ADAPTER     │    │  OCR ADAPTER │
│  ADAPTER     │    │  (Outbound)      │    │  (Outbound)  │
│  (Outbound)  │    │ ┌──────────────┐ │    │ ┌──────────┐ │
│ ┌──────────┐ │    │ │ ILlmExtract  │ │    │ │Tesseract │ │
│ │Repository│ │    │ │   Service    │ │    │ │ Service  │ │
│ │JPA Entity│ │    │ │   (Port)     │ │    │ │   PDF    │ │
│ │ Mappers  │ │    │ └──────┬───────┘ │    │ │  Image   │ │
│ └──────────┘ │    │        ↓         │    │ └──────────┘ │
└──────┬───────┘    │ ┌──────────────┐ │    └──────┬───────┘
       ↓            │ │ GroqLlmService│ │           ↓
   PostgreSQL       │ │  (Adapter)    │ │    Tesseract OCR
                    │ └──────┬────────┘ │
                    └────────┼──────────┘
                             ↓
                      Groq API (Llama 3.1)
```

### Extraction Strategy

```
1. Upload Invoice (PDF/Image)
        ↓
2. OCR Extraction (Tesseract) → Raw Text
        ↓
3. LLM Processing (Groq) → Structured Data
   ├─ Success: Return InvoiceData with Optional fields
   └─ Failure: Fallback to Regex patterns
        ↓
4. Save to Database (PostgreSQL)
```

---

## Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Test with Docker

```bash
# Run tests in container
docker-compose run invoice-extractor-service mvn test
```

---

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATASOURCE_URL` | PostgreSQL connection string | `jdbc:postgresql://localhost:5432/invoicedb` |
| `DATASOURCE_USERNAME` | Database username | `postgres` |
| `DATASOURCE_PASSWORD` | Database password | - |
| `TESSDATA_PREFIX` | Tesseract data path | `/usr/share/tessdata` |
| `LLM_ENABLED` | Enable/disable LLM extraction | `true` |
| `GROQ_API_KEY` | Groq API key for LLM | - |
| `SPRING_PROFILES_ACTIVE` | Active profile | `docker` |

### Groq LLM Configuration

```properties
# Enable LLM extraction
llm.groq.enabled=true

# Your Groq API key (get free at https://console.groq.com/keys)
llm.groq.api-key=your_api_key_here

# Model to use
llm.groq.model=llama-3.1-70b-versatile
```

### Tesseract Configuration

```properties
ocr.tesseract.datapath=/usr/share/tessdata
ocr.tesseract.language=eng
ocr.tesseract.dpi=300
```

---

## Monitoring

### Health Check

```bash
curl http://localhost:8080/invoice-extractor-service/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/invoice-extractor-service/actuator/metrics
```

---

## Troubleshooting

### Tesseract not found

**Verify installation**:
```bash
tesseract --version
```

**Docker**: Tesseract is pre-installed in the container.

### Database connection failed

**Check PostgreSQL is running**:
```bash
# Docker
docker-compose ps postgres

# Local
pg_isready -h localhost -p 5432
```

### Port already in use

**Change port** in `docker-compose.yml` or `application.properties`:
```yaml
ports:
  - "8081:8080"  # Use 8081 instead of 8080
```

---

## Contributing

This is a practice/training project implementing hexagonal architecture patterns.

---

## Documentation

- **Docker Setup**: [docs/docker-setup.md](docs/docker-setup.md)
- **Phase 4 - Database Layer**: [docs/phase-4-database-layer.md](docs/phase-4-database-layer.md)

---

## License

This is a training project for educational purposes.

---

## Contact

Luis Espinoza - Practice Project

---

**Last Updated**: 2025-12-09
**Status**: ✅ Fully Functional - REST API with LLM Intelligence
