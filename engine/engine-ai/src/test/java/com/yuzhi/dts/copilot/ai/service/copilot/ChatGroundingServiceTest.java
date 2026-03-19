package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
        when(intentRouterService.route("hi"))
                .thenReturn(new RoutingResult(null, null, List.of(), 0.0, true));

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
        when(intentRouterService.route("帮我做个统计"))
                .thenReturn(new RoutingResult("project", "v_project_overview", List.of(), 0.0, true));
        when(intentRouterService.generateClarificationMessage())
                .thenReturn("您的问题可能涉及以下方面，请确认：\n1. 项目和客户信息");

        ChatGroundingService.GroundingContext result = chatGroundingService.buildContext("帮我做个统计");

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.clarificationMessage()).contains("您的问题可能涉及以下方面");
    }

    @Test
    @DisplayName("助手自我介绍类问题不应落到业务范围澄清")
    void assistantMetaQuestionUsesAssistantGuidance() {
        when(templateMatcherService.match("我想问下你是什么模型"))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.route("我想问下你是什么模型"))
                .thenReturn(new RoutingResult(null, null, List.of(), 0.0, true));

        ChatGroundingService.GroundingContext result = chatGroundingService.buildContext("我想问下你是什么模型");

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.clarificationMessage()).contains("我是 DTS Copilot");
        assertThat(result.clarificationMessage()).doesNotContain("您的问题可能涉及以下方面");
    }
}
