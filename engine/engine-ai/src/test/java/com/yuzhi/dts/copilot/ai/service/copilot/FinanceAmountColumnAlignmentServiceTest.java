package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceAmountColumnAlignmentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadFourMonthAmountTiersFromGovernanceAsset() {
        FinanceAmountColumnAlignmentRegistry registry = new FinanceAmountColumnAlignmentRegistry(objectMapper);
        registry.init();

        assertThat(registry.tiers())
                .extracting(FinanceAmountColumnAlignmentRegistry.AmountTier::id)
                .containsExactly(
                        "nominal-rent",
                        "receivable-before-discount",
                        "discounted-receivable",
                        "paid-amount");

        assertThat(registry.tier("nominal-rent").orElseThrow().apiField()).isEqualTo("receivableTotalAmount");
        assertThat(registry.tier("nominal-rent").orElseThrow().sourceColumn()).isEqualTo("receivable_total_amount");
        assertThat(registry.tier("receivable-before-discount").orElseThrow().apiField()).isEqualTo("netReceiptTotalAmount");
        assertThat(registry.tier("receivable-before-discount").orElseThrow().sourceColumn()).isEqualTo("net_receipt_total_amount");
        assertThat(registry.tier("discounted-receivable").orElseThrow().apiField()).isEqualTo("foldingAfterTotalAmount");
        assertThat(registry.tier("discounted-receivable").orElseThrow().sourceColumn()).isEqualTo("folding_after_total_amount");
        assertThat(registry.tier("paid-amount").orElseThrow().apiField()).isEqualTo("totalAmount");
        assertThat(registry.tier("paid-amount").orElseThrow().sourceColumn()).isEqualTo("total_amount");
    }

    @Test
    void shouldRejectWrongOrAmbiguousAmountColumnSelections() {
        FinanceAmountColumnAlignmentRegistry registry = initializedRegistry();
        FinanceAmountColumnAlignmentService service = new FinanceAmountColumnAlignmentService(registry);

        FinanceAmountColumnAlignmentService.AlignmentReport ok = service.validateSelections(List.of(
                new FinanceAmountColumnAlignmentService.ColumnSelection("应收折前", "netReceiptTotalAmount"),
                new FinanceAmountColumnAlignmentService.ColumnSelection("折后实收", "foldingAfterTotalAmount"),
                new FinanceAmountColumnAlignmentService.ColumnSelection("已回款", "totalAmount")));

        assertThat(ok.passed()).isTrue();
        assertThat(ok.failureMessage()).isEmpty();

        FinanceAmountColumnAlignmentService.AlignmentReport wrong = service.validateSelections(List.of(
                new FinanceAmountColumnAlignmentService.ColumnSelection("折后实收", "netReceiptTotalAmount")));

        assertThat(wrong.passed()).isFalse();
        assertThat(wrong.failureMessage())
                .contains("折后实收", "expected=foldingAfterTotalAmount", "actual=netReceiptTotalAmount");

        FinanceAmountColumnAlignmentService.AlignmentReport ambiguous = service.validateSelections(List.of(
                new FinanceAmountColumnAlignmentService.ColumnSelection("应收", "netReceiptTotalAmount")));

        assertThat(ambiguous.passed()).isFalse();
        assertThat(ambiguous.failureMessage())
                .contains("应收", "ambiguous", "receivable-before-discount", "discounted-receivable", "paid-amount");
    }

    @Test
    void shouldUseDiscountTrapSamplesToProveColumnAlignment() {
        FinanceAmountColumnAlignmentRegistry registry = initializedRegistry();
        FinanceAmountColumnAlignmentService service = new FinanceAmountColumnAlignmentService(registry);

        FinanceAmountColumnAlignmentService.ProbeReport equalDiscountTrap = service.validateProbeAmounts(
                new FinanceAmountColumnAlignmentService.AmountProbe(Map.of(
                        "receivableTotalAmount", new BigDecimal("100.00"),
                        "netReceiptTotalAmount", new BigDecimal("100.00"),
                        "foldingAfterTotalAmount", new BigDecimal("100.00"),
                        "totalAmount", new BigDecimal("100.00"))),
                Map.of(
                        "应收折前", new BigDecimal("100.00"),
                        "折后实收", new BigDecimal("100.00"),
                        "已回款", new BigDecimal("100.00")));

        assertThat(equalDiscountTrap.conclusive()).isFalse();
        assertThat(equalDiscountTrap.failureMessage()).contains("discount=1 trap");

        FinanceAmountColumnAlignmentService.ProbeReport differentiated = service.validateProbeAmounts(
                new FinanceAmountColumnAlignmentService.AmountProbe(Map.of(
                        "receivableTotalAmount", new BigDecimal("120.00"),
                        "netReceiptTotalAmount", new BigDecimal("110.00"),
                        "foldingAfterTotalAmount", new BigDecimal("100.00"),
                        "totalAmount", new BigDecimal("90.00"))),
                Map.of(
                        "应收折前", new BigDecimal("110.00"),
                        "折后实收", new BigDecimal("100.00"),
                        "已回款", new BigDecimal("90.00")));

        assertThat(differentiated.passed()).isTrue();
        assertThat(differentiated.conclusive()).isTrue();

        FinanceAmountColumnAlignmentService.ProbeReport swapped = service.validateProbeAmounts(
                new FinanceAmountColumnAlignmentService.AmountProbe(Map.of(
                        "receivableTotalAmount", new BigDecimal("120.00"),
                        "netReceiptTotalAmount", new BigDecimal("110.00"),
                        "foldingAfterTotalAmount", new BigDecimal("100.00"),
                        "totalAmount", new BigDecimal("90.00"))),
                Map.of("折后实收", new BigDecimal("110.00")));

        assertThat(swapped.passed()).isFalse();
        assertThat(swapped.failureMessage())
                .contains("折后实收", "foldingAfterTotalAmount", "expected=100.00", "actual=110.00");
    }

    private FinanceAmountColumnAlignmentRegistry initializedRegistry() {
        FinanceAmountColumnAlignmentRegistry registry = new FinanceAmountColumnAlignmentRegistry(objectMapper);
        registry.init();
        return registry;
    }
}
