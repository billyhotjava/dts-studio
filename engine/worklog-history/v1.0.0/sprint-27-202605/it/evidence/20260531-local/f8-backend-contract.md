# F8 后台契约与降级联调验证

## 范围

- 后端 `AiChatMessage` 增加 `assumptions` / `confidence` / `clarifications` / `trace` 契约字段,并用 Liquibase `v1_0_0_023__chat_message_copilot_contract.xml` 落库。
- `CopilotChatContract` 从 `ConversationPlan` 和生成 SQL 构造可消费的口径假设、置信度和 trace,同步响应、SSE `done`、会话恢复共用同一输出。
- analytics session proxy 接收并转发 `assumptionOverrides` / `clarificationAnswers`,AI 侧执行服务写入 prompt、回显用户确认口径并抑制已回答澄清项,保持同步和 SSE 请求体契约一致。

## Backend Contract

- `AgentChatServiceTest`: assistant message 持久化 `assumptions` / `confidence` / `trace`,并把 `assumptionOverrides` / `clarificationAnswers` 传给执行层。
- `AgentExecutionServiceTest`: SSE `done` 输出 `assumptions` / `confidence` / `trace` 以及既有 `sourceRefs` / `qualityNotes`;带用户确认输入时 prompt 写入口径覆盖和澄清答案,`done.assumptions` 回显 `sourceHint=user_override`,且不再返回同项 `clarifications`。
- `CopilotChatRequestContextTest`: 过滤空/null 入参,避免异常请求破坏 F8 降级链路。
- `InternalAgentChatResourceTest`: 会话恢复 message payload 输出结构化 contract 字段,不是 JSON 字符串。
- `CopilotChatStreamingSecurityTest`: `/api/copilot/chat/send` 将重算/澄清入参转发给 AI 内部服务;streaming 安全上下文保持。

## 命令结果

```bash
mvn -pl dts-copilot-ai,dts-copilot-analytics -Dtest=AgentExecutionServiceTest,AgentChatServiceTest,InternalAgentChatResourceTest,CopilotChatRequestContextTest,CopilotChatResourceTest,CopilotChatStreamingSecurityTest test
# dts-copilot-ai: Tests run 19; Failures 0; Errors 0
# dts-copilot-analytics: Tests run 3; Failures 0; Errors 0
# BUILD SUCCESS

mvn -pl dts-copilot-ai,dts-copilot-analytics test
# dts-copilot-ai: Tests run 171; Failures 0; Errors 0
# dts-copilot-analytics: Tests run 99; Failures 0; Errors 0
# BUILD SUCCESS

node --test tests/copilotSessionBootstrap.test.ts tests/copilotSessionFocus.test.ts tests/copilotSessionProxy.test.ts tests/aiChatCompatibility.test.ts
# tests 11; pass 11

pnpm typecheck
# passed

pnpm test
# Test Files 59 passed; Tests 230 passed

pnpm build
# built successfully; existing large chunk warning remains
```

## Follow-up 修正

- 完整 Maven 回归首次暴露 `AiAuditLogJsonbMappingTest` 的测试专用 `ai_chat_message` DDL 未同步 F8 新列;已补齐 `assumptions` / `confidence` / `clarifications` / `trace` 并让用例实际写入 JSONB contract 字段。
- 收尾检查发现内部同步发送在跨用户 session 场景下会按新会话执行但响应仍可能回填旧 `sessionId`;已改为按有效会话回填,并补 `InternalAgentChatResourceTest` 覆盖 contract 字段回填。
- 再次审计发现 analytics 已转发用户确认输入,但 AI 执行层未消费;已新增 `CopilotChatRequestContext`,让 `assumptionOverrides` / `clarificationAnswers` 进入 prompt、SSE `done` 与会话持久化 contract。
- 降级健壮性补丁:过滤空/null 口径覆盖、澄清答案和 mart health 项,避免异常入参触发 `Map.copyOf` NPE。

## Live Contract

- 当前为后端 contract 单元/MockMVC 验证,未在完整部署环境跑真实 `/api/copilot/chat/send-stream` 到前端工作台的端到端链路。
- IT05/IT06/IT09 仍需真实环境验证后再标 Live DONE;当前可标 Backend Contract / Mock Contract 已通过。
