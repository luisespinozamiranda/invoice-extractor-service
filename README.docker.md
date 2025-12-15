# Docker Deployment Guide - Invoice Extractor

Este documento explica cómo ejecutar el stack completo (Frontend + Backend + PostgreSQL) usando Docker Compose.

## Requisitos Previos

- Docker Engine 20.10+
- Docker Compose 2.0+
- API Key de Groq (obtenerla en https://console.groq.com/)

## Estructura del Stack

El sistema está compuesto por 3 servicios:

```
┌─────────────────────────────────────────────────┐
│  Frontend (Angular + Nginx)                     │
│  Puerto: 80                                      │
│  Container: invoice-extractor-frontend          │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Backend (Spring Boot + Tesseract OCR)          │
│  Puerto: 8080                                    │
│  Container: invoice-extractor-api               │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Database (PostgreSQL 15)                       │
│  Puerto: 5432                                    │
│  Container: invoice-postgres                    │
└─────────────────────────────────────────────────┘
```

## Configuración Inicial

### 1. Configurar Variables de Entorno

Copia el archivo de ejemplo y configura tus credenciales:

```bash
cp .env.example .env
```

Edita el archivo `.env` y configura:

```bash
# PostgreSQL
POSTGRES_DB=invoicedb
POSTGRES_USER=invoiceuser
POSTGRES_PASSWORD=tu_password_seguro

# Groq API
GROQ_API_KEY=tu_groq_api_key
```

### 2. Verificar Estructura de Directorios

Asegúrate de que tu estructura sea:

```
Training/
├── invoice-extractor-service/     # Backend
│   ├── docker-compose.yml
│   ├── Dockerfile
│   ├── .env
│   └── src/
│       └── main/
│           └── resources/
│               └── db/
│                   ├── schema.sql
│                   └── sample-data.sql
└── invoice-extractor-frontend/    # Frontend
    ├── Dockerfile
    ├── nginx.conf
    └── src/
```

## Comandos de Uso

### Iniciar Todos los Servicios

```bash
cd invoice-extractor-service
docker-compose up -d
```

Este comando:
1. Inicia PostgreSQL y ejecuta los scripts de inicialización (schema.sql)
2. Construye y levanta el backend Spring Boot
3. Construye y levanta el frontend Angular

### Ver Logs de los Servicios

```bash
# Todos los servicios
docker-compose logs -f

# Solo un servicio específico
docker-compose logs -f postgres
docker-compose logs -f invoice-extractor-service
docker-compose logs -f invoice-extractor-frontend
```

### Verificar Estado de los Servicios

```bash
docker-compose ps
```

Deberías ver:

```
NAME                          STATUS      PORTS
invoice-extractor-api         Up (healthy)   0.0.0.0:8080->8080/tcp
invoice-extractor-frontend    Up (healthy)   0.0.0.0:80->80/tcp
invoice-postgres              Up (healthy)   0.0.0.0:5432->5432/tcp
```

### Detener los Servicios

```bash
# Detener pero mantener los datos
docker-compose stop

# Detener y eliminar contenedores (mantiene volúmenes)
docker-compose down

# Detener, eliminar contenedores Y eliminar volúmenes (BORRA DATOS)
docker-compose down -v
```

### Reconstruir las Imágenes

Si haces cambios en el código:

```bash
# Reconstruir todo
docker-compose up -d --build

# Reconstruir solo un servicio
docker-compose up -d --build invoice-extractor-service
docker-compose up -d --build invoice-extractor-frontend
```

## Acceso a los Servicios

Una vez iniciados los servicios, puedes acceder a:

| Servicio | URL | Descripción |
|----------|-----|-------------|
| **Frontend** | http://localhost | Aplicación Angular |
| **Backend API** | http://localhost:8080/invoice-extractor-service | API REST |
| **Swagger UI** | http://localhost:8080/invoice-extractor-service/swagger-ui.html | Documentación API |
| **Health Check** | http://localhost:8080/invoice-extractor-service/actuator/health | Estado del backend |
| **PostgreSQL** | localhost:5432 | Base de datos (usuario: invoiceuser) |

## Gestión de la Base de Datos

### Conectarse a PostgreSQL

```bash
# Desde el host
psql -h localhost -p 5432 -U invoiceuser -d invoicedb

# Desde el contenedor
docker exec -it invoice-postgres psql -U invoiceuser -d invoicedb
```

### Ver las Tablas Creadas

```sql
\c invoicedb
\dt invoicedata.*

-- Ver datos de ejemplo
SELECT * FROM invoicedata.tb_invoice;
```

### Reiniciar la Base de Datos

Si necesitas reiniciar la BD con datos limpios:

```bash
# Detener y eliminar el volumen de PostgreSQL
docker-compose down
docker volume rm invoice-extractor-service_postgres_data

# Volver a iniciar
docker-compose up -d
```

## Troubleshooting

### El backend no inicia - Error de conexión a la BD

1. Verifica que PostgreSQL esté healthy:
   ```bash
   docker-compose ps postgres
   ```

2. Revisa los logs de PostgreSQL:
   ```bash
   docker-compose logs postgres
   ```

3. Verifica las credenciales en `.env`

### El frontend muestra error de conexión

1. Verifica que el backend esté corriendo:
   ```bash
   curl http://localhost:8080/invoice-extractor-service/actuator/health
   ```

2. Revisa la configuración de CORS en el backend

### Error "Groq API Key not found"

1. Asegúrate de tener el archivo `.env` en la raíz del proyecto
2. Verifica que `GROQ_API_KEY` esté configurado correctamente
3. Reinicia los contenedores:
   ```bash
   docker-compose restart invoice-extractor-service
   ```

### Puertos en uso

Si recibes error de puertos ocupados:

```bash
# Cambiar los puertos en docker-compose.yml
# Por ejemplo, cambiar "80:80" a "3000:80" para el frontend
ports:
  - "3000:80"  # Acceder en http://localhost:3000
```

### Ver uso de recursos

```bash
docker stats
```

## Volúmenes Persistentes

El sistema crea dos volúmenes para persistir datos:

- `postgres_data`: Datos de la base de datos PostgreSQL
- `uploads_data`: Archivos subidos (facturas en PDF/imagen)

Para listar los volúmenes:

```bash
docker volume ls | grep invoice
```

## Limpieza Completa

Para eliminar todo (contenedores, imágenes, volúmenes):

```bash
# CUIDADO: Esto eliminará todos los datos
docker-compose down -v --rmi all
docker volume prune
```

## Producción

Para producción, considera:

1. Usar Docker Secrets para credenciales sensibles
2. Configurar un reverse proxy (Traefik, Nginx) con SSL/TLS
3. Implementar límites de recursos en docker-compose.yml:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '1'
         memory: 1G
   ```
4. Usar health checks más frecuentes
5. Configurar logging centralizado
6. Implementar backups automáticos de PostgreSQL

## Respaldo de la Base de Datos

### Crear un backup

```bash
docker exec invoice-postgres pg_dump -U invoiceuser invoicedb > backup.sql
```

### Restaurar un backup

```bash
cat backup.sql | docker exec -i invoice-postgres psql -U invoiceuser -d invoicedb
```

## Variables de Entorno Completas

| Variable | Default | Descripción |
|----------|---------|-------------|
| `POSTGRES_DB` | invoicedb | Nombre de la base de datos |
| `POSTGRES_USER` | invoiceuser | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | invoicepass | Contraseña de PostgreSQL |
| `GROQ_API_KEY` | - | API Key de Groq (requerido) |
| `GROQ_MODEL` | llama-3.3-70b-versatile | Modelo LLM de Groq |
