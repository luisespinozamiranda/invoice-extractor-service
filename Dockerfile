# Multi-stage Dockerfile for invoice-extractor-service with Tesseract OCR

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage with Tesseract OCR
FROM eclipse-temurin:17-jre-jammy

# Install Tesseract OCR and language data
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-spa \
    ghostscript \
    fonts-dejavu \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create directory for uploaded files
RUN mkdir -p /app/uploads && chmod 777 /app/uploads

# Verify tessdata installation
RUN tesseract --list-langs

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/invoice-extractor-service/actuator/health || exit 1

# Run the application with optimized JVM settings
# -Xmx512m: Maximum heap size (adjust based on Render plan)
# -Xms256m: Initial heap size
# -XX:+UseG1GC: Use G1 garbage collector (good for containerized apps)
# -XX:MaxGCPauseMillis=200: Target max GC pause time
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "app.jar"]
