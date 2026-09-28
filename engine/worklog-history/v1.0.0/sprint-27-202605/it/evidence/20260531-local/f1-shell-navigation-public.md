# Sprint-27 F1 应用骨架与导航重构验证

**日期**: 2026-05-31
**范围**: F1-T01 ~ F1-T05, IT01 / IT02 / IT10
**模块**: `dts-copilot-webapp`

## 变更摘要

- `/agent-bi` 重新注册为 `AgentWorkspacePage`,并保留 `/`、`/home`、`/modern` 到 `APP_HOME_PATH` 的重定向。
- 删除旧碎片页面与路由:`AnalyzePage`、`ExploreSessionsPage`、`ReportFactoryPage`、`MetricLensPage`、旧 `AgentReportsPage`,以及 `/analyze`、`/explore-sessions`、`/report-factory`、`/fixed-reports`、`/metric-lens`。
- `AppLayout` 增加工作台 shell、公开路由极简分支,工作台下不再渲染浮动 `CopilotSidebar`。
- 左侧导航收口为 `newChat` / `chatHistory` / `assets` / `signals`;治理项 `data/models/metrics/admin` 收进右上角治理菜单,按 `privileged/superuser` 可见。
- 移动端由底部 tab 接管核心导航,桌面侧栏在 `<768px` 隐藏,避免挤压 Agent 工作台主区。
- `/public/card/:uuid`、`/public/dashboard/:uuid`、`/public/screen/:uuid` 保留,公开页移除 `/analyze` 与鉴权页跳转死链。

## 验证命令

```bash
cd dts-copilot-webapp
pnpm exec vitest run src/layouts/appNavigation.test.ts src/layouts/AppLayout.workspace.test.ts src/routes.agentWorkspace.test.ts src/pages/AgentWorkspacePage.test.ts
```

结果: 4 个测试文件通过,13 个测试通过。

```bash
cd dts-copilot-webapp
pnpm typecheck
```

结果: 通过。

```bash
cd dts-copilot-webapp
pnpm test
```

结果: 29 个测试文件通过,126 个测试通过。

```bash
cd dts-copilot-webapp
pnpm build
```

结果: 通过。`validate:screen-plugins` 与 `validate:screen-plugin-boundary` 均通过;Vite 仍提示既有大 chunk warning,不影响构建结果。

```bash
cd dts-copilot-webapp
rg -n 'AppLayout|appNavigation|CopilotSidebar|/analyze|nav\.analyze|AgentReportsPage|AnalyzePage|ExploreSessionsPage|ReportFactoryPage|MetricLensPage' src/pages/PublicCardPage.tsx src/pages/PublicDashboardPage.tsx src/pages/screens/PublicScreenPage.tsx
rg -n 'Link to=' src/pages/PublicCardPage.tsx src/pages/PublicDashboardPage.tsx src/pages/screens/PublicScreenPage.tsx
```

结果: 无输出,公开分享页无工作台壳/导航依赖,且无 `/analyze` 或鉴权页死链。

```bash
cd dts-copilot-webapp
rg -n 'nav\.analyze|nav\.exploreSessions|nav\.reportFactory|nav\.fixedReports|nav\.metricLens|analyze\.title|reportFactory\.title|metricLens\.title|path: "/analyze"|path: "/explore-sessions"|path: "/report-factory"|path: "/metric-lens"|path: "/fixed-reports"' src
```

结果: 仅 `routes.agentWorkspace.test.ts` 中的负向断言命中,生产源码无旧路由注册残留。

## Follow-up Smoke

```bash
cd dts-copilot-webapp
pnpm exec vitest run src/layouts/AppLayout.workspace.test.ts
# Test Files 1 passed; Tests 5 passed
```

- Playwright desktop smoke: `http://localhost:3004/agent-bi` 首屏正常渲染工作台、左侧导航和起手问题。
- Playwright mobile smoke(390x844):桌面侧栏隐藏,主区占满 390px,底部 tab 保留核心导航。

## 备注

- `chatHistory` 与 `signals` 当前用 `/agent-bi?...` 占位避免 404;后续由 F3/信号 Feature 接管真实二级体验。
- `assets` 当前临时落到 `/dashboards`;后续由 F7 资产库接管。
