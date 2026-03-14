package com.yuzhi.dts.copilot.ai.service.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.yuzhi.dts.copilot.ai.service.audit.AiAuditService;
import com.yuzhi.dts.copilot.ai.service.tool.CopilotTool;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import com.yuzhi.dts.copilot.ai.service.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Intercepts tool execution to enforce safety policies and log audit events.
 * <p>
 * This interceptor:
 * <ul>
 *   <li>Validates SQL in query-related tools through the {@link SqlSandbox}</li>
 *   <li>Logs all tool executions for audit purposes</li>
 * </ul>
 */
@Component
public class GuardrailsInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GuardrailsInterceptor.class);

    /**
     * Tool names that involve SQL execution and should be checked by the sandbox.
     */
    private static final Set<String> SQL_TOOLS = Set.of("execute_query");

    private final SqlSandbox sqlSandbox;
    private final AiAuditService auditService;

    public GuardrailsInterceptor(SqlSandbox sqlSandbox, AiAuditService auditService) {
        this.sqlSandbox = sqlSandbox;
        this.auditService = auditService;
    }

    /**
     * Intercept and validate a tool execution before it runs.
     *
     * @param tool      the tool to execute
     * @param context   execution context
     * @param arguments parsed arguments
     * @return the tool result (either from the tool itself, or a blocked result)
     */
    public ToolResult intercept(CopilotTool tool, ToolContext context, JsonNode arguments) {
        String toolName = tool.name();

        // Pre-execution: SQL safety check for SQL tools
        if (SQL_TOOLS.contains(toolName)) {
            String sql = arguments.has("sql") ? arguments.get("sql").asText() : null;
            if (sql != null) {
                SqlSandbox.SafetyResult safety = sqlSandbox.validate(sql);
                if (!safety.safe()) {
                    log.warn("Guardrail blocked tool '{}': {}", toolName, safety.reason());
                    auditService.logToolExecution(context.userId(), context.sessionId(),
                            toolName, arguments.toString(), false, safety.reason());
                    return ToolResult.failure("Blocked by safety guardrail: " + safety.reason());
                }
            }
        }

        // Execute the tool
        ToolResult result;
        try {
            result = tool.execute(context, arguments);
        } catch (Exception e) {
            log.error("Tool '{}' threw exception: {}", toolName, e.getMessage(), e);
            auditService.logToolExecution(context.userId(), context.sessionId(),
                    toolName, arguments.toString(), false, e.getMessage());
            return ToolResult.failure("Tool error: " + e.getMessage());
        }

        // Post-execution: audit log
        auditService.logToolExecution(context.userId(), context.sessionId(),
                toolName, arguments.toString(), result.success(),
                result.success() ? null : result.output());

        return result;
    }
}
