package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceDetailReconciliationServiceTest {

    private final FinanceDetailReconciliationService service = new FinanceDetailReconciliationService();

    @Test
    void shouldReconcileOracleAndCopilotDetailRowsToCents() {
        FinanceDetailReconciliationService.DetailReconciliationSpec spec =
                new FinanceDetailReconciliationService.DetailReconciliationSpec(
                        "month-settlement",
                        "rent-settlement",
                        List.of(
                                "receivableTotalAmount",
                                "netReceiptTotalAmount",
                                "foldingAfterTotalAmount",
                                "totalAmount"));

        FinanceDetailReconciliationService.DetailReconciliationReport report = service.reconcile(
                spec,
                List.of(monthRow("结算2026060008", "1001", "202606", "1128.0000", "1128.0000", "1128.0000", "1128.0000")),
                List.of(monthRow("结算2026060008", "1001", "202606", "1128.00", "1128.00", "1128.00", "1128.00")));

        assertThat(report.passed()).isTrue();
        assertThat(report.failureMessage()).isEmpty();
        assertThat(report.diffs()).hasSize(1);
        assertThat(report.diffs().get(0).maxDifference()).isEqualByComparingTo("0.00");

        FinanceDetailReconciliationService.DetailReconciliationReport mismatch = service.reconcile(
                spec,
                List.of(monthRow("结算2026060008", "1001", "202606", "1128.0000", "1128.0000", "1127.9900", "1128.0000")),
                List.of(monthRow("结算2026060008", "1001", "202606", "1128.00", "1128.00", "1128.00", "1128.00")));

        assertThat(mismatch.passed()).isFalse();
        assertThat(mismatch.failureMessage())
                .contains("month-settlement", "rent-settlement", "结算2026060008", "foldingAfterTotalAmount", "0.01");
    }

    @Test
    void shouldFailWhenRowsAreMissingOrChainIsMixed() {
        FinanceDetailReconciliationService.DetailReconciliationSpec spec =
                new FinanceDetailReconciliationService.DetailReconciliationSpec(
                        "sale-account",
                        "sale-gift-bad-debt",
                        List.of("receivableAmount", "netReceiptsAmount", "bizAmount"));

        FinanceDetailReconciliationService.DetailReconciliationReport missingRow = service.reconcile(
                spec,
                List.of(saleRow("BX202606030968", "1001", "202606", "3451.68", "3451.68", "3451.68")),
                List.of());

        assertThat(missingRow.passed()).isFalse();
        assertThat(missingRow.failureMessage())
                .contains("sale-account", "BX202606030968", "missing authority row");

        FinanceDetailReconciliationService.DetailReconciliationReport mixedChain = service.reconcile(
                spec,
                List.of(new FinanceDetailReconciliationService.DetailRow(
                        "rent-settlement",
                        "BX202606030968",
                        "1001",
                        "202606",
                        Map.of("receivableAmount", new BigDecimal("3451.68")))),
                List.of(saleRow("BX202606030968", "1001", "202606", "3451.68", "3451.68", "3451.68")));

        assertThat(mixedChain.passed()).isFalse();
        assertThat(mixedChain.failureMessage())
                .contains("chain mismatch", "expected=sale-gift-bad-debt", "actual=rent-settlement");

        FinanceDetailReconciliationService.DetailReconciliationReport duplicate = service.reconcile(
                spec,
                List.of(
                        saleRow("BX202606030968", "1001", "202606", "3451.68", "3451.68", "3451.68"),
                        saleRow("BX202606030968", "1001", "202606", "3451.68", "3451.68", "3451.68")),
                List.of(saleRow("BX202606030968", "1001", "202606", "3451.68", "3451.68", "3451.68")));

        assertThat(duplicate.passed()).isFalse();
        assertThat(duplicate.failureMessage())
                .contains("duplicate copilot row", "BX202606030968", "1001", "202606");
    }

    @Test
    void shouldReportAuthorityBindingTerminologyInFailureMessages() {
        FinanceDetailReconciliationService.DetailReconciliationSpec spec =
                new FinanceDetailReconciliationService.DetailReconciliationSpec(
                        "month-settlement",
                        "rent-settlement",
                        List.of("foldingAfterTotalAmount"));

        FinanceDetailReconciliationService.DetailReconciliationReport amountMismatch = service.reconcile(
                spec,
                List.of(monthRow("结算2026060008", "1001", "202606", "1128.00", "1128.00", "1127.99", "1128.00")),
                List.of(monthRow("结算2026060008", "1001", "202606", "1128.00", "1128.00", "1128.00", "1128.00")));

        assertThat(amountMismatch.failureMessage())
                .contains("authorityBindingId=month-settlement", "authority=1128.00")
                .doesNotContain("oracleBindingId", "oracle=1128.00");

        FinanceDetailReconciliationService.DetailReconciliationReport missingAuthorityRow = service.reconcile(
                spec,
                List.of(monthRow("结算2026060008", "1001", "202606", "1128.00", "1128.00", "1128.00", "1128.00")),
                List.of());

        assertThat(missingAuthorityRow.failureMessage())
                .contains("authorityBindingId=month-settlement", "missing authority row")
                .doesNotContain("oracleBindingId", "missing oracle row");

        FinanceDetailReconciliationService.DetailReconciliationReport duplicateAuthorityRow = service.reconcile(
                spec,
                List.of(monthRow("结算2026060008", "1001", "202606", "1128.00", "1128.00", "1128.00", "1128.00")),
                List.of(
                        monthRow("结算2026060008", "1001", "202606", "1128.00", "1128.00", "1128.00", "1128.00"),
                        monthRow("结算2026060008", "1001", "202606", "1128.00", "1128.00", "1128.00", "1128.00")));

        assertThat(duplicateAuthorityRow.failureMessage())
                .contains("authorityBindingId=month-settlement", "duplicate authority row")
                .doesNotContain("oracleBindingId", "duplicate oracle row");
    }

    private static FinanceDetailReconciliationService.DetailRow monthRow(
            String businessKey,
            String projectId,
            String accountPeriod,
            String receivableTotalAmount,
            String netReceiptTotalAmount,
            String foldingAfterTotalAmount,
            String totalAmount) {
        return new FinanceDetailReconciliationService.DetailRow(
                "rent-settlement",
                businessKey,
                projectId,
                accountPeriod,
                Map.of(
                        "receivableTotalAmount", new BigDecimal(receivableTotalAmount),
                        "netReceiptTotalAmount", new BigDecimal(netReceiptTotalAmount),
                        "foldingAfterTotalAmount", new BigDecimal(foldingAfterTotalAmount),
                        "totalAmount", new BigDecimal(totalAmount)));
    }

    private static FinanceDetailReconciliationService.DetailRow saleRow(
            String businessKey,
            String projectId,
            String accountPeriod,
            String receivableAmount,
            String netReceiptsAmount,
            String bizAmount) {
        return new FinanceDetailReconciliationService.DetailRow(
                "sale-gift-bad-debt",
                businessKey,
                projectId,
                accountPeriod,
                Map.of(
                        "receivableAmount", new BigDecimal(receivableAmount),
                        "netReceiptsAmount", new BigDecimal(netReceiptsAmount),
                        "bizAmount", new BigDecimal(bizAmount)));
    }
}
