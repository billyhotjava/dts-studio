# AE-01: OpenAI 兼容客户端抽取

**状态**: READY
**依赖**: SC-01

## 目标

从 dts-platform 抽取 `OpenAiCompatibleClient`，作为 copilot-ai 调用任何 OpenAI 兼容 LLM 端点的基础 HTTP 客户端。

## 技术设计

### 来源文件

`/opt/prod/prs/source/dts-stack/source/dts-platform/src/main/java/com/yuzhi/dts/platform/service/ai/OpenAiCompatibleClient.java`

### 抽取改造

1. 包名: `com.yuzhi.dts.platform.service.ai` → `com.yuzhi.dts.copilot.ai.service.llm`
2. 保持完整功能：
   - Chat completion（同步）
   - Chat completion streaming（SSE）
   - Tool/function calling 支持
   - JSON 请求/响应解析
3. 去掉对 dts-platform 内部类的引用（如有）
4. 依赖：Jackson + Java HTTP Client

### 关键接口

```java
public class OpenAiCompatibleClient {
    CompletionResponse chatCompletion(CompletionRequest request);
    void chatCompletionStream(CompletionRequest request, OutputStream output);
}
```

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/OpenAiCompatibleClient.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/dto/CompletionRequest.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/dto/CompletionResponse.java`（新建）

## 完成标准

- [ ] 编译通过，无对 dts-platform 的依赖
- [ ] 单元测试：mock HTTP 调用，验证请求格式和响应解析
- [ ] 可调用本地 Ollama `/v1/chat/completions` 端点
- [ ] 流式响应正确解析 SSE data 行
