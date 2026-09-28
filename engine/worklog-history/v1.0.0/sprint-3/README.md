# Sprint-3: AI 高级能力抽取 (AA)

**前缀**: AA (AI Advanced)
**状态**: READY
**目标**: 从 dts-platform 抽取 AI 高级能力模块（RAG 向量检索、ReAct Agent 引擎、Tool 系统、安全防护、Agent Chat 会话管理），构建 copilot-ai 的完整 AI 能力栈。

## 背景

dts-platform 的 AI 高级能力包括：
- RAG: pgvector 向量存储 + BGE-M3 Embedding + 混合检索（向量 + BM25 + RRF）
- Agent: ReAct 推理引擎 + Tool 注册与执行管线
- Safety: SQL 沙箱 + 权限过滤 + 审计 + GuardrailsInterceptor
- Chat: 会话管理 + 消息持久化 + 流式输出

本 sprint 需要裁剪数据治理专用的 Tool（如 DataLineageTool、DwModelingTool、DataQualityTool 等），保留通用 Tool（SQL 执行、查询验证等），并预留园林业务 Tool 的扩展点。

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| AA-01 | pgvector schema + Embedding 服务迁移 | READY | AE-01 |
| AA-02 | RAG 向量存储与混合检索抽取 | READY | AA-01 |
| AA-03 | ReAct Agent 引擎抽取 | READY | AE-04 |
| AA-04 | Tool 注册与执行管线抽取（裁剪治理专用 Tool） | READY | AA-03 |
| AA-05 | 安全防护抽取（SQL 沙箱 + 权限过滤 + 审计） | READY | AA-03, AA-04 |
| AA-06 | Agent Chat 会话管理（持久化 + 流式） | READY | AA-03 |
| AA-07 | AI 高级能力集成测试 | READY | AA-01~06 |

## 完成标准

- [ ] pgvector 扩展启用，RAG embedding 表创建
- [ ] 向量检索 + BM25 关键词 + RRF 融合正常工作
- [ ] ReAct Agent 可执行多轮推理循环
- [ ] Tool 注册表可动态注册/注销 Tool
- [ ] SQL 沙箱阻止危险操作（DROP/DELETE 等）
- [ ] Agent Chat 会话持久化到数据库
- [ ] 流式输出支持 Tool 调用中间结果

## IT 验证命令

```bash
cd dts-copilot/dts-copilot-ai && mvn test -Dtest="*Rag*,*Agent*,*Tool*,*Safety*"

# RAG 检索测试
curl -X POST http://localhost:8091/api/ai/agent/chat/send \
  -H "Content-Type: application/json" \
  -d '{"message": "查询所有项目的花卉数量"}'
```

## 源代码映射

| copilot-ai 目标包 | dts-platform 来源包 |
|-------------------|-------------------|
| `service.ai.rag.*` | `service.ai.rag.*` |
| `service.ai.engine.ReActEngine` | `service.ai.engine.ReActEngine` |
| `service.ai.tool.*` | `service.ai.tool.*`（裁剪治理 Tool） |
| `service.ai.safety.*` | `service.ai.safety.*` |
| `service.ai.chat.*` | `domain.ai.AiChatSession/Message` + 相关 Service |

## 优先级说明

AA-01 → AA-02（RAG 线）可与 AA-03 → AA-04（Agent 线）并行
AA-05 依赖 AA-03+04 → AA-06 → AA-07 收尾
