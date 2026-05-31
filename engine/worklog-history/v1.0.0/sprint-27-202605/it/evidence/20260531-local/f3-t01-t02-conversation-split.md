# F3-T01/T02 对话脊柱测试护栏与组件拆分证据

**时间**: 2026-05-31
**范围**: `dts-copilot-webapp/src/components/copilot`

## RED

- `pnpm vitest run src/components/copilot/CopilotChat.structure.test.ts`
  - 首次 RED: 5 failed / 5 tests,原因是 `ConversationThread.tsx` / `MessageList.tsx` / `Composer.tsx` 尚不存在,`CopilotChat.tsx` 仍为 1344 行主实现。
  - 审批 hook RED: 2 failed / 6 tests,原因是 `useCopilotApproval.ts` 尚不存在。

## GREEN

- `pnpm vitest run src/components/copilot/CopilotChat.structure.test.ts src/components/copilot/CopilotChat.presentation.test.ts src/components/copilot/copilotSendGuard.test.ts src/components/copilot/copilotStreamReducer.test.ts src/components/copilot/copilotStreamControl.test.ts`
  - 5 files passed,24 tests passed。
- `pnpm typecheck`
  - 通过。
- `pnpm test`
  - 34 files passed,146 tests passed。
- `pnpm build`
  - 通过;仅保留既有 Vite large chunk warning。

## 行数护栏

```text
10  src/components/copilot/CopilotChat.tsx
504 src/components/copilot/ConversationThread.tsx
273 src/components/copilot/MessageList.tsx
97  src/components/copilot/Composer.tsx
94  src/components/copilot/ConversationHeader.tsx
126 src/components/copilot/ApprovalPanel.tsx
452 src/components/copilot/useCopilotStream.ts
159 src/components/copilot/useCopilotApproval.ts
```

## Dev Server Smoke

- `curl http://localhost:3003/agent-bi` -> HTTP 200。
- Vite transform HTTP 200:
  - `/src/components/copilot/CopilotChat.tsx`
  - `/src/components/copilot/ConversationThread.tsx`
  - `/src/components/copilot/MessageList.tsx`
  - `/src/components/copilot/Composer.tsx`

后台未在本地启动,dev server 仍会对 `/api/session/properties` 输出 proxy `ECONNREFUSED`;这不是本次前端拆分错误。真实文字提问到 SSE 出结果仍保留在 IT03/T03 继续验收。
