package com.yuzhi.dts.copilot.ai.service.chat;

import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.repository.AiChatSessionRepository;
import com.yuzhi.dts.copilot.ai.service.agent.AgentExecutionService;
import com.yuzhi.dts.copilot.ai.service.agent.AgentExecutionService.ChatExecutionResult;
import com.yuzhi.dts.copilot.ai.service.audit.AiAuditService;
import com.yuzhi.dts.copilot.ai.service.copilot.ChatGroundingService.GroundingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing AI chat sessions and message flow.
 * Coordinates between the chat persistence layer and the agent execution engine.
 */
@Service
public class AgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AgentChatService.class);

    private final AiChatSessionRepository sessionRepository;
    private final AgentExecutionService agentExecutionService;
    private final AiAuditService auditService;

    public AgentChatService(AiChatSessionRepository sessionRepository,
                            AgentExecutionService agentExecutionService,
                            AiAuditService auditService) {
        this.sessionRepository = sessionRepository;
        this.agentExecutionService = agentExecutionService;
        this.auditService = auditService;
    }

    /**
     * Send a message in a chat session. Creates the session if it does not exist.
     *
     * @param sessionId    the session identifier (may be {@code null} for a new session)
     * @param userId       the user identifier
     * @param message      the user's message
     * @param datasourceId optional data source identifier from the request; overrides
     *                     the session's persisted value when non-null
     * @return the agent's response text
     */
    @Transactional
    public String sendMessage(String sessionId, String userId, String message, Long datasourceId) {
        return sendMessage(sessionId, userId, message, datasourceId, Collections.emptyMap());
    }

    @Transactional
    public String sendMessage(String sessionId, String userId, String message, Long datasourceId,
                              Map<String, Boolean> martHealthSnapshot) {
        AiChatSession session = resolveOrCreateSession(sessionId, userId);

        // If the request carries a datasourceId, update the session so it persists
        if (datasourceId != null) {
            session.setDataSourceId(datasourceId);
        }

        // Persist user message
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setRole("user");
        userMsg.setContent(message);
        session.addMessage(userMsg);

        // Build history from persisted messages (excluding the one we just added)
        List<Map<String, Object>> history = buildHistory(session);

        // Resolve effective dataSourceId: request value takes precedence over session value
        Long effectiveDataSourceId = datasourceId != null ? datasourceId : session.getDataSourceId();

        // Execute agent
        ChatExecutionResult executionResult = agentExecutionService.executeChat(
                session.getSessionId(), userId, message, history, effectiveDataSourceId, martHealthSnapshot);
        String response = executionResult.response();

        // Persist assistant message
        AiChatMessage assistantMsg = new AiChatMessage();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(response);
        assistantMsg.setGeneratedSql(executionResult.generatedSql());
        assistantMsg.setReasoningContent(executionResult.reasoningContent());
        applyGroundingMetadata(assistantMsg, executionResult.groundingContext());
        session.addMessage(assistantMsg);

        // Auto-generate title from first message
        if (session.getTitle() == null || session.getTitle().isBlank()) {
            session.setTitle(generateTitle(message));
        }

        sessionRepository.save(session);

        auditService.logChatAction(userId, session.getSessionId(),
                "CHAT_MESSAGE", message, response);

        return response;
    }

    /**
     * Send a message with streaming response.
     *
     * @param sessionId    the session identifier
     * @param userId       the user identifier
     * @param message      the user's message
     * @param datasourceId optional data source identifier from the request
     * @param output       the output stream for SSE events
     */
    public void sendMessageStream(String sessionId, String userId, String message,
                                  Long datasourceId, OutputStream output) {
        sendMessageStream(sessionId, userId, message, datasourceId, Collections.emptyMap(), output);
    }

    public void sendMessageStream(String sessionId, String userId, String message,
                                  Long datasourceId, Map<String, Boolean> martHealthSnapshot,
                                  OutputStream output) {
        AiChatSession session = resolveOrCreateSession(sessionId, userId);
        if (datasourceId != null) {
            session.setDataSourceId(datasourceId);
        }

        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setRole("user");
        userMsg.setContent(message);
        session.addMessage(userMsg);

        List<Map<String, Object>> history = buildHistory(session);
        Long effectiveDataSourceId = datasourceId != null ? datasourceId : session.getDataSourceId();

        // Send session event first so frontend gets the sessionId immediately
        try {
            String sessionEvent = "event: session\ndata: {\"sessionId\":\"%s\"}\n\n"
                    .formatted(session.getSessionId());
            output.write(sessionEvent.getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (Exception e) {
            log.debug("SSE session event write failed (client disconnected): {}", e.getMessage());
            return;
        }

        // Execute with real streaming
        ChatExecutionResult executionResult;
        try {
            executionResult = agentExecutionService.executeChatStream(
                    session.getSessionId(), userId, message, history, effectiveDataSourceId, martHealthSnapshot, output);
        } catch (Exception e) {
            if (isStreamInterrupted(e)) {
                restoreInterruptFlag(e);
                log.info("Streaming chat interrupted: {}", describeInterruption(e));
                return;
            }
            log.error("Streaming chat failed: {}", e.getMessage(), e);
            String errorMessage = buildStreamFailureMessage(e);
            persistStreamingFailure(session, userId, message, errorMessage);
            try {
                output.write(("event: error\ndata: {\"error\":\"" + escapeForSse(errorMessage) + "\"}\n\n")
                        .getBytes(StandardCharsets.UTF_8));
                output.flush();
            } catch (Exception ignored) {}
            return;
        }

        // Persist assistant message
        AiChatMessage assistantMsg = new AiChatMessage();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(executionResult.response());
        assistantMsg.setGeneratedSql(executionResult.generatedSql());
        assistantMsg.setReasoningContent(executionResult.reasoningContent());
        applyGroundingMetadata(assistantMsg, executionResult.groundingContext());
        session.addMessage(assistantMsg);

        if (session.getTitle() == null || session.getTitle().isBlank()) {
            session.setTitle(generateTitle(message));
        }

        sessionRepository.save(session);
        auditService.logChatAction(userId, session.getSessionId(), "CHAT_MESSAGE", message, executionResult.response());
    }

    /**
     * Get all sessions for a user.
     */
    @Transactional(readOnly = true)
    public List<AiChatSession> getSessions(String userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * Get a specific session with its messages.
     */
    @Transactional(readOnly = true)
    public Optional<AiChatSession> getSession(String sessionId) {
        Optional<AiChatSession> session = sessionRepository.findBySessionId(sessionId);
        // Force-initialize the lazy messages collection
        session.ifPresent(s -> s.getMessages().size());
        return session;
    }

    /**
     * Delete a chat session and all its messages.
     */
    @Transactional
    public void deleteSession(String sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            auditService.logChatAction(session.getUserId(), sessionId,
                    "SESSION_DELETE", null, null);
            sessionRepository.delete(session);
        });
    }

    private AiChatSession resolveOrCreateSession(String sessionId, String userId) {
        if (sessionId != null && !sessionId.isBlank()) {
            Optional<AiChatSession> existing = sessionRepository.findBySessionId(sessionId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // Create new session
        AiChatSession session = new AiChatSession();
        session.setSessionId(sessionId != null && !sessionId.isBlank()
                ? sessionId : UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setStatus("ACTIVE");

        auditService.logChatAction(userId, session.getSessionId(),
                "SESSION_CREATE", null, null);

        return sessionRepository.save(session);
    }

    private List<Map<String, Object>> buildHistory(AiChatSession session) {
        List<Map<String, Object>> history = new ArrayList<>();
        List<AiChatMessage> messages = session.getMessages();

        // Exclude the last message (which is the current user message we just added)
        int end = messages.size() > 1 ? messages.size() - 1 : 0;
        for (int i = 0; i < end; i++) {
            AiChatMessage msg = messages.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("role", msg.getRole());
            entry.put("content", msg.getContent());
            history.add(entry);
        }
        return history;
    }

    private String generateTitle(String firstMessage) {
        if (firstMessage == null) {
            return "New Chat";
        }
        String title = firstMessage.trim();
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }
        return title;
    }

    private String escapeForSse(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\n", "\\n").replace("\r", "");
    }

    private void persistStreamingFailure(AiChatSession session, String userId, String userMessage, String errorMessage) {
        AiChatMessage assistantMsg = new AiChatMessage();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(errorMessage);
        session.addMessage(assistantMsg);

        if (session.getTitle() == null || session.getTitle().isBlank()) {
            session.setTitle(generateTitle(userMessage));
        }

        sessionRepository.save(session);
        auditService.logChatAction(userId, session.getSessionId(),
                "CHAT_MESSAGE_ERROR", userMessage, errorMessage);
    }

    static String buildStreamFailureMessage(Exception exception) {
        String base = "抱歉，本次回答失败，请稍后重试。";
        if (exception == null || !StringUtils.hasText(exception.getMessage())) {
            return base;
        }
        String detail = exception.getMessage().trim();
        if (detail.length() > 200) {
            detail = detail.substring(0, 197) + "...";
        }
        return base + " 原因: " + detail;
    }

    private void applyGroundingMetadata(AiChatMessage assistantMsg, GroundingContext groundingContext) {
        if (assistantMsg == null || groundingContext == null || groundingContext.needsClarification()) {
            return;
        }
        assistantMsg.setRoutedDomain(groundingContext.domain());
        assistantMsg.setTargetView(groundingContext.primaryView());
        assistantMsg.setTemplateCode(groundingContext.templateCode());
    }

    static boolean isStreamInterrupted(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InterruptedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void restoreInterruptFlag(Throwable throwable) {
        if (isStreamInterrupted(throwable)) {
            Thread.currentThread().interrupt();
        }
    }

    private static String describeInterruption(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InterruptedException interrupted) {
                return StringUtils.hasText(interrupted.getMessage()) ? interrupted.getMessage() : interrupted.getClass().getSimpleName();
            }
            current = current.getCause();
        }
        return throwable != null && StringUtils.hasText(throwable.getMessage())
                ? throwable.getMessage()
                : "interrupted";
    }
}
