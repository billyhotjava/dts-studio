# CS-10: CopilotChat 流式 UI

**优先级**: P0
**状态**: READY
**依赖**: CS-09

## 目标

修改 `CopilotChat.tsx` 的 `handleSend` 函数，默认走流式 API，逐 token 更新消息内容。SSE 失败时降级到同步 API。

## 技术设计

### 改动

在 `handleSend` 中：

1. 先创建一个 `streaming: true` 的空 assistant 消息占位
2. 调用 `aiAgentChatSendStream(body, onEvent)`:
   - `session` 事件 → 更新 sessionId
   - `token` 事件 → 追加到 assistant 消息的 content
   - `tool` 事件 → 可选：显示 "正在查询表结构..." 等提示
   - `done` 事件 → 标记 `streaming: false`，设置 generatedSql
   - `error` 事件 → 显示错误
3. `catch` 块：降级到 `analyticsApi.aiAgentChatSend()`（已有同步逻辑）

### 消息类型扩展

消息状态可能需要加 `streaming?: boolean` 字段。具体看现有的 message type 定义。

### 降级策略

```typescript
try {
    await aiAgentChatSendStream(body, onEvent);
} catch {
    // Fallback to synchronous
    const res = await analyticsApi.aiAgentChatSend(body);
    // ... existing sync handling
}
```

## 验收标准

- [ ] 发送消息后立即看到空 assistant 消息气泡
- [ ] 文字逐步出现（非一次性全量渲染）
- [ ] 模板命中问题秒级返回
- [ ] copilot-ai 挂掉时降级到同步仍可用
- [ ] TypeScript 类型检查通过
