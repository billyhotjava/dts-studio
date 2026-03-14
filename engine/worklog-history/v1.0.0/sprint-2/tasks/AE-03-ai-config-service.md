# AE-03: AI 配置管理服务（Provider 模板 + 持久化）

**状态**: READY
**依赖**: AE-01

## 目标

将原来分散在 dts-admin（AiProviderTemplates）和 dts-ingestion（infra_service_settings）中的 AI 配置，合并为 copilot-ai 内置的统一配置管理服务。

## 技术设计

### 来源文件

- `dts-admin/service/infra/AiProviderTemplates.java` — Provider 模板定义
- `dts-platform/service/ai/AiCopilotConfigService.java` — 配置加载逻辑

### 内置 Provider 模板

```java
public enum ProviderTemplate {
    OLLAMA("Ollama", "http://localhost:11434/v1", null, "qwen2.5-coder:7b", true),
    OPENAI("OpenAI", "https://api.openai.com/v1", "sk-...", "gpt-4o", false),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "sk-...", "deepseek-chat", false),
    QWEN("Qwen", "https://dashscope.aliyuncs.com/compatible-mode/v1", "sk-...", "qwen-plus", false),
    ZHIPU("Zhipu", "https://open.bigmodel.cn/api/paas/v4", "...", "glm-4", false);
}
```

### 持久化

配置存储在 `copilot_ai.ai_provider_config` 表（SC-02 已建），通过 REST API 管理。

### REST API

```
GET    /api/ai/config/providers       — 列表
POST   /api/ai/config/providers       — 新增
PUT    /api/ai/config/providers/{id}   — 修改
DELETE /api/ai/config/providers/{id}   — 删除
GET    /api/ai/config/providers/templates — 获取内置模板
POST   /api/ai/config/providers/{id}/test — 测试连通性
```

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/config/AiConfigService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/config/ProviderTemplate.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/AiProviderConfig.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/repository/AiProviderConfigRepository.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiConfigResource.java`（新建）

## 完成标准

- [ ] 内置 5 种 Provider 模板可查询
- [ ] CRUD API 正常工作
- [ ] 连通性测试调用 Provider `/v1/models` 端点
- [ ] 配置持久化到 PostgreSQL
- [ ] 默认配置指向 Ollama
