package com.yuzhi.dts.copilot.ai.service.chat;

import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.repository.AiChatSessionRepository;
import com.yuzhi.dts.copilot.ai.service.agent.AgentExecutionService;
import com.yuzhi.dts.copilot.ai.service.audit.AiAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
     * @param sessionId the session identifier (may be {@code null} for a new session)
     * @param userId    the user identifier
     * @param message   the user's message
     * @return the agent's response text
     */
    @Transactional
    public String sendMessage(String sessionId, String userId, String message) {
        AiChatSession session = resolveOrCreateSession(sessionId, userId);

        // Persist user message
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setRole("user");
        userMsg.setContent(message);
        session.addMessage(userMsg);

        // Build history from persisted messages (excluding the one we just added)
        List<Map<String, Object>> history = buildHistory(session);

        // Execute agent
        String response = agentExecutionService.executeChat(
                session.getSessionId(), userId, message, history, session.getDataSourceId());

        // Persist assistant message
        AiChatMessage assistantMsg = new AiChatMessage();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(response);
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
     * @param sessionId the session identifier
     * @param userId    the user identifier
     * @param message   the user's message
     * @param output    the output stream for SSE events
     */
    @Transactional
    public void sendMessageStream(String sessionId, String userId, String message,
                                  OutputStream output) {
        // For streaming, we delegate to the synchronous path and stream the result.
        // Full SSE streaming with ReAct requires deeper integration; this provides
        // a working baseline that sends the complete response as a single SSE event.
        try {
            String response = sendMessage(sessionId, userId, message);

            // Send the response as SSE
            String sseData = "data: " + escapeForSse(response) + "\n\n";
            output.write(sseData.getBytes(StandardCharsets.UTF_8));
            output.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (Exception e) {
            log.error("Streaming chat failed: {}", e.getMessage(), e);
            try {
                String errorSse = "data: {\"error\": \"" + escapeForSse(e.getMessage()) + "\"}\n\n";
                output.write(errorSse.getBytes(StandardCharsets.UTF_8));
                output.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                output.flush();
            } catch (Exception ignored) {
                // Output stream may already be closed
            }
        }
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
}
