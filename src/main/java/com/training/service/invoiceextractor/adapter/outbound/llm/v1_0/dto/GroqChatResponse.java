package com.training.service.invoiceextractor.adapter.outbound.llm.v1_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for Groq Chat Completions API.
 * Follows OpenAI-compatible format.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroqChatResponse {

    /**
     * Unique identifier for the completion.
     */
    private String id;

    /**
     * Object type (should be "chat.completion").
     */
    private String object;

    /**
     * Unix timestamp of creation.
     */
    private Long created;

    /**
     * Model used for completion.
     */
    private String model;

    /**
     * List of completion choices.
     */
    private List<Choice> choices;

    /**
     * Token usage information.
     */
    private Usage usage;

    /**
     * A completion choice from the API.
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        /**
         * Index of this choice.
         */
        private Integer index;

        /**
         * The generated message.
         */
        private Message message;

        /**
         * Reason the completion finished (stop, length, etc.).
         */
        private String finishReason;
    }

    /**
     * Message in a chat response.
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        /**
         * Role of the message (assistant).
         */
        private String role;

        /**
         * Content of the message.
         */
        private String content;
    }

    /**
     * Token usage statistics.
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        /**
         * Tokens in the prompt.
         */
        private Integer promptTokens;

        /**
         * Tokens in the completion.
         */
        private Integer completionTokens;

        /**
         * Total tokens used.
         */
        private Integer totalTokens;
    }
}
