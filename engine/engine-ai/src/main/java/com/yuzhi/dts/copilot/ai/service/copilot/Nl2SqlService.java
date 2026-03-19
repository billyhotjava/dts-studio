package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.yuzhi.dts.copilot.ai.service.llm.gateway.LlmGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts natural language queries to SQL using LLM.
 * Enforces SQL safety - only SELECT/WITH statements are allowed.
 */
@Service
public class Nl2SqlService {

    private static final Logger log = LoggerFactory.getLogger(Nl2SqlService.class);

    private static final String NL2SQL_SYSTEM_PROMPT =
            "You are a SQL generation expert. Convert the user's natural language question into a SQL SELECT query. " +
            "Rules:\n" +
            "1. Only generate SELECT or WITH (CTE) statements - never INSERT, UPDATE, DELETE, DROP, or DDL.\n" +
            "2. Output ONLY the SQL query with no explanation or markdown formatting.\n" +
            "3. Use the provided schema context to reference correct table and column names.\n" +
            "4. Use appropriate JOINs, WHERE clauses, GROUP BY, and ORDER BY as needed.\n" +
            "5. Prefer explicit column names over SELECT *.";

    private static final String NL2SQL_ROUTED_SYSTEM_PROMPT =
            "You are a SQL generation expert. Convert the user's natural language question into a SQL SELECT query. " +
            "Rules:\n" +
            "1. Only generate SELECT or WITH (CTE) statements - never INSERT, UPDATE, DELETE, DROP, or DDL.\n" +
            "2. Output ONLY the SQL query with no explanation or markdown formatting.\n" +
            "3. Use the provided schema context to reference correct table and column names.\n" +
            "4. Use appropriate JOINs, WHERE clauses, GROUP BY, and ORDER BY as needed.\n" +
            "5. Prefer explicit column names over SELECT *.\n" +
            "6. Only query from the provided views - do NOT reference underlying tables directly.\n" +
            "7. Status and enum fields in views are already translated to Chinese labels. " +
            "When filtering by status, use the Chinese label values (e.g., WHERE status = '正常' instead of WHERE status = 1).\n" +
            "8. If routing context is provided, focus your query on the primary view first. " +
            "Only JOIN secondary views when the question explicitly requires cross-domain data.\n\n" +
            "【重要约束 - 结算域隔离规则】\n" +
            "9. 租金、应收、已收、欠款、结算相关问题，必须且只能从 v_monthly_settlement 获取数据\n" +
            "10. 不要使用 v_project_green_current 的 rent 字段来计算月租金总额\n" +
            "11. v_project_overview.total_rent 是当前在摆绿植月租参考值，不是实际结算金额，两者可能不同\n" +
            "12. 结算金额已包含折扣率、计费模式（按实摆/固定月租）和跨月分摊的计算\n" +
            "13. 如果用户问\"XX项目租金\"，查 v_monthly_settlement.total_rent，不要 SUM(v_project_green_current.rent)\n" +
            "14. outstanding_amount = 应收 - 已收，已在视图中计算好，直接使用";

    private final LlmGatewayService llmGateway;
    private final SqlSafetyChecker safetyChecker;
    private final IntentRouterService intentRouter;

    public Nl2SqlService(LlmGatewayService llmGateway, SqlSafetyChecker safetyChecker,
                         IntentRouterService intentRouter) {
        this.llmGateway = llmGateway;
        this.safetyChecker = safetyChecker;
        this.intentRouter = intentRouter;
    }

    /**
     * Generate SQL from natural language.
     *
     * @param naturalLanguage the user's question
     * @param schemaContext   database schema information for context
     * @return generated SQL query
     * @throws IOException              if LLM call fails
     * @throws IllegalArgumentException if generated SQL is unsafe
     */
    public String nl2sql(String naturalLanguage, String schemaContext) throws IOException {
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", NL2SQL_SYSTEM_PROMPT);
        messages.add(system);

        StringBuilder userContent = new StringBuilder();
        if (schemaContext != null && !schemaContext.isBlank()) {
            userContent.append("Database Schema:\n").append(schemaContext).append("\n\n");
        }
        userContent.append("Question: ").append(naturalLanguage);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userContent.toString());
        messages.add(user);

