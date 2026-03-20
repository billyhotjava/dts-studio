package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.service.chat.AgentChatService;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/internal/agent/chat")
public class InternalAgentChatResource {

    private final AgentChatService agentChatService;
    private final String adminSecret;

    public InternalAgentChatResource(
            AgentChatService agentChatService,
            @Value("${copilot.admin-secret:}") String adminSecret) {
        this.agentChatService = agentChatService;
        this.adminSecret = adminSecret;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @RequestBody ChatRequest request) {
        ResponseEntity<?> authError = checkAdminSecret(secret);
        if (authError != null) {
            return authError;
        }
        if (!request.hasRequiredFields()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId and message are required"));
        }

        // If sessionId belongs to another user, treat as new session
        String effectiveSessionId = request.sessionId();
        if (StringUtils.hasText(effectiveSessionId)) {
            ResponseEntity<?> ownershipError = verifySessionOwnership(effectiveSessionId, request.userId());
            if (ownershipError != null) {
                effectiveSessionId = null;
            }
        }

        String response = agentChatService.sendMessage(
                effectiveSessionId,
                request.userId(),
                request.message(),
                request.datasourceId(),
                request.martHealth());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", resolveSessionIdForResponse(request.sessionId(), request.userId()));
        result.put("response", response);
        result.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping(path = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody sendMessageStream(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @RequestBody ChatRequest request) {
        if (!StringUtils.hasText(adminSecret) || !Objects.equals(adminSecret, secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin secret");
        }
        if (!request.hasRequiredFields()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and message are required");
        }

        // If sessionId belongs to another user, treat as new session
        String effectiveSessionId = request.sessionId();
        if (StringUtils.hasText(effectiveSessionId)) {
            ResponseEntity<?> ownershipError = verifySessionOwnership(effectiveSessionId, request.userId());
            if (ownershipError != null) {
                effectiveSessionId = null;
            }
        }
        final String sessionId = effectiveSessionId;

        return outputStream -> agentChatService.sendMessageStream(
                sessionId, request.userId(), request.message(),
                request.datasourceId(), request.martHealth(), outputStream);
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        ResponseEntity<?> authError = checkAdminSecret(secret);
        if (authError != null) {
            return authError;
        }
        List<Map<String, Object>> result = agentChatService.getSessions(userId).stream()
                .limit(Math.max(limit, 0))
                .map(this::toSessionSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @PathVariable String sessionId,
            @RequestParam String userId) {
        ResponseEntity<?> authError = checkAdminSecret(secret);
        if (authError != null) {
            return authError;
        }
        return agentChatService.getSession(sessionId)
                .filter(session -> Objects.equals(session.getUserId(), userId))
                .map(session -> {
                    Map<String, Object> result = toSessionSummary(session);
                    result.put("messages", session.getMessages().stream()
                            .map(this::toMessageMap)
                            .collect(Collectors.toList()));
                    return ResponseEntity.ok(result);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @PathVariable String sessionId,
            @RequestParam String userId) {
        ResponseEntity<?> authError = checkAdminSecret(secret);
        if (authError != null) {
            return authError;
        }
        ResponseEntity<?> ownershipError = verifySessionOwnership(sessionId, userId);
        if (ownershipError != null) {
            return ownershipError;
        }
        agentChatService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> verifySessionOwnership(String sessionId, String userId) {
        return agentChatService.getSession(sessionId)
                .filter(session -> Objects.equals(session.getUserId(), userId))
                .isPresent()
                ? null
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Session not found"));
    }

    private ResponseEntity<?> checkAdminSecret(String secret) {
        if (!StringUtils.hasText(adminSecret)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Admin secret not configured"));
        }
        if (!Objects.equals(adminSecret, secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid admin secret"));
        }
        return null;
    }

    private String resolveSessionIdForResponse(String requestedSessionId, String userId) {
        if (StringUtils.hasText(requestedSessionId)) {
            return requestedSessionId;
        }
        return agentChatService.getSessions(userId).stream()
                .findFirst()
                .map(AiChatSession::getSessionId)
                .orElse(null);
    }

    private Map<String, Object> toSessionSummary(AiChatSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sessionId", session.getSessionId());
        map.put("userId", session.getUserId());
        map.put("title", session.getTitle());
        map.put("status", session.getStatus());
        map.put("dataSourceId", session.getDataSourceId());
        map.put("createdAt", session.getCreatedAt() != null ? session.getCreatedAt().toString() : null);
        map.put("updatedAt", session.getUpdatedAt() != null ? session.getUpdatedAt().toString() : null);
        return map;
    }

    private Map<String, Object> toMessageMap(AiChatMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", message.getId());
        map.put("role", message.getRole());
        map.put("content", message.getContent());
        map.put("toolCalls", message.getToolCalls());
        map.put("toolCallId", message.getToolCallId());
        map.put("generatedSql", message.getGeneratedSql());
        map.put("reasoningContent", message.getReasoningContent());
        map.put("responseKind", message.getResponseKind());
        map.put("routedDomain", message.getRoutedDomain());
        map.put("targetView", message.getTargetView());
        map.put("templateCode", message.getTemplateCode());
        map.put("createdAt", message.getCreatedAt() != null ? message.getCreatedAt().toString() : null);
        return map;
    }

    public record ChatRequest(
            String sessionId,
            String userId,
            String message,
            @JsonProperty("datasourceId") Long datasourceId,
            Map<String, Boolean> martHealth
    ) {
        boolean hasRequiredFields() {
            return StringUtils.hasText(userId) && StringUtils.hasText(message);
        }
    }
}
