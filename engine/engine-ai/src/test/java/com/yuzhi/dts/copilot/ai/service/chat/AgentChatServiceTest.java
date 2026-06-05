package com.yuzhi.dts.copilot.ai.service.chat;

import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.repository.AiChatSessionRepository;
import com.yuzhi.dts.copilot.ai.service.agent.AgentExecutionService;
import com.yuzhi.dts.copilot.ai.service.agent.AgentExecutionService.ChatExecutionResult;
import com.yuzhi.dts.copilot.ai.service.audit.AiAuditService;
import com.yuzhi.dts.copilot.ai.service.copilot.CopilotChatRequestContext;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceAnswerAuditTrailService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentChatServiceTest {

    @Test
    void sendMessagePersistsPlannerResponseKindOnAssistantMessage() {
        AiChatSessionRepository sessionRepository = mock(AiChatSessionRepository.class);
        AgentExecutionService agentExecutionService = mock(AgentExecutionService.class);
        AiAuditService auditService = mock(AiAuditService.class);
        RouteTelemetryService routeTelemetryService = mock(RouteTelemetryService.class);

        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-1");
        session.setUserId("alice");
        session.setStatus("ACTIVE");

        when(sessionRepository.findBySessionId("sess-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentExecutionService.executeChat(
                eq("sess-1"), eq("alice"), eq("你能分析哪些业务"), anyList(), eq(7L), anyMap()))
                .thenReturn(new ChatExecutionResult(
                        "当前已沉淀的业务分析范围包括：项目履约、现场运营、经营分析。",
                        null,
                        new ConversationPlan(
                                PlanMode.DIRECT_RESPONSE,
                                ResponseKind.BUSINESS_DIRECT_RESPONSE,
                                "当前已沉淀的业务分析范围包括：项目履约、现场运营、经营分析。",
                                "project",
                                null,
                                List.of(),
                                null,
                                null,
                                "VIEW",
                                null,
                                "业务范围说明"),
                        null));

        AgentChatService service = new AgentChatService(
                sessionRepository, agentExecutionService, auditService, routeTelemetryService);

        String response = service.sendMessage("sess-1", "alice", "你能分析哪些业务", 7L, Map.of());

        assertThat(response).contains("业务分析范围");
        assertThat(session.getMessages()).hasSize(2);
        AiChatMessage assistantMessage = session.getMessages().get(1);
        assertThat(assistantMessage.getResponseKind()).isEqualTo("BUSINESS_DIRECT_RESPONSE");
        assertThat(assistantMessage.getRoutedDomain()).isEqualTo("project");
        verify(sessionRepository).save(session);
    }

    @Test
    void sendMessagePersistsAgentBiReportMetadataOnAssistantMessage() {
        AiChatSessionRepository sessionRepository = mock(AiChatSessionRepository.class);
        AgentExecutionService agentExecutionService = mock(AgentExecutionService.class);
        AiAuditService auditService = mock(AiAuditService.class);
        RouteTelemetryService routeTelemetryService = mock(RouteTelemetryService.class);

        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-1");
        session.setUserId("alice");
        session.setStatus("ACTIVE");

        when(sessionRepository.findBySessionId("sess-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentExecutionService.executeChat(
                eq("sess-1"), eq("alice"), eq("租赁收入按月趋势怎么样"), anyList(), eq(7L), anyMap()))
                .thenReturn(new ChatExecutionResult(
                        "已生成报表草稿。",
                        "select month_id, lease_amount from public.xycyl_dws_flowerbiz_project_monthly",
                        new ConversationPlan(
                                PlanMode.AGENT_WORKFLOW,
                                ResponseKind.REPORT_DRAFT,
                                null,
                                "flowerbiz",
                                "public.xycyl_dws_flowerbiz_project_monthly",
                                List.of(),
                                null,
                                null,
                                "MART",
                                "public.xycyl_dws_flowerbiz_project_monthly",
                                "Agent BI 报表目录",
                                "L1_DBT_MART",
                                "MEDIUM",
                                List.of("2025年5月以后租赁项目数据较可用"),
                                "line",
                                "prs.flowerbiz.lease_execution_monthly",
                                List.of("dbt-model:public.xycyl_dws_flowerbiz_project_monthly")),
                        null));

        AgentChatService service = new AgentChatService(
                sessionRepository, agentExecutionService, auditService, routeTelemetryService);

        service.sendMessage("sess-1", "alice", "租赁收入按月趋势怎么样", 7L, Map.of());

        AiChatMessage assistantMessage = session.getMessages().get(1);
        assertThat(assistantMessage.getResponseKind()).isEqualTo("REPORT_DRAFT");
        assertThat(assistantMessage.getDataSurface()).isEqualTo("L1_DBT_MART");
        assertThat(assistantMessage.getQualityLevel()).isEqualTo("MEDIUM");
        assertThat(assistantMessage.getQualityNotes()).isEqualTo("2025年5月以后租赁项目数据较可用");
        assertThat(assistantMessage.getSuggestedDisplay()).isEqualTo("line");
        assertThat(assistantMessage.getReportCode()).isEqualTo("prs.flowerbiz.lease_execution_monthly");
        assertThat(assistantMessage.getSourceRefs()).isEqualTo("dbt-model:public.xycyl_dws_flowerbiz_project_monthly");
        verify(routeTelemetryService).attachQuestion(assistantMessage, "租赁收入按月趋势怎么样");
    }

    @Test
    void sendMessagePersistsSprint27ContractMetadataOnAssistantMessage() {
        AiChatSessionRepository sessionRepository = mock(AiChatSessionRepository.class);
        AgentExecutionService agentExecutionService = mock(AgentExecutionService.class);
        AiAuditService auditService = mock(AiAuditService.class);

        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-1");
        session.setUserId("alice");
        session.setStatus("ACTIVE");

        when(sessionRepository.findBySessionId("sess-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentExecutionService.executeChat(
                eq("sess-1"), eq("alice"), eq("利润趋势"), anyList(), eq(7L), anyMap()))
                .thenReturn(new ChatExecutionResult(
                        "已生成利润趋势。",
                        "select month_id, profit from public.ads_profit",
                        new ConversationPlan(
                                PlanMode.AGENT_WORKFLOW,
                                ResponseKind.REPORT_DRAFT,
                                null,
                                "flowerbiz",
                                "public.ads_profit",
                                List.of(),
                                null,
                                null,
                                "MART",
                                "public.ads_profit",
                                "利润趋势分析",
                                "L2_ADS",
                                "HIGH",
                                List.of("利润=收入-成本"),
                                "line",
                                "prs.flowerbiz.profit_monthly",
                                List.of("dbt-model:public.ads_profit")),
                        null));

        AgentChatService service = new AgentChatService(sessionRepository, agentExecutionService, auditService);

        service.sendMessage("sess-1", "alice", "利润趋势", 7L, Map.of());

        AiChatMessage assistantMessage = session.getMessages().get(1);
        assertThat(assistantMessage.getAssumptions()).contains("\"key\":\"dataSurface\"");
        assertThat(assistantMessage.getConfidence()).isEqualTo(0.86d);
        assertThat(assistantMessage.getTrace())
                .contains("\"domain\":\"flowerbiz\"")
                .contains("\"table\":\"public.ads_profit\"")
                .contains("\"sql\":\"select month_id, profit from public.ads_profit\"");
    }

    @Test
    void sendMessagePersistsFinanceAuditTrailOnAssistantTrace() {
        AiChatSessionRepository sessionRepository = mock(AiChatSessionRepository.class);
        AgentExecutionService agentExecutionService = mock(AgentExecutionService.class);
        AiAuditService auditService = mock(AiAuditService.class);

        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-finance");
        session.setUserId("alice");
        session.setStatus("ACTIVE");

        when(sessionRepository.findBySessionId("sess-finance")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentExecutionService.executeChat(
                eq("sess-finance"), eq("alice"), eq("月对账折后实收是多少"), anyList(), eq(7L), anyMap()))
                .thenReturn(new ChatExecutionResult(
                        "折后实收为 1128.00。",
                        "select sum(folding_after_total_amount) from xycyl_ads_month_settlement_summary",
                        financeMonthSettlementPlan(),
                        null,
                        CopilotChatRequestContext.empty(),
                        financeAuditReport()));

        AgentChatService service = new AgentChatService(sessionRepository, agentExecutionService, auditService);

        service.sendMessage("sess-finance", "alice", "月对账折后实收是多少", 7L, Map.of());

        AiChatMessage assistantMessage = session.getMessages().get(1);
        assertThat(assistantMessage.getTrace())
                .contains("\"financeAudit\"")
                .contains("\"sanitizedSql\"")
                .contains("CAL-MONTH-AMOUNT-TIER")
                .contains("xycyl_ads_month_settlement_summary")
                .contains("\"healthStatus\":\"PASS\"");
    }

    @Test
    void sendMessagePassesAssumptionOverridesAndClarificationAnswersToExecution() {
        AiChatSessionRepository sessionRepository = mock(AiChatSessionRepository.class);
        AgentExecutionService agentExecutionService = mock(AgentExecutionService.class);
        AiAuditService auditService = mock(AiAuditService.class);

        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-1");
        session.setUserId("alice");
        session.setStatus("ACTIVE");

        Map<String, Boolean> martHealth = Map.of("ads_project_monthly", true);
        Map<String, String> assumptionOverrides = Map.of("period", "2026-05");
        Map<String, String> clarificationAnswers = Map.of("target", "在租项目");

        when(sessionRepository.findBySessionId("sess-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentExecutionService.executeChat(
                eq("sess-1"),
                eq("alice"),
                eq("利润趋势"),
                anyList(),
                eq(7L),
                eq(martHealth),
                eq(assumptionOverrides),
                eq(clarificationAnswers)))
                .thenReturn(new ChatExecutionResult(
                        "已按确认口径重算。",
                        "select month_id, profit from public.ads_profit",
                        new ConversationPlan(
                                PlanMode.AGENT_WORKFLOW,
                                ResponseKind.REPORT_DRAFT,
                                null,
                                "flowerbiz",
                                "public.ads_profit",
                                List.of(),
                                null,
                                null,
                                "MART",
                                "public.ads_profit",
                                "利润趋势分析",
                                "L2_ADS",
                                "HIGH",
                                List.of("利润=收入-成本"),
                                "line",
                                "prs.flowerbiz.profit_monthly",
                                List.of("dbt-model:public.ads_profit")),
                        null));

        AgentChatService service = new AgentChatService(sessionRepository, agentExecutionService, auditService);

        service.sendMessage("sess-1", "alice", "利润趋势", 7L,
                martHealth, assumptionOverrides, clarificationAnswers);

        verify(agentExecutionService).executeChat(
                eq("sess-1"),
                eq("alice"),
                eq("利润趋势"),
                anyList(),
                eq(7L),
                eq(martHealth),
                eq(assumptionOverrides),
                eq(clarificationAnswers));
    }

    @Test
    void sendMessageStreamPersistsFinanceAuditTrailOnAssistantTrace() {
        AiChatSessionRepository sessionRepository = mock(AiChatSessionRepository.class);
        AgentExecutionService agentExecutionService = mock(AgentExecutionService.class);
        AiAuditService auditService = mock(AiAuditService.class);

        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-finance-stream");
        session.setUserId("alice");
        session.setStatus("ACTIVE");

        when(sessionRepository.findBySessionId("sess-finance-stream")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentExecutionService.executeChatStream(
                eq("sess-finance-stream"),
                eq("alice"),
                eq("月对账折后实收是多少"),
                anyList(),
                eq(7L),
                anyMap(),
                any()))
                .thenReturn(new ChatExecutionResult(
                        "折后实收为 1128.00。",
                        "select sum(folding_after_total_amount) from xycyl_ads_month_settlement_summary",
                        financeMonthSettlementPlan(),
                        null,
                        CopilotChatRequestContext.empty(),
                        financeAuditReport()));

        AgentChatService service = new AgentChatService(sessionRepository, agentExecutionService, auditService);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.sendMessageStream(
                "sess-finance-stream",
                "alice",
                "月对账折后实收是多少",
                7L,
                output);

        AiChatMessage assistantMessage = session.getMessages().get(1);
        assertThat(assistantMessage.getTrace())
                .contains("\"financeAudit\"")
                .contains("\"sanitizedSql\"")
                .contains("CAL-MONTH-AMOUNT-TIER")
                .contains("\"healthStatus\":\"PASS\"");
        verify(sessionRepository).save(session);
    }

    @Test
    void sendMessageStreamDoesNotPersistAssistantErrorWhenStreamingIsInterrupted() {
        AiChatSessionRepository sessionRepository = mock(AiChatSessionRepository.class);
        AgentExecutionService agentExecutionService = mock(AgentExecutionService.class);
        AiAuditService auditService = mock(AiAuditService.class);

        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-1");
        session.setUserId("alice");
        session.setStatus("ACTIVE");

        when(sessionRepository.findBySessionId("sess-1")).thenReturn(Optional.of(session));
        when(agentExecutionService.executeChatStream(
                eq("sess-1"), eq("alice"), eq("hi"), anyList(), anyLong(), anyMap(), any()))
                .thenThrow(new RuntimeException(new InterruptedException("stream interrupted")));

        AgentChatService service = new AgentChatService(sessionRepository, agentExecutionService, auditService);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.sendMessageStream("sess-1", "alice", "hi", 7L, output);

        assertThat(output.toString(StandardCharsets.UTF_8))
                .contains("event: session")
                .doesNotContain("event: error");
        assertThat(session.getMessages()).hasSize(1);
        assertThat(session.getMessages().get(0).getRole()).isEqualTo("user");

        verify(sessionRepository, never()).save(session);
        verify(auditService, never()).logChatAction(
                eq("alice"), eq("sess-1"), eq("CHAT_MESSAGE_ERROR"), eq("hi"), any());
    }

    @Test
    void sendMessageStreamPersistsAssistantErrorWhenStreamingFails() {
        AiChatSessionRepository sessionRepository = mock(AiChatSessionRepository.class);
        AgentExecutionService agentExecutionService = mock(AgentExecutionService.class);
        AiAuditService auditService = mock(AiAuditService.class);

        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-1");
        session.setUserId("alice");
        session.setStatus("ACTIVE");

        when(sessionRepository.findBySessionId("sess-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentExecutionService.executeChatStream(
                eq("sess-1"), eq("alice"), eq("hi"), anyList(), anyLong(), anyMap(), any()))
                .thenThrow(new IllegalStateException("upstream unavailable"));

        AgentChatService service = new AgentChatService(sessionRepository, agentExecutionService, auditService);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.sendMessageStream("sess-1", "alice", "hi", 7L, output);

        assertThat(output.toString(StandardCharsets.UTF_8))
                .contains("event: session")
                .contains("event: error")
                .contains("upstream unavailable");
        assertThat(session.getMessages()).hasSize(2);
        assertThat(session.getMessages().get(0).getRole()).isEqualTo("user");
        AiChatMessage assistantMessage = session.getMessages().get(1);
        assertThat(assistantMessage.getRole()).isEqualTo("assistant");
        assertThat(assistantMessage.getContent())
                .contains("抱歉，本次回答失败，请稍后重试。")
                .contains("upstream unavailable");
        assertThat(session.getTitle()).isEqualTo("hi");

        verify(sessionRepository, times(1)).save(session);
        verify(auditService).logChatAction("alice", "sess-1",
                "CHAT_MESSAGE_ERROR", "hi", assistantMessage.getContent());
    }

    @Test
    void buildStreamFailureMessageFallsBackToGenericCopyWhenExceptionHasNoMessage() {
        assertThat(AgentChatService.buildStreamFailureMessage(new IllegalStateException()))
                .isEqualTo("抱歉，本次回答失败，请稍后重试。");
    }

    private static ConversationPlan financeMonthSettlementPlan() {
        return new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.REPORT_DRAFT,
                null,
                "finance",
                "xycyl_ads_month_settlement_summary",
                List.of(),
                null,
                null,
                "MART",
                "xycyl_ads_month_settlement_summary",
                "月对账折后实收",
                "L3_ADS",
                "HIGH",
                List.of("财务回答必须附审计溯源"),
                "table",
                "finance.month_settlement",
                List.of("dbt-model:xycyl_ads_month_settlement_summary"),
                null,
                List.of(new ConversationPlan.RouteStep(
                        "TIER_2_MART_TEMPLATE",
                        "ADS 模型",
                        "HIT",
                        "命中月对账 ADS",
                        "xycyl_ads_month_settlement_summary")));
    }

    private static FinanceAnswerAuditTrailService.AuditTrailReport financeAuditReport() {
        return new FinanceAnswerAuditTrailService.AuditTrailReport(
                true,
                "",
                List.of("sql", "caliberRules", "lineage", "oracleStatus", "routeTrace"),
                "select sum(folding_after_total_amount) from xycyl_ads_month_settlement_summary",
                List.of(new FinanceAnswerAuditTrailService.AppliedCaliberRule(
                        "CAL-MONTH-AMOUNT-TIER",
                        "月对账金额必须区分四层。",
                        "P0",
                        "折后实收列必须使用 folding_after_total_amount。",
                        List.of("month-settlement"))),
                List.of(),
                List.of(new FinanceAnswerAuditTrailService.LineageNode(
                        "ADS_MODEL",
                        "xycyl_ads_month_settlement_summary",
                        "auditable-result-model",
                        List.of("dbt:model.xy_cyl.xycyl_ads_month_settlement_summary"))),
                new FinanceAnswerAuditTrailService.OracleAuditStatus(
                        "month-settlement",
                        "月对账",
                        "L2",
                        "rent-settlement",
                        true,
                        "PASS",
                        BigDecimal.ZERO,
                        ""),
                List.of(new FinanceAnswerAuditTrailService.RouteTraceStep(
                        "TIER_2_MART_TEMPLATE",
                        "ADS 模型",
                        "HIT",
                        "命中月对账 ADS",
                        "xycyl_ads_month_settlement_summary")));
    }
}