        JsonNode response = llmGateway.chatCompletion(messages, 0.3, null, null);
        String generatedSql = extractContent(response);

        // Sanitize and validate
        generatedSql = safetyChecker.sanitize(generatedSql);

        if (!safetyChecker.isSafe(generatedSql)) {
            log.warn("NL2SQL generated unsafe SQL, blocking: {}", generatedSql);
            throw new IllegalArgumentException(
                    "Generated SQL contains blocked operations. Only SELECT queries are allowed.");
        }

        return generatedSql;
    }

    /**
     * Generate SQL from natural language with routing context.
     * Uses the routed system prompt that instructs the LLM to query views
     * with pre-translated Chinese status labels.
     *
     * @param naturalLanguage the user's question
     * @param schemaContext   database schema information for context
     * @param routingContext  view DDL and enum context filtered by routing result
     * @return generated SQL query
     * @throws IOException              if LLM call fails
     * @throws IllegalArgumentException if generated SQL is unsafe
     */
    public String nl2sql(String naturalLanguage, String schemaContext, String routingContext) throws IOException {
        return nl2sql(naturalLanguage, schemaContext, routingContext, null);
    }

    /**
     * Generate SQL from natural language with routing context and domain awareness.
     * When the domain is "settlement", injects few-shot examples to enforce
     * correct v_monthly_settlement usage.
     *
     * @param naturalLanguage the user's question
     * @param schemaContext   database schema information for context
     * @param routingContext  view DDL and enum context filtered by routing result
     * @param domain          the routed domain (e.g., "settlement", "project", "plant")
     * @return generated SQL query
     * @throws IOException              if LLM call fails
     * @throws IllegalArgumentException if generated SQL is unsafe
     */
    public String nl2sql(String naturalLanguage, String schemaContext, String routingContext,
                         String domain) throws IOException {
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", NL2SQL_ROUTED_SYSTEM_PROMPT);
        messages.add(system);

        // Inject settlement few-shot examples when domain is settlement
        if ("settlement".equals(domain)) {
            String fewShots = loadSettlementFewShots();
            if (fewShots != null && !fewShots.isBlank()) {
                Map<String, Object> fewShotMsg = new LinkedHashMap<>();
                fewShotMsg.put("role", "system");
                fewShotMsg.put("content", fewShots);
                messages.add(fewShotMsg);
                log.debug("Injected settlement few-shot examples for domain: {}", domain);
            }
        }

        StringBuilder userContent = new StringBuilder();
        if (schemaContext != null && !schemaContext.isBlank()) {
            userContent.append("Database Schema:\n").append(schemaContext).append("\n\n");
        }
        if (routingContext != null && !routingContext.isBlank()) {
            userContent.append("Routing Context (target views):\n").append(routingContext).append("\n\n");
        }
        userContent.append("Question: ").append(naturalLanguage);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userContent.toString());
        messages.add(user);

        JsonNode response = llmGateway.chatCompletion(messages, 0.3, null, null);
        String generatedSql = extractContent(response);

        // Sanitize and validate
        generatedSql = safetyChecker.sanitize(generatedSql);

        if (!safetyChecker.isSafe(generatedSql)) {
            log.warn("NL2SQL generated unsafe SQL, blocking: {}", generatedSql);
            throw new IllegalArgumentException(
                    "Generated SQL contains blocked operations. Only SELECT queries are allowed.");
        }

        return generatedSql;
    }

    /**
     * Get the intent router service for external callers that need routing info.
     */
    public IntentRouterService getIntentRouter() {
        return intentRouter;
    }

    /**
     * Load settlement few-shot examples from classpath resource.
     */
    private String loadSettlementFewShots() {
        try (InputStream is = getClass().getResourceAsStream("/prompts/settlement-few-shots.txt")) {
            if (is == null) {
                log.warn("Settlement few-shots resource not found on classpath");
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to load settlement few-shots: {}", e.getMessage());
            return null;
        }
    }

    private String extractContent(JsonNode response) {
        JsonNode choices = response.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText("");
        }
        return "";
    }
}
