package com.yuzhi.dts.copilot.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * JPA entity for the nl2sql_query_template table.
 * Stores pre-built query templates with intent patterns
 * and parameterized SQL for common business questions.
 */
@Entity
@Table(name = "nl2sql_query_template")
public class Nl2SqlQueryTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "template_code", nullable = false, length = 64, unique = true)
    private String templateCode;

    @NotBlank
    @Column(name = "domain", nullable = false, length = 32)
    private String domain;

    @Column(name = "role_hint", length = 32)
    private String roleHint;

    @NotBlank
    @Column(name = "intent_patterns", nullable = false, columnDefinition = "TEXT")
    private String intentPatterns;

    @NotBlank
    @Column(name = "question_samples", nullable = false, columnDefinition = "TEXT")
    private String questionSamples;

    @NotBlank
    @Column(name = "sql_template", nullable = false, columnDefinition = "TEXT")
    private String sqlTemplate;

    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;

    @Column(name = "target_view", length = 128)
    private String targetView;

    @Column(name = "description", length = 256)
    private String description;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getRoleHint() {
        return roleHint;
    }

    public void setRoleHint(String roleHint) {
        this.roleHint = roleHint;
    }

    public String getIntentPatterns() {
        return intentPatterns;
    }

    public void setIntentPatterns(String intentPatterns) {
        this.intentPatterns = intentPatterns;
    }

    public String getQuestionSamples() {
        return questionSamples;
    }

    public void setQuestionSamples(String questionSamples) {
        this.questionSamples = questionSamples;
    }

    public String getSqlTemplate() {
        return sqlTemplate;
    }

    public void setSqlTemplate(String sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getTargetView() {
        return targetView;
    }

    public void setTargetView(String targetView) {
        this.targetView = targetView;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
