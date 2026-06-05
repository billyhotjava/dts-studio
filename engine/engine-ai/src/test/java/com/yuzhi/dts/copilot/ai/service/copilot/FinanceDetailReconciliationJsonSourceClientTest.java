package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinanceDetailReconciliationJsonSourceClientTest {

    private final FinanceDetailReconciliationJsonSourceClient client =
            new FinanceDetailReconciliationJsonSourceClient(new ObjectMapper());

    @Test
    void shouldParseAdminapiAjaxAndTableRowsIntoOracleDetailRows() {
        FinanceDetailReconciliationSampleRegistry.DetailSample monthSample = monthSample();
        String monthAjaxPayload = """
                {
                  "code": 200,
                  "msg": "操作成功",
                  "data": {
                    "yearAndMonth": "202606",
                    "projectId": "1001",
                    "receivableTotalAmount": 1128.0000,
                    "netReceiptTotalAmount": 1128.00,
                    "foldingAfterTotalAmount": 1128.0,
                    "totalAmount": 1128
                  }
                }
                """;

        List<FinanceDetailReconciliationService.DetailRow> monthRows =
                client.parseOracleRows(monthSample, monthAjaxPayload);

        assertThat(monthRows).hasSize(1);
        assertThat(monthRows.get(0).chain()).isEqualTo("rent-settlement");
        assertThat(monthRows.get(0).businessKey()).isEqualTo("结算2026060008");
        assertThat(monthRows.get(0).projectId()).isEqualTo("1001");
        assertThat(monthRows.get(0).accountPeriod()).isEqualTo("202606");
        assertThat(monthRows.get(0).amounts())
                .containsEntry("receivableTotalAmount", new BigDecimal("1128.00"))
                .containsEntry("netReceiptTotalAmount", new BigDecimal("1128.00"))
                .containsEntry("foldingAfterTotalAmount", new BigDecimal("1128.00"))
                .containsEntry("totalAmount", new BigDecimal("1128.00"));

        FinanceDetailReconciliationSampleRegistry.DetailSample saleSample = saleSample();
        String saleTablePayload = """
                {
                  "total": 1,
                  "code": 200,
                  "msg": "查询成功",
                  "rows": [
                    {
                      "bizCode": "BX202606030968",
                      "projectId": "1001",
                      "accountPriod": "202606",
                      "receivableAmount": "3451.6800",
                      "netReceiptsAmount": "3451.68",
                      "bizAmount": 3451.68
                    }
                  ]
                }
                """;

        List<FinanceDetailReconciliationService.DetailRow> saleRows =
                client.parseOracleRows(saleSample, saleTablePayload);

        assertThat(saleRows).hasSize(1);
        assertThat(saleRows.get(0).chain()).isEqualTo("sale-gift-bad-debt");
        assertThat(saleRows.get(0).businessKey()).isEqualTo("BX202606030968");
        assertThat(saleRows.get(0).projectId()).isEqualTo("1001");
        assertThat(saleRows.get(0).accountPeriod()).isEqualTo("202606");
        assertThat(saleRows.get(0).amounts())
                .containsEntry("receivableAmount", new BigDecimal("3451.68"))
                .containsEntry("netReceiptsAmount", new BigDecimal("3451.68"))
                .containsEntry("bizAmount", new BigDecimal("3451.68"));
    }

    @Test
    void shouldParseCopilotDatasetRowsUsingDatasetColumnMetadata() {
        String datasetPayload = """
                {
                  "status": "completed",
                  "data": {
                    "rows": [
                      ["结算2026060008", "1001", "202606", 1128.0000, 1128.00, 1128.0, 1128]
                    ],
                    "cols": [
                      {"name": "businessKey"},
                      {"name": "projectId"},
                      {"name": "accountPeriod"},
                      {"name": "receivableTotalAmount"},
                      {"name": "netReceiptTotalAmount"},
                      {"name": "foldingAfterTotalAmount"},
                      {"name": "totalAmount"}
                    ],
                    "results_metadata": {
                      "columns": [
                        {"name": "businessKey"},
                        {"name": "projectId"},
                        {"name": "accountPeriod"},
                        {"name": "receivableTotalAmount"},
                        {"name": "netReceiptTotalAmount"},
                        {"name": "foldingAfterTotalAmount"},
                        {"name": "totalAmount"}
                      ]
                    }
                  },
                  "row_count": 1
                }
                """;

        List<FinanceDetailReconciliationService.DetailRow> rows =
                client.parseCopilotRows(monthSample(), datasetPayload);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).chain()).isEqualTo("rent-settlement");
        assertThat(rows.get(0).businessKey()).isEqualTo("结算2026060008");
        assertThat(rows.get(0).projectId()).isEqualTo("1001");
        assertThat(rows.get(0).accountPeriod()).isEqualTo("202606");
        assertThat(rows.get(0).amounts())
                .containsEntry("receivableTotalAmount", new BigDecimal("1128.00"))
                .containsEntry("netReceiptTotalAmount", new BigDecimal("1128.00"))
                .containsEntry("foldingAfterTotalAmount", new BigDecimal("1128.00"))
                .containsEntry("totalAmount", new BigDecimal("1128.00"));
    }

    @Test
    void shouldFetchRowsFromPayloadProviderForHarnessClientContract() {
        FinanceDetailReconciliationJsonSourceClient.PayloadProvider provider =
                new FinanceDetailReconciliationJsonSourceClient.PayloadProvider() {
                    @Override
                    public String oraclePayload(FinanceDetailReconciliationSampleRegistry.DetailSample sample) {
                        return """
                                {"rows":[{"businessKey":"BX202606030968","projectId":"1001","accountPeriod":"202606",
                                "receivableAmount":3451.68,"netReceiptsAmount":3451.68,"bizAmount":3451.68}]}
                                """;
                    }

                    @Override
                    public String copilotPayload(FinanceDetailReconciliationSampleRegistry.DetailSample sample) {
                        return """
                                {"data":{"rows":[{"business_key":"BX202606030968","project_id":"1001","account_period":"202606",
                                "receivable_amount":3451.68,"net_receipts_amount":3451.68,"biz_amount":3451.68}]}}
                                """;
                    }
                };
        FinanceDetailReconciliationHarness.DetailSourceClient harnessClient =
                client.sourceClient(provider);

        List<FinanceDetailReconciliationService.DetailRow> oracleRows =
                harnessClient.fetchOracleRows(saleSample());
        List<FinanceDetailReconciliationService.DetailRow> copilotRows =
                harnessClient.fetchCopilotRows(saleSample());

        assertThat(oracleRows).hasSize(1);
        assertThat(copilotRows).hasSize(1);
        assertThat(copilotRows.get(0).amounts())
                .containsEntry("receivableAmount", new BigDecimal("3451.68"))
                .containsEntry("netReceiptsAmount", new BigDecimal("3451.68"))
                .containsEntry("bizAmount", new BigDecimal("3451.68"));
    }

    @Test
    void shouldRejectAdminapiEnvelopeWithoutDetailRows() {
        assertThatThrownBy(() -> client.parseOracleRows(monthSample(), """
                {"code":500,"msg":"操作失败"}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not contain finance detail rows");
    }

    private static FinanceDetailReconciliationSampleRegistry.DetailSample monthSample() {
        return new FinanceDetailReconciliationSampleRegistry.DetailSample(
                "month-settlement-js2026060008",
                "month-settlement",
                "rent-settlement",
                "POST /rs-flowers-base/operate/monthAccount/getMonthSettlementData",
                "结算2026060008",
                "1001",
                "202606",
                "查询 202606 账期 1001 项目 结算2026060008 的月对账三级金额明细",
                List.of("receivableTotalAmount", "netReceiptTotalAmount", "foldingAfterTotalAmount", "totalAmount"),
                java.util.Map.of("projectId", "1001", "yearAndMonth", "202606"),
                java.util.Map.of("domain", "finance", "chain", "rent-settlement"));
    }

    private static FinanceDetailReconciliationSampleRegistry.DetailSample saleSample() {
        return new FinanceDetailReconciliationSampleRegistry.DetailSample(
                "sale-account-bx202606030968",
                "sale-account",
                "sale-gift-bad-debt",
                "GET /rs-flowers-base/operate/saleAccount/listSaleAccountPage",
                "BX202606030968",
                "1001",
                "202606",
                "查询 202606 账期 1001 项目 BX202606030968 的售账应收实收明细",
                List.of("receivableAmount", "netReceiptsAmount", "bizAmount"),
                java.util.Map.of("projectId", "1001", "bizCode", "BX202606030968"),
                java.util.Map.of("domain", "finance", "chain", "sale-gift-bad-debt"));
    }
}
