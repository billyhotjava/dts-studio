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

import com.yuzhi.dts.copilot.ai.domain.AiDataSource;
import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.AiDataSourceRepository;
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
import org.mockito.ArgumentCaptor;
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
    private AiDataSourceRepository dataSourceRepository;

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
                clientFactory,
                dataSourceRepository
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
        when(conversationPlannerService.plan("打开PRS租赁经营总览大屏", Map.of()))
                .thenReturn(new ConversationPlan(
                        PlanMode.TEMPLATE_FAST_PATH,
                        ResponseKind.FIXED_REPORT,
                        null,
                        "flowerbiz",
                        "screen.prs-flowerbiz-overview-v1",
                        List.of(),
                        "PRS-FLOWERBIZ-OVERVIEW",
                        null,
                        "VIEW",
                        null,
                        "flowerbiz grounding"));

        AgentExecutionService.ChatExecutionResult result = service.executeChat(
                "sess-1", "alice", "打开PRS租赁经营总览大屏", Collections.emptyList(), 7L, Map.of());

        assertThat(result.response()).contains("PRS-FLOWERBIZ-OVERVIEW");
        assertThat(result.response()).contains("资产库资产");
        assertThat(result.response()).contains("资产库查看");
        assertThat(result.response()).doesNotContain("打开报表查看结果");
        assertThat(result.generatedSql()).isNull();
        verify(reActEngine, never()).execute(any(), anyString(), anyList(), any(ToolContext.class), anyDouble(), anyInt());
    }

    @Test
    void executeChatStreamFixedReportFastPathWritesDoneMetadata() {
        when(conversationPlannerService.plan("打开PRS租赁经营总览大屏", Map.of()))
                .thenReturn(new ConversationPlan(
                        PlanMode.TEMPLATE_FAST_PATH,
                        ResponseKind.FIXED_REPORT,
                        null,
                        "flowerbiz",
                        "screen.prs-flowerbiz-overview-v1",
                        List.of(),
                        "PRS-FLOWERBIZ-OVERVIEW",
                        null,
                        "VIEW",
                        null,
                        "flowerbiz grounding"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        AgentExecutionService.ChatExecutionResult result = service.executeChatStream(
                "sess-1", "alice", "打开PRS租赁经营总览大屏", Collections.emptyList(), 7L, Map.of(), output);

        String sse = output.toString();
        assertThat(result.response()).contains("PRS-FLOWERBIZ-OVERVIEW");
        assertThat(sse).contains("\"templateCode\":\"PRS-FLOWERBIZ-OVERVIEW\"");
        assertThat(sse).contains("\"responseKind\":\"FIXED_REPORT\"");
        assertThat(sse).contains("\"routedDomain\":\"flowerbiz\"");
        assertThat(sse).contains("\"targetView\":\"screen.prs-flowerbiz-overview-v1\"");
        verify(reActEngine, never()).executeStreaming(any(), anyString(), anyList(), any(ToolContext.class), anyDouble(), anyInt(), any());
    }

    @Test
    void executeChatStreamReportDraftAddsReportWorkflowPromptAndDoneMetadata() {
        String question = "帮我生成一张PRS租赁项目月度趋势报表";
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(List.of());
        when(conversationPlannerService.plan(question, Map.of()))
                .thenReturn(new ConversationPlan(
                        PlanMode.AGENT_WORKFLOW,
                        ResponseKind.REPORT_DRAFT,
                        null,
                        "flowerbiz",
                        "xycyl_dws_flowerbiz_project_monthly",
                        List.of(),
                        null,
                        null,
                        "MART",
                        "xycyl_dws_flowerbiz_project_monthly",
                        "【报表草稿生成】优先使用 xycyl_dws_flowerbiz_project_monthly",
                        "L1_DBT_MART",
                        "MEDIUM",
                        List.of("2025年5月以后数据较完整，但回款和坏账字段需交叉校验"),
                        "line",
                        "prs.flowerbiz.lease_execution_monthly",
                        List.of("dbt-model:public.xycyl_dws_flowerbiz_project_monthly", "semantic-pack:flowerbiz")));
        when(providerConfigRepository.findByIsDefaultTrue())
                .thenReturn(Optional.of(buildProvider()));
        when(clientFactory.create(any())).thenReturn(llmProviderClient);
        AiDataSource dbtDataSource = new AiDataSource();
        dbtDataSource.setId(88L);
        dbtDataSource.setName("DTS dbt模型库");
        dbtDataSource.setStatus("ACTIVE");
        when(dataSourceRepository.findAllByOrderByUpdatedAtDescIdDesc()).thenReturn(List.of(dbtDataSource));
        when(reActEngine.executeStreaming(eq(llmProviderClient), eq("qwen-plus"), anyList(), any(ToolContext.class),
                eq(0.2), eq(4096), any()))
                .thenReturn("""
                        已生成报表草稿查询。

                        ```sql
                        select month_id, project_name, lease_amount
                        from xycyl_dws_flowerbiz_project_monthly
                        order by month_id
                        ```
                        """);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AgentExecutionService.ChatExecutionResult result = service.executeChatStream(
                "sess-1", "alice", question, Collections.emptyList(), 7L, Map.of(), output);

        String sse = output.toString();
        assertThat(result.generatedSql()).contains("xycyl_dws_flowerbiz_project_monthly");
        assertThat(sse).contains("\"responseKind\":\"REPORT_DRAFT\"");
        assertThat(sse).contains("\"suggestedDisplay\":\"line\"");
        assertThat(sse).contains("\"reportCode\":\"prs.flowerbiz.lease_execution_monthly\"");
        assertThat(sse).contains("\"dataSurface\":\"L1_DBT_MART\"");
        assertThat(sse).contains("\"qualityLevel\":\"MEDIUM\"");
        assertThat(sse).contains("\"qualityNotes\":[\"2025年5月以后数据较完整，但回款和坏账字段需交叉校验\"]");
        assertThat(sse).contains("\"sourceRefs\":[\"dbt-model:public.xycyl_dws_flowerbiz_project_monthly\",\"semantic-pack:flowerbiz\"]");
        assertThat(sse).contains("\"assumptions\"");
        assertThat(sse).contains("\"confidence\":0.72");
        assertThat(sse).contains("\"trace\"");
        assertThat(sse).contains("\"metricCaliber\"");
        assertThat(sse).contains("\"sql\"");
        ArgumentCaptor<List<Map<String, Object>>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<ToolContext> toolContextCaptor = ArgumentCaptor.forClass(ToolContext.class);
        verify(reActEngine).executeStreaming(eq(llmProviderClient), eq("qwen-plus"), messagesCaptor.capture(),
                toolContextCaptor.capture(), eq(0.2), eq(4096), any());
        assertThat(toolContextCaptor.getValue().dataSourceId()).isEqualTo(88L);
        assertThat(String.valueOf(messagesCaptor.getValue().getFirst().get("content")))
                .contains("报表草稿")
                .contains("创建可保存的分析草稿")
                .contains("标准 Markdown 表格")
                .contains("| 指标 | 结果 | 说明 |")
                .contains("不要只输出指标说明表，必须包含 ```sql 代码块")
                .contains("不要把资产库已有模板当成新报表草稿返回")
                .contains("先对目标 dbt 模型调用 schema_lookup")
                .contains("只能使用 schema_lookup 返回字段");
    }

    @Test
    void executeChatStreamAddsUserContractInputsToPromptAndDoneContract() {
        String question = "继续按确认口径分析利润趋势";
        Map<String, String> assumptionOverrides = Map.of("period", "2026-05");
        Map<String, String> clarificationAnswers = Map.of("target", "在租项目");
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(List.of());
        when(conversationPlannerService.plan(question, Map.of()))
                .thenReturn(new ConversationPlan(
                        PlanMode.AGENT_WORKFLOW,
                        ResponseKind.BUSINESS_CLARIFICATION,
                        "请选择要分析的项目范围",
                        "flowerbiz",
                        "public.ads_profit",
                        List.of("在租项目", "全部项目"),
                        null,
                        null,
                        "MART",
                        "public.ads_profit",
                        "利润趋势分析",
                        "L2_ADS",
                        "LOW",
                        List.of("利润=收入-成本"),
                        "line",
                        "prs.flowerbiz.profit_monthly",
                        List.of("dbt-model:public.ads_profit")));
        when(providerConfigRepository.findByIsDefaultTrue())
                .thenReturn(Optional.of(buildProvider()));
        when(clientFactory.create(any())).thenReturn(llmProviderClient);
        when(reActEngine.executeStreaming(eq(llmProviderClient), eq("qwen-plus"), anyList(), any(ToolContext.class),
                eq(0.2), eq(4096), any()))
                .thenReturn("""
                        已按确认口径重算。

                        ```sql
                        select month_id, profit from public.ads_profit
                        ```
                        """);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.executeChatStream(
                "sess-1",
                "alice",
                question,
                Collections.emptyList(),
                7L,
                Map.of(),
                assumptionOverrides,
                clarificationAnswers,
                output);

        String sse = output.toString();
        assertThat(sse)
                .contains("\"key\":\"period\"")
                .contains("\"value\":\"2026-05\"")
                .contains("\"sourceHint\":\"user_override\"")
                .doesNotContain("\"clarifications\"");
        ArgumentCaptor<List<Map<String, Object>>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(reActEngine).executeStreaming(eq(llmProviderClient), eq("qwen-plus"), messagesCaptor.capture(),
                any(ToolContext.class), eq(0.2), eq(4096), any());
        assertThat(String.valueOf(messagesCaptor.getValue().getFirst().get("content")))
                .contains("用户已确认的口径覆盖")
                .contains("period=2026-05")
                .contains("用户已回答的澄清项")
                .contains("target=在租项目");
    }

    @Test
    void executeChatStreamReportDraftRejectsNonReadOnlyGeneratedSql() {
        String question = "删除坏账测试数据";
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(List.of());
        when(conversationPlannerService.plan(question, Map.of()))
                .thenReturn(new ConversationPlan(
                        PlanMode.AGENT_WORKFLOW,
                        ResponseKind.REPORT_DRAFT,
                        null,
                        "flowerbiz",
                        "public.xycyl_ads_flowerbiz_baddebt_summary",
                        List.of(),
                        null,
                        null,
                        "MART",
                        "public.xycyl_ads_flowerbiz_baddebt_summary",
                        "【报表草稿生成】只允许生成 SELECT 报表查询",
                        "L1_DBT_MART",
                        "MEDIUM",
                        List.of("只读报表草稿"),
                        "table",
                        "prs.flowerbiz.baddebt_rank"));
        when(providerConfigRepository.findByIsDefaultTrue())
                .thenReturn(Optional.of(buildProvider()));
        when(clientFactory.create(any())).thenReturn(llmProviderClient);
        when(reActEngine.executeStreaming(eq(llmProviderClient), eq("qwen-plus"), anyList(), any(ToolContext.class),
                eq(0.2), eq(4096), any()))
                .thenReturn("""
                        不能直接删除数据，但这里是模型误返回的 SQL。

                        ```sql
                        delete from public.xycyl_ads_flowerbiz_baddebt_summary
                        ```
                        """);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AgentExecutionService.ChatExecutionResult result = service.executeChatStream(
                "sess-1", "alice", question, Collections.emptyList(), 7L, Map.of(), output);

        assertThat(result.generatedSql()).isNull();
        assertThat(output.toString()).doesNotContain("generatedSql");
    }

    @Test
    void executeChatStreamReportDraftAcceptsReadonlySqlWithLeadingComments() {
        String question = "项目经营 TOP";
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(List.of());
        when(conversationPlannerService.plan(question, Map.of()))
                .thenReturn(new ConversationPlan(
                        PlanMode.AGENT_WORKFLOW,
                        ResponseKind.REPORT_DRAFT,
                        null,
                        "project",
                        "public.xycyl_dws_flowerbiz_customer_monthly",
                        List.of(),
                        null,
                        null,
                        "MART",
                        "public.xycyl_dws_flowerbiz_customer_monthly",
                        "【报表草稿生成】优先使用 dbt 模型",
                        "L1_DBT_MART",
                        "MEDIUM",
                        List.of("客户关联完整度需核验"),
                        "bar",
                        "prs.project.customer_value",
                        List.of("dbt-model:public.xycyl_dws_flowerbiz_customer_monthly")));
        when(providerConfigRepository.findByIsDefaultTrue())
                .thenReturn(Optional.of(buildProvider()));
        when(clientFactory.create(any())).thenReturn(llmProviderClient);
        when(dataSourceRepository.findAllByOrderByUpdatedAtDescIdDesc()).thenReturn(List.of());
        when(reActEngine.executeStreaming(eq(llmProviderClient), eq("qwen-plus"), anyList(), any(ToolContext.class),
                eq(0.2), eq(4096), any()))
                .thenReturn("""
                        ```sql
                        -- 项目经营 TOP 排行
                        WITH base AS (
                            SELECT customer_name, sale_amount_finished
                            FROM public.xycyl_dws_flowerbiz_customer_monthly
                        )
                        SELECT customer_name, SUM(sale_amount_finished) AS sale_amount
                        FROM base
                        GROUP BY customer_name
                        ```
                        """);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AgentExecutionService.ChatExecutionResult result = service.executeChatStream(
                "sess-1", "alice", question, Collections.emptyList(), 7L, Map.of(), output);

        assertThat(result.generatedSql()).startsWith("WITH base AS");
        assertThat(output.toString()).contains("\"generatedSql\"");
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
