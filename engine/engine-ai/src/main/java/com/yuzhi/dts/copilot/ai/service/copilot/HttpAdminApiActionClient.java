package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HttpAdminApiActionClient implements AdminApiActionClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String authorizationHeader;

    public HttpAdminApiActionClient(
            ObjectMapper objectMapper,
            @Value("${copilot.action.adminapi.base-url:}") String baseUrl,
            @Value("${copilot.action.adminapi.authorization:}") String authorizationHeader) {
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.authorizationHeader = normalizeAuthorizationHeader(authorizationHeader);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public AdminApiActionResponse postDraft(String endpoint, Map<String, Object> payload) {
        return post(endpoint, payload);
    }

    @Override
    public AdminApiActionResponse postCommit(String endpoint, Map<String, Object> payload) {
        return post(endpoint, payload);
    }

    private AdminApiActionResponse post(String endpoint, Map<String, Object> payload) {
        try {
            if (!StringUtils.hasText(baseUrl)) {
                return new AdminApiActionResponse(false,
                        "copilot.action.adminapi.base-url is required for adminapi action calls",
                        Map.of());
            }
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(baseUrl + normalizeEndpoint(endpoint)))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json");
            if (StringUtils.hasText(authorizationHeader)) {
                requestBuilder.header("Authorization", authorizationHeader);
            }
            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = parseBody(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new AdminApiActionResponse(false, response.body(), body);
            }
            return new AdminApiActionResponse(isBusinessSuccess(body), resolveMessage(body), body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AdminApiActionResponse(false, "adminapi action call interrupted", Map.of());
        } catch (IOException | RuntimeException e) {
            return new AdminApiActionResponse(false, e.getMessage(), Map.of());
        }
    }

    private Map<String, Object> parseBody(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(body, MAP_TYPE);
    }

    private boolean isBusinessSuccess(Map<String, Object> body) {
        Object code = body.get("code");
        if (code instanceof Number number) {
            return number.intValue() == 200;
        }
        if (code instanceof String text && !text.isBlank()) {
            return "200".equals(text);
        }
        return true;
    }

    private String resolveMessage(Map<String, Object> body) {
        Object message = body.get("msg");
        if (message == null) {
            message = body.get("message");
        }
        return message == null ? "ok" : message.toString();
    }

    private static String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("adminapi action endpoint is blank");
        }
        return endpoint.startsWith("/") ? endpoint : "/" + endpoint;
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeAuthorizationHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())
                ? trimmed
                : "Bearer " + trimmed;
    }
}
