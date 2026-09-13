package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan.MetricCaliber;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CopilotChatContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void writesPublishedIndicatorTraceAndEditableMetricAssumption() {
        ConversationPlan plan = new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.PUBLISHED_INDICATOR,
                null,
                "finance",
                "indicator:cash-in",
                List.of("现金流入", "回款金额"),
                null,
                null,
                "PUBLISHED_INDICATOR",
                null,
                "平台指标目录",
                "L3_PUBLISHED_INDICATOR",
                "HIGH",
                List.of("命中 dts-platform 已发布指标"),
                "table",
                "cash-in",
                List.of("platform-indicator:cash-in"),
                new MetricCaliber("现金流入", "sum(amount)", "finance", "v3", "cash-in"),
                List.of(new ConversationPlan.RouteStep(
                        "TIER_1_PUBLISHED_INDICATOR",
                        "指标优先",
                        "HIT",
                        "命中 dts-platform 已发布指标",
                        "indicator:cash-in")));
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(done, plan, null);

        assertThat(done.at("/trace/routeTrace/0/tier").asText()).isEqualTo("TIER_1_PUBLISHED_INDICATOR");
        assertThat(done.at("/trace/routeTrace/0/status").asText()).isEqualTo("HIT");
        assertThat(done.at("/trace/routeTrace/0/target").asText()).isEqualTo("indicator:cash-in");
        assertThat(done.at("/trace/metricCaliber/name").asText()).isEqualTo("现金流入");
        assertThat(done.at("/trace/metricCaliber/formula").asText()).isEqualTo("sum(amount)");
        assertThat(done.at("/trace/metricCaliber/version").asText()).isEqualTo("v3");
        JsonNode assumptions = done.path("assumptions");
        assertThat(assumptions).hasSize(4);
        JsonNode metric = assumptions.get(0);
        assertThat(metric.path("key").asText()).isEqualTo("metric");
        assertThat(metric.path("label").asText()).isEqualTo("指标");
        assertThat(metric.path("value").asText()).isEqualTo("现金流入");
        assertThat(metric.path("editable").asBoolean()).isTrue();
        assertThat(metric.path("sourceHint").asText()).contains("sum(amount)");
        assertThat(metric.path("options")).hasSize(3);
        assertThat(metric.path("options").get(2).path("value").asText()).isEqualTo("__fallback_generated__");
    }

    @Test
    void writesHighAccuracyEvidenceForAuditedFinanceAdsAnswer() {
        ConversationPlan plan = financeVoucherPlan("L3_ADS", "TPL-57");
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(
                done,
                plan,
                "select account_period, voucher_count from public.xycyl_ads_finance_voucher_monthly",
                CopilotChatRequestContext.empty(),
                passedFinanceAuditTrail());

        JsonNode evidence = done.path("accuracyEvidence");
        assertThat(evidence.path("grade").asText()).isEqualTo("HIGH");
        assertThat(evidence.path("score").asDouble()).isGreaterThanOrEqualTo(0.9d);
        assertThat(evidence.path("route").path("templateCode").asText()).isEqualTo("TPL-57");
        assertThat(evidence.path("route").path("dataSurface").asText()).isEqualTo("L3_ADS");
        assertThat(evidence.path("route").path("targetRelations").get(0).asText())
                .isEqualTo("public.xycyl_ads_finance_voucher_monthly");
        assertThat(evidence.path("sql").path("staticChecks").toString()).contains("READ_ONLY", "ALLOWED_RELATION");
        assertThat(evidence.path("tieout").path("status").asText()).isEqualTo("PASS");
        assertThat(evidence.path("reasons").toString())
                .contains("ADS")
                .contains("对账通过");
        assertThat(done.path("trace").path("accuracyEvidence").path("grade").asText()).isEqualTo("HIGH");
    }

    @Test
    void capsAdsOrDwsAnswerWithoutTieoutAtMediumAccuracy() {
        ConversationPlan plan = financeVoucherPlan("L1_DBT_MART", "TPL-57");
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(
                done,
                plan,
                "select account_period, voucher_count from public.xycyl_ads_finance_voucher_monthly");

        JsonNode evidence = done.path("accuracyEvidence");
        assertThat(evidence.path("grade").asText()).isEqualTo("MEDIUM");
        assertThat(evidence.path("score").asDouble()).isBetween(0.7d, 0.89d);
        assertThat(evidence.path("warnings").toString()).contains("缺少对账证据");
        assertThat(evidence.path("tieout").path("status").asText()).isEqualTo("MISSING");
    }

    @Test
    void downgradesL0ProfileAnswerToUntrustedAccuracy() {
        ConversationPlan plan = new ConversationPlan(
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
                List.of("business-object:finance.voucher"));
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(done, plan, null);

        JsonNode evidence = done.path("accuracyEvidence");
        assertThat(evidence.path("grade").asText()).isEqualTo("UNTRUSTED");
        assertThat(evidence.path("score").asDouble()).isLessThan(0.4d);
        assertThat(evidence.path("reasons").toString()).contains("L0_BUSINESS_OBJECT_PROFILE");
        assertThat(evidence.path("warnings").toString()).contains("统计结论");
    }

    @Test
    void exposesPersistedAccuracyEvidenceAsTopLevelMessageField() {
        AiChatMessage message = new AiChatMessage();
        ConversationPlan plan = financeVoucherPlan("L1_DBT_MART", "TPL-57");

        CopilotChatContract.applyToMessage(
                message,
                plan,
                "select account_period, voucher_count from public.xycyl_ads_finance_voucher_monthly");
        Map<String, Object> fields = new java.util.LinkedHashMap<>();
        CopilotChatContract.putMessageFields(message, fields);

        assertThat(message.getTrace()).contains("\"accuracyEvidence\"");
        assertThat(fields).containsKey("accuracyEvidence");
        @SuppressWarnings("unchecked")
        Map<String, Object> evidence = (Map<String, Object>) fields.get("accuracyEvidence");
        assertThat(evidence).containsEntry("grade", "MEDIUM");
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
