# F6: PRS固定报表资产库收口

**优先级**: P0
**状态**: DONE

## 目标

把已认证的 PRS 花卉租赁固定报表/大屏资产纳入资产库「看板」tab,让用户能看到 `PRS 项目客户经营看板` 等 dts-stack/dbt 大屏资产,并通过有效入口进入 Copilot 自有大屏预览运行态。

## 背景

Sprint-29 完成的是平台指标联邦,资产库新增的是「平台指标」tab;但 PRS 花卉租赁大屏属于 `analytics_report_template` 的固定报表资产,不属于平台指标目录。原看板 tab 只展示 `analytics_dashboard` 和 6 个未分组固定报表快捷入口,还把入口指向 `/dashboards/new?fixedReportTemplate=...`,导致用户看到的资产不完整且链接语义错误。后续验证发现 `/agent-bi?fixedReport=...` 仍会把可视化报表拉回 Agent 文本/表格执行链路,因此最终收口为 `/screens/{screenId}/preview`。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 资产库大屏入口与链接修复 | P0 | DONE | Sprint-28 F1 fixedReport query |
| T02 | 旧固定报表死链降级与防回归 | P0 | DONE | T01 |
| T03 | PRS 原型落成 Copilot 大屏记录并切换预览入口 | P0 | DONE | T01,T02 |

## 完成标准

- [x] 固定报表快捷入口按资产组展示主报表,`DBT_SPLIT` 子报表不挤占大屏入口。
- [x] `screen.*` / `DBT_SCREEN_TABLE` 被识别为 PRS 大屏资产。
- [x] 入口跳转 `/screens/{screenId}/preview`,不再进入 dashboard 创建页或 Agent fixedReport 执行链路。
- [x] `worklog/prs/v1/screens/*.json` 的 12 个原型落成 `analytics_screen` 记录,并替换 `{{DATABASE_ID}}`。
- [x] WH/FIN/PROC 等已归档旧模板不再生成可点击死链;已有旧链接进入工作台时降级为 Agent 业务对象分析。
- [x] 补前端单测和本地 evidence。
