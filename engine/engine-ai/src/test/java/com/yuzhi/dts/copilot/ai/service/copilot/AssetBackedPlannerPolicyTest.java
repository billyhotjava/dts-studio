package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import com.yuzhi.dts.copilot.ai.service.copilot.BusinessDirectResponseCatalogService.CatalogEntry;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.DataLayer;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.ExtendedRoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.SuggestedQuestion;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetBackedPlannerPolicyTest {

    @Mock
    private IntentRouterService intentRouterService;

    @Mock
    private TemplateMatcherService templateMatcherService;

    @Mock
    private SemanticPackService semanticPackService;

    @Mock
    private BusinessDirectResponseCatalogService directResponseCatalogService;

    private AssetBackedPlannerPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AssetBackedPlannerPolicy(
                intentRouterService,
                templateMatcherService,
                semanticPackService,
                directResponseCatalogService
        );
    }

    @Test
    void businessCapabilityQuestionUsesAssetDirectResponse() {
        when(templateMatcherService.match("你能分析哪些业务"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer("你能分析哪些业务", Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult(null, null, List.of(), 0.0, true),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch("你能分析哪些业务"))
                .thenReturn(Optional.of(new CatalogEntry(
                        "business-scope-overview",
                        "BUSINESS_SCOPE_OVERVIEW",
                        List.of(Pattern.compile(".*")))));
        when(semanticPackService.getDomains()).thenReturn(java.util.Set.of("project", "flowerbiz"));
        when(semanticPackService.getContextForDomain("project")).thenReturn("【主题域】项目履约主题域");
        when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("【主题域】现场业务主题域");
        when(templateMatcherService.getSuggestedQuestions(6)).thenReturn(List.of(
                new SuggestedQuestion("project.active", "project", null, "当前有多少在服项目？", null),
                new SuggestedQuestion("flowerbiz.count", "flowerbiz", null, "本月加花总共多少次？", null)
        ));

        ConversationPlan plan = policy.plan("你能分析哪些业务", Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.DIRECT_RESPONSE);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_DIRECT_RESPONSE);
        assertThat(plan.directResponse()).contains("项目履约主题域");
        assertThat(plan.directResponse()).contains("当前有多少在服项目");
    }

    @Test
    void metadataExplorationFallsThroughToAgentWorkflow() {
        when(templateMatcherService.match("帮我查询下所有表"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer("帮我查询下所有表", Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult(null, null, List.of(), 0.0, true),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch("帮我查询下所有表")).thenReturn(Optional.empty());

        ConversationPlan plan = policy.plan("帮我查询下所有表", Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.GENERIC_ANALYSIS);
    }

    @Test
    void templateMatchKeepsTemplateFastPath() {
        when(directResponseCatalogService.findMatch("本月加花最多的项目是哪个")).thenReturn(Optional.empty());
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
        when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("flowerbiz semantic pack");

        ConversationPlan plan = policy.plan("本月加花最多的项目是哪个", Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.TEMPLATE_SQL);
    }

    @Test
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
        when(directResponseCatalogService.findMatch("帮我做个统计")).thenReturn(Optional.empty());

        ConversationPlan plan = policy.plan("帮我做个统计", Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_CLARIFICATION);
        assertThat(plan.promptContext()).contains("不要直接返回固定的业务范围清单");
    }

    private Nl2SqlQueryTemplate buildTemplate(String templateCode, String domain, String targetView) {
        Nl2SqlQueryTemplate template = new Nl2SqlQueryTemplate();
        template.setTemplateCode(templateCode);
        template.setDomain(domain);
        template.setTargetView(targetView);
        template.setIntentPatterns("[]");
        template.setQuestionSamples("[]");
        template.setSqlTemplate("SELECT 1");
        return template;
    }
}
