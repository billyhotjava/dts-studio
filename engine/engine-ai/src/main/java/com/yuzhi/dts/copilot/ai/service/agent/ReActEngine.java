package com.yuzhi.dts.copilot.ai.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.llm.OpenAiCompatibleClient;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import com.yuzhi.dts.copilot.ai.service.tool.ToolRegistry;
import com.yuzhi.dts.copilot.ai.service.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReAct (Reasoning + Acting) engine for the AI agent.
 * <p>
 * Loop:
 * <ol>
 *   <li>Send user message + tool definitions to LLM</li>
 *   <li>If LLM returns tool_calls, execute tools, feed results back, and loop</li>
 *   <li>If LLM returns text content, done</li>
 * </ol>
 * Maximum of 10 iterations to prevent infinite loops.
 */
@Component
public class ReActEngine {

    private static final Logger log = LoggerFactory.getLogger(ReActEngine.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MAX_ITERATIONS = 10;

    private final ToolRegistry toolRegistry;

    public ReActEngine(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * Execute the ReAct loop with the given LLM client and conversation messages.
     *
     * @param client      the LLM client to use
     * @param model       the model name
     * @param messages    the conversation messages (will be mutated with tool call/result messages)
     * @param toolContext context for tool execution
     * @param temperature LLM temperature
     * @param maxTokens   LLM max tokens
     * @return the final text response from the LLM
     */
    public String execute(OpenAiCompatibleClient client, String model,
                          List<Map<String, Object>> messages, ToolContext toolContext,
                          Double temperature, Integer maxTokens) {
        List<Map<String, Object>> toolDefinitions = toolRegistry.getToolDefinitions();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            log.debug("ReAct iteration {}/{}", iteration + 1, MAX_ITERATIONS);

            try {
                JsonNode response = client.chatCompletion(model, messages, temperature, maxTokens,
                        toolDefinitions.isEmpty() ? null : toolDefinitions);

                JsonNode choices = response.get("choices");
                if (choices == null || choices.isEmpty()) {
                    log.error("No choices in LLM response");
                    return "I'm sorry, I received an empty response. Please try again.";
                }

                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                if (message == null) {
                    return "I'm sorry, I received an invalid response. Please try again.";
                }

                // Check for tool calls
                JsonNode toolCalls = message.get("tool_calls");
                if (toolCalls != null && toolCalls.isArray() && !toolCalls.isEmpty()) {
                    // Add the assistant message with tool calls to conversation
                    Map<String, Object> assistantMsg = new LinkedHashMap<>();
                    assistantMsg.put("role", "assistant");
                    if (message.has("content") && !message.get("content").isNull()) {
                        assistantMsg.put("content", message.get("content").asText());
                    } else {
                        assistantMsg.put("content", null);
                    }
                    assistantMsg.put("tool_calls", mapper.treeToValue(toolCalls, List.class));
                    messages.add(assistantMsg);

                    // Execute each tool call and add results
                    for (JsonNode toolCall : toolCalls) {
                        String toolCallId = toolCall.get("id").asText();
                        JsonNode function = toolCall.get("function");
                        String toolName = function.get("name").asText();
                        String argumentsStr = function.get("arguments").asText();

                        JsonNode arguments;
                        try {
                            arguments = mapper.readTree(argumentsStr);
                        } catch (Exception e) {
                            arguments = mapper.createObjectNode();
                        }

                        log.info("Executing tool: {} with args: {}", toolName, argumentsStr);
                        ToolResult result = toolRegistry.executeTool(toolName, toolContext, arguments);

                        Map<String, Object> toolResultMsg = new LinkedHashMap<>();
                        toolResultMsg.put("role", "tool");
                        toolResultMsg.put("tool_call_id", toolCallId);
                        toolResultMsg.put("content", result.output());
                        messages.add(toolResultMsg);
                    }

                    // Continue the loop - LLM needs to process tool results
                    continue;
                }

                // No tool calls - LLM returned a text response, we're done
                String content = message.has("content") && !message.get("content").isNull()
                        ? message.get("content").asText()
                        : "";
                return content;

            } catch (Exception e) {
                log.error("ReAct iteration {} failed: {}", iteration + 1, e.getMessage(), e);
                return "I encountered an error during processing: " + e.getMessage();
            }
        }

        log.warn("ReAct engine reached maximum iterations ({})", MAX_ITERATIONS);
        return "I reached the maximum number of reasoning steps. Here is what I have so far based on the conversation.";
    }
}
