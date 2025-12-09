# Docker Setup - Invoice Extractor Service

## Overview

This document describes the Docker setup for the invoice-extractor-service, including Tesseract OCR integration and PostgreSQL database.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Compose                           │
│                                                              │
│  ┌────────────────────────┐    ┌──────────────────────────┐│
│  │  invoice-extractor-api  │────│  postgres (invoicedb)   ││
│  │  (Spring Boot + Tess4j) │    │  (PostgreSQL 15)        ││
│  │  Port: 8080             │    │  Port: 5432             ││
│  └────────────────────────┘    └──────────────────────────┘│
│           │                              │                  │
│           ├─ Tesseract OCR               ├─ Init Scripts   │
│           ├─ Java 17 JRE                 ├─ Data Volume    │
│           └─ Upload Volume                └─ Network        │
└─────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

- **Docker Desktop** or **Docker Engine** 20.10+
- **Docker Compose** 2.0+
- **4GB RAM** minimum available for containers
- **10GB disk space** for images and volumes

---

## Quick Start

### 1. Build and Run with Docker Compose

```bash
# Clone or navigate to project directory
cd invoice-extractor-service

# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d
```

### 2. Verify Services

**Check running containers:**
```bash
docker-compose ps
```

**Expected output:**
```
NAME                      STATUS        PORTS
invoice-extractor-api     Up            0.0.0.0:8080->8080/tcp
invoice-extractor-db      Up            0.0.0.0:5432->5432/tcp
```

**Health checks:**
```bash
# API Health
curl http://localhost:8080/invoice-extractor-service/actuator/health

# Database Health
docker-compose exec postgres pg_isready -U postgres
```

### 3. Access Services

- **Swagger UI**: http://localhost:8080/invoice-extractor-service/swagger-ui.html
- **API Docs**: http://localhost:8080/invoice-extractor-service/api-docs
- **Health Endpoint**: http://localhost:8080/invoice-extractor-service/actuator/health
- **Metrics**: http://localhost:8080/invoice-extractor-service/actuator/metrics
- **PostgreSQL**: localhost:5432 (user: `postgres`, password: `postgres123`)

---

## Docker Images

### invoice-extractor-api

**Base Image**: `eclipse-temurin:17-jre-alpine`

**Installed Components**:
- Tesseract OCR 5.x
- English and Spanish language data
- Ghostscript (for PDF processing)
- DejaVu fonts

**Build Process** (Multi-stage):
1. **Stage 1** (Builder): Maven build with dependencies caching
2. **Stage 2** (Runtime): Minimal JRE with Tesseract

**Environment Variables**:
- `SPRING_PROFILES_ACTIVE=docker`
- `DATASOURCE_URL`: PostgreSQL connection string
- `DATASOURCE_USERNAME`: Database user
- `DATASOURCE_PASSWORD`: Database password
- `TESSDATA_PREFIX=/usr/share/tessdata`

### postgres

**Image**: `postgres:15-alpine`

**Features**:
- Automatic schema initialization via init scripts
- Data persistence with Docker volume
- Health checks enabled

---

## Configuration

### Environment Variables

Edit `docker-compose.yml` to customize:

```yaml
environment:
  # Database Connection
  DATASOURCE_URL: jdbc:postgresql://postgres:5432/invoicedb
  DATASOURCE_USERNAME: postgres
  DATASOURCE_PASSWORD: postgres123

  # Tesseract OCR
  TESSDATA_PREFIX: /usr/share/tessdata

  # Spring Profile
  SPRING_PROFILES_ACTIVE: docker
```

### Volumes

**postgres_data**: PostgreSQL database files
```bash
# Inspect volume
docker volume inspect invoice-extractor-service_postgres_data
```

**uploads_data**: Uploaded invoice files
```bash
# Inspect volume
docker volume inspect invoice-extractor-service_uploads_data
```

### Ports

Change exposed ports in `docker-compose.yml`:

```yaml
ports:
  - "8080:8080"  # API (change left side for host port)
  - "5432:5432"  # PostgreSQL
```

---

## Database Initialization

The PostgreSQL container automatically runs initialization scripts on first start:

1. **01-schema.sql** - Creates schema and tables
2. **02-sample-data.sql** - Inserts sample data

