# CS-02: HttpClient 连接复用

**优先级**: P0
**状态**: READY
**依赖**: -

## 目标

消除每次 `executeChat()` 都 `new OpenAiCompatibleClient()` 导致的 TCP/TLS 握手重复开销。

## 技术设计

在 `AgentExecutionService` 中缓存 client 实例，按 provider 配置（baseUrl + apiKey + timeout）做 key，配置不变时复用同一个 HttpClient。

### 改动位置

`AgentExecutionService.java` — 新增字段和方法：

```java
private volatile OpenAiCompatibleClient cachedClient;
private volatile String cachedClientKey;

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
                provider.getBaseUrl(), provider.getApiKey(),
                provider.getTimeoutSeconds() != null ? provider.getTimeoutSeconds() : 120);
        cachedClientKey = key;
        return cachedClient;
    }
}
```

替换 `executeChat()` 中的 `new OpenAiCompatibleClient(...)` 为 `getOrCreateClient(provider)`。

## 预期效果

每次请求省去 TCP 握手 + TLS 握手开销，对通义千问约 **200-500ms**。

## 验收标准

- [ ] 编译通过，单元测试通过
- [ ] 连续发两条消息，第二条不再创建新 HttpClient（日志可观测）
