package com.yuzhi.dts.copilot.ai.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.AiProviderConfigRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClient;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClientFactory;
import com.yuzhi.dts.copilot.ai.service.rag.RagService;
import com.yuzhi.dts.copilot.ai.service.rag.dto.RagResult;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReActEngine reActEngine;
    private final RagService ragService;
    private final AiProviderConfigRepository providerConfigRepository;
    private final ConversationPlannerService conversationPlannerService;
    private final LlmProviderClientFactory clientFactory;

    private volatile LlmProviderClient cachedClient;
    private volatile String cachedClientKey;

    public AgentExecutionService(ReActEngine reActEngine,
                                 RagService ragService,
                                 AiProviderConfigRepository providerConfigRepository,
                                 ConversationPlannerService conversationPlannerService,
                                 LlmProviderClientFactory clientFactory) {
        this.reActEngine = reActEngine;
        this.ragService = ragService;
        this.providerConfigRepository = providerConfigRepository;
        this.conversationPlannerService = conversationPlannerService;
        this.clientFactory = clientFactory;
    }

    public ChatExecutionResult executeChat(String sessionId, String userId, String userMessage,
                                           List<Map<String, Object>> history, Long dataSourceId) {
        return executeChat(sessionId, userId, userMessage, history, dataSourceId, Collections.emptyMap());
    }

    public ChatExecutionResult executeChat(String sessionId, String userId, String userMessage,
                                           List<Map<String, Object>> history, Long dataSourceId,
                                           Map<String, Boolean> martHealthSnapshot) {
        ConversationPlan conversationPlan = conversationPlannerService.plan(userMessage, martHealthSnapshot);
        if (conversationPlan.mode() == PlanMode.DIRECT_RESPONSE) {
            return new ChatExecutionResult(conversationPlan.directResponse(), null, conversationPlan, null);
        }
        if (isTemplateFastPath(conversationPlan)) {
            String response = formatFastPathResponse(conversationPlan);
            String generatedSql = resolveFastPathGeneratedSql(conversationPlan);
            return new ChatExecutionResult(response, generatedSql, conversationPlan, null);
        }

        AiProviderConfig provider = resolveProvider();
        if (provider == null) {
            return new ChatExecutionResult(
                    "No AI provider is configured. Please configure a provider in the settings.",
                    null,
                    conversationPlan,
                    null
            );
        }

        LlmProviderClient client = getOrCreateClient(provider);
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildSystemPrompt(userMessage, conversationPlan));
        messages.add(systemMsg);
        if (history != null) {
            messages.addAll(history);
        }
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        ToolContext toolContext = new ToolContext(userId, sessionId, dataSourceId);
        String response = reActEngine.execute(
                client,
                provider.getModel(),
                messages,
                toolContext,
                provider.getTemperature(),
                provider.getMaxTokens());
        return new ChatExecutionResult(
                response,
                resolveGeneratedSql(response, conversationPlan),
                conversationPlan,
                null
        );
    }

    public ChatExecutionResult executeChatStream(String sessionId, String userId, String userMessage,
                                                 List<Map<String, Object>> history, Long dataSourceId,
                                                 OutputStream sseOutput) {
        return executeChatStream(
                sessionId, userId, userMessage, history, dataSourceId, Collections.emptyMap(), sseOutput);
    }

    public ChatExecutionResult executeChatStream(String sessionId, String userId, String userMessage,
                                                 List<Map<String, Object>> history, Long dataSourceId,
                                                 Map<String, Boolean> martHealthSnapshot,
                                                 OutputStream sseOutput) {
        ConversationPlan conversationPlan = conversationPlannerService.plan(userMessage, martHealthSnapshot);
        if (conversationPlan.mode() == PlanMode.DIRECT_RESPONSE) {
            writeTokenAndDone(sseOutput, conversationPlan.directResponse(), null, conversationPlan);
            return new ChatExecutionResult(conversationPlan.directResponse(), null, conversationPlan, null);
        }
        if (isTemplateFastPath(conversationPlan)) {
            String response = formatFastPathResponse(conversationPlan);
            String generatedSql = resolveFastPathGeneratedSql(conversationPlan);
            writeTokenAndDone(sseOutput, response, generatedSql, conversationPlan);
            return new ChatExecutionResult(response, generatedSql, conversationPlan, null);
        }

        AiProviderConfig provider = resolveProvider();
        if (provider == null) {
            String message = "No AI provider is configured. Please configure a provider in the settings.";
            writeTokenAndDone(sseOutput, message, null, conversationPlan);
            return new ChatExecutionResult(message, null, conversationPlan, null);
        }

        LlmProviderClient client = getOrCreateClient(provider);
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildSystemPrompt(userMessage, conversationPlan));
        messages.add(systemMsg);
        if (history != null) {
            messages.addAll(history);
        }
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        ToolContext toolContext = new ToolContext(userId, sessionId, dataSourceId);
        String response = reActEngine.executeStreaming(
                client,
                provider.getModel(),
                messages,
                toolContext,
                provider.getTemperature(),
                provider.getMaxTokens(),
                sseOutput);
        String sql = resolveGeneratedSql(response, conversationPlan);
        writeDoneEvent(sseOutput, sql, conversationPlan);

        return new ChatExecutionResult(
                response,
                sql,
                conversationPlan,
                extractReasoningFromMessages(messages)
        );
    }

    private String buildSystemPrompt(String userMessage, ConversationPlan conversationPlan) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);
        if (conversationPlan != null && StringUtils.hasText(conversationPlan.promptContext())) {
            sb.append("\n\nBusiness grounding context:\n")
                    .append(conversationPlan.promptContext())
                    .append("\n");
        }
        try {
            List<RagResult> ragResults = ragService.retrieve(userMessage, 3);
            if (!ragResults.isEmpty()) {
                sb.append("\n\nRelevant context from the knowledge base:\n");
                for (int i = 0; i < ragResults.size(); i++) {
                    RagResult result = ragResults.get(i);
                    sb.append(String.format("\n--- Context %d [%s: %s] ---\n%s\n",
                            i + 1, result.contentType(), result.sourceId(), result.content()));
                }
            }
        } catch (Exception e) {
            log.debug("RAG retrieval skipped: {}", e.getMessage());
        }
        return sb.toString();
    }

    private AiProviderConfig resolveProvider() {
        return providerConfigRepository.findByIsDefaultTrue()
                .orElseGet(() -> {
                    List<AiProviderConfig> enabled = providerConfigRepository.findByEnabledTrueOrderByPriorityAsc();
                    return enabled.isEmpty() ? null : enabled.get(0);
                });
    }

    private String resolveGeneratedSql(String response, ConversationPlan conversationPlan) {
        String generatedSql = extractSqlFromMarkdown(response);
        if (StringUtils.hasText(generatedSql)) {
            return generatedSql;
        }
        if (conversationPlan != null && StringUtils.hasText(conversationPlan.resolvedSql())) {
            return conversationPlan.resolvedSql().trim();
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

    private boolean isTemplateFastPath(ConversationPlan conversationPlan) {
        return conversationPlan.mode() == PlanMode.TEMPLATE_FAST_PATH
                && StringUtils.hasText(conversationPlan.templateCode());
    }

    private String resolveFastPathGeneratedSql(ConversationPlan plan) {
        if (plan.responseKind() == ConversationPlannerService.ResponseKind.FIXED_REPORT) {
            return null;
        }
        return StringUtils.hasText(plan.resolvedSql()) ? plan.resolvedSql() : null;
    }

    private String formatFastPathResponse(ConversationPlan plan) {
        if (plan.responseKind() == ConversationPlannerService.ResponseKind.FIXED_REPORT) {
            return formatFixedReportResponse(plan);
        }
        return formatTemplateResponse(plan);
    }

    private String formatTemplateResponse(ConversationPlan plan) {
        StringBuilder sb = new StringBuilder();
        if (plan.routedDomain() != null) {
            sb.append("根据您的问题，已从 **").append(plan.routedDomain())
                    .append("** 业务域匹配到预制查询模板");
            if (plan.templateCode() != null) {
                sb.append("（").append(plan.templateCode()).append("）");
            }
            sb.append("。\n\n");
        }
        sb.append("```sql\n").append(plan.resolvedSql().trim()).append("\n```\n");
        if (plan.primaryTarget() != null) {
            sb.append("\n查询目标视图：`").append(plan.primaryTarget()).append("`");
        }
        return sb.toString();
    }

    private String formatFixedReportResponse(ConversationPlan plan) {
        StringBuilder sb = new StringBuilder("根据您的问题，已命中固定报表模板");
        if (StringUtils.hasText(plan.templateCode())) {
            sb.append(" `").append(plan.templateCode()).append("`");
        }
        sb.append("。\n\n");
        if (StringUtils.hasText(plan.routedDomain())) {
            sb.append("- 业务域：").append(plan.routedDomain()).append("\n");
        }
        if (StringUtils.hasText(plan.primaryTarget())) {
            sb.append("- 数据目标：`").append(plan.primaryTarget()).append("`\n");
        }
        sb.append("- 已切换到固定报表快路径，可直接打开报表查看结果。");
        return sb.toString();
    }

    private LlmProviderClient getOrCreateClient(AiProviderConfig provider) {
        String key = provider.getBaseUrl() + "|" + provider.getApiKey() + "|"
                + (provider.getTimeoutSeconds() != null ? provider.getTimeoutSeconds() : 120);
        if (cachedClient != null && key.equals(cachedClientKey)) {
            return cachedClient;
        }
        synchronized (this) {
            if (cachedClient != null && key.equals(cachedClientKey)) {
                return cachedClient;
            }
            cachedClient = clientFactory.create(provider);
            cachedClientKey = key;
            return cachedClient;
        }
    }

    private void writeTokenAndDone(OutputStream out, String text, String sql, ConversationPlan conversationPlan) {
        try {
            String escaped = MAPPER.createObjectNode().put("content", text).toString();
            out.write(("event: token\ndata: " + escaped + "\n\n").getBytes(StandardCharsets.UTF_8));
            writeDoneEvent(out, sql, conversationPlan);
            out.flush();
        } catch (IOException e) {
            log.debug("SSE write failed: {}", e.getMessage());
        }
    }

    private void writeDoneEvent(OutputStream out, String sql, ConversationPlan conversationPlan) {
        try {
            ObjectNode done = MAPPER.createObjectNode();
            if (sql != null) {
                done.put("generatedSql", sql);
            }
            if (conversationPlan != null) {
                if (StringUtils.hasText(conversationPlan.templateCode())) {
                    done.put("templateCode", conversationPlan.templateCode());
                }
                if (conversationPlan.responseKind() != null) {
                    done.put("responseKind", conversationPlan.responseKind().name());
                }
                if (StringUtils.hasText(conversationPlan.routedDomain())) {
                    done.put("routedDomain", conversationPlan.routedDomain());
                }
                if (StringUtils.hasText(conversationPlan.primaryTarget())) {
                    done.put("targetView", conversationPlan.primaryTarget());
                }
            }
            out.write(("event: done\ndata: " + done + "\n\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            log.debug("SSE done event write failed: {}", e.getMessage());
        }
    }

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

    public record ChatExecutionResult(
            String response,
            String generatedSql,
            ConversationPlan conversationPlan,
            String reasoningContent
    ) {}
}
