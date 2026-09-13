package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FinanceDifferentialGridScorecardEvidenceProviderTest {

    private static final String SCORECARD_ID = "sprint33-finance-daily-scorecard";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(FinanceSummaryDualReconciliationRegistry.class,
                    () -> mock(FinanceSummaryDualReconciliationRegistry.class))
            .withBean(FinanceDifferentialGridRegistry.class,
                    () -> mock(FinanceDifferentialGridRegistry.class))
            .withBean(FinanceDifferentialGridService.class,
                    FinanceDifferentialGridService::new)
            .withBean(FinanceInvariantRegressionService.class,
                    () -> mock(FinanceInvariantRegressionService.class))
            .withUserConfiguration(FinanceDifferentialGridScorecardEvidenceProvider.class);

    @Test
    void isDisabledByDefaultSoScheduledPublisherDoesNotRunDifferentialGridWithoutOptIn() {
        contextRunner
                .withBean(FinanceDifferentialGridRowProvider.class,
                        () -> mock(FinanceDifferentialGridRowProvider.class))
                .run(context ->
                        assertThat(context).doesNotHaveBean(FinanceDifferentialGridScorecardEvidenceProvider.class));
    }

    @Test
    void staysDisabledWhenNoLiveRowProviderIsRegistered() {
        contextRunner
                .withPropertyValues("copilot.finance.reconciliation.differential-grid-evidence.enabled=true")
                .run(context ->
                        assertThat(context).doesNotHaveBean(FinanceDifferentialGridScorecardEvidenceProvider.class));
    }

    @Test
    void canBeEnabledExplicitlyWhenLiveRowProviderIsRegistered() {
        contextRunner
                .withBean(FinanceDifferentialGridRowProvider.class,
                        () -> mock(FinanceDifferentialGridRowProvider.class))
                .withPropertyValues("copilot.finance.reconciliation.differential-grid-evidence.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(FinanceDifferentialGridScorecardEvidenceProvider.class));
    }

    @Test
    void exposesDifferentialGridAsF4LaneWithoutFakingF1OrF2LiveEvidence() {
        FinanceDifferentialGridScorecardEvidenceProvider provider = provider(false);

        List<FinanceReconciliationScorecardService.CheckRun> runs = provider.currentRuns();

        assertThat(provider.authorityBindingId()).isEqualTo("month-settlement");
        assertThat(provider.oracleBindingId()).isEqualTo("month-settlement");
        assertThat(provider.scorecardId()).isEqualTo(SCORECARD_ID);
        assertThat(provider.baselineFailures()).isEmpty();
        assertThat(runs)
                .extracting(FinanceReconciliationScorecardService.CheckRun::category)
                .containsExactly(
                        "f1-detail",
                        "f2-summary-voucher",
                        "f3-invariants",
                        "f4-differential-grid",
                        "f4-differential-grid");
        assertPendingLiveEvidence(runs, "f1-detail");
        assertPendingLiveEvidence(runs, "f2-summary-voucher");

        List<FinanceReconciliationScorecardService.CheckRun> f4Runs = runs.stream()
                .filter(run -> "f4-differential-grid".equals(run.category()))
                .toList();
        assertThat(f4Runs)
                .extracting(FinanceReconciliationScorecardService.CheckRun::checkId)
                .containsExactly("month-settlement-representative-grid", "sale-account-representative-grid");
        assertThat(f4Runs).allSatisfy(run -> {
            assertThat(run.passed()).isTrue();
            assertThat(run.failedCells()).isZero();
            assertThat(run.failures()).isEmpty();
        });

        FinanceReconciliationScorecardService.CheckRun invariantRun = run(runs, "f3-invariants");
        assertThat(invariantRun.passed()).isTrue();
        assertThat(invariantRun.failedCells()).isZero();
    }

    @Test
    void mapsDifferentialGridMismatchIntoF4ScorecardFailure() {
        FinanceDifferentialGridScorecardEvidenceProvider provider = provider(true);

        List<FinanceReconciliationScorecardService.CheckRun> f4Runs = provider.currentRuns().stream()
                .filter(run -> "f4-differential-grid".equals(run.category()))
                .toList();

        assertThat(f4Runs).anySatisfy(run -> {
            assertThat(run.checkId()).isEqualTo("sale-account-representative-grid");
            assertThat(run.passed()).isFalse();
            assertThat(run.failedCells()).isEqualTo(1);
            assertThat(run.failures())
                    .singleElement()
                    .satisfies(failure -> {
                        assertThat(failure.status()).isEqualTo("amount mismatch");
                        assertThat(failure.difference()).isEqualByComparingTo("0.01");
                        assertThat(failure.message()).contains("sale-account-representative-grid");
                    });
        });
    }

    @Test
    void mapsRowProviderFailureIntoPendingLiveEvidenceInsteadOfThrowingOrFakingPass() {
        FinanceDifferentialGridScorecardEvidenceProvider provider = providerWithThrowingRowProvider();

        List<FinanceReconciliationScorecardService.CheckRun> f4Runs = provider.currentRuns().stream()
                .filter(run -> "f4-differential-grid".equals(run.category()))
                .toList();

        assertThat(f4Runs).anySatisfy(run -> {
            assertThat(run.checkId()).isEqualTo("month-settlement-representative-grid");
            assertThat(run.passed()).isFalse();
            assertThat(run.failedCells()).isEqualTo(1);
            assertThat(run.failures())
                    .singleElement()
                    .satisfies(failure -> {
                        assertThat(failure.status()).isEqualTo("PENDING_LIVE_EVIDENCE");
                        assertThat(failure.message()).contains("unsupported differential grid filter");
                    });
        });
    }

    private FinanceDifferentialGridScorecardEvidenceProvider provider(boolean mismatchSaleCase) {
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
        return new FinanceDifferentialGridScorecardEvidenceProvider(
                summaryRegistry,
                differentialGridRegistry,
                new FinanceDifferentialGridService(),
                rowProvider(mismatchSaleCase),
                invariantRegressionService);
    }

    private FinanceDifferentialGridScorecardEvidenceProvider providerWithThrowingRowProvider() {
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
        FinanceDifferentialGridRowProvider throwingProvider = new FinanceDifferentialGridRowProvider() {
            @Override
            public List<FinanceDifferentialGridService.GridRow> copilotRows(
                    FinanceDifferentialGridRegistry.DifferentialGridCase gridCase) {
                throw new IllegalArgumentException("unsupported differential grid filter: discountRate");
            }

            @Override
            public List<FinanceDifferentialGridService.GridRow> authorityRows(
                    FinanceDifferentialGridRegistry.DifferentialGridCase gridCase) {
                return List.of();
            }
        };
        return new FinanceDifferentialGridScorecardEvidenceProvider(
                summaryRegistry,
                differentialGridRegistry,
                new FinanceDifferentialGridService(),
                throwingProvider,
                invariantRegressionService);
    }

    private static FinanceDifferentialGridRowProvider rowProvider(boolean mismatchSaleCase) {
        return new FinanceDifferentialGridRowProvider() {
            @Override
            public List<FinanceDifferentialGridService.GridRow> copilotRows(
                    FinanceDifferentialGridRegistry.DifferentialGridCase gridCase) {
                return List.of(row(gridCase, copilotAmount(gridCase)));
            }

            @Override
            public List<FinanceDifferentialGridService.GridRow> authorityRows(
                    FinanceDifferentialGridRegistry.DifferentialGridCase gridCase) {
                return List.of(row(gridCase, authorityAmount(gridCase)));
            }

            private String copilotAmount(FinanceDifferentialGridRegistry.DifferentialGridCase gridCase) {
                if (mismatchSaleCase && "sale-account-representative-grid".equals(gridCase.id())) {
                    return "99.99";
                }
                return "100.00";
            }
        };
    }

    private static String authorityAmount(FinanceDifferentialGridRegistry.DifferentialGridCase gridCase) {
        return "sale-account-representative-grid".equals(gridCase.id()) ? "100.00" : "100.00";
    }

    private static FinanceDifferentialGridService.GridRow row(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase,
            String amount) {
        FinanceDifferentialGridRegistry.GridSlice slice = gridCase.slices().getFirst();
        return new FinanceDifferentialGridService.GridRow(
                slice.id(),
                gridCase.chain(),
                gridCase.metricId(),
                dimensions(gridCase, slice),
                new BigDecimal(amount));
    }

    private static Map<String, String> dimensions(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase,
            FinanceDifferentialGridRegistry.GridSlice slice) {
        return gridCase.dimensionKeys().stream()
                .collect(java.util.stream.Collectors.toMap(
                        key -> key,
                        key -> slice.filters().getOrDefault(key, ""),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new));
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
