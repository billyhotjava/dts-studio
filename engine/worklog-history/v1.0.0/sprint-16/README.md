# Sprint-16: 业务页面盘点、数据库对照与固定报表实施基线 (RC)

**前缀**: RC (Report Catalog)
**状态**: IN_PROGRESS
**目标**: 基于现网业务页面、`adminweb/adminapi/app` 三端源码和 `rs_cloud_flower` 业务库，锁定财务、采购、仓库优先的固定报表目录，建立页面-后端-数据库-模板四方对照，并为后续固定报表快路径实施提供基线。

## 背景

通过真实登录 `app.xycyl.com` 的只读账号盘点后确认：

- 现网系统不是“没有报表”，而是大量报表散落在业务菜单里
- 业务库 `rs_cloud_flower` 有 `163` 张物理表，但 `0` 张视图
- 如果继续让 Copilot 对所有问题都现场找表、拼 SQL，已知报表场景会慢且不稳

因此，本 sprint 的目标不是直接改代码，而是先把：

1. 页面清单
2. 数据库结构
3. 固定报表候选
4. 受控取数面
5. 模板优先实施任务

全部固化下来，作为后续实施输入。

## 现状核实

### 页面侧

- 当前可见路由：`67`
- 其中报表型页面：至少 `60`
- 报表能力最集中的域：
  - 财务管理
  - 运营管理
  - 采购管理
  - 仓库管理
  - 报花管理
  - 项目点管理
  - 任务中心
  - 准备池

### 源码侧

#### adminweb

- `flower/purchase`: `13` 个 Vue 页面
- `flower/store`: `26` 个 Vue 页面
- `flower/finance`: `22` 个 Vue 页面
- `flower/operate`: `20` 个 Vue 页面
- `flower/project`: `46` 个 Vue 页面
- `flower/task`: `51` 个 Vue 页面
- `plant`: `23` 个 Vue 页面

结论：PC 端已经沉淀了大量列表、汇总、明细和统计型页面，是固定报表模板的首要来源。

#### adminapi

- `project/controller`: `19`
- `flowerbiz/controller`: `18`
- `purchase/controller`: `6`
- `task/controller`: `5`
- `tasknew/controller`: `9`
- `operate/controller`: `7`
- `pendulum/controller`: `6`

结论：后端对采购、仓库、报花、任务、项目、运营等域已经有明确控制器边界，可直接作为固定报表取数面的后端归属基础。

#### app

- `pages/task`: `100` 个页面
- `pages/flowers`: `88` 个页面
- `pages/purchase`: `20` 个页面
- `pages/warehousing`: `13` 个页面
- `pages/pendulum`: `17` 个页面
- `pages/project`: `8` 个页面

结论：移动端明显偏现场执行和流程操作，不是财务固定报表的主入口；但对采购、仓库、报花、任务的移动查询和轻量汇总会影响模板设计和后续移动适配。

### 数据库侧

- 业务库：`rs_cloud_flower`
- 物理表：`163`
- 视图：`0`
- 核心表族：
  - 财务：`f_*`、`a_*accounting`、`a_invoice_*`
  - 采购/仓库：`i_purchase_*`、`t_plan_purchase_*`、`t_delivery_*`、`t_warehousing_*`、`s_stock_*`
  - 报花/任务：`t_flower_biz_*`、`t_daily_task_*`
  - 项目：`p_project*`、`p_position*`

## 关键结论

1. 已知高频报表应优先模板化，而不是继续走 NL2SQL
2. 未知问题才进入 Copilot 探索模式
3. 由于业务库无视图，必须建设受控取数面
4. 第一阶段优先域应为：
   - 财务
   - 采购
   - 仓库
5. 第一阶段固定报表主入口应优先落在 PC 端，移动端优先承接轻量查询、任务协同和现场核对

## 当前实现差距

在继续实现前，已经确认 `dts-copilot` 当前固定报表能力存在 4 个关键缺口：

