# F2: 冷启动首屏

**优先级**: P0
**状态**: DONE

## 目标

把进入应用的第一眼从「旧 6 菜单 / 浮动聊天插件」改为一屏式的 agent 起点：居中一个大输入框（支持语音）+ 起手芯片（高频问题 + 固定报表跳转）+ 三张卡（⚠ 主动信号、↻ 继续上次会话、📌 我的资产）。用户问出（或说出）第一句后，首屏让位、分裂为对话脊柱 + 活产物画布（由 F3/F4 承载）。本 Feature 只负责「冷启动空对话态」的引导与发起，不实现对话/画布本身。

> 设计依据：`docs/superpowers/specs/2026-05-30-agent-first-ui-design.md`（§4「状态一 · 冷启动首屏（空对话）」、§9 分期注「P1 冷启动首屏可先放一个『打开报花月报』的简单跳转芯片；⑥ 的完整承接在 P2」、决策 D2 对话脊柱 + 语音主轴）。
>
> P1 范围裁剪：主动信号卡先用规则化简单提醒占位（完整版主动洞察在 P3）；固定报表芯片先做简单跳转 / 直接发起对应查询（模板化完整承接在 P2）。

## 复用与依赖

- 复用 `src/components/copilot/WelcomeCard.tsx`（起手问题芯片来源 `AGENT_REPORT_BUSINESS_GUIDE` + `buildWelcomeSuggestionGroups`）、`welcomeCardModel.ts`（默认分组）。
- 复用 `src/components/copilot/VoiceInputButton.tsx` + `useVoiceInput`（语音转写）。
- 会话发起复用 F3 的 `CopilotChat.handleSendText` 路径；首屏作为外层壳通过 `dts:ai-quick-ask`（`AiQuickAsk.dispatchAiQuickAsk`）或新的「首屏 → 对话」发起回调与 F3 衔接。
- 数据接入 `analyticsApi.listDashboards` / `listCards`（资产计数）、`listAiAgentSessions`（继续上次会话）、`listSuggestedQuestions`（起手问题）。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 首屏布局与大输入框 | P0 | DONE | F1 |
| T02 | 语音入口接入 | P0 | DONE | T01 |
| T03 | 起手芯片 | P0 | DONE | T01 |
| T04 | 三张卡数据接入 | P1 | DONE | T01 |

## 完成标准

- [x] 进入工作台空态时第一眼是冷启动首屏：居中大输入框（占位「问一句，或说一句…」）+ 起手芯片 + 三张卡，不再是旧菜单或空白。
- [x] 在大输入框输入并提交（回车 / 点发送）即发起会话进入对话脊柱（F3），首屏退场。
- [x] 语音按钮可用：最终转写文本回填输入框，可直接发送。
- [x] 起手芯片可点：高频问题芯片点击即发起对应查询；固定报表芯片直接发起对应查询。
- [x] 三张卡有真实数据：我的资产显示看板/卡片计数；继续上次会话显示最近会话并可点回跳；主动信号卡至少有规则化占位提醒，无信号时优雅降级。
- [x] 首屏新建组件单文件 < 800 行，关键行为（提交发起、芯片点击、空态降级）有 vitest 测试护栏。
- [x] `pnpm typecheck`、`pnpm test`、`pnpm build` 全绿。
