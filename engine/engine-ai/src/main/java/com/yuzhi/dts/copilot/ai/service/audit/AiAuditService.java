package com.yuzhi.dts.copilot.ai.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.AiAuditLog;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for recording AI audit log entries.
 * Uses a separate transaction to ensure audit records persist even if the outer transaction fails.
 */
@Service
public class AiAuditService {

    private static final Logger log = LoggerFactory.getLogger(AiAuditService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Log a tool execution event.
     *
     * @param userId       the user who triggered the execution
     * @param sessionId    the chat session identifier
     * @param toolName     the tool that was executed
     * @param input        the tool input (arguments as JSON)
     * @param success      whether the execution succeeded
     * @param errorMessage error message if failed, {@code null} otherwise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logToolExecution(String userId, String sessionId,
                                 String toolName, String input,
                                 boolean success, String errorMessage) {
        try {
            AiAuditLog entry = new AiAuditLog();
            entry.setUserId(userId);
            entry.setSessionId(sessionId);
            entry.setAction("TOOL_EXECUTION");
            entry.setToolName(toolName);
            entry.setInput(truncate(input, 4000));
            entry.setSuccess(success);
            entry.setErrorMessage(truncate(errorMessage, 2000));
            entityManager.persist(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    /**
     * Log a chat interaction event.
     *
     * @param userId    the user identifier
     * @param sessionId the chat session identifier
     * @param action    the action type (e.g. "CHAT_MESSAGE", "SESSION_CREATE")
     * @param input     the user input
     * @param output    the agent output
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logChatAction(String userId, String sessionId,
                              String action, String input, String output) {
        try {
            AiAuditLog entry = new AiAuditLog();
            entry.setUserId(userId);
            entry.setSessionId(sessionId);
            entry.setAction(action);
            entry.setInput(truncate(input, 4000));
            entry.setOutput(truncate(output, 4000));
            entry.setSuccess(true);
            entityManager.persist(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    /**
     * Log a business action draft attempt.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActionExecution(ActionAuditEvent event) {
        if (event == null) {
            return;
        }
        try {
            AiAuditLog entry = new AiAuditLog();
            entry.setUserId(event.userId());
            entry.setSessionId(event.sessionId());
            entry.setAction("ACTION_EXECUTION");
            entry.setToolName(event.actionName());
            entry.setInput(truncate(event.input(), 4000));
            entry.setOutput(truncate(event.result(), 4000));
            entry.setSuccess(event.success());
            entry.setErrorMessage(truncate(event.errorMessage(), 2000));
            entry.setMetadata(truncate(buildActionMetadata(event), 4000));
            entityManager.persist(entry);
        } catch (Exception e) {
            log.error("Failed to write action audit log: {}", e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String buildActionMetadata(ActionAuditEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actionName", event.actionName());
        metadata.put("objectName", event.objectName());
        metadata.put("guard", event.guard());
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public record ActionAuditEvent(
            String userId,
            String sessionId,
            String actionName,
            String objectName,
            String guard,
            String input,
            String result,
            boolean success,
            String errorMessage) {
    }
}
