#!/bin/bash

echo "========================================"
echo " Invoice Extractor - Inicio Rápido"
echo "========================================"
echo ""
echo "Iniciando servicios de Docker Compose..."
echo "- PostgreSQL"
echo "- Backend (Spring Boot + Tesseract OCR)"
echo "- Frontend (Angular + Nginx)"
echo ""

docker-compose up -d

echo ""
echo "========================================"
echo " Servicios iniciados!"
echo "========================================"
echo ""
echo "Espera unos 30-60 segundos para que todo esté listo..."
echo ""
echo "URLs de acceso:"
echo "  Frontend:   http://localhost"
echo "  Backend:    http://localhost:8080/invoice-extractor-service"
echo "  Swagger UI: http://localhost:8080/invoice-extractor-service/swagger-ui.html"
echo ""
echo "Para ver los logs: docker-compose logs -f"
echo "Para detener:     docker-compose down"
echo ""
echo "Estado de los servicios:"
echo ""

sleep 3
docker-compose ps

echo ""
echo "Listo! Puedes acceder a http://localhost"
