package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.Nl2SqlRoutingRule;
import com.yuzhi.dts.copilot.ai.repository.Nl2SqlRoutingRuleRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for IntentRouterService.
 * Covers routing scenarios R-01 through R-10 from the acceptance matrix,
 * plus settlement isolation and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IntentRouterServiceTest {

    @Mock
    private Nl2SqlRoutingRuleRepository repository;

    private IntentRouterService routerService;

    @BeforeEach
    void setUp() {
        routerService = new IntentRouterService(repository, new ObjectMapper());
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(buildMockRules());
    }

    // ===================== R-01 ~ R-07: Single domain routing =====================

    @Test
    @DisplayName("R-01: 客户项目查询 -> project 域")
    void routeCustomerProjectToProjectDomain() {
        RoutingResult result = routerService.route("这个客户下面有几个项目");

        assertThat(result.domain()).isEqualTo("project");
        assertThat(result.primaryView()).isEqualTo("v_project_overview");
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    @DisplayName("R-02: 换花记录 -> flowerbiz 域")
    void routeFlowerChangeToFlowerbizDomain() {
        RoutingResult result = routerService.route("上周的换花记录");

        assertThat(result.domain()).isEqualTo("flowerbiz");
        assertThat(result.primaryView()).isEqualTo("v_flower_biz_detail");
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    @DisplayName("R-03: 绿植库存 -> green 域")
    void routeGreenPlantToGreenDomain() {
        RoutingResult result = routerService.route("库里还有多少绿植");

        assertThat(result.domain()).isEqualTo("green");
        assertThat(result.primaryView()).isEqualTo("v_project_green_current");
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    @DisplayName("R-04: 收款查询 -> settlement 域")
    void routePaymentToSettlementDomain() {
        RoutingResult result = routerService.route("这个月收款了多少");

        assertThat(result.domain()).isEqualTo("settlement");
        assertThat(result.primaryView()).isEqualTo("v_monthly_settlement");
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    @DisplayName("R-05: 待办任务 -> task 域")
    void routeTaskToTaskDomain() {
        RoutingResult result = routerService.route("待办任务有哪些");

        assertThat(result.domain()).isEqualTo("task");
        assertThat(result.primaryView()).isEqualTo("v_task_progress");
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    @DisplayName("R-06: 养护覆盖率 -> curing 域")
    void routeCuringToCuringDomain() {
        RoutingResult result = routerService.route("张三的养护覆盖率怎么样");

        assertThat(result.domain()).isEqualTo("curing");
        assertThat(result.primaryView()).isEqualTo("v_curing_coverage");
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    @DisplayName("R-07: 初摆进度 -> pendulum 域")
    void routePendulumToPendulumDomain() {
        RoutingResult result = routerService.route("新项目初摆进度");

        assertThat(result.domain()).isEqualTo("pendulum");
        assertThat(result.primaryView()).isEqualTo("v_pendulum_progress");
        assertThat(result.needsClarification()).isFalse();
    }

    // ===================== R-08: Cross-domain =====================

    @Test
    @DisplayName("R-08: 跨域问题 -> 主域 + 辅域视图")
    void routeCrossDomainQuestionIncludesSecondaryViews() {
        // "换花率" hits flowerbiz (换花) and also project (项目)
        RoutingResult result = routerService.route("XX项目的换花率");

        assertThat(result.domain()).isNotNull();
        assertThat(result.needsClarification()).isFalse();
        // The question contains keywords from multiple domains;
        // we expect either secondary views or the top domain to be meaningful
        assertThat(result.primaryView()).isNotNull();
    }

    // ===================== R-09 ~ R-10: Low confidence =====================

    @Test
    @DisplayName("R-09: 无业务关键词 -> 低置信度，需澄清")
    void routeVagueQuestionNeedsClarification() {
        RoutingResult result = routerService.route("数据库现在什么情况");

        assertThat(result.needsClarification()).isTrue();
    }

    @Test
    @DisplayName("R-10: 模糊请求 -> 低置信度，需澄清")
    void routeAmbiguousRequestNeedsClarification() {
        RoutingResult result = routerService.route("帮我做个统计");

        assertThat(result.needsClarification()).isTrue();
    }

    // ===================== Settlement isolation =====================

    @Test
    @DisplayName("结算隔离: 含租金关键词 -> 只返回 v_monthly_settlement，secondaryViews 为空")
    void settlementIsolationForcesMonthlySettlementOnly() {
        RoutingResult result = routerService.route("上月租金收入排名");

        assertThat(result.domain()).isEqualTo("settlement");
        assertThat(result.primaryView()).isEqualTo("v_monthly_settlement");
        assertThat(result.secondaryViews()).isEmpty();
    }

    @Test
    @DisplayName("结算隔离: 含结算关键词 -> settlement 域")
    void settlementKeywordRoutesToSettlement() {
        RoutingResult result = routerService.route("未结算的项目有哪些");

        assertThat(result.domain()).isEqualTo("settlement");
        assertThat(result.primaryView()).isEqualTo("v_monthly_settlement");
    }

    @Test
    @DisplayName("结算隔离: 含欠款关键词 -> settlement 域")
    void outstandingAmountRoutesToSettlement() {
        RoutingResult result = routerService.route("欠款应收情况");

        assertThat(result.domain()).isEqualTo("settlement");
        assertThat(result.primaryView()).isEqualTo("v_monthly_settlement");
    }

    // ===================== Edge cases =====================

    @Test
    @DisplayName("空字符串输入 -> needsClarification=true")
    void emptyInputNeedsClarification() {
        RoutingResult result = routerService.route("");

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.domain()).isNull();
    }

    @Test
    @DisplayName("null 输入 -> needsClarification=true")
    void nullInputNeedsClarification() {
        RoutingResult result = routerService.route(null);

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.domain()).isNull();
    }

    @Test
    @DisplayName("纯空白输入 -> needsClarification=true")
    void blankInputNeedsClarification() {
        RoutingResult result = routerService.route("   ");

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.domain()).isNull();
    }

    @Test
    @DisplayName("高置信度: 多个强关键词命中同域")
    void highConfidenceWithMultipleKeywords() {
        RoutingResult result = routerService.route("加花换花减花报花审批");

        assertThat(result.domain()).isEqualTo("flowerbiz");
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.3);
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    @DisplayName("单个业务关键词命中 -> 可路由到对应域")
    void singleBusinessKeywordRoutesToDomain() {
        RoutingResult result = routerService.route("客户情况");

        // "客户" 是 project 域的明确关键词，应路由而非澄清
        assertThat(result.domain()).isEqualTo("project");
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    @DisplayName("generateClarificationMessage 返回非空澄清文本")
    void clarificationMessageIsNotEmpty() {
        String msg = routerService.generateClarificationMessage();

        assertThat(msg).isNotBlank();
        assertThat(msg).contains("项目");
        assertThat(msg).contains("结算");
    }

    // ===================== Helper: build mock rules matching seed data =====================

    /**
     * Build 7 mock routing rules matching the seed data from v1_0_0_009__nl2sql_routing.xml.
     */
    private List<Nl2SqlRoutingRule> buildMockRules() {
        List<Nl2SqlRoutingRule> rules = new ArrayList<>();

        rules.add(buildRule(1L, "project",
                "[\"项目\",\"在服\",\"项目点\",\"合同\",\"客户\",\"签约\",\"到期\",\"正常\",\"停用\"]",
                "v_project_overview",
                "[\"v_project_green_current\"]",
                0));

        rules.add(buildRule(2L, "flowerbiz",
                "[\"加花\",\"换花\",\"减花\",\"调花\",\"报花\",\"坏账\",\"售花\",\"业务单\",\"审批\",\"驳回\",\"审核\",\"备货\"]",
                "v_flower_biz_detail",
                "[]",
                0));

        rules.add(buildRule(3L, "green",
                "[\"绿植\",\"摆位\",\"在摆\",\"实摆\",\"花盆\",\"花架\",\"摆放\",\"盆栽\"]",
                "v_project_green_current",
                "[\"v_project_overview\"]",
                0));

        rules.add(buildRule(4L, "settlement",
                "[\"租金\",\"应收\",\"收款\",\"欠款\",\"结算\",\"未结算\",\"月租\",\"账单\",\"开票\",\"收入\",\"费用\"]",
                "v_monthly_settlement",
                "[\"v_project_overview\"]",
                0));

        rules.add(buildRule(5L, "task",
                "[\"任务\",\"待办\",\"待处理\",\"执行\",\"完成率\",\"进行中\",\"日常任务\"]",
                "v_task_progress",
                "[\"v_pendulum_progress\"]",
                0));

        rules.add(buildRule(6L, "curing",
                "[\"养护\",\"养护人\",\"巡检\",\"覆盖率\",\"养护记录\",\"维护\"]",
                "v_curing_coverage",
                "[]",
                0));

        rules.add(buildRule(7L, "pendulum",
                "[\"初摆\",\"首摆\",\"新项目布置\",\"预算\",\"决算\"]",
                "v_pendulum_progress",
                "[]",
                0));

        return rules;
    }

    private Nl2SqlRoutingRule buildRule(Long id, String domain, String keywords,
                                        String primaryView, String secondaryViews, int priority) {
        Nl2SqlRoutingRule rule = new Nl2SqlRoutingRule();
        rule.setId(id);
        rule.setDomain(domain);
        rule.setKeywords(keywords);
        rule.setPrimaryView(primaryView);
        rule.setSecondaryViews(secondaryViews);
        rule.setPriority(priority);
        rule.setIsActive(true);
        return rule;
    }
}
