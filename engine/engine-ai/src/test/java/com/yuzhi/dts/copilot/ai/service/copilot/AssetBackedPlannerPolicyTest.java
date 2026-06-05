package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
import java.util.Arrays;
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

    @Mock
    private IndicatorMatcherService indicatorMatcherService;

    private AgentBiReportCatalogService reportCatalogService;

    private BusinessObjectCatalogService businessObjectCatalogService;

    private AssetBackedPlannerPolicy policy;

    @BeforeEach
    void setUp() {
        reportCatalogService = new AgentBiReportCatalogService();
        businessObjectCatalogService = new BusinessObjectCatalogService();
        lenient().when(indicatorMatcherService.match(anyString()))
                .thenReturn(IndicatorMatcherService.IndicatorMatchResult.none());
        policy = new AssetBackedPlannerPolicy(
                intentRouterService,
                templateMatcherService,
                semanticPackService,
                new OntologyService(semanticPackService),
                directResponseCatalogService,
                reportCatalogService,
                businessObjectCatalogService,
                indicatorMatcherService
        );
    }

    @Test
    void routeTierOrderIsExplicitAndStable() {
        Class<?> routeTierType = Arrays.stream(AssetBackedPlannerPolicy.class.getDeclaredClasses())
                .filter(type -> "RouteTier".equals(type.getSimpleName()))
                .findFirst()
                .orElse(null);

        assertThat(routeTierType).isNotNull();
        assertThat(routeTierType.isEnum()).isTrue();
        assertThat(Arrays.stream(routeTierType.getEnumConstants())
                .map(Object::toString))
                .containsExactly(
                        "TIER_1_PUBLISHED_INDICATOR",
                        "TIER_2_MART_TEMPLATE",
                        "TIER_3_ONTOLOGY_OBJECT_GRAPH",
                        "TIER_4_GUARDRAIL_FEDERATED",
                        "TIER_5_DIRECT_DETAIL");
    }

    @Test
    void routeResolutionIsExplicitResponsibilityChain() throws Exception {
        assertThat(Arrays.stream(AssetBackedPlannerPolicy.class.getDeclaredClasses())
                .map(Class::getSimpleName))
                .contains("RouteEvaluationContext", "PlanRoute");

        java.lang.reflect.Method routeChain = AssetBackedPlannerPolicy.class.getDeclaredMethod("assetRouteChain");
        routeChain.setAccessible(true);
        Object value = routeChain.invoke(policy);

        assertThat(value).isInstanceOf(List.class);
        assertThat((List<?>) value).hasSizeGreaterThanOrEqualTo(10);
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
        assertThat(plan.routeTrace())
                .extracting(
                        ConversationPlan.RouteStep::tier,
                        ConversationPlan.RouteStep::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("TIER_1_PUBLISHED_INDICATOR", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_2_MART_TEMPLATE", "HIT"));
        assertThat(plan.routeTrace().get(1).target()).isEqualTo("flowerbiz.top-project");
    }

    @Test
    void publishedIndicatorMatchHasHigherPriorityThanTemplateAndViewRouting() {
        String question = "本月现金流入是多少";
        when(indicatorMatcherService.match(question))
                .thenReturn(new IndicatorMatcherService.IndicatorMatchResult(
                        List.of(indicatorMatch("cash-in", "现金流入", "finance", "v3", 0.91d)),
                        IndicatorMatcherService.Confidence.HIGH));

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.PUBLISHED_INDICATOR);
        assertThat(plan.primaryTarget()).isEqualTo("indicator:cash-in");
        assertThat(plan.reportCode()).isEqualTo("cash-in");
        assertThat(plan.dataSurface()).isEqualTo("L3_PUBLISHED_INDICATOR");
        assertThat(plan.qualityLevel()).isEqualTo("HIGH");
        assertThat(plan.sourceRefs()).contains("platform-indicator:cash-in");
        assertThat(plan.metricCaliber()).isNotNull();
        assertThat(plan.metricCaliber().name()).isEqualTo("现金流入");
        assertThat(plan.metricCaliber().formula()).isEqualTo("sum(amount)");
        assertThat(plan.metricCaliber().version()).isEqualTo("v3");
        assertThat(plan.promptContext())
                .contains("平台指标目录")
                .contains("现金流入")
                .contains("sum(amount)");
    }

    @Test
    void metricFallbackOverrideSkipsPublishedIndicatorBranch() {
        String question = "本月现金流入是多少";
        when(indicatorMatcherService.match(question))
                .thenReturn(new IndicatorMatcherService.IndicatorMatchResult(
                        List.of(indicatorMatch("cash-in", "现金流入", "finance", "v3", 0.91d)),
                        IndicatorMatcherService.Confidence.HIGH));
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("finance", "v_finance_cash", List.of(), 0.8, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());

        ConversationPlan plan = policy.plan(
                question,
                CopilotChatRequestContext.of(
                        Map.of(),
                        Map.of("metric", "__fallback_generated__"),
                        Map.of()));

        assertThat(plan.responseKind()).isNotEqualTo(ResponseKind.PUBLISHED_INDICATOR);
        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
    }

    @Test
    void metricOverrideSelectsRequestedCandidateFromIndicatorMatches() {
        String question = "本月资金情况";
        when(indicatorMatcherService.match(question))
                .thenReturn(new IndicatorMatcherService.IndicatorMatchResult(
                        List.of(
                                indicatorMatch("cash-in", "现金流入", "finance", "v1", 0.72d),
                                indicatorMatch("cash-return", "回款金额", "finance", "v2", 0.68d)),
                        IndicatorMatcherService.Confidence.MEDIUM));

        ConversationPlan plan = policy.plan(
                question,
                CopilotChatRequestContext.of(
                        Map.of(),
                        Map.of("metric", "回款金额"),
                        Map.of()));

        assertThat(plan.responseKind()).isEqualTo(ResponseKind.PUBLISHED_INDICATOR);
        assertThat(plan.reportCode()).isEqualTo("cash-return");
        assertThat(plan.primaryTarget()).isEqualTo("indicator:cash-return");
        assertThat(plan.metricCaliber().name()).isEqualTo("回款金额");
        assertThat(plan.metricCaliber().version()).isEqualTo("v2");
    }

    @Test
    void fixedReportTemplateMatchUsesFixedReportFastPathWithoutResolvedSql() {
        String question = "打开PRS租赁经营总览大屏";
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(
                        true,
                        buildTemplate("PRS-FLOWERBIZ-OVERVIEW", "flowerbiz", "screen.prs-flowerbiz-overview-v1"),
                        Map.of(),
                        null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "screen.prs-flowerbiz-overview-v1", List.of(), 0.95, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("flowerbiz semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.FIXED_REPORT);
        assertThat(plan.templateCode()).isEqualTo("PRS-FLOWERBIZ-OVERVIEW");
        assertThat(plan.resolvedSql()).isNull();
        assertThat(plan.primaryTarget()).isEqualTo("screen.prs-flowerbiz-overview-v1");
    }

    @Test
    void procurementAuthorityTemplateUsesTemplateSqlFastPath() {
        String question = "查询2025年2月，绿萝这个产品的采购详细情况，按采购人、采购金额统计";
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(
                        true,
                        buildTemplate("TPL-33", "procurement", "authority.procurement.purchase_amount_by_buyer"),
                        Map.of("month", "2025-02", "good_name", "绿萝"),
                        "SELECT b.purchase_user_name FROM t_purchase_price_item a LEFT JOIN t_purchase_info b ON a.purchase_info_id = b.id"));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult(null, null, List.of(), 0.0, true),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(semanticPackService.getContextForDomain("procurement")).thenReturn("procurement semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.TEMPLATE_SQL);
        assertThat(plan.templateCode()).isEqualTo("TPL-33");
        assertThat(plan.primaryTarget()).isEqualTo("authority.procurement.purchase_amount_by_buyer");
        assertThat(plan.resolvedSql()).contains("t_purchase_price_item");
        assertThat(plan.resolvedSql()).doesNotContain("i_pendulum_purchase");
        assertThat(plan.resolvedSql()).doesNotContain("title like");
    }

    @Test
    void procurementTemplateMetadataOverridesGenericGreenRouting() {
        String question = "看下2026年各个绿植的采购情况";
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(
                        true,
                        buildTemplate("TPL-34", "procurement", "mysql.rs_cloud_flower.t_purchase_price_item"),
                        Map.of("year", "2026"),
                        "SELECT a.good_name FROM mysql.rs_cloud_flower.t_purchase_price_item a"));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult(
                                "green",
                                "public.xycyl_dwd_project_green_snapshot",
                                List.of(),
                                0.88,
                                false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(semanticPackService.getContextForDomain("procurement")).thenReturn("procurement semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.TEMPLATE_SQL);
        assertThat(plan.templateCode()).isEqualTo("TPL-34");
        assertThat(plan.routedDomain()).isEqualTo("procurement");
        assertThat(plan.primaryTarget()).isEqualTo("mysql.rs_cloud_flower.t_purchase_price_item");
        assertThat(plan.promptContext()).contains("procurement semantic pack");
        assertThat(plan.promptContext()).doesNotContain("public.xycyl_dwd_project_green_snapshot");
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

    @Test
    void genericPrsReportQuestionUsesUnifiedFixedReportCatalogBeforeExploration() {
        String question = "看下PRS租赁大屏";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "xycyl_dws_flowerbiz_project_monthly", List.of(), 0.18, true),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.FIXED_REPORT);
        assertThat(plan.reportCode()).isEqualTo("prs.flowerbiz.overview");
        assertThat(plan.templateCode()).isEqualTo("PRS-FLOWERBIZ-OVERVIEW");
        assertThat(plan.dataSurface()).isEqualTo("L2_FIXED_REPORT");
        assertThat(plan.primaryTarget()).isEqualTo("public.xycyl_ads_flowerbiz_overview");
        assertThat(plan.promptContext()).contains("L2 固定报表资产");
    }

    @Test
    void explicitNewReportRequestUsesUnifiedFixedReportCatalogWhenAssetExists() {
        String question = "帮我生成一张PRS租赁项目月度趋势报表";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "xycyl_dws_flowerbiz_project_monthly", List.of(), 0.82, false),
                        DataLayer.MART,
                        "xycyl_dws_flowerbiz_project_monthly",
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("flowerbiz semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.FIXED_REPORT);
        assertThat(plan.routedDomain()).isEqualTo("flowerbiz");
        assertThat(plan.reportCode()).isEqualTo("prs.flowerbiz.lease_execution_monthly");
        assertThat(plan.templateCode()).isEqualTo("PRS-FLOWERBIZ-LEASE-EXECUTION");
        assertThat(plan.dataSurface()).isEqualTo("L2_FIXED_REPORT");
        assertThat(plan.qualityLevel()).isEqualTo("MEDIUM");
        assertThat(plan.primaryTarget()).isEqualTo("public.xycyl_ads_flowerbiz_lease_summary");
        assertThat(plan.sourceRefs())
                .contains("fixed-report:PRS-FLOWERBIZ-LEASE-EXECUTION", "semantic-pack:flowerbiz");
        assertThat(plan.promptContext()).contains("L2 固定报表资产");
        assertThat(plan.routeTrace())
                .extracting(
                        ConversationPlan.RouteStep::tier,
                        ConversationPlan.RouteStep::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("TIER_1_PUBLISHED_INDICATOR", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_2_MART_TEMPLATE", "HIT"));
        assertThat(plan.routeTrace().get(1).target()).isEqualTo("PRS-FLOWERBIZ-LEASE-EXECUTION");
    }

    @Test
    void genericGeneratedReportDraftCarriesDataSurfaceAndQualityMetadata() {
        String question = "帮我生成一张运营总览统计报表";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("operations", "v_operation_summary", List.of(), 0.42, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("operations")).thenReturn("operations semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.REPORT_DRAFT);
        assertThat(plan.dataSurface()).isEqualTo("L1_SEMANTIC_VIEW");
        assertThat(plan.qualityLevel()).isEqualTo("MEDIUM");
        assertThat(plan.qualityNotes()).isNotEmpty();
        assertThat(plan.suggestedDisplay()).isEqualTo("scalar");
        assertThat(plan.promptContext()).contains("报表草稿");
    }

    @Test
    void trendQuestionUsesUnifiedFixedReportCatalogEvenWithoutGenerateVerb() {
        String question = "从2025年5月到现在，租赁收入按月趋势怎么样";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "v_flower_biz_detail", List.of(), 0.82, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("flowerbiz semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.FIXED_REPORT);
        assertThat(plan.reportCode()).isEqualTo("prs.flowerbiz.lease_execution_monthly");
        assertThat(plan.templateCode()).isEqualTo("PRS-FLOWERBIZ-LEASE-EXECUTION");
        assertThat(plan.dataSurface()).isEqualTo("L2_FIXED_REPORT");
        assertThat(plan.qualityLevel()).isEqualTo("MEDIUM");
        assertThat(plan.qualityNotes()).isNotEmpty();
        assertThat(plan.primaryTarget()).isEqualTo("public.xycyl_ads_flowerbiz_lease_summary");
        assertThat(plan.sourceRefs())
                .contains("fixed-report:PRS-FLOWERBIZ-LEASE-EXECUTION", "semantic-pack:flowerbiz");
        assertThat(plan.promptContext())
                .contains("source refs")
                .contains("dbt-model:public.xycyl_ads_flowerbiz_lease_summary");
        assertThat(plan.promptContext()).contains("L2 固定报表资产");
    }

    @Test
    void businessDetailQuestionUsesAdminApiReadonlySurface() {
        String question = "这个项目有哪些待确认账单";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("project", "v_project_overview", List.of(), 0.42, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("project")).thenReturn("project semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_DETAIL);
        assertThat(plan.reportCode()).isEqualTo("prs.rental.pending_bill_detail");
        assertThat(plan.dataSurface()).isEqualTo("L0_ADMINAPI_READONLY");
        assertThat(plan.primaryTarget()).contains("/operate/monthAccount");
        assertThat(plan.promptContext()).contains("adminapi 只读");
    }

    @Test
    void procurementDeliveryRecordStatusQuestionUsesBusinessObjectProfileSurface() {
        String question = "采购配送记录各状态有多少";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("purchase_inventory", "ods_ptr_mysql_t_delivery_info", List.of(), 0.64, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("purchase_inventory")).thenReturn("");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_INSIGHT);
        assertThat(plan.reportCode()).isEqualTo("prs.procurement.delivery_record.profile");
        assertThat(plan.dataSurface()).isEqualTo("L0_BUSINESS_OBJECT_PROFILE");
        assertThat(plan.primaryTarget()).isEqualTo("business-object:prs.procurement.delivery_record");
        assertThat(plan.sourceRefs())
                .contains("business-object:prs.procurement.delivery_record", "ods-profile:procurement.delivery_record");
        assertThat(plan.promptContext())
                .contains("采购管理 > 配送记录")
                .contains("字段画像")
                .contains("只读 ODS")
                .contains("mysql.rs_cloud_flower")
                .contains("PRODUCTION")
                .contains("| 指标 | 结果 | 说明 |");
        assertThat(plan.routeTrace())
                .extracting(
                        ConversationPlan.RouteStep::tier,
                        ConversationPlan.RouteStep::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("TIER_1_PUBLISHED_INDICATOR", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_2_MART_TEMPLATE", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_3_ONTOLOGY_OBJECT_GRAPH", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_4_GUARDRAIL_FEDERATED", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_5_DIRECT_DETAIL", "HIT"));
        assertThat(plan.routeTrace().get(4).target()).isEqualTo("business-object:prs.procurement.delivery_record");
    }

    @Test
    void flowerBizDocumentStatusQuestionUsesBusinessObjectProfileSurface() {
        String question = "报花单据状态分布";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "ods_ptr_mysql_f_flower_biz_info", List.of(), 0.72, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_INSIGHT);
        assertThat(plan.reportCode()).isEqualTo("prs.flowerbiz.biz_order.profile");
        assertThat(plan.dataSurface()).isEqualTo("L0_BUSINESS_OBJECT_PROFILE");
        assertThat(plan.primaryTarget()).isEqualTo("business-object:prs.flowerbiz.biz_order");
        assertThat(plan.promptContext()).contains("报花管理 > 报花单据");
        assertThat(plan.promptContext()).contains("项目点, 业务类型, 单号");
    }

    @Test
    void lowStockAlertUsesWarehouseBusinessObjectInsteadOfUnpublishedAsset() {
        String question = "低库存预警";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("warehouse", "ods_ptr_mysql_s_stock_info", List.of(), 0.72, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("warehouse")).thenReturn("");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_INSIGHT);
        assertThat(plan.templateCode()).isNull();
        assertThat(plan.reportCode()).isEqualTo("prs.warehouse.stock_info.profile");
        assertThat(plan.dataSurface()).isEqualTo("L0_BUSINESS_OBJECT_PROFILE");
        assertThat(plan.primaryTarget()).isEqualTo("business-object:prs.warehouse.stock_info");
        assertThat(plan.sourceRefs())
                .contains("business-object:prs.warehouse.stock_info", "mysql-table:s_stock_info");
        assertThat(plan.promptContext())
                .contains("仓库管理 > 库存管理 > 库存")
                .contains("库存现量主表是 s_stock_info")
                .doesNotContain("资产库资产")
                .doesNotContain("固定报表资产");
        assertThat(plan.routeTrace())
                .extracting(
                        ConversationPlan.RouteStep::tier,
                        ConversationPlan.RouteStep::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("TIER_1_PUBLISHED_INDICATOR", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_2_MART_TEMPLATE", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_3_ONTOLOGY_OBJECT_GRAPH", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_4_GUARDRAIL_FEDERATED", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_5_DIRECT_DETAIL", "HIT"));
        assertThat(plan.routeTrace().get(4).target()).isEqualTo("business-object:prs.warehouse.stock_info");
    }

    @Test
    void traversalQuestionUsesObjectGraphNavigationInsteadOfSingleObjectProfile() {
        String question = "从客户到项目再到租赁报花明细的全链路追溯";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "public.xycyl_ads_flowerbiz_lease_detail", List.of(), 0.86, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        lenient().when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("flowerbiz semantic pack");
        lenient().when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizGraphPack()));

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.OBJECT_GRAPH_NAVIGATION);
        assertThat(plan.dataSurface()).isEqualTo("L1_ONTOLOGY_GRAPH");
        assertThat(plan.primaryTarget()).isEqualTo("ontology:flowerbiz");
        assertThat(plan.sourceRefs()).containsExactly(
                "public.xycyl_dim_customer",
                "public.xycyl_dim_project",
                "public.xycyl_ads_flowerbiz_lease_detail");
        assertThat(plan.routeTrace())
                .extracting(
                        ConversationPlan.RouteStep::tier,
                        ConversationPlan.RouteStep::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("TIER_1_PUBLISHED_INDICATOR", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_2_MART_TEMPLATE", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_3_ONTOLOGY_OBJECT_GRAPH", "HIT"));
        assertThat(plan.routeTrace().get(2).target()).isEqualTo("ontology:flowerbiz");
        assertThat(plan.promptContext())
                .contains("对象图导航")
                .contains("LEFT JOIN public.xycyl_dim_project")
                .contains("LEFT JOIN public.xycyl_ads_flowerbiz_lease_detail")
                .contains("source refs");
    }

    @Test
    void riskQuestionUsesOntologySignalBranchBeforeReportCatalog() {
        String question = "哪些项目有坏账风险需要关注";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "public.xycyl_ads_flowerbiz_baddebt_summary", List.of(), 0.86, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        lenient().when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("flowerbiz semantic pack");
        lenient().when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizSignalPack()));

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.RISK_SIGNAL_QUERY);
        assertThat(plan.dataSurface()).isEqualTo("L2_ONTOLOGY_SIGNAL");
        assertThat(plan.primaryTarget()).isEqualTo("ontology:flowerbiz:signals");
        assertThat(plan.reportCode()).isEqualTo("ontology.flowerbiz.signals");
        assertThat(plan.sourceRefs()).containsExactly("public.xycyl_ads_flowerbiz_baddebt_summary");
        assertThat(plan.routeTrace())
                .extracting(
                        ConversationPlan.RouteStep::tier,
                        ConversationPlan.RouteStep::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("TIER_1_PUBLISHED_INDICATOR", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_2_MART_TEMPLATE", "MISS"),
                        org.assertj.core.groups.Tuple.tuple("TIER_3_ONTOLOGY_OBJECT_GRAPH", "HIT"));
        assertThat(plan.routeTrace().get(2).target()).isEqualTo("ontology:flowerbiz:signals");
        assertThat(plan.promptContext())
                .contains("预警查询")
                .contains("坏账风险")
                .contains("severity: high")
                .contains("建议发起坏账处理单草稿")
                .contains("创建坏账处理单")
                .contains("HAVING");
    }

    @Test
    void explicitMonthlyReportOpenQuestionPrefersFixedReportOverSignalKeyword() {
        String question = "打开报花月报，按月展示 PRS 租赁报花执行、收入、回收和异常波动。";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "public.xycyl_ads_flowerbiz_lease_summary", List.of(), 0.86, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("flowerbiz semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.FIXED_REPORT);
        assertThat(plan.reportCode()).isEqualTo("prs.flowerbiz.lease_execution_monthly");
        assertThat(plan.templateCode()).isEqualTo("PRS-FLOWERBIZ-LEASE-EXECUTION");
        assertThat(plan.dataSurface()).isEqualTo("L2_FIXED_REPORT");
        assertThat(plan.primaryTarget()).isEqualTo("public.xycyl_ads_flowerbiz_lease_summary");
        assertThat(plan.sourceRefs())
                .contains("fixed-report:PRS-FLOWERBIZ-LEASE-EXECUTION");
        assertThat(plan.promptContext()).contains("L2 固定报表资产");
    }

    @Test
    void projectSiteStatusQuestionUsesBusinessObjectProfileSurface() {
        String question = "项目点状态统计";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("project", "ods_ptr_mysql_p_project", List.of(), 0.68, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("project")).thenReturn("");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_INSIGHT);
        assertThat(plan.reportCode()).isEqualTo("prs.project.project_site.profile");
        assertThat(plan.dataSurface()).isEqualTo("L0_BUSINESS_OBJECT_PROFILE");
        assertThat(plan.primaryTarget()).isEqualTo("business-object:prs.project.project_site");
        assertThat(plan.promptContext()).contains("项目点管理 > 项目点");
    }

    @Test
    void financeBankStatementQuestionUsesBusinessObjectProfileSurface() {
        String question = "银行流水未核对有多少";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("finance", "ods_ptr_mysql_f_bank_statement", List.of(), 0.7, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(semanticPackService.getContextForDomain("finance")).thenReturn("");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_INSIGHT);
        assertThat(plan.reportCode()).isEqualTo("prs.finance.bank_statement.profile");
        assertThat(plan.dataSurface()).isEqualTo("L0_BUSINESS_OBJECT_PROFILE");
        assertThat(plan.primaryTarget()).isEqualTo("business-object:prs.finance.bank_statement");
        assertThat(plan.promptContext()).contains("财务管理 > 银行流水");
    }

    @Test
    void actionRequestCreatesProposalInsteadOfExecutingBusinessWrite() {
        String question = "帮我发起催收任务";
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("settlement", "v_monthly_settlement", List.of(), 0.33, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.responseKind()).isEqualTo(ResponseKind.ACTION_PROPOSAL);
        assertThat(plan.reportCode()).isEqualTo("prs.rental.collection_followup_proposal");
        assertThat(plan.dataSurface()).isEqualTo("ACTION_PROPOSAL");
        assertThat(plan.primaryTarget()).isEqualTo("rental.create_collection_followup");
        assertThat(plan.promptContext()).contains("不得直接调用业务写接口");
        assertThat(plan.promptContext()).contains("只生成动作提案");
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

    private static IndicatorMatcherService.IndicatorMatch indicatorMatch(
            String code,
            String name,
            String domain,
            String version,
            double confidence) {
        return new IndicatorMatcherService.IndicatorMatch(
                "id-" + code,
                code,
                name,
                domain,
                domain,
                name + "权威口径",
                "sum(amount)",
                version,
                confidence,
                List.of("name:" + name));
    }

    private static SemanticPackService.SemanticPack flowerbizGraphPack() {
        return new SemanticPackService.SemanticPack(
                "flowerbiz",
                "flowerbiz graph",
                List.of(
                        object("客户", "public.xycyl_dim_customer", List.of("customer_code", "customer_name")),
                        object("项目", "public.xycyl_dim_project", List.of("project_id", "customer_code")),
                        object("租赁报花明细", "public.xycyl_ads_flowerbiz_lease_detail",
                                List.of("报花单id", "项目id", "明细id"))),
                Map.of(),
                List.of(),
                List.of(),
                List.of(
                        new SemanticPackService.OntologyLink(
                                "客户_项目",
                                "客户",
                                "项目",
                                "customer_code",
                                "customer_code",
                                "1:N",
                                "可能孤儿",
                                "shared dimension may have orphan rows"),
                        new SemanticPackService.OntologyLink(
                                "项目_报花",
                                "项目",
                                "租赁报花明细",
                                "project_id",
                                "项目id",
                                "1:N",
                                "",
                                "project to lease detail")),
                List.of(),
                List.of(),
                List.of());
    }

    private static SemanticPackService.SemanticPack flowerbizSignalPack() {
        SemanticPackService.SemanticObject baddebt = objectWithMeasures(
                "坏账汇总",
                "public.xycyl_ads_flowerbiz_baddebt_summary",
                List.of("业务月份", "项目", "客户"),
                List.of("坏账成本全口径", "坏账租金损失全口径"));
        return new SemanticPackService.SemanticPack(
                "flowerbiz",
                "flowerbiz signals",
                List.of(baddebt),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new SemanticPackService.OntologyMetric(
                                "坏账租金损失",
                                "坏账汇总",
                                "SUM(\"坏账租金损失全口径\")",
                                "CNY",
                                "currency",
                                "dbt_amount:rent"),
                        new SemanticPackService.OntologyMetric(
                                "项目坏账率",
                                "坏账汇总",
                                "SUM(\"坏账租金损失全口径\") / NULLIF(SUM(\"坏账成本全口径\") + SUM(\"坏账租金损失全口径\"), 0)",
                                "%",
                                "percent",
                                "dbt_amount:rent + dbt_amount:cost")),
                List.of(new SemanticPackService.OntologySignal(
                        "坏账风险",
                        "坏账汇总",
                        "high",
                        "项目坏账率 > 0.15 AND 坏账租金损失 > 0",
                        "建议发起坏账处理单草稿",
                        List.of("创建坏账处理单"))),
                List.of());
    }

    private static SemanticPackService.SemanticObject object(String name, String view, List<String> keyDimensions) {
        return new SemanticPackService.SemanticObject(
                name,
                view,
                name + " description",
                keyDimensions,
                List.of(),
                keyDimensions,
                "");
    }

    private static SemanticPackService.SemanticObject objectWithMeasures(
            String name,
            String view,
            List<String> keyDimensions,
            List<String> keyMeasures) {
        return new SemanticPackService.SemanticObject(
                name,
                view,
                name + " description",
                keyDimensions,
                keyMeasures,
                keyDimensions,
                keyDimensions.getFirst());
    }
}
