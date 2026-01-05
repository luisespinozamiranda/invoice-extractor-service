@echo off
echo ========================================
echo  Invoice Extractor - Quick Start
echo ========================================
echo.

REM Check if .env file exists
if not exist ".env" (
    echo [ERROR] .env file not found
    echo.
    echo Please create the .env file with your configuration:
    echo   1. Copy the .env.example file: copy .env.example .env
    echo   2. Edit .env and add your GROQ_API_KEY
    echo   3. Get your free API key at: https://console.groq.com/keys
    echo.
    echo Press any key to exit...
    pause > nul
    exit /b 1
)

echo Building application...
call mvn clean package -DskipTests
echo.
echo Stopping previous services...
docker-compose down
echo.
echo Rebuilding Docker image...
docker-compose build --no-cache invoice-extractor-service
echo.
echo Starting Docker Compose services...
echo - PostgreSQL
echo - Backend (Spring Boot + Tesseract OCR)
echo - Frontend (Angular + Nginx)
echo.

docker-compose up -d

echo.
echo ========================================
echo  Services started!
echo ========================================
echo.
echo Wait about 30-60 seconds for everything to be ready...
echo.
echo Access URLs:
echo   Frontend:   http://localhost
echo   Backend:    http://localhost:8080/invoice-extractor-service
echo   Swagger UI: http://localhost:8080/invoice-extractor-service/swagger-ui.html
echo   pgAdmin:    http://localhost:5050 (user: admin, pass: admin)
echo.
echo To view logs:
echo   All:      docker-compose logs -f
echo   Backend:  docker-compose logs -f invoice-extractor-service
echo   Frontend: docker-compose logs -f invoice-extractor-frontend
echo To stop: docker-compose down
echo.
echo Press any key to view services status...
pause > nul

docker-compose ps

echo.
echo Press any key to exit...
pause > nul
