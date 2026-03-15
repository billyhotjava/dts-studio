package com.yuzhi.dts.copilot.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

/**
 * JPA entity for the ai_audit_log table.
 * Records all AI-related actions for auditing and compliance.
 */
@Entity
@Table(name = "ai_audit_log")
public class AiAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 128)
    private String userId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "tool_name", length = 64)
    private String toolName;

    @Column(name = "input", columnDefinition = "TEXT")
    private String input;

    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "metadata", columnDefinition = "JSONB")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String metadata;

    @Column(name = "created_at")
    private Instant createdAt;

    public AiAuditLog() {
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
