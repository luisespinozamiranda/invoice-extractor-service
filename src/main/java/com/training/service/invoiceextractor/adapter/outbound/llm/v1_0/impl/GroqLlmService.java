package com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.ILlmExtractionService;
import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.InvoiceData;
import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.dto.GroqChatRequest;
import com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.dto.GroqChatResponse;
import com.training.service.invoiceextractor.configuration.GroqProperties;
import com.training.service.invoiceextractor.utils.error.ErrorCodes;
import com.training.service.invoiceextractor.utils.error.InvoiceExtractorServiceException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.util.Strings;

/**
 * Groq LLM service implementation for invoice data extraction.
 * Uses Groq's free API with Llama 3.1 model for intelligent field extraction.
 */
@Service
@Slf4j
public class GroqLlmService implements ILlmExtractionService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String USER_ROLE = "user";
    private static final String JSON_OBJECT_TYPE = "json_object";
    private static final double DEFAULT_CONFIDENCE = 0.85;

    private static final String EXTRACTION_PROMPT_TEMPLATE = """
            You are an expert at extracting structured data from invoices.

            Extract the following fields from the invoice text below:
            - invoice_number: The invoice or document number
            - amount: the exact text containing the total amount, exactly as it appears
            - vendor_name: The vendor name
            - vendor_address: The vendor address (if available)
            - currency: (ISO code if possible)
            - confidence: A decimal number between 0.0 and 1.0 representing your confidence in the extraction quality

            Return ONLY a valid JSON object with these exact field names. No explanations.
            If a field is not found, use null for strings or 0 for amount.

            Confidence score guidelines:
            - 0.9-1.0: All fields clearly visible and extracted with high certainty
            - 0.7-0.89: Most fields found, some minor ambiguity
            - 0.5-0.69: Several fields missing or unclear
            - 0.0-0.49: Poor quality text, most fields unclear or missing

            Rules:
            - DO NOT normalize numbers
            - DO NOT change separators
            - DO NOT calculate
            - Copy values EXACTLY as seen in the text
            - If multiple totals exist, choose the FINAL payable amount

            Invoice text:
            %s

            JSON output:
            """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final GroqProperties properties;

    public GroqLlmService(
            GroqProperties properties,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(GROQ_API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    @Override
    public CompletableFuture<InvoiceData> extractInvoiceData(String ocrText) {
        validateServiceAvailability();

        return CompletableFuture.supplyAsync(() -> {
            try {
                GroqChatRequest request = buildChatRequest(ocrText);
                GroqChatResponse response = callGroqApi(request);
                String content = extractContentFromResponse(response);
                return parseInvoiceJson(content);

            } catch (InvoiceExtractorServiceException ex) {
                throw ex;
            } catch (WebClientResponseException ex) {
                throw buildApiException(ex);
            } catch (Exception ex) {
                log.error("Unexpected error calling Groq LLM API", ex);
                throw new InvoiceExtractorServiceException(
                        ErrorCodes.LLM_API_ERROR,
                        "Unexpected error calling LLM API: " + ex.getMessage(),
                        ex
                );
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return Strings.isNotBlank(properties.getApiKey()) && Strings.isNotBlank(properties.getModel());
    }

    @Override
    public String getProviderName() {
        return "Groq (" + properties.getModel() + ")";
    }

    /**
     * Validates that the LLM service is properly configured and available.
     * @throws InvoiceExtractorServiceException if service is not available
     */
    private void validateServiceAvailability() {
        if (!isAvailable()) {
            throw new InvoiceExtractorServiceException(ErrorCodes.LLM_SERVICE_UNAVAILABLE)
                    .addDetail("provider", "Groq")
                    .addDetail("model", properties.getModel());
        }
    }

    /**
     * Builds a chat completion request for Groq API.
     */
    private GroqChatRequest buildChatRequest(String ocrText) {
        String prompt = String.format(EXTRACTION_PROMPT_TEMPLATE, ocrText);

        return GroqChatRequest.builder()
                .model(properties.getModel())
                .messages(List.of(
                        GroqChatRequest.Message.builder()
                                .role(USER_ROLE)
                                .content(prompt)
                                .build()
                ))
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .responseFormat(Map.of("type", JSON_OBJECT_TYPE))
                .build();
    }

    /**
     * Calls Groq API with the given request and returns the response.
     */
    private GroqChatResponse callGroqApi(GroqChatRequest request) {
        try {
            return webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(
                                            new InvoiceExtractorServiceException(
                                                    ErrorCodes.LLM_API_ERROR,
                                                    "Groq API returned error: " + response.statusCode()
                                            ).addDetail("statusCode", response.statusCode().value())
                                             .addDetail("errorBody", errorBody)
                                    ))
                    )
                    .bodyToMono(GroqChatResponse.class)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .onErrorMap(TimeoutException.class, e ->
                            new InvoiceExtractorServiceException(ErrorCodes.LLM_TIMEOUT, e.getMessage(), e)
                    )
                    .block();

        } catch (Exception ex) {
            log.error("Error during Groq API call", ex);
            throw ex;
        }
    }

    /**
     * Extracts the content string from a Groq chat response.
     */
    private String extractContentFromResponse(GroqChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new InvoiceExtractorServiceException(ErrorCodes.LLM_INVALID_RESPONSE)
                    .addDetail("reason", "No choices in response");
        }

        GroqChatResponse.Choice choice = response.getChoices().get(0);
        if (choice.getMessage() == null || Strings.isBlank(choice.getMessage().getContent())) {
            throw new InvoiceExtractorServiceException(ErrorCodes.LLM_INVALID_RESPONSE)
                    .addDetail("reason", "Empty content in message");
        }

        return choice.getMessage().getContent();
    }

    /**
     * Parses the JSON string returned by the LLM into InvoiceData.
     */
    private InvoiceData parseInvoiceJson(String jsonContent) {
        try {
            String cleanedJson = cleanJsonResponse(jsonContent);
            JsonNode invoiceJson = objectMapper.readTree(cleanedJson);

            String invoiceNumber = extractStringField(invoiceJson, "invoice_number");
            String vendorName = extractStringField(invoiceJson, "vendor_name");
            String vendorAddress = extractStringField(invoiceJson, "vendor_address");
            String currency = extractStringField(invoiceJson, "currency");
            BigDecimal amount = extractAmountField(invoiceJson, "amount");
            double confidence = extractConfidenceField(invoiceJson, "confidence");

            return InvoiceData.create(
                    invoiceNumber,
                    amount,
                    vendorName,
                    vendorAddress,
                    currency,
                    confidence
            );

        } catch (JsonProcessingException ex) {
            throw new InvoiceExtractorServiceException(
                    ErrorCodes.LLM_INVALID_RESPONSE,
                    "Failed to parse invoice data from LLM JSON: " + ex.getMessage(),
                    ex
            ).addDetail("jsonContent", jsonContent);
        }
    }

    /**
     * Cleans JSON response by removing markdown code blocks.
     */
    private String cleanJsonResponse(String jsonContent) {
        return jsonContent
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    /**
     * Extracts a string field from JSON, returning null if not found or empty.
     */
    private String extractStringField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }

        String value = fieldNode.asText();
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) {
            return null;
        }

        return value.trim();
    }

    /**
     * Extracts an amount field from JSON, returning null if not found or invalid.
     */
    private BigDecimal extractAmountField(JsonNode node, String fieldName) {
        JsonNode amountNode = node.path(fieldName);
        if (amountNode.isMissingNode() || amountNode.isNull()) {
            return null;
        }

        try {
            String amountStr = amountNode.asText();
            String cleanedAmount = amountStr.replaceAll("[^0-9.]", "");
            return cleanedAmount.isEmpty() ? null : new BigDecimal(cleanedAmount);
        } catch (Exception ex) {
            log.warn("Failed to parse amount: {}", amountNode.asText());
            return null;
        }
    }

    /**
     * Extracts a confidence field from JSON, returning DEFAULT_CONFIDENCE if not found or invalid.
     * Validates that the confidence value is between 0.0 and 1.0.
     */
    private double extractConfidenceField(JsonNode node, String fieldName) {
        JsonNode confidenceNode = node.path(fieldName);
        if (confidenceNode.isMissingNode() || confidenceNode.isNull()) {
            log.debug("Confidence field not found in LLM response, using default: {}", DEFAULT_CONFIDENCE);
            return DEFAULT_CONFIDENCE;
        }

        try {
            double confidence = confidenceNode.asDouble();

            // Validate confidence is within valid range [0.0, 1.0]
            if (confidence < 0.0 || confidence > 1.0) {
                log.warn("Invalid confidence value {} outside range [0.0, 1.0], using default: {}",
                        confidence, DEFAULT_CONFIDENCE);
                return DEFAULT_CONFIDENCE;
            }

            return confidence;
        } catch (Exception ex) {
            log.warn("Failed to parse confidence: {}, using default: {}",
                    confidenceNode.asText(), DEFAULT_CONFIDENCE);
            return DEFAULT_CONFIDENCE;
        }
    }

    /**
     * Builds an InvoiceExtractorServiceException from a WebClientResponseException.
     */
    private InvoiceExtractorServiceException buildApiException(WebClientResponseException ex) {
        return new InvoiceExtractorServiceException(
                ErrorCodes.LLM_API_ERROR,
                "HTTP error calling LLM API: " + ex.getStatusCode(),
                ex
        ).addDetail("statusCode", ex.getStatusCode().value())
         .addDetail("responseBody", ex.getResponseBodyAsString());
    }
}
