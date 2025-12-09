package com.training.service.invoiceextractor.adapter.outbound.llm.v1_0;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Data Transfer Object representing extracted invoice data from LLM.
 * This is the structured output we expect from the LLM after analyzing invoice text.
 * Uses Optional to explicitly indicate fields that may not be extractable.
 *
 * @param invoiceNumber Invoice/Document number (Optional)
 * @param amount        Total invoice amount (Optional)
 * @param clientName    Client/Customer name (Optional)
 * @param clientAddress Client address (Optional)
 * @param currency      Currency code (always present, defaults to USD)
 * @param confidence    LLM confidence score (0.0 to 1.0)
 */
public record InvoiceData(
        Optional<String> invoiceNumber,
        Optional<BigDecimal> amount,
        Optional<String> clientName,
        Optional<String> clientAddress,
        String currency,
        double confidence
) {
    /**
     * Create InvoiceData wrapping nullable values in Optional
     */
    public static InvoiceData create(
            String invoiceNumber,
            BigDecimal amount,
            String clientName,
            String clientAddress,
            String currency,
            double confidence
    ) {
        return new InvoiceData(
                Optional.ofNullable(invoiceNumber).filter(s -> !s.isBlank()),
                Optional.ofNullable(amount).filter(a -> a.compareTo(BigDecimal.ZERO) > 0),
                Optional.ofNullable(clientName).filter(s -> !s.isBlank()),
                Optional.ofNullable(clientAddress).filter(s -> !s.isBlank()),
                (currency != null && !currency.isBlank()) ? currency : "USD",
                confidence
        );
    }

    /**
     * Check if at least some critical fields were extracted successfully
     */
    public boolean isValid() {
        // Valid if we have at least invoice number OR amount
        return invoiceNumber.isPresent() || amount.isPresent();
    }
}
