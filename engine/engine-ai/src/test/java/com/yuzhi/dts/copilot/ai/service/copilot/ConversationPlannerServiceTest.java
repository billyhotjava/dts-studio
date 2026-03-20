package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.DataLayer;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.ExtendedRoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversationPlannerServiceTest {

    @Mock
    private IntentRouterService intentRouterService;

    @Mock
    private TemplateMatcherService templateMatcherService;

    @Mock
    private SemanticPackService semanticPackService;

    private ConversationPlannerService plannerService;

    @BeforeEach
    void setUp() {
        plannerService = new ConversationPlannerService(
                intentRouterService,
                templateMatcherService,
                semanticPackService
        );
    }

    @Test
    @DisplayName("问候语进入直接回复模式")
    void greetingUsesDirectResponseMode() {
        ConversationPlan result = plannerService.plan("hi", Map.of());

        assertThat(result.mode()).isEqualTo(PlanMode.DIRECT_RESPONSE);
        assertThat(result.responseKind()).isEqualTo(ResponseKind.GREETING_GUIDANCE);
        assertThat(result.directResponse()).contains("你好");
    }

    @Test
    @DisplayName("元数据探索问题进入 agent workflow")
    void metadataExplorationUsesAgentWorkflow() {
        when(templateMatcherService.match("帮我查询下新业务测试库1的所有表"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));

        ConversationPlan result = plannerService.plan("帮我查询下新业务测试库1的所有表", Map.of());

        assertThat(result.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(result.responseKind()).isEqualTo(ResponseKind.SCHEMA_EXPLORATION);
        assertThat(result.promptContext()).contains("schema_lookup");
    }

    @Test
    @DisplayName("模糊业务问题不再直接拦截，而是进入 agent workflow")
    void ambiguousBusinessQuestionUsesAgentWorkflowInsteadOfHardClarification() {
        when(templateMatcherService.match("帮我做个统计"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer("帮我做个统计", Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("project", "v_project_overview", List.of(), 0.0, true),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));

        ConversationPlan result = plannerService.plan("帮我做个统计", Map.of());

        assertThat(result.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(result.responseKind()).isEqualTo(ResponseKind.BUSINESS_CLARIFICATION);
        assertThat(result.directResponse()).isNull();
        assertThat(result.promptContext()).contains("不要直接返回固定的业务范围清单");
    }

    @Test
    @DisplayName("模板命中仍然保留快路径")
    void templateMatchUsesTemplateFastPath() {
        when(templateMatcherService.match("本月加花最多的项目是哪个"))
                .thenReturn(new TemplateMatchResult(
                        true,
                        buildTemplate("flowerbiz.top-project", "flowerbiz", "v_flower_biz_detail"),
                        Map.of(),
                        "SELECT * FROM v_flower_biz_detail"));
        when(intentRouterService.routeWithDataLayer("本月加花最多的项目是哪个", Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "v_flower_biz_detail", List.of(), 0.9, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));

        ConversationPlan result = plannerService.plan("本月加花最多的项目是哪个", Map.of());

        assertThat(result.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(result.responseKind()).isEqualTo(ResponseKind.TEMPLATE_SQL);
        assertThat(result.templateCode()).isEqualTo("flowerbiz.top-project");
        assertThat(result.resolvedSql()).contains("SELECT");
    }

    @Test
    @DisplayName("高置信业务分析带业务路由上下文进入 agent workflow")
    void confidentBusinessAnalysisUsesAgentWorkflowWithRoutingContext() {
        when(templateMatcherService.match("最近半年加花趋势"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer("最近半年加花趋势", Map.of("fact_field_operation_event", true)))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "v_flower_biz_detail", List.of(), 0.8, false),
                        DataLayer.MART,
                        "fact_field_operation_event",
                        false,
                        null));
        when(semanticPackService.getContextForDomain("flowerbiz"))
                .thenReturn("flowerbiz semantic pack");

        ConversationPlan result = plannerService.plan("最近半年加花趋势", Map.of("fact_field_operation_event", true));

        assertThat(result.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(result.responseKind()).isEqualTo(ResponseKind.BUSINESS_ANALYSIS);
        assertThat(result.promptContext()).contains("【业务路由】");
        assertThat(result.promptContext()).contains("data layer: MART");
        assertThat(result.primaryTarget()).isEqualTo("fact_field_operation_event");
    }

    private com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate buildTemplate(
            String templateCode,
            String domain,
            String targetView) {
        com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate template =
                new com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate();
        template.setTemplateCode(templateCode);
        template.setDomain(domain);
        template.setTargetView(targetView);
        template.setIntentPatterns("[]");
        template.setQuestionSamples("[]");
        template.setSqlTemplate("SELECT 1");
        return template;
    }
}