1. `0040_seed_finance_procurement_templates.xml` 已经种入了 `16` 个 `FIN/PROC/WH` 模板，但除 `FIN-AR-OVERVIEW`、`PROC-SUPPLIER-AMOUNT-RANK`、`WH-STOCK-OVERVIEW` 和 `WH-LOW-STOCK-ALERT` 外，大多数 `target_object` 仍是 `authority.finance.*`、`authority.procurement.*`、`authority.inventory.*` 这类概念型目标，不是已验证的业务取数面。
2. 当前 `rs_cloud_flower` 测试库里没有任何 MySQL VIEW；而 `0036_business_views_metadata.xml` 里登记的 `7` 个 `v_*` 业务视图只存在于 sprint-10 的 SQL 资产，还没有实际落库。
3. `FixedReportResource` 当前只返回执行计划元数据，不返回真实报表结果，因此“查看固定报表”更多是目录/路由能力，不是已经完成的数据交付能力。
4. Copilot 模板优先命中的前端体验已经贯通，但它目前只是把用户带到固定报表页，不能保证模板就一定命中到真正可执行的数据面。

因此，sprint-16 的真正目标不是继续堆模板，而是先完成：

- 报表候选目录的真实页面化
- 固定报表模板种子的业务化
- 固定报表 backing 状态的核对和退役策略
- L0/L1 受控取数面的最小实施基线

## 本轮代码进展

当前已经完成两条可见改进：

1. 固定报表占位治理
   - 目录、详情、运行接口都已显式区分 `BACKING_REQUIRED`
   - 占位模板不会再被表现成“已可直接运行”
2. 页面语言对齐
   - 新增 `0041_refresh_fixed_report_page_labels.xml`
   - 当前 `FIN/PROC/WH` 模板目录名称开始向现网页面心智收敛
   - `TemplateMatcherService` 已支持 `财务结算汇总 / 采购汇总 / 库存现量` 等页面化问法
3. 欢迎建议对齐
   - `getSuggestedQuestions()` 已优先混入固定报表页面语言
   - Copilot 首屏建议开始向现网页面问法收敛
4. 候选固定报表直答
   - 泛问 `财务报表 / 采购报表 / 仓库报表` 时，Copilot 已会先返回固定报表候选，不再直接掉回探索模式
   - 候选结果会在聊天区展示成可点击的固定报表入口
5. 现网页面锚点回落
   - `FixedReportResource` / `ReportTemplateCatalogService` 已向已知模板暴露 `legacyPageTitle / legacyPagePath`
   - `FixedReportRunPage` 已可直接打开 `app.xycyl.com` 对应现网页面，作为 backing 未完全接通前的临时落点
6. 多入口固定报表复用
   - `ReportFactoryPage` 已接入固定报表快捷入口
   - `DashboardsPage` 已接入同一套固定报表快捷入口
   - `ScreensPage` 已接入轻量固定报表快捷区
   - 三个入口统一复用 `buildFixedReportQuickStartItems()` 的业务优先级排序
7. 创建流程 handoff
   - `DashboardsPage` 固定报表快捷入口已下沉到 `DashboardEditorPage` 创建链
   - `ReportFactoryPage` 固定报表快捷入口已下沉到报告模板创建链
   - `ScreensPage` 固定报表快捷入口已下沉到大屏创建链，并可直接基于固定报表生成大屏草稿
8. 浏览器级多入口验收
   - 已新增 `it/test_multi_surface_fixed_report_reuse.sh`
   - 实测通过 `固定报表目录 / Dashboard / Report Factory / Screens` 四处入口与 handoff 一致性
9. 首个真实 backing 接通
   - 新增 `0044_promote_procurement_summary_fixed_report.xml`
   - `PROC-SUPPLIER-AMOUNT-RANK` 已提升为真实可执行模板，当前页面名称为 `采购汇总`
   - `target_object` 已固定到 `authority.procurement.purchase_summary`
   - `FixedReportResource` 现在可直接返回结果预览，而不是只返回执行计划
   - `FixedReportRunPage` 已能展示 `databaseName / rowCount / preview table`
10. 仓库域首个真实 backing 接通
   - 新增 `0045_promote_stock_overview_fixed_report.xml`
   - `WH-STOCK-OVERVIEW` 已提升为真实可执行模板，当前页面名称为 `库存现量`
   - `target_object` 已固定到 `authority.inventory.stock_overview`
   - `FixedReportResource` 现在可直接返回库存现量结果预览
   - 真实权威表已锁定到 `s_stock_info + b_goods_price`
