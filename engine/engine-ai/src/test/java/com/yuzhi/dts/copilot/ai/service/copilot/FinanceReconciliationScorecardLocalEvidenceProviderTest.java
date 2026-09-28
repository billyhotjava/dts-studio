package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FinanceReconciliationScorecardLocalEvidenceProviderTest {

    private static final String SCORECARD_ID = "sprint33-finance-daily-scorecard";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(FinanceSummaryDualReconciliationRegistry.class,
                    () -> mock(FinanceSummaryDualReconciliationRegistry.class))
            .withBean(FinanceDifferentialGridRegistry.class,
                    () -> mock(FinanceDifferentialGridRegistry.class))
            .withBean(FinanceInvariantRegressionService.class,
                    () -> mock(FinanceInvariantRegressionService.class))
            .withUserConfiguration(FinanceReconciliationScorecardLocalEvidenceProvider.class);

    @Test
    void isDisabledByDefaultSoScheduledPublisherDoesNotMistakeLocalEvidenceForLiveEvidence() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(FinanceReconciliationScorecardLocalEvidenceProvider.class));
    }

    @Test
    void canBeEnabledExplicitlyForManualLocalReadinessRuns() {
        contextRunner
                .withPropertyValues("copilot.finance.reconciliation.local-evidence.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(FinanceReconciliationScorecardLocalEvidenceProvider.class));
    }

    @Test
    void exposesAllRequiredScorecardLanesWithoutFakingMissingLiveEvidenceAsPass() {
        FinanceReconciliationScorecardLocalEvidenceProvider provider = provider();

        List<FinanceReconciliationScorecardService.CheckRun> runs = provider.currentRuns();

        assertThat(provider.authorityBindingId()).isEqualTo("month-settlement");
        assertThat(provider.oracleBindingId()).isEqualTo("month-settlement");
        assertThat(provider.scorecardId()).isEqualTo(SCORECARD_ID);
        assertThat(provider.baselineFailures()).isEmpty();
        assertThat(runs)
                .extracting(FinanceReconciliationScorecardService.CheckRun::category)
                .containsExactly("f1-detail", "f2-summary-voucher", "f3-invariants", "f4-differential-grid");

        assertPendingLiveEvidence(runs, "f1-detail");
        assertPendingLiveEvidence(runs, "f2-summary-voucher");
        assertPendingLiveEvidence(runs, "f4-differential-grid");

        FinanceReconciliationScorecardService.CheckRun invariantRun = run(runs, "f3-invariants");
        assertThat(invariantRun.passed()).isTrue();
        assertThat(invariantRun.totalCells()).isEqualTo(8);
        assertThat(invariantRun.failedCells()).isZero();
        assertThat(invariantRun.failures()).isEmpty();

        FinanceReconciliationScorecardRegistry scorecardRegistry =
                new FinanceReconciliationScorecardRegistry(objectMapper);
        scorecardRegistry.init();
        FinanceReconciliationScorecardService.ScorecardReport report =
                new FinanceReconciliationScorecardService().score(
                        scorecardRegistry.policy(SCORECARD_ID).orElseThrow().scorecardSpec(),
                        runs,
                        provider.baselineFailures());

        assertThat(report.passed()).isFalse();
        assertThat(report.healthStatus()).isEqualTo("DRIFT");
        assertThat(report.failureMessage()).contains("PENDING_LIVE_EVIDENCE");
    }

    private FinanceReconciliationScorecardLocalEvidenceProvider provider() {
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
        return new FinanceReconciliationScorecardLocalEvidenceProvider(
                summaryRegistry,
                differentialGridRegistry,
                invariantRegressionService);
    }

    private static void assertPendingLiveEvidence(
            List<FinanceReconciliationScorecardService.CheckRun> runs,
            String category) {
        FinanceReconciliationScorecardService.CheckRun run = run(runs, category);
        assertThat(run.passed()).isFalse();
        assertThat(run.failedCells()).isEqualTo(1);
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
