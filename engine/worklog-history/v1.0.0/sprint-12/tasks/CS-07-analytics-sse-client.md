# CS-07: analytics SSE 代理客户端

**优先级**: P0
**状态**: READY
**依赖**: CS-06

## 目标

在 `CopilotAgentChatClient` 中新增 `sendMessageStream()` 方法，POST 到 copilot-ai 的 SSE 端点并将响应流透传到调用方的 OutputStream。

## 技术设计

### 新增方法

```java
public void sendMessageStream(String userId, String sessionId, String message,
                               Long datasourceId, OutputStream output)
```

### 实现要点

- 使用 `java.net.http.HttpClient` 发 POST 请求（RestClient 不支持流式 body 消费）
- `HttpResponse.BodyHandlers.ofInputStream()` 获取 SSE 流
- 逐块读取 `response.body()` 并 `write()` 到 output，每次读后 `flush()`
- baseUrl 需要作为字段保存（当前构造函数中只传给了 RestClient）

### 改动

1. 构造函数新增 `this.baseUrl = baseUrl` 字段保存
2. 新增 `sendMessageStream()` 方法

## 验收标准

- [ ] 编译通过
- [ ] 能将 copilot-ai 的 SSE 流透传到 OutputStream
- [ ] 已有同步方法 `sendMessage()` 不受影响
