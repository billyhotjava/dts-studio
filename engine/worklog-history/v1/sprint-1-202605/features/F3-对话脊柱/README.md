# F3: 对话脊柱(CopilotChat 拆分扶正)

**优先级**: P0
**状态**: READY

## 目标

把现有 1379 行的浮动聊天 `CopilotChat.tsx` 扶正为 agent 工作台的「对话脊柱」主界面：先建测试护栏锁住现有关键行为，再按单一职责拆分为 `ConversationThread`(脊柱容器) / `MessageList`(消息流) / `Composer`(输入器) + 会话状态 hooks,每个文件 < 800 行;流式输出(SSE reasoning + token + done)、语音输入、发送/停止、会话历史(新建/切换/恢复)行为全部保持不变。拆分完成后,F4 活产物画布可通过「产物引用」接口(消息持有产物 id)与脊柱解耦对接。

> 设计依据:`docs/superpowers/specs/2026-05-30-agent-first-ui-design.md`(§4「状态二 · 对话脊柱 + 活产物画布」、§6「对话完整回路 + 溯源信任」、§8「技术落点 — 复用 / 模块边界原则」、§10 风险「`CopilotChat` 1379 行重构风险高,需先建测试护栏再拆」)。
>
> 范围内只做「拆分扶正 + 行为不变」,不新增画布/信号/本体等业务(由 F4 及后续 Feature 承载);仅按 §8「画布与对话解耦,通过产物引用接口连接」预留接口,本 Feature 不实现画布渲染。

## 现状与衔接点(已核对源码)

- 主文件 `src/components/copilot/CopilotChat.tsx`(1379 行):`interface Props { hasSessionAccess, focusRequest, promptRequest, presentation, compactReasoning }`;核心方法 `handleSendText` / `handleSend`(发送)、`handleStopStreaming` / `abortStreaming`(停止)、`handleApprove` / `handleCancel`(待审批)、`handleNewChat` / `handleDeleteSession`(会话);流式走 `aiAgentChatSendStream`,同步回退走 `analyticsApi.aiAgentChatSend`(`type CopilotSendBody = Parameters<typeof analyticsApi.aiAgentChatSend>[0]`)。
- 渲染分三段:会话条(`select` + 新建 + 删除)、数据源条、消息区(`WelcomeCard` / 消息列表 / `pendingAction` 审批 / error)、输入区(`textarea` + `VoiceInputButton` + 发送/停止按钮,约第 1356 行)。
- 已抽出的纯逻辑模块:`CopilotChat.helpers.ts`(常量 + `sortMessages` / `normalizeMicroForm` / `buildInitialApprovalValues` / `getToolMessagesForAssistant` / `getUserQuestionForAssistant` 等)、`copilotStreamControl.ts`(`resolveCopilotSendAction` + `createCopilotStreamWatchdog`)、`copilotReasoningState.ts`(`appendReasoningDelta` / `appendToolProgressLine`)、`copilotComposerState.ts`(`canEditCopilotComposer`)、`copilotInputBehavior.ts`(`shouldSubmitCopilotInputOnEnter`)、`copilotSessionBootstrap.ts`(`shouldRestorePersistedCopilotSession`)、`copilotSessionFocus.ts`(`buildCopilotSessionFocusRequest` 等)。
- 流式解析:`src/api/copilotSse.ts`(`createSseEventParser`)被 `src/api/modules/copilot.ts` 的 `aiAgentChatSendStream` 使用;`analyticsApi` 通过 `...copilotApi` 聚合并 `export { aiAgentChatSendStream }`。会话历史接口 `copilotApi.listAiAgentSessions(limit)` 返回 `AiAgentChatSession[]`(含 id/title/lastActiveAt),`getAiAgentSession(id)` / `deleteAiAgentSession(id)`。
- 现有测试护栏:`CopilotChat.presentation.test.ts` 用 `readFileSync` 断言源码/CSS 含特定字符串(`compactReasoning`、`copilot-chat__reasoning-details`、`<CopilotMessageContent content={msg.content} />`),拆分时必须保留这些标记或同步迁移断言。
- 衔接点更正:`dts:ai-quick-ask` 事件实际由 `AiAssistantFab.tsx` 监听并经 `COPILOT_PROMPT_REQUEST_EVENT` 转发,`CopilotChat` 通过 `promptRequest` prop(`resolveCopilotPromptHandoff`)消费后落到 `handleSendText`;拆分须保持这条链路不变。`CopilotSidebar.tsx` 是 `CopilotChat` 的现有宿主,拆分后仍需可被其复用。

## 复用与依赖

- 复用:`aiAgentChatSendStream` / `copilotSse` / `copilotStreamControl` / `copilotReasoningState`(流式)、`copilotComposerState` / `copilotInputBehavior` / `VoiceInputButton`(输入器)、`copilotSessionBootstrap` / `copilotSessionFocus` / `copilotApi.listAiAgentSessions`(会话)、`CopilotMessageContent` / `InlineSqlPreview` / `TracePanel` / `FeedbackButtons`(消息渲染)。
- 依赖:**F1**(工作台双栏壳与 F3 插槽就绪)。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 拆分前先建测试护栏 | P0 | READY | F1 |
| T02 | CopilotChat 拆分 | P0 | READY | T01 |
| T03 | SSE 流式接入脊柱 | P0 | READY | T02 |
| T04 | 输入器 + 语音整合 | P0 | READY | T02 |
| T05 | 会话状态与历史 | P0 | READY | T02 |

## 完成标准

- [ ] 拆分前测试护栏已建立并全绿:消息渲染、`handleSendText` 发送、SSE 流式状态(reasoning/token/done)、停止四类关键行为有可执行的 vitest 断言,作为重构基线(T01)。
- [ ] `CopilotChat.tsx` 拆为 `ConversationThread` / `MessageList` / `Composer` + 会话状态 hooks,每个文件 < 800 行,且对外仍可被 `CopilotSidebar` 与 F1 工作台脊柱插槽复用(T02)。
- [ ] 流式 reasoning + token + done + 工具进度在新脊柱内正常渲染,超时看门狗、停止、同步回退路径行为不变(T03)。
- [ ] 输入器整合文字输入(回车提交/Shift+回车换行/输入法 229 防误提交)、语音转写回填、发送/停止/打断重发,行为与拆分前一致(T04)。
- [ ] 会话 bootstrap(恢复持久化会话)+ focus(回跳来源对话)+「💬历史会话」列表(`listAiAgentSessions`)接入,支持新建 / 切换 / 删除 / 恢复(T05)。
- [ ] `CopilotChat.presentation.test.ts` 等既有护栏迁移/保留并通过;`pnpm typecheck`、`pnpm test`、`pnpm build` 全绿。
