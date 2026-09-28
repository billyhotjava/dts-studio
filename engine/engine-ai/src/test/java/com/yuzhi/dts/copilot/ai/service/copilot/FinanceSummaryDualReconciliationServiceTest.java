package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class FinanceSummaryDualReconciliationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadSummaryCasesBoundToOracleRegistryAndSeparatedByChain() {
        FinanceAuthorityRegistry oracleRegistry = new FinanceAuthorityRegistry(objectMapper);
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
    void shouldPreferAuthorityNamedSummaryCaseFieldsWhileKeepingLegacyAccessors() throws Exception {
        String json = """
                {
                  "id": "month-authority-summary",
                  "authorityBindingId": "month-settlement",
                  "chain": "rent-settlement",
                  "metricId": "discounted-receivable",
                  "metricName": "月对账折后实收汇总",
                  "amountField": "foldingAfterTotalAmount",
                  "dimensionKeys": ["projectId", "accountPeriod"],
                  "copilotQuestion": "按项目和账期汇总月对账折后实收金额",
                  "copilotQuery": {
                    "kind": "native-sql",
                    "database": "prs.flowerbiz.federated",
                    "nativeSql": "select 1"
                  },
                  "authorityQuery": {
                    "kind": "golden-sql",
                    "database": "prs.flowerbiz.federated",
                    "nativeSql": "select 2"
                  },
                  "notes": "authority 字段为新契约，oracle 字段仅兼容旧资产"
                }
                """;

        FinanceSummaryDualReconciliationRegistry.SummaryCase summaryCase =
                objectMapper.readValue(json, FinanceSummaryDualReconciliationRegistry.SummaryCase.class);

        assertThat(summaryCase.authorityBindingId()).isEqualTo("month-settlement");
        assertThat(summaryCase.oracleBindingId()).isEqualTo("month-settlement");
        assertThat(summaryCase.authorityQuery().kind()).isEqualTo("golden-sql");
        assertThat(summaryCase.oracleQuery().nativeSql()).isEqualTo("select 2");
    }

    @Test
    void shouldLogAuthorityBindingWhenSummaryCaseBindingIsMissing() {
        FinanceAuthorityRegistry oracleRegistry = mock(FinanceAuthorityRegistry.class);
        when(oracleRegistry.binding("month-settlement")).thenReturn(Optional.empty());
        when(oracleRegistry.binding("sale-account")).thenReturn(Optional.empty());
        Logger logger = (Logger) LoggerFactory.getLogger(FinanceSummaryDualReconciliationRegistry.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            FinanceSummaryDualReconciliationRegistry registry =
                    new FinanceSummaryDualReconciliationRegistry(objectMapper, oracleRegistry);
            registry.init();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> assertThat(message)
                        .contains("Missing finance authority binding: month-settlement")
                        .doesNotContain("Missing finance oracle binding"));
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
                .contains("missing authority cell", "accountPeriod=202606", "copilot=1128.00", "authority=0.00", "difference=1128.00");
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

    @Test
    void shouldReportAuthorityTerminologyInSummaryFailures() {
        FinanceSummaryDualReconciliationService service = new FinanceSummaryDualReconciliationService();
        FinanceSummaryDualReconciliationService.SummarySpec spec =
                new FinanceSummaryDualReconciliationService.SummarySpec(
                        "month-settlement-discounted-receivable",
                        "rent-settlement",
                        "discounted-receivable",
                        List.of("projectId", "accountPeriod"));

        FinanceSummaryDualReconciliationService.SummaryReconciliationReport mismatch = service.reconcile(
                spec,
                List.of(row("rent-settlement", "discounted-receivable", "1001", "202606", "1127.99")),
                List.of(row("rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")));

        assertThat(mismatch.failureMessage())
                .contains("authority=1128.00")
                .doesNotContain("oracle=1128.00");

        FinanceSummaryDualReconciliationService.SummaryReconciliationReport missingAuthority = service.reconcile(
                spec,
                List.of(row("rent-settlement", "discounted-receivable", "1001", "202606", "1128.00")),
                List.of());

        assertThat(missingAuthority.failureMessage())
                .contains("missing authority cell", "authority=0.00")
                .doesNotContain("missing oracle cell", "oracle=0.00");
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
