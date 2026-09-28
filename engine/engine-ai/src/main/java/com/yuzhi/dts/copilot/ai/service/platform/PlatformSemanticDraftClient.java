package com.yuzhi.dts.copilot.ai.service.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticDraftGovernanceSubmissionService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PlatformSemanticDraftClient implements SemanticDraftGovernanceSubmissionService.GovernanceDraftClient {

    private final PlatformSemanticDraftProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PlatformSemanticDraftClient(
            PlatformSemanticDraftProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();
    }

    @Override
    public SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult submit(
            SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmission submission) {
        if (!StringUtils.hasText(properties.baseUrl())) {
            throw new IllegalStateException("copilot.platform.semantic-draft.base-url is required");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(submission.payload());
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(resolve(submission.targetPath()))
                    .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));
            addAuthHeaders(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(errorMessage(response));
            }
            JsonNode data = unwrapData(objectMapper.readTree(response.body()));
            return new SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult(
                    submission.draftId(),
                    submission.targetType(),
                    submission.targetPath(),
                    firstText(data, "id", "uuid", "indicatorId", "standardId"),
                    firstText(data, "status", "reviewStatus"),
                    "DRAFT_SUBMITTED",
                    true,
                    "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("platform semantic draft call interrupted", e);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void addAuthHeaders(HttpRequest.Builder builder) {
        if (StringUtils.hasText(properties.serviceName()) && StringUtils.hasText(properties.serviceToken())) {
            builder.header("X-DTS-Service", properties.serviceName());
            builder.header("X-DTS-Service-Token", properties.serviceToken());
        } else if (StringUtils.hasText(properties.authToken())) {
            builder.header("Authorization", "Bearer " + properties.authToken());
        }
    }

    private URI resolve(String path) {
        String base = properties.baseUrl().replaceAll("/+$", "");
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(base + normalizedPath);
    }

    private JsonNode unwrapData(JsonNode payload) {
        if (payload == null) {
            return objectMapper.createObjectNode();
        }
        JsonNode data = payload.path("data");
        return data.isMissingNode() || data.isNull() ? payload : data;
    }

    private String firstText(JsonNode node, String... keys) {
        if (node == null) {
            return "";
        }
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText("").trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    private String errorMessage(HttpResponse<String> response) {
        String message = "HTTP " + response.statusCode();
        String body = response.body();
        if (!StringUtils.hasText(body)) {
            return message;
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        if (compact.length() > 500) {
            compact = compact.substring(0, 500) + "...";
        }
        return message + ": " + compact;
    }
}
