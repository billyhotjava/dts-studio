# T03: SSE 流式接入脊柱

**优先级**: P0
**状态**: READY
**依赖**: T02

## 目标

把现有 SSE 流式链路(`aiAgentChatSendStream` → `copilotSse` 解析 → `copilotStreamControl` 看门狗/动作 → `copilotReasoningState` reasoning 归并)完整接入拆分后的对话脊柱,确保流式 reasoning + token + 工具进度 + done 结果在 `ConversationThread` / `MessageList` 内的渲染、超时、停止、同步回退行为与拆分前**逐字节一致**。

## 技术设计

### 流式逻辑收口到 `useCopilotStream`

把现有 `handleSendText`(`CopilotChat.tsx` L405~L745)整体迁入 `useCopilotStream.ts`,保留全部分支语义:

- **乐观插入**:先 push 用户 `optimistic` 消息,再 push `assistant` 占位(`reasoningContent: STREAM_PENDING_REASONING`)。
- **事件归并**:复用 T01 抽出的 `reduceCopilotStreamEvent` 处理 `aiAgentChatSendStream` 回调:
  - `session` → `setSessionId` + 写 `SESSION_ID_KEY` + 更新 `activeStreamingSessionIdRef`;
  - `reasoning` → `appendReasoningDelta`(`copilotReasoningState`),首个 delta 清掉 `STREAM_PENDING_REASONING`;
  - `token` → 累加 `streamedContent` 写入 `content`;
  - `tool` → `appendToolProgressLine`(`copilotReasoningState`);
  - `done` → 写入 `generatedSql` / `templateCode` / `routedDomain` / `targetView` / `responseKind` / `suggestedDisplay` / `dataSurface` / `qualityLevel` / `qualityNotes` / `reportCode` / `sourceRefs`,并触发 `createGeneratedReportDraft`(报表草稿沉淀,见下);
  - `error` → 写 error 文案。
- **看门狗**:复用 `createCopilotStreamWatchdog`(`copilotStreamControl`,`idleMs: STREAM_IDLE_TIMEOUT_MS = 30000`),`onIdle` 置超时文案、abort、把空占位消息改为「响应超时被中断」。
- **停止 / 打断重发**:`abortStreaming` + `handleStopStreaming` + `queuedInput` 队列(打断当前流后用 `queuedInputRef` 重发);区分「用户主动停止」(写"已停止本次回答生成。")与「为替换打断」(`interruptedForReplacement` 不写中断文案)。
- **同步回退**:SSE 抛非 abort 错误且 `!sawStreamEvent` 时,删除 `stream-*` 占位消息,回退 `analyticsApi.aiAgentChatSend`(`CopilotSendBody`),处理 `requiresApproval` / `pendingAction`;`sawStreamEvent` 为真时只置 error 不回退(避免重复请求)。
- **reverse-proxy 容错**:`copilotSse` 已在 `receivedEvents > 0` 时吞掉收尾 TypeError,保持不变。

### refs 与并发归属

`streamInFlightRef` / `streamAbortRef` / `activeStreamingSessionIdRef` / `queuedInputRef` 全部迁入 `useCopilotStream`,通过返回值暴露 `handleSend` / `handleSendText` / `handleStopStreaming` / `sending` 给容器。`activeStreamingSessionIdRef` 用于「流式进行中不被 `reloadMessages` 抢答」的判断(现 L302~L307),该判断在 T05 的 `useCopilotSessionState` 中需读到同一 ref —— 通过容器把 ref 在两 hook 间传递,或合并到一个共享的 stream-context ref 对象。

### 报表草稿沉淀(`createGeneratedReportDraft`)

保留现有逻辑:`done` 且 `responseKind === "REPORT_DRAFT"` 且有 `generatedSql` 且能解析出 `draftDatabaseId`(`resolveCopilotSqlDatabaseId`)时,调 `analyticsApi.createAnalysisDraft`(`buildCopilotAnalysisDraftPayload`),按 `saving` / `saved` / `error` 更新消息上的 `analysisDraftStatus`。本任务只迁移不改语义。

### `analyticsApi.listAnalysisDrafts` 关联

`reloadMessages` 里 best-effort 的 `attachAnalysisDraftLinksToMessages` 保持不变(归 T05 的会话恢复路径)。

## 影响范围

- 复用(不改契约,仅确认接入):`src/api/copilotSse.ts`(`createSseEventParser`)、`src/api/modules/copilot.ts`(`aiAgentChatSendStream`,经 `analyticsApi` re-export)。
- `src/components/copilot/copilotStreamControl.ts`(`resolveCopilotSendAction` / `createCopilotStreamWatchdog`,复用)。
- `src/components/copilot/copilotReasoningState.ts`(`appendReasoningDelta` / `appendToolProgressLine`,复用)。
- 新增 `src/components/copilot/useCopilotStream.ts`(承接 `handleSendText` 等流式逻辑)。
- `src/components/copilot/copilotStreamReducer.ts`(T01 抽出,本任务接入真实回调)。
- `ConversationThread.tsx` / `MessageList.tsx`(消费流式消息状态与 `STREAM_PENDING_REASONING` 占位渲染)。

## 验证

- [ ] `copilotStreamReducer.test.ts`(T01)对 `session/reasoning/token/tool/done/error` 序列断言全绿。
- [ ] 既有 `tests/copilotSse.test.ts`、`copilotStreamControl.test.ts`、`copilotReasoningState.test.ts` 不需改动且通过。
- [ ] 手动回归:流式时先出「正在思考…」占位 → reasoning 滚动 → token 逐字 → done 出 SQL/结果与动作行;30s 静默触发超时文案;点停止写「已停止」;输入新问题打断当前流并重发;断网/SSE 异常时回退同步接口仍出结果。
- [ ] `pnpm typecheck`、`pnpm test`、`pnpm build` 全绿。

## 完成标准

- [ ] 流式 reasoning + token + tool 进度 + done 结果在新脊柱内渲染正常,字段(`generatedSql` / `responseKind` / `sourceRefs` 等)全部落位。
- [ ] 超时看门狗、停止、打断重发、同步回退、reverse-proxy 收尾容错行为与拆分前一致。
- [ ] `REPORT_DRAFT` 草稿沉淀(`createGeneratedReportDraft`)的 saving/saved/error 状态机不变。
- [ ] 流式并发 ref 归属清晰,`reloadMessages` 不会在流式进行中抢答。
