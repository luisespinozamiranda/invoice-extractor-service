package com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for Groq Chat Completions API.
 * Follows OpenAI-compatible format.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroqChatRequest {

    /**
     * Model identifier (e.g., llama-3.1-70b-versatile).
     */
    private String model;

    /**
     * List of messages in the conversation.
     */
    private List<Message> messages;

    /**
     * Sampling temperature (0.0 to 1.0).
     */
    private Double temperature;

    /**
     * Maximum number of tokens to generate.
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * Response format specification.
     */
    @JsonProperty("response_format")
    private Map<String, String> responseFormat;

    /**
     * Message in a chat conversation.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * Role of the message sender (system, user, assistant).
         */
        private String role;

        /**
         * Content of the message.
         */
        private String content;
    }
}
