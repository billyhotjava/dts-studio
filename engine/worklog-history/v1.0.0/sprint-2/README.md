# Sprint-2: AI 引擎核心抽取 (AE)

**前缀**: AE (AI Engine)
**状态**: READY
**目标**: 从 dts-platform 抽取 AI 引擎核心模块（LLM Gateway、OpenAI 客户端、配置管理、Copilot 服务、NL2SQL），构建 copilot-ai 服务的基础 AI 能力。

## 背景

dts-platform 的 AI 核心能力集中在以下包路径：
- `com.yuzhi.dts.platform.service.ai.gateway.*` — LLM Gateway（多 Provider 切换、熔断、降级）
- `com.yuzhi.dts.platform.service.ai.OpenAiCompatibleClient` — OpenAI 兼容 HTTP 客户端
- `com.yuzhi.dts.platform.service.ai.AiCopilotService` — Copilot 核心服务（complete/stream/nl2sql/explain/optimize）
- `com.yuzhi.dts.platform.service.ai.AiCopilotConfigService` — AI 配置管理
- `com.yuzhi.dts.platform.config.AiCopilotProperties` — 配置属性

抽取时需要：
1. 包名从 `com.yuzhi.dts.platform` 重构为 `com.yuzhi.dts.copilot.ai`
2. 去掉对 dts-ingestion 的 AiConfigClient 依赖，改为本地配置管理
3. 去掉对 dts-admin 的 AiProviderTemplates 依赖，将模板内置

## 架构概览

```
REST API 层
│
├─ AiCopilotResource (/api/ai/copilot/*)
│   ├─ POST /complete    — SQL 补全
│   ├─ POST /stream      — 流式补全
│   ├─ POST /nl2sql      — 自然语言转 SQL
│   ├─ POST /explain     — SQL 解释
│   ├─ POST /optimize    — SQL 优化
│   └─ GET  /status      — AI 可用性状态
│
├─ AiCopilotService
│   ├─ OpenAiCompatibleClient → LLM Provider
│   └─ AiGatewayService → 多 Provider 路由 + 熔断
│
└─ AiCopilotConfigService
    └─ ai_provider_config 表（持久化）
```

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| AE-01 | OpenAI 兼容客户端抽取 | READY | SC-01 |
| AE-02 | LLM Gateway 服务（多 Provider + 熔断降级） | READY | AE-01 |
| AE-03 | AI 配置管理服务（Provider 模板 + 持久化） | READY | AE-01 |
| AE-04 | AiCopilotService 核心抽取 | READY | AE-02, AE-03 |
| AE-05 | NL2SQL 服务抽取（语义增强） | READY | AE-04 |
| AE-06 | AI REST API 端点 | READY | AE-04, AE-05 |
| AE-07 | AI 引擎集成测试 | READY | AE-01~06 |

## 完成标准

- [ ] `POST /api/ai/copilot/complete` 调用 Ollama 返回 SQL 补全结果
- [ ] `POST /api/ai/copilot/stream` 返回 SSE 流式响应
- [ ] `POST /api/ai/copilot/nl2sql` 将中文自然语言转为 SQL
- [ ] LLM Gateway 支持至少 2 个 Provider（Ollama + OpenAI）
- [ ] Provider 不可用时自动降级到备用 Provider
- [ ] AI 配置通过 API 和数据库持久化管理

## IT 验证命令

```bash
# 编译
cd dts-copilot/dts-copilot-ai && mvn compile

# 单元测试
cd dts-copilot/dts-copilot-ai && mvn test -Dtest="*Copilot*,*Gateway*,*OpenAi*"

# API 冒烟测试（需要 Ollama 运行）
curl -X POST http://localhost:8091/api/ai/copilot/complete \
  -H "Content-Type: application/json" \
  -d '{"prompt": "SELECT * FROM", "context": "CREATE TABLE users (id INT, name VARCHAR)"}'

curl http://localhost:8091/api/ai/copilot/status
```

## 源代码映射

| copilot-ai 目标 | dts-platform 来源 | 改造说明 |
|----------------|-------------------|---------|
| `service/ai/OpenAiCompatibleClient.java` | `service/ai/OpenAiCompatibleClient.java` | 包名重构 |
| `service/ai/gateway/AiGatewayService.java` | `service/ai/gateway/AiGatewayService.java` | 包名重构 |
| `service/ai/AiCopilotService.java` | `service/ai/AiCopilotService.java` | 去掉治理专用上下文 |
| `service/ai/AiCopilotConfigService.java` | `service/ai/AiCopilotConfigService.java` | 改为本地配置管理 |
| `config/AiCopilotProperties.java` | `config/AiCopilotProperties.java` | 包名重构 |
| `web/rest/AiCopilotResource.java` | `web/rest/AiCopilotResource.java` | 包名重构，去掉治理端点 |

## 优先级说明

AE-01 最先 → AE-02/AE-03 可并行 → AE-04 汇聚 → AE-05 → AE-06 → AE-07 收尾
