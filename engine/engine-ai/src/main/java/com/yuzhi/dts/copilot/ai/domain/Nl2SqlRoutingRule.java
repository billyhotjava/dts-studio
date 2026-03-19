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
 * JPA entity for the nl2sql_routing_rule table.
 * Stores keyword-based routing rules that map user questions
 * to the appropriate database views for NL2SQL generation.
 */
@Entity
@Table(name = "nl2sql_routing_rule")
public class Nl2SqlRoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "domain", nullable = false, length = 32)
    private String domain;

    @NotBlank
    @Column(name = "keywords", nullable = false, columnDefinition = "TEXT")
    private String keywords;

    @NotBlank
    @Column(name = "primary_view", nullable = false, length = 128)
    private String primaryView;

    @Column(name = "secondary_views", columnDefinition = "TEXT")
    private String secondaryViews;

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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getPrimaryView() {
        return primaryView;
    }

    public void setPrimaryView(String primaryView) {
        this.primaryView = primaryView;
    }

    public String getSecondaryViews() {
        return secondaryViews;
    }

    public void setSecondaryViews(String secondaryViews) {
        this.secondaryViews = secondaryViews;
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
