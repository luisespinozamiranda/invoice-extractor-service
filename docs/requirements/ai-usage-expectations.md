# AI Usage Expectations

**Project:** AI-Assisted Invoice Extraction App
**Document Type:** AI Integration Guidelines
**Purpose:** Define how AI should be used throughout the development process
**Version:** 1.0
**Date:** 2025-12-05

---

## Core Principle

**The goal of this exercise is NOT just to code the app, but to learn how to use AI as a development assistant.**

You should use AI (e.g., Claude, ChatGPT, or similar) to help you plan, design, and implement the solution — not to code everything manually from scratch.

---

## Required AI-Assisted Tasks

### 3.1. Create Technical Acceptance Criteria (TAC)

**What to do:**
- Take the business acceptance criteria
- Use AI to transform them into **Technical Acceptance Criteria**

**Expected AI Assistance:**
- API endpoints definitions (URLs, methods, payloads)
- Data model and DB schema
- Expected request/response schemas
- Validation requirements

**Deliverable:**
- **TAC document (1–2 pages)** clearly describing what "done" means from a technical perspective

**Example Prompts:**
```
"Convert these business requirements into technical acceptance criteria including
API endpoints, data models, and validation rules"

"Generate a detailed technical specification for an invoice extraction REST API"
```

---

### 3.2. Create Technical Requirements Document (TRD) with Effort Estimates

**What to do:**
- Use AI to generate a comprehensive **Technical Requirements Document**

**Expected AI Assistance:**
- Architecture overview (frontend, backend, DB, AI/OCR service)
- List of components and services (e.g., InvoiceController, InvoiceService, OcrService, Angular components)
- API contracts (request/response JSON)
- DB schema (PostgreSQL tables/columns)
- Break down work into tasks
- Add estimated effort per task (in hours or story points)

**Deliverable:**
- **TRD + task list with effort estimates**

**Example Prompts:**
```
"Create a technical requirements document for an invoice extraction system
using Java Spring Boot, Angular, and PostgreSQL"

"Break down this project into development tasks with effort estimates"

"Generate a database schema for storing extracted invoice data"
```

---

### 3.3. Design and Implement an AI/OCR Agent

**What to do:**
- Use AI to help design and implement the OCR/extraction pipeline

**Expected AI Assistance:**
- Recommend OCR library or API (e.g., Tesseract, Google Vision, AWS Textract, etc.)
- Explain how to extract structured data from raw OCR text (using regex, prompts, or light NLP)
- Generate code for the OCR component/service

**Component to Build:**
`OcrInvoiceExtractor` that:
- Receives a file
- Performs OCR
- Parses out invoice number, amount, client name, address

**Deliverable:**
- **Working OCR pipeline** integrated into the Java backend

**Example Prompts:**
```
"What's the best OCR solution for extracting invoice data in Java?"

"Generate Java code to extract invoice number, amount, client name,
and address from OCR text"

"How do I integrate Tesseract OCR with Spring Boot?"
```

---

### 3.4. Save JSON to PostgreSQL

**What to do:**
- Use AI to help implement the persistence layer

**Expected AI Assistance:**
- Generate PostgreSQL table definition
- Generate Java entity, repository, and service code
- Implement data access layer

**Deliverable:**
- **Invoices stored in PostgreSQL** and can be queried

**Example Prompts:**
```
"Generate a PostgreSQL table for storing invoice extraction results"

"Create JPA entity and repository for invoice data in Spring Boot"

"Write Spring Data JPA code to save and retrieve invoice records"
```

---

### 3.5. Build the Angular Frontend

**What to do:**
- Use AI to help create the Angular UI components

**Expected AI Assistance:**
- Angular file upload component
- Service to call the backend API
- Table component to display processed invoices

**Components to Implement:**
- Upload page with:
  - Upload button
  - "Process" action
  - Results table

**Deliverable:**
- **Frontend page** allowing upload and visualization

**Example Prompts:**
```
"Generate an Angular component for file upload with drag-and-drop"

"Create an Angular service to call a REST API and upload files"

"Build an Angular table component to display invoice data from an API"
```

---

## AI Tools Recommended

### Primary Tools:
- **Claude** (Anthropic) - Available via claude.ai
- **ChatGPT** (OpenAI) - Available via chat.openai.com
- **GitHub Copilot** - IDE integration (if available)

### Use Cases by Tool:
| Tool | Best For |
|------|----------|
| Claude | Documentation, architecture design, code review |
| ChatGPT | Code generation, debugging, learning |
| Copilot | In-IDE code completion, refactoring |

---

## Best Practices for AI Usage

### DO:
✅ **Ask for explanations** - Don't just copy code, understand it
✅ **Iterate** - Refine prompts based on responses
✅ **Validate** - Test AI-generated code thoroughly
✅ **Document** - Keep track of what AI helped with
✅ **Learn** - Use AI as a teacher, not just a code generator

### DON'T:
❌ **Blindly copy-paste** - Always review and understand
❌ **Skip testing** - AI can make mistakes
❌ **Ignore security** - Review for vulnerabilities
❌ **Forget attribution** - Note where AI was used significantly
❌ **Rely 100% on AI** - Use your own judgment

---

## Documentation of AI Usage

For each deliverable, include a section documenting:

1. **AI Tools Used** - Which AI tools were used
2. **Key Prompts** - Important prompts that generated useful results
3. **AI-Generated vs. Manual** - What percentage was AI-assisted
4. **Modifications Made** - How AI output was adapted
5. **Lessons Learned** - What worked well, what didn't

**Example:**
```markdown
## AI Usage Report

**Tool:** Claude 3.5 Sonnet
**Prompts Used:**
1. "Generate PostgreSQL schema for invoice storage"
2. "Create Spring Data JPA repository for invoices"

**AI Contribution:** ~60% (schema design and basic CRUD)
**Manual Work:** ~40% (custom queries, optimization)

**Modifications:**
- Added composite index for performance
- Changed JSONB column to separate fields
- Added audit columns (created_at, updated_at)

**Lessons:**
- AI provided excellent starting point
- Needed manual optimization for production
```

---

## Evaluation Focus

The Technical Lead will specifically evaluate:

1. **How effectively you used AI** to accelerate development
2. **Quality of prompts** and iterative refinement
3. **Understanding** of AI-generated code (not just copying)
4. **Documentation** of AI assistance in your deliverables
5. **Critical thinking** in adapting AI suggestions

---

## Resources

### AI Prompting Guides:
- [Anthropic's Prompt Engineering Guide](https://docs.anthropic.com/claude/docs/prompt-engineering)
- [OpenAI's Best Practices](https://platform.openai.com/docs/guides/prompt-engineering)

### Technical Documentation:
- Spring Boot: https://spring.io/projects/spring-boot
- Angular: https://angular.io/docs
- PostgreSQL: https://www.postgresql.org/docs/

---

**Remember:** The goal is to learn AI-assisted development, not just to complete the project!

---

**Document Version:** 1.0
**Last Updated:** 2025-12-05
