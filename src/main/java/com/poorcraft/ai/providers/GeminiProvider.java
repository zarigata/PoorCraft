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
 * Gemini AI provider integration.
 */
public class GeminiProvider implements AIProvider {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final HttpClient httpClient;

    public GeminiProvider() {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();
    }

    @Override
    public String sendRequest(String prompt, AIConfig config) throws IOException {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IOException("Gemini provider requires an API key");
        }

        String baseUrl = config.baseUrl() != null && !config.baseUrl().isEmpty()
            ? config.baseUrl()
            : DEFAULT_BASE_URL;

        String url = String.format("%s/models/%s:generateContent?key=%s",
            baseUrl,
            config.model(),
            config.apiKey());

        JsonObject message = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject text = new JsonObject();
        text.addProperty("text", prompt);
        parts.add(text);
        content.add("parts", parts);
        contents.add(content);
        message.add("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(message.toString()))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new IOException("Gemini rate limit hit (HTTP 429).");
            }
            if (response.statusCode() >= 400) {
                throw new IOException("Gemini responded with status code " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Gemini request interrupted", e);
        }
    }

    @Override
    public String getName() {
        return "gemini";
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

        String content = extractContent(json);
        Map<String, Object> metadata = new HashMap<>();
        if (json.has("usageMetadata")) {
            metadata.put("usageMetadata", json.get("usageMetadata").toString());
        }

        return new AIResponse(content, null, metadata);
    }

    private String extractContent(JsonObject json) {
        if (!json.has("candidates")) {
            return "";
        }
        JsonArray candidates = json.getAsJsonArray("candidates");
        if (candidates.size() == 0) {
            return "";
        }
        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
        if (!firstCandidate.has("content")) {
            return "";
        }
        JsonObject content = firstCandidate.getAsJsonObject("content");
        if (!content.has("parts")) {
            return "";
        }
        JsonArray parts = content.getAsJsonArray("parts");
        if (parts.size() == 0) {
            return "";
        }
        JsonObject firstPart = parts.get(0).getAsJsonObject();
        if (!firstPart.has("text")) {
            return "";
        }
        return firstPart.get("text").getAsString();
    }
}
