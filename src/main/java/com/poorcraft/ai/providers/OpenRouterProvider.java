package com.poorcraft.ai.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.poorcraft.ai.AIProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenRouter provider implementation using their OpenAI-compatible API.
 */
public class OpenRouterProvider implements AIProvider {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";

    private final HttpClient httpClient;

    public OpenRouterProvider() {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();
    }

    @Override
    public String sendRequest(String prompt, AIConfig config) throws IOException {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IOException("OpenRouter provider requires an API key");
        }

        String baseUrl = config.baseUrl() != null && !config.baseUrl().isEmpty()
            ? config.baseUrl()
            : DEFAULT_BASE_URL;

        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.model());
        JsonArray messages = new JsonArray();

        String systemPrompt = null;
        String userMessageContent = null;
        if (config.headers() != null) {
            systemPrompt = config.headers().get("__system_prompt");
            userMessageContent = config.headers().get("__user_message");
        }

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemPrompt);
            messages.add(systemMessage);
        }

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        String content = userMessageContent != null && !userMessageContent.isBlank() ? userMessageContent : prompt;
        userMessage.addProperty("content", content != null ? content : "");
        messages.add(userMessage);

        if (messages.size() == 0) {
            JsonObject fallback = new JsonObject();
            fallback.addProperty("role", "user");
            fallback.addProperty("content", prompt != null ? prompt : "");
            messages.add(fallback);
        }
        payload.add("messages", messages);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/chat/completions"))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + config.apiKey())
            .header("HTTP-Referer", "https://poorcraft.game")
            .header("X-Title", "PoorCraft")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()));

        if (config.headers() != null) {
            config.headers().forEach(requestBuilder::header);
        }

        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IOException("OpenRouter responded with status code " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("OpenRouter request interrupted", e);
        }
    }

    @Override
    public String getName() {
        return "openrouter";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isAvailable(AIConfig config) {
        return config != null && config.apiKey() != null && !config.apiKey().isBlank();
    }

    @Override
    public AIResponse parseResponse(String jsonResponse) throws JsonSyntaxException {
        JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            return new AIResponse("", null, new HashMap<>());
        }
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");

        String content = message != null && message.has("content") ? message.get("content").getAsString() : "";
        String reasoning = null;
        if (message != null && message.has("reasoning")) {
            reasoning = message.get("reasoning").getAsString();
        }

        Map<String, Object> metadata = new HashMap<>();
        if (json.has("usage")) {
            metadata.put("usage", json.get("usage").toString());
        }
        if (firstChoice.has("finish_reason")) {
            metadata.put("finish_reason", firstChoice.get("finish_reason").getAsString());
        }

        return new AIResponse(content, reasoning, metadata);
    }
}
