package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.service.chat.AgentChatService;
import org.junit.jupiter.api.Test;
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
    void sendMessageStreamTreatsSessionOwnedByAnotherUserAsNewSession() throws Exception {
        AgentChatService agentChatService = mock(AgentChatService.class);
        AiChatSession session = new AiChatSession();
        session.setSessionId("sess-1");
        session.setUserId("alice");
        when(agentChatService.getSession("sess-1")).thenReturn(Optional.of(session));

        InternalAgentChatResource resource = new InternalAgentChatResource(agentChatService, "secret");
        InternalAgentChatResource.ChatRequest request =
                new InternalAgentChatResource.ChatRequest("sess-1", "bob", "hi", 1L, null);

        resource.sendMessageStream("secret", request).writeTo(new java.io.ByteArrayOutputStream());

        verify(agentChatService)
                .sendMessageStream(isNull(), eq("bob"), eq("hi"), eq(1L), isNull(), any());
    }
}
