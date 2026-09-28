package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceApplicationMysqlAuthorityProofServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadApplicationMysqlAuthoritySqlCasesWithoutWarehouseOrTrinoTables() {
        FinanceAuthorityRegistry authorityRegistry = new FinanceAuthorityRegistry(objectMapper);
        authorityRegistry.init();
        FinanceApplicationMysqlAuthorityRegistry registry =
                new FinanceApplicationMysqlAuthorityRegistry(objectMapper, authorityRegistry);
        registry.init();

        assertThat(registry.cases())
                .extracting(FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase::id)
                .containsExactly(
                        "month-settlement-discounted-receivable",
                        "sale-account-receivable",
                        "voucher-year-2026-count");
        FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase settlement =
                registry.caseById("month-settlement-discounted-receivable").orElseThrow();
        FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase sale =
                registry.caseById("sale-account-receivable").orElseThrow();
        FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase voucher =
                registry.caseById("voucher-year-2026-count").orElseThrow();

        assertThat(settlement.oracleBindingId()).isEqualTo("month-settlement");
        assertThat(settlement.chain()).isEqualTo("rent-settlement");
        assertThat(settlement.metricId()).isEqualTo("discounted-receivable");
        assertThat(settlement.dimensionKeys()).containsExactly("projectId", "accountPeriod");
        assertThat(settlement.applicationMysqlQuery().nativeSql())
                .contains("FROM a_month_accounting")
                .doesNotContain("public.ods_", "mysql.rs_cloud_flower", "jdbc:mysql", "password");
        assertThat(settlement.applicationMysqlQuery().nativeSql())
                .contains("m.settlement_year = 2026")
                .contains("CAST(SUBSTRING(m.year_and_month, 1, 4) AS UNSIGNED) = 2026")
                .doesNotContain("END LIKE '2026-%'");
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
                .contains("FROM f_voucher")
                .contains("v.account_priod REGEXP '^[0-9]{6}$'")
                .contains("CONCAT(SUBSTRING(v.account_priod, 1, 4), '-', SUBSTRING(v.account_priod, 5, 2))")
                .contains("v.code IS NOT NULL")
                .contains("LIKE '2026-%'")
                .doesNotContain("v.account_priod LIKE '2026-%'")
                .doesNotContain("JOIN f_voucher_item")
                .doesNotContain("public.ods_", "mysql.rs_cloud_flower", "jdbc:mysql", "password");
        assertThat(voucher.copilotQuery().nativeSql())
                .contains("public.xycyl_ads_finance_voucher_monthly")
                .doesNotContain("mysql.rs_cloud_flower");
    }

    @Test
    void shouldPreferAuthorityNamedApplicationMysqlProofCaseFieldsWhileKeepingLegacyAccessors() throws Exception {
        String json = """
                {
                  "id": "voucher-authority-case",
                  "authorityBindingId": "voucher-ledger",
                  "chain": "voucher-ledger",
                  "metricId": "voucher-count",
                  "metricName": "2026 会计凭证月度凭证数",
                  "dimensionKeys": ["accountPeriod"],
                  "copilotQuestion": "2026年凭证的数据统计下",
                  "copilotQuery": {
                    "kind": "warehouse-ads-sql",
                    "database": "prs.flowerbiz.federated",
                    "nativeSql": "select 1"
                  },
                  "applicationMysqlQuery": {
                    "kind": "application-mysql-sql",
                    "database": "rs_cloud_flower",
                    "nativeSql": "select 2"
                  },
                  "notes": "authority 字段为新契约，oracle 字段仅兼容旧资产"
                }
                """;

        FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase authorityCase =
                objectMapper.readValue(json, FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase.class);

        assertThat(authorityCase.authorityBindingId()).isEqualTo("voucher-ledger");
        assertThat(authorityCase.oracleBindingId()).isEqualTo("voucher-ledger");
        assertThat(authorityCase.applicationMysqlQuery().kind()).isEqualTo("application-mysql-sql");
    }

    @Test
    void shouldProveNl2SqlSummaryWithApplicationMysqlAuthorityRows() {
        FinanceAuthorityRegistry authorityRegistry = new FinanceAuthorityRegistry(objectMapper);
        authorityRegistry.init();
        FinanceApplicationMysqlAuthorityRegistry registry =
                new FinanceApplicationMysqlAuthorityRegistry(objectMapper, authorityRegistry);
        registry.init();
        FinanceApplicationMysqlAuthorityProofService service =
                new FinanceApplicationMysqlAuthorityProofService(new FinanceSummaryDualReconciliationService());
        FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase voucher =
                registry.caseById("voucher-year-2026-count").orElseThrow();
        RecordingQueryExecutor copilotExecutor = new RecordingQueryExecutor(List.of(
                row("voucher-ledger", "voucher-count", "2026-01", "31.00"),
                row("voucher-ledger", "voucher-count", "2026-02", "125.00")));
        RecordingQueryExecutor mysqlExecutor = new RecordingQueryExecutor(List.of(
                row("voucher-ledger", "voucher-count", "2026-01", "31.00"),
                row("voucher-ledger", "voucher-count", "2026-02", "125.00")));

        FinanceApplicationMysqlAuthorityProofService.ProofReport report =
                service.prove(voucher, copilotExecutor, mysqlExecutor);

        assertThat(report.passed()).isTrue();
        assertThat(report.failureMessage()).isEmpty();
        assertThat(report.authoritySource()).isEqualTo("APPLICATION_MYSQL");
        assertThat(report.caseId()).isEqualTo("voucher-year-2026-count");
        assertThat(report.reconciliation().diffs()).hasSize(2);
        assertThat(copilotExecutor.calls()).containsExactly(voucher.copilotQuery().nativeSql());
        assertThat(mysqlExecutor.calls()).containsExactly(voucher.applicationMysqlQuery().nativeSql());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExposeAuthoritySourceInProofReportWithLegacyOracleSourceAlias() {
        FinanceApplicationMysqlAuthorityProofService.ProofReport report =
                new FinanceApplicationMysqlAuthorityProofService.ProofReport(
                        "voucher-year-2026-count",
                        "APPLICATION_MYSQL",
                        true,
                        "",
                        new FinanceSummaryDualReconciliationService.SummaryReconciliationReport(true, List.of(), ""));

        Map<String, Object> serialized = objectMapper.convertValue(report, Map.class);

        assertThat(serialized)
                .containsEntry("authoritySource", "APPLICATION_MYSQL")
                .containsEntry("oracleSource", "APPLICATION_MYSQL");
    }

    @Test
    void shouldProveAllCoreApplicationMysqlAuthorityCases() {
        FinanceAuthorityRegistry authorityRegistry = new FinanceAuthorityRegistry(objectMapper);
        authorityRegistry.init();
        FinanceApplicationMysqlAuthorityRegistry registry =
                new FinanceApplicationMysqlAuthorityRegistry(objectMapper, authorityRegistry);
        registry.init();
        FinanceApplicationMysqlAuthorityProofService service =
                new FinanceApplicationMysqlAuthorityProofService(new FinanceSummaryDualReconciliationService());

        Map<String, List<Map<String, Object>>> fixtures = Map.of(
                "month-settlement-discounted-receivable",
                List.of(row("rent-settlement", "discounted-receivable", "1001", "2026-01", "8000.00")),
                "sale-account-receivable",
                List.of(row("sale-gift-bad-debt", "sale-receivable", "1002", "2026-02", "1200.50")),
                "voucher-year-2026-count",
                List.of(row("voucher-ledger", "voucher-count", "2026-01", "31.00")));

        for (FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase authorityCase : registry.cases()) {
            RecordingQueryExecutor copilotExecutor = new RecordingQueryExecutor(fixtures.get(authorityCase.id()));
            RecordingQueryExecutor mysqlExecutor = new RecordingQueryExecutor(fixtures.get(authorityCase.id()));

            FinanceApplicationMysqlAuthorityProofService.ProofReport report =
                    service.prove(authorityCase, copilotExecutor, mysqlExecutor);

            assertThat(report.passed()).as(authorityCase.id()).isTrue();
            assertThat(report.authoritySource()).isEqualTo("APPLICATION_MYSQL");
            assertThat(copilotExecutor.calls()).containsExactly(authorityCase.copilotQuery().nativeSql());
            assertThat(mysqlExecutor.calls()).containsExactly(authorityCase.applicationMysqlQuery().nativeSql());
        }
    }

    @Test
    void shouldFailWhenApplicationMysqlAuthorityDiffersFromNl2SqlResult() {
        FinanceAuthorityRegistry authorityRegistry = new FinanceAuthorityRegistry(objectMapper);
        authorityRegistry.init();
        FinanceApplicationMysqlAuthorityRegistry registry =
                new FinanceApplicationMysqlAuthorityRegistry(objectMapper, authorityRegistry);
        registry.init();
        FinanceApplicationMysqlAuthorityProofService service =
                new FinanceApplicationMysqlAuthorityProofService(new FinanceSummaryDualReconciliationService());
        FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase voucher =
                registry.caseById("voucher-year-2026-count").orElseThrow();

        FinanceApplicationMysqlAuthorityProofService.ProofReport report = service.prove(
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

    private static final class RecordingQueryExecutor implements FinanceApplicationMysqlAuthorityProofService.QueryExecutor {
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
