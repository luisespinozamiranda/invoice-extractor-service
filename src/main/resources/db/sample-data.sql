-- ==============================================
-- Sample Data for Invoice Extractor Service
-- Schema: invoicedata
-- Purpose: Testing and development
-- Fields: Invoice number, Invoice amount, Client name, Client address
-- ==============================================

-- Insert sample invoices with client information
INSERT INTO invoicedata.tb_invoice (
    invoice_key,
    invoice_number,
    invoice_amount,
    client_name,
    client_address,
    currency,
    status,
    original_file_name,
    created_at,
    updated_at
)
VALUES
    (
        'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d',
        'INV-2024-001',
        1450.75,
        'ACME Corporation',
        '123 Main Street, Salt Lake City, UT 84101',
        'USD',
        'EXTRACTED',
        'invoice_2024_001.pdf',
        NOW(),
        NOW()
    ),
    (
        'b2c3d4e5-f6a7-4b5c-9d0e-1f2a3b4c5d6e',
        'INV-2024-002',
        2850.00,
        'Global Supplies Inc',
        '456 Commerce Ave, Provo, UT 84601',
        'USD',
        'EXTRACTED',
        'invoice_2024_002.pdf',
        NOW(),
        NOW()
    ),
    (
        'c3d4e5f6-a7b8-4c5d-0e1f-2a3b4c5d6e7f',
        'INV-2024-003',
        3200.50,
        'Tech Solutions LLC',
        '789 Innovation Blvd, Lehi, UT 84043',
        'USD',
        'EXTRACTED',
        'invoice_2024_003.pdf',
        NOW(),
        NOW()
    )
ON CONFLICT (invoice_key) DO NOTHING;

-- Insert sample extraction metadata
INSERT INTO invoicedata.tb_extraction_metadata (
    extraction_key,
    invoice_key,
    source_file_name,
    extraction_timestamp,
    extraction_status,
    confidence_score,
    ocr_engine,
    extraction_data,
    error_message,
    created_at
)
VALUES
    (
        'aaaa1111-bbbb-2222-cccc-333344445555',
        'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d',
        'invoice_2024_001.pdf',
        NOW(),
        'COMPLETED',
        0.92,
        'Tesseract 5.x',
        '{"invoice_number": "INV-2024-001", "invoice_amount": 1450.75, "client_name": "ACME Corporation", "client_address": "123 Main Street, Salt Lake City, UT 84101", "fields_detected": 4}',
        NULL,
        NOW()
    ),
    (
        'bbbb2222-cccc-3333-dddd-444455556666',
        'b2c3d4e5-f6a7-4b5c-9d0e-1f2a3b4c5d6e',
        'invoice_2024_002.pdf',
        NOW(),
        'COMPLETED',
        0.88,
        'Tesseract 5.x',
        '{"invoice_number": "INV-2024-002", "invoice_amount": 2850.00, "client_name": "Global Supplies Inc", "client_address": "456 Commerce Ave, Provo, UT 84601", "fields_detected": 4}',
        NULL,
        NOW()
    ),
    (
        'cccc3333-dddd-4444-eeee-555566667777',
        'c3d4e5f6-a7b8-4c5d-0e1f-2a3b4c5d6e7f',
        'invoice_2024_003.pdf',
        NOW(),
        'COMPLETED',
        0.95,
        'Tesseract 5.x',
        '{"invoice_number": "INV-2024-003", "invoice_amount": 3200.50, "client_name": "Tech Solutions LLC", "client_address": "789 Innovation Blvd, Lehi, UT 84043", "fields_detected": 4}',
        NULL,
        NOW()
    )
ON CONFLICT (extraction_key) DO NOTHING;

-- ==============================================
-- Verification Queries
-- ==============================================

-- Count records in each table
SELECT 'Invoices' as table_name, COUNT(*) as record_count FROM invoicedata.tb_invoice
UNION ALL
SELECT 'Extraction Metadata', COUNT(*) FROM invoicedata.tb_extraction_metadata
ORDER BY table_name;

-- Display all invoices with extracted information
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

-- Display extraction metadata with confidence scores
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
