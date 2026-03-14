package com.yuzhi.dts.copilot.ai.service.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for all available tools.
 * Auto-discovers {@link CopilotTool} beans at startup and supports dynamic registration.
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, CopilotTool> tools = new ConcurrentHashMap<>();

    /**
     * Constructor injection: auto-discovers all CopilotTool beans.
     */
    public ToolRegistry(List<CopilotTool> toolBeans) {
        for (CopilotTool tool : toolBeans) {
            register(tool);
        }
        log.info("ToolRegistry initialized with {} tools: {}", tools.size(), tools.keySet());
    }

    /**
     * Register a tool. Overwrites any existing tool with the same name.
     */
    public void register(CopilotTool tool) {
        tools.put(tool.name(), tool);
        log.debug("Registered tool: {}", tool.name());
    }

    /**
     * Unregister a tool by name.
     */
    public void unregister(String name) {
        CopilotTool removed = tools.remove(name);
        if (removed != null) {
            log.debug("Unregistered tool: {}", name);
        }
    }

    /**
     * Get a tool by name.
     */
    public Optional<CopilotTool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * Get tool definitions in the OpenAI function-calling format for LLM requests.
     *
     * @return list of tool definitions as Maps suitable for JSON serialization
     */
    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (CopilotTool tool : tools.values()) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", tool.parameterSchema());

            Map<String, Object> toolDef = new LinkedHashMap<>();
            toolDef.put("type", "function");
            toolDef.put("function", function);

            definitions.add(toolDef);
        }
        return definitions;
    }

    /**
     * Find and execute a tool by name.
     *
     * @param name      the tool name
     * @param context   execution context
     * @param arguments parsed JSON arguments
     * @return the tool result, or a failure result if the tool is not found
     */
    public ToolResult executeTool(String name, ToolContext context, JsonNode arguments) {
        CopilotTool tool = tools.get(name);
        if (tool == null) {
            log.warn("Tool not found: {}", name);
            return ToolResult.failure("Unknown tool: " + name);
        }
        try {
            return tool.execute(context, arguments);
        } catch (Exception e) {
            log.error("Tool execution failed: {} - {}", name, e.getMessage(), e);
            return ToolResult.failure("Tool execution error: " + e.getMessage());
        }
    }

    /**
     * Get the names of all registered tools.
     */
    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }
}
