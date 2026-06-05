package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceDetailReconciliationHttpPayloadProvider
        implements FinanceDetailReconciliationJsonSourceClient.PayloadProvider {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String oracleBaseUrl;
    private final String analyticsBaseUrl;
    private final String oracleAuthorizationHeader;
    private final String oracleCookieHeader;
    private final String analyticsAuthorizationHeader;
    private final String analyticsCookieHeader;
    private final int timeoutSeconds;

    @Autowired
    public FinanceDetailReconciliationHttpPayloadProvider(
            ObjectMapper objectMapper,
            @Value("${copilot.finance.reconciliation.oracle-base-url:}") String oracleBaseUrl,
            @Value("${copilot.finance.reconciliation.analytics-base-url:}") String analyticsBaseUrl,
            @Value("${copilot.finance.reconciliation.oracle-authorization:${copilot.finance.reconciliation.authorization:}}") String oracleAuthorizationHeader,
            @Value("${copilot.finance.reconciliation.oracle-cookie:${copilot.finance.reconciliation.cookie:}}") String oracleCookieHeader,
            @Value("${copilot.finance.reconciliation.analytics-authorization:${copilot.finance.reconciliation.authorization:}}") String analyticsAuthorizationHeader,
            @Value("${copilot.finance.reconciliation.analytics-cookie:${copilot.finance.reconciliation.cookie:}}") String analyticsCookieHeader) {
        this(
                objectMapper,
                oracleBaseUrl,
                analyticsBaseUrl,
                oracleAuthorizationHeader,
                oracleCookieHeader,
                analyticsAuthorizationHeader,
                analyticsCookieHeader,
                30);
    }

    FinanceDetailReconciliationHttpPayloadProvider(
            ObjectMapper objectMapper,
            String oracleBaseUrl,
            String analyticsBaseUrl,
            String authorizationHeader,
            String cookieHeader,
            int timeoutSeconds) {
        this(
                objectMapper,
                oracleBaseUrl,
                analyticsBaseUrl,
                authorizationHeader,
                cookieHeader,
                authorizationHeader,
                cookieHeader,
                timeoutSeconds);
    }

    FinanceDetailReconciliationHttpPayloadProvider(
            ObjectMapper objectMapper,
            String oracleBaseUrl,
            String analyticsBaseUrl,
            String oracleAuthorizationHeader,
            String oracleCookieHeader,
            String analyticsAuthorizationHeader,
            String analyticsCookieHeader,
            int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.oracleBaseUrl = trimTrailingSlash(oracleBaseUrl);
        this.analyticsBaseUrl = trimTrailingSlash(analyticsBaseUrl);
        this.oracleAuthorizationHeader = normalizeAuthorizationHeader(oracleAuthorizationHeader);
        this.oracleCookieHeader = trimToNull(oracleCookieHeader);
        this.analyticsAuthorizationHeader = normalizeAuthorizationHeader(analyticsAuthorizationHeader);
        this.analyticsCookieHeader = trimToNull(analyticsCookieHeader);
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .build();
    }

    @Override
    public String oraclePayload(FinanceDetailReconciliationSampleRegistry.DetailSample sample) {
        if (!StringUtils.hasText(oracleBaseUrl)) {
            throw new IllegalStateException("copilot.finance.reconciliation.oracle-base-url is required");
        }
        Endpoint endpoint = Endpoint.parse(sample.oracleEndpoint());
        URI uri = URI.create(oracleBaseUrl + endpoint.pathWithQuery(sample.oracleRequest()));
        HttpRequest.Builder builder = requestBuilder(uri, oracleAuthorizationHeader, oracleCookieHeader);
        if ("GET".equals(endpoint.method())) {
            return send(builder.GET().build());
        }
        if ("POST".equals(endpoint.method())) {
            return send(builder
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(writeJson(sample.oracleRequest())))
                    .build());
        }
        throw new IllegalArgumentException("Unsupported finance oracle endpoint method: " + endpoint.method());
    }

    @Override
    public String copilotPayload(FinanceDetailReconciliationSampleRegistry.DetailSample sample) {
        if (!StringUtils.hasText(analyticsBaseUrl)) {
            throw new IllegalStateException("copilot.finance.reconciliation.analytics-base-url is required");
        }
        URI uri = URI.create(analyticsBaseUrl + "/api/dataset");
        HttpRequest request = requestBuilder(uri, analyticsAuthorizationHeader, analyticsCookieHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(datasetRequest(sample.copilotRequest()))))
                .build();
        return send(request);
    }

    private HttpRequest.Builder requestBuilder(URI uri, String authorizationHeader, String cookieHeader) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Accept", "application/json");
        if (StringUtils.hasText(authorizationHeader)) {
            builder.header("Authorization", authorizationHeader);
        }
        if (StringUtils.hasText(cookieHeader)) {
            builder.header("Cookie", cookieHeader);
        }
        return builder;
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Finance detail reconciliation HTTP "
                        + response.statusCode() + ": " + response.body()
                        + routeFailureHint(request.uri()));
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Finance detail reconciliation HTTP call interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Finance detail reconciliation HTTP call failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode datasetRequest(Map<String, String> copilotRequest) {
        String nativeSql = text(copilotRequest, "nativeSql");
        if (!StringUtils.hasText(nativeSql)) {
            throw new IllegalArgumentException("copilotRequest.nativeSql is required for live dataset reconciliation");
        }
        ObjectNode body = objectMapper.createObjectNode();
        putDatabase(body, text(copilotRequest, "database"));
        body.put("type", "native");
        body.put("context", textOrDefault(copilotRequest, "context", "finance-detail-reconciliation"));
        ObjectNode nativeNode = objectMapper.createObjectNode();
        nativeNode.put("query", nativeSql);
        body.set("native", nativeNode);
        ObjectNode constraints = objectMapper.createObjectNode();
        constraints.put("max-results", parseInt(text(copilotRequest, "maxResults"), 2000));
        body.set("constraints", constraints);
        return body;
    }

    private static String routeFailureHint(URI uri) {
        if (uri == null || !uri.getPath().contains("/rs-flowers-base/")) {
            return "";
        }
        return "; oracle-route-hint=expected legacy adminapi rs-gateway/rs-flowers-base route. "
                + "Check copilot.finance.reconciliation.oracle-base-url: use /flowers-dev-api or an equivalent "
                + "rs-gateway/rs-flowers-base base URL, not the dts-admin /api service.";
    }

    private void putDatabase(ObjectNode body, String rawDatabase) {
        if (!StringUtils.hasText(rawDatabase)) {
            throw new IllegalArgumentException("copilotRequest.database is required for live dataset reconciliation");
        }
        String value = rawDatabase.trim();
        try {
            body.put("database", Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            body.put("database", value);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize finance detail reconciliation request", e);
        }
    }

    private static String text(Map<String, String> values, String key) {
        if (values == null) {
            return "";
        }
        String value = values.get(key);
        return value == null ? "" : value.trim();
    }

    private static String textOrDefault(Map<String, String> values, String key, String fallback) {
        String value = text(values, key);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static int parseInt(String value, int fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String normalizeAuthorizationHeader(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())
                || trimmed.regionMatches(true, 0, "Basic ", 0, "Basic ".length())
                ? trimmed
                : "Bearer " + trimmed;
    }

    private static String trimTrailingSlash(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record Endpoint(String method, String path) {
        private static Endpoint parse(String signature) {
            if (!StringUtils.hasText(signature)) {
                throw new IllegalArgumentException("finance oracle endpoint is blank");
            }
            String[] parts = signature.trim().split("\\s+", 2);
            if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
                throw new IllegalArgumentException("finance oracle endpoint must be '<METHOD> <PATH>': " + signature);
            }
            String method = parts[0].trim().toUpperCase();
            String path = parts[1].trim();
            return new Endpoint(method, path.startsWith("/") ? path : "/" + path);
        }

        private String pathWithQuery(Map<String, String> queryValues) {
            if (!"GET".equals(method) || queryValues == null || queryValues.isEmpty()) {
                return path;
            }
            StringBuilder query = new StringBuilder();
            queryValues.forEach((key, value) -> {
                if (!StringUtils.hasText(key) || value == null) {
                    return;
                }
                if (!query.isEmpty()) {
                    query.append('&');
                }
                query.append(encode(key)).append('=').append(encode(value));
            });
            return query.isEmpty() ? path : path + "?" + query;
        }

        private static String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
    }
}
