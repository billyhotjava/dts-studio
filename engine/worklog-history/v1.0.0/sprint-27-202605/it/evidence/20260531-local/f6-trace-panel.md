# F6 溯源信任面板验证

## 范围

- TracePanel 从消息内 `trace-toggle` 折叠块迁移为画布动作行 `trace-sql` 触发的右侧抽屉。
- 前端消费 `trace.metricCaliber` / `trace.sources` / `trace.sql`，缺字段时回退 `sourceRefs` / `generatedSql` / tool messages。
- `FeedbackButtons` 新增 correction 模式，`submitCaliberCorrection` 本期仅落 `/api/ai/nl2sql/feedback`，不写回评测集或本体草稿。

## Mock / Degraded Contract

- `TracePanel.test.tsx`: `open=false` 不渲染；`open=true` 渲染 `SQL·溯源` dialog；展示「利润=收入-成本 · 报花域 · 口径 v3」、来源表/字段、默认收起 SQL、工具步骤展开、`sourceRefs + generatedSql` 降级；入口「⚑口径不对？纠正」展开 correction 表单。
- `copilotStreamEvent.test.ts` / `copilotStreamReducer.test.ts`: SSE `done.trace` 被归一化并复制到 assistant message。
- `aiChatCompatibility.test.ts`: legacy detail/response payload 保留 `trace`。
- `AgentWorkspacePage.test.ts`: 工作台包含 `onArtifactAction`、`trace-sql` 与 `<TracePanel` 接线。

## 命令结果

```bash
pnpm vitest run src/components/copilot/TracePanel.test.tsx src/components/copilot/FeedbackButtons.test.tsx src/api/modules/copilotStreamEvent.test.ts src/components/copilot/copilotStreamReducer.test.ts src/pages/AgentWorkspacePage.test.ts src/components/copilot/CopilotChat.structure.test.ts src/components/copilot/CopilotChat.presentation.test.ts src/components/copilot/MessageList.assumptions.test.tsx src/components/copilot/MessageList.clarifications.test.tsx
# Test Files 9 passed; Tests 32 passed

node --test tests/copilotSessionBootstrap.test.ts tests/copilotSessionFocus.test.ts tests/copilotSessionProxy.test.ts tests/aiChatCompatibility.test.ts
# tests 11; pass 11

pnpm typecheck
# passed

pnpm test
# Test Files 51 passed; Tests 207 passed

pnpm build
# built successfully; existing large chunk warning remains
```

## Live Contract

- 本文件的 Mock / Degraded 证据已被 `f8-live-contract.md` 的 IT09 live trace 证据补齐。
- `trace.metricCaliber` / `trace.sources[]` / `trace.sql` 已在 live SSE `done` 中同时存在,可供前端 SQL 溯源面板消费。
- 纠正写回仍沿用本期 `/api/ai/nl2sql/feedback` 降级路径,不作为 IT09 的完成前置条件。
