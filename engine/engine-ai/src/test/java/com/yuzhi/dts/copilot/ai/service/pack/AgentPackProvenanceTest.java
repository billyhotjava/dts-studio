package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.*;
import com.yuzhi.dts.copilot.ai.service.agent.AgentExecutionService;
import com.yuzhi.dts.copilot.ai.service.agent.ReActEngine;
import com.yuzhi.dts.copilot.ai.service.audit.AiAuditService;
import com.yuzhi.dts.copilot.ai.service.chat.AgentChatService;
import com.yuzhi.dts.copilot.ai.service.copilot.CopilotChatContract;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.*;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticPackService;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClient;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClientFactory;
import com.yuzhi.dts.copilot.ai.service.rag.RagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentPackProvenanceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<PackAssetResolver.Snapshot> active = new AtomicReference<>();
    private final ReActEngine engine = mock(ReActEngine.class);
    private final ConversationPlannerService planner = mock(ConversationPlannerService.class);
    private final AiProviderConfigRepository providers = mock(AiProviderConfigRepository.class);
    private final LlmProviderClientFactory clients = mock(LlmProviderClientFactory.class);
    private final AiChatSession session = new AiChatSession();
    private final AgentExecutionService execution = new AgentExecutionService(engine, mock(RagService.class),
            providers, planner, clients, mock(AiDataSourceRepository.class));

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void versionChangedDuringModelCallDoesNotRewriteResponseOrHistory(boolean streaming) throws Exception {
        preparePlanner(PlanMode.AGENT_WORKFLOW);
        var provider = new AiProviderConfig();
        provider.setName("fixture"); provider.setBaseUrl("https://example.test"); provider.setApiKey("fixture");
        provider.setModel("fixture"); provider.setTemperature(0.2); provider.setMaxTokens(100);
        when(providers.findByIsDefaultTrue()).thenReturn(Optional.of(provider));
        when(clients.create(any())).thenReturn(mock(LlmProviderClient.class));
        when(engine.execute(any(), anyString(), anyList(), any(), anyDouble(), anyInt())).thenAnswer(call -> {
            activate("2.0.0", 2); return "answer";
        });
        when(engine.executeStreaming(any(), anyString(), anyList(), any(), anyDouble(), anyInt(), any()))
                .thenAnswer(call -> { activate("2.0.0", 2); return "answer"; });
        var chat = chat();
        var output = new ByteArrayOutputStream();
        if (streaming) chat.sendMessageStream("session", "reader", "question", 7L, output);
        else assertThat(chat.sendMessage("session", "reader", "question", 7L)).isEqualTo("answer");
        assertThat(PackReadScope.currentSources()).isEmpty();
        var assistant = session.getMessages().getLast();
        JsonNode refs = mapper.readTree(assistant.getTrace()).path("packRefs");
        assertThat(refs).isEqualTo(mapper.valueToTree(List.of(new PackSourceRef("fixture", "1.0.0"))));
        try (var schema = getClass().getResourceAsStream("/protocol/pack-source-ref.v1.schema.json")) {
            assertThat(schema).isNotNull();
            var validator = com.networknt.schema.JsonSchemaFactory
                    .getInstance(com.networknt.schema.SpecVersion.VersionFlag.V202012)
                    .getSchema(mapper.readTree(schema));
            refs.forEach(ref -> assertThat(validator.validate(ref)).isEmpty());
        }
        Map<String, Object> replay = new LinkedHashMap<>();
        CopilotChatContract.putMessageFields(assistant, replay);
        assertThat(mapper.valueToTree(replay).path("packRefs")).isEqualTo(refs);
        if (streaming) {
            String payload = output.toString(StandardCharsets.UTF_8).split("event: done\\ndata: ", 2)[1].strip();
            assertThat(mapper.readTree(payload).path("packRefs")).isEqualTo(refs);
            assertThat(mapper.readTree(payload).path("trace").path("packRefs")).isEqualTo(refs);
        }
        activate("3.0.0", 3); // Replay remains an immutable execution record after another activation.
        CopilotChatContract.putMessageFields(assistant, replay);
        assertThat(mapper.valueToTree(replay).path("packRefs")).isEqualTo(refs);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void directResponsesAlsoCarryActualPackDependencies(boolean streaming) throws Exception {
        preparePlanner(PlanMode.DIRECT_RESPONSE);
        var output = new ByteArrayOutputStream();
        var result = streaming
                ? execution.executeChatStream("s", "u", "question", List.of(), 7L, output)
                : execution.executeChat("s", "u", "question", List.of(), 7L);
        assertThat(result.packSources()).containsExactly(new PackSourceRef("fixture", "1.0.0"));
        verifyNoInteractions(engine, providers);
        assertThat(PackReadScope.currentSources()).isEmpty();
    }

    @Test
    void plannerFailureCleansScopeBeforeNextRequest() throws Exception {
        preparePlanner(PlanMode.DIRECT_RESPONSE);
        when(planner.plan("question", Map.of())).thenAnswer(call -> {
            PackReadScope.record(List.of(new PackSourceRef("failed", "1.0.0")));
            throw new IllegalStateException("planning failed");
        });
        assertThatThrownBy(() -> execution.executeChat("s", "u", "question", List.of(), 7L))
                .hasMessage("planning failed");
        assertThat(PackReadScope.currentSources()).isEmpty();
        when(planner.plan("next", Map.of())).thenReturn(plan(PlanMode.DIRECT_RESPONSE));
        assertThat(execution.executeChat("s", "u", "next", List.of(), 7L).packSources()).isEmpty();
    }

    @Test
    void historicalAndMalformedTracesRemainReadableWithoutInventedProvenance() {
        var message = new com.yuzhi.dts.copilot.ai.domain.AiChatMessage();
        for (String trace : List.of("{}", "null", "broken")) {
            message.setTrace(trace);
            var replay = new LinkedHashMap<String, Object>();
            CopilotChatContract.putMessageFields(message, replay);
            assertThat(replay).doesNotContainKey("packRefs");
        }
    }

    private void preparePlanner(PlanMode mode) throws Exception {
        activate("1.0.0", 1);
        var semantics = new SemanticPackService(mapper);
        semantics.setPackResourceReader(new PackResourceReader(active::get, false));
        semantics.getPack("fixture");
        when(planner.plan("question", Map.of())).thenAnswer(call -> {
            semantics.getPack("fixture");
            return plan(mode);
        });
    }

    private ConversationPlan plan(PlanMode mode) {
        return new ConversationPlan(mode, ResponseKind.BUSINESS_CLARIFICATION, "direct", "fixture",
                null, List.of(), null, null, "VIEW", null, "context");
    }

    private void activate(String version, long generation) throws Exception {
        String json = "{\"domain\":\"fixture\",\"objects\":[]}";
        active.set(new PackAssetResolver.Snapshot(generation, List.of(new PackAssetResolver.Asset(
                "fixture", version, "ontology", "semantic-packs/fixture.json", json, mapper.readTree(json)))));
    }

    private AgentChatService chat() {
        session.setSessionId("session"); session.setUserId("reader"); session.setTitle("fixture");
        var sessions = mock(AiChatSessionRepository.class);
        when(sessions.findBySessionId("session")).thenReturn(Optional.of(session));
        when(sessions.save(any())).thenAnswer(call -> call.getArgument(0));
        return new AgentChatService(sessions, execution, mock(AiAuditService.class));
    }
}
