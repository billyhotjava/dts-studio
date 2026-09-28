# CS-04: AgentExecutionService 流式执行

**优先级**: P0
**状态**: READY
**依赖**: CS-01, CS-03

## 目标

在 `AgentExecutionService` 中新增 `executeChatStream()` 方法，集成模板快速通道和流式 ReAct 引擎。

## 技术设计

### 新增方法

```java
public ChatExecutionResult executeChatStream(
        String sessionId, String userId, String userMessage,
        List<Map<String, Object>> history, Long dataSourceId,
        OutputStream sseOutput)
```

### 流程

1. `chatGroundingService.buildContext()` → 如果需要澄清，写 token event + done event，返回
2. 模板快速通道命中 → 写 token event + done event，返回（不调 LLM）
3. 非模板 → `getOrCreateClient()` 获取缓存的 client → 构建 messages → `reActEngine.executeStreaming()` → 写 done event

### done 事件

```java
private void writeDoneEvent(OutputStream out, String sql) {
    ObjectNode done = mapper.createObjectNode();
    if (sql != null) done.put("generatedSql", sql);
    writeSseEvent(out, "done", done.toString());
}
```

## 验收标准

- [ ] 模板命中问题通过 SSE 输出完整文本 + done 事件
- [ ] 非模板问题通过 SSE 输出流式 token + done 事件
- [ ] 编译通过
