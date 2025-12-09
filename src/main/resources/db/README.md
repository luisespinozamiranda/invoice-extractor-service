# Database Scripts - Invoice Extractor Service

## Schema: invoicedata

Este directorio contiene los scripts SQL para crear y poblar la base de datos PostgreSQL.

**Arquitectura simplificada:** Solo 2 tablas principales.

**Campos a extraer del invoice:**
- Invoice number
- Invoice amount
- Client name
- Client address

---

## Archivos Disponibles

### 1. `schema.sql`
**Propósito:** Crear el schema completo de la base de datos

**Contenido:**
- Creación del schema `invoicedata`
- 2 tablas principales:
  - `tb_invoice` - Facturas con los 4 campos extraídos
  - `tb_extraction_metadata` - Metadata de extracción OCR
- Índices para optimización
- Llaves foráneas y constraints
- Comentarios de documentación

### 2. `sample-data.sql`
**Propósito:** Insertar datos de ejemplo para testing

**Contenido:**
- 3 invoices de ejemplo
- 3 extraction metadata records
- Queries de verificación

---

## Cómo Ejecutar los Scripts

### Opción 1: Usando psql (Línea de comandos)

```bash
# Conectar a la base de datos
psql -h dpg-d4pfk8khg0os73ar3c70-a.virginia-postgres.render.com \
     -U lespinoza \
     -d postgresql_invoice_service

# Ejecutar schema
\i C:/Users/lespinoza/Documents/Projects/Training/invoice-extractor-service/src/main/resources/db/schema.sql

# Ejecutar datos de ejemplo (opcional)
\i C:/Users/lespinoza/Documents/Projects/Training/invoice-extractor-service/src/main/resources/db/sample-data.sql

# Salir
\q
```

### Opción 2: Usando psql con archivo

```bash
# Ejecutar schema directamente
psql -h dpg-d4pfk8khg0os73ar3c70-a.virginia-postgres.render.com \
     -U lespinoza \
     -d postgresql_invoice_service \
     -f src/main/resources/db/schema.sql

# Ejecutar datos de ejemplo
psql -h dpg-d4pfk8khg0os73ar3c70-a.virginia-postgres.render.com \
     -U lespinoza \
     -d postgresql_invoice_service \
     -f src/main/resources/db/sample-data.sql
```

### Opción 3: Copiar y pegar en un cliente GUI

Si usas un cliente como pgAdmin, DBeaver, o DataGrip:

1. Conecta a la base de datos
2. Abre `schema.sql`
3. Ejecuta el script completo
4. (Opcional) Abre `sample-data.sql` y ejecuta para datos de ejemplo

---

## Verificación

### Verificar que las tablas se crearon correctamente:

```sql
-- Listar todas las tablas en el schema invoicedata
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'invoicedata'
ORDER BY table_name;

-- Resultado esperado:
-- tb_extraction_metadata
-- tb_invoice
```

### Verificar estructura de una tabla:

```sql
-- Ver columnas de tb_invoice
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_schema = 'invoicedata'
  AND table_name = 'tb_invoice'
ORDER BY ordinal_position;
```

### Verificar índices:

```sql
-- Listar índices
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'invoicedata'
ORDER BY tablename, indexname;
```

### Verificar datos de ejemplo (si ejecutaste sample-data.sql):

```sql
-- Contar registros en cada tabla
SELECT 'Invoices' as table_name, COUNT(*) as record_count FROM invoicedata.tb_invoice
UNION ALL
SELECT 'Extraction Metadata', COUNT(*) FROM invoicedata.tb_extraction_metadata
ORDER BY table_name;

-- Resultado esperado:
-- Extraction Metadata: 3
-- Invoices: 3
```

---

## Estructura de Tablas

### tb_invoice
```
Campos extraídos del invoice:
- invoice_number (VARCHAR 100) - Número de factura
- invoice_amount (DECIMAL 15,2) - Monto total de la factura
- client_name (VARCHAR 255) - Nombre del cliente
- client_address (TEXT) - Dirección del cliente

Campos técnicos:
- id (BIGSERIAL) - Primary key
- invoice_key (UUID) - Business key (unique)
- currency (VARCHAR 3) - Moneda (default: USD)
- status (VARCHAR 50) - Estado: PROCESSING, EXTRACTED, EXTRACTION_FAILED
- original_file_name (VARCHAR 255) - Nombre del archivo original
- created_at (TIMESTAMP) - Fecha de creación
- updated_at (TIMESTAMP) - Fecha de actualización
- is_deleted (BOOLEAN) - Soft delete flag (FALSE = activo, TRUE = eliminado)
```

