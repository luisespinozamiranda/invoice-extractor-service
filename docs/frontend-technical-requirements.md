# Frontend Technical Requirements

**Project:** Invoice Extractor - Angular Frontend
**Document Type:** Frontend Technical Requirements
**Version:** 1.0
**Date:** 2025-12-08
**Related Documents:**
- [Business Acceptance Criteria](requirements/business-acceptance-criteria.md)
- [Technical Requirements Document](technical-requirements-document.md)
- [Backend Technical Acceptance Criteria](technical-acceptance-criteria.md)

---

## 1. Technology Stack

### Core Framework
- **Angular:** 16+ (or latest stable version)
- **TypeScript:** 5.0+
- **Node.js:** 18+ LTS
- **npm/yarn:** Latest stable

### UI Components
- **Angular Material:** 16+ (Material Design components)
- **Angular CDK:** For drag-and-drop functionality
- **Flex Layout:** For responsive design (or CSS Grid/Flexbox)

### HTTP & State Management
- **HttpClient:** Angular's built-in HTTP client
- **RxJS:** 7+ for reactive programming
- **Optional:** NgRx or Akita for state management (if needed for complexity)

### File Handling
- **ng2-file-upload** or native HTML5 File API
- **ngx-dropzone** for drag-and-drop file upload

### Additional Libraries
- **date-fns** or **moment.js:** Date formatting and manipulation
- **ngx-toastr:** Toast notifications for user feedback

---

## 2. Project Structure

```
invoice-extractor-frontend/
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── services/
│   │   │   │   ├── invoice.service.ts          # API communication
│   │   │   │   ├── file-upload.service.ts      # File handling
│   │   │   │   └── error-handler.service.ts    # Error handling
│   │   │   ├── interceptors/
│   │   │   │   ├── http-error.interceptor.ts   # Global error handling
│   │   │   │   └── loading.interceptor.ts      # Loading state
│   │   │   ├── models/
│   │   │   │   ├── invoice.model.ts            # Invoice interface
│   │   │   │   ├── extraction.model.ts         # Extraction response
│   │   │   │   └── error-response.model.ts     # Error response
│   │   │   └── guards/
│   │   │       └── can-deactivate.guard.ts     # Navigation guard
│   │   │
│   │   ├── features/
│   │   │   ├── invoice-upload/
│   │   │   │   ├── invoice-upload.component.ts
│   │   │   │   ├── invoice-upload.component.html
│   │   │   │   ├── invoice-upload.component.scss
│   │   │   │   └── invoice-upload.component.spec.ts
│   │   │   │
│   │   │   └── invoice-list/
│   │   │       ├── invoice-list.component.ts
│   │   │       ├── invoice-list.component.html
│   │   │       ├── invoice-list.component.scss
│   │   │       └── invoice-list.component.spec.ts
│   │   │
│   │   ├── shared/
│   │   │   ├── components/
│   │   │   │   ├── loading-spinner/
│   │   │   │   ├── error-message/
│   │   │   │   └── confirmation-dialog/
│   │   │   ├── pipes/
│   │   │   │   ├── currency-format.pipe.ts
│   │   │   │   └── date-format.pipe.ts
│   │   │   └── directives/
│   │   │       └── file-drag-drop.directive.ts
│   │   │
│   │   ├── app.component.ts
│   │   ├── app.component.html
│   │   ├── app.component.scss
│   │   ├── app.routes.ts                        # Angular 16+ routing
│   │   └── app.config.ts                        # App configuration
│   │
│   ├── assets/
│   │   ├── images/
│   │   └── styles/
│   │       └── _variables.scss                  # SCSS variables
│   │
│   ├── environments/
│   │   ├── environment.ts                       # Development
│   │   └── environment.prod.ts                  # Production
│   │
│   ├── styles.scss                              # Global styles
│   ├── index.html
│   └── main.ts
│
├── angular.json
├── package.json
├── tsconfig.json
└── README.md
```

---

## 3. API Integration

### 3.1 Backend API Configuration

**Environment Configuration:**

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/invoice-extractor-service/api/v1.0',
  uploadTimeout: 30000, // 30 seconds
  maxFileSize: 10485760, // 10 MB in bytes
  allowedFileTypes: ['application/pdf', 'image/png', 'image/jpg', 'image/jpeg']
};

// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.production.com/invoice-extractor-service/api/v1.0',
  uploadTimeout: 30000,
  maxFileSize: 10485760,
  allowedFileTypes: ['application/pdf', 'image/png', 'image/jpg', 'image/jpeg']
};
```

### 3.2 Data Models

**Invoice Model:**

```typescript
// src/app/core/models/invoice.model.ts
export interface Invoice {
  invoiceKey: string;
  invoiceNumber: string;
  invoiceAmount: number;
  clientName: string;
  clientAddress: string;
  vendorKey?: string;
  issueDate?: string;
  dueDate?: string;
  currency: string;
  status: InvoiceStatus;
  createdAt: string;
  updatedAt: string;
}

export enum InvoiceStatus {
  PROCESSING = 'PROCESSING',
  EXTRACTED = 'EXTRACTED',
  EXTRACTION_FAILED = 'EXTRACTION_FAILED',
  PENDING = 'PENDING'
}

export interface InvoiceListResponse {
  content: Invoice[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

**Extraction Models:**

```typescript
// src/app/core/models/extraction.model.ts
export interface ExtractionRequest {
  file: File;
}

export interface ExtractionResponse {
  extractionKey: string;
  fileName: string;
  status: string;
  uploadedAt: string;
}

export interface ExtractionMetadata {
  extractionKey: string;
  invoiceKey: string;
  sourceFileName: string;
  extractionTimestamp: string;
  extractionStatus: string;
  confidenceScore?: number;
  ocrEngine?: string;
  errorMessage?: string;
}
```

**Error Response Model:**

```typescript
// src/app/core/models/error-response.model.ts
export interface ErrorResponse {
  errorCode: string;
  message: string;
  timestamp: string;
  details?: Record<string, any>;
}
```

### 3.3 Invoice Service

```typescript
// src/app/core/services/invoice.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Invoice, InvoiceListResponse } from '../models/invoice.model';
import { ExtractionResponse } from '../models/extraction.model';

@Injectable({
  providedIn: 'root'
})
export class InvoiceService {
  private readonly apiUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  /**
   * Upload invoice file for extraction
   * @param file - File to upload (PDF, PNG, JPG, JPEG)
   * @returns Observable with extraction response
   */
  uploadInvoice(file: File): Observable<ExtractionResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ExtractionResponse>(
      `${this.apiUrl}/invoices/upload`,
      formData,
      {
        reportProgress: true,
        observe: 'body'
      }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Get invoice by key
   * @param invoiceKey - UUID of the invoice
   * @returns Observable with invoice details
   */
  getInvoiceByKey(invoiceKey: string): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}/invoices/${invoiceKey}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Get all invoices with pagination
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param sort - Sort parameter (e.g., 'createdAt,desc')
   * @returns Observable with paginated invoice list
   */
  getAllInvoices(
    page: number = 0,
    size: number = 20,
    sort: string = 'createdAt,desc'
  ): Observable<InvoiceListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<InvoiceListResponse>(`${this.apiUrl}/invoices`, { params })
      .pipe(catchError(this.handleError));
  }

  /**
   * Delete invoice by key
   * @param invoiceKey - UUID of the invoice
   * @returns Observable with void
   */
  deleteInvoice(invoiceKey: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/invoices/${invoiceKey}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('API Error:', error);
    return throwError(() => error);
  }
}
```

### 3.4 File Upload Service

```typescript
// src/app/core/services/file-upload.service.ts
import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FileUploadService {
  private readonly maxFileSize = environment.maxFileSize;
  private readonly allowedFileTypes = environment.allowedFileTypes;

  /**
   * Validate file before upload
   * @param file - File to validate
   * @returns Validation result with error message if invalid
   */
  validateFile(file: File): { isValid: boolean; errorMessage?: string } {
    // Check file size
    if (file.size > this.maxFileSize) {
      return {
        isValid: false,
        errorMessage: `File size exceeds maximum allowed size of ${this.formatFileSize(this.maxFileSize)}`
      };
    }

    // Check file type
    if (!this.allowedFileTypes.includes(file.type)) {
      return {
        isValid: false,
        errorMessage: 'Invalid file type. Accepted types: PDF, PNG, JPG, JPEG'
      };
    }

    return { isValid: true };
  }

  /**
   * Format file size in human-readable format
   * @param bytes - File size in bytes
   * @returns Formatted file size (e.g., "10 MB")
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }

  /**
   * Get file extension from filename
   * @param filename - Name of the file
   * @returns File extension (e.g., "pdf")
   */
  getFileExtension(filename: string): string {
    return filename.split('.').pop()?.toLowerCase() || '';
  }

  /**
   * Check if file type is allowed
   * @param fileType - MIME type of the file
   * @returns True if file type is allowed
   */
  isFileTypeAllowed(fileType: string): boolean {
    return this.allowedFileTypes.includes(fileType);
  }
}
```

---

## 4. Component Specifications

### 4.1 Invoice Upload Component

**Component Responsibilities:**
- Display file upload interface with drag-and-drop
- Validate file type and size before upload
- Show upload progress indicator
- Display success/error notifications
- Trigger invoice list refresh on successful upload

**Component Structure:**

```typescript
// src/app/features/invoice-upload/invoice-upload.component.ts
import { Component, EventEmitter, Output } from '@angular/core';
import { InvoiceService } from '../../core/services/invoice.service';
import { FileUploadService } from '../../core/services/file-upload.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-invoice-upload',
  templateUrl: './invoice-upload.component.html',
  styleUrls: ['./invoice-upload.component.scss']
})
export class InvoiceUploadComponent {
  @Output() uploadSuccess = new EventEmitter<void>();

  selectedFile: File | null = null;
  isUploading = false;
  uploadProgress = 0;
  isDragOver = false;

  constructor(
    private invoiceService: InvoiceService,
    private fileUploadService: FileUploadService,
    private toastr: ToastrService
  ) {}

  /**
   * Handle file selection from input
   */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  /**
   * Handle file drop
   */
  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.handleFile(event.dataTransfer.files[0]);
    }
  }

  /**
   * Handle drag over event
   */
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  /**
   * Handle drag leave event
   */
  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  /**
   * Process selected file
   */
  private handleFile(file: File): void {
    const validation = this.fileUploadService.validateFile(file);

    if (!validation.isValid) {
      this.toastr.error(validation.errorMessage, 'Invalid File');
      return;
    }

    this.selectedFile = file;
  }

  /**
   * Upload selected file
   */
  uploadFile(): void {
    if (!this.selectedFile) {
      this.toastr.warning('Please select a file to upload', 'No File Selected');
      return;
    }

    this.isUploading = true;
    this.uploadProgress = 0;

    this.invoiceService.uploadInvoice(this.selectedFile).subscribe({
      next: (response) => {
        this.isUploading = false;
        this.uploadProgress = 100;
        this.toastr.success(
          `Invoice uploaded successfully! File: ${response.fileName}`,
          'Upload Successful'
        );
        this.resetUpload();
        this.uploadSuccess.emit();
      },
      error: (error) => {
        this.isUploading = false;
        this.uploadProgress = 0;
        const errorMessage = error.error?.message || 'Failed to upload invoice. Please try again.';
        this.toastr.error(errorMessage, 'Upload Failed');
      }
    });
  }

  /**
   * Reset upload state
   */
  resetUpload(): void {
    this.selectedFile = null;
    this.uploadProgress = 0;
  }

  /**
   * Get formatted file size
   */
  getFileSize(): string {
    return this.selectedFile
      ? this.fileUploadService.formatFileSize(this.selectedFile.size)
      : '';
  }
}
```

**Component Template:**

```html
<!-- src/app/features/invoice-upload/invoice-upload.component.html -->
<mat-card class="upload-card">
  <mat-card-header>
    <mat-card-title>Upload Invoice</mat-card-title>
    <mat-card-subtitle>Upload PDF or image files (PNG, JPG, JPEG) - Max 10 MB</mat-card-subtitle>
  </mat-card-header>

  <mat-card-content>
    <!-- Drag & Drop Zone -->
    <div
      class="drop-zone"
      [class.drag-over]="isDragOver"
      [class.has-file]="selectedFile"
      (drop)="onFileDrop($event)"
      (dragover)="onDragOver($event)"
      (dragleave)="onDragLeave($event)"
    >
      <mat-icon class="upload-icon">cloud_upload</mat-icon>

      <p *ngIf="!selectedFile" class="drop-message">
        Drag and drop your invoice here or click to browse
      </p>

      <div *ngIf="selectedFile" class="file-info">
        <mat-icon>description</mat-icon>
        <div class="file-details">
          <p class="file-name">{{ selectedFile.name }}</p>
          <p class="file-size">{{ getFileSize() }}</p>
        </div>
        <button mat-icon-button (click)="resetUpload()" [disabled]="isUploading">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <input
        #fileInput
        type="file"
        accept=".pdf,.png,.jpg,.jpeg"
        (change)="onFileSelected($event)"
        style="display: none"
      />

      <button
        mat-raised-button
        color="primary"
        (click)="fileInput.click()"
        [disabled]="isUploading"
        *ngIf="!selectedFile"
      >
        Browse Files
      </button>
    </div>

    <!-- Upload Progress -->
    <div *ngIf="isUploading" class="upload-progress">
      <mat-progress-bar mode="indeterminate" color="accent"></mat-progress-bar>
      <p class="progress-text">Processing invoice... Please wait.</p>
    </div>
  </mat-card-content>

  <mat-card-actions align="end">
    <button
      mat-button
      (click)="resetUpload()"
      [disabled]="isUploading || !selectedFile"
    >
      Cancel
    </button>
    <button
      mat-raised-button
      color="primary"
      (click)="uploadFile()"
      [disabled]="isUploading || !selectedFile"
    >
      <mat-icon>upload</mat-icon>
      Upload & Extract
    </button>
  </mat-card-actions>
</mat-card>
```

**Component Styles:**

```scss
// src/app/features/invoice-upload/invoice-upload.component.scss
.upload-card {
  max-width: 600px;
  margin: 20px auto;
}

.drop-zone {
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  &.drag-over {
    border-color: #3f51b5;
    background-color: rgba(63, 81, 181, 0.05);
  }

  &.has-file {
    border-color: #4caf50;
    background-color: rgba(76, 175, 80, 0.05);
  }
}

.upload-icon {
  font-size: 64px;
  width: 64px;
  height: 64px;
  color: #757575;
  margin-bottom: 16px;
}

.drop-message {
  color: #757575;
  margin: 16px 0;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px;
  background-color: #f5f5f5;
  border-radius: 4px;

  mat-icon {
    color: #4caf50;
  }

  .file-details {
    flex: 1;
    text-align: left;

    .file-name {
      margin: 0;
      font-weight: 500;
    }

    .file-size {
      margin: 4px 0 0 0;
      font-size: 12px;
      color: #757575;
    }
  }
}

.upload-progress {
  margin-top: 20px;

  .progress-text {
    text-align: center;
    margin-top: 8px;
    color: #757575;
  }
}
```

### 4.2 Invoice List Component

**Component Responsibilities:**
- Display paginated table of invoices
- Sort by columns
- Format currency and dates
- Refresh list after new uploads
- Handle loading and error states

**Component Structure:**

```typescript
// src/app/features/invoice-list/invoice-list.component.ts
import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { InvoiceService } from '../../core/services/invoice.service';
import { Invoice } from '../../core/models/invoice.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-invoice-list',
  templateUrl: './invoice-list.component.html',
  styleUrls: ['./invoice-list.component.scss']
})
export class InvoiceListComponent implements OnInit {
  displayedColumns: string[] = [
    'invoiceNumber',
    'invoiceAmount',
    'clientName',
    'clientAddress',
    'createdAt',
    'actions'
  ];

  dataSource: MatTableDataSource<Invoice>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private invoiceService: InvoiceService,
    private toastr: ToastrService
  ) {
    this.dataSource = new MatTableDataSource<Invoice>([]);
  }

  ngOnInit(): void {
    this.loadInvoices();
  }

  /**
   * Load invoices from API
   */
  loadInvoices(): void {
    this.isLoading = true;

    const sortParam = this.sort?.active
      ? `${this.sort.active},${this.sort.direction}`
      : 'createdAt,desc';

    this.invoiceService.getAllInvoices(this.currentPage, this.pageSize, sortParam)
      .subscribe({
        next: (response) => {
          this.dataSource.data = response.content;
          this.totalElements = response.totalElements;
          this.isLoading = false;
        },
        error: (error) => {
          this.isLoading = false;
          this.toastr.error('Failed to load invoices', 'Error');
          console.error('Error loading invoices:', error);
        }
      });
  }

  /**
   * Handle page change event
   */
  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadInvoices();
  }

  /**
   * Handle sort change event
   */
  onSortChange(): void {
    this.currentPage = 0;
    this.loadInvoices();
  }

  /**
   * Refresh invoice list (called after upload)
   */
  refreshList(): void {
    this.currentPage = 0;
    this.loadInvoices();
  }

  /**
   * Format currency for display
   */
  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }

  /**
   * Format date for display
   */
  formatDate(isoDate: string): string {
    return new Date(isoDate).toLocaleString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /**
   * Truncate text if too long
   */
  truncateText(text: string, maxLength: number): string {
    return text.length > maxLength
      ? text.substring(0, maxLength) + '...'
      : text;
  }

  /**
   * Delete invoice
   */
  deleteInvoice(invoiceKey: string): void {
    if (confirm('Are you sure you want to delete this invoice?')) {
      this.invoiceService.deleteInvoice(invoiceKey).subscribe({
        next: () => {
          this.toastr.success('Invoice deleted successfully', 'Success');
          this.loadInvoices();
        },
        error: (error) => {
          this.toastr.error('Failed to delete invoice', 'Error');
          console.error('Error deleting invoice:', error);
        }
      });
    }
  }
}
```

**Component Template:**

```html
<!-- src/app/features/invoice-list/invoice-list.component.html -->
<mat-card class="invoice-list-card">
  <mat-card-header>
    <mat-card-title>Processed Invoices</mat-card-title>
    <mat-card-subtitle>View all extracted invoice data</mat-card-subtitle>
  </mat-card-header>

  <mat-card-content>
    <!-- Loading Spinner -->
    <div *ngIf="isLoading" class="loading-container">
      <mat-spinner diameter="50"></mat-spinner>
      <p>Loading invoices...</p>
    </div>

    <!-- Invoice Table -->
    <div *ngIf="!isLoading" class="table-container">
      <table mat-table [dataSource]="dataSource" matSort (matSortChange)="onSortChange()">

        <!-- Invoice Number Column -->
        <ng-container matColumnDef="invoiceNumber">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Invoice Number</th>
          <td mat-cell *matCellDef="let invoice">{{ invoice.invoiceNumber }}</td>
        </ng-container>

        <!-- Invoice Amount Column -->
        <ng-container matColumnDef="invoiceAmount">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Amount</th>
          <td mat-cell *matCellDef="let invoice" class="amount-cell">
            {{ formatCurrency(invoice.invoiceAmount) }}
          </td>
        </ng-container>

        <!-- Client Name Column -->
        <ng-container matColumnDef="clientName">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Client Name</th>
          <td mat-cell *matCellDef="let invoice">
            {{ truncateText(invoice.clientName, 50) }}
          </td>
        </ng-container>

        <!-- Client Address Column -->
        <ng-container matColumnDef="clientAddress">
          <th mat-header-cell *matHeaderCellDef>Client Address</th>
          <td mat-cell *matCellDef="let invoice">
            {{ truncateText(invoice.clientAddress, 75) }}
          </td>
        </ng-container>

        <!-- Created At Column -->
        <ng-container matColumnDef="createdAt">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Processed Date</th>
          <td mat-cell *matCellDef="let invoice">
            {{ formatDate(invoice.createdAt) }}
          </td>
        </ng-container>

        <!-- Actions Column -->
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef>Actions</th>
          <td mat-cell *matCellDef="let invoice">
            <button mat-icon-button [matMenuTriggerFor]="menu">
              <mat-icon>more_vert</mat-icon>
            </button>
            <mat-menu #menu="matMenu">
              <button mat-menu-item (click)="deleteInvoice(invoice.invoiceKey)">
                <mat-icon>delete</mat-icon>
                <span>Delete</span>
              </button>
            </mat-menu>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

        <!-- Empty State -->
        <tr class="mat-row" *matNoDataRow>
          <td class="mat-cell empty-state" [attr.colspan]="displayedColumns.length">
            <mat-icon>inbox</mat-icon>
            <p>No invoices found. Upload an invoice to get started!</p>
          </td>
        </tr>
      </table>

      <!-- Paginator -->
      <mat-paginator
        [length]="totalElements"
        [pageSize]="pageSize"
        [pageSizeOptions]="[10, 20, 50, 100]"
        (page)="onPageChange($event)"
        showFirstLastButtons
      ></mat-paginator>
    </div>
  </mat-card-content>
</mat-card>
```

**Component Styles:**

```scss
// src/app/features/invoice-list/invoice-list.component.scss
.invoice-list-card {
  margin: 20px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;

  p {
    margin-top: 16px;
    color: #757575;
  }
}

.table-container {
  overflow-x: auto;
}

table {
  width: 100%;

  .amount-cell {
    font-weight: 500;
    color: #4caf50;
  }

  .empty-state {
    text-align: center;
    padding: 40px;

    mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #bdbdbd;
    }

    p {
      margin-top: 16px;
      color: #757575;
    }
  }
}

// Responsive Design
@media (max-width: 768px) {
  .table-container {
    table {
      font-size: 12px;
    }

    th, td {
      padding: 8px 4px;
    }
  }
}
```

---

## 5. Shared Components & Utilities

### 5.1 Currency Format Pipe

```typescript
// src/app/shared/pipes/currency-format.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'currencyFormat'
})
export class CurrencyFormatPipe implements PipeTransform {
  transform(value: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(value);
  }
}
```

### 5.2 Date Format Pipe

```typescript
// src/app/shared/pipes/date-format.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'dateFormat'
})
export class DateFormatPipe implements PipeTransform {
  transform(value: string, format: 'short' | 'long' = 'short'): string {
    const date = new Date(value);

    if (format === 'short') {
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    }

    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }
}
```

### 5.3 HTTP Error Interceptor

```typescript
// src/app/core/interceptors/http-error.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ToastrService } from 'ngx-toastr';

@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  constructor(private toastr: ToastrService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'An unexpected error occurred';

        if (error.error instanceof ErrorEvent) {
          // Client-side error
          errorMessage = `Error: ${error.error.message}`;
        } else {
          // Server-side error
          if (error.error?.message) {
            errorMessage = error.error.message;
          } else {
            errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
          }
        }

        console.error('HTTP Error:', error);

        // Don't show toast for specific handled errors
        if (!req.headers.has('X-Skip-Error-Notification')) {
          this.toastr.error(errorMessage, 'Error');
        }

        return throwError(() => error);
      })
    );
  }
}
```

### 5.4 Loading Spinner Component

```typescript
// src/app/shared/components/loading-spinner/loading-spinner.component.ts
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  template: `
    <div class="spinner-container" *ngIf="isLoading">
      <mat-spinner [diameter]="diameter"></mat-spinner>
      <p *ngIf="message" class="spinner-message">{{ message }}</p>
    </div>
  `,
  styles: [`
    .spinner-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px;

      .spinner-message {
        margin-top: 16px;
        color: #757575;
      }
    }
  `]
})
export class LoadingSpinnerComponent {
  @Input() isLoading = false;
  @Input() diameter = 50;
  @Input() message = '';
}
```

---

## 6. Routing Configuration

```typescript
// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { InvoiceUploadComponent } from './features/invoice-upload/invoice-upload.component';
import { InvoiceListComponent } from './features/invoice-list/invoice-list.component';

export const routes: Routes = [
  { path: '', redirectTo: '/invoices', pathMatch: 'full' },
  { path: 'upload', component: InvoiceUploadComponent },
  { path: 'invoices', component: InvoiceListComponent },
  { path: '**', redirectTo: '/invoices' }
];
```

---

## 7. Testing Requirements

### 7.1 Unit Tests

**Service Tests:**
```typescript
// src/app/core/services/invoice.service.spec.ts
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { InvoiceService } from './invoice.service';
import { environment } from '../../../environments/environment';

describe('InvoiceService', () => {
  let service: InvoiceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [InvoiceService]
    });

    service = TestBed.inject(InvoiceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should upload invoice file', () => {
    const mockFile = new File([''], 'test.pdf', { type: 'application/pdf' });
    const mockResponse = {
      extractionKey: '123',
      fileName: 'test.pdf',
      status: 'PROCESSING',
      uploadedAt: '2025-12-08T10:00:00Z'
    };

    service.uploadInvoice(mockFile).subscribe(response => {
      expect(response).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/invoices/upload`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should get all invoices', () => {
    const mockResponse = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0
    };

    service.getAllInvoices().subscribe(response => {
      expect(response).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/invoices?page=0&size=20&sort=createdAt,desc`
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });
});
```

**Component Tests:**
```typescript
// src/app/features/invoice-upload/invoice-upload.component.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InvoiceUploadComponent } from './invoice-upload.component';
import { InvoiceService } from '../../core/services/invoice.service';
import { FileUploadService } from '../../core/services/file-upload.service';
import { ToastrService } from 'ngx-toastr';
import { of, throwError } from 'rxjs';

describe('InvoiceUploadComponent', () => {
  let component: InvoiceUploadComponent;
  let fixture: ComponentFixture<InvoiceUploadComponent>;
  let mockInvoiceService: jasmine.SpyObj<InvoiceService>;
  let mockFileUploadService: jasmine.SpyObj<FileUploadService>;
  let mockToastrService: jasmine.SpyObj<ToastrService>;

  beforeEach(async () => {
    mockInvoiceService = jasmine.createSpyObj('InvoiceService', ['uploadInvoice']);
    mockFileUploadService = jasmine.createSpyObj('FileUploadService', ['validateFile', 'formatFileSize']);
    mockToastrService = jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']);

    await TestBed.configureTestingModule({
      declarations: [InvoiceUploadComponent],
      providers: [
        { provide: InvoiceService, useValue: mockInvoiceService },
        { provide: FileUploadService, useValue: mockFileUploadService },
        { provide: ToastrService, useValue: mockToastrService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InvoiceUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should validate file before upload', () => {
    const mockFile = new File([''], 'test.pdf', { type: 'application/pdf' });
    mockFileUploadService.validateFile.and.returnValue({ isValid: true });

    component['handleFile'](mockFile);

    expect(mockFileUploadService.validateFile).toHaveBeenCalledWith(mockFile);
    expect(component.selectedFile).toEqual(mockFile);
  });

  it('should show error for invalid file', () => {
    const mockFile = new File([''], 'test.txt', { type: 'text/plain' });
    mockFileUploadService.validateFile.and.returnValue({
      isValid: false,
      errorMessage: 'Invalid file type'
    });

    component['handleFile'](mockFile);

    expect(mockToastrService.error).toHaveBeenCalledWith('Invalid file type', 'Invalid File');
    expect(component.selectedFile).toBeNull();
  });

  it('should upload file successfully', () => {
    const mockFile = new File([''], 'test.pdf', { type: 'application/pdf' });
    const mockResponse = {
      extractionKey: '123',
      fileName: 'test.pdf',
      status: 'PROCESSING',
      uploadedAt: '2025-12-08T10:00:00Z'
    };

    component.selectedFile = mockFile;
    mockInvoiceService.uploadInvoice.and.returnValue(of(mockResponse));

    component.uploadFile();

    expect(mockInvoiceService.uploadInvoice).toHaveBeenCalledWith(mockFile);
    expect(mockToastrService.success).toHaveBeenCalled();
    expect(component.selectedFile).toBeNull();
  });

  it('should handle upload error', () => {
    const mockFile = new File([''], 'test.pdf', { type: 'application/pdf' });
    const mockError = { error: { message: 'Upload failed' } };

    component.selectedFile = mockFile;
    mockInvoiceService.uploadInvoice.and.returnValue(throwError(() => mockError));

    component.uploadFile();

    expect(mockToastrService.error).toHaveBeenCalledWith('Upload failed', 'Upload Failed');
    expect(component.isUploading).toBeFalse();
  });
});
```

### 7.2 E2E Tests (Protractor/Cypress)

```typescript
// e2e/src/invoice-upload.e2e-spec.ts
describe('Invoice Upload Flow', () => {
  beforeEach(() => {
    cy.visit('/upload');
  });

  it('should display upload form', () => {
    cy.contains('Upload Invoice').should('be.visible');
    cy.get('.drop-zone').should('be.visible');
  });

  it('should upload a valid PDF file', () => {
    const fileName = 'test-invoice.pdf';

    cy.fixture(fileName).then(fileContent => {
      cy.get('input[type="file"]').attachFile({
        fileContent: fileContent.toString(),
        fileName: fileName,
        mimeType: 'application/pdf'
      });
    });

    cy.contains('Upload & Extract').click();
    cy.contains('Upload Successful', { timeout: 35000 }).should('be.visible');
  });

  it('should show error for invalid file type', () => {
    const fileName = 'test.txt';

    cy.fixture(fileName).then(fileContent => {
      cy.get('input[type="file"]').attachFile({
        fileContent: fileContent.toString(),
        fileName: fileName,
        mimeType: 'text/plain'
      });
    });

    cy.contains('Invalid File').should('be.visible');
  });
});

describe('Invoice List', () => {
  beforeEach(() => {
    cy.visit('/invoices');
  });

  it('should display invoice table', () => {
    cy.get('table').should('be.visible');
    cy.contains('Invoice Number').should('be.visible');
    cy.contains('Amount').should('be.visible');
  });

  it('should paginate results', () => {
    cy.get('mat-paginator').should('be.visible');
    cy.contains('Next page').click();
  });

  it('should sort by column', () => {
    cy.contains('Invoice Number').click();
    cy.get('table tbody tr').first().should('exist');
  });
});
```

---

## 8. Build & Deployment

### 8.1 Build Configuration

**package.json scripts:**
```json
{
  "scripts": {
    "ng": "ng",
    "start": "ng serve",
    "build": "ng build",
    "build:prod": "ng build --configuration production",
    "test": "ng test",
    "test:ci": "ng test --watch=false --browsers=ChromeHeadless",
    "lint": "ng lint",
    "e2e": "ng e2e"
  }
}
```

### 8.2 Environment Configuration

**Production Build:**
```bash
ng build --configuration production --base-href /invoice-extractor/
```

**Output:**
- Build artifacts stored in `dist/invoice-extractor-frontend/`
- Optimized bundles with tree-shaking and minification
- Source maps generated for debugging

### 8.3 Deployment Checklist

- [ ] Update `environment.prod.ts` with production API URL
- [ ] Run `ng build --configuration production`
- [ ] Test build artifacts locally (`ng serve --configuration production`)
- [ ] Deploy to web server (Nginx, Apache, or cloud hosting)
- [ ] Configure CORS on backend for frontend origin
- [ ] Set up HTTPS certificate
- [ ] Test all features in production environment

---

## 9. Definition of Done - Frontend

### Core Features
- [ ] File upload component with drag-and-drop UI implemented
- [ ] Client-side file validation (type and size) working
- [ ] Upload progress indicator displays during processing
- [ ] Success/error notifications display correctly
- [ ] Invoice list table displays all 5 required columns
- [ ] Pagination works (20 records per page)
- [ ] Sorting by columns functional
- [ ] Currency formatting displays as `$1,450.75`
- [ ] Date formatting displays in local timezone
- [ ] Auto-refresh after successful upload works
- [ ] Delete invoice functionality implemented

### API Integration
- [ ] InvoiceService communicates with backend API
- [ ] All HTTP requests include proper headers
- [ ] Error responses handled gracefully
- [ ] HTTP interceptor catches and logs errors
- [ ] Loading states managed correctly

### Testing
- [ ] Unit tests for services pass (> 80% coverage)
- [ ] Unit tests for components pass (> 70% coverage)
- [ ] E2E tests for critical flows pass
- [ ] All linting checks pass (`ng lint`)

### Responsive Design
- [ ] Mobile view (< 768px) works correctly
- [ ] Tablet view (768px - 1024px) works correctly
- [ ] Desktop view (> 1024px) works correctly
- [ ] Touch interactions work on mobile devices

### Performance
- [ ] Initial load time < 3 seconds
- [ ] Bundle size optimized (< 2 MB gzipped)
- [ ] Lazy loading implemented where applicable
- [ ] Images and assets optimized

### Documentation
- [ ] README.md with setup instructions
- [ ] Component documentation (JSDoc comments)
- [ ] API service documentation
- [ ] Environment configuration documented

---

## 10. Nice-to-Have Features (Future Enhancements)

### Phase 2 Features
- [ ] Search/filter invoices by invoice number, client name, or date range
- [ ] Export invoice list to CSV/Excel
- [ ] Bulk upload (multiple files at once)
- [ ] Invoice detail view (modal or separate page)
- [ ] Edit extracted data before saving
- [ ] Dark mode theme
- [ ] Multilingual support (i18n)

### Phase 3 Features
- [ ] Real-time extraction status updates (WebSocket)
- [ ] Invoice templates for common vendors
- [ ] OCR confidence score visualization
- [ ] Invoice approval workflow
- [ ] Email notifications for processed invoices
- [ ] Mobile app (Ionic/React Native)

---

**Document Status:** Draft
**Approval Pending:** Technical Lead
**Last Updated:** 2025-12-08
