# Sprint-20 双入口协同验收矩阵

## 覆盖范围

| 场景 | 入口 | 动作 | 期望 | 当前证据 |
|------|------|------|------|----------|
| 查询资产中心展示双入口资产 | `/questions` | 切换 `全部 / 正式查询 / Copilot 草稿 / 最近分析` | 可按来源与状态管理分析资产 | `tests/queryAssetCenterModel.test.ts`, `tests/queryAssetLifecycle.test.ts` |
| Copilot 结果保存为草稿 | `InlineSqlPreview` | 点击 `保存草稿` | 生成 `analysis_draft`，不直接污染正式查询目录 | `tests/copilotAnalysisDraft.test.ts` |
| Copilot 在查询中打开 | `InlineSqlPreview` | 点击 `在查询中打开` | 进入查询编辑页并保留来源上下文 | `tests/queryDraftHandoff.test.ts` |
| Copilot 创建可视化直达 | `InlineSqlPreview` | 点击 `创建可视化` | 查询编辑页聚焦结果/可视化区域 | `tests/copilotAnalysisDraft.test.ts` |
| 查询编辑页展示来源条 | `/questions/new?draft=...` | 查看页面顶部 | 显示来源、问题、数据源、回到对话动作 | `tests/analysisProvenanceModel.test.ts` |
| 仪表盘保留来源追溯 | `/dashboards/new?...` / `/dashboards/:id?...` | 保存后查看详情 | 可回到草稿/来源查询/固定报表 | `tests/analysisAssetProvenanceEntry.test.ts` |
| 大屏保留来源追溯 | `/screens?...` / `/screens/:id/edit?...` | 创建后查看设计页 | 可回到草稿/来源查询/固定报表 | `tests/analysisAssetProvenanceEntry.test.ts` |
| 已发布资产来源统一编码 | published assets | 读取 query params | `analysisDraft / fixedReportTemplate / sourceCard` 编码一致 | `tests/analysisDraftSurfaceEntry.test.ts`, `tests/analysisAssetProvenanceEntry.test.ts` |
| 双入口前端基线 | repo root | 执行 smoke | node 契约测试、typecheck、build 全通过 | `it/test_analysis_workspace_peer_entry.sh` |
| 采购域业务回归 | Copilot | 询问 `2025年2月绿萝采购详细情况` | 不再走错误表链，结果对齐回归基线 | `it/procurement-query-regression.md` + `it/test_procurement_query_regression.sh` + 真人联调 |

## 自动化证据

- 一键脚本：
  - `it/test_analysis_workspace_peer_entry.sh`
- 契约测试：
  - `tests/analysisProvenanceModel.test.ts`
  - `tests/queryAssetCenterModel.test.ts`
  - `tests/queryAssetLifecycle.test.ts`
  - `tests/queryDraftHandoff.test.ts`
  - `tests/analysisDraftSurfaceEntry.test.ts`
  - `tests/analysisAssetProvenanceEntry.test.ts`
  - `tests/copilotAnalysisDraft.test.ts`
- 构建基线：
  - `pnpm --dir dts-copilot-webapp run typecheck`
  - `pnpm --dir dts-copilot-webapp run build`

## 待补真人证据

- 登录后从 Copilot 生成一条 SQL 类回答
- 保存草稿并进入查询页
- 从查询页晋升为正式查询
- 从正式查询进入仪表盘 / 大屏 / 报告工厂
- 核对采购域绿萝问句回归结果
