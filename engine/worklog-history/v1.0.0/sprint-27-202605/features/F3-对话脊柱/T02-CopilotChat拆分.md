# T02: CopilotChat 拆分

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

把 1379 行的 `CopilotChat.tsx` 按单一职责拆为脊柱容器 `ConversationThread`、消息流 `MessageList`、输入器 `Composer` 三块展示组件,外加会话/流式/审批状态 hooks,每个文件 < 800 行,行为严格不变(T01 护栏全绿)。拆分后 `CopilotChat` 退化为薄编排层,继续被 `CopilotSidebar` 与 F1 工作台脊柱插槽复用。

## 技术设计

### 目标文件结构(均在 `src/components/copilot/`)

- `ConversationThread.tsx` —— **脊柱容器**(对话脊柱)。持有顶层布局与各状态 hooks 的编排,渲染 `<MessageList>` + 审批区 + error + `<Composer>`。承接现有 `Props`(`hasSessionAccess` / `focusRequest` / `promptRequest` / `presentation` / `compactReasoning`)。
- `MessageList.tsx` —— **消息流**。接收 `chatMessages` / `sortedMessages` / `expandedTraces` / `compactReasoning` / `selectedDbId` / `databases` / 回调,渲染每条消息(reasoning 块、`CopilotMessageContent`、固定报表芯片、`InlineSqlPreview`、`TracePanel`、`FeedbackButtons`)。当前消息渲染逻辑约 240 行(L986~L1185),独立成文件。`WelcomeCard` 空态由容器或 MessageList 渲染。
- `Composer.tsx` —— **输入器**。`textarea` + `VoiceInputButton` + 发送/停止按钮 + 新对话按钮 + 会话条 + 数据源条。接收 `input` / `sendAction` / `canEditComposer` / `copilotEnabled` 与回调(详见 T04)。
- 状态 hooks(从组件体抽出,降低主文件行数,且便于测试):
  - `useCopilotSessionState.ts` —— sessionId / sessions / messages / pendingAction / databases / selectedDbId 的加载与持久化(`reloadSessions` / `reloadMessages` / bootstrap effect / focus effect / 数据源 effect)。详见 T05。
  - `useCopilotStream.ts` —— `handleSendText` / `handleSend` / `abortStreaming` / `handleStopStreaming` + 流式 ref(`streamInFlightRef` / `streamAbortRef` / `activeStreamingSessionIdRef` / `queuedInputRef`)+ watchdog。详见 T03。
  - `useCopilotApproval.ts` —— `pendingAction` / `approvalValues` / `approvalSchema` / `handleApprove` / `handleCancel`(审批表单逻辑约 L764~L856,90+ 行,独立)。
- `CopilotChat.tsx` —— 保留为兼容入口:`export function CopilotChat(props) { return <ConversationThread {...props} /> }`,或直接把 `CopilotSidebar` 与 F1 改为引用 `ConversationThread`(二选一,优先保留 `CopilotChat` 名以减少调用方改动)。

### 拆分纪律

- **纯搬运,不改逻辑**:每次只移动一段并把跨段共享的状态/回调以 props 形式显式传递;每步后跑 `pnpm test`(T01 护栏)+ `pnpm typecheck`。
- **状态归属清晰**:容器 `ConversationThread` 持有所有状态 hooks 的返回值,向 `MessageList` / `Composer` 单向传 props + 回调,展示组件保持纯函数(无自有副作用,符合 web/patterns 的 container/presentational 拆分)。
- **CSS 不动**:沿用 `CopilotChat.css` 与现有 `copilot-chat__*` 类名,避免引入视觉回归;`presentation`/`compactReasoning` 的 className 拼接逻辑随容器走。
- **护栏迁移**:`CopilotChat.presentation.test.ts` 中断言的字符串(`<CopilotMessageContent content={msg.content} />`、`copilot-chat__reasoning-details` 等)随渲染代码迁移到 `MessageList.tsx`,同步把 `readFileSync` 目标改为对应子文件(T01 已在注释中标注迁移目标)。

### F4 解耦预留(只留接口,不实现)

按 spec §8「画布与对话通过产物引用接口连接 —— 消息持有产物 id,画布按 id 渲染」:在 `MessageList` 的消息项上预留 `onSelectArtifact?(messageId)` / `activeArtifactMessageId?` 这类 props(可选、默认 no-op),供 F4 接入活产物画布;本任务不渲染画布、不实现选中态业务。

## 影响范围

- `src/components/copilot/CopilotChat.tsx` → 拆为 `ConversationThread.tsx` / `MessageList.tsx` / `Composer.tsx` + `useCopilotSessionState.ts` / `useCopilotStream.ts` / `useCopilotApproval.ts`。
- `src/components/copilot/CopilotChat.presentation.test.ts`(断言目标随渲染迁移)。
- `src/components/copilot/CopilotSidebar.tsx`(确认引用入口不变或改指 `ConversationThread`)。
- F1 工作台脊柱插槽(确认挂载 `ConversationThread` / `CopilotChat`)。
- `CopilotChat.css`(类名沿用,不改样式)。

## 验证

- [ ] T01 护栏(`copilotSendGuard.test.ts` / `copilotStreamReducer.test.ts` / `CopilotChat.presentation.test.ts` + 既有逻辑测试)全绿,行为无回归。
- [ ] `wc -l` 确认 `ConversationThread.tsx` / `MessageList.tsx` / `Composer.tsx` / 各 hook 文件均 < 800 行。
- [ ] `pnpm typecheck` 通过;`Props` 与各子组件 props 类型显式、无 `any`。
- [ ] `pnpm build` 通过;在 `CopilotSidebar` 与 F1 工作台壳中均能正常渲染对话脊柱。
- [ ] 手动回归:发送、流式、停止、审批、新建/切换/删除会话、语音、空态 `WelcomeCard` 全部如旧。

## 完成标准

- [ ] 1379 行单文件拆为 `ConversationThread`(脊柱)/ `MessageList`(消息流)/ `Composer`(输入器)+ 会话/流式/审批 hooks,职责单一。
- [ ] 每个新文件 < 800 行;`CopilotChat` 名保留为薄入口或调用方平滑改指 `ConversationThread`。
- [ ] 展示组件为纯函数,状态集中在容器 + hooks;F4 产物引用接口已预留(不实现)。
- [ ] `pnpm typecheck`、`pnpm test`、`pnpm build` 全绿,行为与拆分前一致。
