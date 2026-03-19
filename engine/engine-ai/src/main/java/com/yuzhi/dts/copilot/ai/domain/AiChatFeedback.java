package com.yuzhi.dts.copilot.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * JPA entity for the ai_chat_feedback table.
 * Stores user feedback (thumbs up/down) on AI-generated responses.
 */
@Entity
@Table(name = "ai_chat_feedback")
public class AiChatFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @NotBlank
    @Column(name = "message_id", nullable = false, length = 64)
    private String messageId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "user_name", length = 128)
    private String userName;

    @NotBlank
    @Column(name = "rating", nullable = false, length = 8)
    private String rating;

    @Column(name = "reason", length = 32)
    private String reason;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "generated_sql", columnDefinition = "TEXT")
    private String generatedSql;

    @Column(name = "corrected_sql", columnDefinition = "TEXT")
    private String correctedSql;

    @Column(name = "routed_domain", length = 32)
    private String routedDomain;

    @Column(name = "target_view", length = 128)
    private String targetView;

    @Column(name = "template_code", length = 64)
    private String templateCode;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getGeneratedSql() {
        return generatedSql;
    }

    public void setGeneratedSql(String generatedSql) {
        this.generatedSql = generatedSql;
    }

    public String getCorrectedSql() {
        return correctedSql;
    }

    public void setCorrectedSql(String correctedSql) {
        this.correctedSql = correctedSql;
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
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