**Scripts location**: `src/main/resources/db/`

**To reset database**:
```bash
# Stop and remove volumes
docker-compose down -v

# Restart (will reinitialize)
docker-compose up -d
```

---

## Tesseract OCR Configuration

### Installed Languages
- **English** (eng)
- **Spanish** (spa)

### Configuration in application-docker.properties
```properties
tesseract.datapath=/usr/share/tessdata
tesseract.language=eng+spa
tesseract.page-seg-mode=1  # Automatic page segmentation with OSD
tesseract.oem-mode=3        # Default, based on what is available
```

### Add More Languages

Edit `Dockerfile`:
```dockerfile
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-eng \
    tesseract-ocr-data-spa \
    tesseract-ocr-data-fra \  # Add French
    tesseract-ocr-data-deu    # Add German
```

---

## Docker Commands Reference

### Build and Run

```bash
# Build images
docker-compose build

# Start services
docker-compose up

# Start in background
docker-compose up -d

# Build and start
docker-compose up --build

# Stop services
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes
docker-compose down -v
```

### Logs

```bash
# View all logs
docker-compose logs

# Follow logs
docker-compose logs -f

# Service-specific logs
docker-compose logs invoice-extractor-service
docker-compose logs postgres

# Last 100 lines
docker-compose logs --tail=100 -f
```

### Container Access

```bash
# Access API container shell
docker-compose exec invoice-extractor-service sh

# Access PostgreSQL container
docker-compose exec postgres psql -U postgres -d invoicedb

# Run Tesseract directly
docker-compose exec invoice-extractor-service tesseract --version
```

### Resource Monitoring

```bash
# Container stats
docker stats

# Disk usage
docker system df

# Inspect service
docker-compose exec invoice-extractor-service env
```

---

## Troubleshooting

### Service won't start

**Check logs**:
```bash
docker-compose logs invoice-extractor-service
```

**Common issues**:
- Port already in use → Change port in `docker-compose.yml`
- Database not ready → Increase `healthcheck` intervals
- Out of memory → Allocate more RAM to Docker

### Database connection failed

**Verify PostgreSQL is running**:
```bash
docker-compose ps postgres
docker-compose logs postgres
```

**Test connection**:
```bash
docker-compose exec postgres psql -U postgres -d invoicedb -c "SELECT 1"
```

### Tesseract not found

**Verify installation**:
```bash
docker-compose exec invoice-extractor-service tesseract --version
docker-compose exec invoice-extractor-service ls -la /usr/share/tessdata
```

### File upload fails

**Check upload directory permissions**:
```bash
docker-compose exec invoice-extractor-service ls -la /app/uploads
```

**Inspect upload volume**:
```bash
docker volume inspect invoice-extractor-service_uploads_data
```

---

## Production Deployment

### Security Considerations

1. **Change default passwords** in `docker-compose.yml`
2. **Use secrets management** (Docker Secrets, env files)
3. **Enable HTTPS** with reverse proxy (Nginx, Traefik)
4. **Restrict network access** with firewall rules
5. **Regular updates** of base images

### Performance Tuning

**Java Memory**:
```yaml
environment:
  JAVA_OPTS: "-Xms512m -Xmx2g"
```

**PostgreSQL**:
```yaml
environment:
  POSTGRES_SHARED_BUFFERS: "256MB"
  POSTGRES_MAX_CONNECTIONS: "100"
```

### Monitoring

**Add Prometheus metrics**:
```properties
management.metrics.export.prometheus.enabled=true
```

**Health checks**:
```bash
# Endpoint
curl http://localhost:8080/invoice-extractor-service/actuator/health
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Docker Build and Push

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build Docker image
        run: docker build -t invoice-extractor:latest .

      - name: Run tests
        run: docker-compose run invoice-extractor-service mvn test
```

---

## Cleanup

### Remove everything

```bash
# Stop and remove containers, networks, volumes
docker-compose down -v

# Remove images
docker rmi invoice-extractor-service-invoice-extractor-service

# Remove unused images and volumes
docker system prune -a --volumes
```

---

**Last updated**: 2025-12-08
**Docker Compose version**: 3.8
**Status**: ✅ READY FOR USE
