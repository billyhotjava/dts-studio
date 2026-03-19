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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

        HttpRequest request = buildRequest("chat/completions", body.toString());
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

        HttpRequest request = buildRequest("chat/completions", body.toString());
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

    public StreamingChatResult chatCompletionStream(String model, List<Map<String, Object>> messages,
                                                    Double temperature, Integer maxTokens,
                                                    List<Map<String, Object>> tools,
                                                    StreamEventHandler handler) throws IOException, InterruptedException {
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
        body.put("stream", true);

        HttpRequest request = buildRequest("chat/completions", body.toString());
        HttpResponse<java.io.InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("LLM API error: " + response.statusCode() + " - " + errorBody);
        }

        StreamAccumulator accumulator = new StreamAccumulator();
        StreamEventHandler safeHandler = handler != null ? handler : StreamEventHandler.noop();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) {
                    continue;
                }
                String data = line.substring(6).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }
                accumulator.consume(mapper.readTree(data), safeHandler);
            }
        }

        return accumulator.toResult();
    }

    /**
     * List available models from the provider.
     */
    public JsonNode listModels() throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(resolveApiUrl("models")))
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
                .uri(URI.create(resolveApiUrl(path)))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder.build();
    }

    private String resolveApiUrl(String relativePath) {
        String normalizedPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        if (hasVersionedApiRoot()) {
            return baseUrl + "/" + normalizedPath;
        }
        return baseUrl + "/v1/" + normalizedPath;
    }

    private boolean hasVersionedApiRoot() {
        String path = URI.create(baseUrl).getPath();
        if (path == null || path.isBlank()) {
            return false;
        }
        return Arrays.stream(path.split("/"))
                .anyMatch(segment -> segment.matches("v\\d+([A-Za-z0-9._-]*)?"));
    }

    public interface StreamEventHandler {
        default void onReasoningDelta(String delta) {}
        default void onContentDelta(String delta) {}

        static StreamEventHandler noop() {
            return new StreamEventHandler() {};
        }
    }

    public record StreamingChatResult(
            String content,
            String reasoningContent,
            List<Map<String, Object>> toolCalls,
            String finishReason
    ) {}

    private static final class StreamAccumulator {
        private final StringBuilder content = new StringBuilder();
        private final StringBuilder reasoning = new StringBuilder();
        private final Map<Integer, ToolCallAccumulator> toolCalls = new TreeMap<>();
        private String finishReason;

        void consume(JsonNode chunk, StreamEventHandler handler) {
            JsonNode choices = chunk.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return;
            }

            JsonNode choice = choices.get(0);
            if (choice.hasNonNull("finish_reason")) {
                finishReason = choice.get("finish_reason").asText();
            }

            JsonNode delta = choice.path("delta");
            String reasoningDelta = readTextDelta(delta, "reasoning_content", "reasoning");
            if (!reasoningDelta.isEmpty()) {
                reasoning.append(reasoningDelta);
                handler.onReasoningDelta(reasoningDelta);
            }

            String contentDelta = readTextDelta(delta, "content");
            if (!contentDelta.isEmpty()) {
                content.append(contentDelta);
                handler.onContentDelta(contentDelta);
            }

            JsonNode toolCallDeltas = delta.path("tool_calls");
            if (toolCallDeltas.isArray()) {
                for (JsonNode toolCallDelta : toolCallDeltas) {
                    int index = toolCallDelta.path("index").asInt(toolCalls.size());
                    ToolCallAccumulator accumulator = toolCalls.computeIfAbsent(index, ignored -> new ToolCallAccumulator());
                    accumulator.consume(toolCallDelta);
                }
            }
        }

        StreamingChatResult toResult() {
            List<Map<String, Object>> aggregatedToolCalls = new ArrayList<>();
            for (ToolCallAccumulator value : toolCalls.values()) {
                aggregatedToolCalls.add(value.toMap());
            }
            return new StreamingChatResult(
                    content.toString(),
                    reasoning.toString(),
                    aggregatedToolCalls,
                    finishReason
            );
        }

        private static String readTextDelta(JsonNode delta, String... fieldNames) {
            for (String fieldName : fieldNames) {
                JsonNode field = delta.path(fieldName);
                if (field.isTextual()) {
                    return field.asText();
                }
            }
            return "";
        }
    }

    private static final class ToolCallAccumulator {
        private String id;
        private String type = "function";
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();

        void consume(JsonNode toolCallDelta) {
            if (toolCallDelta.hasNonNull("id")) {
                id = toolCallDelta.get("id").asText();
            }
            if (toolCallDelta.hasNonNull("type")) {
                type = toolCallDelta.get("type").asText();
            }
            JsonNode function = toolCallDelta.path("function");
            if (function.hasNonNull("name")) {
                name.append(function.get("name").asText());
            }
            if (function.hasNonNull("arguments")) {
                arguments.append(function.get("arguments").asText());
            }
        }

        Map<String, Object> toMap() {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", name.toString());
            function.put("arguments", arguments.toString());

            Map<String, Object> toolCall = new LinkedHashMap<>();
            toolCall.put("id", id);
            toolCall.put("type", type);
            toolCall.put("function", function);
            return toolCall;
        }
    }
}
