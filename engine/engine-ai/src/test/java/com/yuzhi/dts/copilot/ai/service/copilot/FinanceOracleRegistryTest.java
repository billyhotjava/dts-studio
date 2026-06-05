package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FinanceOracleRegistryTest {

    private final FinanceOracleRegistry registry = new FinanceOracleRegistry(new ObjectMapper());

    @Test
    void shouldBindCoreFinanceReportsToAdminApiOracles() {
        registry.init();

        assertThat(registry.bindings())
                .extracting(FinanceOracleRegistry.OracleBinding::id)
                .containsExactly("month-settlement", "sale-account", "voucher-ledger");

        FinanceOracleRegistry.OracleBinding monthSettlement = registry.binding("month-settlement").orElseThrow();
        assertThat(monthSettlement.oracleLevel()).isEqualTo("L2");
        assertThat(monthSettlement.chain()).isEqualTo("rent-settlement");
        assertThat(monthSettlement.sourceTables()).contains("a_month_accounting", "a_green_accounting");
        assertThat(monthSettlement.amountColumns())
                .contains("receivable_total_amount", "net_receipt_total_amount", "folding_after_total_amount", "total_amount");
        assertThat(monthSettlement.endpoints())
                .anySatisfy(endpoint -> assertThat(endpoint.signature())
                        .isEqualTo("GET /rs-flowers-base/operate/monthAccount/listMonthAccountingPage"))
                .anySatisfy(endpoint -> assertThat(endpoint.signature())
                        .isEqualTo("POST /rs-flowers-base/operate/monthAccount/getMonthSettlementData"));

        FinanceOracleRegistry.OracleBinding saleAccount = registry.binding("sale-account").orElseThrow();
        assertThat(saleAccount.oracleLevel()).isEqualTo("L2");
        assertThat(saleAccount.chain()).isEqualTo("sale-gift-bad-debt");
        assertThat(saleAccount.sourceTables()).contains("a_sale_account");
        assertThat(saleAccount.endpoints())
                .anySatisfy(endpoint -> assertThat(endpoint.signature())
                        .isEqualTo("GET /rs-flowers-base/operate/saleAccount/listSaleAccountPage"));

        FinanceOracleRegistry.OracleBinding voucherLedger = registry.binding("voucher-ledger").orElseThrow();
        assertThat(voucherLedger.oracleLevel()).isEqualTo("L3");
        assertThat(voucherLedger.chain()).isEqualTo("voucher-ledger");
        assertThat(voucherLedger.ledger().debitColumn()).isEqualTo("debit_amount");
        assertThat(voucherLedger.ledger().creditColumn()).isEqualTo("credit_amount");
        assertThat(voucherLedger.endpoints())
                .anySatisfy(endpoint -> assertThat(endpoint.signature())
                        .isEqualTo("GET /rs-flowers-base/finace/voucher/list"))
                .anySatisfy(endpoint -> assertThat(endpoint.signature())
                        .isEqualTo("POST /rs-flowers-base/finace/voucher/listByCodes"));
    }

    @Test
    void shouldExposeAdminWebEvidenceForRegisteredEndpoints() {
        registry.init();

        assertThat(registry.binding("month-settlement").orElseThrow().adminWebEvidence())
                .contains("adminweb/src/api/flower/operate/monthAccount.js");
        assertThat(registry.binding("sale-account").orElseThrow().adminWebEvidence())
                .contains("adminweb/src/api/flower/operate/saleAccount.js");
        assertThat(registry.binding("voucher-ledger").orElseThrow().adminWebEvidence())
                .contains("adminweb/src/api/flower/finance/voucher.js");
    }
}
