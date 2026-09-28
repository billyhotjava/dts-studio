package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Nl2SqlAccuracyEvidenceContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void tpl57FinanceVoucherAdsWithPassedTieoutIsHigh() {
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(
                done,
                financeVoucherPlan("L3_ADS", "TPL-57"),
                "select account_period, voucher_count from public.xycyl_ads_finance_voucher_monthly",
                CopilotChatRequestContext.of(
                        Map.of(),
                        Map.of("public.xycyl_ads_finance_voucher_monthly", "FRESH"),
                        Map.of(),
                        Map.of()),
                passedFinanceAuditTrail());

        JsonNode evidence = done.path("accuracyEvidence");
        assertThat(evidence.path("grade").asText()).isEqualTo("HIGH");
        assertThat(evidence.path("route").path("templateCode").asText()).isEqualTo("TPL-57");
        assertThat(evidence.path("data").path("freshness").asText()).isEqualTo("FRESH");
        assertThat(evidence.path("tieout").path("status").asText()).isEqualTo("PASS");
        assertThat(evidence.path("reasons").toString()).contains("对账通过");
    }

    @Test
    void adsOrDwsWithoutTieoutIsAtMostMedium() {
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(
                done,
                financeVoucherPlan("L1_DBT_MART", "TPL-57"),
                "select account_period, voucher_count from public.xycyl_ads_finance_voucher_monthly");

        JsonNode evidence = done.path("accuracyEvidence");
        assertThat(evidence.path("grade").asText()).isEqualTo("MEDIUM");
        assertThat(evidence.path("warnings").toString()).contains("缺少对账证据");
        assertThat(evidence.path("tieout").path("status").asText()).isEqualTo("MISSING");
    }

    @Test
    void missingFreshnessDowngradesWarehouseAnswerToUntrusted() {
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(
                done,
                financeVoucherPlan("L3_ADS", "TPL-57"),
                "select account_period, voucher_count from public.xycyl_ads_finance_voucher_monthly",
                CopilotChatRequestContext.of(
                        Map.of(),
                        Map.of("xycyl_ads_finance_voucher_monthly", "MISSING"),
                        Map.of(),
                        Map.of()),
                passedFinanceAuditTrail());

        JsonNode evidence = done.path("accuracyEvidence");
        assertThat(evidence.path("grade").asText()).isEqualTo("UNTRUSTED");
        assertThat(evidence.path("data").path("freshness").asText()).isEqualTo("MISSING");
        assertThat(evidence.path("warnings").toString()).contains("未入湖或未构建");
    }

    @Test
    void staleFreshnessCapsWarehouseAnswerAtMedium() {
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(
                done,
                financeVoucherPlan("L3_ADS", "TPL-57"),
                "select account_period, voucher_count from public.xycyl_ads_finance_voucher_monthly",
                CopilotChatRequestContext.of(
                        Map.of(),
                        Map.of("public.xycyl_ads_finance_voucher_monthly", "STALE"),
                        Map.of(),
                        Map.of()),
                passedFinanceAuditTrail());

        JsonNode evidence = done.path("accuracyEvidence");
        assertThat(evidence.path("grade").asText()).isEqualTo("MEDIUM");
        assertThat(evidence.path("data").path("freshness").asText()).isEqualTo("STALE");
        assertThat(evidence.path("warnings").toString()).contains("已过期");
    }

    @Test
    void l0BusinessObjectProfileIsUntrustedForStatistics() {
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(done, new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.SCHEMA_EXPLORATION,
                null,
                "finance",
                "finance.profile",
                List.of(),
                null,
                null,
                "PROFILE",
                null,
                "凭证字段画像",
                "L0_BUSINESS_OBJECT_PROFILE",
                "MEDIUM",
                List.of("仅字段画像，不是可采信统计"),
                "table",
                "prs.finance.voucher.profile",
                List.of("business-object:finance.voucher")), null);

        JsonNode evidence = done.path("accuracyEvidence");
        assertThat(evidence.path("grade").asText()).isEqualTo("UNTRUSTED");
        assertThat(evidence.path("warnings").toString()).contains("统计结论");
    }

    @Test
    void applicationMysqlDirectQueryIsUntrustedForWarehouseStatistics() {
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(done, new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.BUSINESS_DETAIL,
                null,
                "finance",
                "mysql.rs_cloud_flower.f_voucher",
                List.of(),
                null,
                null,
                "MYSQL",
                null,
                "应用 MySQL 只读明细",
                "L0_ADMINAPI_READONLY",
                "MEDIUM",
                List.of("只能作为对账证明基准"),
                "table",
                "finance.voucher.mysql-direct",
                List.of("application-mysql:mysql.rs_cloud_flower.f_voucher")),
                "select count(*) from mysql.rs_cloud_flower.f_voucher");

        JsonNode evidence = done.path("accuracyEvidence");
        assertThat(evidence.path("grade").asText()).isEqualTo("UNTRUSTED");
        assertThat(evidence.path("reasons").toString()).contains("应用 MySQL");
        assertThat(evidence.path("warnings").toString()).contains("仓库统计结论");
    }

    private static ConversationPlan financeVoucherPlan(String dataSurface, String templateCode) {
        return new ConversationPlan(
                PlanMode.TEMPLATE_FAST_PATH,
                ResponseKind.TEMPLATE_SQL,
                null,
                "finance",
                "public.xycyl_ads_finance_voucher_monthly",
                List.of(),
                templateCode,
                "select account_period, voucher_count from public.xycyl_ads_finance_voucher_monthly",
                "MART",
                "public.xycyl_ads_finance_voucher_monthly",
                "2026 年凭证统计",
                dataSurface,
                "HIGH",
                List.of("命中财务凭证 ADS 模板"),
                "table",
                "finance.voucher.yearly",
                List.of("dbt-model:public.xycyl_ads_finance_voucher_monthly"),
                null,
                List.of(new ConversationPlan.RouteStep(
                        "TIER_2_MART_TEMPLATE",
                        "ADS 模板",
                        "HIT",
                        "命中凭证统计模板",
                        "public.xycyl_ads_finance_voucher_monthly")));
    }

    private static FinanceAnswerAuditTrailService.AuditTrailReport passedFinanceAuditTrail() {
        return new FinanceAnswerAuditTrailService.AuditTrailReport(
                true,
                "",
                List.of("sql", "caliberRules", "lineage", "oracleStatus", "routeTrace"),
                "select account_period, voucher_count from public.xycyl_ads_finance_voucher_monthly",
                List.of(new FinanceAnswerAuditTrailService.AppliedCaliberRule(
                        "CAL-VOUCHER-HEADER-COUNT",
                        "凭证统计按有效凭证头计数。",
                        "P0",
                        "必须排除空凭证号。",
                        List.of("voucher-ledger"))),
                List.of(),
                List.of(new FinanceAnswerAuditTrailService.LineageNode(
                        "ADS_MODEL",
                        "public.xycyl_ads_finance_voucher_monthly",
                        "auditable-result-model",
                        List.of("dbt:model.xy_cyl.xycyl_ads_finance_voucher_monthly"))),
                new FinanceAnswerAuditTrailService.OracleAuditStatus(
                        "voucher-ledger",
                        "凭证账本",
                        "L2",
                        "voucher-ledger",
                        true,
                        "PASS",
                        BigDecimal.ZERO,
                        ""),
                List.of(new FinanceAnswerAuditTrailService.RouteTraceStep(
                        "TIER_2_MART_TEMPLATE",
                        "ADS 模板",
                        "HIT",
                        "命中凭证统计模板",
                        "public.xycyl_ads_finance_voucher_monthly")));
    }
}
