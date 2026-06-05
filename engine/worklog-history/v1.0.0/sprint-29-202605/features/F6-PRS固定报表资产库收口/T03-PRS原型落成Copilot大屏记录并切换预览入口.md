# T03: PRS原型落成Copilot大屏记录并切换预览入口

**优先级**: P0
**状态**: DONE
**依赖**: T01,T02

## 目标

取消 PRS 固定报表资产的 Agent fixedReport 打开路径,把 `worklog/prs/v1/screens/*.json` 中已 review 的 12 个大屏原型落成 `analytics_screen` 运行态记录,资产库点击后直接进入 Copilot 自有大屏预览。

## 根因

- `/agent-bi?fixedReport=...` 是 Agent 文本/表格执行链路,不适合承载 PRS 大屏布局。
- 资产库已经展示 12 个 PRS 报表,但没有对应 `analytics_screen` 实体,所以不能用 `/screens/:id/preview` 打开。
- PRS v1 原型使用 `richtext` 背景组件,但 `specV2` 白名单未包含该类型,预览规范化时会丢组件。

## 技术设计

- 为 `PRS_SCREEN_SHORTCUTS` 增加稳定 `screenId=290001..290012`。
- `buildFixedReportOpenPath` 和 Agent 报表 href 对 PRS 模板返回 `/screens/{screenId}/preview`;旧 WH/FIN/PROC 仍按 T02 规则降级。
- `specV2` 支持 `richtext`,避免 PRS 背景层被过滤。
- 新增 Liquibase `0064_prs_flowerbiz_copilot_screen_records.xml`,把 12 个 JSON 原型写入 `analytics_screen`,并把 `{{DATABASE_ID}}` 替换为当前 `DTS dbt模型库` 的数据库 ID。
- 同步更新 `analytics_report_template.spec_json/presentation_schema_json`,记录 `screenId` 和 `screenPreviewPath`。

## 影响范围

- `dts-copilot-webapp/src/shared/prsScreenShortcuts.ts`
- `dts-copilot-webapp/src/pages/fixed-reports/fixedReportCatalogModel.ts`
- `dts-copilot-webapp/src/shared/fixedReportAvailability.ts`
- `dts-copilot-webapp/src/pages/screens/specV2.ts`
- `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0064_prs_flowerbiz_copilot_screen_records.xml`

## 验证

- [x] 先补失败测试,确认 PRS 资产仍错误跳 `/agent-bi?fixedReport=...`,且 `richtext` 会被过滤。
- [x] `pnpm test -- fixedReportCatalogModel DashboardsPage specV2 copilotFixedReportMessage artifact ArtifactCanvas`
- [x] `pnpm typecheck`
- [x] XML 解析校验 master/0063/0064。
- [x] 0064 SQL 在本地库事务内 dry-run 返回 `screen_count=12, acl_count=12, template_count=12`。
- [x] 重建并重启 `copilot-analytics`、`copilot-webapp` 后,运行库存在 12 个 `analytics_screen` PRS 记录,无 `{{DATABASE_ID}}` 占位符。
- [x] 经 webapp 代理认证请求 `/api/screens/290006?mode=draft` 返回 `PRS 项目客户经营看板`,组件数 22。

## 完成标准

- [x] 资产库 PRS 主资产点击进入 `/screens/{screenId}/preview`。
- [x] 12 个 PRS 原型均有稳定 screen id 和运行态记录。
- [x] PRS 预览接口能返回组件与全局变量。
- [x] Agent fixedReport 不再承担 PRS 大屏展示。
