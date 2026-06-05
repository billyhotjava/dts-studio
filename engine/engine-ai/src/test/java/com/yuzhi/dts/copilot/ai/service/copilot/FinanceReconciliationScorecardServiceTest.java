package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceReconciliationScorecardServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadScorecardPolicyWithFourRequiredLanes() {
        FinanceReconciliationScorecardRegistry registry =
                new FinanceReconciliationScorecardRegistry(objectMapper);
        registry.init();

        FinanceReconciliationScorecardRegistry.ScorecardPolicy policy =
                registry.policy("sprint33-finance-daily-scorecard").orElseThrow();

        assertThat(policy.runMode()).isEqualTo("manual-or-scheduled");
        assertThat(policy.maxAllowedDifference()).isEqualByComparingTo("0.00");
        assertThat(policy.failOnNewDrift()).isTrue();
        assertThat(policy.categories())
                .extracting(FinanceReconciliationScorecardRegistry.CategoryPolicy::id)
                .containsExactly("f1-detail", "f2-summary-voucher", "f3-invariants", "f4-differential-grid");
        assertThat(policy.itScript()).isEqualTo("worklog/v1.0.0/sprint-33-202607/it/test_sprint33_reconciliation_scorecard.sh");
    }

    @Test
    void shouldBuildScorecardPassRatesAndRaiseDriftForNewDifferences() {
        FinanceReconciliationScorecardService service = new FinanceReconciliationScorecardService();
        FinanceReconciliationScorecardService.ScorecardSpec spec = spec();

        FinanceReconciliationScorecardService.ScorecardReport report = service.score(
                spec,
                List.of(
                        check("f1-detail", "detail-harness", true, 2, 0, "0.00", List.of()),
                        check("f2-summary-voucher", "summary-dual", true, 3, 0, "0.00", List.of()),
                        check(
                                "f2-summary-voucher",
                                "voucher-subject",
                                false,
                                1,
                                1,
                                "0.01",
                                List.of(failure("f2-summary-voucher", "voucher-subject", "JS2026060008/202606", "amount mismatch", "0.01"))),
                        check("f3-invariants", "invariant-regression", true, 8, 0, "0.00", List.of()),
                        check("f4-differential-grid", "representative-grid", true, 7, 0, "0.00", List.of())),
                List.of());

        assertThat(report.passed()).isFalse();
        assertThat(report.drifted()).isTrue();
        assertThat(report.healthStatus()).isEqualTo("DRIFT");
        assertThat(report.totalChecks()).isEqualTo(5);
        assertThat(report.passedChecks()).isEqualTo(4);
        assertThat(report.passRate()).isEqualByComparingTo("80.00");
        assertThat(report.maxDifference()).isEqualByComparingTo("0.01");
        assertThat(report.failureMessage())
                .contains("new drift", "f2-summary-voucher", "voucher-subject", "JS2026060008/202606", "difference=0.01");

        FinanceReconciliationScorecardService.CategoryScore f2 = report.categoryScores().stream()
                .filter(score -> "f2-summary-voucher".equals(score.category()))
                .findFirst()
                .orElseThrow();
        assertThat(f2.totalChecks()).isEqualTo(2);
        assertThat(f2.passedChecks()).isEqualTo(1);
        assertThat(f2.passRate()).isEqualByComparingTo("50.00");
        assertThat(f2.failedCells()).isEqualTo(1);
    }

    @Test
    void shouldKeepKnownBaselineFailureWithoutDriftAlert() {
        FinanceReconciliationScorecardService service = new FinanceReconciliationScorecardService();
        FinanceReconciliationScorecardService.ScorecardSpec spec = spec();
        FinanceReconciliationScorecardService.ReconciliationFailure known =
                failure("f4-differential-grid", "representative-grid", "sale-cross-month/projectId=1001", "amount mismatch", "0.01");

        FinanceReconciliationScorecardService.ScorecardReport report = service.score(
                spec,
                List.of(
                        check("f1-detail", "detail-harness", true, 2, 0, "0.00", List.of()),
                        check("f2-summary-voucher", "summary-dual", true, 3, 0, "0.00", List.of()),
                        check("f3-invariants", "invariant-regression", true, 8, 0, "0.00", List.of()),
                        check("f4-differential-grid", "representative-grid", false, 7, 1, "0.01", List.of(known))),
                List.of(known));

        assertThat(report.passed()).isFalse();
        assertThat(report.drifted()).isFalse();
        assertThat(report.healthStatus()).isEqualTo("FAIL");
        assertThat(report.failureMessage()).contains("known reconciliation failure", "representative-grid");
        assertThat(report.newDrifts()).isEmpty();
    }

    @Test
    void shouldRejectMissingRequiredCategoriesUnknownCategoriesAndDuplicateChecks() {
        FinanceReconciliationScorecardService service = new FinanceReconciliationScorecardService();
        FinanceReconciliationScorecardService.ScorecardSpec spec = spec();

        FinanceReconciliationScorecardService.ScorecardReport missingCategory = service.score(
                spec,
                List.of(
                        check("f1-detail", "detail-harness", true, 1, 0, "0.00", List.of()),
                        check("f2-summary-voucher", "summary-dual", true, 1, 0, "0.00", List.of()),
                        check("f3-invariants", "invariant-regression", true, 1, 0, "0.00", List.of())),
                List.of());

        assertThat(missingCategory.passed()).isFalse();
        assertThat(missingCategory.failureMessage()).contains("missing required category", "f4-differential-grid");

        FinanceReconciliationScorecardService.ScorecardReport unknownCategory = service.score(
                spec,
                List.of(
                        check("f1-detail", "detail-harness", true, 1, 0, "0.00", List.of()),
                        check("f2-summary-voucher", "summary-dual", true, 1, 0, "0.00", List.of()),
                        check("f3-invariants", "invariant-regression", true, 1, 0, "0.00", List.of()),
                        check("manual", "ad-hoc-check", true, 1, 0, "0.00", List.of()),
                        check("f4-differential-grid", "representative-grid", true, 1, 0, "0.00", List.of())),
                List.of());

        assertThat(unknownCategory.passed()).isFalse();
        assertThat(unknownCategory.failureMessage()).contains("unknown category", "manual");

        FinanceReconciliationScorecardService.ScorecardReport duplicate = service.score(
                spec,
                List.of(
                        check("f1-detail", "detail-harness", true, 1, 0, "0.00", List.of()),
                        check("f1-detail", "detail-harness", true, 1, 0, "0.00", List.of()),
                        check("f2-summary-voucher", "summary-dual", true, 1, 0, "0.00", List.of()),
                        check("f3-invariants", "invariant-regression", true, 1, 0, "0.00", List.of()),
                        check("f4-differential-grid", "representative-grid", true, 1, 0, "0.00", List.of())),
                List.of());

        assertThat(duplicate.passed()).isFalse();
        assertThat(duplicate.failureMessage()).contains("duplicate check", "f1-detail/detail-harness");
    }

    @Test
    void shouldFailWhenDifferenceExceedsPolicyThresholdEvenIfCheckClaimsPassed() {
        FinanceReconciliationScorecardService service = new FinanceReconciliationScorecardService();
        FinanceReconciliationScorecardService.ScorecardSpec spec = spec();

        FinanceReconciliationScorecardService.ScorecardReport report = service.score(
                spec,
                List.of(
                        check("f1-detail", "detail-harness", true, 2, 0, "0.00", List.of()),
                        check("f2-summary-voucher", "summary-dual", true, 3, 0, "0.00", List.of()),
                        check("f3-invariants", "invariant-regression", true, 8, 0, "0.00", List.of()),
                        check("f4-differential-grid", "representative-grid", true, 7, 0, "0.01", List.of())),
                List.of());

        assertThat(report.passed()).isFalse();
        assertThat(report.drifted()).isTrue();
        assertThat(report.passedChecks()).isEqualTo(3);
        assertThat(report.passRate()).isEqualByComparingTo("75.00");
        assertThat(report.failureMessage())
                .contains("difference threshold exceeded", "representative-grid", "difference=0.01");
    }

    @Test
    void shouldConvertDifferentialGridReportIntoScorecardCheck() {
        FinanceDifferentialGridService.GridReport gridReport = new FinanceDifferentialGridService.GridReport(
                false,
                List.of(
                        new FinanceDifferentialGridService.GridDiff(
                                new FinanceDifferentialGridService.GridKey(
                                        "all-project-202606-month-boundary",
                                        Map.of("projectId", "1001", "accountPeriod", "202606")),
                                "matched",
                                new BigDecimal("1128.00"),
                                new BigDecimal("1128.00"),
                                BigDecimal.ZERO),
                        new FinanceDifferentialGridService.GridDiff(
                                new FinanceDifferentialGridService.GridKey(
                                        "sale-cross-month",
                                        Map.of("projectId", "1001", "accountPeriod", "202606")),
                                "amount mismatch",
                                new BigDecimal("99.99"),
                                new BigDecimal("100.00"),
                                new BigDecimal("0.01"))),
                "sample mismatch");

        FinanceReconciliationScorecardService.CheckRun check =
                FinanceReconciliationScorecardService.CheckRun.fromDifferentialGrid(
                        "f4-differential-grid",
                        "representative-grid",
                        "代表性过滤网格",
                        gridReport);

        assertThat(check.passed()).isFalse();
        assertThat(check.totalCells()).isEqualTo(2);
        assertThat(check.failedCells()).isEqualTo(1);
        assertThat(check.maxDifference()).isEqualByComparingTo("0.01");
        assertThat(check.failures()).hasSize(1);
        assertThat(check.failures().getFirst().cellKey())
                .contains("sale-cross-month", "projectId=1001", "accountPeriod=202606");
    }

    private static FinanceReconciliationScorecardService.ScorecardSpec spec() {
        return new FinanceReconciliationScorecardService.ScorecardSpec(
                "sprint33-finance-daily-scorecard",
                new BigDecimal("0.00"),
                true,
                List.of("f1-detail", "f2-summary-voucher", "f3-invariants", "f4-differential-grid"));
    }

    private static FinanceReconciliationScorecardService.CheckRun check(
            String category,
            String checkId,
            boolean passed,
            int totalCells,
            int failedCells,
            String maxDifference,
            List<FinanceReconciliationScorecardService.ReconciliationFailure> failures) {
        return new FinanceReconciliationScorecardService.CheckRun(
                category,
                checkId,
                checkId,
                passed,
                totalCells,
                failedCells,
                new BigDecimal(maxDifference),
                failures);
    }

    private static FinanceReconciliationScorecardService.ReconciliationFailure failure(
            String category,
            String checkId,
            String cellKey,
            String status,
            String difference) {
        return new FinanceReconciliationScorecardService.ReconciliationFailure(
                category,
                checkId,
                cellKey,
                status,
                new BigDecimal(difference),
                status + " at " + cellKey);
    }
}
