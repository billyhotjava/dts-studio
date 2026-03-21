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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateFirstRoutingTest {

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
    void highConfidenceFixedReportQuestionUsesTemplateFastPath() {
        String question = "客户欠款排行";
        Nl2SqlQueryTemplate template = buildTemplate(
                "FIN-CUSTOMER-AR-RANK",
                "财务",
                "mart.finance.customer_ar_rank_daily",
                null
        );
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(templateMatcherService.match(question)).thenReturn(new TemplateMatchResult(
                true,
                template,
                Map.of(),
                null
        ));
        when(intentRouterService.routeWithDataLayer(question, Map.of())).thenReturn(new ExtendedRoutingResult(
                new RoutingResult("财务", "v_monthly_settlement", List.of(), 0.98, false),
                DataLayer.MART,
                "mart.finance.customer_ar_rank_daily",
                false,
                null
        ));
        when(semanticPackService.getContextForDomain("财务")).thenReturn("finance semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.FIXED_REPORT);
        assertThat(plan.templateCode()).isEqualTo("FIN-CUSTOMER-AR-RANK");
        assertThat(plan.resolvedSql()).isNull();
        assertThat(plan.primaryTarget()).isEqualTo("mart.finance.customer_ar_rank_daily");
    }

    @Test
    void parameterizedVariantStillPrefersTemplateFastPath() {
        String question = "待收款明细";
        Nl2SqlQueryTemplate template = buildTemplate(
                "FIN-PENDING-RECEIPTS-DETAIL",
                "财务",
                "authority.finance.pending_receipts_detail",
                null
        );
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(templateMatcherService.match(question)).thenReturn(new TemplateMatchResult(
                true,
                template,
                Map.of(),
                null
        ));
        when(intentRouterService.routeWithDataLayer(question, Map.of())).thenReturn(new ExtendedRoutingResult(
                new RoutingResult("财务", "v_monthly_settlement", List.of(), 0.95, false),
                DataLayer.VIEW,
                null,
                false,
                null
        ));
        when(semanticPackService.getContextForDomain("财务")).thenReturn("finance semantic pack");

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.TEMPLATE_FAST_PATH);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.FIXED_REPORT);
        assertThat(plan.templateCode()).isEqualTo("FIN-PENDING-RECEIPTS-DETAIL");
        assertThat(plan.primaryTarget()).isEqualTo("v_monthly_settlement");
        assertThat(plan.resolvedSql()).isNull();
    }

    @Test
    void lowConfidenceQuestionFallsBackToExplorationWorkflow() {
        String question = "帮我看一下情况";
        when(directResponseCatalogService.findMatch(question)).thenReturn(Optional.empty());
        when(templateMatcherService.match(question)).thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(question, Map.of())).thenReturn(new ExtendedRoutingResult(
                new RoutingResult("project", "v_project_overview", List.of(), 0.0, true),
                DataLayer.VIEW,
                null,
                false,
                null
        ));

        ConversationPlan plan = policy.plan(question, Map.of());

        assertThat(plan.mode()).isEqualTo(PlanMode.AGENT_WORKFLOW);
        assertThat(plan.responseKind()).isEqualTo(ResponseKind.BUSINESS_CLARIFICATION);
        assertThat(plan.templateCode()).isNull();
        assertThat(plan.promptContext()).contains("不要直接返回固定的业务范围清单");
    }

    private Nl2SqlQueryTemplate buildTemplate(String templateCode, String domain, String targetView, String sqlTemplate) {
        Nl2SqlQueryTemplate template = new Nl2SqlQueryTemplate();
        template.setTemplateCode(templateCode);
        template.setDomain(domain);
        template.setTargetView(targetView);
        template.setSqlTemplate(sqlTemplate);
        template.setIntentPatterns("[]");
        template.setQuestionSamples("[]");
        return template;
    }
}
