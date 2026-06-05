package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoucherLedgerTieoutRegistryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldBindVoucherLedgerTieoutToAdminApiAndAdminWebEvidence() {
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();

        VoucherLedgerTieoutRegistry registry = new VoucherLedgerTieoutRegistry(objectMapper, oracleRegistry);
        registry.init();

        VoucherLedgerTieoutRegistry.TieoutMapping mapping = registry.mapping("voucher-ledger").orElseThrow();
        assertThat(registry.mappings())
                .extracting(VoucherLedgerTieoutRegistry.TieoutMapping::id)
                .containsExactly("voucher-ledger");
        assertThat(mapping.oracleBindingId()).isEqualTo("voucher-ledger");
        assertThat(mapping.adminApiEndpoints())
                .contains(
                        "GET /rs-flowers-base/finace/voucher/list",
                        "POST /rs-flowers-base/finace/voucher/listByCodes",
                        "GET /rs-flowers-base/finace/voucher/listVoucherItems",
                        "GET /rs-flowers-base/finace/voucher/getcountItems");
        assertThat(mapping.sourceTables()).containsExactly("f_voucher", "f_voucher_item");
        assertThat(mapping.ledgerColumns().voucherTable()).isEqualTo("f_voucher");
        assertThat(mapping.ledgerColumns().itemTable()).isEqualTo("f_voucher_item");
        assertThat(mapping.ledgerColumns().businessCodeColumn()).isEqualTo("biz_code");
        assertThat(mapping.ledgerColumns().periodColumn()).isEqualTo("account_priod");
        assertThat(mapping.ledgerColumns().voucherCodeColumn()).isEqualTo("voucher_code");
        assertThat(mapping.ledgerColumns().subjectColumn()).isEqualTo("subject_id");
        assertThat(mapping.ledgerColumns().debitColumn()).isEqualTo("debit_amount");
        assertThat(mapping.ledgerColumns().creditColumn()).isEqualTo("credit_amount");
        assertThat(mapping.joinRules())
                .contains(
                        "f_voucher.code = f_voucher_item.voucher_code",
                        "f_voucher.account_priod = f_voucher_item.account_priod");
        assertThat(mapping.adminWebEvidence())
                .contains(
                        "adminweb/src/api/flower/finance/voucher.js",
                        "adminweb/src/views/flower/finance/voucher/list-voucher.vue",
                        "adminweb/src/views/flower/finance/voucher/detail-index.vue",
                        "adminweb/src/views/flower/finance/voucher/summary.vue");
    }

    @Test
    void shouldAggregateVoucherRowsByBusinessCodePeriodAndSubjectAndDetectImbalance() {
        VoucherLedgerTieoutService service = new VoucherLedgerTieoutService();

        VoucherLedgerTieoutService.TieoutReport report = service.tieOut(List.of(
                row("JS2026060008", "PZ-202606-0001", "202606", 100101L, "1128.00", "0"),
                row("JS2026060008", "PZ-202606-0001", "202606", 600101L, "0", "1128.00"),
                row("BX202606030968", "PZ-202606-0012", "202606", 660201L, "3451.68", "0"),
                row("BX202606030968", "PZ-202606-0012", "202606", 100201L, "0", "3451.68")));

        assertThat(report.balanced()).isTrue();
        assertThat(report.failureMessage()).isEmpty();
        assertThat(report.subjectLines())
                .anySatisfy(line -> {
                    assertThat(line.businessCode()).isEqualTo("JS2026060008");
                    assertThat(line.accountPeriod()).isEqualTo("202606");
                    assertThat(line.subjectId()).isEqualTo(100101L);
                    assertThat(line.debitAmount()).isEqualByComparingTo("1128.00");
                    assertThat(line.creditAmount()).isEqualByComparingTo("0");
                })
                .anySatisfy(line -> {
                    assertThat(line.businessCode()).isEqualTo("BX202606030968");
                    assertThat(line.accountPeriod()).isEqualTo("202606");
                    assertThat(line.subjectId()).isEqualTo(100201L);
                    assertThat(line.debitAmount()).isEqualByComparingTo("0");
                    assertThat(line.creditAmount()).isEqualByComparingTo("3451.68");
                });

        VoucherLedgerTieoutService.TieoutReport imbalanced = service.tieOut(List.of(
                row("JS2026060008", "PZ-202606-0001", "202606", 100101L, "1128.00", "0"),
                row("JS2026060008", "PZ-202606-0001", "202606", 600101L, "0", "1127.99")));

        assertThat(imbalanced.balanced()).isFalse();
        assertThat(imbalanced.failureMessage())
                .contains("JS2026060008", "PZ-202606-0001", "202606", "0.01");
    }

    private static VoucherLedgerTieoutService.VoucherLedgerRow row(
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
                subjectId,
                new BigDecimal(debitAmount),
                new BigDecimal(creditAmount));
    }
}
