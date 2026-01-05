@echo off
echo ========================================
echo  Invoice Extractor - Inicio Rapido
echo ========================================
echo.

REM Verificar si existe el archivo .env
if not exist ".env" (
    echo [ERROR] No se encontro el archivo .env
    echo.
    echo Por favor, crea el archivo .env con tu configuracion:
    echo   1. Copia el archivo .env.example: copy .env.example .env
    echo   2. Edita .env y agrega tu GROQ_API_KEY
    echo   3. Obten tu API key gratis en: https://console.groq.com/keys
    echo.
    echo Presiona cualquier tecla para salir...
    pause > nul
    exit /b 1
)

echo Construyendo aplicacion...
call mvn clean package -DskipTests
echo.
echo Deteniendo servicios previos...
docker-compose down
echo.
echo Reconstruyendo imagen Docker...
docker-compose build --no-cache invoice-extractor-service
echo.
echo Iniciando servicios de Docker Compose...
echo - PostgreSQL
echo - Backend (Spring Boot + Tesseract OCR)
echo - Frontend (Angular + Nginx)
echo.

docker-compose up -d

echo.
echo ========================================
echo  Servicios iniciados!
echo ========================================
echo.
echo Espera unos 30-60 segundos para que todo este listo...
echo.
echo URLs de acceso:
echo   Frontend:   http://localhost
echo   Backend:    http://localhost:8080/invoice-extractor-service
echo   Swagger UI: http://localhost:8080/invoice-extractor-service/swagger-ui.html
echo   pgAdmin:    http://localhost:5050 (user: admin, pass: admin)
echo.
echo Para ver logs:
echo   Todos:    docker-compose logs -f
echo   Backend:  docker-compose logs -f invoice-extractor-service
echo   Frontend: docker-compose logs -f invoice-extractor-frontend
echo Para detener: docker-compose down
echo.
echo Presiona cualquier tecla para ver el estado de los servicios...
pause > nul

docker-compose ps

echo.
echo Presiona cualquier tecla para salir...
pause > nul
