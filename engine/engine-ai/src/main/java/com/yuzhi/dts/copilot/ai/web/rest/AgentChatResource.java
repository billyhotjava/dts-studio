package com.yuzhi.dts.copilot.ai.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.security.CopilotUserContext;
import com.yuzhi.dts.copilot.ai.security.CopilotUserContextHolder;
import com.yuzhi.dts.copilot.ai.service.chat.AgentChatService;
import com.yuzhi.dts.copilot.ai.service.copilot.OntologyActionApprovalService;
import com.yuzhi.dts.copilot.ai.service.copilot.OntologyActionExecutor;
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
    private final OntologyActionApprovalService actionApprovalService;

    public AgentChatResource(
            AgentChatService agentChatService,
            OntologyActionApprovalService actionApprovalService) {
        this.agentChatService = agentChatService;
        this.actionApprovalService = actionApprovalService;
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
        map.put("dataSurface", msg.getDataSurface());
        map.put("qualityLevel", msg.getQualityLevel());
        map.put("qualityNotes", msg.getQualityNotes());
        map.put("suggestedDisplay", msg.getSuggestedDisplay());
        map.put("reportCode", msg.getReportCode());
        map.put("sourceRefs", msg.getSourceRefs());
        map.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null);
        return map;
    }

    @PostMapping("/approve")
    public ResponseEntity<Map<String, Object>> approveAction(@RequestBody ApproveActionRequest request) {
        ActionRef actionRef = parseActionRef(request == null ? null : request.actionId());
        if (actionRef == null) {
            return ResponseEntity.badRequest().body(errorResponse(
                    request == null ? null : request.sessionId(),
                    "Invalid actionId, expected '<domain>:<actionName>'"));
        }
        CopilotUserContext userContext = CopilotUserContextHolder.get();
        Map<String, Object> formData = request.formData() == null ? Map.of() : request.formData();
        OntologyActionApprovalService.ActionApprovalResult result = actionApprovalService.requestDraft(
                new OntologyActionApprovalService.ActionApprovalRequest(
                        actionRef.domain(),
                        actionRef.actionName(),
                        formData,
                        true,
                        userContext,
                        request.sessionId()));
        return ResponseEntity.ok(toActionChatResponse(request.sessionId(), result));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelAction(@RequestBody CancelActionRequest request) {
        String sessionId = request == null ? null : request.sessionId();
        String actionId = request == null ? null : request.actionId();
        String message = StringUtils.hasText(actionId)
                ? "已取消建议动作：" + actionId
                : "已取消建议动作。";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sessionId", sessionId);
        body.put("agentMessage", message);
        body.put("response", message);
        body.put("timestamp", Instant.now().toString());
        body.put("toolCalls", List.of());
        body.put("requiresApproval", false);
        body.put("pendingAction", null);
        return ResponseEntity.ok(body);
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

    public record ApproveActionRequest(
            String sessionId,
            String actionId,
            Map<String, Object> formData
    ) {}

    public record CancelActionRequest(
            String sessionId,
            String actionId
    ) {}

    private Map<String, Object> toActionChatResponse(
            String sessionId,
            OntologyActionApprovalService.ActionApprovalResult result) {
        Map<String, Object> body = new LinkedHashMap<>();
        String message = result == null ? "动作处理失败" : result.message();
        body.put("sessionId", sessionId);
        body.put("agentMessage", message);
        body.put("response", message);
        body.put("timestamp", Instant.now().toString());
        body.put("toolCalls", result == null ? List.of() : toToolCalls(result));
        body.put("requiresApproval", result != null && result.requiresApproval());
        body.put("pendingAction", result != null && result.requiresApproval() ? result.card() : null);
        return body;
    }

    private List<Map<String, Object>> toToolCalls(OntologyActionApprovalService.ActionApprovalResult result) {
        OntologyActionExecutor.ActionDraftResult draftResult = result.draftResult();
        if (draftResult == null) {
            return List.of();
        }
        Map<String, Object> toolResult = new LinkedHashMap<>();
        toolResult.put("success", draftResult.success());
        toolResult.put("textSummary", draftResult.message());
        toolResult.put("data", draftResult.responseBody());

        Map<String, Object> toolCall = new LinkedHashMap<>();
        toolCall.put("toolId", result.card() == null ? "ontology.action.createDraft" : result.card().toolId());
        toolCall.put("params", draftResult.payload());
        toolCall.put("result", toolResult);
        return List.of(toolCall);
    }

    private Map<String, Object> errorResponse(String sessionId, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sessionId", sessionId);
        body.put("agentMessage", message);
        body.put("response", message);
        body.put("timestamp", Instant.now().toString());
        body.put("toolCalls", List.of());
        body.put("requiresApproval", false);
        body.put("pendingAction", null);
        return body;
    }

    private ActionRef parseActionRef(String actionId) {
        if (!StringUtils.hasText(actionId)) {
            return null;
        }
        int separator = actionId.indexOf(':');
        if (separator <= 0 || separator >= actionId.length() - 1) {
            return null;
        }
        return new ActionRef(actionId.substring(0, separator), actionId.substring(separator + 1));
    }

    private record ActionRef(String domain, String actionName) {
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
}
