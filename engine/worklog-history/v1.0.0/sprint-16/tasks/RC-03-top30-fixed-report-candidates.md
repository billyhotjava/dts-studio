# RC-03 固定报表 Top 30 候选目录

**状态**: DONE
**目标**: 从现网高价值业务页中抽取首批固定报表候选，优先满足客户最常用、最容易被演示和最不适合继续走 NL2SQL 的场景。

## Top 30 候选

| 序号 | 业务域 | 现网页面 | 代表页面文件 | 后端归属 | 主要表族/对象 | 建议层 |
|------|--------|----------|--------------|----------|----------------|--------|
| 1 | 财务 | 财务结算列表 | `flower/finance/settlement/list-settlement.vue` | `SettlementController` | `a_month_accounting`, `a_collection_record` | L0 |
| 2 | 财务 | 财务结算汇总 | `flower/finance/settlement/summary.vue` | `SettlementController` | `a_month_accounting`, `a_collection_record` | L1 |
| 3 | 财务 | 财务报表（月度） | `flower/report/cost/list-report.vue` | `MonthReportInfoController` | 月报聚合口径 | L1 |
| 4 | 财务 | 开票管理 | `operate/invoice*` | `InvoiceInfoController` | `a_invoice_*` | L0 |
| 5 | 财务 | 日常报销 | `flower/finance/expense/list-expense.vue` | `ExpenseController` | `f_expense_*` | L0 |
| 6 | 财务 | 预支申请 | `expense/advance/*` | `AdvanceInfoController` | `f_advance_*` | L0 |
| 7 | 财务 | 支出管理 | `finace/controller/PayRecordController.java` 对应页面 | `PayRecordController` | `f_pay_*` | L0 |
| 8 | 采购 | 采购汇总 | `flower/purchase/purchase/*` | `PurchaseInfoController` | `i_purchase_*` | L1 |
| 9 | 采购 | 采购明细 | `flower/purchase/purchaseitem/list-purchase-good.vue` | `PurchaseInfoController` | `i_purchase_*` | L0 |
| 10 | 采购 | 采购计划明细 | `flower/purchase/plan/list-plan-purchase.vue` | `PlanPurchaseInfoController` | `t_plan_purchase_*` | L0 |
| 11 | 采购 | 采购驳回 | `flower/purchase/reject/*` | `PurchaseRejectController` | `i_purchase_reject_*` | L0 |
| 12 | 采购 | 配送记录 | `flower/distribution/*` | `DeliveryInfoController` | `t_delivery_*` | L0 |
| 13 | 仓库 | 库存现量 | `flower/store/storeinfo/list-store-info.vue` | `StockInfoController` | `s_stock_*`, `s_stock_item` | L0 |
| 14 | 仓库 | 出入库记录 | `flower/store/storeinfo/store-index.vue` | `StockInfoController` | `s_stock_*`, `t_warehousing_*`, `t_ex_warehouse_*` | L0 |
| 15 | 仓库 | 入库管理 | `flower/store/warehousing/list-warehousing.vue` | `WarehousingInfoController` | `t_warehousing_*` | L0 |
| 16 | 仓库 | 出库管理 | `flower/store/exwarehouse/*` | `ExWarehouseInfoController` | `t_ex_warehouse_*` | L0 |
| 17 | 仓库 | 报损管理 | `flower/store/loss/*` | `FrmLossController` | `t_frm_loss_*` | L0 |
| 18 | 仓库 | 调拨管理 | `flower/store/allocation/index.vue` | `AllocationController` | `t_allocation_*` | L0 |
| 19 | 仓库 | 调拨明细 | `flower/store/allocation/list-record.vue` | `AllocationController` | `t_allocation_*` | L0 |
| 20 | 仓库 | 退货管理 | `back/*` | `ProjectGreenBackController` | `a_back_green*` | L0 |
| 21 | 报花 | 报花单据管理 | `flower/flowerSum/list-flower-sum.vue` | `FlowerSumController` | `t_flower_biz_*` | L1 |
| 22 | 报花 | 报花明细 | `flower/flowerbiz/*` | `FlowerBizInfoController` / `FlowerDetailController` | `t_flower_biz_*` | L0 |
| 23 | 任务 | 任务中心 | `flower/task*` / `tasknew/*` | `TaskInfoController` / `ExecuteTaskController` | `t_daily_task_*` | L0 |
| 24 | 任务 | 我的任务 | `tasknew/*` | `DailTaskInfoController` / `ExecuteTaskController` | `t_daily_task_*` | L0 |
| 25 | 项目 | 项目点汇总 | `flower/statistics/projectsummary/list-project-summary.vue` | `ProjectSummaryController` | `p_project*`, `p_position*`, `p_project_green*` | L1 |
| 26 | 项目 | 实摆物品统计 | `flower/statistics/realgood/*` | `ProjectRealGoodController` | `p_project_green*` | L1 |
| 27 | 项目 | 实摆租金统计 | `flower/statistics/realrent/*` | `ProjectRealRentController` | `p_project_green*`, `a_month_accounting` | L1 |
| 28 | 准备池 | 备货汇总 | `plant/plant-distribute-index.vue` | `DistributeInfoController` | `t_plan_purchase_*`, `s_stock_*` | L1 |
| 29 | 准备池 | 项目汇总 | `app/pages/choice/choice-index.vue` 对应 PC 同类页 | `DistributeInfoController` | `t_plan_purchase_*`, `p_project*` | L1 |
| 30 | 准备池 | 采购计划汇总 | `plant/plant-distribute-index.vue` | `PlanPurchaseInfoController` | `t_plan_purchase_*` | L1 |

## 优先实施顺序

1. 财务
2. 采购
3. 仓库
4. 报花 / 任务 / 项目样板

## 实施原则

- 已知报表先模板化，不走 NL2SQL
- 每张报表都要绑定明确的 `templateCode`
- 每张报表都要标记实时性和取数来源
- 第一批模板命名应直接贴合现网页面，而不是抽象成不对应现网心智的概念名
