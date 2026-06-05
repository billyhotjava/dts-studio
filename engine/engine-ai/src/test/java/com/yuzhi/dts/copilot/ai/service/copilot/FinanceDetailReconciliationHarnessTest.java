package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceDetailReconciliationHarnessTest {

    private static final String FEDERATED_DATABASE = "prs.flowerbiz.federated";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadSamplesAlignedWithRegisteredL2OracleEndpoints() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();

        FinanceDetailReconciliationSampleRegistry sampleRegistry =
                new FinanceDetailReconciliationSampleRegistry(objectMapper, oracleRegistry);
        sampleRegistry.init();

        assertThat(sampleRegistry.samples())
                .extracting(FinanceDetailReconciliationSampleRegistry.DetailSample::id)
                .containsExactly("month-settlement-js2026060008", "sale-account-bx202606030968");

        FinanceDetailReconciliationSampleRegistry.DetailSample month =
                sampleRegistry.sample("month-settlement-js2026060008").orElseThrow();
        assertThat(month.oracleBindingId()).isEqualTo("month-settlement");
        assertThat(month.chain()).isEqualTo("rent-settlement");
        assertThat(month.oracleEndpoint()).isEqualTo("POST /rs-flowers-base/operate/monthAccount/getMonthSettlementData");
        assertThat(month.businessKey()).isEqualTo("结算2026060008");
        assertThat(month.projectId()).isEqualTo("1001");
        assertThat(month.accountPeriod()).isEqualTo("202606");
        assertThat(month.amountFields())
                .containsExactly("receivableTotalAmount", "netReceiptTotalAmount", "foldingAfterTotalAmount", "totalAmount");
        assertThat(month.copilotRequest()).containsEntry("database", FEDERATED_DATABASE);
        assertThat(month.copilotRequest().get("nativeSql"))
                .contains(
                        "public.ods_ptr_mysql_a_month_accounting",
                        "\"businessKey\"",
                        "\"receivableTotalAmount\"",
                        "\"netReceiptTotalAmount\"",
                        "\"foldingAfterTotalAmount\"",
                        "\"totalAmount\"");

        FinanceDetailReconciliationSampleRegistry.DetailSample sale =
                sampleRegistry.sample("sale-account-bx202606030968").orElseThrow();
        assertThat(sale.oracleBindingId()).isEqualTo("sale-account");
        assertThat(sale.chain()).isEqualTo("sale-gift-bad-debt");
        assertThat(sale.oracleEndpoint()).isEqualTo("GET /rs-flowers-base/operate/saleAccount/listSaleAccountPage");
        assertThat(sale.amountFields()).containsExactly("receivableAmount", "netReceiptsAmount", "bizAmount");
        assertThat(sale.copilotRequest()).containsEntry("database", FEDERATED_DATABASE);
        assertThat(sale.copilotRequest().get("nativeSql"))
                .contains(
                        "public.ods_ptr_mysql_a_sale_account",
                        "public.ods_ptr_mysql_t_flower_biz_info",
                        "\"businessKey\"",
                        "\"receivableAmount\"",
                        "\"netReceiptsAmount\"",
                        "\"bizAmount\"");
    }

    @Test
    void shouldRunOracleAndCopilotFetchersForTheSameSampleThenCompareRows() {
        FinanceDetailReconciliationHarness harness = new FinanceDetailReconciliationHarness(
                initializedSampleRegistry(),
                new FinanceDetailReconciliationService());
        RecordingDetailSourceClient client = new RecordingDetailSourceClient();
        client.oracleRows = List.of(monthRow("1128.00", "1128.00", "1128.00", "1128.00"));
        client.copilotRows = List.of(monthRow("1128.00", "1128.00", "1128.00", "1128.00"));

        FinanceDetailReconciliationHarness.HarnessReport report =
                harness.run("month-settlement-js2026060008", client);

        assertThat(report.passed()).isTrue();
        assertThat(report.failureMessage()).isEmpty();
        assertThat(client.oracleRequests)
                .containsExactly("month-settlement-js2026060008|POST /rs-flowers-base/operate/monthAccount/getMonthSettlementData");
        assertThat(client.copilotRequests)
                .containsExactly("month-settlement-js2026060008|查询 202606 账期 1001 项目 结算2026060008 的月对账三级金额明细");
        assertThat(report.reconciliation().diffs()).hasSize(1);

        client.copilotRows = List.of(monthRow("1128.00", "1128.00", "1127.99", "1128.00"));
        FinanceDetailReconciliationHarness.HarnessReport mismatch =
                harness.run("month-settlement-js2026060008", client);

        assertThat(mismatch.passed()).isFalse();
        assertThat(mismatch.failureMessage())
                .contains("month-settlement-js2026060008", "foldingAfterTotalAmount", "0.01");
    }

    private FinanceDetailReconciliationSampleRegistry initializedSampleRegistry() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceDetailReconciliationSampleRegistry sampleRegistry =
                new FinanceDetailReconciliationSampleRegistry(objectMapper, oracleRegistry);
        sampleRegistry.init();
        return sampleRegistry;
    }

    private static FinanceDetailReconciliationService.DetailRow monthRow(
            String receivableTotalAmount,
            String netReceiptTotalAmount,
            String foldingAfterTotalAmount,
            String totalAmount) {
        return new FinanceDetailReconciliationService.DetailRow(
                "rent-settlement",
                "结算2026060008",
                "1001",
                "202606",
                Map.of(
                        "receivableTotalAmount", new BigDecimal(receivableTotalAmount),
                        "netReceiptTotalAmount", new BigDecimal(netReceiptTotalAmount),
                        "foldingAfterTotalAmount", new BigDecimal(foldingAfterTotalAmount),
                        "totalAmount", new BigDecimal(totalAmount)));
    }

    private static final class RecordingDetailSourceClient implements FinanceDetailReconciliationHarness.DetailSourceClient {

        private final List<String> oracleRequests = new ArrayList<>();
        private final List<String> copilotRequests = new ArrayList<>();
        private List<FinanceDetailReconciliationService.DetailRow> oracleRows = List.of();
        private List<FinanceDetailReconciliationService.DetailRow> copilotRows = List.of();

        @Override
        public List<FinanceDetailReconciliationService.DetailRow> fetchOracleRows(
                FinanceDetailReconciliationSampleRegistry.DetailSample sample) {
            oracleRequests.add(sample.id() + "|" + sample.oracleEndpoint());
            return oracleRows;
        }

        @Override
        public List<FinanceDetailReconciliationService.DetailRow> fetchCopilotRows(
                FinanceDetailReconciliationSampleRegistry.DetailSample sample) {
            copilotRequests.add(sample.id() + "|" + sample.copilotQuestion());
            return copilotRows;
        }
    }
}
