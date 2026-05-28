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

    private AgentBiReportCatalogService reportCatalogService;

    private BusinessObjectCatalogService businessObjectCatalogService;

    private AssetBackedPlannerPolicy policy;

    @BeforeEach
    void setUp() {
        reportCatalogService = new AgentBiReportCatalogService();
        businessObjectCatalogService = new BusinessObjectCatalogService();
        policy = new AssetBackedPlannerPolicy(
                intentRouterService,
                templateMatcherService,
                semanticPackService,
                directResponseCatalogService,
                reportCatalogService,
                businessObjectCatalogService
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
    void genericPrsReportQuestionReturnsScreenFixedReportCandidatesBeforeExploration() {
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
        when(templateMatcherService.getFixedReportSuggestionsByDomain("flowerbiz", 3)).thenReturn(List.of(
                new SuggestedQuestion("PRS-FLOWERBIZ-OVERVIEW", "flowerbiz", "flowerbiz", "PRS 租赁经营总览", "desc"),
                new SuggestedQuestion("PRS-FLOWERBIZ-LEASE-EXECUTION", "flowerbiz", "flowerbiz", "PRS 租赁报花执行看板", "desc"),
                new SuggestedQuestion("PRS-FLOWERBIZ-FINANCE-COST", "flowerbiz", "flowerbiz", "PRS 销售坏账与费用看板", "desc")
        ));

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.DIRECT_RESPONSE);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.FIXED_REPORT_CANDIDATES);
        assertThat(plan.directResponse()).contains("PRS 租赁经营总览");
        assertThat(plan.directResponse()).contains("PRS 销售坏账与费用看板");
    }

    @Test
    void explicitNewReportRequestUsesAgentGeneratedReportDraftInsteadOfFixedReportCandidates() {
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

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.REPORT_DRAFT);
        assertThat(plan.reportCode()).isEqualTo("prs.flowerbiz.lease_execution_monthly");
        assertThat(plan.dataSurface()).isEqualTo("L1_DBT_MART");
        assertThat(plan.qualityLevel()).isEqualTo("MEDIUM");
        assertThat(plan.promptContext()).contains("报表草稿");
        assertThat(plan.promptContext()).contains("xycyl_dws_flowerbiz_project_monthly");
        assertThat(plan.promptContext()).contains("先对 data surface 指定的 dbt 模型调用 schema_lookup");
        assertThat(plan.promptContext()).contains("只能使用 schema_lookup 返回字段");
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
    void trendQuestionUsesAgentBiCatalogReportDraftEvenWithoutGenerateVerb() {
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

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.REPORT_DRAFT);
        assertThat(plan.reportCode()).isEqualTo("prs.flowerbiz.lease_execution_monthly");
        assertThat(plan.dataSurface()).isEqualTo("L1_DBT_MART");
        assertThat(plan.qualityLevel()).isEqualTo("MEDIUM");
        assertThat(plan.qualityNotes()).isNotEmpty();
        assertThat(plan.primaryTarget()).isEqualTo("public.xycyl_dws_flowerbiz_project_monthly");
        assertThat(plan.sourceRefs())
                .contains("dbt-model:public.xycyl_dws_flowerbiz_project_monthly", "semantic-pack:flowerbiz");
        assertThat(plan.promptContext())
                .contains("source refs")
                .contains("dbt-model:public.xycyl_dws_flowerbiz_project_monthly");
        assertThat(plan.promptContext()).contains("先对 data surface 指定的 dbt 模型调用 schema_lookup");
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
                .contains("| 指标 | 结果 | 说明 |");
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
}
