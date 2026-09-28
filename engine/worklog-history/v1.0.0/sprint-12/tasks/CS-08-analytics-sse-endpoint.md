# CS-08: analytics SSE 端点

**优先级**: P0
**状态**: READY
**依赖**: CS-07

## 目标

在 `CopilotChatResource` 中新增 `POST /api/copilot/chat/send-stream` SSE 端点，供前端直连。

## 技术设计

### 新增端点

```java
@PostMapping(path = "/send-stream", consumes = MediaType.APPLICATION_JSON_VALUE,
             produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public StreamingResponseBody sendMessageStream(
        @RequestBody ChatSendRequest body, HttpServletRequest request)
```

- 做用户认证和 datasource 解析（与 `/send` 相同逻辑）
- 认证失败或参数错误抛 `ResponseStatusException`
- 调用 `copilotAgentChatClient.sendMessageStream()` 透传 SSE
- 异常时写 `event: error` SSE 事件

### Vite 代理

`/api/copilot/chat/send-stream` 匹配已有的 `/api` → `:8092` 代理规则，无需额外配置。

## 验收标准

- [ ] 浏览器能通过 `fetch()` 连接到 `/api/copilot/chat/send-stream` 并收到 SSE 事件
- [ ] 未认证时返回 401
- [ ] copilot-ai 不可用时返回 `event: error`
