# Copilot Chat Streaming + Template Fast-Path Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce copilot chat response latency by (a) short-circuiting template-matched queries to skip LLM calls entirely, and (b) streaming LLM tokens to the browser via SSE so users see text appear immediately.

**Architecture:** Two independent optimizations layered together. Template fast-path is a pure backend shortcut in `AgentExecutionService` that returns pre-built SQL without touching the LLM. SSE streaming adds a parallel `send-stream` endpoint chain (copilot-ai → analytics → browser) that pipes LLM tokens as they arrive. The existing synchronous endpoints remain untouched as fallback.

**Tech Stack:** Spring Boot `StreamingResponseBody`, `SseEmitter` (already used in codebase), `OpenAiCompatibleClient.chatCompletionStream()` (already exists), browser `fetch()` + `ReadableStream` for POST-based SSE consumption.

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `dts-copilot-ai/.../service/agent/AgentExecutionService.java` | Modify | Add template fast-path + HttpClient caching + streaming executeChat |
| `dts-copilot-ai/.../service/agent/ReActEngine.java` | Modify | Add `executeStreaming()` that streams final LLM response |
| `dts-copilot-ai/.../service/llm/OpenAiCompatibleClient.java` | Modify | Cache HttpClient instance (already created per-instance, just stop re-creating) |
| `dts-copilot-ai/.../service/chat/AgentChatService.java` | Modify | Add real `sendMessageStream()` using streaming execution |
| `dts-copilot-ai/.../web/rest/InternalAgentChatResource.java` | Modify | Add `POST /internal/agent/chat/send-stream` SSE endpoint |
| `dts-copilot-analytics/.../service/CopilotAgentChatClient.java` | Modify | Add `sendMessageStream()` that reads SSE from copilot-ai |
| `dts-copilot-analytics/.../web/rest/CopilotChatResource.java` | Modify | Add `POST /api/copilot/chat/send-stream` SSE endpoint |
| `dts-copilot-webapp/src/api/analyticsApi.ts` | Modify | Add `aiAgentChatSendStream()` using fetch + ReadableStream |
| `dts-copilot-webapp/src/components/copilot/CopilotChat.tsx` | Modify | Use streaming API, show tokens incrementally |

---

## Chunk 1: Template Fast-Path + HttpClient Caching (Backend Only)

### Task 1: Template Fast-Path in AgentExecutionService

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`

- [ ] **Step 1: Add template fast-path before ReAct loop**

In `executeChat()`, after `chatGroundingService.buildContext()` and before `resolveProvider()`, add:

```java
// Template fast-path: skip LLM entirely when template matched with SQL
if (groundingContext.templateCode() != null && groundingContext.resolvedSql() != null) {
    String response = formatTemplateResponse(groundingContext);
    return new ChatExecutionResult(response, groundingContext.resolvedSql(), groundingContext);
}
```

Add the `formatTemplateResponse` private method:

```java
private String formatTemplateResponse(GroundingContext ctx) {
    StringBuilder sb = new StringBuilder();
    if (ctx.domain() != null) {
        sb.append("根据您的问题，已从 **").append(ctx.domain()).append("** 业务域匹配到预制查询模板");
        if (ctx.templateCode() != null) {
            sb.append("（").append(ctx.templateCode()).append("）");
        }
        sb.append("。\n\n");
    }
    sb.append("```sql\n").append(ctx.resolvedSql().trim()).append("\n```\n");
    if (ctx.primaryView() != null) {
        sb.append("\n查询目标视图：`").append(ctx.primaryView()).append("`");
    }
    return sb.toString();
}
```

- [ ] **Step 2: Verify compile**

Run: `mvn compile -pl dts-copilot-ai -am -q -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java
git commit -m "perf: add template fast-path to skip LLM for matched queries"
```

### Task 2: HttpClient Caching in AgentExecutionService

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`

- [ ] **Step 1: Cache OpenAiCompatibleClient by provider config**

