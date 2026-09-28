# NV-03: NL2SQL Agent 系统提示词优化

**状态**: READY
**依赖**: NV-02

## 目标

优化 Agent 系统提示词，使其在用户提出数据问题时自动走 NL2SQL 工作流：schema 召回 → SQL 生成 → 返回可提取的 SQL。

## 技术设计

### 系统提示词增强

在 Agent 的 system prompt 中增加 NL2SQL 工作流指引：

```
当用户提出数据查询相关问题时，请按照以下步骤操作：
1. 调用 schema_lookup 工具获取相关表结构
2. 基于表结构信息，生成符合要求的 SQL 查询
3. SQL 必须为只读查询（仅 SELECT 或 WITH...SELECT）
4. 将生成的 SQL 用 ```sql 代码块包裹
5. 简要解释 SQL 的查询逻辑

回复格式示例：
根据您的问题，我查询了 xxx 表，以下是生成的 SQL：
```sql
SELECT ...
```
这条 SQL 的作用是...
```

### 提示词调优要点

- 引导 Agent 先调 `schema_lookup` 再生成 SQL（而非直接猜测表名）
- 强调 SQL 安全约束（只读、无副作用）
- 确保 SQL 包裹在 markdown code block 中（前端需要提取）
- 对于模糊问题，先澄清再生成

### 可选：few-shot 示例注入

利用 `Nl2SqlSemanticRecallService` 的 few-shot 召回能力，将匹配的示例注入到 Agent 上下文中。需要在 Agent 编排层调用召回服务。

## 影响文件

- Agent 系统提示词配置（可能在 `AgentChatService` 或 prompt template 文件中）
- 可选：Agent 编排层增加 `Nl2SqlSemanticRecallService` 调用

## 完成标准

- [ ] 在 CopilotChat 中提问"查询本月各项目费用"，Agent 调用 schema_lookup 并返回包含 SQL 代码块的回答
- [ ] 返回的 SQL 可被 markdown 代码块正则提取
- [ ] Agent 不在无表结构信息时凭空生成 SQL
