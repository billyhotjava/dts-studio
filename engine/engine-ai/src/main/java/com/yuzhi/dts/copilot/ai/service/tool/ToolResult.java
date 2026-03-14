package com.yuzhi.dts.copilot.ai.service.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Result of a tool execution.
 *
 * @param success whether the tool executed successfully
 * @param output  human-readable output text
 * @param data    optional structured data payload
 */
public record ToolResult(boolean success, String output, JsonNode data) {

    public static ToolResult success(String output) {
        return new ToolResult(true, output, null);
    }

    public static ToolResult success(String output, JsonNode data) {
        return new ToolResult(true, output, data);
    }

    public static ToolResult failure(String reason) {
        return new ToolResult(false, reason, null);
    }
}
