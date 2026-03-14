package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.yuzhi.dts.copilot.ai.service.llm.gateway.LlmGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    private final LlmGatewayService llmGateway;
    private final SqlSafetyChecker safetyChecker;

    public Nl2SqlService(LlmGatewayService llmGateway, SqlSafetyChecker safetyChecker) {
        this.llmGateway = llmGateway;
        this.safetyChecker = safetyChecker;
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

    private String extractContent(JsonNode response) {
        JsonNode choices = response.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText("");
        }
        return "";
    }
}