11. 财务域首个真实 backing 接通
   - 新增 `0046_promote_finance_settlement_summary_fixed_report.xml`
   - `FIN-AR-OVERVIEW` 已提升为真实可执行模板，当前页面名称为 `财务结算汇总`
   - `target_object` 已固定到 `authority.finance.settlement_summary`
   - `FixedReportResource` 现在可直接返回财务结算汇总结果预览
   - 真实权威表已锁定到 `f_settled_items`
12. 平台数据源详情 fallback 收口
   - 当 `analytics_database.details_json` 仍引用 `dataSourceId`，但 `analytics -> copilot-ai` 的详情接口暂时不可达时
   - `PlatformInfraClient` 会回退到本地 `copilot_ai.data_source` 读取 JDBC/账号信息
   - 固定报表运行不再因为 `未返回数据源详情` 直接失败
13. 仓库域第二个真实 backing 接通
   - 新增 `0047_promote_low_stock_alert_fixed_report.xml`
   - `WH-LOW-STOCK-ALERT` 已提升为真实可执行模板，当前页面名称为 `库存现量-低库存预警`
   - `target_object` 已固定到 `authority.inventory.low_stock_alert`
   - `FixedReportRunPage` 现在可直接返回低库存清单预览，而不是占位态
   - 真实权威表仍锁定到 `s_stock_info + b_goods_price`

## 任务列表

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| RC-01 | 现网页面盘点与 `adminweb/app` 报表型页面识别 | P0 | DONE | 真实登录态 |
| RC-02 | 业务库扫描与 `adminapi/adminweb/app` 对照 | P0 | DONE | RC-01 |
| RC-03 | 固定报表 Top 30 候选目录 | P0 | DONE | RC-01, RC-02 |
| RC-04 | 受控取数面策略（L0/L1） | P0 | DONE | RC-02, RC-03 |
| RC-05 | 固定报表模板模型与目录种子 | P1 | IN_PROGRESS | RC-03, RC-04 |
| RC-06 | 模板优先 Copilot 路由接入 | P1 | IN_PROGRESS | RC-05 |
| RC-07 | Dashboard / Screen / Report Factory 模板复用 | P2 | DONE | RC-05 |
| RC-08 | IT 验收与性能基线 | P2 | DONE | RC-04~RC-07 |
| RC-09 | 固定报表 backing 审计与占位模板退役 | P0 | DONE | RC-03, RC-04 |

## 首批固定报表候选

### 财务

- 财务结算列表
- 财务结算汇总
- 财务报表（月度）
- 开票管理
- 日常报销
- 预支申请
- 支出管理

### 采购

- 采购汇总
- 采购明细
- 采购计划明细
- 采购驳回
- 配送记录

> 当前已接通真实 backing：
> - `财务结算汇总 (FIN-AR-OVERVIEW)`
> - `采购汇总 (PROC-SUPPLIER-AMOUNT-RANK)`
> - `库存现量 (WH-STOCK-OVERVIEW)`
> - `库存现量-低库存预警 (WH-LOW-STOCK-ALERT)`

### 仓库

- 库存现量
- 出入库记录
- 入库管理
- 出库管理
- 报损管理
- 调拨管理
- 调拨明细
- 退货管理

### 第二梯队样板

- 报花单据管理
- 报花明细
- 任务中心
- 我的任务
- 项目点汇总
- 实摆物品统计
- 实摆租金统计
- 准备池-备货汇总
- 准备池-项目汇总
- 准备池-采购计划汇总

## 代表性现网页面锚点

为了避免“只按业务名脑补模板”，本 sprint 采用以下现网页面作为实施锚点：

### 财务 / 运营

- `财务结算列表`
  - `adminweb`: `src/views/flower/finance/settlement/list-settlement.vue`
  - `adminapi`: `finace/controller/SettlementController.java`
- `财务结算汇总`
  - `adminweb`: `src/views/flower/finance/settlement/summary.vue`
  - `adminapi`: `finace/controller/SettlementController.java`
