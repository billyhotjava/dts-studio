package com.yuzhi.dts.copilot.ai.service.agent;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.AiProviderConfigRepository;
import com.yuzhi.dts.copilot.ai.service.llm.OpenAiCompatibleClient;
import com.yuzhi.dts.copilot.ai.service.rag.RagService;
import com.yuzhi.dts.copilot.ai.service.rag.dto.RagResult;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * High-level agent execution service.
 * Manages the chat session lifecycle and delegates to the ReAct engine for reasoning.
 */
@Service
public class AgentExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionService.class);

    private static final String SYSTEM_PROMPT = """
            You are DTS Copilot, an intelligent data assistant. You help users explore databases, \
            write SQL queries, and analyze data. You have access to tools that let you query databases \
            and look up schema information.

            Guidelines:
            - Always verify table and column names using the schema lookup tool before writing queries.
            - Only execute SELECT queries; never modify data.
            - Explain your reasoning and results clearly.
            - If you're unsure about something, ask the user for clarification.
            """;

    private final ReActEngine reActEngine;
    private final RagService ragService;
    private final AiProviderConfigRepository providerConfigRepository;

    public AgentExecutionService(ReActEngine reActEngine,
                                 RagService ragService,
                                 AiProviderConfigRepository providerConfigRepository) {
        this.reActEngine = reActEngine;
        this.ragService = ragService;
        this.providerConfigRepository = providerConfigRepository;
    }

    /**
     * Execute a chat interaction: enrich with RAG context, then run the ReAct loop.
     *
     * @param sessionId   the chat session identifier
     * @param userId      the user identifier
     * @param userMessage the user's message
     * @param history     previous conversation messages (role/content maps)
     * @param dataSourceId optional active data source
     * @return the agent's text response
     */
    public String executeChat(String sessionId, String userId, String userMessage,
                              List<Map<String, Object>> history, Long dataSourceId) {
        AiProviderConfig provider = resolveProvider();
        if (provider == null) {
            return "No AI provider is configured. Please configure a provider in the settings.";
        }

        OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                provider.getBaseUrl(),
                provider.getApiKey(),
                provider.getTimeoutSeconds() != null ? provider.getTimeoutSeconds() : 120
        );

        // Build conversation messages
        List<Map<String, Object>> messages = new ArrayList<>();

        // System prompt with RAG context
        String systemPrompt = buildSystemPrompt(userMessage);
        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        // Add conversation history
        if (history != null) {
            messages.addAll(history);
        }

        // Add current user message
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        // Execute ReAct loop
        ToolContext toolContext = new ToolContext(userId, sessionId, dataSourceId);

        return reActEngine.execute(client, provider.getModel(), messages, toolContext,
                provider.getTemperature(), provider.getMaxTokens());
    }

    private String buildSystemPrompt(String userMessage) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

        // Enrich with RAG context
        try {
            List<RagResult> ragResults = ragService.retrieve(userMessage, 3);
            if (!ragResults.isEmpty()) {
                sb.append("\n\nRelevant context from the knowledge base:\n");
                for (int i = 0; i < ragResults.size(); i++) {
                    RagResult r = ragResults.get(i);
                    sb.append(String.format("\n--- Context %d [%s: %s] ---\n%s\n",
                            i + 1, r.contentType(), r.sourceId(), r.content()));
                }
            }
        } catch (Exception e) {
            log.debug("RAG retrieval skipped: {}", e.getMessage());
        }

        return sb.toString();
    }

    private AiProviderConfig resolveProvider() {
        // Try default provider first, then first enabled
        return providerConfigRepository.findByIsDefaultTrue()
                .orElseGet(() -> {
                    List<AiProviderConfig> enabled = providerConfigRepository
                            .findByEnabledTrueOrderByPriorityAsc();
                    return enabled.isEmpty() ? null : enabled.get(0);
                });
    }
}
