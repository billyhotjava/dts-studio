package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.service.llm.gateway.LlmGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for AI-powered screen (大屏) generation and revision.
 * Uses LlmGatewayService to call the LLM and produce a screenSpec JSON.
 */
@Service
public class ScreenGenerationService {

	private static final Logger log = LoggerFactory.getLogger(ScreenGenerationService.class);

	private static final String GENERATE_SYSTEM_PROMPT = """
		你是一个数据大屏设计专家。用户会给你一段业务场景描述，你需要生成一个完整的大屏配置 JSON。

		你必须严格返回一个 JSON 对象（不要包裹在 markdown code block 中），包含以下字段：
		{
		  "intent": { "domain": "业务领域", "timeRange": "时间范围", "granularity": "粒度", "metrics": ["指标列表"], "dimensions": ["维度列表"] },
		  "semanticModelHints": { "domain": "领域", "factTable": "事实表名", "timeField": "时间字段" },
		  "queryRecommendations": [{ "id": "q1", "purpose": "用途", "domain": "领域", "factTable": "表名", "timeRange": "时间范围", "granularity": "粒度" }],
		  "sqlBlueprints": [{ "queryId": "q1", "purpose": "用途", "sql": "SELECT ... FROM ...", "factTable": "表名" }],
		  "vizRecommendations": [{ "queryId": "q1", "componentType": "line-chart|bar-chart|pie-chart|kpi-card|table|area-chart|gauge|radar-chart", "title": "组件标题" }],
		  "screenSpec": {
		    "name": "大屏名称",
		    "description": "大屏描述",
		    "width": 1920,
		    "height": 1080,
		    "theme": "glacier",
		    "backgroundColor": "#0a1628",
		    "components": [
		      {
		        "id": "comp_1",
		        "type": "line-chart|bar-chart|pie-chart|kpi-card|table|area-chart|gauge|radar-chart|text|filter-select",
		        "title": "组件标题",
		        "x": 0, "y": 0, "w": 6, "h": 4,
		        "dataSource": { "queryId": "q1" },
		        "config": {}
		      }
		    ],
		    "globalVariables": []
		  },
		  "quality": { "score": 85 }
		}

		布局规则：
		- 画布宽度按 24 列栅格，高度按行（每行约 60px）
		- 第一行通常放 KPI 卡片（3-4 个，w=6 h=2）
		- 中间放图表（w=8 或 w=12，h=4 或 h=5）
		- 底部可放明细表（w=24 h=4）
		- 组件不要重叠

		SQL 规则：
		- 只生成 SELECT 查询，不要 INSERT/UPDATE/DELETE/DROP
		- 使用合理的表名和字段名，与业务场景匹配
		- 如果不确定具体表名，使用业务语义推断（如销售用 sales_order，生产用 production_record）

		只返回 JSON，不要添加任何其他文字。
		""";

	private static final String REVISE_SYSTEM_PROMPT = """
		你是一个数据大屏设计专家。用户会给你一个已有的大屏配置和修改指令，你需要根据指令修改大屏配置。

		你必须严格返回一个 JSON 对象（不要包裹在 markdown code block 中），格式与生成时相同，包含完整的 intent、screenSpec、sqlBlueprints 等字段。
		同时在顶层增加 "actions" 字段（字符串数组），描述你做了哪些修改。

		只返回 JSON，不要添加任何其他文字。
		""";

	private final LlmGatewayService llmGateway;
	private final ObjectMapper objectMapper;

	public ScreenGenerationService(LlmGatewayService llmGateway, ObjectMapper objectMapper) {
		this.llmGateway = llmGateway;
		this.objectMapper = objectMapper;
	}

	/**
	 * Generate a screen spec from a natural language prompt.
	 */
	public ObjectNode generate(String prompt, int width, int height) throws IOException {
		String userMessage = "业务需求：%s\n画布尺寸：%d x %d".formatted(prompt, width, height);

		List<Map<String, Object>> messages = new ArrayList<>();
		messages.add(Map.of("role", "system", "content", GENERATE_SYSTEM_PROMPT));
		messages.add(Map.of("role", "user", "content", userMessage));

		JsonNode llmResponse = llmGateway.chatCompletion(messages, 0.7, 4096, null);
		return extractJsonFromResponse(llmResponse);
	}

	/**
	 * Revise an existing screen spec based on an instruction.
	 */
	public ObjectNode revise(String instruction, JsonNode screenSpec,
	                         List<String> context, String mode) throws IOException {
		StringBuilder userMsg = new StringBuilder();
		userMsg.append("修改指令：").append(instruction).append("\n");
		userMsg.append("模式：").append(mode).append("（apply=直接修改，suggest=仅建议）\n");
		if (context != null && !context.isEmpty()) {
			userMsg.append("历史上下文：\n");
			for (String c : context) {
				userMsg.append("- ").append(c).append("\n");
			}
		}
		userMsg.append("\n当前大屏配置：\n");
		userMsg.append(objectMapper.writeValueAsString(screenSpec));

		List<Map<String, Object>> messages = new ArrayList<>();
		messages.add(Map.of("role", "system", "content", REVISE_SYSTEM_PROMPT));
		messages.add(Map.of("role", "user", "content", userMsg.toString()));

		JsonNode llmResponse = llmGateway.chatCompletion(messages, 0.7, 4096, null);
		return extractJsonFromResponse(llmResponse);
	}

	private ObjectNode extractJsonFromResponse(JsonNode llmResponse) throws IOException {
		// Extract content from OpenAI-compatible response format
		String content = "";
		if (llmResponse.has("choices")) {
			JsonNode choices = llmResponse.get("choices");
			if (choices.isArray() && !choices.isEmpty()) {
				JsonNode message = choices.get(0).get("message");
				if (message != null && message.has("content")) {
					content = message.get("content").asText("");
				}
			}
		} else if (llmResponse.has("content")) {
			content = llmResponse.get("content").asText("");
		}

		if (content.isBlank()) {
			throw new IOException("LLM returned empty content");
		}

		// Strip markdown code fences if present
		content = content.strip();
		if (content.startsWith("```json")) {
			content = content.substring(7);
		} else if (content.startsWith("```")) {
			content = content.substring(3);
		}
		if (content.endsWith("```")) {
			content = content.substring(0, content.length() - 3);
		}
		content = content.strip();

		try {
			JsonNode parsed = objectMapper.readTree(content);
			if (parsed.isObject()) {
				return (ObjectNode) parsed;
			}
			throw new IOException("LLM response is not a JSON object");
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse LLM response as JSON, content length={}", content.length());
			throw new IOException("Failed to parse LLM response as JSON: " + e.getMessage(), e);
		}
	}
}
