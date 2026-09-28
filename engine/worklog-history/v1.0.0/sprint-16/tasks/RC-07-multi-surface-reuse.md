# RC-07 多入口复用

**状态**: DONE
**目标**: 让固定报表模板不只存在于报表目录，而能被多个展示入口复用。

## 复用入口

- 固定报表目录
- Dashboard
- Screen
- Report Factory
- Copilot

## 原则

- 同一张报表只维护一份模板
- 展示层只改变呈现方式，不改变口径

## 当前进展

- `ReportFactoryPage` 已接入固定报表快捷入口，优先展示财务 / 采购 / 仓库已认证模板
- `DashboardsPage` 已接入同一套快捷入口选择逻辑，不再维护独立报表清单
- `ScreensPage` 已接入轻量固定报表快捷区，支持从大屏管理页直接跳转到固定报表运行页
- 三个入口均复用 `buildFixedReportQuickStartItems()`，保持相同的业务优先级排序
- `DashboardsPage`
  - 快捷入口已下沉到 `/dashboards/new?fixedReportTemplate=...`
  - `DashboardEditorPage` 已能读取 `fixedReportTemplate` 并展示固定报表创建上下文
- `ReportFactoryPage`
  - 快捷入口已下沉到 `/report-factory?fixedReportTemplate=...`
  - 页面已能读取 `fixedReportTemplate` 并预填报告模板草稿语义
- `ScreensPage`
  - 快捷入口已下沉到 `/screens?fixedReportTemplate=...`
  - 页面已能读取 `fixedReportTemplate` 并直接生成基于固定报表的大屏草稿
- 浏览器级 smoke 已落地：
  - `it/test_multi_surface_fixed_report_reuse.sh`
  - 已验证目录页、Dashboard、ReportFactory、Screen 三处入口的 handoff 一致性

## 验收

- 同一张固定报表模板已可从 `Dashboard / Report Factory / Screens` 三个入口进入各自的创建流程
- 创建流程已能读取 `fixedReportTemplate` 并展示固定报表上下文
- 浏览器级 smoke 已覆盖目录页、三处入口和 handoff 页面一致性
