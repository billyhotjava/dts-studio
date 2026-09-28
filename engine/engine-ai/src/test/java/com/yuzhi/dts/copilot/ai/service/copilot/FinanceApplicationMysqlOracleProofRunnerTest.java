package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FinanceApplicationMysqlOracleProofRunnerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReportDisabledWhenRuntimeExecutorsAreNotConfigured() {
        FinanceApplicationMysqlOracleProofRunner runner = new FinanceApplicationMysqlOracleProofRunner(
                registry(),
                new FinanceApplicationMysqlOracleProofService(new FinanceSummaryDualReconciliationService()),
                (FinanceApplicationMysqlOracleProofService.QueryExecutor) null,
                (FinanceApplicationMysqlOracleProofService.QueryExecutor) null);

        FinanceApplicationMysqlOracleProofRunner.RunResult result =
                runner.prove("voucher-year-2026-count");

        assertThat(result.status()).isEqualTo(FinanceApplicationMysqlOracleProofRunner.RunStatus.DISABLED);
        assertThat(result.message())
                .contains("Finance application MySQL authority proof")
                .contains("not configured")
                .doesNotContain("oracle proof");
        assertThat(result.reports()).isEmpty();
    }

    @Test
    void shouldStartAsSpringBeanWithOptionalRuntimeExecutors() {
        new ApplicationContextRunner()
                .withBean(FinanceApplicationMysqlAuthorityRegistry.class, this::registry)
                .withBean(
                        FinanceApplicationMysqlAuthorityProofService.class,
                        () -> new FinanceApplicationMysqlAuthorityProofService(
                                new FinanceSummaryDualReconciliationService()))
                .withBean(FinanceApplicationMysqlAuthorityProofRunner.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FinanceApplicationMysqlAuthorityProofRunner runner =
                            context.getBean(FinanceApplicationMysqlAuthorityProofRunner.class);

                    assertThat(runner.prove("voucher-year-2026-count").status())
                            .isEqualTo(FinanceApplicationMysqlAuthorityProofRunner.RunStatus.DISABLED);
                    assertThat(context).doesNotHaveBean(FinanceApplicationMysqlOracleProofRunner.class);
                });
    }

    @Test
    void shouldRunConfiguredProofCaseThroughCopilotAndApplicationMysqlExecutors() {
        RecordingQueryExecutor copilotExecutor = new RecordingQueryExecutor(List.of(
                row("voucher-ledger", "voucher-count", "2026-01", "31.00")));
        RecordingQueryExecutor mysqlExecutor = new RecordingQueryExecutor(List.of(
                row("voucher-ledger", "voucher-count", "2026-01", "31.00")));
        FinanceApplicationMysqlOracleProofRunner runner = new FinanceApplicationMysqlOracleProofRunner(
                registry(),
                new FinanceApplicationMysqlOracleProofService(new FinanceSummaryDualReconciliationService()),
                copilotExecutor,
                mysqlExecutor);

        FinanceApplicationMysqlOracleProofRunner.RunResult result =
                runner.prove("voucher-year-2026-count");

        assertThat(result.status()).isEqualTo(FinanceApplicationMysqlOracleProofRunner.RunStatus.PASSED);
        assertThat(result.reports()).singleElement().satisfies(report -> {
            assertThat(report.caseId()).isEqualTo("voucher-year-2026-count");
            assertThat(report.passed()).isTrue();
            assertThat(report.oracleSource()).isEqualTo(FinanceApplicationMysqlOracleProofService.ORACLE_SOURCE);
        });
        assertThat(copilotExecutor.calls()).hasSize(1);
        assertThat(copilotExecutor.calls().getFirst()).contains("public.xycyl_ads_finance_voucher_monthly");
        assertThat(mysqlExecutor.calls()).hasSize(1);
        assertThat(mysqlExecutor.calls().getFirst())
                .contains("FROM f_voucher", "v.code IS NOT NULL")
                .doesNotContain("JOIN f_voucher_item");
    }

    @Test
    void shouldReturnFailedWhenRuntimeExecutorThrows() {
        FinanceApplicationMysqlOracleProofRunner runner = new FinanceApplicationMysqlOracleProofRunner(
                registry(),
                new FinanceApplicationMysqlOracleProofService(new FinanceSummaryDualReconciliationService()),
                new FailingQueryExecutor("Access denied for user using password: NO"),
                new RecordingQueryExecutor(List.of()));

        FinanceApplicationMysqlOracleProofRunner.RunResult result =
                runner.prove("voucher-year-2026-count");

        assertThat(result.status()).isEqualTo(FinanceApplicationMysqlOracleProofRunner.RunStatus.FAILED);
        assertThat(result.message())
                .contains("Finance application MySQL authority proof failed")
                .doesNotContain("oracle proof");
        assertThat(result.message()).contains("Access denied");
        assertThat(result.reports()).isEmpty();
    }

    @Test
    void shouldRunAllCasesAndFailFastForMissingCaseId() {
        FinanceApplicationMysqlOracleProofRunner runner = new FinanceApplicationMysqlOracleProofRunner(
                registry(),
                new FinanceApplicationMysqlOracleProofService(new FinanceSummaryDualReconciliationService()),
                new RecordingQueryExecutor(List.of()),
                new RecordingQueryExecutor(List.of()));

        assertThat(runner.proveAll().reports())
                .extracting(FinanceApplicationMysqlOracleProofService.ProofReport::caseId)
                .containsExactly(
                        "month-settlement-discounted-receivable",
                        "sale-account-receivable",
                        "voucher-year-2026-count");
        assertThat(runner.prove("missing-case").status())
                .isEqualTo(FinanceApplicationMysqlOracleProofRunner.RunStatus.NOT_FOUND);
        assertThat(runner.prove("missing-case").message())
                .contains("Finance application MySQL authority proof case not found")
                .doesNotContain("oracle proof");
    }

    private FinanceApplicationMysqlAuthorityRegistry registry() {
        FinanceAuthorityRegistry oracleRegistry = new FinanceAuthorityRegistry(objectMapper);
        oracleRegistry.init();
        FinanceApplicationMysqlAuthorityRegistry registry =
                new FinanceApplicationMysqlAuthorityRegistry(objectMapper, oracleRegistry);
        registry.init();
        return registry;
    }

    private static Map<String, Object> row(String chain, String metricId, String accountPeriod, String amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("chain", chain);
        row.put("metricId", metricId);
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
            calls.add(nativeSql);
            return rows;
        }

        List<String> calls() {
            return calls;
        }
    }

    private static final class FailingQueryExecutor implements FinanceApplicationMysqlOracleProofService.QueryExecutor {
        private final String message;

        private FailingQueryExecutor(String message) {
            this.message = message;
        }

        @Override
        public List<Map<String, Object>> query(String database, String nativeSql) {
            throw new IllegalStateException(message);
        }
    }
}
