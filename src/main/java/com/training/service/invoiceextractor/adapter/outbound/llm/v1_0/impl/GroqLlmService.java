package com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.ILlmExtractionService;
import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Groq LLM service implementation for invoice data extraction.
 * Uses Groq's free API with Llama 3.1 model for intelligent field extraction.
 */
@Service
@Slf4j
public class GroqLlmService implements ILlmExtractionService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public GroqLlmService(
            @Value("${llm.groq.api-key:}") String apiKey,
            @Value("${llm.groq.model:llama-3.1-70b-versatile}") String model,
            @Value("${llm.groq.enabled:false}") boolean enabled,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model != null && !model.isBlank() ? model : DEFAULT_MODEL;
        this.enabled = enabled;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();

        if (enabled && (apiKey == null || apiKey.isBlank())) {
            log.warn("Groq LLM is enabled but API key is not configured. Service will not be available.");
        } else if (enabled) {
            log.info("Groq LLM service initialized with model: {}", this.model);
        }
    }

    @Override
    public CompletableFuture<InvoiceData> extractInvoiceData(String ocrText) {
        if (!isAvailable()) {
            log.warn("Groq LLM service is not available. Returning default values.");
            return CompletableFuture.completedFuture(createDefaultInvoiceData());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Sending OCR text to Groq LLM for extraction");
                String prompt = buildExtractionPrompt(ocrText);
                String requestBody = buildRequestBody(prompt);

                Request request = new Request.Builder()
                        .url(GROQ_API_URL)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        log.error("Groq API error: {} - {} - Body: {}", response.code(), response.message(), errorBody);
                        return createDefaultInvoiceData();
                    }

                    String responseBody = response.body().string();
                    return parseGroqResponse(responseBody);
                }

            } catch (Exception ex) {
                log.error("Error calling Groq LLM API", ex);
                return createDefaultInvoiceData();
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "Groq (" + model + ")";
    }

    /**
     * Build the extraction prompt for the LLM.
     * This prompt instructs the model to extract invoice fields in JSON format.
     */
    private String buildExtractionPrompt(String ocrText) {
        return """
                You are an expert at extracting structured data from invoices.

                Extract the following fields from the invoice text below:
                - invoice_number: The invoice or document number
                - amount: The total invoice amount (as a decimal number, without currency symbols)
                - client_name: The customer or "Bill To" name
                - client_address: The customer address (if available)
                - currency: The currency code (e.g., USD, EUR, MXN)

                Return ONLY a valid JSON object with these exact field names. No explanations.
                If a field is not found, use null for strings or 0 for amount.

                Invoice text:
                %s

                JSON output:
                """.formatted(ocrText);
    }

    /**
     * Build the request body for Groq API in OpenAI-compatible format.
     */
    private String buildRequestBody(String prompt) {
        try {
            var request = new java.util.HashMap<String, Object>();
            request.put("model", model);
            request.put("messages", new Object[]{
                    java.util.Map.of("role", "user", "content", prompt)
            });
            request.put("temperature", 0.1); // Low temperature for consistent, factual extraction
            request.put("max_tokens", 2048);
            request.put("response_format", java.util.Map.of("type", "json_object")); // Force JSON output

            return objectMapper.writeValueAsString(request);
        } catch (Exception ex) {
            log.error("Error building request body", ex);
            return "{}";
        }
    }

    /**
     * Parse the Groq API response and extract InvoiceData.
     */
    private InvoiceData parseGroqResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");

            if (choices.isEmpty()) {
                log.warn("No choices in Groq response");
                return createDefaultInvoiceData();
            }

            String content = choices.get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // Parse the JSON content from LLM
            return parseInvoiceJson(content);

        } catch (Exception ex) {
            log.error("Error parsing Groq response", ex);
            return createDefaultInvoiceData();
        }
    }

    /**
     * Parse the JSON string returned by the LLM into InvoiceData.
     */
    private InvoiceData parseInvoiceJson(String jsonContent) {
        try {
            // Clean up the response (remove markdown code blocks if present)
            String cleanedJson = jsonContent
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode invoiceJson = objectMapper.readTree(cleanedJson);

            // Extract fields - keep NULL if not found (don't use defaults)
            String invoiceNumber = getStringOrNull(invoiceJson.path("invoice_number"));
            String clientName = getStringOrNull(invoiceJson.path("client_name"));
            String clientAddress = getStringOrNull(invoiceJson.path("client_address"));
            String currency = getStringOrNull(invoiceJson.path("currency"));

            // Parse amount - keep NULL if not found or invalid
            BigDecimal amount = null;
            JsonNode amountNode = invoiceJson.path("amount");
            if (!amountNode.isMissingNode() && !amountNode.isNull()) {
                try {
                    String amountStr = amountNode.asText();
                    // Clean amount string (remove commas, currency symbols, spaces)
                    String cleanedAmount = amountStr.replaceAll("[^0-9.]", "");
                    if (!cleanedAmount.isEmpty()) {
                        amount = new BigDecimal(cleanedAmount);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to parse amount from LLM response: {}", amountNode.asText());
                }
            }

            // High confidence since LLM understands context better than regex
            double confidence = 0.85;

            return InvoiceData.create(
                    invoiceNumber,
                    amount,
                    clientName,
                    clientAddress,
                    currency,
                    confidence
            );

        } catch (Exception ex) {
            log.error("Error parsing invoice JSON from LLM", ex);
            return createDefaultInvoiceData();
        }
    }

    /**
     * Create default invoice data when LLM extraction fails completely.
     * Returns empty Optionals to indicate extraction failure.
     */
    private InvoiceData createDefaultInvoiceData() {
        return new InvoiceData(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "USD",
                0.0
        );
    }

    /**
     * Helper method to safely extract string from JsonNode, returning null if not found or empty.
     */
    private String getStringOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        // Return null if empty, "null" string, or whitespace only
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) {
            return null;
        }
        return value.trim();
    }
}
