package com.yuzhi.dts.copilot.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "finance_weak_path_reconciliation_candidate_snapshot")
public class FinanceWeakPathReconciliationCandidateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, length = 128)
    private String runId;

    @Column(name = "policy_id", nullable = false, length = 128)
    private String policyId;

    @Column(name = "candidate_key", nullable = false, length = 512)
    private String candidateKey;

    @Column(name = "final_tier", length = 64)
    private String finalTier;

    @Column(name = "domain", length = 64)
    private String domain;

    @Column(name = "response_kind", length = 64)
    private String responseKind;

    @Column(name = "target", length = 512)
    private String target;

    @Column(name = "data_surface", length = 256)
    private String dataSurface;

    @Column(name = "source_count")
    private Long sourceCount;

    @Column(name = "priority_score")
    private Integer priorityScore;

    @Column(name = "semantic_draft_action", length = 64)
    private String semanticDraftAction;

    @Column(name = "semantic_draft_id", length = 128)
    private String semanticDraftId;

    @Column(name = "semantic_draft_status", length = 64)
    private String semanticDraftStatus;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "question_samples_json", nullable = false, columnDefinition = "JSONB")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String questionSamplesJson;

    @Column(name = "reconciliation_sets_json", nullable = false, columnDefinition = "JSONB")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String reconciliationSetsJson;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getCandidateKey() {
        return candidateKey;
    }

    public void setCandidateKey(String candidateKey) {
        this.candidateKey = candidateKey;
    }

    public String getFinalTier() {
        return finalTier;
    }

    public void setFinalTier(String finalTier) {
        this.finalTier = finalTier;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getResponseKind() {
        return responseKind;
    }

    public void setResponseKind(String responseKind) {
        this.responseKind = responseKind;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDataSurface() {
        return dataSurface;
    }

    public void setDataSurface(String dataSurface) {
        this.dataSurface = dataSurface;
    }

    public Long getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(Long sourceCount) {
        this.sourceCount = sourceCount;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getSemanticDraftAction() {
        return semanticDraftAction;
    }

    public void setSemanticDraftAction(String semanticDraftAction) {
        this.semanticDraftAction = semanticDraftAction;
    }

    public String getSemanticDraftId() {
        return semanticDraftId;
    }

    public void setSemanticDraftId(String semanticDraftId) {
        this.semanticDraftId = semanticDraftId;
    }

    public String getSemanticDraftStatus() {
        return semanticDraftStatus;
    }

    public void setSemanticDraftStatus(String semanticDraftStatus) {
        this.semanticDraftStatus = semanticDraftStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getQuestionSamplesJson() {
        return questionSamplesJson;
    }

    public void setQuestionSamplesJson(String questionSamplesJson) {
        this.questionSamplesJson = questionSamplesJson;
    }

    public String getReconciliationSetsJson() {
        return reconciliationSetsJson;
    }

    public void setReconciliationSetsJson(String reconciliationSetsJson) {
        this.reconciliationSetsJson = reconciliationSetsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
