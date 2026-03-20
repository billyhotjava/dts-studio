package com.yuzhi.dts.copilot.ai.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.service.chat.AgentChatService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for AI agent chat operations.
 */
@RestController
@RequestMapping("/api/ai/agent/chat")
public class AgentChatResource {

    private static final Logger log = LoggerFactory.getLogger(AgentChatResource.class);

    private final AgentChatService agentChatService;

    public AgentChatResource(AgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    /**
     * POST /api/ai/agent/chat/send - Send a message and get a synchronous response.
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody ChatRequest request) {
        log.info("Chat send: sessionId={}, userId={}", request.sessionId(), request.userId());

        String response = agentChatService.sendMessage(
                request.sessionId(), request.userId(), request.message(), request.datasourceId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", resolveSessionIdForResponse(request.sessionId(), request.userId()));
        result.put("response", response);
        result.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/ai/agent/chat/stream - Send a message with SSE streaming response.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamMessage(@RequestBody ChatRequest request,
                              HttpServletResponse response) {
        log.info("Chat stream: sessionId={}, userId={}", request.sessionId(), request.userId());

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        try {
            OutputStream output = response.getOutputStream();
            agentChatService.sendMessageStream(
                    request.sessionId(), request.userId(), request.message(), request.datasourceId(), output);
        } catch (Exception e) {
            log.error("Stream chat failed: {}", e.getMessage(), e);
        }
    }

    /**
     * GET /api/ai/agent/chat/sessions - List all sessions for a user.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> getSessions(
            @RequestParam String userId) {
        List<AiChatSession> sessions = agentChatService.getSessions(userId);

        List<Map<String, Object>> result = sessions.stream()
                .map(this::toSessionSummary)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/ai/agent/chat/{sessionId} - Get a session with all messages.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        return agentChatService.getSession(sessionId)
                .map(session -> {
                    Map<String, Object> result = toSessionSummary(session);
                    result.put("messages", session.getMessages().stream()
                            .map(this::toMessageMap)
                            .collect(Collectors.toList()));
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/ai/agent/chat/{sessionId} - Delete a session.
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        agentChatService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
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

    private Map<String, Object> toMessageMap(AiChatMessage msg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", msg.getId());
        map.put("role", msg.getRole());
        map.put("content", msg.getContent());
        map.put("toolCalls", msg.getToolCalls());
        map.put("toolCallId", msg.getToolCallId());
        map.put("generatedSql", msg.getGeneratedSql());
        map.put("reasoningContent", msg.getReasoningContent());
        map.put("responseKind", msg.getResponseKind());
        map.put("routedDomain", msg.getRoutedDomain());
        map.put("targetView", msg.getTargetView());
        map.put("templateCode", msg.getTemplateCode());
        map.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null);
        return map;
    }

    /**
     * Request body for chat send/stream endpoints.
     */
    public record ChatRequest(
            String sessionId,
            String userId,
            String message,
            @JsonProperty("datasourceId") Long datasourceId
    ) {}

    private String resolveSessionIdForResponse(String requestedSessionId, String userId) {
        if (StringUtils.hasText(requestedSessionId)) {
            return requestedSessionId;
        }
        return agentChatService.getSessions(userId).stream()
                .findFirst()
                .map(AiChatSession::getSessionId)
                .orElse(null);
    }
}
