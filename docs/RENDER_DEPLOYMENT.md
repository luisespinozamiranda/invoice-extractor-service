# Guía de Despliegue en Render con Tesseract OCR

Este documento detalla los pasos necesarios para desplegar el servicio `invoice-extractor-service` en Render con soporte completo para Tesseract OCR.

## 📋 Requisitos Previos

- Cuenta en Render.com
- Repositorio Git con el código del proyecto
- Base de datos PostgreSQL creada en Render

## 🔧 Variables de Entorno Requeridas

Debes configurar las siguientes variables de entorno en Render:

### Base de Datos
```bash
DATASOURCE_URL=jdbc:postgresql://<tu-host>.render.com/<nombre-db>
DATASOURCE_USERNAME=<tu-usuario>
DATASOURCE_PASSWORD=<tu-password>
```

### Tesseract OCR
```bash
TESSDATA_PREFIX=/usr/share/tessdata
```

### LLM Integration (Groq API)
```bash
LLM_ENABLED=true
GROQ_API_KEY=<tu-groq-api-key>
```

**Obtener Groq API Key gratis**: https://console.groq.com/keys

### Almacenamiento de Archivos
```bash
UPLOAD_DIRECTORY=/app/uploads
```

## 🐳 Dockerfile Requerido

Render requiere un `Dockerfile` que incluya Tesseract. Crea un archivo `Dockerfile` en la raíz del proyecto:

```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS build

# Build stage
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install Tesseract OCR and language data
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-eng \
    tesseract-ocr-data-spa

# Create upload directory
RUN mkdir -p /app/uploads && chmod 777 /app/uploads

# Copy application
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 📦 Pasos de Despliegue en Render

### 1. Crear Web Service

1. Ve a tu dashboard en [Render.com](https://render.com)
2. Click en **"New +"** → **"Web Service"**
3. Conecta tu repositorio Git
4. Configura el servicio:
   - **Name**: `invoice-extractor-service`
   - **Environment**: `Docker`
   - **Region**: Selecciona la más cercana
   - **Branch**: `main` (o tu rama principal)

### 2. Configurar Build & Deploy

Render detectará automáticamente el `Dockerfile`. Si no:
- **Build Command**: (dejar vacío, usa Dockerfile)
- **Start Command**: (dejar vacío, usa Dockerfile)

### 3. Agregar Variables de Entorno

En la sección **Environment**, agrega todas las variables mencionadas arriba:

```
DATASOURCE_URL=jdbc:postgresql://dpg-xxxxx.virginia-postgres.render.com/postgresql_invoice_service
DATASOURCE_USERNAME=lespinoza
DATASOURCE_PASSWORD=q79wfvFfHxhdL36pWB0D4iSCRriuygt1
TESSDATA_PREFIX=/usr/share/tessdata
LLM_ENABLED=true
GROQ_API_KEY=<tu-groq-api-key>
UPLOAD_DIRECTORY=/app/uploads
```

### 4. Configurar Health Check (Opcional pero Recomendado)

- **Health Check Path**: `/invoice-extractor-service/actuator/health`
- **Port**: `8080`

### 5. Deploy

1. Click en **"Create Web Service"**
2. Render iniciará el build y deploy automáticamente
3. Monitorea los logs para verificar que Tesseract se instaló correctamente

## ✅ Verificación Post-Despliegue

### 1. Verificar que Tesseract está instalado

Revisa los logs de inicio, deberías ver:

```
=== OCR Configuration ===
OCR Enabled: true
Tesseract Data Path: /usr/share/tessdata
Tesseract Language: eng
PDF Rendering DPI: 300
Tesseract data path found: /usr/share/tessdata
Language file found: eng.traineddata
========================

✓ LLM extraction service is enabled and available: Groq (Llama 3.1 70B)
```

### 2. Probar el Health Endpoint

```bash
curl https://tu-servicio.onrender.com/invoice-extractor-service/actuator/health
```

Respuesta esperada:
```json
{
  "status": "UP"
}
```

### 3. Probar Swagger UI

Abre en el navegador:
```
https://tu-servicio.onrender.com/invoice-extractor-service/swagger-ui.html
```

### 4. Probar la Extracción de Invoices

```bash
curl -X POST https://tu-servicio.onrender.com/invoice-extractor-service/api/v1.0/extractions \
  -H "Content-Type: multipart/form-data" \
  -F "file=@invoice.pdf"
