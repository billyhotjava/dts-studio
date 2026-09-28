package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConversationPlannerServiceTest {

    @Test
    void defaultsToAssetPlannerPolicy() {
        PlannerPolicy assetPolicy = new StubPlannerPolicy(
                "asset",
                new ConversationPlan(
                        PlanMode.AGENT_WORKFLOW,
                        ResponseKind.GENERIC_ANALYSIS,
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        "VIEW",
                        null,
                        "asset-plan"));
        ConversationPlannerService service = new ConversationPlannerService(List.of(assetPolicy), "asset");

        ConversationPlan plan = service.plan("帮我查下数据", Map.of());

        assertThat(plan.promptContext()).isEqualTo("asset-plan");
    }

    @Test
    void fallsBackToAssetWhenConfiguredModeIsUnavailable() {
        PlannerPolicy assetPolicy = new StubPlannerPolicy(
                "asset",
                new ConversationPlan(
                        PlanMode.DIRECT_RESPONSE,
                        ResponseKind.BUSINESS_DIRECT_RESPONSE,
                        "当前已沉淀的业务分析范围包括：",
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        "VIEW",
                        null,
                        ""));
        ConversationPlannerService service = new ConversationPlannerService(List.of(assetPolicy), "llm");

        ConversationPlan plan = service.plan("你能分析哪些业务", Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.DIRECT_RESPONSE);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_DIRECT_RESPONSE);
    }

    private record StubPlannerPolicy(String mode, ConversationPlan plan) implements PlannerPolicy {
        @Override
        public ConversationPlan plan(String userQuestion, Map<String, Boolean> martHealthSnapshot) {
            return plan;
        }
    }
}
