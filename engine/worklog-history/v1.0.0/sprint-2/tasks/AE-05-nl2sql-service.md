# AE-05: NL2SQL 服务抽取（语义增强）

**状态**: READY
**依赖**: AE-04

## 目标

抽取自然语言转 SQL（NL2SQL）能力，保留语义增强机制，支持通过数据源 schema 元数据构建 NL2SQL 上下文。

## 技术设计

### 来源

`dts-platform/service/ai/AiCopilotService.java` 中的 `nl2sql()` 和 `nl2sqlCompare()` 方法

### NL2SQL 流程

```
用户自然语言 → 构建上下文（DDL schema / 语义术语）→ LLM 推理 → 清洗 SQL → 返回
```

### 上下文构建

1. **DDL 模式**: 从注册数据源获取表结构作为上下文
2. **语义增强**（可选）: 如果 RAG 可用（sprint-3），自动检索相关语义术语补充上下文
3. 上下文限制: 最多 12 项，每项 240 字符

### 安全

- SQL 结果清洗：移除 markdown 代码围栏
- 危险操作拦截：INSERT/UPDATE/DELETE/DROP/TRUNCATE/ALTER/CREATE 默认阻止

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/Nl2SqlService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/SqlSafetyChecker.java`（新建）

## 完成标准

- [ ] 中文自然语言成功转为 SQL 查询
- [ ] DDL 上下文正确注入到 prompt
- [ ] 危险 SQL 被拦截
- [ ] 结果正确清洗（无 markdown 围栏）
- [ ] 语义增强为可选，RAG 不可用时降级为纯 DDL 模式
