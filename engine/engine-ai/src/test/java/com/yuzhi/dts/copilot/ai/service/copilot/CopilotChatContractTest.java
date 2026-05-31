package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan.MetricCaliber;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import java.util.List;
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
                new MetricCaliber("现金流入", "sum(amount)", "finance", "v3", "cash-in"));
        ObjectNode done = MAPPER.createObjectNode();

        CopilotChatContract.putDoneFields(done, plan, null);

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
}
