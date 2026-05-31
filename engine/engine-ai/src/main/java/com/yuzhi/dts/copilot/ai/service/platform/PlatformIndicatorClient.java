package com.yuzhi.dts.copilot.ai.service.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PlatformIndicatorClient implements PlatformIndicatorCatalogClient {

    private final PlatformIndicatorProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile CachedToken cachedToken;

    public PlatformIndicatorClient(
            PlatformIndicatorProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();
    }

    @Override
    public PlatformIndicatorPage listPublishedIndicators(int page, int size) {
        if (!StringUtils.hasText(properties.baseUrl())) {
            throw new IllegalStateException("copilot.platform.indicator.base-url is required");
        }
        try {
            String path = "/api/governance/indicators?status=" + encode("PUBLISHED")
                    + "&page=" + encode(String.valueOf(Math.max(0, page)))
                    + "&size=" + encode(String.valueOf(Math.max(1, size)));
            JsonNode payload = sendGet(path);
            JsonNode data = unwrapData(payload);
            JsonNode itemsNode = findItemsNode(data);
            return new PlatformIndicatorPage(
                    toDtos(itemsNode),
                    firstInt(data, page, "page", "number", "current"),
                    firstInt(data, size, "size", "pageSize"),
                    resolveTotalPages(data, page));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("platform indicator call interrupted", e);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private JsonNode sendGet(String path) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(resolve(path))
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .GET()
                .header("Accept", "application/json");
        if (StringUtils.hasText(properties.serviceName()) && StringUtils.hasText(properties.serviceToken())) {
            builder.header("X-DTS-Service", properties.serviceName());
            builder.header("X-DTS-Service-Token", properties.serviceToken());
        } else {
            String token = resolveAccessToken();
            if (StringUtils.hasText(token)) {
                builder.header("Authorization", "Bearer " + token);
            }
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private String resolveAccessToken() throws IOException, InterruptedException {
        if (StringUtils.hasText(properties.authToken())) {
            return properties.authToken().trim();
        }
        if (!StringUtils.hasText(properties.tokenUrl())
                || !StringUtils.hasText(properties.clientId())
                || !StringUtils.hasText(properties.clientSecret())) {
            return "";
        }
        CachedToken token = cachedToken;
        if (token != null && token.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return token.value();
        }
        String body = "grant_type=client_credentials"
                + "&client_id=" + encode(properties.clientId())
                + "&client_secret=" + encode(properties.clientSecret());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.tokenUrl()))
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Token HTTP " + response.statusCode());
        }
        JsonNode payload = objectMapper.readTree(response.body());
        String accessToken = firstText(payload, "access_token", "token");
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("Token response missing access_token");
        }
        long expiresIn = payload.path("expires_in").asLong(300);
        cachedToken = new CachedToken(accessToken, Instant.now().plusSeconds(Math.max(60, expiresIn)));
        return accessToken;
    }

    private List<PlatformIndicatorDto> toDtos(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<PlatformIndicatorDto> result = new ArrayList<>();
        for (JsonNode item : node) {
            String id = firstText(item, "id", "indicatorId", "indicator_id", "uuid");
            String code = firstText(item, "code", "indicatorCode", "indicator_code");
            String name = firstText(item, "name", "displayName", "display_name");
            if (!StringUtils.hasText(id) || !StringUtils.hasText(name)) {
                continue;
            }
            result.add(new PlatformIndicatorDto(
                    id,
                    code,
                    name,
                    firstText(item, "category", "categoryName", "dept"),
                    firstText(item, "domain", "businessDomain", "business_domain"),
                    firstText(item, "definition", "description"),
                    firstText(item, "expressionSql", "expression_sql", "formula"),
                    firstText(item, "status"),
                    firstText(item, "version", "latestVersion", "latest_version"),
                    firstText(item, "tags"),
                    firstText(item, "dimensionFields", "dimension_fields", "dimensions"),
                    firstText(item, "dateColumn", "date_column", "timeColumn", "time_column"),
                    firstText(item, "timeGrain", "time_grain", "granularity"),
                    firstText(item, "aggregationType", "aggregation_type", "aggregation"),
                    firstText(item, "measureField", "measure_field", "measure"),
                    firstText(item, "dataLevel", "data_level", "classificationLevel"),
                    firstText(item, "owner")));
        }
        return List.copyOf(result);
    }

    private JsonNode unwrapData(JsonNode payload) {
        if (payload == null) {
            return objectMapper.createObjectNode();
        }
        JsonNode data = payload.path("data");
        return data.isMissingNode() || data.isNull() ? payload : data;
    }

    private JsonNode findItemsNode(JsonNode data) {
        if (data == null) {
            return null;
        }
        if (data.isArray()) {
            return data;
        }
        for (String key : List.of("items", "content", "records", "list")) {
            JsonNode node = data.path(key);
            if (node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private int resolveTotalPages(JsonNode data, int fallbackPage) {
        int totalPages = firstInt(data, -1, "totalPages", "total_pages", "pages");
        if (totalPages > 0) {
            return totalPages;
        }
        int totalElements = firstInt(data, -1, "totalElements", "total", "totalCount");
        int size = firstInt(data, 0, "size", "pageSize");
        if (totalElements >= 0 && size > 0) {
            return Math.max(1, (int) Math.ceil(totalElements / (double) size));
        }
        return fallbackPage + 1;
    }

    private int firstInt(JsonNode node, int fallback, String... keys) {
        JsonNode value = firstExisting(node, keys);
        if (value == null) {
            return fallback;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private JsonNode firstExisting(JsonNode node, String... keys) {
        if (node == null) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... keys) {
        JsonNode value = firstExisting(node, keys);
        if (value == null) {
            return null;
        }
        if (value.isTextual()) {
            String text = value.asText().trim();
            return text.isEmpty() ? null : text;
        }
        if (value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        if (value.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : value) {
                String text = item.asText("").trim();
                if (!text.isEmpty()) {
                    values.add(text);
                }
            }
            return String.join(",", values);
        }
        return null;
    }

    private URI resolve(String path) {
        String base = properties.baseUrl().replaceAll("/+$", "");
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(base + normalizedPath);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record CachedToken(String value, Instant expiresAt) {}
}
