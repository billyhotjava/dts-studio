# F5-T02 乐观执行契约验证

**日期**: 2026-05-31
**范围**: `dts-copilot-webapp`
**阶段**: Mock Contract + Degraded Runtime

## 变更点

- `AiAgentChatMessage` / `AiAgentChatResponse` / `CopilotStreamEvent.done` 支持 `assumptions[]` 与 `confidence`。
- SSE `done` 解析新增 `normalizeCopilotDoneStreamEvent`,保留 `confidence=0` 这类低置信有效值,并过滤异常 `assumptions`。
- legacy 同步响应与历史会话归一化透传 `assumptions[]` / `confidence`。
- `shouldExecuteOptimistically` 固化默认阈值 `0.6`:缺失置信度向后兼容为乐观执行,低于阈值留给 F5-T03 降级反问。

## 验证命令

```bash
pnpm vitest run src/components/copilot/assumptions/assumptionConfidence.test.ts src/api/modules/copilotStreamEvent.test.ts src/components/copilot/copilotStreamReducer.test.ts src/components/copilot/useCopilotStream.test.ts
node --test tests/aiChatCompatibility.test.ts
node --test tests/copilotSessionBootstrap.test.ts tests/copilotSessionFocus.test.ts tests/copilotSessionProxy.test.ts tests/aiChatCompatibility.test.ts
pnpm test
pnpm typecheck
pnpm build
```

## 结果

- targeted vitest:4 files / 15 tests passed。
- `tests/aiChatCompatibility.test.ts`:3 tests passed。
- Node session/compat suite:11 tests passed。
- `pnpm test`:48 files / 197 tests passed。
- `pnpm typecheck`:passed。
- `pnpm build`:passed;仍有既有 large chunk warning。

## 结论

F5-T02 前端契约与容错消费已完成。F8 live contract 未完成前,真实后台链路不会强行标为完整通过;当前证据覆盖 mock contract 与后台缺字段时的降级兼容。
