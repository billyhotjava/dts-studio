# AE-02: LLM Gateway 服务（多 Provider + 熔断降级）

**状态**: READY
**依赖**: AE-01

## 目标

从 dts-platform 抽取 `AiGatewayService`，实现多 LLM Provider 路由、熔断、降级和租户配额管理。

## 技术设计

### 来源文件

- `dts-platform/service/ai/gateway/AiGatewayService.java`
- 相关的 Provider 状态管理、熔断逻辑

### 核心能力

1. **多 Provider 管理**: 注册多个 LLM Provider（Ollama、OpenAI、DeepSeek、Qwen 等）
2. **路由策略**: 按优先级顺序尝试，支持 "race mode"（并发竞争取最快）
3. **熔断器**: Provider 连续失败 N 次后短路，冷却期后重试
4. **降级**: 主 Provider 不可用时自动切换备用
5. **配额**: 基于 API Key 的调用配额管理（预留）

### 配置

```yaml
dts:
  copilot:
    gateway:
      fallback-strategy: sequential  # sequential | race
      circuit-breaker:
        failure-threshold: 3
        cooldown-seconds: 60
```

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/gateway/AiGatewayService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/gateway/ProviderState.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/gateway/CircuitBreaker.java`（新建）

## 完成标准

- [ ] 注册多个 Provider，按优先级路由
- [ ] 主 Provider 不可用时自动切换备用
- [ ] 连续失败触发熔断，冷却期后恢复
- [ ] 单元测试覆盖熔断和降级场景