```

## 🚨 Solución de Problemas

### Problema: "Tesseract data path does not exist"

**Solución**: Verifica que el Dockerfile instala correctamente `tesseract-ocr` y `tesseract-ocr-data-eng`

### Problema: "Failed to read image/PDF"

**Solución**:
- Asegúrate de que PDFBox está en el `pom.xml`
- Verifica que el archivo subido sea un PDF o imagen válida

### Problema: "Out of Memory Error"

**Solución**:
- Aumenta la memoria en el plan de Render
- O reduce el DPI en `application.properties`: `ocr.tesseract.dpi=200`

### Problema: "File storage failed"

**Solución**:
- Verifica que `/app/uploads` tiene permisos de escritura
- En el Dockerfile, asegúrate de: `RUN mkdir -p /app/uploads && chmod 777 /app/uploads`

### Problema: "LLM extraction failed"

**Solución**:
- Verifica que `GROQ_API_KEY` esté configurada correctamente
- Verifica que `LLM_ENABLED=true` esté configurado
- Revisa los logs para ver el mensaje de error de Groq API
- El servicio automáticamente fallback a regex si LLM falla

## 📊 Monitoreo

### Logs en Tiempo Real

```bash
# Desde el dashboard de Render, ve a "Logs"
# O usa Render CLI:
render logs -f <service-id>
```

### Métricas Importantes

Monitorea en Render Dashboard:
- **CPU Usage**: Tesseract puede ser intensivo en CPU
- **Memory Usage**: Los PDFs grandes requieren más memoria
- **Request Duration**: La extracción OCR + LLM toma tiempo (~30-35 segundos)
- **API Rate Limits**: Groq free tier tiene límites de requests por minuto

## 🔄 Actualizar el Servicio

1. Haz push de tus cambios al repositorio Git
2. Render detectará automáticamente los cambios
3. Iniciará un nuevo deploy automáticamente

## 📝 Notas Adicionales

### Idiomas Soportados

Por defecto, solo inglés (`eng`) está configurado. Para agregar más idiomas:

1. Actualiza el `Dockerfile`:
```dockerfile
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-eng \
    tesseract-ocr-data-spa \
    tesseract-ocr-data-fra
```

2. Actualiza `application.properties`:
```properties
ocr.tesseract.language=eng+spa+fra
```

### Almacenamiento Persistente

**⚠️ IMPORTANTE**: Los archivos en `/app/uploads` **NO son persistentes** en Render (almacenamiento efímero).

Para producción, considera:
- Amazon S3
- Google Cloud Storage
- Cloudinary

### LLM Configuration

El servicio usa Groq API con el modelo Llama 3.1 70B para extracción inteligente:

**Características**:
- **Gratis**: Groq ofrece API gratuita (con rate limits)
- **Rápido**: 2-5 segundos para extraer datos
- **Preciso**: Entiende contexto y formatos variados de facturas
- **Fallback**: Si LLM falla, usa regex automáticamente

**Alternativas**:
Si quieres cambiar de LLM provider, la arquitectura hexagonal lo permite fácilmente:
1. Implementa `ILlmExtractionService` para tu provider
2. Marca tu implementación con `@Service`
3. Actualiza las variables de entorno

**Desactivar LLM**:
```bash
LLM_ENABLED=false
```
El servicio usará solo patrones regex para extracción.

### Costos

- Free Tier: Limitado, el servicio se duerme después de inactividad
- Starter Plan ($7/mes): Servicio siempre activo, más recursos
- **Recomendación**: Usar al menos Starter para OCR (CPU intensivo)
- **Groq API**: Gratis con rate limits (suficiente para pruebas)

## 🔗 Enlaces Útiles

- [Render Docs](https://render.com/docs)
- [Tesseract GitHub](https://github.com/tesseract-ocr/tesseract)
- [Tess4J Documentation](http://tess4j.sourceforge.net/)
- [Groq Console](https://console.groq.com/) - Obtener API key
- [Groq API Docs](https://console.groq.com/docs)

---

**Última actualización**: 2025-12-09
**Mantenido por**: Luis Espinoza
