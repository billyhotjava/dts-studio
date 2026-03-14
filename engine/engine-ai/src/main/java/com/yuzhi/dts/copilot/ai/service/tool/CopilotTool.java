package com.yuzhi.dts.copilot.ai.service.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for all tools that can be invoked by the AI agent.
 * Implementations should be annotated with {@code @Component} for auto-discovery.
 */
public interface CopilotTool {

    /**
     * Unique tool name used in LLM function-calling.
     */
    String name();

    /**
     * Human-readable description shown to the LLM.
     */
    String description();

    /**
     * JSON Schema describing the tool's parameters.
     */
    JsonNode parameterSchema();

    /**
     * Execute the tool with the given context and arguments.
     *
     * @param context   execution context (user, session, data source)
     * @param arguments parsed JSON arguments matching {@link #parameterSchema()}
     * @return the execution result
     */
    ToolResult execute(ToolContext context, JsonNode arguments);
}