### tb_extraction_metadata
```
Columnas principales:
- id (BIGSERIAL) - Primary key
- extraction_key (UUID) - Business key (unique)
- invoice_key (UUID) - FK a tb_invoice
- source_file_name (VARCHAR 255) - Nombre del archivo fuente
- extraction_timestamp (TIMESTAMP) - Timestamp de extracción
- extraction_status (VARCHAR 50) - Estado: PROCESSING, COMPLETED, FAILED
- confidence_score (DECIMAL 3,2) - Score de confianza OCR (0.00-1.00)
- ocr_engine (VARCHAR 100) - Motor OCR usado (Tesseract, AWS Textract, etc.)
- extraction_data (JSONB) - Datos raw en JSON
- error_message (TEXT) - Mensaje de error si falla
- created_at (TIMESTAMP) - Fecha de creación
- is_deleted (BOOLEAN) - Soft delete flag (FALSE = activo, TRUE = eliminado)
```

---

## Troubleshooting

### Error: "schema invoicedata does not exist"
**Solución:** El schema se crea automáticamente en el script. Verifica que tienes permisos.

### Error: "permission denied for schema invoicedata"
**Solución:** Necesitas permisos de CREATE en la base de datos:
```sql
GRANT CREATE ON DATABASE postgresql_invoice_service TO lespinoza;
```

### Error: "relation already exists"
**Solución:** Las tablas ya existen. Para recrearlas:
```sql
DROP SCHEMA invoicedata CASCADE;
-- Luego ejecuta schema.sql nuevamente
```

### Error de conexión
**Solución:** Verifica las credenciales:
- Host: dpg-d4pfk8khg0os73ar3c70-a.virginia-postgres.render.com
- Database: postgresql_invoice_service
- User: lespinoza
- Password: (ver application.properties)

---

## Limpieza de Datos

### Eliminar solo datos de ejemplo (soft delete):
```sql
-- Marcar como eliminados
UPDATE invoicedata.tb_invoice SET is_deleted = TRUE;
UPDATE invoicedata.tb_extraction_metadata SET is_deleted = TRUE;
```

### Eliminar datos físicamente (HARD DELETE):
```sql
TRUNCATE TABLE invoicedata.tb_extraction_metadata CASCADE;
TRUNCATE TABLE invoicedata.tb_invoice CASCADE;
```

### Eliminar todo el schema:
```sql
DROP SCHEMA invoicedata CASCADE;
```

### Consultar solo registros activos:
```sql
SELECT * FROM invoicedata.tb_invoice WHERE is_deleted = FALSE;
SELECT * FROM invoicedata.tb_extraction_metadata WHERE is_deleted = FALSE;
```

### Restaurar registros eliminados:
```sql
-- Restaurar una factura específica
UPDATE invoicedata.tb_invoice
SET is_deleted = FALSE, updated_at = NOW()
WHERE invoice_key = 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d';
```

---

## Queries Útiles

### Ver todas las facturas con los campos extraídos:
```sql
SELECT
    invoice_number,
    invoice_amount,
    client_name,
    client_address,
    currency,
    status,
    original_file_name
FROM invoicedata.tb_invoice
WHERE is_deleted = FALSE
ORDER BY created_at DESC;
```

### Ver metadata de extracción con detalles de factura:
```sql
SELECT
    em.extraction_key,
    i.invoice_number,
    i.client_name,
    i.invoice_amount,
    em.extraction_status,
    em.confidence_score,
    em.ocr_engine,
    em.extraction_timestamp
FROM invoicedata.tb_extraction_metadata em
INNER JOIN invoicedata.tb_invoice i ON em.invoice_key = i.invoice_key
WHERE em.is_deleted = FALSE AND i.is_deleted = FALSE
ORDER BY em.extraction_timestamp DESC;
```

### Buscar facturas por cliente:
```sql
SELECT *
FROM invoicedata.tb_invoice
WHERE client_name ILIKE '%ACME%'
  AND is_deleted = FALSE;
```

### Buscar facturas por rango de monto:
```sql
SELECT
    invoice_number,
    client_name,
    invoice_amount,
    currency
FROM invoicedata.tb_invoice
WHERE invoice_amount BETWEEN 1000 AND 5000
  AND is_deleted = FALSE
ORDER BY invoice_amount DESC;
```

---

## Próximos Pasos

Después de ejecutar estos scripts:

1. ✅ Verificar que todas las tablas se crearon
2. ✅ Ejecutar queries de verificación
3. ✅ Probar inserción de datos
4. ⏭️ Continuar con Phase 4 (JPA Entities)

---

**Última actualización:** 2025-12-08
**Versión del schema:** 3.0 (Requirements-aligned)
**Campos extraídos:** Invoice number, Invoice amount, Client name, Client address
