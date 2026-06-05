package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.service.chat.AgentChatService;
import com.yuzhi.dts.copilot.ai.service.chat.RouteTelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalAgentChatResourceTest {

    @Test
    void getRouteTelemetryRequiresAdminSecretAndReturnsSummary() {
        AgentChatService agentChatService = mock(AgentChatService.class);
        RouteTelemetryService routeTelemetryService = mock(RouteTelemetryService.class);
        RouteTelemetryService.RouteTelemetrySummary summary =
                new RouteTelemetryService.RouteTelemetrySummary(
                        14,
                        3,
                        Map.of("TIER_5_DIRECT_DETAIL", 3L),
                        List.of());
        when(routeTelemetryService.summarize(14, 5)).thenReturn(summary);

        InternalAgentChatResource resource =
                new InternalAgentChatResource(agentChatService, routeTelemetryService, "secret");

        ResponseEntity<?> response = resource.getRouteTelemetry("secret", 14, 5);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(summary);
    }

    @Test
    void getSessionIncludesResponseKindInMessagePayload() {
        AgentChatService agentChatService = mock(AgentChatService.class);
        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-1");
        session.setUserId("alice");

        AiChatMessage assistant = new AiChatMessage();
        assistant.setRole("assistant");
        assistant.setContent("当前已沉淀的业务分析范围包括：");
        assistant.setResponseKind("BUSINESS_DIRECT_RESPONSE");
        assistant.setSourceRefs("semantic-pack:project");
        assistant.setAssumptions("[{\"key\":\"dataSurface\",\"label\":\"数据层\",\"value\":\"L2_ADS\"}]");
        assistant.setConfidence(0.86d);
        assistant.setTrace("{\"metricCaliber\":{\"domain\":\"flowerbiz\"},\"sql\":\"select 1\"}");
        session.addMessage(assistant);

        when(agentChatService.getSession("sess-1")).thenReturn(Optional.of(session));

        InternalAgentChatResource resource = new InternalAgentChatResource(agentChatService, "secret");

        ResponseEntity<?> response = resource.getSession("secret", "sess-1", "alice");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        var body = (java.util.Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var messages = (java.util.List<java.util.Map<String, Object>>) body.get("messages");
        assertThat(messages).singleElement().satisfies(message ->
                assertThat(message)
                        .containsEntry("responseKind", "BUSINESS_DIRECT_RESPONSE")
                        .containsEntry("sourceRefs", "semantic-pack:project")
                        .containsEntry("confidence", 0.86d)
                        .containsKeys("assumptions", "trace"));
    }

    @Test
    void sendMessageStreamTreatsSessionOwnedByAnotherUserAsNewSession() throws Exception {
        AgentChatService agentChatService = mock(AgentChatService.class);
        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-1");
        session.setUserId("alice");
        when(agentChatService.getSession("sess-1")).thenReturn(Optional.of(session));

        InternalAgentChatResource resource = new InternalAgentChatResource(agentChatService, "secret");
        InternalAgentChatResource.ChatRequest request =
                new InternalAgentChatResource.ChatRequest("sess-1", "bob", "hi", 1L, null, null, null);

        resource.sendMessageStream("secret", request).writeTo(new java.io.ByteArrayOutputStream());

        verify(agentChatService)
                .sendMessageStream(
                        isNull(), eq("bob"), eq("hi"), eq(1L),
                        isNull(), isNull(), isNull(), any());
    }

    @Test
    void sendMessageTreatsSessionOwnedByAnotherUserAsNewSessionInResponse() {
        AgentChatService agentChatService = mock(AgentChatService.class);
        AiChatSession existingSession = new AiChatSession();
        existingSession.setSessionId("sess-1");
        existingSession.setUserId("alice");
        when(agentChatService.getSession("sess-1")).thenReturn(Optional.of(existingSession));

        AiChatSession newSession = new AiChatSession();
        newSession.setSessionId("sess-2");
        newSession.setUserId("bob");
        AiChatMessage assistant = new AiChatMessage();
        assistant.setRole("assistant");
        assistant.setContent("ok");
        assistant.setResponseKind("PUBLISHED_INDICATOR");
        assistant.setReportCode("codex_sprint29_live_metric");
        assistant.setTargetView("indicator:codex_sprint29_live_metric");
        assistant.setDataSurface("L3_PUBLISHED_INDICATOR");
        assistant.setQualityLevel("HIGH");
        assistant.setSourceRefs("platform-indicator:codex_sprint29_live_metric");
        assistant.setConfidence(0.86d);
        assistant.setTrace("{\"metricCaliber\":{\"name\":\"Sprint29 验证指标\",\"ontologyRef\":\"29000000-0000-4000-8000-000000000029\"}}");
        newSession.addMessage(assistant);
        when(agentChatService.getSessions("bob")).thenReturn(List.of(newSession));
        when(agentChatService.getSession("sess-2")).thenReturn(Optional.of(newSession));
        when(agentChatService.sendMessage(
                isNull(), eq("bob"), eq("hi"), eq(1L),
                isNull(), isNull(), isNull()))
                .thenReturn("ok");

        InternalAgentChatResource resource = new InternalAgentChatResource(agentChatService, "secret");
        InternalAgentChatResource.ChatRequest request =
                new InternalAgentChatResource.ChatRequest("sess-1", "bob", "hi", 1L, null, null, null);

        ResponseEntity<?> response = resource.sendMessage("secret", request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        var body = (java.util.Map<String, Object>) response.getBody();
        assertThat(body)
                .containsEntry("sessionId", "sess-2")
                .containsEntry("response", "ok")
                .containsEntry("responseKind", "PUBLISHED_INDICATOR")
                .containsEntry("reportCode", "codex_sprint29_live_metric")
                .containsEntry("targetView", "indicator:codex_sprint29_live_metric")
                .containsEntry("dataSurface", "L3_PUBLISHED_INDICATOR")
                .containsEntry("qualityLevel", "HIGH")
                .containsEntry("sourceRefs", "platform-indicator:codex_sprint29_live_metric")
                .containsEntry("confidence", 0.86d)
                .containsKey("trace");
        verify(agentChatService).sendMessage(
                isNull(), eq("bob"), eq("hi"), eq(1L),
                isNull(), isNull(), isNull());
    }
}
