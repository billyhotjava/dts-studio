# RC-02 业务库扫描与页面-数据库对照

**状态**: DONE
**目标**: 扫描测试业务库 `rs_cloud_flower`，并结合 `adminapi` 控制器边界确认现网页面对应的真实业务表族，为固定报表模板和受控取数面提供依据。

## 数据库扫描结论

- 业务库物理表：`163`
- 业务库视图：`0`

这说明：

- 当前业务库是典型 OLTP 表结构
- 没有现成的报表视图层
- 固定报表快路径不能只依赖“找已有视图”

## 主要表族

### 财务

- `f_settlement`
- `f_settlement_item`
- `f_advance_info`
- `f_expense_account_info`
- `f_expense_account_item`
- `f_pay_record`
- `f_bank_statement`
- `a_invoice_info`
- `a_invoice_item`
- `a_green_accounting`
- `a_month_accounting`

### 采购 / 仓库

- `i_purchase_info`
- `i_purchase_item`
- `t_plan_purchase_info`
- `t_plan_purchase_item`
- `t_delivery_info`
- `t_delivery_item`
- `t_warehousing_info`
- `t_warehousing_item`
- `t_allocation`
- `t_allocation_item`
- `t_back_good_info`
- `t_back_good_item`
- `s_stock_info`
- `s_stock_item`
- `s_storehouse_info`

### 报花 / 任务 / 项目

- `t_flower_biz_info`
- `t_flower_biz_item`
- `t_daily_task_info`
- `t_daily_task_item`
- `p_project`
- `p_project_green`
- `p_project_green_item`
- `p_position`

## 页面到表族映射

## adminapi 模块归属

`rs-modules/rs-flowers-base` 当前已具备比较清晰的业务域控制器：

- `project/controller`: `19`
- `flowerbiz/controller`: `18`
- `purchase/controller`: `6`
- `task/controller`: `5`
- `tasknew/controller`: `9`
- `operate/controller`: `7`
- `pendulum/controller`: `6`

### 典型控制器

- 采购：
  - `PurchaseInfoController`
  - `PlanPurchaseInfoController`
  - `DeliveryInfoController`
  - `PurchaseRejectController`

- 项目/报花/任务：
  - `ProjectController`
  - `FlowerBizInfoController`
  - `TaskInfoController`
  - `DailTaskInfoController`
  - `ExecuteTaskController`

- 运营/财务：
  - `MonthAccountController`
  - `InvoiceInfoController`
  - `CollectionController`
  - `SaleAccountController`

### 采购管理

- `采购汇总` / `采购明细`
  - 页面 API: `@/api/flower/purchase/purchase`
  - 主要表族: `i_purchase_info`, `i_purchase_item`

- `采购计划明细`
  - 页面 API: `@/api/flower/purchase/planPurchase`
  - 主要表族: `t_plan_purchase_info`, `t_plan_purchase_item`

- `配送记录`
  - 页面 API: `@/api/flower/purchase/deliveryInfo`
  - 主要表族: `t_delivery_info`, `t_delivery_item`

### 仓库管理

- `库存`
  - 主要表族: `s_stock_info`, `s_stock_item`, `s_storehouse_info`

- `入库管理`
  - 主要表族: `t_warehousing_info`, `t_warehousing_item`

- `出库管理`
  - 主要表族: `t_ex_warehouse_info`, `t_ex_warehouse_item`

- `调拨管理`
  - 主要表族: `t_allocation`, `t_allocation_item`

- `退货管理`
  - 主要表族: `t_back_good_info`, `t_back_good_item`

### 财务 / 运营

- `财务结算`
  - 页面 API: `@/api/flower/finance/settlement`
  - 主要表族: `f_settlement`, `f_settlement_item`

- `开票管理`
  - 页面 API: `@/api/flower/operate/invoice`
  - 主要表族: `a_invoice_info`, `a_invoice_item`

- `日常报销`
  - 页面 API: `@/api/flower/finance/expense`
  - 主要表族: `f_expense_account_info`, `f_expense_account_item`

- `预支申请`
  - 页面 API: `@/api/flower/finance/advance`
  - 主要表族: `f_advance_info`

- `支出管理`
  - 页面 API: `@/api/flower/finance/payRecord`
  - 主要表族: `f_pay_record`

## 结论

页面、后端模块和数据库之间是可直接对上的，但当前对报表产品化最不利的一点是：业务库无视图层。因此固定报表实施时必须额外设计受控取数面，不能把所有查询都直接压在基础交易表上。
