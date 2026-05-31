# F1: 应用骨架与导航重构

**优先级**: P0
**状态**: DONE

## 目标

把 dts-copilot-webapp 从「BI 工具 + 浮动聊天插件」的多入口形态，重构为「单一 agent 工作台」骨架：删除碎片化旧路由（保留 `/public/*` 分享链接）、新建工作台首页与双栏壳、收口为 4 个一级导航入口，为后续 Feature（对话脊柱 F3 / 活产物画布 F4 / 资产库 F7 / 信号 等）提供承载容器。

> 设计依据：`docs/superpowers/specs/2026-05-30-agent-first-ui-design.md`（§4 整体形态、§5 信息架构收口、决策 D5/D7/D10）。
> 范围内不实现对话/画布/信号的具体业务，只搭骨架与插槽。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 删除旧 NL2SQL/报表碎片路由与页面 | P0 | DONE | F3-T00 现状测试基线 |
| T02 | 定义新工作台路由与首页壳 | P0 | DONE | T01 |
| T03 | AppLayout 改造为双栏工作台壳 | P0 | DONE | T02 |
| T04 | 4 个一级入口导航重构 | P0 | DONE | T01, T03 |
| T05 | /public/* 分享链接隔离验证 | P0 | DONE | T01 |

## 完成标准

- [x] `AnalyzePage` / `ExploreSessionsPage` / `ReportFactoryPage` / `MetricLensPage` 及旧 `AgentReportsPage` 页面与其路由注册全部删除，对应 URL 不再可达（命中 `NotFoundPage`）。
- [x] 删除旧页面前，F3-T00 的现状测试基线已经建立并通过，确保重构有回归闸门。
- [x] `ScreensCenterRedirect` / `FixedReportsRedirect` 过渡跳转清理完成；`ModernAliasRedirect` 行为对齐新首页。
- [x] `/public/card/:uuid`、`/public/dashboard/:uuid`、`/public/screen/:uuid` 三类分享链接仍保留，且不依赖任何被删除的页面/组件（决策 D10）。
- [x] `dashboards` / `questions(cards)` / `collections` / `screens` 页面与路由保留可达，供 F7 资产库复用；`data` / `models` / `metrics` / `admin` 保留作治理后台。
- [x] `APP_HOME_PATH` 指向新工作台，根路径 `/` 重定向到工作台首页。
- [x] `AppLayout` 区分「全屏布局」（auth/public/screens）与「工作台双栏布局」，双栏容器留有 F3/F4 插槽。
- [x] 左侧一级导航收口为「＋新对话 / 💬历史会话 / 📁资产库 / 🔔信号」四项；治理后台入口收进右上角并按 `privileged/superuser` 控制可见。
- [x] `appNavigation.test.ts` 更新并通过；`pnpm typecheck`、`pnpm test`、`pnpm build` 全绿。
