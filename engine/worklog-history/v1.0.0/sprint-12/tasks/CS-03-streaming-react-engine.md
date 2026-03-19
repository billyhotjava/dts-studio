# CS-03: ReAct 引擎流式输出

**优先级**: P0
**状态**: READY
**依赖**: CS-02

## 目标

在 `ReActEngine` 中新增 `executeStreaming()` 方法，ReAct 循环中 tool_call 阶段保持同步（需要完整 JSON 解析 tool_calls），最终文本响应阶段将内容按 chunk 写入 SSE OutputStream。

## 技术设计

### 新增方法签名

```java
public String executeStreaming(
        OpenAiCompatibleClient client, String model,
        List<Map<String, Object>> messages, ToolContext toolContext,
        Double temperature, Integer maxTokens,
        OutputStream sseOutput)
```

### 行为

1. tool_call 迭代：同步调 `chatCompletion()`，解析 tool_calls，执行 tool，向 sseOutput 写 `event: tool` 进度事件
2. 最终文本迭代：拿到完整文本后，按 ~20 字符一个 chunk 写 `event: token` 事件
3. 返回完整文本字符串供调用方持久化

### SSE 辅助方法

```java
private void writeSseEvent(OutputStream out, String event, String data) {
    try {
        out.write(("event: " + event + "\ndata: " + data + "\n\n")
                .getBytes(StandardCharsets.UTF_8));
        out.flush();
    } catch (IOException e) {
        log.debug("SSE write failed: {}", e.getMessage());
    }
}
```

## 验收标准

- [ ] 编译通过
- [ ] tool_call 阶段向 OutputStream 写入 `event: tool` 事件
- [ ] 文本响应按 chunk 写入 `event: token` 事件
- [ ] 返回值为完整文本字符串
