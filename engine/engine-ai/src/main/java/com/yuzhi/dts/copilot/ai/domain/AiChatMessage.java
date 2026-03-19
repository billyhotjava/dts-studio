package com.yuzhi.dts.copilot.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

/**
 * JPA entity for the ai_chat_message table.
 * Represents a single message in a chat session.
 */
@Entity
@Table(name = "ai_chat_message")
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", referencedColumnName = "id", nullable = false)
    private AiChatSession session;

    @NotBlank
    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tool_calls", columnDefinition = "JSONB")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String toolCalls;

    @Column(name = "tool_call_id", length = 128)
    private String toolCallId;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "generated_sql", columnDefinition = "TEXT")
    private String generatedSql;

    @Column(name = "routed_domain", length = 32)
    private String routedDomain;

    @Column(name = "target_view", length = 128)
    private String targetView;

    @Column(name = "template_code", length = 64)
    private String templateCode;

    @Column(name = "created_at")
    private Instant createdAt;

    public AiChatMessage() {
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AiChatSession getSession() {
        return session;
    }

    public void setSession(AiChatSession session) {
        this.session = session;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(String toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public String getGeneratedSql() {
        return generatedSql;
    }

    public void setGeneratedSql(String generatedSql) {
        this.generatedSql = generatedSql;
    }

    public String getRoutedDomain() {
        return routedDomain;
    }

    public void setRoutedDomain(String routedDomain) {
        this.routedDomain = routedDomain;
    }

    public String getTargetView() {
        return targetView;
    }

    public void setTargetView(String targetView) {
        this.targetView = targetView;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
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
