# CS-05: AgentChatService 流式消息

**优先级**: P0
**状态**: READY
**依赖**: CS-04

## 目标

重写 `AgentChatService.sendMessageStream()` 从假流式（调同步方法再包一层 SSE）变为调用 `executeChatStream()` 的真流式。

## 技术设计

### 改动

替换 `sendMessageStream()` 方法体：

1. 解析/创建 session
2. 持久化 user message
3. 先写 `event: session` 事件（含 sessionId）
4. 调 `agentExecutionService.executeChatStream()` → 真流式写入 sseOutput
5. 持久化 assistant message（用 executeChatStream 的返回值）
6. 生成 title、save session

### 关键点

- `@Transactional` 在方法返回后才 commit，流式输出在事务提交之前完成
- session 事件在流开始时就发出，前端立即拿到 sessionId

## 验收标准

- [ ] 编译通过
- [ ] 流式输出的第一个事件是 `event: session`
- [ ] 流结束后消息被正确持久化到数据库
