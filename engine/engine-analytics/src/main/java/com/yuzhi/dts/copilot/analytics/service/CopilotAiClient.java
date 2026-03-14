package com.yuzhi.dts.copilot.analytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client for communicating with copilot-ai REST APIs.
 * Handles auth verification and data source management via copilot-ai.
 */
@Service
public class CopilotAiClient {

    private static final Logger log = LoggerFactory.getLogger(CopilotAiClient.class);

    private final RestClient restClient;

    public CopilotAiClient(@Value("${dts.copilot.ai.base-url:http://localhost:8091}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Verify an API key by calling copilot-ai's /internal/auth/verify endpoint.
     *
     * @param rawKey the raw API key to verify
     * @return a map with verification result containing "valid" boolean and optional "user" map
     */
    public Optional<Map<String, Object>> verifyApiKey(String rawKey) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/internal/auth/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("apiKey", rawKey))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && Boolean.TRUE.equals(response.get("valid"))) {
                return Optional.of(response);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to verify API key via copilot-ai: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get all data sources from copilot-ai.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDataSources(String apiKey) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/ai/copilot/datasources")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.containsKey("data")) {
                return (List<Map<String, Object>>) response.get("data");
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to get data sources from copilot-ai: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get a specific data source from copilot-ai.
     */
    public Optional<Map<String, Object>> getDataSource(Long id, String apiKey) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/ai/copilot/datasources/{id}", id)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return Optional.ofNullable(data);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get data source {} from copilot-ai: {}", id, e.getMessage());
            return Optional.empty();
        }
    }
}
