# T01: 财务 authority / mart 面盘点与口径锁定

**优先级**: P0
**状态**: READY
**依赖**: sprint-21 完成

## 目标

为本 sprint 后续语义化工作建立"财务 authority 视图清单 + 字段口径"的单一事实来源（SOT），避免在 semantic pack 与 query template 中各自重复推断。

## 技术设计

### 1. 视图清单核实

逐个核实下列对象在 `analytics_table` / 业务库中已落地，并记录字段清单：

| 对象 | 来源类型 | 主键/粒度 | 时间字段 | 关键度量 | 关键维度 |
|---|---|---|---|---|---|
| `authority.finance.settlement_summary` | AUTHORITY_SQL | 项目 × 账期 | `account_period` | `total_rent` / `receivable_amount` / `received_amount` / `outstanding_amount` | `project_name` / `customer_name` / `settlement_status_name` |
| `authority.finance.receivable_overview` | VIEW | 全局 KPI | `as_of_date` | `total_receivable` / `total_outstanding` / `over_30d` / `over_60d` / `over_90d` | `as_of_date` |
| `authority.finance.pending_receipts_detail` | VIEW | 单据明细 | `due_date` | `outstanding_amount` | `project_name` / `customer_name` / `due_date` / `aging_bucket` |
| `authority.finance.pending_payment_approval` | VIEW | 单据明细 | `submit_time` | `payment_amount` | `payee_name` / `approval_node` / `submit_time` |
| `authority.finance.project_collection_progress` | VIEW | 项目 × 账期 | `account_period` | `receivable_amount` / `received_amount` / `collection_rate` | `project_name` / `customer_name` / `account_period` |
| `authority.finance.advance_request_status` | VIEW | 单据明细 | `apply_time` | `advance_amount` / `offset_amount` / `outstanding_advance` | `applicant_name` / `status_name` |
| `authority.finance.reimbursement_status` | VIEW | 单据明细 | `submit_time` | `expense_amount` | `applicant_name` / `status_name` |
| `authority.finance.reimbursement_list` | AUTHORITY_SQL | 单据明细 | `submit_time` | `expense_amount` | `applicant_name` / `category_name` / `status_name` |
| `authority.finance.invoice_reconciliation` | VIEW | 发票明细 | `invoice_date` | `invoice_amount` / `reconciled_amount` | `customer_name` / `invoice_no` / `status_name` |
| `mart.finance.customer_ar_rank_daily` | MART | 客户 × 日 | `snapshot_date` | `outstanding_amount` / `aging_30d` / `aging_60d` / `aging_90d` | `customer_name` / `snapshot_date` |

### 2. 字段口径锁定

对每个视图核实并文档化：

- **金额精度**：所有金额字段统一 `DECIMAL(14,2)`，下游 SUM 必须 `ROUND(..., 2)`
- **时间口径**：账期 `account_period` 用 `YYYY-MM` 字符串还是 `DATE`？回款进度的 `account_period` vs 待收明细的 `due_date` vs 报销的 `submit_time` 各自语义
- **状态口径**：哪些视图已经把 `status` 翻译成 `*_name`，哪些还要靠 `BizEnumDictionary` 翻译
- **去除软删**：核实每个视图是否已经过滤 `del_flag=0` 与 `status<>-1`
- **租户/分支**：是否需要按 `tenant_id` / `branch_id` 过滤

### 3. 与 project 域协同视图的边界

`v_monthly_settlement` 已经在 `project-fulfillment.json` 内声明，覆盖部分财务问句。本 task 必须明确：

- `v_monthly_settlement` 是 **project 域协同视图**，仅在问句以"项目"为主语时使用（如 `XX 项目本月租金`）
- 当问句以"客户 / 应收 / 欠款 / 待收 / 回款"为主语时，**统一走 `authority.finance.*`**
- 在 `assets/finance-authority-catalog.md` 中给出明确的"如果问句包含 X，去 Y 视图"决策表

### 4. 旧系统页面回链

每张视图对应到 adminweb 页面的具体路径，为口径核实提供可视化依据：

| 视图 | adminweb 页面 | adminapi Mapper |
|---|---|---|
| settlement_summary | `flower/finance/settlement/list-summary.vue` | `mapper/finance/SettlementMapper.xml` |
| pending_receipts_detail | `flower/finance/settlement/list-pending-receipts.vue` | `mapper/finance/CollectionMapper.xml` |
| pending_payment_approval | `expense/pay/list-pending-approval.vue` | `mapper/finance/PayRecordMapper.xml` |
| project_collection_progress | `flower/finance/settlement/list-collection-progress.vue` | `mapper/finance/SettlementMapper.xml` |
| advance_request_status | `expense/advance/list-advance.vue` | `mapper/finance/AdvanceInfoMapper.xml` |
| reimbursement_list | `expense/daily/list-daily.vue` | `mapper/finance/ExpenseAccountMapper.xml` |
| invoice_reconciliation | `flower/finance/invoice/list-invoice.vue` | `mapper/finance/InvoiceMapper.xml` |
| customer_ar_rank_daily | `flower/finance/dashboard/customer-ar-rank.vue` | `mapper/finance/CustomerArRankMapper.xml` |

> 注：表中 Mapper 路径以实际仓库为准，T01 完成时需校对修正。

### 5. 与 dts-stack dbt 模型的对照

本 task 同时是 F4 的输入。每张 authority/mart 视图必须明确两件事：

- **当前**：sprint-21 在 dts-copilot PG 中的物化形态（schema / 字段 / 索引）
- **未来**：在 dts-stack dbt 项目中的预期模型路径（如 `models/xycyl/ads/xycyl_ads_finance_settlement_summary.sql`）

`assets/finance-authority-catalog.md` 中需新增"dbt 模型映射"列，包含：

- 模型相对路径
- 物化策略（`table` / `incremental` / `view`）
- 上游依赖（dwd / dws / 其他 ods source）
- 预期 tag（统一加 `xycyl` + `xycyl-finance` + 视图主题如 `settlement` / `ar`）

具体规范见 `assets/dts-stack-dbt-conventions.md`（F4 产出）。

## 影响范围

- `worklog/v1.0.0/sprint-22-202605/assets/finance-authority-catalog.md` —— 新增（本 task 的核心交付物，含 dbt 模型映射列）
- 不动任何代码或数据库

## 验证

- [ ] 10 个视图在 `analytics_table` 中都有记录，且 `target_object` 字段值与本文档一致
- [ ] 每个视图的字段清单与 `DESCRIBE/INFORMATION_SCHEMA` 输出 1:1 对齐
- [ ] `assets/finance-authority-catalog.md` 中"问句决策表"覆盖 sprint README 列出的 8 条样本问句

## 完成标准

- [ ] 财务域 authority 视图清单完整、字段口径锁定、状态/精度/时间口径明确
- [ ] `v_monthly_settlement` 与 `authority.finance.*` 的协同边界写入文档
- [ ] T02 ~ T04 可以在不再做"现场考古"的前提下基于本 task 输出物推进
