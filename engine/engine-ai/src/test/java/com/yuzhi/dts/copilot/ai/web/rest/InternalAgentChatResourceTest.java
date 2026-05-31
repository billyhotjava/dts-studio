package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.service.chat.AgentChatService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import java.util.List;
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
        assistant.setConfidence(0.86d);
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
                .containsEntry("confidence", 0.86d);
        verify(agentChatService).sendMessage(
                isNull(), eq("bob"), eq("hi"), eq(1L),
                isNull(), isNull(), isNull());
    }
}
