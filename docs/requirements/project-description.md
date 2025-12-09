# Project Description - AI-Assisted Invoice Extraction App

## Overview

**Project Name:** AI-Assisted Invoice Extraction App
**Deadline:** December 22, 2025
**Type:** Training Exercise

---

## Tech Stack

- **Frontend:** Angular
- **Backend:** Java (Spring Boot)
- **Database:** PostgreSQL
- **AI/OCR:** AI agent using OCR (external API or AI model)

---

## Project Goal

Build a small application that, given an invoice file (image or PDF), will:

1. Analyze the document using OCR/AI
2. Extract the following fields:
   - Invoice number
   - Invoice amount
   - Client name
   - Client address
3. Store the extracted data in a PostgreSQL database
4. Return and display the extracted data in JSON format
5. Show results in a table in the UI

---

## Core Requirements

### 1. File Upload
- User can upload an invoice in PDF or image format from the Angular UI

### 2. Automatic Data Extraction
After upload, the system automatically extracts:
- Invoice number
- Invoice total amount
- Client name
- Client address

### 3. JSON Response
The backend exposes an endpoint that returns the extracted data in valid JSON format:

```json
{
  "invoiceNumber": "INV-12345",
  "invoiceAmount": 1450.75,
  "clientName": "ACME Corp",
  "clientAddress": "123 Main St, Salt Lake City, UT"
}
```

### 4. Database Storage
Each processed invoice is saved in PostgreSQL with:
- An ID
- Original file name
- Extracted fields
- Timestamp

### 5. UI Display
The UI shows a table of processed invoices listing:
- Invoice number
- Invoice amount
- Client name
- Client address
- Processed date/time

### 6. Basic Error Handling
- If the file is not readable or OCR fails, the user sees a clear error message in the UI
- The app does not crash and logs the error

---

## Deliverables

By **December 22, 2025**, deliver a functioning application meeting all acceptance criteria above.

### Required Documentation:
1. **Technical Acceptance Criteria (TAC)** - 1-2 pages
2. **Technical Requirements Document (TRD)** - Including task breakdown and effort estimates
3. **Working Application** - Full stack implementation

---

## Evaluation Criteria

The Technical Lead will evaluate:

1. **Functionality** - Does it work as expected?
2. **Code Quality & Documentation** - Is the code clean, well-structured, and documented?
3. **AI Usage** - Was AI effectively used in planning, design, and implementation?
4. **Completeness of TAC and TRD** - Are the technical documents thorough and accurate?

---

## Support Provided

- Access to AI tools for code generation and documentation
- Technical guidance from the assigned Tech Lead
- Access to examples, documentation, and internal standards

---

## Consequences

- The Technical Lead will provide written evaluation of both deliverables
- Failure to meet the expectations or deadlines may lead to additional performance actions

---

**Document Version:** 1.0
**Last Updated:** 2025-12-05
