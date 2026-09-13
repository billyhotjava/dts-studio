package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FinanceApplicationMysqlScorecardEvidenceProviderTest {

    private static final String SCORECARD_ID = "sprint33-finance-daily-scorecard";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(FinanceApplicationMysqlOracleProofRunner.class,
                    () -> mock(FinanceApplicationMysqlOracleProofRunner.class))
            .withBean(FinanceApplicationMysqlOracleScorecardCheckService.class,
                    FinanceApplicationMysqlOracleScorecardCheckService::new)
            .withBean(FinanceSummaryDualReconciliationRegistry.class,
                    () -> mock(FinanceSummaryDualReconciliationRegistry.class))
            .withBean(FinanceDifferentialGridRegistry.class,
                    () -> mock(FinanceDifferentialGridRegistry.class))
            .withBean(FinanceInvariantRegressionService.class,
                    () -> mock(FinanceInvariantRegressionService.class))
            .withUserConfiguration(FinanceApplicationMysqlScorecardEvidenceProvider.class);

    @Test
    void isDisabledByDefaultSoScheduledPublisherDoesNotRunLiveProofWithoutOptIn() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(FinanceApplicationMysqlScorecardEvidenceProvider.class));
    }

    @Test
    void canBeEnabledExplicitlyForApplicationMysqlProofEvidence() {
        contextRunner
                .withPropertyValues("copilot.finance.reconciliation.application-mysql-evidence.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(FinanceApplicationMysqlScorecardEvidenceProvider.class));
    }

    @Test
    void exposesApplicationMysqlProofAsF2LaneWithoutFakingOtherLiveLanes() {
        FinanceApplicationMysqlOracleProofRunner proofRunner = mock(FinanceApplicationMysqlOracleProofRunner.class);
        when(proofRunner.proveAll()).thenReturn(new FinanceApplicationMysqlOracleProofRunner.RunResult(
                "",
                FinanceApplicationMysqlOracleProofRunner.RunStatus.PASSED,
                "",
                List.of(
                        report("month-settlement-discounted-receivable"),
                        report("sale-account-receivable"),
                        report("voucher-year-2026-count"))));
        FinanceApplicationMysqlScorecardEvidenceProvider provider = provider(proofRunner);

        List<FinanceReconciliationScorecardService.CheckRun> runs = provider.currentRuns();

        assertThat(provider.authorityBindingId()).isEqualTo("month-settlement");
        assertThat(provider.oracleBindingId()).isEqualTo("month-settlement");
        assertThat(provider.scorecardId()).isEqualTo(SCORECARD_ID);
        assertThat(provider.baselineFailures()).isEmpty();
        assertThat(runs)
                .extracting(FinanceReconciliationScorecardService.CheckRun::category)
                .containsExactly("f1-detail", "f2-summary-voucher", "f3-invariants", "f4-differential-grid");
        assertPendingLiveEvidence(runs, "f1-detail");
        assertPendingLiveEvidence(runs, "f4-differential-grid");

        FinanceReconciliationScorecardService.CheckRun f2Run = run(runs, "f2-summary-voucher");
        assertThat(f2Run.passed()).isTrue();
        assertThat(f2Run.totalCells()).isEqualTo(3);
        assertThat(f2Run.failedCells()).isZero();
        assertThat(f2Run.failures()).isEmpty();

        FinanceReconciliationScorecardService.CheckRun invariantRun = run(runs, "f3-invariants");
        assertThat(invariantRun.passed()).isTrue();
        assertThat(invariantRun.failedCells()).isZero();
    }

    private FinanceApplicationMysqlScorecardEvidenceProvider provider(
            FinanceApplicationMysqlOracleProofRunner proofRunner) {
        ObjectMapper objectMapper = new ObjectMapper();
        FinanceAuthorityRegistry oracleRegistry = new FinanceAuthorityRegistry(objectMapper);
        oracleRegistry.init();
        FinanceSummaryDualReconciliationRegistry summaryRegistry =
                new FinanceSummaryDualReconciliationRegistry(objectMapper, oracleRegistry);
        summaryRegistry.init();
        FinanceDifferentialGridRegistry differentialGridRegistry =
                new FinanceDifferentialGridRegistry(objectMapper, summaryRegistry);
        differentialGridRegistry.init();
        CaliberRuleRegistry caliberRuleRegistry = new CaliberRuleRegistry(objectMapper);
        caliberRuleRegistry.init();
        FinanceInvariantRegistry invariantRegistry = new FinanceInvariantRegistry(objectMapper, caliberRuleRegistry);
        invariantRegistry.init();
        FinanceInvariantRegressionService invariantRegressionService =
                new FinanceInvariantRegressionService(objectMapper, invariantRegistry);
        invariantRegressionService.init();
        return new FinanceApplicationMysqlScorecardEvidenceProvider(
                proofRunner,
                new FinanceApplicationMysqlOracleScorecardCheckService(),
                summaryRegistry,
                differentialGridRegistry,
                invariantRegressionService);
    }

    private static FinanceApplicationMysqlOracleProofService.ProofReport report(String caseId) {
        FinanceSummaryDualReconciliationService.SummaryKey key =
                new FinanceSummaryDualReconciliationService.SummaryKey(
                        "voucher-count",
                        Map.of("accountPeriod", "2026-06", "caseId", caseId));
        FinanceSummaryDualReconciliationService.SummaryDiff diff =
                new FinanceSummaryDualReconciliationService.SummaryDiff(
                        key,
                        "matched",
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO);
        FinanceSummaryDualReconciliationService.SummaryReconciliationReport reconciliation =
                new FinanceSummaryDualReconciliationService.SummaryReconciliationReport(
                        true,
                        List.of(diff),
                        "");
        return new FinanceApplicationMysqlOracleProofService.ProofReport(
                caseId,
                FinanceApplicationMysqlOracleProofService.ORACLE_SOURCE,
                true,
                "",
                reconciliation);
    }

    private static void assertPendingLiveEvidence(
            List<FinanceReconciliationScorecardService.CheckRun> runs,
            String category) {
        FinanceReconciliationScorecardService.CheckRun run = run(runs, category);
        assertThat(run.passed()).isFalse();
        assertThat(run.failures())
                .singleElement()
                .extracting(FinanceReconciliationScorecardService.ReconciliationFailure::status)
                .isEqualTo("PENDING_LIVE_EVIDENCE");
    }

    private static FinanceReconciliationScorecardService.CheckRun run(
            List<FinanceReconciliationScorecardService.CheckRun> runs,
            String category) {
        return runs.stream()
                .filter(candidate -> category.equals(candidate.category()))
                .findFirst()
                .orElseThrow();
    }
}
