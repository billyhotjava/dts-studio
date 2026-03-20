package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
class ChatGroundingServiceTest {

    @Mock
    private IntentRouterService intentRouterService;

    @Mock
    private TemplateMatcherService templateMatcherService;

    @Mock
    private SemanticPackService semanticPackService;

    private ChatGroundingService chatGroundingService;

    @BeforeEach
    void setUp() {
        chatGroundingService = new ChatGroundingService(
                intentRouterService,
                templateMatcherService,
                semanticPackService
        );
    }

    @Test
    @DisplayName("问候语不应落到业务范围澄清文案")
    void greetingUsesFriendlyGuidanceInsteadOfBusinessClarification() {
        when(templateMatcherService.match("hi"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));

        ChatGroundingService.GroundingContext result = chatGroundingService.buildContext("hi");

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.clarificationMessage()).contains("你好");
        assertThat(result.clarificationMessage()).doesNotContain("您的问题可能涉及以下方面");
    }

    @Test
    @DisplayName("模糊业务问题仍然返回业务范围澄清")
    void ambiguousBusinessQuestionStillUsesBusinessClarification() {
        when(templateMatcherService.match("帮我做个统计"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer("帮我做个统计", Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("project", "v_project_overview", List.of(), 0.0, true),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(intentRouterService.generateClarificationMessage())
                .thenReturn("您的问题可能涉及以下方面，请确认：\n1. 项目和客户信息");

        ChatGroundingService.GroundingContext result =
                chatGroundingService.buildContext("帮我做个统计", Map.of());

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.clarificationMessage()).contains("您的问题可能涉及以下方面");
    }

    @Test
    @DisplayName("助手自我介绍类问题不应落到业务范围澄清")
    void assistantMetaQuestionUsesAssistantGuidance() {
        when(templateMatcherService.match("我想问下你是什么模型"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));

        ChatGroundingService.GroundingContext result = chatGroundingService.buildContext("我想问下你是什么模型");

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.clarificationMessage()).contains("我是 DTS Copilot");
        assertThat(result.clarificationMessage()).doesNotContain("您的问题可能涉及以下方面");
    }

    @Test
    @DisplayName("库表探索问题不应被业务范围澄清拦截")
    void metadataExplorationQuestionBypassesBusinessClarification() {
        when(templateMatcherService.match("帮我查询下新业务测试库1的所有表"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer("帮我查询下新业务测试库1的所有表", Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("project", "v_project_overview", List.of(), 0.0, true),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));

        ChatGroundingService.GroundingContext result =
                chatGroundingService.buildContext("帮我查询下新业务测试库1的所有表", Map.of());

        assertThat(result.needsClarification()).isFalse();
        assertThat(result.clarificationMessage()).isNull();
        assertThat(result.promptContext()).contains("schema_lookup");
        assertThat(result.promptContext()).doesNotContain("您的问题可能涉及以下方面");
    }

    @Test
    @DisplayName("主题层健康时，主链使用 MART 目标表")
    void healthyMartRoutingUsesMartTableInGroundingContext() {
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

        ChatGroundingService.GroundingContext result =
                chatGroundingService.buildContext("最近半年加花趋势", Map.of("fact_field_operation_event", true));

        assertThat(result.needsClarification()).isFalse();
        assertThat(result.primaryView()).isEqualTo("fact_field_operation_event");
        assertThat(result.promptContext()).contains("data layer: MART");
        assertThat(result.promptContext()).contains("fact_field_operation_event");
    }

    @Test
    @DisplayName("主题层不健康时，主链回退到 VIEW")
    void unhealthyMartRoutingFallsBackToViewInGroundingContext() {
        when(templateMatcherService.match("最近半年加花趋势"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer("最近半年加花趋势", Map.of("fact_field_operation_event", false)))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult("flowerbiz", "v_flower_biz_detail", List.of(), 0.8, false),
                        DataLayer.VIEW,
                        null,
                        true,
                        "mart unavailable: fact_field_operation_event"));
        when(semanticPackService.getContextForDomain("flowerbiz"))
                .thenReturn("flowerbiz semantic pack");

        ChatGroundingService.GroundingContext result =
                chatGroundingService.buildContext("最近半年加花趋势", Map.of("fact_field_operation_event", false));

        assertThat(result.needsClarification()).isFalse();
        assertThat(result.primaryView()).isEqualTo("v_flower_biz_detail");
        assertThat(result.promptContext()).contains("data layer: VIEW");
        assertThat(result.promptContext()).contains("fallback: mart unavailable: fact_field_operation_event");
    }
}
