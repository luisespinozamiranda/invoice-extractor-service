@echo off
echo ========================================
echo  Invoice Extractor - Detener Servicios
echo ========================================
echo.
echo Deteniendo servicios de Docker Compose...
echo.

docker-compose down

echo.
echo ========================================
echo  Servicios detenidos!
echo ========================================
echo.
echo Los datos se han conservado.
echo Para iniciar nuevamente: start.bat
echo.
echo Presiona cualquier tecla para salir...
pause > nul
