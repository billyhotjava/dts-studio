package com.yuzhi.dts.copilot.analytics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CopilotAgentChatClient {

    private final RestClient restClient;
    private final String adminSecret;

    public CopilotAgentChatClient(
            @Value("${dts.copilot.ai.base-url:http://localhost:8091}") String baseUrl,
            @Value("${copilot.admin-secret:}") String adminSecret) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.adminSecret = adminSecret;
    }

    public Map<String, Object> sendMessage(String userId, String sessionId, String message, Long datasourceId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("message", message);
        if (sessionId != null && !sessionId.isBlank()) {
            payload.put("sessionId", sessionId);
        }
        if (datasourceId != null) {
            payload.put("datasourceId", datasourceId);
        }
        return restClient.post()
                .uri("/internal/agent/chat/send")
                .header("X-Admin-Secret", adminSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<Map<String, Object>> listSessions(String userId, int limit) {
        List<Map<String, Object>> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/agent/chat/sessions")
                        .queryParam("userId", userId)
                        .queryParam("limit", limit)
                        .build())
                .header("X-Admin-Secret", adminSecret)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return body != null ? body : Collections.emptyList();
    }

    public Map<String, Object> getSession(String userId, String sessionId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/agent/chat/{sessionId}")
                        .queryParam("userId", userId)
                        .build(sessionId))
                .header("X-Admin-Secret", adminSecret)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public void deleteSession(String userId, String sessionId) {
        restClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/agent/chat/{sessionId}")
                        .queryParam("userId", userId)
                        .build(sessionId))
                .header("X-Admin-Secret", adminSecret)
                .retrieve()
                .toBodilessEntity();
    }
}
