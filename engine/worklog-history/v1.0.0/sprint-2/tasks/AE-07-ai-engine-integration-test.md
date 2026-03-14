# AE-07: AI 引擎集成测试

**状态**: READY
**依赖**: AE-01~06

## 目标

编写 copilot-ai AI 引擎的集成测试套件，验证端到端的 AI 调用链路。

## 技术设计

### 测试场景

1. **LLM 调用链路**: API → CopilotService → Gateway → OpenAiClient → Ollama
2. **多 Provider 降级**: 主 Provider 不可用 → 自动切换备用
3. **NL2SQL**: 自然语言 → SQL 生成 → 安全检查
4. **流式响应**: SSE 流式输出正确性
5. **配置管理**: Provider CRUD + 连通性测试

### 测试分层

- 单元测试: mock LLM 调用，验证业务逻辑
- 集成测试: 需要 Ollama 运行，验证真实调用
- API 测试: HTTP 端点验收

## 影响文件

- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/AiCopilotServiceTest.java`（新建）
- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/llm/gateway/AiGatewayServiceTest.java`（新建）
- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/web/rest/AiCopilotResourceIT.java`（新建）

## 完成标准

- [ ] 单元测试全部通过
- [ ] 集成测试在有 Ollama 环境下通过
- [ ] API 测试覆盖所有端点
- [ ] 测试覆盖率 > 70%（核心服务类）
