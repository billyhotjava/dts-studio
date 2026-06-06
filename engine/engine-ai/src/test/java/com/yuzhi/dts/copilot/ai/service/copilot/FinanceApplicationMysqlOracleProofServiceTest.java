package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceApplicationMysqlOracleProofServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadApplicationMysqlOracleSqlCasesWithoutWarehouseOrTrinoTables() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceApplicationMysqlOracleRegistry registry =
                new FinanceApplicationMysqlOracleRegistry(objectMapper, oracleRegistry);
        registry.init();

        assertThat(registry.cases())
                .extracting(FinanceApplicationMysqlOracleRegistry.OracleSqlCase::id)
                .containsExactly(
                        "month-settlement-discounted-receivable",
                        "sale-account-receivable",
                        "voucher-year-2026-count");
        FinanceApplicationMysqlOracleRegistry.OracleSqlCase settlement =
                registry.caseById("month-settlement-discounted-receivable").orElseThrow();
        FinanceApplicationMysqlOracleRegistry.OracleSqlCase sale =
                registry.caseById("sale-account-receivable").orElseThrow();
        FinanceApplicationMysqlOracleRegistry.OracleSqlCase voucher =
                registry.caseById("voucher-year-2026-count").orElseThrow();

        assertThat(settlement.oracleBindingId()).isEqualTo("month-settlement");
        assertThat(settlement.chain()).isEqualTo("rent-settlement");
        assertThat(settlement.metricId()).isEqualTo("discounted-receivable");
        assertThat(settlement.dimensionKeys()).containsExactly("projectId", "accountPeriod");
        assertThat(settlement.applicationMysqlQuery().nativeSql())
                .contains("FROM a_month_accounting")
                .doesNotContain("public.ods_", "mysql.rs_cloud_flower", "jdbc:mysql", "password");
        assertThat(settlement.copilotQuery().nativeSql())
                .contains("public.xycyl_ads_finance_month_settlement")
                .doesNotContain("mysql.rs_cloud_flower");
        assertThat(sale.oracleBindingId()).isEqualTo("sale-account");
        assertThat(sale.chain()).isEqualTo("sale-gift-bad-debt");
        assertThat(sale.metricId()).isEqualTo("sale-receivable");
        assertThat(sale.dimensionKeys()).containsExactly("projectId", "accountPeriod");
        assertThat(sale.applicationMysqlQuery().nativeSql())
                .contains("FROM a_sale_account", "JOIN t_flower_biz_info")
                .doesNotContain("public.ods_", "mysql.rs_cloud_flower", "jdbc:mysql", "password");
        assertThat(sale.copilotQuery().nativeSql())
                .contains("public.xycyl_ads_sale_account_summary")
                .doesNotContain("mysql.rs_cloud_flower");
        assertThat(voucher.oracleBindingId()).isEqualTo("voucher-ledger");
        assertThat(voucher.chain()).isEqualTo("voucher-ledger");
        assertThat(voucher.metricId()).isEqualTo("voucher-count");
        assertThat(voucher.dimensionKeys()).containsExactly("accountPeriod");
        assertThat(voucher.applicationMysqlQuery().kind()).isEqualTo("application-mysql-sql");
        assertThat(voucher.applicationMysqlQuery().database()).isEqualTo("rs_cloud_flower");
        assertThat(voucher.applicationMysqlQuery().nativeSql())
                .contains("FROM f_voucher", "JOIN f_voucher_item")
                .doesNotContain("public.ods_", "mysql.rs_cloud_flower", "jdbc:mysql", "password");
        assertThat(voucher.copilotQuery().nativeSql())
                .contains("public.xycyl_ads_finance_voucher_monthly")
                .doesNotContain("mysql.rs_cloud_flower");
    }

    @Test
    void shouldProveNl2SqlSummaryWithApplicationMysqlOracleRows() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceApplicationMysqlOracleRegistry registry =
                new FinanceApplicationMysqlOracleRegistry(objectMapper, oracleRegistry);
        registry.init();
        FinanceApplicationMysqlOracleProofService service =
                new FinanceApplicationMysqlOracleProofService(new FinanceSummaryDualReconciliationService());
        FinanceApplicationMysqlOracleRegistry.OracleSqlCase voucher =
                registry.caseById("voucher-year-2026-count").orElseThrow();
        RecordingQueryExecutor copilotExecutor = new RecordingQueryExecutor(List.of(
                row("voucher-ledger", "voucher-count", "2026-01", "31.00"),
                row("voucher-ledger", "voucher-count", "2026-02", "125.00")));
        RecordingQueryExecutor mysqlExecutor = new RecordingQueryExecutor(List.of(
                row("voucher-ledger", "voucher-count", "2026-01", "31.00"),
                row("voucher-ledger", "voucher-count", "2026-02", "125.00")));

        FinanceApplicationMysqlOracleProofService.ProofReport report =
                service.prove(voucher, copilotExecutor, mysqlExecutor);

        assertThat(report.passed()).isTrue();
        assertThat(report.failureMessage()).isEmpty();
        assertThat(report.oracleSource()).isEqualTo("APPLICATION_MYSQL");
        assertThat(report.caseId()).isEqualTo("voucher-year-2026-count");
        assertThat(report.reconciliation().diffs()).hasSize(2);
        assertThat(copilotExecutor.calls()).containsExactly(voucher.copilotQuery().nativeSql());
        assertThat(mysqlExecutor.calls()).containsExactly(voucher.applicationMysqlQuery().nativeSql());
    }

    @Test
    void shouldProveAllCoreApplicationMysqlOracleCases() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceApplicationMysqlOracleRegistry registry =
                new FinanceApplicationMysqlOracleRegistry(objectMapper, oracleRegistry);
        registry.init();
        FinanceApplicationMysqlOracleProofService service =
                new FinanceApplicationMysqlOracleProofService(new FinanceSummaryDualReconciliationService());

        Map<String, List<Map<String, Object>>> fixtures = Map.of(
                "month-settlement-discounted-receivable",
                List.of(row("rent-settlement", "discounted-receivable", "1001", "2026-01", "8000.00")),
                "sale-account-receivable",
                List.of(row("sale-gift-bad-debt", "sale-receivable", "1002", "2026-02", "1200.50")),
                "voucher-year-2026-count",
                List.of(row("voucher-ledger", "voucher-count", "2026-01", "31.00")));

        for (FinanceApplicationMysqlOracleRegistry.OracleSqlCase oracleCase : registry.cases()) {
            RecordingQueryExecutor copilotExecutor = new RecordingQueryExecutor(fixtures.get(oracleCase.id()));
            RecordingQueryExecutor mysqlExecutor = new RecordingQueryExecutor(fixtures.get(oracleCase.id()));

            FinanceApplicationMysqlOracleProofService.ProofReport report =
                    service.prove(oracleCase, copilotExecutor, mysqlExecutor);

            assertThat(report.passed()).as(oracleCase.id()).isTrue();
            assertThat(report.oracleSource()).isEqualTo("APPLICATION_MYSQL");
            assertThat(copilotExecutor.calls()).containsExactly(oracleCase.copilotQuery().nativeSql());
            assertThat(mysqlExecutor.calls()).containsExactly(oracleCase.applicationMysqlQuery().nativeSql());
        }
    }

    @Test
    void shouldFailWhenApplicationMysqlOracleDiffersFromNl2SqlResult() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceApplicationMysqlOracleRegistry registry =
                new FinanceApplicationMysqlOracleRegistry(objectMapper, oracleRegistry);
        registry.init();
        FinanceApplicationMysqlOracleProofService service =
                new FinanceApplicationMysqlOracleProofService(new FinanceSummaryDualReconciliationService());
        FinanceApplicationMysqlOracleRegistry.OracleSqlCase voucher =
                registry.caseById("voucher-year-2026-count").orElseThrow();

        FinanceApplicationMysqlOracleProofService.ProofReport report = service.prove(
                voucher,
                new RecordingQueryExecutor(List.of(row("voucher-ledger", "voucher-count", "2026-01", "30.00"))),
                new RecordingQueryExecutor(List.of(row("voucher-ledger", "voucher-count", "2026-01", "31.00"))));

        assertThat(report.passed()).isFalse();
        assertThat(report.failureMessage())
                .contains("voucher-year-2026-count", "voucher-count", "accountPeriod=2026-01", "difference=1.00");
    }

    private static Map<String, Object> row(String chain, String metricId, String accountPeriod, String amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("chain", chain);
        row.put("metricId", metricId);
        row.put("accountPeriod", accountPeriod);
        row.put("amount", new BigDecimal(amount));
        return row;
    }

    private static Map<String, Object> row(
            String chain,
            String metricId,
            String projectId,
            String accountPeriod,
            String amount) {
        Map<String, Object> row = row(chain, metricId, accountPeriod, amount);
        row.put("projectId", projectId);
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
            calls.add(nativeSql);
            return rows;
        }

        List<String> calls() {
            return calls;
        }
    }
}
