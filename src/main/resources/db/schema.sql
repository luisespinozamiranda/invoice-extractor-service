-- ==============================================
-- Invoice Extractor Service - Database Schema
-- Schema: invoicedata
-- Database: PostgreSQL 15+
-- Strategy: Soft Delete (Logical Deletion via Application Layer)
-- Simplified: Single invoice table with client information
-- ==============================================

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS invoicedata;

-- ==============================================
-- Table: tb_invoice
-- Description: Stores invoice information extracted from documents
-- Fields: Invoice number, Invoice amount, Client name, Client address
-- ==============================================
CREATE TABLE IF NOT EXISTS invoicedata.tb_invoice (
    id BIGSERIAL PRIMARY KEY,
    invoice_key UUID NOT NULL UNIQUE,

    -- Extracted fields from invoice
    invoice_number VARCHAR(100) NOT NULL,
    invoice_amount DECIMAL(15, 2) NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    client_address TEXT,

    -- Additional metadata
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- ==============================================
-- Table: tb_extraction_metadata
-- Description: Stores OCR extraction metadata and results
-- ==============================================
CREATE TABLE IF NOT EXISTS invoicedata.tb_extraction_metadata (
    id BIGSERIAL PRIMARY KEY,
    extraction_key UUID NOT NULL UNIQUE,
    invoice_key UUID,
    source_file_name VARCHAR(255) NOT NULL,
    extraction_timestamp TIMESTAMP NOT NULL,
    extraction_status VARCHAR(50) NOT NULL,
    confidence_score DECIMAL(3, 2),
    ocr_engine VARCHAR(100),
    extraction_data JSONB,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_extraction_invoice FOREIGN KEY (invoice_key)
        REFERENCES invoicedata.tb_invoice(invoice_key)
);

-- ==============================================
-- INDEXES for Performance Optimization
-- ==============================================

-- Invoice indexes
CREATE INDEX IF NOT EXISTS idx_invoice_key ON invoicedata.tb_invoice(invoice_key);
CREATE INDEX IF NOT EXISTS idx_invoice_number ON invoicedata.tb_invoice(invoice_number) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_invoice_client_name ON invoicedata.tb_invoice(client_name) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_invoice_status ON invoicedata.tb_invoice(status) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_invoice_created_at ON invoicedata.tb_invoice(created_at DESC) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_invoice_is_deleted ON invoicedata.tb_invoice(is_deleted);

-- Extraction metadata indexes
CREATE INDEX IF NOT EXISTS idx_extraction_key ON invoicedata.tb_extraction_metadata(extraction_key);
CREATE INDEX IF NOT EXISTS idx_extraction_invoice_key ON invoicedata.tb_extraction_metadata(invoice_key) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_extraction_status ON invoicedata.tb_extraction_metadata(extraction_status) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_extraction_timestamp ON invoicedata.tb_extraction_metadata(extraction_timestamp DESC) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_extraction_is_deleted ON invoicedata.tb_extraction_metadata(is_deleted);

-- JSONB GIN index for efficient JSON queries
CREATE INDEX IF NOT EXISTS idx_extraction_data_gin ON invoicedata.tb_extraction_metadata USING GIN (extraction_data) WHERE is_deleted = FALSE;

-- ==============================================
-- COMMENTS for Documentation
-- ==============================================

COMMENT ON SCHEMA invoicedata IS 'Schema for invoice extraction service data with soft delete support';

COMMENT ON TABLE invoicedata.tb_invoice IS 'Stores extracted invoice information: invoice number, amount, client name, and client address. Uses soft delete (is_deleted column)';
COMMENT ON TABLE invoicedata.tb_extraction_metadata IS 'Stores OCR extraction metadata and results. Uses soft delete (is_deleted column)';

COMMENT ON COLUMN invoicedata.tb_invoice.invoice_number IS 'Invoice number extracted from document';
COMMENT ON COLUMN invoicedata.tb_invoice.invoice_amount IS 'Total invoice amount extracted from document';
COMMENT ON COLUMN invoicedata.tb_invoice.client_name IS 'Client name extracted from document';
COMMENT ON COLUMN invoicedata.tb_invoice.client_address IS 'Client address extracted from document';
COMMENT ON COLUMN invoicedata.tb_invoice.is_deleted IS 'Soft delete flag. FALSE = active, TRUE = deleted';
COMMENT ON COLUMN invoicedata.tb_extraction_metadata.is_deleted IS 'Soft delete flag. FALSE = active, TRUE = deleted';

COMMENT ON COLUMN invoicedata.tb_invoice.status IS 'Invoice status: PROCESSING, EXTRACTED, EXTRACTION_FAILED, PENDING';
COMMENT ON COLUMN invoicedata.tb_extraction_metadata.extraction_status IS 'Extraction status: PROCESSING, COMPLETED, FAILED, PARTIAL';
COMMENT ON COLUMN invoicedata.tb_extraction_metadata.confidence_score IS 'OCR confidence score from 0.00 to 1.00';
COMMENT ON COLUMN invoicedata.tb_extraction_metadata.extraction_data IS 'Raw extraction data in JSON format (JSONB)';
