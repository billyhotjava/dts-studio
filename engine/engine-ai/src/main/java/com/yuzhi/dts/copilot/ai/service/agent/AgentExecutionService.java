package com.yuzhi.dts.copilot.ai.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.AiDataSourceRepository;
import com.yuzhi.dts.copilot.ai.repository.AiProviderConfigRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.CopilotChatContract;
import com.yuzhi.dts.copilot.ai.service.copilot.CopilotChatRequestContext;
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
import java.util.Locale;
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

    private static final String DEFAULT_DBT_DATA_SOURCE_NAME = "DTS dbt模型库";

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

            ## 新报表草稿工作流

            当用户明确要求生成新的报表、图表、看板草稿或自定义统计时：
            1. 先结合业务路由和语义包确认优先数据层，优先使用 MART/ADS/DWS 聚合表，不要默认下钻 ODS 原始表
            2. 先对目标 dbt 模型调用 schema_lookup 工具验证表名和字段名；SQL 只能使用 schema_lookup 返回字段，不要猜测旧 ODS 字段
            3. 创建可保存的分析草稿：生成只读 SQL，并按需要调用 execute_query 做小样本预览
            4. 回复中必须包含一个 ```sql 代码块，并说明统计口径、推荐图表类型和可能的数据质量 caveat
            5. 不要把固定报表模板当成新报表草稿返回，除非用户明确要求打开已有报表
            6. 指标摘要、口径说明、推荐图表和数据质量提示统一使用标准 Markdown 表格；报表摘要表固定使用表头 `| 指标 | 结果 | 说明 |`，第二行必须是 `| --- | --- | --- |`
            7. 不要只输出指标说明表，必须包含 ```sql 代码块；如果字段不足以生成 SQL，明确说明缺失字段并请求补充，不要声称已生成报表草稿

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
    private final AiDataSourceRepository dataSourceRepository;

    private volatile LlmProviderClient cachedClient;
    private volatile String cachedClientKey;

    public AgentExecutionService(ReActEngine reActEngine,
                                 RagService ragService,
                                 AiProviderConfigRepository providerConfigRepository,
                                 ConversationPlannerService conversationPlannerService,
                                 LlmProviderClientFactory clientFactory,
                                 AiDataSourceRepository dataSourceRepository) {
        this.reActEngine = reActEngine;
        this.ragService = ragService;
        this.providerConfigRepository = providerConfigRepository;
        this.conversationPlannerService = conversationPlannerService;
        this.clientFactory = clientFactory;
        this.dataSourceRepository = dataSourceRepository;
    }

    public ChatExecutionResult executeChat(String sessionId, String userId, String userMessage,
                                           List<Map<String, Object>> history, Long dataSourceId) {
        return executeChat(sessionId, userId, userMessage, history, dataSourceId, Collections.emptyMap());
    }

    public ChatExecutionResult executeChat(String sessionId, String userId, String userMessage,
                                           List<Map<String, Object>> history, Long dataSourceId,
                                           Map<String, Boolean> martHealthSnapshot) {
        return executeChat(
                sessionId,
                userId,
                userMessage,
                history,
                dataSourceId,
                martHealthSnapshot,
                Collections.emptyMap(),
                Collections.emptyMap());
    }

    public ChatExecutionResult executeChat(String sessionId, String userId, String userMessage,
                                           List<Map<String, Object>> history, Long dataSourceId,
                                           Map<String, Boolean> martHealthSnapshot,
                                           Map<String, String> assumptionOverrides,
                                           Map<String, String> clarificationAnswers) {
        CopilotChatRequestContext requestContext = CopilotChatRequestContext.of(
                martHealthSnapshot, assumptionOverrides, clarificationAnswers);
        ConversationPlan conversationPlan = planConversation(userMessage, requestContext);
        if (conversationPlan.mode() == PlanMode.DIRECT_RESPONSE) {
            return new ChatExecutionResult(conversationPlan.directResponse(), null, conversationPlan, null, requestContext);
        }
        if (isTemplateFastPath(conversationPlan)) {
            String response = formatFastPathResponse(conversationPlan);
            String generatedSql = resolveFastPathGeneratedSql(conversationPlan);
            return new ChatExecutionResult(response, generatedSql, conversationPlan, null, requestContext);
        }

        AiProviderConfig provider = resolveProvider();
        if (provider == null) {
            return new ChatExecutionResult(
                    "No AI provider is configured. Please configure a provider in the settings.",
                    null,
                    conversationPlan,
                    null,
                    requestContext
            );
        }

        LlmProviderClient client = getOrCreateClient(provider);
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildSystemPrompt(userMessage, conversationPlan, requestContext));
        messages.add(systemMsg);
        if (history != null) {
            messages.addAll(history);
        }
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        ToolContext toolContext = new ToolContext(userId, sessionId, resolveToolDataSourceId(dataSourceId, conversationPlan));
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
                null,
                requestContext
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
        return executeChatStream(
                sessionId,
                userId,
                userMessage,
                history,
                dataSourceId,
                martHealthSnapshot,
                Collections.emptyMap(),
                Collections.emptyMap(),
                sseOutput);
    }

    public ChatExecutionResult executeChatStream(String sessionId, String userId, String userMessage,
                                                 List<Map<String, Object>> history, Long dataSourceId,
                                                 Map<String, Boolean> martHealthSnapshot,
                                                 Map<String, String> assumptionOverrides,
                                                 Map<String, String> clarificationAnswers,
                                                 OutputStream sseOutput) {
        CopilotChatRequestContext requestContext = CopilotChatRequestContext.of(
                martHealthSnapshot, assumptionOverrides, clarificationAnswers);
        ConversationPlan conversationPlan = planConversation(userMessage, requestContext);
        if (conversationPlan.mode() == PlanMode.DIRECT_RESPONSE) {
            writeTokenAndDone(sseOutput, conversationPlan.directResponse(), null, conversationPlan, null, requestContext);
            return new ChatExecutionResult(conversationPlan.directResponse(), null, conversationPlan, null, requestContext);
        }
        if (isTemplateFastPath(conversationPlan)) {
            String response = formatFastPathResponse(conversationPlan);
            String generatedSql = resolveFastPathGeneratedSql(conversationPlan);
            writeTokenAndDone(sseOutput, response, generatedSql, conversationPlan, null, requestContext);
            return new ChatExecutionResult(response, generatedSql, conversationPlan, null, requestContext);
        }

        AiProviderConfig provider = resolveProvider();
        if (provider == null) {
            String message = "No AI provider is configured. Please configure a provider in the settings.";
            writeTokenAndDone(sseOutput, message, null, conversationPlan, null, requestContext);
            return new ChatExecutionResult(message, null, conversationPlan, null, requestContext);
        }

        LlmProviderClient client = getOrCreateClient(provider);
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildSystemPrompt(userMessage, conversationPlan, requestContext));
        messages.add(systemMsg);
        if (history != null) {
            messages.addAll(history);
        }
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        ToolContext toolContext = new ToolContext(userId, sessionId, resolveToolDataSourceId(dataSourceId, conversationPlan));
        String response = reActEngine.executeStreaming(
                client,
                provider.getModel(),
                messages,
                toolContext,
                provider.getTemperature(),
                provider.getMaxTokens(),
                sseOutput);
        String sql = resolveGeneratedSql(response, conversationPlan);
        writeDoneEvent(sseOutput, sql, conversationPlan, inferSuggestedDisplay(userMessage, sql, conversationPlan), requestContext);

        return new ChatExecutionResult(
                response,
                sql,
                conversationPlan,
                extractReasoningFromMessages(messages),
                requestContext
        );
    }

    private String buildSystemPrompt(String userMessage, ConversationPlan conversationPlan) {
        return buildSystemPrompt(userMessage, conversationPlan, CopilotChatRequestContext.empty());
    }

    private ConversationPlan planConversation(String userMessage, CopilotChatRequestContext requestContext) {
        if (requestContext != null
                && requestContext.assumptionOverrides().containsKey("metric")) {
            return conversationPlannerService.plan(userMessage, requestContext);
        }
        return conversationPlannerService.plan(
                userMessage,
                requestContext == null ? Collections.emptyMap() : requestContext.martHealthSnapshot());
    }

    private String buildSystemPrompt(
            String userMessage,
            ConversationPlan conversationPlan,
            CopilotChatRequestContext requestContext) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);
        if (conversationPlan != null && StringUtils.hasText(conversationPlan.promptContext())) {
            sb.append("\n\nBusiness grounding context:\n")
                    .append(conversationPlan.promptContext())
                    .append("\n");
        }
        appendRequestContractContext(sb, requestContext);
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

    private void appendRequestContractContext(StringBuilder sb, CopilotChatRequestContext requestContext) {
        if (requestContext == null) {
            return;
        }
        if (requestContext.hasAssumptionOverrides()) {
            sb.append("\n\n用户已确认的口径覆盖:\n");
            requestContext.assumptionOverrides().forEach((key, value) ->
                    sb.append("- ").append(key).append("=").append(value).append("\n"));
            sb.append("生成 SQL 和分析结论时必须优先遵守这些口径覆盖。\n");
        }
        if (requestContext.hasClarificationAnswers()) {
            sb.append("\n\n用户已回答的澄清项:\n");
            requestContext.clarificationAnswers().forEach((key, value) ->
                    sb.append("- ").append(key).append("=").append(value).append("\n"));
            sb.append("不要再次询问同一澄清项,请按答案继续执行。\n");
        }
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
            return normalizeReadOnlySql(generatedSql);
        }
        if (conversationPlan != null && StringUtils.hasText(conversationPlan.resolvedSql())) {
            return normalizeReadOnlySql(conversationPlan.resolvedSql());
        }
        return null;
    }

    private String normalizeReadOnlySql(String sql) {
        if (!StringUtils.hasText(sql)) {
            return null;
        }
        String trimmed = stripLeadingSqlComments(sql);
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (!(normalized.startsWith("select ") || normalized.startsWith("with "))) {
            log.warn("Rejected non-read-only generated SQL because it does not start with SELECT/WITH");
            return null;
        }
        if (normalized.matches("(?s).*;\\s*\\S+.*")) {
            log.warn("Rejected generated SQL with multiple statements");
            return null;
        }
        if (normalized.matches("(?s).*\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|merge|call|execute)\\b.*")) {
            log.warn("Rejected generated SQL containing a disallowed keyword");
            return null;
        }
        return trimmed;
    }

    private String stripLeadingSqlComments(String sql) {
        String trimmed = sql == null ? "" : sql.trim();
        boolean changed = true;
        while (changed && StringUtils.hasText(trimmed)) {
            changed = false;
            if (trimmed.startsWith("--")) {
                int newline = trimmed.indexOf('\n');
                trimmed = newline >= 0 ? trimmed.substring(newline + 1).trim() : "";
                changed = true;
            } else if (trimmed.startsWith("/*")) {
                int end = trimmed.indexOf("*/", 2);
                trimmed = end >= 0 ? trimmed.substring(end + 2).trim() : "";
                changed = true;
            }
        }
        return trimmed;
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

    private Long resolveToolDataSourceId(Long requestedDataSourceId, ConversationPlan conversationPlan) {
        if (!isDbtMartPlan(conversationPlan)) {
            return requestedDataSourceId;
        }
        try {
            return dataSourceRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                    .filter(source -> DEFAULT_DBT_DATA_SOURCE_NAME.equalsIgnoreCase(String.valueOf(source.getName())))
                    .filter(source -> !StringUtils.hasText(source.getStatus()) || "ACTIVE".equalsIgnoreCase(source.getStatus()))
                    .map(source -> source.getId())
                    .filter(id -> id != null && id > 0)
                    .findFirst()
                    .orElse(requestedDataSourceId);
        } catch (Exception ex) {
            log.warn("Failed to resolve dbt mart datasource for agent tools: {}", ex.getMessage());
            return requestedDataSourceId;
        }
    }

    private boolean isDbtMartPlan(ConversationPlan conversationPlan) {
        if (conversationPlan == null) {
            return false;
        }
        if ("L1_DBT_MART".equalsIgnoreCase(String.valueOf(conversationPlan.dataSurface()))) {
            return true;
        }
        String target = String.valueOf(conversationPlan.primaryTarget()).toLowerCase(Locale.ROOT);
        return target.contains("xycyl_ads_flowerbiz_")
                || target.contains("xycyl_dws_flowerbiz_")
                || target.contains("xycyl_dwd_flowerbiz_")
                || target.contains("xycyl_dim_flowerbiz_");
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

    private void writeTokenAndDone(
            OutputStream out,
            String text,
            String sql,
            ConversationPlan conversationPlan,
            String suggestedDisplay) {
        writeTokenAndDone(out, text, sql, conversationPlan, suggestedDisplay, CopilotChatRequestContext.empty());
    }

    private void writeTokenAndDone(
            OutputStream out,
            String text,
            String sql,
            ConversationPlan conversationPlan,
            String suggestedDisplay,
            CopilotChatRequestContext requestContext) {
        try {
            String escaped = MAPPER.createObjectNode().put("content", text).toString();
            out.write(("event: token\ndata: " + escaped + "\n\n").getBytes(StandardCharsets.UTF_8));
            writeDoneEvent(out, sql, conversationPlan, suggestedDisplay, requestContext);
            out.flush();
        } catch (IOException e) {
            log.debug("SSE write failed: {}", e.getMessage());
        }
    }

    private void writeDoneEvent(
            OutputStream out,
            String sql,
            ConversationPlan conversationPlan,
            String suggestedDisplay) {
        writeDoneEvent(out, sql, conversationPlan, suggestedDisplay, CopilotChatRequestContext.empty());
    }

    private void writeDoneEvent(
            OutputStream out,
            String sql,
            ConversationPlan conversationPlan,
            String suggestedDisplay,
            CopilotChatRequestContext requestContext) {
        try {
            ObjectNode done = MAPPER.createObjectNode();
            if (sql != null) {
                done.put("generatedSql", sql);
            }
            String effectiveSuggestedDisplay = conversationPlan != null
                    && StringUtils.hasText(conversationPlan.suggestedDisplay())
                    ? conversationPlan.suggestedDisplay()
                    : suggestedDisplay;
            if (StringUtils.hasText(effectiveSuggestedDisplay)) {
                done.put("suggestedDisplay", effectiveSuggestedDisplay);
            }
            if (conversationPlan != null) {
                if (StringUtils.hasText(conversationPlan.templateCode())) {
                    done.put("templateCode", conversationPlan.templateCode());
                }
                if (StringUtils.hasText(conversationPlan.reportCode())) {
                    done.put("reportCode", conversationPlan.reportCode());
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
                if (StringUtils.hasText(conversationPlan.dataSurface())) {
                    done.put("dataSurface", conversationPlan.dataSurface());
                }
                if (StringUtils.hasText(conversationPlan.qualityLevel())) {
                    done.put("qualityLevel", conversationPlan.qualityLevel());
                }
                if (!conversationPlan.qualityNotes().isEmpty()) {
                    var qualityNotes = done.putArray("qualityNotes");
                    conversationPlan.qualityNotes().forEach(qualityNotes::add);
                }
                if (!conversationPlan.sourceRefs().isEmpty()) {
                    var sourceRefs = done.putArray("sourceRefs");
                    conversationPlan.sourceRefs().forEach(sourceRefs::add);
                }
                CopilotChatContract.putDoneFields(done, conversationPlan, sql, requestContext);
            }
            out.write(("event: done\ndata: " + done + "\n\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            log.debug("SSE done event write failed: {}", e.getMessage());
        }
    }

    private String inferSuggestedDisplay(String userMessage, String sql, ConversationPlan conversationPlan) {
        if (conversationPlan == null
                || conversationPlan.responseKind() != ConversationPlannerService.ResponseKind.REPORT_DRAFT) {
            return null;
        }
        if (StringUtils.hasText(conversationPlan.suggestedDisplay())) {
            return conversationPlan.suggestedDisplay();
        }
        String text = (String.valueOf(userMessage) + "\n" + String.valueOf(sql) + "\n"
                + String.valueOf(conversationPlan.primaryTarget())).toLowerCase();
        if (containsAny(text, "趋势", "走势", "月度", "按月", "按日", "date_trunc", "month_id", "day_id", "order by month")) {
            return "line";
        }
        if (containsAny(text, "排行", "排名", "top", "最多", "最高", "最低", "rank")) {
            return "bar";
        }
        if (containsAny(text, "占比", "比例", "构成", "分布", "rate", "ratio", "percent")) {
            return "pie";
        }
        if (containsAny(text, "总览", "指标", "kpi", "总数", "合计")) {
            return "scalar";
        }
        return "table";
    }

    private boolean containsAny(String text, String... patterns) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
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
            String reasoningContent,
            CopilotChatRequestContext requestContext
    ) {
        public ChatExecutionResult(
                String response,
                String generatedSql,
                ConversationPlan conversationPlan,
                String reasoningContent) {
            this(response, generatedSql, conversationPlan, reasoningContent, CopilotChatRequestContext.empty());
        }
    }
}
