package com.yuzhi.dts.copilot.ai.service.chat;

import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.repository.AiChatSessionRepository;
import com.yuzhi.dts.copilot.ai.service.agent.AgentExecutionService;
import com.yuzhi.dts.copilot.ai.service.audit.AiAuditService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
}
