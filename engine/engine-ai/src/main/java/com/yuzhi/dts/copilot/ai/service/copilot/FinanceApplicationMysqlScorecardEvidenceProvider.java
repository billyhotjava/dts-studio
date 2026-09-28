package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "copilot.finance.reconciliation.application-mysql-evidence",
        name = "enabled",
        havingValue = "true")
public class FinanceApplicationMysqlScorecardEvidenceProvider implements FinanceReconciliationScorecardEvidenceProvider {

    static final String SCORECARD_ID = "sprint33-finance-daily-scorecard";
    static final String AUTHORITY_BINDING_ID = "month-settlement";
    private static final String PENDING_LIVE_EVIDENCE = "PENDING_LIVE_EVIDENCE";

    private final FinanceApplicationMysqlAuthorityProofRunner proofRunner;
    private final FinanceApplicationMysqlAuthorityScorecardCheckService proofCheckService;
    private final FinanceSummaryDualReconciliationRegistry summaryRegistry;
    private final FinanceDifferentialGridRegistry differentialGridRegistry;
    private final FinanceInvariantRegressionService invariantRegressionService;

    public FinanceApplicationMysqlScorecardEvidenceProvider(
            FinanceApplicationMysqlAuthorityProofRunner proofRunner,
            FinanceApplicationMysqlAuthorityScorecardCheckService proofCheckService,
            FinanceSummaryDualReconciliationRegistry summaryRegistry,
            FinanceDifferentialGridRegistry differentialGridRegistry,
            FinanceInvariantRegressionService invariantRegressionService) {
        this.proofRunner = proofRunner;
        this.proofCheckService = proofCheckService;
        this.summaryRegistry = summaryRegistry;
        this.differentialGridRegistry = differentialGridRegistry;
        this.invariantRegressionService = invariantRegressionService;
    }

    @Override
    public String authorityBindingId() {
        return AUTHORITY_BINDING_ID;
    }

    @Override
    public String scorecardId() {
        return SCORECARD_ID;
    }

    @Override
    public List<FinanceReconciliationScorecardService.CheckRun> currentRuns() {
        return List.of(
                pendingLiveEvidence(
                        "f1-detail",
                        "detail-harness-live-evidence",
                        "明细级对账 live 权威基准端点",
                        1,
                        "legacy adminapi/rs-gateway L2 endpoint is not configured"),
                proofCheckService.toCheckRun(proofRunner.proveAll()),
                invariantRegressionCheck(),
                pendingLiveEvidence(
                        "f4-differential-grid",
                        "representative-grid-live-evidence",
                        "代表性过滤网格 live 差分",
                        differentialGridCells(),
                        "live copilot dataset and authority endpoint rows are not configured"));
    }

    @Override
    public List<FinanceReconciliationScorecardService.ReconciliationFailure> baselineFailures() {
        return List.of();
    }

    private FinanceReconciliationScorecardService.CheckRun invariantRegressionCheck() {
        FinanceInvariantRegressionService.RegressionResult result = invariantRegressionService.runAll();
        List<FinanceReconciliationScorecardService.ReconciliationFailure> failures = result.failures().stream()
                .map(failure -> new FinanceReconciliationScorecardService.ReconciliationFailure(
                        "f3-invariants",
                        "invariant-regression-local",
                        failure.caseId(),
                        "INVARIANT_VIOLATION",
                        BigDecimal.ZERO,
                        failure.reproducibleFailure()))
                .toList();
        return new FinanceReconciliationScorecardService.CheckRun(
                "f3-invariants",
                "invariant-regression-local",
                "财务口径不变量本地回归网",
                result.allowed(),
                result.caseResults().size(),
                failures.size(),
                BigDecimal.ZERO,
                failures);
    }

    private int differentialGridCells() {
        int cells = differentialGridRegistry.cases().stream()
                .mapToInt(gridCase -> gridCase.slices().size())
                .sum();
        return Math.max(cells, 1);
    }

    private static FinanceReconciliationScorecardService.CheckRun pendingLiveEvidence(
            String category,
            String checkId,
            String checkName,
            int totalCells,
            String message) {
        FinanceReconciliationScorecardService.ReconciliationFailure failure =
                new FinanceReconciliationScorecardService.ReconciliationFailure(
                        category,
                        checkId,
                        "live-evidence",
                        PENDING_LIVE_EVIDENCE,
                        BigDecimal.ZERO,
                        message);
        List<FinanceReconciliationScorecardService.ReconciliationFailure> failures = new ArrayList<>();
        failures.add(failure);
        return new FinanceReconciliationScorecardService.CheckRun(
                category,
                checkId,
                checkName,
                false,
                Math.max(totalCells, 1),
                1,
                BigDecimal.ZERO,
                failures);
    }
}
