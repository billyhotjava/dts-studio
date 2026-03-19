# Sprint-12: Copilot 对话流式响应与模板快速通道 (CS)

**前缀**: CS (Chat Streaming)
**状态**: READY
**目标**: 通过模板命中短路和端到端 SSE 流式输出两项优化，将 Copilot 对话响应体感从"等待 10-25 秒白屏"提升到"模板问题 <500ms 返回、非模板问题 ~1s 出字"。

## 背景

当前 Copilot 对话链路存在三个性能瓶颈：

1. **模板已命中仍走 LLM** — `ChatGroundingService` 匹配到预制模板并生成 SQL 后，仍将结果作为 context 塞给 LLM 做 ReAct 循环，浪费 10s+ 的 LLM 调用
2. **全程同步阻塞** — 前端 POST → analytics 代理 → copilot-ai → ReAct 多轮 LLM 调用，全部完成后才返回 JSON。用户看到的是长时间白屏
3. **HttpClient 不复用** — `AgentExecutionService.executeChat()` 每次请求 `new OpenAiCompatibleClient()`，TCP/TLS 握手重复开销

## 优化策略

```
用户发消息
    │
    ├─ 模板命中 → 快速通道：跳过 LLM，直接返回 SQL + 解释（<500ms）
    │
    └─ 非模板 → SSE 流式 ReAct：
         ├─ tool_call 阶段：发送进度事件（"正在查表结构..."）
         └─ 最终响应：逐 token 流式输出到浏览器
```

SSE 事件协议：

| 事件类型 | 数据格式 | 说明 |
|---------|---------|------|
| `session` | `{"sessionId":"xxx"}` | 会话 ID（首个事件） |
| `token` | `{"content":"根据"}` | LLM 输出 token |
| `tool` | `{"tool":"schema_lookup","status":"running"}` | 工具执行进度 |
| `done` | `{"generatedSql":"SELECT ..."}` | 完成，含元数据 |
| `error` | `{"error":"..."}` | 错误 |

## 任务列表

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| CS-01 | 模板快速通道 | P0 | READY | - |
| CS-02 | HttpClient 连接复用 | P0 | READY | - |
| CS-03 | ReAct 引擎流式输出 | P0 | READY | CS-02 |
| CS-04 | AgentExecutionService 流式执行 | P0 | READY | CS-01, CS-03 |
| CS-05 | AgentChatService 流式消息 | P0 | READY | CS-04 |
| CS-06 | copilot-ai SSE 端点 | P0 | READY | CS-05 |
| CS-07 | analytics SSE 代理客户端 | P0 | READY | CS-06 |
| CS-08 | analytics SSE 端点 | P0 | READY | CS-07 |
| CS-09 | 前端流式 API 方法 | P0 | READY | CS-08 |
| CS-10 | CopilotChat 流式 UI | P0 | READY | CS-09 |
| CS-11 | IT 集成测试 | P1 | READY | CS-01~10 |

## 架构变化

### 新增端点（不影响已有端点）

| 端点 | 模块 | 协议 |
|------|------|------|
| `POST /internal/agent/chat/send-stream` | copilot-ai | SSE |
| `POST /api/copilot/chat/send-stream` | analytics | SSE |

已有的同步端点 `/send` 保持不变，前端 SSE 失败时降级到同步调用。

### 改动文件清单

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `AgentExecutionService.java` | 修改 | 模板快速通道 + HttpClient 缓存 + `executeChatStream()` |
| `ReActEngine.java` | 修改 | 新增 `executeStreaming()` |
| `AgentChatService.java` | 修改 | 重写 `sendMessageStream()` |
| `InternalAgentChatResource.java` | 修改 | 新增 SSE 端点 |
| `CopilotAgentChatClient.java` | 修改 | 新增 `sendMessageStream()` |
| `CopilotChatResource.java` | 修改 | 新增 SSE 端点 |
| `analyticsApi.ts` | 修改 | 新增 `aiAgentChatSendStream()` |
| `CopilotChat.tsx` | 修改 | 使用流式 API + 降级 |

## 完成标准

- [ ] 模板命中问题（"哪些合同快到期"）响应 < 500ms
- [ ] 非模板问题首 token 出现 < 2s
- [ ] tool_call 阶段浏览器显示进度提示
- [ ] SSE 连接中断时自动降级到同步 API
- [ ] 已有同步端点行为不变
- [ ] 全部 IT 用例通过
