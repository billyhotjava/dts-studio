package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceApplicationMysqlOracleScorecardCheckServiceTest {

    @Test
    void convertsPassedApplicationMysqlProofIntoF2ScorecardCheck() {
        FinanceApplicationMysqlOracleScorecardCheckService service =
                new FinanceApplicationMysqlOracleScorecardCheckService();
        FinanceApplicationMysqlOracleProofRunner.RunResult runResult =
                new FinanceApplicationMysqlOracleProofRunner.RunResult(
                        "",
                        FinanceApplicationMysqlOracleProofRunner.RunStatus.PASSED,
                        "",
                        List.of(
                                report("month-settlement-discounted-receivable", true, "matched", "0.00"),
                                report("voucher-year-2026-count", true, "matched", "0.00")));

        FinanceReconciliationScorecardService.CheckRun check = service.toCheckRun(runResult);

        assertThat(check.category()).isEqualTo("f2-summary-voucher");
        assertThat(check.checkId()).isEqualTo("application-mysql-authority-proof");
        assertThat(check.passed()).isTrue();
        assertThat(check.totalCells()).isEqualTo(2);
        assertThat(check.failedCells()).isZero();
        assertThat(check.maxDifference()).isEqualByComparingTo("0.00");
        assertThat(check.failures()).isEmpty();
    }

    @Test
    void convertsDisabledProofIntoPendingLiveEvidenceFailureWithoutFakingPass() {
        FinanceApplicationMysqlOracleScorecardCheckService service =
                new FinanceApplicationMysqlOracleScorecardCheckService();
        FinanceApplicationMysqlOracleProofRunner.RunResult disabled =
                new FinanceApplicationMysqlOracleProofRunner.RunResult(
                        "",
                        FinanceApplicationMysqlOracleProofRunner.RunStatus.DISABLED,
                        "Finance application MySQL authority proof executors are not configured",
                        List.of());

        FinanceReconciliationScorecardService.CheckRun check = service.toCheckRun(disabled);

        assertThat(check.category()).isEqualTo("f2-summary-voucher");
        assertThat(check.passed()).isFalse();
        assertThat(check.totalCells()).isEqualTo(1);
        assertThat(check.failedCells()).isEqualTo(1);
        assertThat(check.maxDifference()).isEqualByComparingTo("0.00");
        assertThat(check.failures()).singleElement().satisfies(failure -> {
            assertThat(failure.cellKey()).isEqualTo("application-mysql-authority-proof");
            assertThat(failure.status()).isEqualTo("PENDING_LIVE_EVIDENCE");
            assertThat(failure.message())
                    .contains("Finance application MySQL authority proof")
                    .contains("executors are not configured")
                    .doesNotContain("oracle proof");
        });
    }

    @Test
    void convertsMissingProofIntoAuthorityPendingEvidenceMessage() {
        FinanceApplicationMysqlOracleScorecardCheckService service =
                new FinanceApplicationMysqlOracleScorecardCheckService();

        FinanceReconciliationScorecardService.CheckRun check = service.toCheckRun(null);

        assertThat(check.passed()).isFalse();
        assertThat(check.failures()).singleElement().satisfies(failure -> {
            assertThat(failure.status()).isEqualTo("PENDING_LIVE_EVIDENCE");
            assertThat(failure.message())
                    .contains("Finance application MySQL authority proof result is missing")
                    .doesNotContain("oracle proof");
        });
    }

    @Test
    void preservesApplicationMysqlProofDifferencesAsScorecardFailures() {
        FinanceApplicationMysqlOracleScorecardCheckService service =
                new FinanceApplicationMysqlOracleScorecardCheckService();
        FinanceApplicationMysqlOracleProofRunner.RunResult failed =
                new FinanceApplicationMysqlOracleProofRunner.RunResult(
                        "",
                        FinanceApplicationMysqlOracleProofRunner.RunStatus.FAILED,
                        "month settlement mismatch",
                        List.of(report(
                                "month-settlement-discounted-receivable",
                                false,
                                "amount mismatch",
                                "0.01")));

        FinanceReconciliationScorecardService.CheckRun check = service.toCheckRun(failed);

        assertThat(check.passed()).isFalse();
        assertThat(check.totalCells()).isEqualTo(1);
        assertThat(check.failedCells()).isEqualTo(1);
        assertThat(check.maxDifference()).isEqualByComparingTo("0.01");
        assertThat(check.failures()).singleElement().satisfies(failure -> {
            assertThat(failure.cellKey())
                    .contains("month-settlement-discounted-receivable", "accountPeriod=2026-06");
            assertThat(failure.status()).isEqualTo("amount mismatch");
            assertThat(failure.difference()).isEqualByComparingTo("0.01");
        });
    }

    private static FinanceApplicationMysqlOracleProofService.ProofReport report(
            String caseId,
            boolean passed,
            String status,
            String difference) {
        FinanceSummaryDualReconciliationService.SummaryKey key =
                new FinanceSummaryDualReconciliationService.SummaryKey(
                        "discounted-receivable",
                        Map.of("projectId", "1001", "accountPeriod", "2026-06"));
        FinanceSummaryDualReconciliationService.SummaryDiff diff =
                new FinanceSummaryDualReconciliationService.SummaryDiff(
                        key,
                        status,
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00").add(new BigDecimal(difference)),
                        new BigDecimal(difference));
        FinanceSummaryDualReconciliationService.SummaryReconciliationReport reconciliation =
                new FinanceSummaryDualReconciliationService.SummaryReconciliationReport(
                        passed,
                        List.of(diff),
                        passed ? "" : "summary mismatch");
        return new FinanceApplicationMysqlOracleProofService.ProofReport(
                caseId,
                FinanceApplicationMysqlOracleProofService.ORACLE_SOURCE,
                passed,
                passed ? "" : "summary mismatch",
                reconciliation);
    }
}
