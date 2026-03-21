package com.yuzhi.dts.copilot.ai.service.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.AiProviderConfigRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClient;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClientFactory;
import com.yuzhi.dts.copilot.ai.service.rag.RagService;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentExecutionServiceTest {

    @Mock
    private ReActEngine reActEngine;

    @Mock
    private RagService ragService;

    @Mock
    private AiProviderConfigRepository providerConfigRepository;

    @Mock
    private ConversationPlannerService conversationPlannerService;

    @Mock
    private LlmProviderClientFactory clientFactory;

    @Mock
    private LlmProviderClient llmProviderClient;

    private AgentExecutionService service;

    @BeforeEach
    void setUp() {
        service = new AgentExecutionService(
                reActEngine,
                ragService,
                providerConfigRepository,
                conversationPlannerService,
                clientFactory
        );
    }

    @Test
    void executeChatBusinessDirectResponseBypassesReactEngine() {
        when(conversationPlannerService.plan("你能分析哪些业务", Map.of()))
                .thenReturn(new ConversationPlan(
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

        AgentExecutionService.ChatExecutionResult result = service.executeChat(
                "sess-1", "alice", "你能分析哪些业务", Collections.emptyList(), 7L, Map.of());

        assertThat(result.response()).contains("业务分析范围");
        verify(reActEngine, never()).execute(any(), anyString(), anyList(), any(ToolContext.class), anyDouble(), anyInt());
    }

    @Test
    void executeChatAmbiguousBusinessQuestionStillUsesReactEngine() {
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(List.of());
        when(conversationPlannerService.plan("帮我做个统计", Map.of()))
                .thenReturn(new ConversationPlan(
                        PlanMode.AGENT_WORKFLOW,
                        ResponseKind.BUSINESS_CLARIFICATION,
                        null,
                        "project",
                        "v_project_overview",
                        List.of(),
                        null,
                        null,
                        "VIEW",
                        null,
                        "不要直接返回固定的业务范围清单"));
        when(providerConfigRepository.findByIsDefaultTrue())
                .thenReturn(Optional.of(buildProvider()));
        when(clientFactory.create(any())).thenReturn(llmProviderClient);
        when(reActEngine.execute(eq(llmProviderClient), eq("qwen-plus"), anyList(), any(ToolContext.class), eq(0.2), eq(4096)))
                .thenReturn("请确认统计口径");

        AgentExecutionService.ChatExecutionResult result = service.executeChat(
                "sess-1", "alice", "帮我做个统计", Collections.emptyList(), 7L, Map.of());

        assertThat(result.response()).isEqualTo("请确认统计口径");
        verify(clientFactory).create(any(AiProviderConfig.class));
        verify(reActEngine).execute(eq(llmProviderClient), eq("qwen-plus"), anyList(), any(ToolContext.class), eq(0.2), eq(4096));
    }

    @Test
    void executeChatFixedReportFastPathBypassesReactEngineWithoutResolvedSql() {
        when(conversationPlannerService.plan("客户欠款排行", Map.of()))
                .thenReturn(new ConversationPlan(
                        PlanMode.TEMPLATE_FAST_PATH,
                        ResponseKind.FIXED_REPORT,
                        null,
                        "财务",
                        "mart.finance.customer_ar_rank_daily",
                        List.of(),
                        "FIN-CUSTOMER-AR-RANK",
                        null,
                        "MART",
                        "mart.finance.customer_ar_rank_daily",
                        "finance grounding"));

        AgentExecutionService.ChatExecutionResult result = service.executeChat(
                "sess-1", "alice", "客户欠款排行", Collections.emptyList(), 7L, Map.of());

        assertThat(result.response()).contains("FIN-CUSTOMER-AR-RANK");
        assertThat(result.response()).contains("固定报表");
        assertThat(result.generatedSql()).isNull();
        verify(reActEngine, never()).execute(any(), anyString(), anyList(), any(ToolContext.class), anyDouble(), anyInt());
    }

    @Test
    void executeChatStreamFixedReportFastPathWritesDoneMetadata() {
        when(conversationPlannerService.plan("待收款明细", Map.of()))
                .thenReturn(new ConversationPlan(
                        PlanMode.TEMPLATE_FAST_PATH,
                        ResponseKind.FIXED_REPORT,
                        null,
                        "财务",
                        "authority.finance.pending_receipts_detail",
                        List.of(),
                        "FIN-PENDING-RECEIPTS-DETAIL",
                        null,
                        "VIEW",
                        null,
                        "finance grounding"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        AgentExecutionService.ChatExecutionResult result = service.executeChatStream(
                "sess-1", "alice", "待收款明细", Collections.emptyList(), 7L, Map.of(), output);

        String sse = output.toString();
        assertThat(result.response()).contains("FIN-PENDING-RECEIPTS-DETAIL");
        assertThat(sse).contains("\"templateCode\":\"FIN-PENDING-RECEIPTS-DETAIL\"");
        assertThat(sse).contains("\"responseKind\":\"FIXED_REPORT\"");
        assertThat(sse).contains("\"routedDomain\":\"财务\"");
        assertThat(sse).contains("\"targetView\":\"authority.finance.pending_receipts_detail\"");
        verify(reActEngine, never()).executeStreaming(any(), anyString(), anyList(), any(ToolContext.class), anyDouble(), anyInt(), any());
    }

    private AiProviderConfig buildProvider() {
        AiProviderConfig provider = new AiProviderConfig();
        provider.setName("qwen");
        provider.setBaseUrl("https://example.test");
        provider.setApiKey("test-key");
        provider.setModel("qwen-plus");
        provider.setTemperature(0.2);
        provider.setMaxTokens(4096);
        provider.setTimeoutSeconds(120);
        provider.setEnabled(true);
        provider.setIsDefault(true);
        return provider;
    }
}
