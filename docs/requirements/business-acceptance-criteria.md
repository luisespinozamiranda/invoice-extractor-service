# Business-Level Acceptance Criteria

**Project:** AI-Assisted Invoice Extraction App
**Document Type:** Business Acceptance Criteria
**Version:** 1.0
**Date:** 2025-12-05

---

## 1. File Upload

**Criteria:**
- The user can upload an invoice in **PDF** or **image format** (PNG, JPG, JPEG) from the Angular UI
- The upload interface must be intuitive and clearly indicate accepted file types
- The system must validate file type before processing

**Acceptance:**
- ✅ User can select a file from their local system
- ✅ System accepts PDF, PNG, JPG, JPEG formats
- ✅ System rejects unsupported file types with clear error message

---

## 2. Automatic Data Extraction

**Criteria:**
After upload, the system automatically extracts the following fields from the invoice:

1. **Invoice Number** - Alphanumeric identifier
2. **Invoice Total Amount** - Numeric value with currency
3. **Client Name** - Full name or company name
4. **Client Address** - Complete address

**Acceptance:**
- ✅ System processes the file without manual intervention
- ✅ All four fields are extracted when present in the document
- ✅ Extraction happens within a reasonable timeframe (< 30 seconds)

---

## 3. JSON Response

**Criteria:**
The backend exposes an endpoint that returns the extracted data in **valid JSON format**

**Expected JSON Structure:**
```json
{
  "invoiceNumber": "INV-12345",
  "invoiceAmount": 1450.75,
  "clientName": "ACME Corp",
  "clientAddress": "123 Main St, Salt Lake City, UT"
}
```

**Acceptance:**
- ✅ Response is valid JSON (parseable)
- ✅ All four fields are present in the response
- ✅ Data types are correct (string for text, number for amount)
- ✅ Response includes appropriate HTTP status codes (200 for success, 4xx/5xx for errors)

---

## 4. Database Storage

**Criteria:**
Each processed invoice is saved in **PostgreSQL** with the following information:

- **ID** - Unique identifier (auto-generated)
- **Original File Name** - Name of uploaded file
- **Extracted Fields** - All four extracted data points
- **Timestamp** - When the invoice was processed

**Acceptance:**
- ✅ Each processed invoice creates a new database record
- ✅ All required fields are stored correctly
- ✅ Database schema supports all data types appropriately
- ✅ Timestamp is automatically set to current date/time
- ✅ Data persists after application restart

---

## 5. UI Display

**Criteria:**
The UI shows a **table of processed invoices** with the following columns:

- Invoice Number
- Invoice Amount
- Client Name
- Client Address
- Processed Date/Time

**Acceptance:**
- ✅ Table displays all processed invoices from the database
- ✅ All five columns are visible and properly labeled
- ✅ Data is formatted appropriately (currency for amount, date/time format)
- ✅ Table updates after a new invoice is processed
- ✅ Table is responsive and user-friendly

---

## 6. Basic Error Handling

**Criteria:**
If the file is not readable or OCR fails:
- The user sees a **clear error message** in the UI
- The app **does not crash**
- Errors are **logged** for troubleshooting

**Acceptance:**
- ✅ Unreadable files show user-friendly error message
- ✅ OCR failures display appropriate error to user
- ✅ Application remains functional after errors
- ✅ Errors are logged with sufficient detail for debugging
- ✅ User can upload another file after an error

---

## Success Criteria Summary

| # | Criteria | Must Have | Nice to Have |
|---|----------|-----------|--------------|
| 1 | File Upload (PDF/Image) | ✅ | Multiple file upload |
| 2 | Extract 4 fields automatically | ✅ | Additional fields |
| 3 | JSON response | ✅ | XML alternative |
| 4 | PostgreSQL storage | ✅ | Audit trail |
| 5 | UI table display | ✅ | Search/filter |
| 6 | Error handling | ✅ | Retry mechanism |

---

## Out of Scope (for this phase)

- User authentication/authorization
- Invoice editing after extraction
- Bulk file upload
- Export to CSV/Excel
- Invoice validation against business rules
- Email notifications
- Multi-language support

---

**Approved By:** [Technical Lead]
**Date:** [To be filled]
