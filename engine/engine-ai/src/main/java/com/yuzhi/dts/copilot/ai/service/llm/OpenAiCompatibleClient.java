package com.yuzhi.dts.copilot.ai.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for any OpenAI-compatible LLM endpoint (Ollama, OpenAI, DeepSeek, etc.)
 */
public class OpenAiCompatibleClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final int timeoutSeconds;

    public OpenAiCompatibleClient(String baseUrl, String apiKey, int timeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Synchronous chat completion.
     */
    public JsonNode chatCompletion(String model, List<Map<String, Object>> messages,
                                   Double temperature, Integer maxTokens,
                                   List<Map<String, Object>> tools) throws IOException, InterruptedException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.set("messages", mapper.valueToTree(messages));
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", mapper.valueToTree(tools));
        }
        body.put("stream", false);

        HttpRequest request = buildRequest("/v1/chat/completions", body.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("LLM API error: " + response.statusCode() + " - " + response.body());
        }
        return mapper.readTree(response.body());
    }

    /**
     * Streaming chat completion via SSE.
     */
    public void chatCompletionStream(String model, List<Map<String, Object>> messages,
                                     Double temperature, Integer maxTokens,
                                     OutputStream output) throws IOException, InterruptedException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.set("messages", mapper.valueToTree(messages));
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }
        body.put("stream", true);

        HttpRequest request = buildRequest("/v1/chat/completions", body.toString());
        HttpResponse<java.io.InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("LLM API error: " + response.statusCode() + " - " + errorBody);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        output.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                        output.flush();
                        break;
                    }
                    output.write(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
                    output.flush();
                }
            }
        }
    }

    /**
     * List available models from the provider.
     */
    public JsonNode listModels() throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/models"))
                .timeout(Duration.ofSeconds(10))
                .GET();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body());
    }

    /**
     * Check if the provider endpoint is reachable.
     */
    public boolean isAvailable() {
        try {
            listModels();
            return true;
        } catch (Exception e) {
            log.debug("Provider at {} is not available: {}", baseUrl, e.getMessage());
            return false;
        }
    }

    private HttpRequest buildRequest(String path, String jsonBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder.build();
    }
}
