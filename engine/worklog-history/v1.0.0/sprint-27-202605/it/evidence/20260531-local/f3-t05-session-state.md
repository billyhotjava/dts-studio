# F3-T05 会话状态与历史验证

**日期**: 2026-05-31
**范围**: `useCopilotSessionState` 会话状态抽取、历史会话排序、持久化恢复、focus 回跳、流式刷新保护。

## 实现结果

- 新增 `src/components/copilot/useCopilotSessionState.ts`,把 `sessionId` / `sessions` / `messages` / `pendingAction` / `databases` / `selectedDbId` / `focusNotice` / `focusedMessageId` 收口到单一 hook。
- `ConversationThread.tsx` 改为装配 hook,继续向 `ConversationHeader`、`MessageList`、`Composer`、`useCopilotStream`、`useCopilotApproval` 传递同一份状态。
- 历史会话通过 `analyticsApi.listAiAgentSessions(50)` 加载,按 `lastActiveAt` / `createdAt` 倒序暴露给脊柱。
- `tests/copilotSessionFocus.test.ts` 的静态护栏迁移到拆分后的 `ConversationThread` / `useCopilotSessionState` / `MessageList`。
- `tests/copilotSessionProxy.test.ts` 改为 Node 24 可直接执行的 `.ts` 源码导入。

## 行为覆盖

`src/components/copilot/useCopilotSessionState.test.tsx`

- 恢复持久化业务会话,消息按 `sequenceNum` 排序,REPORT_DRAFT 消息可挂接 `analysisDraftId`。
- greeting-only 的 `BUSINESS_DIRECT_RESPONSE` 旧会话不会自动恢复,并清理 `SESSION_ID_KEY`。
- 历史会话按最近活跃时间倒序输出。
- focus request 会切换会话、加载消息、暴露 `focusNotice` / `focusedMessageId`。
- 当前会话仍在 SSE 流式输出时,不会被 `reloadMessages` 抢答覆盖。

## 验证命令

```bash
pnpm vitest run src/components/copilot/useCopilotSessionState.test.tsx
```

结果: 1 file / 5 tests passed。

```bash
node --test tests/copilotSessionBootstrap.test.ts tests/copilotSessionFocus.test.ts tests/copilotSessionProxy.test.ts tests/aiChatCompatibility.test.ts
```

结果: 11 tests passed。

```bash
pnpm test
```

结果: 37 files / 163 tests passed。

```bash
pnpm typecheck
```

结果: passed。

```bash
pnpm build
```

结果: passed。仍有既有 large chunk warning,本次未引入新的构建失败。

## 结构约束

```text
CopilotChat.tsx              10 lines
ConversationThread.tsx      335 lines
MessageList.tsx             273 lines
Composer.tsx                 97 lines
ConversationHeader.tsx       94 lines
ApprovalPanel.tsx           126 lines
useCopilotStream.ts         452 lines
useCopilotApproval.ts       159 lines
useCopilotSessionState.ts   250 lines
```

所有拆分后的核心文件均 < 800 行。

## 未完成的真实链路

真实后台 Live Contract 尚未跑通,IT03/IT04 仍保留 TODO。T05 本次以 hook/component/static guard 覆盖会话状态行为,不把真实文字/语音端到端链路标为 DONE。
