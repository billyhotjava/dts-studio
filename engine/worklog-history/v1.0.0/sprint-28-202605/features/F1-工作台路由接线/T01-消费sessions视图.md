# T01: 消费 `view=sessions` 并渲染历史会话

**优先级**: P0
**状态**: DONE
**依赖**: 无

## 目标

点击“历史会话”进入 `/agent-bi?view=sessions` 后展示可恢复的会话列表,不能停留在冷启动首屏。

## 技术设计

- 在 `dts-copilot-webapp/src/pages/AgentWorkspacePage.tsx` 引入 `useSearchParams`,增加 `workspaceView` 规范化逻辑。
- 复用 `ColdStartHome` 已使用的会话数据来源,抽出可独立渲染的历史会话面板,优先放在 `dts-copilot-webapp/src/components/copilot/sessions/`。
- 点击历史会话时设置 `submittedPrompt` 或 session focus,进入现有 `ConversationThread` 工作区。
- 在 `dts-copilot-webapp/src/layouts/appNavigation.test.ts` 或新建 `AgentWorkspacePage.routing.test.tsx` 断言 `/agent-bi?view=sessions` 不渲染冷启动主 CTA。

## 影响范围

- `dts-copilot-webapp/src/pages/AgentWorkspacePage.tsx`
- `dts-copilot-webapp/src/components/copilot/cold-start/ColdStartCards.tsx`
- `dts-copilot-webapp/src/components/copilot/sessions/*`
- `dts-copilot-webapp/src/pages/AgentWorkspacePage.test.ts`

## 验证

- [x] `pnpm test -- AgentWorkspacePage`
- [x] `pnpm typecheck`
- [x] Playwright 打开 `/agent-bi?view=sessions`,确认页面出现历史会话列表或空态,且不显示冷启动卡片组。

## 完成标准

- [x] 历史会话导航点击后有可见结果。
- [x] 空会话时显示空态,不是白屏或冷启动误导态。
- [x] 测试覆盖 query 参数规范化。

## 证据

- `worklog/v1.0.0/sprint-28-202605/it/evidence/20260531-local/f1-workspace-routing.md`
