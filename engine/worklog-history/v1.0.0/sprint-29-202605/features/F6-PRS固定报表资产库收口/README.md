# F6: PRS固定报表资产库收口

**优先级**: P0
**状态**: DONE

## 目标

把已认证的 PRS 花卉租赁固定报表/大屏资产纳入资产库「看板」tab,让用户能看到 `PRS 项目客户经营看板` 等 dts-stack/dbt 大屏资产,并通过有效入口进入 Agent BI 固定报表执行链路。

## 背景

Sprint-29 完成的是平台指标联邦,资产库新增的是「平台指标」tab;但 PRS 花卉租赁大屏属于 `analytics_report_template` 的固定报表资产,不属于平台指标目录。原看板 tab 只展示 `analytics_dashboard` 和 6 个未分组固定报表快捷入口,还把入口指向 `/dashboards/new?fixedReportTemplate=...`,导致用户看到的资产不完整且链接语义错误。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 资产库大屏入口与链接修复 | P0 | DONE | Sprint-28 F1 fixedReport query |

## 完成标准

- [x] 固定报表快捷入口按资产组展示主报表,`DBT_SPLIT` 子报表不挤占大屏入口。
- [x] `screen.*` / `DBT_SCREEN_TABLE` 被识别为 PRS 大屏资产。
- [x] 入口跳转 `/agent-bi?fixedReport=...`,不再进入 dashboard 创建页。
- [x] 补前端单测和本地 evidence。