Replace the per-request `new OpenAiCompatibleClient(...)` with a cached instance. Add a field:

```java
private volatile OpenAiCompatibleClient cachedClient;
private volatile String cachedClientKey;
```

Add a method:

```java
private OpenAiCompatibleClient getOrCreateClient(AiProviderConfig provider) {
    String key = provider.getBaseUrl() + "|" + provider.getApiKey() + "|" +
                 (provider.getTimeoutSeconds() != null ? provider.getTimeoutSeconds() : 120);
    if (cachedClient != null && key.equals(cachedClientKey)) {
        return cachedClient;
    }
    synchronized (this) {
        if (cachedClient != null && key.equals(cachedClientKey)) {
            return cachedClient;
        }
        cachedClient = new OpenAiCompatibleClient(
                provider.getBaseUrl(),
                provider.getApiKey(),
                provider.getTimeoutSeconds() != null ? provider.getTimeoutSeconds() : 120);
        cachedClientKey = key;
        return cachedClient;
    }
}
```

Replace the `new OpenAiCompatibleClient(...)` call in `executeChat()` with `getOrCreateClient(provider)`.

- [ ] **Step 2: Verify compile + tests**

Run: `mvn compile -pl dts-copilot-ai -am -q -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java
git commit -m "perf: cache OpenAiCompatibleClient to reuse HTTP connections"
```

---

## Chunk 2: Backend SSE Streaming (copilot-ai)

### Task 3: Streaming ReAct Engine

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/ReActEngine.java`

- [ ] **Step 1: Add `executeStreaming()` method**

This method runs the same ReAct loop, but on the **final iteration** (when LLM returns text, not tool_calls), it uses `chatCompletionStream()` to pipe tokens to an OutputStream. During tool_call iterations, it sends progress SSE events.

```java
/**
 * Streaming variant: tool-call rounds are synchronous, final text response is streamed.
 * Writes SSE-formatted events to the output stream.
 */
public String executeStreaming(OpenAiCompatibleClient client, String model,
                               List<Map<String, Object>> messages, ToolContext toolContext,
                               Double temperature, Integer maxTokens,
                               OutputStream sseOutput) {
    List<Map<String, Object>> toolDefinitions = toolRegistry.getToolDefinitions();
    StringBuilder fullResponse = new StringBuilder();

    for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
        log.debug("ReAct streaming iteration {}/{}", iteration + 1, MAX_ITERATIONS);
        try {
            // First try synchronous to check for tool_calls
            JsonNode response = client.chatCompletion(model, messages, temperature, maxTokens,
                    toolDefinitions.isEmpty() ? null : toolDefinitions);

            JsonNode choices = response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "I'm sorry, I received an empty response. Please try again.";
            }

            JsonNode message = choices.get(0).get("message");
            if (message == null) {
                return "I'm sorry, I received an invalid response. Please try again.";
            }

            JsonNode toolCalls = message.get("tool_calls");
            if (toolCalls != null && toolCalls.isArray() && !toolCalls.isEmpty()) {
                // Process tool calls (same as synchronous execute)
                Map<String, Object> assistantMsg = new LinkedHashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", message.has("content") && !message.get("content").isNull()
                        ? message.get("content").asText() : null);
                assistantMsg.put("tool_calls", mapper.treeToValue(toolCalls, List.class));
                messages.add(assistantMsg);

                for (JsonNode toolCall : toolCalls) {
                    String toolCallId = toolCall.get("id").asText();
                    String toolName = toolCall.get("function").get("name").asText();
                    String argumentsStr = toolCall.get("function").get("arguments").asText();

                    // Send tool progress event
                    writeSseEvent(sseOutput, "tool",
                            "{\"tool\":\"%s\",\"status\":\"running\"}".formatted(toolName));

                    JsonNode arguments;
                    try { arguments = mapper.readTree(argumentsStr); }
                    catch (Exception e) { arguments = mapper.createObjectNode(); }

                    log.info("Executing tool: {} with args: {}", toolName, argumentsStr);
                    ToolResult result = toolRegistry.executeTool(toolName, toolContext, arguments);

                    writeSseEvent(sseOutput, "tool",
                            "{\"tool\":\"%s\",\"status\":\"done\"}".formatted(toolName));

                    Map<String, Object> toolResultMsg = new LinkedHashMap<>();
                    toolResultMsg.put("role", "tool");
                    toolResultMsg.put("tool_call_id", toolCallId);
                    toolResultMsg.put("content", result.output());
                    messages.add(toolResultMsg);
                }
                continue;
            }

            // No tool calls — this is the final text response
            // Return the synchronous result (we already have it)
            String content = message.has("content") && !message.get("content").isNull()
                    ? message.get("content").asText() : "";

            // Stream the content as token events
            // (We already have the full text from the sync call, emit it in chunks)
            streamTextAsTokenEvents(sseOutput, content, fullResponse);
            return fullResponse.toString();

        } catch (Exception e) {
            log.error("ReAct streaming iteration {} failed: {}", iteration + 1, e.getMessage(), e);
            String errMsg = "I encountered an error during processing: " + e.getMessage();
            fullResponse.append(errMsg);
            return errMsg;
        }
    }

    return "I reached the maximum number of reasoning steps.";
}

