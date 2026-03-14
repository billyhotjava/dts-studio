package com.yuzhi.dts.copilot.ai.service.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Embedding service that calls an OpenAI-compatible /v1/embeddings endpoint.
 * Defaults to a local Ollama instance with the bge-m3 model.
 */
@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int BATCH_SIZE = 32;
    private static final int DIMENSIONS = 1024;

    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final HttpClient httpClient;

    public OpenAiEmbeddingService(
            @Value("${dts.ai.embedding.base-url:http://localhost:11434}") String baseUrl,
            @Value("${dts.ai.embedding.model:bge-m3}") String model,
            @Value("${dts.ai.embedding.api-key:}") String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public float[] embed(String text) {
        try {
            List<float[]> results = callEmbeddingApi(List.of(text));
            return results != null && !results.isEmpty() ? results.get(0) : null;
        } catch (Exception e) {
            log.warn("Embedding call failed for single text: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        try {
            List<float[]> allResults = new ArrayList<>();
            for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
                List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
                List<float[]> batchResults = callEmbeddingApi(batch);
                if (batchResults == null) {
                    return null;
                }
                allResults.addAll(batchResults);
            }
            return allResults;
        } catch (Exception e) {
            log.warn("Embedding batch call failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            float[] test = embed("ping");
            return test != null && test.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private List<float[]> callEmbeddingApi(List<String> texts) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        if (texts.size() == 1) {
            body.put("input", texts.get(0));
        } else {
            ArrayNode inputArray = body.putArray("input");
            texts.forEach(inputArray::add);
        }
        body.put("dimensions", DIMENSIONS);

        String jsonBody = mapper.writeValueAsString(body);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/embeddings"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Embedding API error: {} - {}", response.statusCode(), response.body());
            return null;
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode dataArray = root.get("data");
        if (dataArray == null || !dataArray.isArray()) {
            log.error("Invalid embedding response: missing 'data' array");
            return null;
        }

        List<float[]> results = new ArrayList<>();
        for (JsonNode item : dataArray) {
            JsonNode embeddingNode = item.get("embedding");
            if (embeddingNode == null || !embeddingNode.isArray()) {
                log.error("Invalid embedding response: missing 'embedding' array in data item");
                return null;
            }
            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            results.add(vector);
        }
        return results;
    }
}
