package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "copilot.finance.reconciliation.differential-grid-evidence",
        name = "enabled",
        havingValue = "true")
@ConditionalOnBean(FinanceDifferentialGridRowProvider.class)
public class FinanceDifferentialGridScorecardEvidenceProvider implements FinanceReconciliationScorecardEvidenceProvider {

    static final String SCORECARD_ID = "sprint33-finance-daily-scorecard";
    static final String AUTHORITY_BINDING_ID = "month-settlement";
    private static final String PENDING_LIVE_EVIDENCE = "PENDING_LIVE_EVIDENCE";

    private final FinanceSummaryDualReconciliationRegistry summaryRegistry;
    private final FinanceDifferentialGridRegistry differentialGridRegistry;
    private final FinanceDifferentialGridService differentialGridService;
    private final FinanceDifferentialGridRowProvider rowProvider;
    private final FinanceInvariantRegressionService invariantRegressionService;

    public FinanceDifferentialGridScorecardEvidenceProvider(
            FinanceSummaryDualReconciliationRegistry summaryRegistry,
            FinanceDifferentialGridRegistry differentialGridRegistry,
            FinanceDifferentialGridService differentialGridService,
            FinanceDifferentialGridRowProvider rowProvider,
            FinanceInvariantRegressionService invariantRegressionService) {
        this.summaryRegistry = summaryRegistry;
        this.differentialGridRegistry = differentialGridRegistry;
        this.differentialGridService = differentialGridService;
        this.rowProvider = rowProvider;
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
        List<FinanceReconciliationScorecardService.CheckRun> runs = new ArrayList<>();
        runs.add(pendingLiveEvidence(
                "f1-detail",
                "detail-harness-live-evidence",
                "明细级对账 live 权威基准端点",
                1,
                "legacy adminapi/rs-gateway L2 endpoint is not configured"));
        runs.add(pendingLiveEvidence(
                "f2-summary-voucher",
                "summary-dual-live-evidence",
                "汇总双路与凭证 tie-out live 证据",
                Math.max(summaryRegistry.cases().size(), 1),
                "live L2/signoff SQL and voucher subject tree are not signed"));
        runs.add(invariantRegressionCheck());
        List<FinanceDifferentialGridRegistry.DifferentialGridCase> gridCases = differentialGridRegistry.cases();
        if (gridCases.isEmpty()) {
            runs.add(pendingLiveEvidence(
                    "f4-differential-grid",
                    "representative-grid-live-evidence",
                    "代表性过滤网格 live 差分",
                    1,
                    "differential grid registry is empty"));
        } else {
            for (FinanceDifferentialGridRegistry.DifferentialGridCase gridCase : gridCases) {
                try {
                    FinanceDifferentialGridService.GridReport report = differentialGridService.reconcile(
                            gridCase.gridSpec(),
                            rowProvider.copilotRows(gridCase),
                            rowProvider.authorityRows(gridCase));
                    runs.add(FinanceReconciliationScorecardService.CheckRun.fromDifferentialGrid(
                            "f4-differential-grid",
                            gridCase.id(),
                            gridCase.metricName(),
                            report));
                } catch (RuntimeException e) {
                    runs.add(pendingLiveEvidence(
                            "f4-differential-grid",
                            gridCase.id(),
                            gridCase.metricName(),
                            Math.max(gridCase.slices().size(), 1),
                            e.getMessage()));
                }
            }
        }
        return List.copyOf(runs);
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
        return new FinanceReconciliationScorecardService.CheckRun(
                category,
                checkId,
                checkName,
                false,
                Math.max(totalCells, 1),
                1,
                BigDecimal.ZERO,
                List.of(failure));
    }
}
