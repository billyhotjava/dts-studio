# F5-T03 低置信澄清验证

**日期**: 2026-05-31
**范围**: `dts-copilot-webapp`
**阶段**: Mock Contract + Degraded Runtime

## 变更点

- 新增 `CopilotClarification` / `CopilotClarificationOption` 契约,消息、同步响应与 SSE `done` 均支持 `clarifications[]`。
- 低置信且有 `clarifications[]` 时,`MessageList` 渲染 `ClarificationChips`,并隐藏 SQL 预览/产物动作。
- `ClarificationChips` 支持每个维度单选、未选满禁用「继续」、Enter/方向键选择。
- 用户提交澄清后,`ConversationThread` 以原问题重新发送,请求体带 `clarificationAnswers`。

## 验证命令

```bash
pnpm vitest run src/components/copilot/assumptions/ClarificationChips.test.tsx src/components/copilot/MessageList.clarifications.test.tsx src/api/modules/copilotStreamEvent.test.ts src/components/copilot/copilotStreamReducer.test.ts src/components/copilot/useCopilotStream.test.ts
node --test tests/aiChatCompatibility.test.ts
node --test tests/copilotSessionBootstrap.test.ts tests/copilotSessionFocus.test.ts tests/copilotSessionProxy.test.ts tests/aiChatCompatibility.test.ts
pnpm test
pnpm typecheck
pnpm build
```

## 结果

- targeted vitest:5 files / 16 tests passed。
- `tests/aiChatCompatibility.test.ts`:3 tests passed。
- Node session/compat suite:11 tests passed。
- `pnpm test`:50 files / 201 tests passed。
- `pnpm typecheck`:passed。
- `pnpm build`:passed;仍有既有 large chunk warning。

## 结论

F5-T03 前端低置信澄清路径已按 mock contract 完成。F8 backend contract 已补齐 `clarificationAnswers` 的 AI 侧消费与同项反问抑制;IT06 仍需真实部署链路验证后才能标 Live DONE。
