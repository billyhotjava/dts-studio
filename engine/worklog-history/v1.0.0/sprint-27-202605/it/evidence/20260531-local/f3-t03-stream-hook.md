# F3-T03 SSE 流式接入脊柱证据

**时间**: 2026-05-31
**范围**: `useCopilotStream` / `copilotStreamControl` / `copilotStreamReducer`

## 覆盖内容

- `useCopilotStream.test.ts`
  - `session` / `reasoning` / `tool` / `token` / `done` 事件归并到脊柱消息。
  - SSE 首包前失败时回退 `analyticsApi.aiAgentChatSend`,并恢复会话消息。
  - 用户停止 active stream 时 abort,assistant 占位消息写入“已停止本次回答生成。”。
  - `REPORT_DRAFT` done 事件触发 `createAnalysisDraft`,assistant 消息进入 `saved` 状态并带回 draft id。
- `copilotStreamControl.test.ts`
  - send / stop / interrupt-and-send 三态动作。
  - `createCopilotStreamWatchdog` idle 触发、活动续期、stop 取消。
- `copilotStreamReducer.test.ts`
  - reasoning/token/tool/done/error 字段归并。

## 验证命令

- `pnpm vitest run src/components/copilot/useCopilotStream.test.ts src/components/copilot/copilotStreamControl.test.ts src/components/copilot/copilotStreamReducer.test.ts`
  - 3 files passed,16 tests passed。
- `pnpm test`
  - 35 files passed,152 tests passed。
- `pnpm typecheck`
  - 通过。

## 未覆盖

本地后台未启动,真实“文字提问 → SSE → NL2SQL 结果”仍保留在 IT03 做 Live Contract 验收。
