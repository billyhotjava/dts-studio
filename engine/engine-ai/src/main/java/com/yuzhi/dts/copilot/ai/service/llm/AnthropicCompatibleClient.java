package com.yuzhi.dts.copilot.ai.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnthropicCompatibleClient implements LlmProviderClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicCompatibleClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final int timeoutSeconds;

    public AnthropicCompatibleClient(String baseUrl, String apiKey, int timeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public JsonNode chatCompletion(String model, List<Map<String, Object>> messages,
                                   Double temperature, Integer maxTokens,
                                   List<Map<String, Object>> tools) throws IOException, InterruptedException {
        ObjectNode body = buildMessagesBody(model, messages, temperature, maxTokens, tools, false);
        HttpRequest request = buildRequest("messages", body.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("LLM API error: " + response.statusCode() + " - " + response.body());
        }
        JsonNode anthropic = mapper.readTree(response.body());
        return normalizeMessageResponse(anthropic);
    }

    @Override
    public OpenAiCompatibleClient.StreamingChatResult chatCompletionStream(String model, List<Map<String, Object>> messages,
                                                                           Double temperature, Integer maxTokens,
                                                                           List<Map<String, Object>> tools,
                                                                           OpenAiCompatibleClient.StreamEventHandler handler)
            throws IOException, InterruptedException {
        ObjectNode body = buildMessagesBody(model, messages, temperature, maxTokens, tools, true);
        HttpRequest request = buildRequest("messages", body.toString());
        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("LLM API error: " + response.statusCode() + " - " + errorBody);
        }

        AnthropicStreamAccumulator accumulator = new AnthropicStreamAccumulator();
        OpenAiCompatibleClient.StreamEventHandler safeHandler =
                handler != null ? handler : OpenAiCompatibleClient.StreamEventHandler.noop();

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
                accumulator.consume(mapper.readTree(data), safeHandler);
                if (accumulator.isStopped()) {
                    break;
                }
            }
        }

        return accumulator.toResult();
    }

    @Override
    public JsonNode listModels() throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(resolveApiUrl("models")))
                .timeout(Duration.ofSeconds(10))
                .GET();
        addAuthHeaders(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("LLM API error: " + response.statusCode() + " - " + response.body());
        }
        return mapper.readTree(response.body());
    }

    @Override
    public boolean isAvailable() {
        try {
            listModels();
            return true;
        } catch (Exception e) {
            log.debug("Provider at {} is not available: {}", baseUrl, e.getMessage());
            return false;
        }
    }

    private ObjectNode buildMessagesBody(String model, List<Map<String, Object>> messages,
                                         Double temperature, Integer maxTokens,
                                         List<Map<String, Object>> tools, boolean stream) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens != null ? maxTokens : 4096);
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        if (stream) {
            body.put("stream", true);
        }

        String systemPrompt = extractSystemPrompt(messages);
        if (!systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }
        body.set("messages", mapper.valueToTree(toAnthropicMessages(messages)));
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", mapper.valueToTree(toAnthropicTools(tools)));
        }
        return body;
    }

    private HttpRequest buildRequest(String path, String jsonBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(resolveApiUrl(path)))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        addAuthHeaders(builder);
        return builder.build();
    }

    private void addAuthHeaders(HttpRequest.Builder builder) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("x-api-key", apiKey);
        }
        builder.header("anthropic-version", ANTHROPIC_VERSION);
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

    private String extractSystemPrompt(List<Map<String, Object>> messages) {
        StringBuilder builder = new StringBuilder();
        if (messages == null) {
            return "";
        }
        for (Map<String, Object> message : messages) {
            if (!"system".equals(String.valueOf(message.get("role")))) {
                continue;
            }
            String content = plainTextContent(message.get("content"));
            if (!content.isBlank()) {
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append(content);
            }
        }
        return builder.toString();
    }

    private List<Map<String, Object>> toAnthropicMessages(List<Map<String, Object>> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (messages == null) {
            return result;
        }
        for (Map<String, Object> message : messages) {
            String role = String.valueOf(message.get("role"));
            if ("system".equals(role)) {
                continue;
            }
            if ("tool".equals(role)) {
                Map<String, Object> anthropicMessage = new LinkedHashMap<>();
                anthropicMessage.put("role", "user");
                anthropicMessage.put("content", List.of(Map.of(
                        "type", "tool_result",
                        "tool_use_id", stringValue(message.get("tool_call_id")),
                        "content", plainTextContent(message.get("content"))
                )));
                result.add(anthropicMessage);
                continue;
            }

            Map<String, Object> anthropicMessage = new LinkedHashMap<>();
            anthropicMessage.put("role", role);
            anthropicMessage.put("content", buildAnthropicContent(message));
            result.add(anthropicMessage);
        }
        return result;
    }

    private List<Map<String, Object>> buildAnthropicContent(Map<String, Object> message) {
        List<Map<String, Object>> content = new ArrayList<>();
        String reasoning = stringValue(message.get("reasoning_content"));
        if (!reasoning.isBlank()) {
            content.add(Map.of("type", "thinking", "thinking", reasoning));
        }

        String text = plainTextContent(message.get("content"));
        if (!text.isBlank()) {
            content.add(Map.of("type", "text", "text", text));
        }

        Object toolCallsValue = message.get("tool_calls");
        if (toolCallsValue instanceof List<?> rawCalls) {
            for (Object rawCall : rawCalls) {
                if (!(rawCall instanceof Map<?, ?> rawMap)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> call = (Map<String, Object>) rawMap;
                @SuppressWarnings("unchecked")
                Map<String, Object> function = call.get("function") instanceof Map<?, ?> rawFunction
                        ? (Map<String, Object>) rawFunction
                        : Map.of();
                content.add(Map.of(
                        "type", "tool_use",
                        "id", stringValue(call.get("id")),
                        "name", stringValue(function.get("name")),
                        "input", parseArgumentsJson(stringValue(function.get("arguments")))
                ));
            }
        }

        if (content.isEmpty()) {
            content.add(Map.of("type", "text", "text", ""));
        }
        return content;
    }

    private List<Map<String, Object>> toAnthropicTools(List<Map<String, Object>> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> tool : tools) {
            @SuppressWarnings("unchecked")
            Map<String, Object> function = tool.get("function") instanceof Map<?, ?> rawFunction
                    ? (Map<String, Object>) rawFunction
                    : Map.of();
            Map<String, Object> anthropicTool = new LinkedHashMap<>();
            anthropicTool.put("name", stringValue(function.get("name")));
            anthropicTool.put("description", stringValue(function.get("description")));
            anthropicTool.put("input_schema", function.getOrDefault("parameters", Map.of()));
            result.add(anthropicTool);
        }
        return result;
    }

    private JsonNode normalizeMessageResponse(JsonNode anthropic) {
        ObjectNode message = mapper.createObjectNode();
        StringBuilder content = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        ArrayNode toolCalls = mapper.createArrayNode();

        JsonNode blocks = anthropic.path("content");
        if (blocks.isArray()) {
            for (JsonNode block : blocks) {
                String type = block.path("type").asText("");
                if ("text".equals(type)) {
                    content.append(block.path("text").asText(""));
                } else if ("thinking".equals(type)) {
                    reasoning.append(block.path("thinking").asText(""));
                } else if ("tool_use".equals(type)) {
                    ObjectNode toolCall = mapper.createObjectNode();
                    toolCall.put("id", block.path("id").asText());
                    toolCall.put("type", "function");
                    ObjectNode function = toolCall.putObject("function");
                    function.put("name", block.path("name").asText());
                    function.put("arguments", block.path("input").toString());
                    toolCalls.add(toolCall);
                }
            }
        }

        message.put("content", content.toString());
        if (reasoning.length() > 0) {
            message.put("reasoning_content", reasoning.toString());
        }
        if (!toolCalls.isEmpty()) {
            message.set("tool_calls", toolCalls);
        }

        ObjectNode choice = mapper.createObjectNode();
        choice.set("message", message);
        choice.put("finish_reason", mapStopReason(anthropic.path("stop_reason").asText("")));

        ObjectNode normalized = mapper.createObjectNode();
        normalized.set("choices", mapper.createArrayNode().add(choice));
        return normalized;
    }

    private JsonNode parseArgumentsJson(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return mapper.createObjectNode();
        }
        try {
            return mapper.readTree(arguments);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    private String plainTextContent(Object content) {
        if (content == null) {
            return "";
        }
        if (content instanceof String text) {
            return text;
        }
        return String.valueOf(content);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String mapStopReason(String stopReason) {
        return switch (stopReason) {
            case "tool_use" -> "tool_calls";
            case "end_turn" -> "stop";
            default -> stopReason;
        };
    }

    private static final class AnthropicStreamAccumulator {
        private final StringBuilder content = new StringBuilder();
        private final StringBuilder reasoning = new StringBuilder();
        private final Map<Integer, ToolCallAccumulator> toolCalls = new TreeMap<>();
        private String finishReason;
        private boolean stopped;

        void consume(JsonNode event, OpenAiCompatibleClient.StreamEventHandler handler) {
            String type = event.path("type").asText("");
            switch (type) {
                case "content_block_start" -> handleBlockStart(event);
                case "content_block_delta" -> handleBlockDelta(event, handler);
                case "message_delta" -> finishReason = switch (event.path("delta").path("stop_reason").asText("")) {
                    case "tool_use" -> "tool_calls";
                    case "end_turn" -> "stop";
                    default -> event.path("delta").path("stop_reason").asText("");
                };
                case "message_stop" -> stopped = true;
                default -> {
                }
            }
        }

        boolean isStopped() {
            return stopped;
        }

        private void handleBlockStart(JsonNode event) {
            JsonNode contentBlock = event.path("content_block");
            if (!"tool_use".equals(contentBlock.path("type").asText(""))) {
                return;
            }
            int index = event.path("index").asInt();
            ToolCallAccumulator accumulator = toolCalls.computeIfAbsent(index, key -> new ToolCallAccumulator());
            accumulator.id = contentBlock.path("id").asText("");
            accumulator.name = contentBlock.path("name").asText("");
        }

        private void handleBlockDelta(JsonNode event, OpenAiCompatibleClient.StreamEventHandler handler) {
            JsonNode delta = event.path("delta");
            String deltaType = delta.path("type").asText("");
            if ("thinking_delta".equals(deltaType)) {
                String chunk = delta.path("thinking").asText("");
                reasoning.append(chunk);
                handler.onReasoningDelta(chunk);
                return;
            }
            if ("text_delta".equals(deltaType)) {
                String chunk = delta.path("text").asText("");
                content.append(chunk);
                handler.onContentDelta(chunk);
                return;
            }
            if ("input_json_delta".equals(deltaType)) {
                int index = event.path("index").asInt();
                ToolCallAccumulator accumulator = toolCalls.computeIfAbsent(index, key -> new ToolCallAccumulator());
                accumulator.arguments.append(delta.path("partial_json").asText(""));
            }
        }

        OpenAiCompatibleClient.StreamingChatResult toResult() {
            List<Map<String, Object>> normalizedToolCalls = new ArrayList<>();
            for (ToolCallAccumulator accumulator : toolCalls.values()) {
                Map<String, Object> toolCall = new LinkedHashMap<>();
                toolCall.put("id", accumulator.id);
                toolCall.put("type", "function");
                Map<String, Object> function = new LinkedHashMap<>();
                function.put("name", accumulator.name);
                function.put("arguments", accumulator.arguments.toString());
                toolCall.put("function", function);
                normalizedToolCalls.add(toolCall);
            }
            return new OpenAiCompatibleClient.StreamingChatResult(
                    content.toString(),
                    reasoning.toString(),
                    normalizedToolCalls,
                    finishReason
            );
        }
    }

    private static final class ToolCallAccumulator {
        private String id = "";
        private String name = "";
        private final StringBuilder arguments = new StringBuilder();
    }
}
