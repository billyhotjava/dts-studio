# AA-07: AI 高级能力集成测试

**状态**: READY
**依赖**: AA-01~06

## 目标

编写 AI 高级能力的集成测试，验证 RAG、Agent、Tool、Safety 的端到端链路。

## 测试场景

1. **RAG 链路**: 索引文档 → 向量化 → 混合检索 → 返回相关结果
2. **Agent 对话**: 发送消息 → ReAct 推理 → Tool 调用 → 返回答案
3. **Tool 执行**: 注册自定义 Tool → Agent 调用 → 验证结果
4. **安全防护**: 尝试执行危险 SQL → 被沙箱拦截
5. **会话管理**: 创建会话 → 多轮对话 → 查询历史

## 影响文件

- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/rag/RagServiceTest.java`（新建）
- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/agent/ReActEngineTest.java`（新建）
- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/safety/SqlSandboxTest.java`（新建）
- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/web/rest/AgentChatResourceIT.java`（新建）

## 完成标准

- [ ] 所有单元测试通过
- [ ] 集成测试在有 Ollama + PostgreSQL 环境下通过
- [ ] SQL 沙箱测试覆盖所有危险操作类型
- [ ] Agent 多轮对话测试验证上下文保持
