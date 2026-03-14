# AE-04: AiCopilotService 核心抽取

**状态**: READY
**依赖**: AE-02, AE-03

## 目标

从 dts-platform 抽取 `AiCopilotService` 核心能力：SQL 补全、流式补全、SQL 解释、SQL 优化。

## 技术设计

### 来源文件

`dts-platform/service/ai/AiCopilotService.java`

### 保留功能

- `complete(AiCopilotRequest)` — SQL 补全（同步）
- `completeStream(AiCopilotRequest, OutputStream)` — 流式补全（SSE）
- `explain(String sql)` — SQL 解释
- `optimize(String sql)` — SQL 优化建议
- dbt 模式支持（可选）

### 裁剪内容

- 去掉 `buildGovernanceContext()` 中的治理专用上下文（质量规则、数据标准等）
- 去掉对 `SemanticModelService` 的强依赖，改为可选注入
- 去掉 `AiAuditService` 的增强审计（简化为日志审计）

### Prompt 模板

保留核心系统 prompt 模板，支持可配置化：
```
你是一个 SQL 专家助手。根据以下数据库结构信息，帮助用户完成 SQL 查询。
{context}
```

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/AiCopilotService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/dto/AiCopilotRequest.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/dto/AiCopilotResponse.java`（新建）

## 完成标准

- [ ] `complete()` 调用 LLM Gateway 返回补全结果
- [ ] `completeStream()` 返回正确的 SSE 流
- [ ] `explain()` 和 `optimize()` 返回人类可读的分析
- [ ] 无对 dts-platform 的依赖
- [ ] 单元测试覆盖核心路径
