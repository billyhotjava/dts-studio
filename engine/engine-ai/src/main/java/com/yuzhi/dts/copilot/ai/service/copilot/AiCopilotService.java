package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.yuzhi.dts.copilot.ai.service.llm.gateway.LlmGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Copilot service for SQL completion, explanation, and optimization.
 */
@Service
public class AiCopilotService {

    private static final Logger log = LoggerFactory.getLogger(AiCopilotService.class);

    private static final String SQL_COMPLETION_SYSTEM_PROMPT =
            "You are a SQL copilot assistant. Help the user write SQL queries. " +
            "When given a partial SQL query or description, complete it with valid SQL. " +
            "Only output the SQL code without explanation unless asked. " +
            "Consider the database context provided.";

    private static final String SQL_EXPLAIN_SYSTEM_PROMPT =
            "You are a SQL expert. Explain the given SQL query in clear, concise natural language. " +
            "Describe what the query does, which tables it accesses, any joins, filters, " +
            "aggregations, and the expected result.";

    private static final String SQL_OPTIMIZE_SYSTEM_PROMPT =
            "You are a SQL optimization expert. Analyze the given SQL query and suggest optimizations. " +
            "Consider index usage, join order, subquery elimination, and query restructuring. " +
            "Provide the optimized SQL and explain what was changed and why.";

    private final LlmGatewayService llmGateway;

    public AiCopilotService(LlmGatewayService llmGateway) {
        this.llmGateway = llmGateway;
    }

    /**
     * Complete a SQL prompt with optional context.
     */
    public String complete(String prompt, String context) throws IOException {
        List<Map<String, Object>> messages = buildMessages(SQL_COMPLETION_SYSTEM_PROMPT, prompt, context);
        JsonNode response = llmGateway.chatCompletion(messages, null, null, null);
        return extractContent(response);
    }

    /**
     * Streaming SQL completion.
     */
    public void completeStream(String prompt, String context, OutputStream outputStream) throws IOException {
        List<Map<String, Object>> messages = buildMessages(SQL_COMPLETION_SYSTEM_PROMPT, prompt, context);
        llmGateway.chatCompletionStream(messages, null, null, outputStream);
    }

    /**
     * Explain a SQL query in natural language.
     */
    public String explain(String sql) throws IOException {
        List<Map<String, Object>> messages = buildMessages(SQL_EXPLAIN_SYSTEM_PROMPT, sql, null);
        JsonNode response = llmGateway.chatCompletion(messages, null, null, null);
        return extractContent(response);
    }

    /**
     * Suggest optimizations for a SQL query.
     */
    public String optimize(String sql) throws IOException {
        List<Map<String, Object>> messages = buildMessages(SQL_OPTIMIZE_SYSTEM_PROMPT, sql, null);
        JsonNode response = llmGateway.chatCompletion(messages, null, null, null);
        return extractContent(response);
    }

    private List<Map<String, Object>> buildMessages(String systemPrompt, String userMessage, String context) {
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", systemPrompt);
        messages.add(system);

        String content = userMessage;
        if (context != null && !context.isBlank()) {
            content = "Context:\n" + context + "\n\n" + userMessage;
        }

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", content);
        messages.add(user);

        return messages;
    }

    private String extractContent(JsonNode response) {
        JsonNode choices = response.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText("");
        }
        return "";
    }
}
