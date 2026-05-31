# F6 PRS 固定报表资产库收口证据

日期: 2026-06-01

## 根因

- 资产库「看板」tab 的普通 dashboard 数据来自 `analytics_dashboard`;当前运行库只有 `Sprint27 IT08 看板...`,所以用户看不到 PRS 花卉租赁大屏。
- PRS 花卉租赁大屏实际沉淀在 `analytics_report_template`,例如 `PRS-FLOWERBIZ-PROJECT-CUSTOMER`。
- 原快捷入口只拉 `limit=12`,且未按 `assetGroup` 折叠,会被 `DBT_SPLIT` 子报表挤占;链接还指向 `/dashboards/new?fixedReportTemplate=...`,不是运行固定报表。

## 数据库核对

```sql
select template_code, name, target_object, published, archived, certification_status
from copilot_analytics.analytics_report_template
where template_code in ('PRS-FLOWERBIZ-PROJECT-CUSTOMER','PRS-FLOWERBIZ-OVERVIEW');
```

结果摘要:

```text
PRS-FLOWERBIZ-OVERVIEW         | PRS 租赁经营总览     | screen.prs-flowerbiz-overview-v1         | published=t | archived=f | CERTIFIED
PRS-FLOWERBIZ-PROJECT-CUSTOMER | PRS 项目客户经营看板 | screen.prs-flowerbiz-project-customer-v1 | published=t | archived=f | CERTIFIED
```

```sql
select id, name, description, archived
from copilot_analytics.analytics_dashboard
where archived=false
order by id;
```

结果摘要:

```text
1 | Sprint27 IT08 看板 20260531064612 | Sprint-27 IT08 live pin dashboard | f
```

## 代码验证

```bash
pnpm exec vitest run src/pages/DashboardsPage.test.tsx src/pages/fixed-reports/fixedReportCatalogModel.test.ts
```

结果:

```text
Test Files  2 passed (2)
Tests  19 passed (19)
```

```bash
pnpm exec vitest run src/pages/AssetLibraryPage.test.ts src/pages/DashboardsPage.test.tsx src/pages/fixed-reports/fixedReportCatalogModel.test.ts src/pages/AgentWorkspacePage.test.ts src/pages/MetricAssetsPanel.test.tsx
```

结果:

```text
Test Files  5 passed (5)
Tests  36 passed (36)
```

```bash
pnpm run typecheck
pnpm run build
```

结果:

```text
typecheck: PASS
build: PASS
note: vite 仍提示既有 chunk size warning,本次修复未新增构建错误。
```

覆盖点:

- `DashboardsPage` 固定报表目录请求 `limit=100`。
- `PRS 项目客户经营看板` 在资产库看板页以主资产出现。
- 入口 href 为 `/agent-bi?fixedReport=PRS-FLOWERBIZ-PROJECT-CUSTOMER`。
- `PRS 项目经营 TOP` 等 `DBT_SPLIT` 子报表不作为一级大屏快捷入口。
