package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class FinanceAuthorityRegistryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FinanceAuthorityRegistry registry = new FinanceAuthorityRegistry(objectMapper);

    @Test
    void shouldBindCoreFinanceReportsToAdminApiOracles() {
        registry.init();

        assertThat(registry.bindings())
                .extracting(FinanceAuthorityRegistry.AuthorityBinding::id)
                .containsExactly("month-settlement", "sale-account", "voucher-ledger");

        FinanceAuthorityRegistry.AuthorityBinding monthSettlement = registry.binding("month-settlement").orElseThrow();
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

        FinanceAuthorityRegistry.AuthorityBinding saleAccount = registry.binding("sale-account").orElseThrow();
        assertThat(saleAccount.oracleLevel()).isEqualTo("L2");
        assertThat(saleAccount.chain()).isEqualTo("sale-gift-bad-debt");
        assertThat(saleAccount.sourceTables()).contains("a_sale_account");
        assertThat(saleAccount.endpoints())
                .anySatisfy(endpoint -> assertThat(endpoint.signature())
                        .isEqualTo("GET /rs-flowers-base/operate/saleAccount/listSaleAccountPage"));

        FinanceAuthorityRegistry.AuthorityBinding voucherLedger = registry.binding("voucher-ledger").orElseThrow();
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

    @Test
    void shouldPreferAuthorityLevelFieldWhileKeepingLegacyOracleLevelAccessor() throws Exception {
        String json = """
                {
                  "id": "voucher-ledger",
                  "reportName": "凭证",
                  "authorityLevel": "L3",
                  "chain": "voucher-ledger",
                  "sourceTables": ["f_voucher", "f_voucher_item"],
                  "amountColumns": ["debit_amount", "credit_amount"],
                  "endpoints": [],
                  "ledger": {
                    "voucherTable": "f_voucher",
                    "itemTable": "f_voucher_item",
                    "debitColumn": "debit_amount",
                    "creditColumn": "credit_amount",
                    "subjectColumn": "subject_id",
                    "voucherCodeColumn": "code"
                  },
                  "adminWebEvidence": ["adminweb/src/api/flower/finance/voucher.js"],
                  "notes": "authorityLevel 是新契约，oracleLevel 仅兼容旧资产"
                }
                """;

        FinanceAuthorityRegistry.AuthorityBinding binding =
                objectMapper.readValue(json, FinanceAuthorityRegistry.AuthorityBinding.class);

        assertThat(binding.authorityLevel()).isEqualTo("L3");
        assertThat(binding.oracleLevel()).isEqualTo("L3");
        assertThat(binding.sourceTables()).containsExactly("f_voucher", "f_voucher_item");
    }

    @Test
    void shouldLogAuthorityBindingsWithoutOracleDatabaseTerminology() {
        Logger logger = (Logger) LoggerFactory.getLogger(FinanceAuthorityRegistry.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            registry.init();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> assertThat(message)
                        .contains("finance authority binding(s)")
                        .doesNotContain("finance oracle binding(s)"));
    }
}
