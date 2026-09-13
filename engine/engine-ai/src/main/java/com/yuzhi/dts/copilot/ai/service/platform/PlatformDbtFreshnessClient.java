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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PlatformDbtFreshnessClient implements DbtFreshnessResolver {

    private static final Logger log = LoggerFactory.getLogger(PlatformDbtFreshnessClient.class);
    private static final Set<String> SUCCESS_STATUSES = Set.of("SUCCESS", "PASS", "PASSED", "OK", "COMPLETED");

    private final PlatformDbtFreshnessProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Clock clock;

    @Autowired
    public PlatformDbtFreshnessClient(
            PlatformDbtFreshnessProperties properties,
            ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    PlatformDbtFreshnessClient(
            PlatformDbtFreshnessProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();
    }

    @Override
    public Map<String, String> resolveFreshness(Collection<String> relations) {
        if (!properties.enabled() || !StringUtils.hasText(properties.baseUrl())
                || relations == null || relations.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String relation : normalizeRelations(relations)) {
            try {
                String status = diagnose(relation);
                if (StringUtils.hasText(status)) {
                    result.put(relation, status);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("dbt freshness diagnostics interrupted for {}", relation);
                break;
            } catch (IOException | RuntimeException e) {
                log.debug("dbt freshness diagnostics skipped for {}: {}", relation, e.getMessage());
            }
        }
        return result.isEmpty() ? Map.of() : Collections.unmodifiableMap(result);
    }

    private List<String> normalizeRelations(Collection<String> relations) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String relation : relations) {
            String normalized = normalizeRelation(relation);
            if (StringUtils.hasText(normalized)) {
                values.add(normalized);
            }
        }
        return List.copyOf(values);
    }

    private String diagnose(String relation) throws IOException, InterruptedException {
        JsonNode payload = sendGet("/api/etl/dbt/models/" + encode(modelName(relation)) + "/diagnostics");
        return mapDiagnosticsToFreshness(unwrapData(payload));
    }

    private JsonNode sendGet(String path) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(resolve(path))
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .GET()
                .header("Accept", "application/json");
        addAuthHeaders(builder);
        if (StringUtils.hasText(properties.activeDept())) {
            builder.header("X-Active-Dept", properties.activeDept().trim());
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private void addAuthHeaders(HttpRequest.Builder builder) {
        if (StringUtils.hasText(properties.serviceName()) && StringUtils.hasText(properties.serviceToken())) {
            builder.header("X-DTS-Service", properties.serviceName().trim());
            builder.header("X-DTS-Service-Token", properties.serviceToken().trim());
        } else if (StringUtils.hasText(properties.authToken())) {
            builder.header("Authorization", "Bearer " + properties.authToken().trim());
        }
    }

    private String mapDiagnosticsToFreshness(JsonNode data) {
        if (data == null || data.isMissingNode() || data.isNull()) {
            return "UNKNOWN";
        }
        if (!booleanValue(data, "enabled", true)) {
            return "UNKNOWN";
        }
        boolean success = booleanValue(data, "success", true);
        JsonNode current = data.path("current");
        if (current.isMissingNode() || !booleanValue(current, "exists", false)) {
            return "MISSING";
        }
        if (!success) {
            return "STALE";
        }
        JsonNode dbtRun = data.path("runtime").path("dbtRun");
        if (booleanValue(dbtRun, "present", false)) {
            return freshnessFromRun(dbtRun, "status", "generatedAt");
        }
        JsonNode airflowRun = data.path("runtime").path("airflowRun");
        if (booleanValue(airflowRun, "present", false)) {
            return freshnessFromRun(airflowRun, "state", "logicalDate");
        }
        return "STALE";
    }

    private String freshnessFromRun(JsonNode run, String statusKey, String timestampKey) {
        String status = firstText(run, statusKey);
        if (StringUtils.hasText(status) && !isSuccessStatus(status)) {
            return "STALE";
        }
        Instant generatedAt = parseInstant(firstText(run, timestampKey));
        if (generatedAt == null) {
            return "STALE";
        }
        Duration age = Duration.between(generatedAt, Instant.now(clock));
        if (age.compareTo(Duration.ofHours(properties.staleAfterHours())) > 0) {
            return "STALE";
        }
        return "FRESH";
    }

    private boolean isSuccessStatus(String status) {
        return SUCCESS_STATUSES.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    private JsonNode unwrapData(JsonNode payload) {
        if (payload == null) {
            return objectMapper.createObjectNode();
        }
        JsonNode data = payload.path("data");
        return data.isMissingNode() || data.isNull() ? payload : data;
    }

    private boolean booleanValue(JsonNode node, String key, boolean fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        JsonNode value = node.path(key);
        if (value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isTextual()) {
            String text = value.asText("").trim();
            if ("true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text) || "1".equals(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text) || "0".equals(text)) {
                return false;
            }
        }
        return fallback;
    }

    private String firstText(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (value.isMissingNode() || value.isNull()) {
                continue;
            }
            String text = value.asText("").trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Instant.parse(trimmed);
        } catch (Exception ignored) {
            // try offset/local formats below
        }
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (Exception ignored) {
            // try local format below
        }
        try {
            return LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeRelation(String relation) {
        if (!StringUtils.hasText(relation)) {
            return null;
        }
        String normalized = relation.trim();
        int colon = normalized.indexOf(':');
        if (colon >= 0) {
            normalized = normalized.substring(colon + 1).trim();
        }
        normalized = normalized.replace("`", "").replace("\"", "");
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String modelName(String relation) {
        String normalized = normalizeRelation(relation);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        int dot = normalized.lastIndexOf('.');
        return dot >= 0 ? normalized.substring(dot + 1) : normalized;
    }

    private URI resolve(String path) {
        String base = properties.baseUrl().replaceAll("/+$", "");
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(base + normalizedPath);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
