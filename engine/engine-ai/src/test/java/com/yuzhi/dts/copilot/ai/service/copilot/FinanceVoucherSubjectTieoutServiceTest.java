package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceVoucherSubjectTieoutServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadMetricToVoucherSubjectMappingsFromGovernanceAsset() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceVoucherSubjectTieoutRegistry registry =
                new FinanceVoucherSubjectTieoutRegistry(objectMapper, oracleRegistry);
        registry.init();

        assertThat(registry.mappings())
                .extracting(FinanceVoucherSubjectTieoutRegistry.SubjectTieoutMapping::id)
                .containsExactly("settlement-discounted-receivable", "sale-receivable", "bad-debt-loss");

        FinanceVoucherSubjectTieoutRegistry.SubjectTieoutMapping settlement =
                registry.mapping("settlement-discounted-receivable").orElseThrow();
        assertThat(settlement.oracleBindingId()).isEqualTo("voucher-ledger");
        assertThat(settlement.chain()).isEqualTo("rent-settlement");
        assertThat(settlement.metricId()).isEqualTo("discounted-receivable");
        assertThat(settlement.voucherBusinessType()).isEqualTo(1);
        assertThat(settlement.voucherSide()).isEqualTo("debit");
        assertThat(settlement.subjectGroup()).isEqualTo("settlement-receivable-subjects");
        assertThat(settlement.subjectIds()).containsExactly(100101L, 112201L);

        FinanceVoucherSubjectTieoutRegistry.SubjectTieoutMapping sale =
                registry.mapping("sale-receivable").orElseThrow();
        assertThat(sale.chain()).isEqualTo("sale-gift-bad-debt");
        assertThat(sale.voucherSide()).isEqualTo("debit");
        assertThat(sale.subjectGroup()).isEqualTo("sale-receivable-subjects");

        FinanceVoucherSubjectTieoutRegistry.SubjectTieoutMapping badDebt =
                registry.mapping("bad-debt-loss").orElseThrow();
        assertThat(badDebt.chain()).isEqualTo("sale-gift-bad-debt");
        assertThat(badDebt.metricId()).isEqualTo("bad-debt-loss");
        assertThat(badDebt.voucherSide()).isEqualTo("debit");
        assertThat(badDebt.subjectGroup()).isEqualTo("bad-debt-loss-subjects");
        assertThat(badDebt.notes()).contains("坏账不计入收入");
    }

    @Test
    void shouldTieOutSummaryMetricToVoucherSubjectSideByBusinessCodeAndPeriod() {
        FinanceVoucherSubjectTieoutService service = new FinanceVoucherSubjectTieoutService();
        FinanceVoucherSubjectTieoutService.SubjectTieoutSpec spec =
                new FinanceVoucherSubjectTieoutService.SubjectTieoutSpec(
                        "settlement-discounted-receivable",
                        "rent-settlement",
                        "discounted-receivable",
                        1,
                        "debit",
                        List.of(100101L, 112201L),
                        List.of("businessCode", "accountPeriod"));

        FinanceVoucherSubjectTieoutService.SubjectTieoutReport matched = service.tieOut(
                spec,
                List.of(summary("rent-settlement", "discounted-receivable", "JS2026060008", "202606", "1128.00")),
                List.of(
                        voucher("JS2026060008", "PZ-202606-0001", "202606", 100101L, "1000.00", "0"),
                        voucher("JS2026060008", "PZ-202606-0001", "202606", 112201L, "128.00", "0"),
                        voucher("JS2026060008", "PZ-202606-0099", "202606", 20, 112201L, "999.00", "0"),
                        voucher("JS2026060008", "PZ-202606-0001", "202606", 600101L, "0", "1128.00")));

        assertThat(matched.passed()).isTrue();
        assertThat(matched.failureMessage()).isEmpty();
        assertThat(matched.diffs()).hasSize(1);
        assertThat(matched.diffs().getFirst().voucherAmount()).isEqualByComparingTo("1128.00");

        FinanceVoucherSubjectTieoutService.SubjectTieoutReport mismatch = service.tieOut(
                spec,
                List.of(summary("rent-settlement", "discounted-receivable", "JS2026060008", "202606", "1128.00")),
                List.of(voucher("JS2026060008", "PZ-202606-0001", "202606", 100101L, "1127.99", "0")));

        assertThat(mismatch.passed()).isFalse();
        assertThat(mismatch.failureMessage())
                .contains("settlement-discounted-receivable", "JS2026060008", "difference=0.01");
    }

    @Test
    void shouldRejectWrongChainMetricMissingCellsAndUnmappedSubjects() {
        FinanceVoucherSubjectTieoutService service = new FinanceVoucherSubjectTieoutService();
        FinanceVoucherSubjectTieoutService.SubjectTieoutSpec spec =
                new FinanceVoucherSubjectTieoutService.SubjectTieoutSpec(
                        "sale-receivable",
                        "sale-gift-bad-debt",
                        "sale-receivable",
                        1,
                        "debit",
                        List.of(112201L),
                        List.of("businessCode", "accountPeriod"));

        FinanceVoucherSubjectTieoutService.SubjectTieoutReport wrongChain = service.tieOut(
                spec,
                List.of(summary("rent-settlement", "sale-receivable", "BX202606030968", "202606", "3451.68")),
                List.of(voucher("BX202606030968", "PZ-202606-0012", "202606", 112201L, "3451.68", "0")));

        assertThat(wrongChain.passed()).isFalse();
        assertThat(wrongChain.failureMessage()).contains("chain mismatch", "expected=sale-gift-bad-debt", "actual=rent-settlement");

        FinanceVoucherSubjectTieoutService.SubjectTieoutReport missingVoucher = service.tieOut(
                spec,
                List.of(summary("sale-gift-bad-debt", "sale-receivable", "BX202606030968", "202606", "3451.68")),
                List.of());

        assertThat(missingVoucher.passed()).isFalse();
        assertThat(missingVoucher.failureMessage())
                .contains("missing voucher subject cell", "summary=3451.68", "voucher=0.00", "difference=3451.68");

        FinanceVoucherSubjectTieoutService.SubjectTieoutReport unmappedOnly = service.tieOut(
                spec,
                List.of(summary("sale-gift-bad-debt", "sale-receivable", "BX202606030968", "202606", "3451.68")),
                List.of(voucher("BX202606030968", "PZ-202606-0012", "202606", 600101L, "0", "3451.68")));

        assertThat(unmappedOnly.passed()).isFalse();
        assertThat(unmappedOnly.failureMessage()).contains("missing voucher subject cell");
        assertThat(unmappedOnly.diffs().getFirst().ignoredVoucherAmount()).isEqualByComparingTo("3451.68");
    }

    private static FinanceVoucherSubjectTieoutService.SummaryMetricRow summary(
            String chain,
            String metricId,
            String businessCode,
            String accountPeriod,
            String amount) {
        return new FinanceVoucherSubjectTieoutService.SummaryMetricRow(
                chain,
                metricId,
                Map.of("businessCode", businessCode, "accountPeriod", accountPeriod),
                new BigDecimal(amount));
    }

    private static VoucherLedgerTieoutService.VoucherLedgerRow voucher(
            String businessCode,
            String voucherCode,
            String accountPeriod,
            Long subjectId,
            String debitAmount,
            String creditAmount) {
        return new VoucherLedgerTieoutService.VoucherLedgerRow(
                businessCode,
                voucherCode,
                accountPeriod,
                1,
                subjectId,
                new BigDecimal(debitAmount),
                new BigDecimal(creditAmount));
    }

    private static VoucherLedgerTieoutService.VoucherLedgerRow voucher(
            String businessCode,
            String voucherCode,
            String accountPeriod,
            Integer businessType,
            Long subjectId,
            String debitAmount,
            String creditAmount) {
        return new VoucherLedgerTieoutService.VoucherLedgerRow(
                businessCode,
                voucherCode,
                accountPeriod,
                businessType,
                subjectId,
                new BigDecimal(debitAmount),
                new BigDecimal(creditAmount));
    }
}
