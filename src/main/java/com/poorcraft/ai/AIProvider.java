package com.poorcraft.ai;

import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.Map;

/**
 * Defines the contract for AI providers used by the AI companion system.
 */
public interface AIProvider {
    /**
     * Sends a prompt to the AI provider using the supplied configuration.
     *
     * @param prompt Prompt to send
     * @param config Provider-specific configuration
     * @return Raw JSON response as string
     * @throws IOException When the HTTP request fails
     */
    String sendRequest(String prompt, AIConfig config) throws IOException;

    /**
     * @return Human-readable provider name.
     */
    String getName();

    /**
     * Determines whether the provider is currently available.
     *
     * @return True if the provider can be used.
     */
    boolean isAvailable();

    /**
     * Determines whether the provider is available when supplied with the given configuration.
     * Providers that require API keys or other credentials should validate them here.
     *
     * @param config Provider configuration built from user settings
     * @return True if the provider can be used with the supplied configuration
     */
    default boolean isAvailable(AIConfig config) {
        return isAvailable();
    }

    /**
     * Parses the JSON response and extracts a structured AI response.
     *
     * @param jsonResponse Raw JSON response
     * @return Parsed AI response
     * @throws JsonSyntaxException When the payload is not valid JSON
     */
    AIResponse parseResponse(String jsonResponse) throws JsonSyntaxException;

    /**
     * Represents a normalized AI response across providers.
     */
    record AIResponse(String content, String reasoning, Map<String, Object> metadata) {
    }

    /**
     * Provider configuration containing connection and authentication details.
     */
    record AIConfig(String apiKey, String model, String baseUrl, Map<String, String> headers) {
    }
}
