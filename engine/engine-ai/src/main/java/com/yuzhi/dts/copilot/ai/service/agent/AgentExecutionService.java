package com.yuzhi.dts.copilot.ai.service.agent;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.AiProviderConfigRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.ChatGroundingService;
import com.yuzhi.dts.copilot.ai.service.copilot.ChatGroundingService.GroundingContext;
import com.yuzhi.dts.copilot.ai.service.llm.OpenAiCompatibleClient;
import com.yuzhi.dts.copilot.ai.service.rag.RagService;
import com.yuzhi.dts.copilot.ai.service.rag.dto.RagResult;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * High-level agent execution service.
 * Manages the chat session lifecycle and delegates to the ReAct engine for reasoning.
 */
@Service
public class AgentExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionService.class);

    private static final String SYSTEM_PROMPT = """
            你是 DTS Copilot，一个智能数据分析助手。你由 DTS 智能平台提供，底层接入了用户配置的大语言模型。
            当用户问你是谁、什么模型时，回答"我是 DTS Copilot，由 DTS 智能平台提供的数据分析助手"，不要声称自己是其他产品。

            你可以帮助用户探索数据库、编写 SQL 查询和分析数据。你拥有查询数据库和查看表结构的工具。

            使用规范：
            - 使用 schema_lookup 工具验证表名和字段名后再写查询
            - 只执行 SELECT 查询，不允许修改数据
            - 清晰解释你的推理和结果
            - 如果不确定，向用户请求澄清
            - 默认使用中文回复

            ## 数据查询工作流

            当用户提出数据查询相关问题时，按照以下步骤操作：

            1. 调用 schema_lookup 工具获取相关表结构和字段信息
            2. 基于表结构信息，生成符合要求的 SQL 查询：
               - 只允许 SELECT 或 WITH...SELECT 语句
               - 不允许 INSERT/UPDATE/DELETE/DROP 等写操作
               - 优先使用简单查询，避免不必要的复杂 JOIN
            3. 将生成的 SQL 用 ```sql 代码块包裹返回
            4. 简要解释 SQL 的查询逻辑

            回复格式：
            [对问题的理解和分析]

            ```sql
            SELECT ...
            ```

            [SQL 逻辑说明]
            """;

    private static final ObjectMapper mapper = new ObjectMapper();

    private final ReActEngine reActEngine;
    private final RagService ragService;
    private final AiProviderConfigRepository providerConfigRepository;
    private final ChatGroundingService chatGroundingService;

    // CS-02: Cached LLM client to reuse HTTP connections
    private volatile OpenAiCompatibleClient cachedClient;
    private volatile String cachedClientKey;

    public AgentExecutionService(ReActEngine reActEngine,
                                 RagService ragService,
                                 AiProviderConfigRepository providerConfigRepository,
                                 ChatGroundingService chatGroundingService) {
        this.reActEngine = reActEngine;
        this.ragService = ragService;
        this.providerConfigRepository = providerConfigRepository;
        this.chatGroundingService = chatGroundingService;
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
    public ChatExecutionResult executeChat(String sessionId, String userId, String userMessage,
                                           List<Map<String, Object>> history, Long dataSourceId) {
        return executeChat(sessionId, userId, userMessage, history, dataSourceId, Collections.emptyMap());
    }

    public ChatExecutionResult executeChat(String sessionId, String userId, String userMessage,
                                           List<Map<String, Object>> history, Long dataSourceId,
                                           Map<String, Boolean> martHealthSnapshot) {
        GroundingContext groundingContext = chatGroundingService.buildContext(userMessage, martHealthSnapshot);
        if (groundingContext.needsClarification()) {
            return new ChatExecutionResult(
                    groundingContext.clarificationMessage(),
                    null,
                    groundingContext,
                    null
            );
        }

        // CS-01: Template fast-path — skip LLM entirely when template matched with SQL
        if (groundingContext.templateCode() != null && groundingContext.resolvedSql() != null) {
            String response = formatTemplateResponse(groundingContext);
            return new ChatExecutionResult(response, groundingContext.resolvedSql(), groundingContext, null);
        }

        AiProviderConfig provider = resolveProvider();
        if (provider == null) {
            return new ChatExecutionResult(
                    "No AI provider is configured. Please configure a provider in the settings.",
                    null,
                    groundingContext,
                    null
            );
        }

        OpenAiCompatibleClient client = getOrCreateClient(provider);

        // Build conversation messages
        List<Map<String, Object>> messages = new ArrayList<>();

        // System prompt with RAG context
        String systemPrompt = buildSystemPrompt(userMessage, groundingContext);
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

        String response = reActEngine.execute(client, provider.getModel(), messages, toolContext,
                provider.getTemperature(), provider.getMaxTokens());
        return new ChatExecutionResult(
                response,
                resolveGeneratedSql(response, groundingContext),
                groundingContext,
                null
        );
    }

    private String buildSystemPrompt(String userMessage, GroundingContext groundingContext) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

        if (groundingContext != null && groundingContext.promptContext() != null
                && !groundingContext.promptContext().isBlank()) {
            sb.append("\n\nBusiness grounding context:\n")
                    .append(groundingContext.promptContext())
                    .append("\n");
        }

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

    private String resolveGeneratedSql(String response, GroundingContext groundingContext) {
        String generatedSql = extractSqlFromMarkdown(response);
        if (StringUtils.hasText(generatedSql)) {
            return generatedSql;
        }
        if (groundingContext != null && StringUtils.hasText(groundingContext.resolvedSql())) {
            return groundingContext.resolvedSql().trim();
        }
        return null;
    }

    private String extractSqlFromMarkdown(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        int start = content.indexOf("```sql");
        if (start < 0) {
            return null;
        }
        int bodyStart = start + "```sql".length();
        int end = content.indexOf("```", bodyStart);
        if (end < 0) {
            return null;
        }
        String sql = content.substring(bodyStart, end).trim();
        return StringUtils.hasText(sql) ? sql : null;
    }

    // ── CS-04: Streaming variant ──────────────────────────────────────────

    /**
     * Streaming variant of executeChat. Writes SSE events to output.
     * Returns the complete response text for persistence.
     */
    public ChatExecutionResult executeChatStream(String sessionId, String userId, String userMessage,
                                                  List<Map<String, Object>> history, Long dataSourceId,
                                                  OutputStream sseOutput) {
        return executeChatStream(sessionId, userId, userMessage, history, dataSourceId, Collections.emptyMap(), sseOutput);
    }

    public ChatExecutionResult executeChatStream(String sessionId, String userId, String userMessage,
                                                 List<Map<String, Object>> history, Long dataSourceId,
                                                 Map<String, Boolean> martHealthSnapshot,
                                                 OutputStream sseOutput) {
        GroundingContext groundingContext = chatGroundingService.buildContext(userMessage, martHealthSnapshot);
        if (groundingContext.needsClarification()) {
            writeTokenAndDone(sseOutput, groundingContext.clarificationMessage(), null);
            return new ChatExecutionResult(groundingContext.clarificationMessage(), null, groundingContext, null);
        }

        // CS-01: Template fast-path (streaming)
        if (groundingContext.templateCode() != null && groundingContext.resolvedSql() != null) {
            String response = formatTemplateResponse(groundingContext);
            writeTokenAndDone(sseOutput, response, groundingContext.resolvedSql());
            return new ChatExecutionResult(response, groundingContext.resolvedSql(), groundingContext, null);
        }

        AiProviderConfig provider = resolveProvider();
        if (provider == null) {
            String msg = "No AI provider is configured. Please configure a provider in the settings.";
            writeTokenAndDone(sseOutput, msg, null);
            return new ChatExecutionResult(msg, null, groundingContext, null);
        }

        OpenAiCompatibleClient client = getOrCreateClient(provider);

        List<Map<String, Object>> messages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt(userMessage, groundingContext);
        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        if (history != null) {
            messages.addAll(history);
        }
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        ToolContext toolContext = new ToolContext(userId, sessionId, dataSourceId);
        String response = reActEngine.executeStreaming(client, provider.getModel(), messages, toolContext,
                provider.getTemperature(), provider.getMaxTokens(), sseOutput);

        String sql = resolveGeneratedSql(response, groundingContext);
        writeDoneEvent(sseOutput, sql);

        return new ChatExecutionResult(
                response,
                sql,
                groundingContext,
                extractReasoningFromMessages(messages)
        );
    }

    // ── CS-01: Template response formatting ─────────────────────────────

    private String formatTemplateResponse(GroundingContext ctx) {
        StringBuilder sb = new StringBuilder();
        if (ctx.domain() != null) {
            sb.append("根据您的问题，已从 **").append(ctx.domain())
              .append("** 业务域匹配到预制查询模板");
            if (ctx.templateCode() != null) {
                sb.append("（").append(ctx.templateCode()).append("）");
            }
            sb.append("。\n\n");
        }
        sb.append("```sql\n").append(ctx.resolvedSql().trim()).append("\n```\n");
        if (ctx.primaryView() != null) {
            sb.append("\n查询目标视图：`").append(ctx.primaryView()).append("`");
        }
        return sb.toString();
    }

    // ── CS-02: HttpClient caching ───────────────────────────────────────

    private OpenAiCompatibleClient getOrCreateClient(AiProviderConfig provider) {
        String key = provider.getBaseUrl() + "|" + provider.getApiKey() + "|" +
                     (provider.getTimeoutSeconds() != null ? provider.getTimeoutSeconds() : 120);
        if (cachedClient != null && key.equals(cachedClientKey)) {
            return cachedClient;
        }
        synchronized (this) {
            if (cachedClient != null && key.equals(cachedClientKey)) {
                return cachedClient;
            }
            cachedClient = new OpenAiCompatibleClient(
                    provider.getBaseUrl(), provider.getApiKey(),
                    provider.getTimeoutSeconds() != null ? provider.getTimeoutSeconds() : 120);
            cachedClientKey = key;
            return cachedClient;
        }
    }

    // ── SSE helpers ─────────────────────────────────────────────────────

    private void writeTokenAndDone(OutputStream out, String text, String sql) {
        try {
            String escaped = mapper.createObjectNode().put("content", text).toString();
            out.write(("event: token\ndata: " + escaped + "\n\n").getBytes(StandardCharsets.UTF_8));
            writeDoneEvent(out, sql);
            out.flush();
        } catch (IOException e) {
            log.debug("SSE write failed: {}", e.getMessage());
        }
    }

    private void writeDoneEvent(OutputStream out, String sql) {
        try {
            ObjectNode done = mapper.createObjectNode();
            if (sql != null) {
                done.put("generatedSql", sql);
            }
            out.write(("event: done\ndata: " + done.toString() + "\n\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            log.debug("SSE done event write failed: {}", e.getMessage());
        }
    }

    public record ChatExecutionResult(
            String response,
            String generatedSql,
            GroundingContext groundingContext,
            String reasoningContent
    ) {}

    private String extractReasoningFromMessages(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            Object value = messages.get(index).get("reasoning_content");
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
