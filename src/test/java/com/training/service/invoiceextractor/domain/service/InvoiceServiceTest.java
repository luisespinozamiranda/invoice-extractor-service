package com.training.service.invoiceextractor.domain.service;

import com.training.service.invoiceextractor.adapter.outbound.database.v1_0.repository.IInvoiceRepositoryService;
import com.training.service.invoiceextractor.domain.model.InvoiceModel;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceService.
 *
 * <p>Tests all invoice business logic operations including CRUD, search,
 * soft delete, and restore functionality.
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-24
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService Tests")
class InvoiceServiceTest {

    @Mock
    private IInvoiceRepositoryService invoiceRepositoryService;

    @InjectMocks
    private InvoiceService invoiceService;

    private InvoiceModel testInvoice;
    private UUID testInvoiceKey;

    @BeforeEach
    void setUp() {
        testInvoiceKey = UUID.randomUUID();
        testInvoice = new InvoiceModel(
                testInvoiceKey,
                "INV-2024-001",
                new BigDecimal("1500.00"),
                "ACME Corp",
                "123 Main Street, Suite 100",
                "USD",
                "ACTIVE",
                "test-invoice.pdf",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // =========================
    // GET BY KEY TESTS
    // =========================

    @Test
    @DisplayName("Should get invoice by key successfully")
    void testGetInvoiceByKey_Success() {
        // Arrange
        when(invoiceRepositoryService.findByInvoiceKey(testInvoiceKey))
                .thenReturn(CompletableFuture.completedFuture(testInvoice));

        // Act
        CompletableFuture<InvoiceModel> result = invoiceService.getInvoiceByKey(testInvoiceKey);

        // Assert
        assertNotNull(result);
        InvoiceModel invoice = result.join();
        assertEquals(testInvoiceKey, invoice.invoiceKey());
        assertEquals("INV-2024-001", invoice.invoiceNumber());
        verify(invoiceRepositoryService, times(1)).findByInvoiceKey(testInvoiceKey);
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void testGetInvoiceByKey_NotFound() {
        // Arrange
        CompletableFuture<InvoiceModel> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(
                new InvoiceExtractorServiceException(
                        ErrorCodes.INVOICE_NOT_FOUND,
                        "Invoice not found"
                )
        );
        when(invoiceRepositoryService.findByInvoiceKey(testInvoiceKey))
                .thenReturn(failedFuture);

        // Act & Assert
        CompletableFuture<InvoiceModel> result = invoiceService.getInvoiceByKey(testInvoiceKey);

        Exception exception = assertThrows(Exception.class, result::join);
        assertTrue(exception.getCause() instanceof InvoiceExtractorServiceException);
        verify(invoiceRepositoryService, times(1)).findByInvoiceKey(testInvoiceKey);
    }

    // =========================
    // GET ALL TESTS
    // =========================

    @Test
    @DisplayName("Should get all active invoices")
    void testGetAllInvoices_Success() {
        // Arrange
        List<InvoiceModel> invoices = Arrays.asList(testInvoice, testInvoice);
        when(invoiceRepositoryService.findAllActive())
                .thenReturn(CompletableFuture.completedFuture(invoices));

        // Act
        CompletableFuture<List<InvoiceModel>> result = invoiceService.getAllInvoices();

        // Assert
        assertNotNull(result);
        List<InvoiceModel> invoiceList = result.join();
        assertEquals(2, invoiceList.size());
        verify(invoiceRepositoryService, times(1)).findAllActive();
    }

    @Test
    @DisplayName("Should return empty list when no invoices exist")
    void testGetAllInvoices_Empty() {
        // Arrange
        when(invoiceRepositoryService.findAllActive())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        // Act
        CompletableFuture<List<InvoiceModel>> result = invoiceService.getAllInvoices();

        // Assert
        assertNotNull(result);
        List<InvoiceModel> invoiceList = result.join();
        assertTrue(invoiceList.isEmpty());
        verify(invoiceRepositoryService, times(1)).findAllActive();
    }

    // =========================
    // CREATE TESTS
    // =========================

    @Test
    @DisplayName("Should create invoice successfully")
    void testCreateInvoice_Success() {
        // Arrange
        when(invoiceRepositoryService.save(any(InvoiceModel.class)))
                .thenReturn(CompletableFuture.completedFuture(testInvoice));

        // Act
        CompletableFuture<InvoiceModel> result = invoiceService.createInvoice(testInvoice);

        // Assert
        assertNotNull(result);
        InvoiceModel created = result.join();
        assertEquals("INV-2024-001", created.invoiceNumber());
        verify(invoiceRepositoryService, times(1)).save(any(InvoiceModel.class));
    }

    // =========================
    // UPDATE TESTS
    // =========================

    @Test
    @DisplayName("Should update invoice successfully")
    void testUpdateInvoice_Success() {
        // Arrange
        InvoiceModel updatedInvoice = new InvoiceModel(
                testInvoiceKey,
                "INV-2024-001-UPDATED",
                new BigDecimal("2000.00"),
                testInvoice.clientName(),
                testInvoice.clientAddress(),
                "USD",
                "PAID",
                testInvoice.originalFileName(),
                testInvoice.createdAt(),
                LocalDateTime.now()
        );

        when(invoiceRepositoryService.findByInvoiceKey(testInvoiceKey))
                .thenReturn(CompletableFuture.completedFuture(testInvoice));
        when(invoiceRepositoryService.save(any(InvoiceModel.class)))
                .thenReturn(CompletableFuture.completedFuture(updatedInvoice));

        // Act
        CompletableFuture<InvoiceModel> result = invoiceService.updateInvoice(testInvoiceKey, updatedInvoice);

        // Assert
        assertNotNull(result);
        InvoiceModel updated = result.join();
        assertEquals("INV-2024-001-UPDATED", updated.invoiceNumber());
        assertEquals(new BigDecimal("2000.00"), updated.invoiceAmount());
        assertEquals("PAID", updated.status());
        verify(invoiceRepositoryService, times(1)).findByInvoiceKey(testInvoiceKey);
        verify(invoiceRepositoryService, times(1)).save(any(InvoiceModel.class));
    }

    // =========================
    // DELETE TESTS
    // =========================

    @Test
    @DisplayName("Should soft delete invoice successfully")
    void testDeleteInvoice_Success() {
        // Arrange
        when(invoiceRepositoryService.softDelete(testInvoiceKey))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = invoiceService.deleteInvoice(testInvoiceKey);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(result::join);
        verify(invoiceRepositoryService, times(1)).softDelete(testInvoiceKey);
    }

    // =========================
    // RESTORE TESTS
    // =========================

    @Test
    @DisplayName("Should restore deleted invoice successfully")
    void testRestoreInvoice_Success() {
        // Arrange
        when(invoiceRepositoryService.restore(testInvoiceKey))
                .thenReturn(CompletableFuture.completedFuture(testInvoice));

        // Act
        CompletableFuture<InvoiceModel> result = invoiceService.restoreInvoice(testInvoiceKey);

        // Assert
        assertNotNull(result);
        InvoiceModel restored = result.join();
        assertEquals(testInvoiceKey, restored.invoiceKey());
        verify(invoiceRepositoryService, times(1)).restore(testInvoiceKey);
    }

    // =========================
    // SEARCH TESTS
    // =========================

    @Test
    @DisplayName("Should search invoices by client name")
    void testSearchByClientName_Success() {
        // Arrange
        String clientName = "ACME Corp";
        List<InvoiceModel> searchResults = Arrays.asList(testInvoice);
        when(invoiceRepositoryService.findByClientName(clientName))
                .thenReturn(CompletableFuture.completedFuture(searchResults));

        // Act
        CompletableFuture<List<InvoiceModel>> result = invoiceService.findByClientName(clientName);

        // Assert
        assertNotNull(result);
        List<InvoiceModel> invoices = result.join();
        assertEquals(1, invoices.size());
        verify(invoiceRepositoryService, times(1)).findByClientName(clientName);
    }

    @Test
    @DisplayName("Should return empty list when no matches found")
    void testSearchByClientName_NoMatches() {
        // Arrange
        String clientName = "NonExistent";
        when(invoiceRepositoryService.findByClientName(clientName))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        // Act
        CompletableFuture<List<InvoiceModel>> result = invoiceService.findByClientName(clientName);

        // Assert
        assertNotNull(result);
        List<InvoiceModel> invoices = result.join();
        assertTrue(invoices.isEmpty());
        verify(invoiceRepositoryService, times(1)).findByClientName(clientName);
    }

    // =========================
    // EXISTS BY KEY TESTS
    // =========================

    @Test
    @DisplayName("Should return true when invoice exists")
    void testExistsByInvoiceKey_Exists() {
        // Arrange
        when(invoiceRepositoryService.existsByInvoiceKey(testInvoiceKey))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Act
        CompletableFuture<Boolean> result = invoiceService.invoiceExists(testInvoiceKey);

        // Assert
        assertNotNull(result);
        assertTrue(result.join());
        verify(invoiceRepositoryService, times(1)).existsByInvoiceKey(testInvoiceKey);
    }

    @Test
    @DisplayName("Should return false when invoice does not exist")
    void testExistsByInvoiceKey_NotExists() {
        // Arrange
        when(invoiceRepositoryService.existsByInvoiceKey(testInvoiceKey))
                .thenReturn(CompletableFuture.completedFuture(false));

        // Act
        CompletableFuture<Boolean> result = invoiceService.invoiceExists(testInvoiceKey);

        // Assert
        assertNotNull(result);
        assertFalse(result.join());
        verify(invoiceRepositoryService, times(1)).existsByInvoiceKey(testInvoiceKey);
    }
}
