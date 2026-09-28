package com.yuzhi.dts.copilot.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "finance_reconciliation_scorecard_snapshot")
public class FinanceReconciliationScorecardSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oracle_binding_id", nullable = false, length = 128)
    private String oracleBindingId;

    @Column(name = "scorecard_id", nullable = false, length = 128)
    private String scorecardId;

    @Column(name = "health_status", nullable = false, length = 32)
    private String healthStatus;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "drifted")
    private Boolean drifted;

    @Column(name = "total_checks")
    private Integer totalChecks;

    @Column(name = "passed_checks")
    private Integer passedChecks;

    @Column(name = "pass_rate", precision = 8, scale = 2)
    private BigDecimal passRate;

    @Column(name = "max_difference", precision = 18, scale = 2)
    private BigDecimal maxDifference;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "report_json", nullable = false, columnDefinition = "JSONB")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String reportJson;

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

    public String getOracleBindingId() {
        return oracleBindingId;
    }

    public void setOracleBindingId(String oracleBindingId) {
        this.oracleBindingId = oracleBindingId;
    }

    public String getScorecardId() {
        return scorecardId;
    }

    public void setScorecardId(String scorecardId) {
        this.scorecardId = scorecardId;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Boolean getDrifted() {
        return drifted;
    }

    public void setDrifted(Boolean drifted) {
        this.drifted = drifted;
    }

    public Integer getTotalChecks() {
        return totalChecks;
    }

    public void setTotalChecks(Integer totalChecks) {
        this.totalChecks = totalChecks;
    }

    public Integer getPassedChecks() {
        return passedChecks;
    }

    public void setPassedChecks(Integer passedChecks) {
        this.passedChecks = passedChecks;
    }

    public BigDecimal getPassRate() {
        return passRate;
    }

    public void setPassRate(BigDecimal passRate) {
        this.passRate = passRate;
    }

    public BigDecimal getMaxDifference() {
        return maxDifference;
    }

    public void setMaxDifference(BigDecimal maxDifference) {
        this.maxDifference = maxDifference;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getReportJson() {
        return reportJson;
    }

    public void setReportJson(String reportJson) {
        this.reportJson = reportJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
