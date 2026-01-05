#!/bin/bash

echo "========================================"
echo " Invoice Extractor - Quick Start"
echo "========================================"
echo ""
echo "Starting Docker Compose services..."
echo "- PostgreSQL"
echo "- Backend (Spring Boot + Tesseract OCR)"
echo "- Frontend (Angular + Nginx)"
echo ""

docker-compose up -d

echo ""
echo "========================================"
echo " Services started!"
echo "========================================"
echo ""
echo "Wait about 30-60 seconds for everything to be ready..."
echo ""
echo "Access URLs:"
echo "  Frontend:   http://localhost"
echo "  Backend:    http://localhost:8080/invoice-extractor-service"
echo "  Swagger UI: http://localhost:8080/invoice-extractor-service/swagger-ui.html"
echo ""
echo "To view logs: docker-compose logs -f"
echo "To stop:      docker-compose down"
echo ""
echo "Services status:"
echo ""

sleep 3
docker-compose ps

echo ""
echo "Ready! You can access http://localhost"
