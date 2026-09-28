package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.DataLayer;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.ExtendedRoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Sprint-26 F0/T03 报花域 NL2SQL 路由基线。
 *
 * <p>目的：F1/F2/F3 会在 {@link AssetBackedPlannerPolicy#plan} 决策树中插入「对象图导航 / 预警查询 /
 * Action 建议」分支。本测试锁定报花域现有问句的路由契约，作为「不退化」的回归网：
 * <ul>
 *   <li>B01-B08：8 条 fewShot 在命中 query_template 时走 TEMPLATE_FAST_PATH/TEMPLATE_SQL，
 *       primaryTarget 指向对应 ADS 视图。</li>
 *   <li>B10：单据状态画像走 L0 业务对象画像（BUSINESS_INSIGHT），F1 对象图导航不得抢占。</li>
 * </ul>
 *
 * <p>口径说明：plan() 本身确定性、不调 LLM；路由/模板服务在生产中由 DB 数据驱动，此处按生产命中形态
 * 显式建模其输出，锁定的是决策树本身的分支契约。对照集见
 * {@code worklog/v1.0.0/sprint-26-202605/it/sql/flowerbiz_baseline_questions.tsv}。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FlowerbizNl2SqlBaselineTest {

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
                new OntologyService(semanticPackService),
                directResponseCatalogService,
                reportCatalogService,
                businessObjectCatalogService
        );
    }

    static List<Arguments> flowerbizFewShotTemplateBaseline() {
        return List.of(
                Arguments.of("B01", "本月各项目加摆撤摆净增减多少？", "FLOWERBIZ-LEASE-SUMMARY",
                        "public.xycyl_ads_flowerbiz_lease_summary"),
                Arguments.of("B02", "万象城最近的报花单", "FLOWERBIZ-LEASE-DETAIL",
                        "public.xycyl_ads_flowerbiz_lease_detail"),
                Arguments.of("B03", "审核中超过 7 天的报花单", "FLOWERBIZ-PENDING",
                        "public.xycyl_ads_flowerbiz_pending"),
                Arguments.of("B04", "本月销售金额前 10", "FLOWERBIZ-SALE-SUMMARY",
                        "public.xycyl_ads_flowerbiz_sale_summary"),
                Arguments.of("B05", "本月坏账项目排行", "FLOWERBIZ-BADDEBT-SUMMARY",
                        "public.xycyl_ads_flowerbiz_baddebt_summary"),
                Arguments.of("B06", "李师傅本月经手多少报花单", "FLOWERBIZ-CURING-WORKLOAD",
                        "public.xycyl_ads_flowerbiz_curing_workload"),
                Arguments.of("B07", "近三个月变更类型分布", "FLOWERBIZ-CHANGE-LOG",
                        "public.xycyl_ads_flowerbiz_change_log"),
                Arguments.of("B08", "本月回收去向分布", "FLOWERBIZ-RECOVERY-DETAIL",
                        "public.xycyl_ads_flowerbiz_recovery_detail")
        );
    }

    @ParameterizedTest(name = "{0} {1} -> TEMPLATE_SQL @ {3}")
    @MethodSource("flowerbizFewShotTemplateBaseline")
    @DisplayName("报花 fewShot 命中模板走 TEMPLATE_FAST_PATH 且 primaryTarget 指向对应 ADS")
    void flowerbizFewShotKeepsTemplateFastPath(String id, String question, String templateCode, String adsView) {
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(templateMatcherService.match(question))
                .thenReturn(new TemplateMatchResult(
                        true,
                        buildTemplate(templateCode, "flowerbiz", adsView),
                        Map.of(),
                        "SELECT 1 FROM " + adsView));
        when(intentRouterService.routeWithDataLayer(question, Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult(null, null, List.of(), 0.0, true),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(semanticPackService.getContextForDomain("flowerbiz")).thenReturn("flowerbiz semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).as("%s mode", id).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).as("%s kind", id).isEqualTo(ResponseKind.TEMPLATE_SQL);
        assertThat(plan.templateCode()).as("%s templateCode", id).isEqualTo(templateCode);
        assertThat(plan.primaryTarget()).as("%s primaryTarget", id).isEqualTo(adsView);
        assertThat(plan.resolvedSql()).as("%s sql", id).contains(adsView);
    }

    @Test
    @DisplayName("B10 报花单据状态分布走 L0 业务对象画像（F1 对象图导航不得抢占）")
    void flowerbizDocumentStatusKeepsBusinessObjectProfile() {
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
        assertThat(plan.dataSurface()).isEqualTo("L0_BUSINESS_OBJECT_PROFILE");
        assertThat(plan.primaryTarget()).isEqualTo("business-object:prs.flowerbiz.biz_order");
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
