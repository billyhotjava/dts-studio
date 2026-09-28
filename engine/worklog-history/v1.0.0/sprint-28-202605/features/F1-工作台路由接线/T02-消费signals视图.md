# T02: 消费 `view=signals` 并渲染信号容器

**优先级**: P0
**状态**: DONE
**依赖**: T01

## 目标

点击“信号”进入 `/agent-bi?view=signals` 后进入信号视图容器,不能保留死链。

## 技术设计

- 在 `AgentWorkspacePage` 的 `workspaceView` 分支中增加 `signals`。
- 先实现 `dts-copilot-webapp/src/components/copilot/signals/SignalsView.tsx` 容器,展示加载态、空态、错误态和数据态。
- 数据接入由 F3 完成;本任务先保证路由与容器存在。
- 若 F3 尚未完成,导航入口可保留但视图必须显示“信号数据未接通”的受控空态,不得显示假业务预警。

## 影响范围

- `dts-copilot-webapp/src/pages/AgentWorkspacePage.tsx`
- `dts-copilot-webapp/src/components/copilot/signals/SignalsView.tsx`
- `dts-copilot-webapp/src/layouts/appNavigation.ts`
- `dts-copilot-webapp/src/pages/AgentWorkspacePage.test.ts`

## 验证

- [x] `pnpm test -- AgentWorkspacePage`
- [x] Playwright 打开 `/agent-bi?view=signals`,确认页面不是冷启动首屏,且没有占位假信号。

## 完成标准

- [x] `/agent-bi?view=signals` 有稳定视图。
- [x] 未接通真实数据时显示受控空态。
- [x] F3 接入后该容器可直接承载真实 signals。

## 证据

- `worklog/v1.0.0/sprint-28-202605/it/evidence/20260531-local/f1-workspace-routing.md`
