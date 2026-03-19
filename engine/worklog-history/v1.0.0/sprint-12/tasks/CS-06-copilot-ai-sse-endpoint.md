# CS-06: copilot-ai SSE 端点

**优先级**: P0
**状态**: READY
**依赖**: CS-05

## 目标

在 `InternalAgentChatResource` 中新增 `POST /internal/agent/chat/send-stream` SSE 端点。

## 技术设计

### 新增端点

```java
@PostMapping(path = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public StreamingResponseBody sendMessageStream(
        @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
        @RequestBody ChatRequest request)
```

- 返回类型 `StreamingResponseBody`，Spring 会异步写入响应体
- 鉴权失败抛 `ResponseStatusException`（在流开始前返回 HTTP 403）
- 内部调用 `agentChatService.sendMessageStream()` 将 SSE 写入 outputStream

### 已有端点不变

`POST /internal/agent/chat/send` 保持原样，作为降级通道。

## 验收标准

- [ ] `curl -X POST .../send-stream -H 'X-Admin-Secret: ...' -d '{"userId":"test","message":"hi"}' ` 返回 SSE 流
- [ ] 无 admin secret 时返回 403
- [ ] 已有 /send 端点行为不变
