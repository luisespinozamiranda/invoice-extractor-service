package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST DTO for Invoice (API v1.0)
 * Represents invoice data in REST API requests/responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceV1_0 {

    @JsonProperty("invoice_key")
    private UUID invoiceKey;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("invoice_amount")
    private BigDecimal invoiceAmount;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("client_address")
    private String clientAddress;

    @JsonProperty("issue_date")
    private LocalDate issueDate;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("status")
    private String status;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("is_deleted")
    private Boolean isDeleted;
}
