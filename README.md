# Invoice Extractor Service

A microservice for extracting invoice data from PDF and image files using Tesseract OCR and hexagonal architecture.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Tesseract](https://img.shields.io/badge/Tesseract-5.x-red)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)

---

## Features

- **OCR Extraction**: Extract invoice data from PDF and images using Tesseract OCR
- **Hexagonal Architecture**: Clean separation of concerns with ports and adapters
- **Async Operations**: Non-blocking operations with CompletableFuture
- **RESTful API**: OpenAPI 3.0 documented endpoints
- **Docker Support**: Containerized deployment with Docker Compose
- **Database Persistence**: PostgreSQL with JPA/Hibernate
- **Health Checks**: Spring Boot Actuator for monitoring

---

## Extracted Fields

- Invoice Number
- Invoice Amount
- Client Name
- Client Address

---

## Tech Stack

### Core
- **Java 17**
- **Spring Boot 3.1.2**
- **Spring Data JPA**
- **PostgreSQL 15**

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

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/invoicedb
spring.datasource.username=your_username
spring.datasource.password=your_password

# Tesseract (update path)
tesseract.datapath=C:/Program Files/Tesseract-OCR/tessdata
```

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
│   │   │   │       └── ocr/                   # Tesseract OCR
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
│  │  Domain Models (Records) ← Business Services           │ │
│  └────────────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────────┘
                            │
            ┌───────────────┴──────────────┐
            ↓                              ↓
┌───────────────────────┐      ┌─────────────────────────┐
│  DATABASE ADAPTER     │      │   OCR ADAPTER           │
│  (Outbound)           │      │   (Outbound)            │
│  ┌─────────────────┐ │      │  ┌───────────────────┐  │
│  │ Repositories    │ │      │  │ Tesseract Service │  │
│  │ JPA Entities    │ │      │  │ PDF Processor     │  │
│  │ Mappers         │ │      │  │ Image Processor   │  │
│  └─────────────────┘ │      │  └───────────────────┘  │
└───────────────────────┘      └─────────────────────────┘
           ↓                              ↓
       PostgreSQL                    Tesseract OCR
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
| `SPRING_PROFILES_ACTIVE` | Active profile | `default` |

### Tesseract Configuration

```properties
tesseract.datapath=/usr/share/tessdata
tesseract.language=eng+spa
tesseract.page-seg-mode=1
tesseract.oem-mode=3
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

**Last Updated**: 2025-12-08
**Status**: ✅ Fully Functional - REST API Ready
