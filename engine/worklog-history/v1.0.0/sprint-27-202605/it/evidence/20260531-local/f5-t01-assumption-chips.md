# F5-T01 假设/口径芯片验证

**日期**: 2026-05-31
**范围**: `CopilotAssumption` 类型、`EditableAssumptionChips`、assistant 消息结果卡顶部集成。

## 实现结果

- `src/api/types.ts` 增加可选 `CopilotAssumption` / `CopilotAssumptionOption`,并给 `AiAgentChatMessage` 增加 `assumptions?: CopilotAssumption[]`。
- 新增 `src/components/copilot/assumptions/assumptionTypes.ts`,提供 `formatAssumptionChipLabel` / `hasAssumptionChanged`。
- 新增 `EditableAssumptionChips`,支持文本 input / options select / 只读芯片 / disabled 状态 / Enter 提交 / Escape 取消。
- `MessageList` 在 assistant 结果卡顶部渲染 `msg.assumptions`,并通过 `onAssumptionCommit(messageId, key, nextValue)` 上抛编辑结果;实际重算留给 F5-T04。

## 验证命令

```bash
pnpm vitest run src/components/copilot/assumptions/assumptionChipsModel.test.ts src/components/copilot/assumptions/EditableAssumptionChips.test.tsx src/components/copilot/MessageList.assumptions.test.tsx
```

结果: 3 files / 7 tests passed。

```bash
pnpm test
```

结果: 46 files / 192 tests passed。

```bash
pnpm typecheck
```

结果: passed。

```bash
pnpm build
```

结果: passed。仍有既有 large chunk warning,本次未引入新的构建失败。

## 说明

T01 只完成常驻口径芯片展示与编辑回调。`assumptions[]` 的 SSE/同步响应消费、置信度判定、低置信反问和改后重算分别由 F5-T02/T03/T04 继续实现。
