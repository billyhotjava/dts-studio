# T04: 工作台 query 路由浏览器验证

**优先级**: P0
**状态**: DONE
**依赖**: T01,T02,T03

## 目标

用真实浏览器验证导航到 `sessions`、`signals`、`fixedReport` 三类 query 的运行时行为。

## 技术设计

- 使用 Playwright 打开本地 webapp。
- 通过侧边栏点击和直接 URL 两种方式验证路由。
- 记录截图或结构化输出到 `worklog/v1.0.0/sprint-28-202605/it/evidence/20260531-local/f1-workspace-routing.md`。

## 影响范围

- `worklog/v1.0.0/sprint-28-202605/it/evidence/20260531-local/f1-workspace-routing.md`
- `dts-copilot-webapp/src/pages/AgentWorkspacePage.test.ts`

## 验证

- [x] `pnpm test -- AgentWorkspacePage`
- [x] Playwright smoke 覆盖 `/agent-bi?view=sessions`
- [x] Playwright smoke 覆盖 `/agent-bi?view=signals`
- [x] Playwright smoke 覆盖 `/agent-bi?fixedReport=PRS-FLOWERBIZ-OVERVIEW`

## 完成标准

- [x] 三类 query 均有证据。
- [x] 证据文件包含命令、URL、关键断言和结果。

## 证据

- `worklog/v1.0.0/sprint-28-202605/it/evidence/20260531-local/f1-workspace-routing.md`
