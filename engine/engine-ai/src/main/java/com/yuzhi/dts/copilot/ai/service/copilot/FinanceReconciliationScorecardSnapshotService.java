package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.FinanceReconciliationScorecardSnapshot;
import com.yuzhi.dts.copilot.ai.repository.FinanceReconciliationScorecardSnapshotRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceReconciliationScorecardSnapshotService implements FinanceReconciliationScorecardSource {

    private static final Logger log = LoggerFactory.getLogger(FinanceReconciliationScorecardSnapshotService.class);

    private final FinanceReconciliationScorecardSnapshotRepository repository;
    private final ObjectMapper objectMapper;

    public FinanceReconciliationScorecardSnapshotService(
            FinanceReconciliationScorecardSnapshotRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public FinanceReconciliationScorecardSnapshot publish(
            String oracleBindingId,
            String scorecardId,
            FinanceReconciliationScorecardService.ScorecardReport report) {
        if (!StringUtils.hasText(oracleBindingId) || !StringUtils.hasText(scorecardId) || report == null) {
            throw new IllegalArgumentException("oracleBindingId, scorecardId and report are required");
        }
        try {
            FinanceReconciliationScorecardSnapshot snapshot = new FinanceReconciliationScorecardSnapshot();
            snapshot.setOracleBindingId(oracleBindingId.trim());
            snapshot.setScorecardId(scorecardId.trim());
            snapshot.setHealthStatus(report.healthStatus());
            snapshot.setPassed(report.passed());
            snapshot.setDrifted(report.drifted());
            snapshot.setTotalChecks(report.totalChecks());
            snapshot.setPassedChecks(report.passedChecks());
            snapshot.setPassRate(report.passRate());
            snapshot.setMaxDifference(report.maxDifference());
            snapshot.setFailureMessage(report.failureMessage());
            snapshot.setReportJson(objectMapper.writeValueAsString(report));
            return repository.save(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish finance reconciliation scorecard snapshot", e);
        }
    }

    @Override
    public Optional<FinanceReconciliationScorecardService.ScorecardReport> latestScorecard(String oracleBindingId) {
        if (!StringUtils.hasText(oracleBindingId)) {
            return Optional.empty();
        }
        return repository.findFirstByOracleBindingIdOrderByCreatedAtDescIdDesc(oracleBindingId.trim())
                .flatMap(this::readReport);
    }

    private Optional<FinanceReconciliationScorecardService.ScorecardReport> readReport(
            FinanceReconciliationScorecardSnapshot snapshot) {
        if (snapshot == null || !StringUtils.hasText(snapshot.getReportJson())) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(
                    snapshot.getReportJson(),
                    FinanceReconciliationScorecardService.ScorecardReport.class));
        } catch (Exception e) {
            log.warn("Failed to parse finance scorecard snapshot id={}: {}", snapshot.getId(), e.getMessage());
            return Optional.empty();
        }
    }
}
