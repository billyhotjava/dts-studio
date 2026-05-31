# Sprint-28 诊断基线

## 核心结论

构建全绿不能证明运行时导航正确。当前缺陷主要来自“导航写入了 query 参数,消费端没有实现”,以及“旧 BI 资产页面被整页嵌进新资产库,形成双入口/双标题”。

## P0 缺陷

| 问题 | 证据 | Sprint-28 归属 |
|------|------|----------------|
| 历史会话入口 `/agent-bi?view=sessions` 无消费端 | `dts-copilot-webapp/src/layouts/appNavigation.ts` 指向 query,`AgentWorkspacePage.tsx` 未使用 `useSearchParams` | F1-T01 |
| 信号入口 `/agent-bi?view=signals` 无消费端 | `appNavigation.ts` 指向 query,`AgentWorkspacePage.tsx` 只按 `submittedPrompt` 切换冷启动/对话 | F1-T02,F3 |
| 固定报表入口 `/agent-bi?fixedReport=xxx` 丢上下文 | `fixedReportSurfaceEntry.ts` 生成 query,`AgentWorkspacePage.tsx` 未读取 | F1-T03 |
| 资产库双入口和双标题 | `AssetLibraryPage.tsx` 直接嵌入 `DashboardsPage` / `CardsPage` / `CollectionsPage`,同时 `routes.tsx` 保留列表一级路由 | F2 |

## P1 缺陷

| 问题 | 证据 | Sprint-28 归属 |
|------|------|----------------|
| Sprint-26 signals 能力没有前端落点 | `OntologyService` / semantic pack signals 已存在,前端冷启动仍用 `buildPlaceholderSignals()` | F3 |
| 状态簿记和证据口径不一致 | `sprint-queue.md` 中 S25/S26 与各 sprint README/IT 证据存在差异 | F4 |

## P2 清理

| 问题 | 证据 | Sprint-28 归属 |
|------|------|----------------|
| `Nl2SqlEvalPage` 孤儿页 | 文件存在但路由未消费 | F5-T01 |
| `appShellConfig` 死常量 | `CORE_NAV_PATHS` / `REMOVED_ROUTE_PREFIXES` 仅定义无实际消费 | F5-T02 |

