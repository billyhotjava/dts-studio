# F6/T02 旧固定报表死链降级证据

**日期**: 2026-06-01
**范围**: 资产库/Agent 对话中固定报表候选、固定报表消息快捷入口、Artifact 报表产物和 `/agent-bi?fixedReport=...` 旧链接落地页。

## 根因确认

运行库确认旧 WH/FIN/PROC 模板已归档,但前端此前仍会合成 fixedReport 链接:

```sql
select template_code,name,domain,published,archived,certification_status,updated_at
from copilot_analytics.analytics_report_template
where template_code in (
  'WH-LOW-STOCK-ALERT',
  'WH-STOCK-OVERVIEW',
  'FIN-AR-OVERVIEW',
  'PROC-SUPPLIER-AMOUNT-RANK',
  'PRS-FLOWERBIZ-OVERVIEW'
)
order by template_code, updated_at desc;
```

关键结果:

```text
FIN-AR-OVERVIEW           | 财务结算汇总        | 财务    | published=t | archived=t | CERTIFIED
PROC-SUPPLIER-AMOUNT-RANK | 采购汇总            | 采购    | published=t | archived=t | CERTIFIED
PRS-FLOWERBIZ-OVERVIEW    | PRS 租赁经营总览    | PRS租赁 | published=t | archived=f | CERTIFIED
WH-LOW-STOCK-ALERT        | 库存现量-低库存预警 | 仓库    | published=t | archived=t | CERTIFIED
WH-STOCK-OVERVIEW         | 库存现量            | 仓库    | published=t | archived=t | CERTIFIED
```

## 修复点

- `WH-LOW-STOCK-ALERT` 等旧模板在固定报表候选中保留为不可点击 chip。
- `FIXED_REPORT` 消息若命中旧模板,不再展示“回到 AI 报表入口”死链。
- 直接打开 `/agent-bi?fixedReport=WH-LOW-STOCK-ALERT` 时,工作台降级为 `agent-workspace-fixed-report-fallback` prompt,继续交给 Agent 分析。
- Artifact 报表产物只为当前可运行 PRS 模板附带 `reportHref`。

## 回归测试

```bash
pnpm exec vitest run \
  src/components/copilot/copilotFixedReportMessage.test.ts \
  src/components/copilot/MessageList.fixedReports.test.ts \
  src/pages/AgentWorkspacePage.test.ts \
  src/types/artifact.test.ts
```

结果:

```text
Test Files  4 passed (4)
Tests       24 passed (24)
```

扩大到资产库/固定报表/Agent 工作台/Artifact 的前端回归:

```bash
pnpm exec vitest run \
  src/pages/AssetLibraryPage.test.ts \
  src/pages/DashboardsPage.test.tsx \
  src/pages/fixed-reports/fixedReportCatalogModel.test.ts \
  src/components/copilot/copilotFixedReportMessage.test.ts \
  src/components/copilot/MessageList.fixedReports.test.ts \
  src/pages/AgentWorkspacePage.test.ts \
  src/types/artifact.test.ts \
  src/pages/MetricAssetsPanel.test.tsx
```

结果:

```text
Test Files  8 passed (8)
Tests       50 passed (50)
```

构建验证:

```bash
pnpm run typecheck
pnpm run build
```

结果:

```text
typecheck exit 0
build exit 0
```

全量前端测试:

```bash
pnpm test
```

结果:

```text
Test Files  68 passed (68)
Tests       275 passed (275)
```

生产源码死链扫描:

```bash
rg -n "fixedReport=WH-LOW-STOCK-ALERT|fixedReport=WH-STOCK-OVERVIEW|fixedReport=FIN-AR-OVERVIEW|fixedReport=PROC-SUPPLIER-AMOUNT-RANK" \
  dts-copilot-webapp/src \
  -g '!**/*.test.ts' \
  -g '!**/*.test.tsx'
```

结果:无匹配。
