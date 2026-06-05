package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceDifferentialGridServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadRepresentativeGridCasesFromGovernanceAsset() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceSummaryDualReconciliationRegistry summaryRegistry =
                new FinanceSummaryDualReconciliationRegistry(objectMapper, oracleRegistry);
        summaryRegistry.init();
        FinanceDifferentialGridRegistry registry =
                new FinanceDifferentialGridRegistry(objectMapper, summaryRegistry);
        registry.init();

        assertThat(registry.cases())
                .extracting(FinanceDifferentialGridRegistry.DifferentialGridCase::id)
                .containsExactly("month-settlement-representative-grid", "sale-account-representative-grid");

        FinanceDifferentialGridRegistry.DifferentialGridCase month =
                registry.caseById("month-settlement-representative-grid").orElseThrow();
        assertThat(month.summaryCaseId()).isEqualTo("month-settlement-discounted-receivable");
        assertThat(month.chain()).isEqualTo("rent-settlement");
        assertThat(month.metricId()).isEqualTo("discounted-receivable");
        assertThat(month.dimensionKeys()).containsExactly("projectId", "accountPeriod");
        assertThat(month.slices())
                .extracting(FinanceDifferentialGridRegistry.GridSlice::id)
                .containsExactly(
                        "all-project-202606-month-boundary",
                        "single-project-cross-year",
                        "discount-one-sample",
                        "empty-project-period");
        assertThat(month.slices().getFirst().filters())
                .containsEntry("dateStart", "2026-06-01")
                .containsEntry("dateEndExclusive", "2026-07-01");

        FinanceDifferentialGridRegistry.DifferentialGridCase sale =
                registry.caseById("sale-account-representative-grid").orElseThrow();
        assertThat(sale.summaryCaseId()).isEqualTo("sale-account-receivable");
        assertThat(sale.chain()).isEqualTo("sale-gift-bad-debt");
        assertThat(sale.metricId()).isEqualTo("sale-receivable");
        assertThat(sale.slices())
                .extracting(FinanceDifferentialGridRegistry.GridSlice::boundary)
                .contains("cross-month", "empty-result");
    }

    @Test
    void shouldCompareCopilotAndOracleCellsAcrossRepresentativeSlices() {
        FinanceDifferentialGridService service = new FinanceDifferentialGridService();
        FinanceDifferentialGridService.GridSpec spec = spec();

        FinanceDifferentialGridService.GridReport matched = service.reconcile(
                spec,
                List.of(row("all-project-202606-month-boundary", "rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")),
                List.of(row("all-project-202606-month-boundary", "rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")));

        assertThat(matched.passed()).isTrue();
        assertThat(matched.failureMessage()).isEmpty();
        assertThat(matched.diffs()).hasSize(2);
        assertThat(matched.diffs())
                .extracting(FinanceDifferentialGridService.GridDiff::status)
                .containsExactly("matched", "empty matched");

        FinanceDifferentialGridService.GridReport mismatch = service.reconcile(
                spec,
                List.of(row("all-project-202606-month-boundary", "rent-settlement", "discounted-receivable", "1001", "202606", "1127.99")),
                List.of(row("all-project-202606-month-boundary", "rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")));

        assertThat(mismatch.passed()).isFalse();
        assertThat(mismatch.failureMessage())
                .contains(
                        "month-grid",
                        "all-project-202606-month-boundary",
                        "projectId=1001",
                        "accountPeriod=202606",
                        "difference=0.01");
    }

    @Test
    void shouldRejectWrongContractsMissingSidesAndDuplicateGridCells() {
        FinanceDifferentialGridService service = new FinanceDifferentialGridService();
        FinanceDifferentialGridService.GridSpec spec = spec();

        FinanceDifferentialGridService.GridReport wrongChain = service.reconcile(
                spec,
                List.of(row("all-project-202606-month-boundary", "sale-gift-bad-debt", "discounted-receivable", "1001", "202606", "1128.00")),
                List.of(row("all-project-202606-month-boundary", "rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")));

        assertThat(wrongChain.passed()).isFalse();
        assertThat(wrongChain.failureMessage()).contains("chain mismatch", "expected=rent-settlement", "actual=sale-gift-bad-debt");

        FinanceDifferentialGridService.GridReport unknownSlice = service.reconcile(
                spec,
                List.of(row("manual-unregistered-slice", "rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")),
                List.of(row("manual-unregistered-slice", "rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")));

        assertThat(unknownSlice.passed()).isFalse();
        assertThat(unknownSlice.failureMessage()).contains("unknown grid slice", "manual-unregistered-slice");

        FinanceDifferentialGridService.GridReport missingOracle = service.reconcile(
                spec,
                List.of(row("all-project-202606-month-boundary", "rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")),
                List.of());

        assertThat(missingOracle.passed()).isFalse();
        assertThat(missingOracle.failureMessage())
                .contains("missing oracle cell", "copilot=1128.00", "oracle=0.00");

        FinanceDifferentialGridService.GridReport duplicate = service.reconcile(
                spec,
                List.of(
                        row("all-project-202606-month-boundary", "rent-settlement", "discounted-receivable", "1001", "202606", "10.00"),
                        row("all-project-202606-month-boundary", "rent-settlement", "discounted-receivable", "1001", "202606", "20.00")),
                List.of(row("all-project-202606-month-boundary", "rent-settlement", "discounted-receivable", "1001", "202606", "30.00")));

        assertThat(duplicate.passed()).isFalse();
        assertThat(duplicate.failureMessage()).contains("duplicate copilot grid cell", "sliceId=all-project-202606-month-boundary");
    }

    private static FinanceDifferentialGridService.GridSpec spec() {
        return new FinanceDifferentialGridService.GridSpec(
                "month-grid",
                "rent-settlement",
                "discounted-receivable",
                List.of("projectId", "accountPeriod"),
                List.of(
                        new FinanceDifferentialGridService.GridSlice(
                                "all-project-202606-month-boundary",
                                Map.of("projectId", "ALL", "accountPeriod", "202606"),
                                "month-boundary"),
                        new FinanceDifferentialGridService.GridSlice(
                                "empty-project-period",
                                Map.of("projectId", "NO_DATA", "accountPeriod", "209912"),
                                "empty-result")));
    }

    private static FinanceDifferentialGridService.GridRow row(
            String sliceId,
            String chain,
            String metricId,
            String projectId,
            String accountPeriod,
            String amount) {
        return new FinanceDifferentialGridService.GridRow(
                sliceId,
                chain,
                metricId,
                Map.of("projectId", projectId, "accountPeriod", accountPeriod),
                new BigDecimal(amount));
    }
}
