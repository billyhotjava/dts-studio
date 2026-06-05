package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceSummaryDualReconciliationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadSummaryCasesBoundToOracleRegistryAndSeparatedByChain() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceSummaryDualReconciliationRegistry registry =
                new FinanceSummaryDualReconciliationRegistry(objectMapper, oracleRegistry);
        registry.init();

        assertThat(registry.cases())
                .extracting(FinanceSummaryDualReconciliationRegistry.SummaryCase::id)
                .containsExactly("month-settlement-discounted-receivable", "sale-account-receivable");

        FinanceSummaryDualReconciliationRegistry.SummaryCase month =
                registry.caseById("month-settlement-discounted-receivable").orElseThrow();
        assertThat(month.oracleBindingId()).isEqualTo("month-settlement");
        assertThat(month.chain()).isEqualTo("rent-settlement");
        assertThat(month.metricId()).isEqualTo("discounted-receivable");
        assertThat(month.amountField()).isEqualTo("foldingAfterTotalAmount");
        assertThat(month.dimensionKeys()).containsExactly("projectId", "accountPeriod");
        assertThat(month.copilotQuery().database()).isEqualTo("prs.flowerbiz.federated");
        assertThat(month.copilotQuery().nativeSql())
                .contains("public.ods_ptr_mysql_a_month_accounting", "folding_after_total_amount");
        assertThat(month.oracleQuery().kind()).isEqualTo("golden-sql");
        assertThat(month.oracleQuery().nativeSql())
                .contains("public.ods_ptr_mysql_a_month_accounting", "folding_after_total_amount");

        FinanceSummaryDualReconciliationRegistry.SummaryCase sale =
                registry.caseById("sale-account-receivable").orElseThrow();
        assertThat(sale.oracleBindingId()).isEqualTo("sale-account");
        assertThat(sale.chain()).isEqualTo("sale-gift-bad-debt");
        assertThat(sale.metricId()).isEqualTo("sale-receivable");
        assertThat(sale.amountField()).isEqualTo("receivableAmount");
        assertThat(sale.dimensionKeys()).containsExactly("projectId", "accountPeriod");
        assertThat(sale.copilotQuery().nativeSql())
                .contains(
                        "public.ods_ptr_mysql_a_sale_account",
                        "public.ods_ptr_mysql_t_flower_biz_info",
                        "date_format(CAST(COALESCE(b.finish_time, b.apply_time) AS timestamp), '%Y%m')")
                .doesNotContain("DATE_FORMAT");
        assertThat(sale.oracleQuery().nativeSql())
                .contains(
                        "public.ods_ptr_mysql_a_sale_account",
                        "public.ods_ptr_mysql_t_flower_biz_info",
                        "date_format(CAST(COALESCE(b.finish_time, b.apply_time) AS timestamp), '%Y%m')")
                .doesNotContain("DATE_FORMAT");
    }

    @Test
    void shouldReconcileCopilotAndOracleSummaryCellsByMetricAndDimensions() {
        FinanceSummaryDualReconciliationService service = new FinanceSummaryDualReconciliationService();
        FinanceSummaryDualReconciliationService.SummarySpec spec =
                new FinanceSummaryDualReconciliationService.SummarySpec(
                        "month-settlement-discounted-receivable",
                        "rent-settlement",
                        "discounted-receivable",
                        List.of("projectId", "accountPeriod"));

        FinanceSummaryDualReconciliationService.SummaryReconciliationReport matched = service.reconcile(
                spec,
                List.of(row("rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")),
                List.of(row("rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")));

        assertThat(matched.passed()).isTrue();
        assertThat(matched.failureMessage()).isEmpty();
        assertThat(matched.diffs()).hasSize(1);
        assertThat(matched.diffs().getFirst().difference()).isEqualByComparingTo("0.00");

        FinanceSummaryDualReconciliationService.SummaryReconciliationReport mismatch = service.reconcile(
                spec,
                List.of(row("rent-settlement", "discounted-receivable", "1001", "202606", "1127.99")),
                List.of(row("rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")));

        assertThat(mismatch.passed()).isFalse();
        assertThat(mismatch.failureMessage())
                .contains("month-settlement-discounted-receivable", "discounted-receivable", "projectId=1001", "0.01");

        FinanceSummaryDualReconciliationService.SummaryReconciliationReport missingOracle = service.reconcile(
                spec,
                List.of(row("rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")),
                List.of());

        assertThat(missingOracle.passed()).isFalse();
        assertThat(missingOracle.failureMessage())
                .contains("missing oracle cell", "accountPeriod=202606", "copilot=1128.00", "oracle=0.00", "difference=1128.00");
    }

    @Test
    void shouldRejectMixedChainsMetricsAndDuplicateSummaryCells() {
        FinanceSummaryDualReconciliationService service = new FinanceSummaryDualReconciliationService();
        FinanceSummaryDualReconciliationService.SummarySpec spec =
                new FinanceSummaryDualReconciliationService.SummarySpec(
                        "sale-account-receivable",
                        "sale-gift-bad-debt",
                        "sale-receivable",
                        List.of("projectId", "accountPeriod"));

        FinanceSummaryDualReconciliationService.SummaryReconciliationReport wrongChain = service.reconcile(
                spec,
                List.of(row("rent-settlement", "sale-receivable", "1001", "202606", "3451.68")),
                List.of(row("sale-gift-bad-debt", "sale-receivable", "1001", "202606", "3451.68")));

        assertThat(wrongChain.passed()).isFalse();
        assertThat(wrongChain.failureMessage()).contains("chain mismatch", "expected=sale-gift-bad-debt", "actual=rent-settlement");

        FinanceSummaryDualReconciliationService.SummaryReconciliationReport wrongMetric = service.reconcile(
                spec,
                List.of(row("sale-gift-bad-debt", "discounted-receivable", "1001", "202606", "3451.68")),
                List.of(row("sale-gift-bad-debt", "sale-receivable", "1001", "202606", "3451.68")));

        assertThat(wrongMetric.passed()).isFalse();
        assertThat(wrongMetric.failureMessage()).contains("metric mismatch", "expected=sale-receivable", "actual=discounted-receivable");

        FinanceSummaryDualReconciliationService.SummaryReconciliationReport duplicate = service.reconcile(
                spec,
                List.of(
                        row("sale-gift-bad-debt", "sale-receivable", "1001", "202606", "10.00"),
                        row("sale-gift-bad-debt", "sale-receivable", "1001", "202606", "20.00")),
                List.of(row("sale-gift-bad-debt", "sale-receivable", "1001", "202606", "30.00")));

        assertThat(duplicate.passed()).isFalse();
        assertThat(duplicate.failureMessage()).contains("duplicate copilot summary cell", "projectId=1001");
    }

    private static FinanceSummaryDualReconciliationService.SummaryRow row(
            String chain,
            String metricId,
            String projectId,
            String accountPeriod,
            String amount) {
        return new FinanceSummaryDualReconciliationService.SummaryRow(
                chain,
                metricId,
                Map.of("projectId", projectId, "accountPeriod", accountPeriod),
                new BigDecimal(amount));
    }
}
