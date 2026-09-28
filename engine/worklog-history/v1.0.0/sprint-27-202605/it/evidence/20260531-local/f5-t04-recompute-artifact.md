# F5-T04 改芯片重算与画布刷新验证

**日期**: 2026-05-31
**范围**: `dts-copilot-webapp`
**阶段**: Mock Contract + Degraded Runtime

## 变更点

- `CopilotSendBody` / SSE body 支持 `assumptionOverrides`。
- `ConversationThread` 将口径芯片 `onCommit` 接到原问题重算,并复用 F3 `handleSendText` 流式通道。
- `useCopilotStream` 支持 `replaceAssistantMessageId`,重算时更新原 assistant 消息,使结果卡内 SQL/芯片回显原地刷新。
- `useCopilotStream` 支持可选 `artifactStore`,在 `done` 后用原 `artifactId` 调用 `upsert`,画布同 id 刷新,托盘不新增条目。
- `AgentWorkspacePage` 挂接 `ConversationThread + CanvasPanel + useArtifactStore`,对话与画布共享同一个产物 store。

## 验证命令

```bash
pnpm vitest run src/components/copilot/useCopilotStream.test.ts src/pages/AgentWorkspacePage.test.ts
node --test tests/aiChatCompatibility.test.ts
node --test tests/copilotSessionBootstrap.test.ts tests/copilotSessionFocus.test.ts tests/copilotSessionProxy.test.ts tests/aiChatCompatibility.test.ts
pnpm test
pnpm typecheck
pnpm build
```

## 结果

- targeted vitest:2 files / 7 tests passed。
- `tests/aiChatCompatibility.test.ts`:3 tests passed。
- Node session/compat suite:11 tests passed。
- `pnpm test`:50 files / 202 tests passed。
- `pnpm typecheck`:passed。
- `pnpm build`:passed;仍有既有 large chunk warning。

## 结论

F5-T04 前端重算闭环已完成:请求体带 `assumptionOverrides`,原 assistant 消息被替换更新,画布产物通过同一 `artifactId` `upsert` 刷新。F8-T02 backend contract 已补齐 AI 侧消费与 `sourceHint=user_override` 回显;IT05 仍需真实部署链路验证后才能标 Live DONE。
