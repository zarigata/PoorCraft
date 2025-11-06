package com.poorcraft.ai.providers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.poorcraft.ai.AIProvider;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Ollama-based AI provider that targets the local REST API.
 */
public class OllamaProvider implements AIProvider {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private long lastAvailabilityCheck;
    private boolean lastAvailabilityResult;

    public OllamaProvider() {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();
        this.lastAvailabilityCheck = 0L;
        this.lastAvailabilityResult = false;
    }

    @Override
    public String sendRequest(String prompt, AIConfig config) throws IOException {
        String baseUrl = config.baseUrl() != null && !config.baseUrl().isEmpty()
            ? config.baseUrl()
            : DEFAULT_BASE_URL;

        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.model());
        payload.addProperty("prompt", prompt);
        payload.addProperty("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/generate"))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IOException("Ollama responded with status code " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ollama request interrupted", e);
        }
    }

    @Override
    public String getName() {
        return "ollama";
    }

    @Override
    public boolean isAvailable() {
        long now = System.currentTimeMillis();
        if (now - lastAvailabilityCheck < 30_000L) {
            return lastAvailabilityResult;
        }

        lastAvailabilityCheck = now;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DEFAULT_BASE_URL + "/api/tags"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            lastAvailabilityResult = response.statusCode() < 500;
        } catch (ConnectException e) {
            lastAvailabilityResult = false;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            lastAvailabilityResult = false;
        }

        return lastAvailabilityResult;
    }

    @Override
    public AIResponse parseResponse(String jsonResponse) throws JsonSyntaxException {
        JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
        String content = json.has("response") ? json.get("response").getAsString() : "";

        Map<String, Object> metadata = new HashMap<>();
        if (json.has("done_reason")) {
            metadata.put("done_reason", json.get("done_reason").getAsString());
        }
        if (json.has("model")) {
            metadata.put("model", json.get("model").getAsString());
        }

        return new AIResponse(content, null, metadata);
    }
}
