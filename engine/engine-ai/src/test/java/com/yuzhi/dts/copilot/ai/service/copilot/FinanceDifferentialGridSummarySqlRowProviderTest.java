package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FinanceDifferentialGridSummarySqlRowProviderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(FinanceSummaryDualReconciliationRegistry.class,
                    () -> summaryRegistry())
            .withBean(
                    "financeApplicationMysqlAuthorityCopilotJdbcQueryExecutor",
                    FinanceApplicationMysqlOracleProofService.QueryExecutor.class,
                    () -> new RecordingQueryExecutor(List.of()))
            .withBean(
                    "financeApplicationMysqlAuthorityJdbcQueryExecutor",
                    FinanceApplicationMysqlOracleProofService.QueryExecutor.class,
                    () -> new RecordingQueryExecutor(List.of()))
            .withUserConfiguration(FinanceDifferentialGridSummarySqlRowProvider.class);

    @Test
    void isDisabledByDefaultSoItCannotAccidentallyBecomeLiveEvidence() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(FinanceDifferentialGridSummarySqlRowProvider.class));
    }

    @Test
    void canBeEnabledOnlyWhenExplicitlyConfigured() {
        contextRunner
                .withPropertyValues("copilot.finance.reconciliation.differential-grid-summary-sql-row-provider.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(FinanceDifferentialGridSummarySqlRowProvider.class);
                    assertThat(context).hasSingleBean(FinanceDifferentialGridRowProvider.class);
                });
    }

    @Test
    void mapsRegisteredSummarySqlRowsIntoDifferentialGridRowsForSupportedSlices() {
        RecordingQueryExecutor copilotExecutor = new RecordingQueryExecutor(List.of(
                row("1001", "202606", "10.00"),
                row("1001", "202607", "20.00"),
                row("1002", "202606", "30.00")));
        RecordingQueryExecutor authorityExecutor = new RecordingQueryExecutor(List.of(
                row("1001", "202606", "10.00")));
        FinanceDifferentialGridSummarySqlRowProvider provider = new FinanceDifferentialGridSummarySqlRowProvider(
                summaryRegistry(),
                copilotExecutor,
                authorityExecutor);

        List<FinanceDifferentialGridService.GridRow> copilotRows = provider.copilotRows(supportedGridCase());
        List<FinanceDifferentialGridService.GridRow> authorityRows = provider.authorityRows(supportedGridCase());

        assertThat(copilotExecutor.calls()).containsExactly("copilot-db|select copilot");
        assertThat(authorityExecutor.calls()).containsExactly("authority-db|select authority");
        assertThat(copilotRows)
                .extracting(row -> row.sliceId() + "|" + row.dimensions().get("projectId")
                        + "|" + row.dimensions().get("accountPeriod") + "|" + row.amount())
                .containsExactly(
                        "project-1001-period-202606|1001|202606|10.00",
                        "all-project-period-202606|1001|202606|10.00",
                        "all-project-period-202606|1002|202606|30.00",
                        "project-1001-period-range|1001|202606|10.00",
                        "project-1001-period-range|1001|202607|20.00");
        assertThat(authorityRows)
                .extracting(row -> row.sliceId() + "|" + row.dimensions().get("projectId")
                        + "|" + row.dimensions().get("accountPeriod") + "|" + row.amount())
                .containsExactly(
                        "project-1001-period-202606|1001|202606|10.00",
                        "all-project-period-202606|1001|202606|10.00",
                        "project-1001-period-range|1001|202606|10.00");
    }

    @Test
    void supportsDateRangeFiltersByNormalizingThemToAccountPeriodRanges() {
        RecordingQueryExecutor executor = new RecordingQueryExecutor(List.of(
                row("1001", "202606", "10.00"),
                row("1001", "202607", "20.00"),
                row("1001", "202608", "30.00")));
        FinanceDifferentialGridSummarySqlRowProvider provider = new FinanceDifferentialGridSummarySqlRowProvider(
                summaryRegistry(),
                executor,
                executor);

        List<FinanceDifferentialGridService.GridRow> rows = provider.copilotRows(dateRangeGridCase());

        assertThat(rows)
                .extracting(row -> row.sliceId() + "|" + row.dimensions().get("accountPeriod"))
                .containsExactly("date-range|202606", "date-range|202607");
    }

    @Test
    void rejectsUnsupportedFiltersInsteadOfSilentlyFakingEvidence() {
        RecordingQueryExecutor executor = new RecordingQueryExecutor(List.of(row("1001", "202606", "10.00")));
        FinanceDifferentialGridSummarySqlRowProvider provider = new FinanceDifferentialGridSummarySqlRowProvider(
                summaryRegistry(),
                executor,
                executor);

        assertThatThrownBy(() -> provider.copilotRows(unsupportedFilterGridCase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported differential grid filter")
                .hasMessageContaining("discountRate");
    }

    private static FinanceSummaryDualReconciliationRegistry summaryRegistry() {
        FinanceSummaryDualReconciliationRegistry registry = mock(FinanceSummaryDualReconciliationRegistry.class);
        when(registry.caseById("month-summary")).thenReturn(Optional.of(summaryCase()));
        return registry;
    }

    private static FinanceSummaryDualReconciliationRegistry.SummaryCase summaryCase() {
        return new FinanceSummaryDualReconciliationRegistry.SummaryCase(
                "month-summary",
                "month-settlement",
                "rent-settlement",
                "discounted-receivable",
                "月对账折后实收汇总",
                "foldingAfterTotalAmount",
                List.of("projectId", "accountPeriod"),
                "按项目和账期汇总月对账折后实收金额",
                new FinanceSummaryDualReconciliationRegistry.SummaryQuery(
                        "native-sql",
                        "copilot-db",
                        "select copilot",
                        ""),
                new FinanceSummaryDualReconciliationRegistry.SummaryQuery(
                        "golden-sql",
                        "authority-db",
                        "select authority",
                        ""),
                "");
    }

    private static FinanceDifferentialGridRegistry.DifferentialGridCase supportedGridCase() {
        return gridCase(List.of(
                slice("project-1001-period-202606", Map.of("projectId", "1001", "accountPeriod", "202606")),
                slice("all-project-period-202606", Map.of("projectId", "ALL", "accountPeriod", "202606")),
                slice("project-1001-period-range", Map.of(
                        "projectId", "1001",
                        "accountPeriodStart", "202606",
                        "accountPeriodEnd", "202607")),
                slice("empty-slice", Map.of("projectId", "NO_DATA", "accountPeriod", "209912"))));
    }

    private static FinanceDifferentialGridRegistry.DifferentialGridCase dateRangeGridCase() {
        return gridCase(List.of(slice("date-range", Map.of(
                "projectId", "1001",
                "dateStart", "2026-06-15",
                "dateEndExclusive", "2026-08-01"))));
    }

    private static FinanceDifferentialGridRegistry.DifferentialGridCase unsupportedFilterGridCase() {
        return gridCase(List.of(slice("discount-one", Map.of(
                "projectId", "1001",
                "accountPeriod", "202606",
                "discountRate", "1"))));
    }

    private static FinanceDifferentialGridRegistry.DifferentialGridCase gridCase(
            List<FinanceDifferentialGridRegistry.GridSlice> slices) {
        return new FinanceDifferentialGridRegistry.DifferentialGridCase(
                "month-grid",
                "month-summary",
                "month-settlement",
                "rent-settlement",
                "discounted-receivable",
                "月对账折后实收代表性过滤网格",
                List.of("projectId", "accountPeriod"),
                slices,
                "按代表性过滤网格对比月对账折后实收金额",
                "GET /rs-flowers-base/finance/monthAccounting/listData",
                "");
    }

    private static FinanceDifferentialGridRegistry.GridSlice slice(String id, Map<String, String> filters) {
        return new FinanceDifferentialGridRegistry.GridSlice(id, filters, "test", "");
    }

    private static Map<String, Object> row(String projectId, String accountPeriod, String amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("projectId", projectId);
        row.put("accountPeriod", accountPeriod);
        row.put("amount", new BigDecimal(amount));
        return row;
    }

    private static final class RecordingQueryExecutor implements FinanceApplicationMysqlOracleProofService.QueryExecutor {

        private final List<Map<String, Object>> rows;
        private final List<String> calls = new ArrayList<>();

        private RecordingQueryExecutor(List<Map<String, Object>> rows) {
            this.rows = rows;
        }

        @Override
        public List<Map<String, Object>> query(String database, String nativeSql) {
            calls.add(database + "|" + nativeSql);
            return rows;
        }

        private List<String> calls() {
            return calls;
        }
    }
}