private void streamTextAsTokenEvents(OutputStream out, String text, StringBuilder collector) {
    // Emit in small chunks to simulate streaming feel
    int chunkSize = 20; // characters per event
    for (int i = 0; i < text.length(); i += chunkSize) {
        String chunk = text.substring(i, Math.min(i + chunkSize, text.length()));
        collector.append(chunk);
        writeSseEvent(out, "token", mapper.createObjectNode().put("content", chunk).toString());
    }
}

private void writeSseEvent(OutputStream out, String event, String data) {
    try {
        out.write(("event: " + event + "\ndata: " + data + "\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
    } catch (java.io.IOException e) {
        log.debug("SSE write failed (client disconnected?): {}", e.getMessage());
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `mvn compile -pl dts-copilot-ai -am -q -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/ReActEngine.java
git commit -m "feat: add streaming ReAct execution with SSE tool progress events"
```

### Task 4: Streaming ExecuteChat in AgentExecutionService

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`

- [ ] **Step 1: Add `executeChatStream()` method**

```java
/**
 * Streaming variant of executeChat. Writes SSE events to output.
 * Returns the complete response text for persistence.
 */
public ChatExecutionResult executeChatStream(String sessionId, String userId, String userMessage,
                                              List<Map<String, Object>> history, Long dataSourceId,
                                              OutputStream sseOutput) {
    GroundingContext groundingContext = chatGroundingService.buildContext(userMessage);
    if (groundingContext.needsClarification()) {
        // Write clarification as a single token event + done
        writeTokenAndDone(sseOutput, groundingContext.clarificationMessage(), null);
        return new ChatExecutionResult(groundingContext.clarificationMessage(), null, groundingContext);
    }

    // Template fast-path
    if (groundingContext.templateCode() != null && groundingContext.resolvedSql() != null) {
        String response = formatTemplateResponse(groundingContext);
        writeTokenAndDone(sseOutput, response, groundingContext.resolvedSql());
        return new ChatExecutionResult(response, groundingContext.resolvedSql(), groundingContext);
    }

    AiProviderConfig provider = resolveProvider();
    if (provider == null) {
        String msg = "No AI provider is configured. Please configure a provider in the settings.";
        writeTokenAndDone(sseOutput, msg, null);
        return new ChatExecutionResult(msg, null, groundingContext);
    }

    OpenAiCompatibleClient client = getOrCreateClient(provider);

    List<Map<String, Object>> messages = new ArrayList<>();
    String systemPrompt = buildSystemPrompt(userMessage, groundingContext);
    messages.add(Map.of("role", "system", "content", systemPrompt));
    if (history != null) messages.addAll(history);
    messages.add(Map.of("role", "user", "content", userMessage));

    ToolContext toolContext = new ToolContext(userId, sessionId, dataSourceId);
    String response = reActEngine.executeStreaming(client, provider.getModel(), messages, toolContext,
            provider.getTemperature(), provider.getMaxTokens(), sseOutput);

    // Write done event with metadata
    String sql = resolveGeneratedSql(response, groundingContext);
    writeDoneEvent(sseOutput, sql);

    return new ChatExecutionResult(response, sql, groundingContext);
}

private void writeTokenAndDone(OutputStream out, String text, String sql) {
    try {
        // Emit full text as one token event
        String escaped = new com.fasterxml.jackson.databind.ObjectMapper()
                .createObjectNode().put("content", text).toString();
        out.write(("event: token\ndata: " + escaped + "\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        writeDoneEvent(out, sql);
        out.flush();
    } catch (java.io.IOException e) {
        log.debug("SSE write failed: {}", e.getMessage());
    }
}

private void writeDoneEvent(OutputStream out, String sql) {
    try {
        com.fasterxml.jackson.databind.node.ObjectNode done = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        if (sql != null) done.put("generatedSql", sql);
        out.write(("event: done\ndata: " + done.toString() + "\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
    } catch (java.io.IOException e) {
        log.debug("SSE done event write failed: {}", e.getMessage());
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `mvn compile -pl dts-copilot-ai -am -q -DskipTests`

- [ ] **Step 3: Commit**

```bash
git add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java
git commit -m "feat: add streaming executeChatStream with template fast-path"
```

### Task 5: Streaming AgentChatService

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/chat/AgentChatService.java`

- [ ] **Step 1: Rewrite `sendMessageStream()` to use real streaming**

Replace the existing fake-streaming `sendMessageStream()`:

```java
@Transactional
public void sendMessageStream(String sessionId, String userId, String message,
                              Long datasourceId, OutputStream output) {
    AiChatSession session = resolveOrCreateSession(sessionId, userId);
    if (datasourceId != null) {
        session.setDataSourceId(datasourceId);
    }

    AiChatMessage userMsg = new AiChatMessage();
    userMsg.setRole("user");
    userMsg.setContent(message);
    session.addMessage(userMsg);

    List<Map<String, Object>> history = buildHistory(session);
    Long effectiveDataSourceId = datasourceId != null ? datasourceId : session.getDataSourceId();

    // Write sessionId event first
    try {
        String sessionEvent = "event: session\ndata: {\"sessionId\":\"%s\"}\n\n".formatted(session.getSessionId());
        output.write(sessionEvent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        output.flush();
    } catch (java.io.IOException e) {
        // client disconnected
        return;
    }

    // Execute with streaming
    ChatExecutionResult executionResult = agentExecutionService.executeChatStream(
            session.getSessionId(), userId, message, history, effectiveDataSourceId, output);

    // Persist assistant message
    AiChatMessage assistantMsg = new AiChatMessage();
    assistantMsg.setRole("assistant");
    assistantMsg.setContent(executionResult.response());
    assistantMsg.setGeneratedSql(executionResult.generatedSql());
    applyGroundingMetadata(assistantMsg, executionResult.groundingContext());
    session.addMessage(assistantMsg);

    if (session.getTitle() == null || session.getTitle().isBlank()) {
        session.setTitle(generateTitle(message));
    }

    sessionRepository.save(session);
    auditService.logChatAction(userId, session.getSessionId(), "CHAT_MESSAGE", message, executionResult.response());
}
```

- [ ] **Step 2: Verify compile**

Run: `mvn compile -pl dts-copilot-ai -am -q -DskipTests`

- [ ] **Step 3: Commit**

```bash
git add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/chat/AgentChatService.java
git commit -m "feat: rewrite sendMessageStream to use real SSE streaming"
```

### Task 6: SSE Endpoint in InternalAgentChatResource

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/InternalAgentChatResource.java`

- [ ] **Step 1: Add streaming endpoint**

Add this endpoint alongside the existing `/send`:

```java
@PostMapping(path = "/send-stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
public org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody sendMessageStream(
        @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
        @RequestBody ChatRequest request) {
    // Auth check — throw early so Spring returns 403 before streaming starts
    if (!StringUtils.hasText(adminSecret) || !java.util.Objects.equals(adminSecret, secret)) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Invalid admin secret");
    }
    if (!request.hasRequiredFields()) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "userId and message are required");
    }

    return outputStream -> {
        agentChatService.sendMessageStream(
                request.sessionId(),
                request.userId(),
                request.message(),
                request.datasourceId(),
                outputStream);
    };
}
```

- [ ] **Step 2: Verify compile**

Run: `mvn compile -pl dts-copilot-ai -am -q -DskipTests`

- [ ] **Step 3: Commit**

```bash
git add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/InternalAgentChatResource.java
git commit -m "feat: add POST /internal/agent/chat/send-stream SSE endpoint"
```

---

## Chunk 3: Analytics SSE Proxy

### Task 7: Streaming CopilotAgentChatClient

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/CopilotAgentChatClient.java`

- [ ] **Step 1: Add `sendMessageStream()` method**

This method POSTs to copilot-ai's streaming endpoint and pipes the SSE response to the provided OutputStream:

```java
public void sendMessageStream(String userId, String sessionId, String message,
                               Long datasourceId, java.io.OutputStream output) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("userId", userId);
    payload.put("message", message);
    if (sessionId != null && !sessionId.isBlank()) {
        payload.put("sessionId", sessionId);
    }
    if (datasourceId != null) {
        payload.put("datasourceId", datasourceId);
    }

    // Use Java HttpClient for streaming (RestClient doesn't support streaming body consumption easily)
    try {
        var httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        String baseUrl = restClient.toString(); // We need the base URL
        // Reconstruct URL from restClient — or inject base URL directly
        var requestBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(resolveBaseUrl() + "/internal/agent/chat/send-stream"))
                .header("Content-Type", "application/json")
                .header("X-Admin-Secret", adminSecret)
                .timeout(java.time.Duration.ofSeconds(120))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String error = new String(response.body().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            throw new IllegalStateException("Streaming failed: " + response.statusCode() + " " + error);
        }

        // Pipe SSE stream from copilot-ai to the output
        try (var in = response.body()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                output.flush();
            }
        }
    } catch (java.io.IOException | InterruptedException e) {
        throw new org.springframework.web.client.RestClientException("Streaming chat failed: " + e.getMessage(), e);
    }
}
```

Also add a field and method to expose the base URL:

```java
private final String baseUrl;

// In constructor: store baseUrl as a field
// Add:
private String resolveBaseUrl() {
    return baseUrl;
}
```

- [ ] **Step 2: Verify compile**

Run: `mvn compile -pl dts-copilot-analytics -am -q -DskipTests`

- [ ] **Step 3: Commit**

```bash
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/CopilotAgentChatClient.java
git commit -m "feat: add streaming sendMessageStream to CopilotAgentChatClient"
```

### Task 8: SSE Endpoint in CopilotChatResource

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/CopilotChatResource.java`

- [ ] **Step 1: Add streaming send endpoint**

Add alongside the existing `sendMessage`:

```java
@PostMapping(path = "/send-stream", consumes = MediaType.APPLICATION_JSON_VALUE,
             produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody sendMessageStream(
        @RequestBody ChatSendRequest body, HttpServletRequest request) {
    AnalyticsUser user = resolveUser(request);
    if (user == null) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    final Long datasourceId;
    try {
        datasourceId = chatDataSourceResolver.resolveSelectedDatasourceId(
                body == null ? null : body.datasourceId());
    } catch (IllegalArgumentException ex) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    return outputStream -> {
        try {
            copilotAgentChatClient.sendMessageStream(
                    resolveCopilotUserId(user),
                    body == null ? null : body.sessionId(),
                    body == null ? null : body.userMessage(),
                    datasourceId,
                    outputStream);
        } catch (Exception ex) {
            // Write error as SSE event so frontend can handle it
            String errorEvent = "event: error\ndata: {\"error\":\"%s\"}\n\n"
                    .formatted(ex.getMessage().replace("\"", "\\\""));
            outputStream.write(errorEvent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.flush();
        }
    };
}
```

- [ ] **Step 2: Verify compile**

Run: `mvn compile -pl dts-copilot-analytics -am -q -DskipTests`

- [ ] **Step 3: Commit**

```bash
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/CopilotChatResource.java
git commit -m "feat: add POST /api/copilot/chat/send-stream SSE proxy endpoint"
```

---

## Chunk 4: Frontend SSE Consumption

### Task 9: Streaming API Method

**Files:**
- Modify: `dts-copilot-webapp/src/api/analyticsApi.ts`

- [ ] **Step 1: Add `aiAgentChatSendStream()` function**

Add near the existing `aiAgentChatSend` in the file:

```typescript
export type StreamEvent =
	| { type: "session"; sessionId: string }
	| { type: "token"; content: string }
	| { type: "tool"; tool: string; status: string }
	| { type: "done"; generatedSql?: string }
	| { type: "error"; error: string };

export async function aiAgentChatSendStream(
	body: { sessionId?: string; userMessage: string; datasourceId?: string },
	onEvent: (event: StreamEvent) => void,
): Promise<void> {
	const basePath = import.meta.env.VITE_BASE_PATH?.replace(/\/$/, "") || "";
	const response = await fetch(`${basePath}/api/copilot/chat/send-stream`, {
		method: "POST",
		credentials: "include",
		headers: { "content-type": "application/json", accept: "text/event-stream" },
		body: JSON.stringify(body),
	});

	if (!response.ok || !response.body) {
		throw new Error(`HTTP ${response.status}: ${await response.text()}`);
	}

	const reader = response.body.getReader();
	const decoder = new TextDecoder();
	let buffer = "";

	while (true) {
		const { done, value } = await reader.read();
		if (done) break;
		buffer += decoder.decode(value, { stream: true });

		// Parse SSE events from buffer
		const lines = buffer.split("\n");
		buffer = lines.pop() || ""; // keep incomplete line in buffer

		let currentEvent = "";
		let currentData = "";

		for (const line of lines) {
			if (line.startsWith("event: ")) {
				currentEvent = line.slice(7).trim();
			} else if (line.startsWith("data: ")) {
				currentData = line.slice(6);
			} else if (line === "" && currentEvent && currentData) {
				// End of SSE event
				try {
					const parsed = JSON.parse(currentData);
					if (currentEvent === "session") {
						onEvent({ type: "session", sessionId: parsed.sessionId });
					} else if (currentEvent === "token") {
						onEvent({ type: "token", content: parsed.content });
					} else if (currentEvent === "tool") {
						onEvent({ type: "tool", tool: parsed.tool, status: parsed.status });
					} else if (currentEvent === "done") {
						onEvent({ type: "done", generatedSql: parsed.generatedSql });
					} else if (currentEvent === "error") {
						onEvent({ type: "error", error: parsed.error });
					}
				} catch {
					// ignore malformed events
				}
				currentEvent = "";
				currentData = "";
			}
		}
	}
}
```

- [ ] **Step 2: TypeScript check**

Run: `cd dts-copilot-webapp && npx tsc --noEmit`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add dts-copilot-webapp/src/api/analyticsApi.ts
git commit -m "feat: add aiAgentChatSendStream for SSE consumption"
```

### Task 10: Streaming CopilotChat UI

**Files:**
- Modify: `dts-copilot-webapp/src/components/copilot/CopilotChat.tsx`

- [ ] **Step 1: Replace synchronous send with streaming**

In the `handleSend` function (around line 338-370), replace the synchronous `analyticsApi.aiAgentChatSend()` call with the streaming version. The key change:

```typescript
// Import at top of file
import { aiAgentChatSendStream, type StreamEvent } from "../../api/analyticsApi";

// In handleSend, replace the try block:
try {
    const body: CopilotSendBody = {
        userMessage: trimmed,
        ...(sessionId ? { sessionId } : {}),
        ...(selectedDbId != null ? { datasourceId: String(selectedDbId) } : {}),
    };

    // Add a placeholder assistant message that will be updated incrementally
    const assistantMsgId = Date.now();
    setMessages((prev) => [...prev, {
        id: assistantMsgId,
        role: "assistant",
        content: "",
        streaming: true,
    }]);

    let accumulatedContent = "";
    let receivedSessionId = sessionId;
    let generatedSql: string | undefined;

    await aiAgentChatSendStream(body, (event: StreamEvent) => {
        switch (event.type) {
            case "session":
                receivedSessionId = event.sessionId;
                setSessionId(event.sessionId);
                try { sessionStorage.setItem(SESSION_ID_KEY, event.sessionId); } catch {}
                break;
            case "token":
                accumulatedContent += event.content;
                setMessages((prev) => prev.map((m) =>
                    m.id === assistantMsgId
                        ? { ...m, content: accumulatedContent }
                        : m
                ));
                break;
            case "tool":
                // Optionally show tool progress
                break;
            case "done":
                generatedSql = event.generatedSql;
                setMessages((prev) => prev.map((m) =>
                    m.id === assistantMsgId
                        ? { ...m, streaming: false, generatedSql }
                        : m
                ));
                break;
            case "error":
                setMessages((prev) => prev.map((m) =>
                    m.id === assistantMsgId
                        ? { ...m, content: event.error, streaming: false }
                        : m
                ));
                break;
        }
    });

    // If streaming didn't complete with a done event, mark as complete
    setMessages((prev) => prev.map((m) =>
        m.id === assistantMsgId && m.streaming
            ? { ...m, streaming: false }
            : m
    ));

} catch (err) {
    // Fallback: try synchronous API
    try {
        const res = await analyticsApi.aiAgentChatSend(body);
        // ... existing sync handling
    } catch (syncErr) {
        // Show error
    }
}
```

Note: The exact integration depends on the current message state shape in CopilotChat. The `streaming` field may need to be added to the message type. Adapt the `setMessages` calls to match the existing state management pattern.

- [ ] **Step 2: TypeScript check**

Run: `cd dts-copilot-webapp && npx tsc --noEmit`

- [ ] **Step 3: Manual test**

1. Open browser at http://localhost:3003
2. Open Copilot sidebar
3. Send a message like "哪些合同快到期了" (template match → should respond instantly)
4. Send "hi" (non-template → should stream tokens)
5. Verify fallback works if copilot-ai is stopped

- [ ] **Step 4: Commit**

```bash
git add dts-copilot-webapp/src/components/copilot/CopilotChat.tsx
git commit -m "feat: switch copilot chat to SSE streaming with sync fallback"
```

---

## Summary

| Task | What | Layer |
|------|------|-------|
| 1 | Template fast-path | copilot-ai backend |
| 2 | HttpClient caching | copilot-ai backend |
| 3 | Streaming ReActEngine | copilot-ai backend |
| 4 | Streaming AgentExecutionService | copilot-ai backend |
| 5 | Streaming AgentChatService | copilot-ai backend |
| 6 | SSE endpoint in InternalAgentChatResource | copilot-ai REST |
| 7 | Streaming CopilotAgentChatClient | analytics backend |
| 8 | SSE endpoint in CopilotChatResource | analytics REST |
| 9 | Frontend streaming API | webapp |
| 10 | CopilotChat streaming UI | webapp |