- `财务报表（月度）`
  - `adminweb`: `src/views/flower/report/cost/list-report.vue`
  - `adminapi`: `jmreport/controller/MonthReportInfoController.java`
- `开票管理`
  - `adminapi`: `operate/controller/InvoiceInfoController.java`
- `日常报销 / 预支申请 / 支出管理`
  - `adminweb`: `src/views/flower/finance/expense/*`
  - `adminapi`: `finace/controller/ExpenseController.java`, `advance/controller/AdvanceInfoController.java`, `finace/controller/PayRecordController.java`

### 采购

- `采购汇总`
  - `adminweb`: `src/views/flower/purchase/purchase/*`
  - `adminapi`: `purchase/controller/PurchaseInfoController.java`
- `采购明细`
  - `adminweb`: `src/views/flower/purchase/purchaseitem/list-purchase-good.vue`
  - `adminapi`: `purchase/controller/PurchaseInfoController.java`
- `采购计划明细`
  - `adminweb`: `src/views/flower/purchase/plan/list-plan-purchase.vue`
  - `adminapi`: `purchase/controller/PlanPurchaseInfoController.java`
- `采购驳回`
  - `adminapi`: `purchase/controller/PurchaseRejectController.java`
- `配送记录`
  - `adminapi`: `purchase/controller/DeliveryInfoController.java`

### 仓库

- `库存现量 / 出入库记录`
  - `adminweb`: `src/views/flower/store/storeinfo/store-index.vue`, `list-store-info.vue`
  - `adminapi`: `storehouse/controller/StockInfoController.java`
- `入库管理`
  - `adminweb`: `src/views/flower/store/warehousing/list-warehousing.vue`
  - `adminapi`: `storehouse/controller/WarehousingInfoController.java`
- `出库管理`
  - `adminapi`: `storehouse/controller/ExWarehouseInfoController.java`
- `报损管理`
  - `adminapi`: `storehouse/controller/FrmLossController.java`
- `调拨管理 / 调拨明细`
  - `adminweb`: `src/views/flower/store/allocation/*`
  - `adminapi`: `storehouse/controller/AllocationController.java`
- `退货管理`
  - `adminapi`: `back/controller/ProjectGreenBackController.java`

### 项目 / 统计样板

- `项目点汇总`
  - `adminweb`: `src/views/flower/statistics/projectsummary/list-project-summary.vue`
  - `adminapi`: `statistics/controller/ProjectSummaryController.java`
- `实摆物品统计`
  - `adminweb`: `src/views/flower/statistics/realgood/*`
  - `adminapi`: `green/controller/ProjectRealGoodController.java`
- `实摆租金统计`
  - `adminweb`: `src/views/flower/statistics/realrent/*`
  - `adminapi`: `green/controller/ProjectRealRentController.java`

### 移动端补充

- `项目点统计`
  - `app`: `pages/project/summary/project-summary.vue`
  - `adminapi`: `app/controller/AppProjectSummaryController.java`
- `备货/采购/配送汇总`
  - `app`: `pages/choice/choice-index.vue`, `pages/purchasenew/list-need-purchase.vue`

## 验收标准

- [x] 固化现网业务页面盘点结果
- [x] 固化业务库扫描和页面-数据库对照结论
- [x] 形成 Top 30 固定报表候选目录
- [x] 形成 L0/L1 受控取数面策略
- [ ] 形成固定报表模板模型与目录种子方案
- [ ] 形成模板优先实施任务和验收基线
- [x] 完成固定报表 backing 审计并给出退役/改造策略

## 关联文档

- [设计文档](/opt/prod/prs/source/dts-copilot/docs/plans/2026-03-21-xycyl-report-catalog-and-db-grounding-design.md)
- [实现计划](/opt/prod/prs/source/dts-copilot/docs/plans/2026-03-21-xycyl-report-catalog-and-db-grounding-plan.md)
- [RC-01-visible-page-inventory-and-report-like-pages.md](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-16/tasks/RC-01-visible-page-inventory-and-report-like-pages.md)
- [RC-02-business-db-scan-and-page-mapping.md](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-16/tasks/RC-02-business-db-scan-and-page-mapping.md)
