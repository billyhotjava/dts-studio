# 财务域 Authority / Mart 视图清单（基线）

> 本文件作为 sprint-22 的单一事实来源（SOT），由 F1-T01 在实施时根据生产库实际情况校对修正。
>
> 校对前：列表为根据 sprint-19 ~ sprint-21 changelog 推断的初始模板。
> 校对后：所有字段必须与 `INFORMATION_SCHEMA.COLUMNS` 输出 1:1 对齐。
>
> **双轨说明（sprint-22 引入）**：每张视图同时存在两个产出形态——
> - 短期：sprint-19 ~ sprint-21 在 dts-copilot PG 中物化的 `authority.finance.*` / `mart.finance.*`
> - 长期：dts-stack dbt 项目 `models/xycyl/ads/*.sql` 产出（F4-T02 实施）
> 见末尾"dbt 模型映射"小节。

## 视图清单

### 1. authority.finance.settlement_summary

- **来源类型**：AUTHORITY_SQL（落地于 `0046_promote_finance_settlement_summary_fixed_report.xml`）
- **粒度**：项目 × 账期
- **adminweb 页面**：`flower/finance/settlement/list-summary.vue`
- **adminapi Mapper**：`mapper/finance/SettlementMapper.xml`（待校对）
- **关键字段**：
  - `project_name` — 项目点
  - `customer_name` — 客户
  - `account_period` — 账期（`YYYY-MM` 字符串）
  - `total_rent` — 月租金（DECIMAL(14,2)）
  - `receivable_amount` — 应收
  - `received_amount` — 已收
  - `outstanding_amount` — 待收 / 欠款
  - `settlement_status_name` — 结算状态（中文）
- **过滤**：`del_flag = 0` AND `status <> -1`（视图内已做）
- **同义词词条**：应收 / 已收 / 待收 / 欠款 / 月租 / 账期

### 2. authority.finance.receivable_overview

- **来源类型**：VIEW
- **粒度**：全局 KPI（每日一行，按 `as_of_date`）
- **adminweb 页面**：`flower/finance/dashboard/ar-overview.vue`（待校对）
- **关键字段**：
  - `as_of_date` — 截止日
  - `total_receivable` — 总应收
  - `total_outstanding` — 总欠款
  - `over_30d` — 30 天内
  - `over_60d` — 30-60 天
  - `over_90d` — 60-90 天 / 90 天以上（具体口径校对）
- **同义词词条**：应收概览 / KPI / 账龄 / 账龄分桶

### 3. authority.finance.pending_receipts_detail

- **来源类型**：VIEW
- **粒度**：单据明细
- **adminweb 页面**：`flower/finance/settlement/list-pending-receipts.vue`
- **adminapi Mapper**：`mapper/finance/CollectionMapper.xml`（待校对）
- **关键字段**：
  - `project_name` / `customer_name` / `due_date` / `aging_bucket`
  - `outstanding_amount`
- **过滤**：仅 `outstanding_amount > 0`
- **同义词词条**：待收明细 / 逾期 / 账龄

### 4. authority.finance.pending_payment_approval

- **来源类型**：VIEW
- **粒度**：单据明细
- **adminweb 页面**：`expense/pay/list-pending-approval.vue`（待校对）
- **关键字段**：
  - `payee_name` / `approval_node` / `submit_time` / `payment_amount`
- **过滤**：仅 Flowable 流程未结束的单
- **同义词词条**：待付款 / 审批 / 流转节点

### 5. authority.finance.project_collection_progress

- **来源类型**：VIEW
- **粒度**：项目 × 账期
- **adminweb 页面**：`flower/finance/settlement/list-collection-progress.vue`（待校对）
- **关键字段**：
  - `project_name` / `customer_name` / `account_period`
  - `receivable_amount` / `received_amount` / `collection_rate`
- **特殊说明**：`collection_rate` 是小数（0.85），展示要 × 100
- **同义词词条**：回款 / 回款率 / 回款进度

### 6. authority.finance.advance_request_status

- **来源类型**：VIEW
- **粒度**：单据明细
- **adminweb 页面**：`expense/advance/list-advance.vue`
- **adminapi Mapper**：`mapper/finance/AdvanceInfoMapper.xml`
- **关键字段**：
  - `applicant_name` / `status_name` / `apply_time`
  - `advance_amount` / `offset_amount` / `outstanding_advance`
- **同义词词条**：预支 / 核销 / 销账 / 未核销

### 7. authority.finance.reimbursement_status

- **来源类型**：VIEW
- **粒度**：审批状态汇总
- **关键字段**：
  - `applicant_name` / `status_name` / `expense_amount`

### 8. authority.finance.reimbursement_list

- **来源类型**：AUTHORITY_SQL（落地于 `0052_promote_batch2_reports.xml`）
- **粒度**：单据明细
- **adminweb 页面**：`expense/daily/list-daily.vue`
- **adminapi Mapper**：`mapper/finance/ExpenseAccountMapper.xml`
- **关键字段**：
  - `applicant_name` / `category_name` / `status_name` / `submit_time`
  - `expense_amount`
- **同义词词条**：报销 / 报销金额 / 费用

### 9. authority.finance.invoice_reconciliation

- **来源类型**：VIEW
- **粒度**：发票明细
- **adminweb 页面**：`flower/finance/invoice/list-invoice.vue`（待校对）
- **adminapi Mapper**：`mapper/finance/InvoiceMapper.xml`
- **关键字段**：
  - `customer_name` / `invoice_no` / `status_name` / `invoice_date`
  - `invoice_amount` / `reconciled_amount`
- **同义词词条**：发票 / 开票 / 对账 / 开票金额

### 10. mart.finance.customer_ar_rank_daily

- **来源类型**：MART
- **粒度**：客户 × 日（每日快照）
- **ELT job**：`EltSyncScheduler` + 财务 SyncJob（待校对实际类名）
- **关键字段**：
  - `customer_name` / `snapshot_date`
  - `outstanding_amount` / `aging_30d` / `aging_60d` / `aging_90d`
- **特殊说明**：每日快照，问"客户欠款排行"必读最新 `snapshot_date`
- **同义词词条**：客户欠款 / 欠款排行 / 账龄分桶

## 与 project 域协同视图

### v_monthly_settlement（在 project-fulfillment.json 内）

- **粒度**：项目 × 账期
- **使用边界**：
  - 问句以"项目"为主语 + 关心"租金 / 摆位 / 月份租金" → 用 `v_monthly_settlement`
  - 问句以"客户 / 应收 / 欠款 / 待收 / 回款"为主语 → 用 `authority.finance.*`
- **不要**让 finance 域 routing rule 抢路 `v_monthly_settlement`，反之亦然

## 问句决策表

| 问句关键词 | 主视图 | 备注 |
|---|---|---|
| 客户欠款 + 排行 / 前 N | `mart.finance.customer_ar_rank_daily` | 必读最新快照 |
| 客户 X 欠款 / 还欠多少 | `mart.finance.customer_ar_rank_daily` | + customer_name LIKE |
| 项目应收 / 项目欠款 | `authority.finance.settlement_summary` | + GROUP BY project_name |
| 项目租金 / 项目月租 | `v_monthly_settlement`（project 域） | 不走 finance |
| 项目回款进度 / 回款率 | `authority.finance.project_collection_progress` | |
| 待收款明细 / 账龄 | `authority.finance.pending_receipts_detail` | |
| 待付款审批 / 超过 N 天 | `authority.finance.pending_payment_approval` | + DATEDIFF |
| 预支 / 核销 / 销账 | `authority.finance.advance_request_status` | |
| 报销 / 张三报销单 | `authority.finance.reimbursement_list` | |
| 开票金额 / 发票对账 | `authority.finance.invoice_reconciliation` | |
| 应收概览 / 总应收 | `authority.finance.receivable_overview` | |
| 月度结算总览 | `authority.finance.settlement_summary` | |

## 字段口径一览（金额 / 时间 / 状态）

| 类别 | 规则 |
|---|---|
| 金额精度 | 所有 `*_amount` 字段 `DECIMAL(14,2)`，下游 SUM 必须 `ROUND(..., 2)` |
| 账期 | `account_period` 字符串 `YYYY-MM`，**不**用 `DATE_FORMAT(create_time,'%Y-%m')` |
| 截止日 / 申请日 / 提交日 | `as_of_date` / `apply_time` / `submit_time`，类型为 `DATE` 或 `DATETIME`，避免混用 |
| 回款率 | `collection_rate` 小数（0~1），展示百分比 `ROUND(... * 100, 1)` |
| 状态 | 视图层已翻译为 `*_name`（中文）。`BizEnumDictionary` 仅用于反向解析问句中提到的中文标签 |
| 软删 | 所有视图已过滤 `del_flag=0` AND `status<>-1`，下游不需要重复加 |

## 反例（禁止重复出现）

| 错误 | 正确 |
|---|---|
| `SUM(outstanding_amount) FROM settlement_summary GROUP BY customer_name` 算客户欠款 | 直接读 `mart.finance.customer_ar_rank_daily` |
| `JOIN act_ru_task` 拿审批节点 | 读 `authority.finance.pending_payment_approval.approval_node` |
| `t_advance_info.status=2` 判断已发放 | 读 `authority.finance.advance_request_status.status_name='已发放'` |
| `v_project_overview` 找应收（没有应收字段） | 读 `authority.finance.settlement_summary` |
| `t_invoice` 自拼且不过滤作废 | 读 `authority.finance.invoice_reconciliation` |
| `v_monthly_settlement` 算回款率（无 `collection_rate` 列） | 读 `authority.finance.project_collection_progress` |

## dbt 模型映射（F4 长期产出）

下表为 F4-T02 的输入。每张 sprint-21 落地的视图都对应一个 dbt 模型，命名 / 分层 / tag 严格遵循 `dts-stack-dbt-conventions.md`。

| sprint-21 视图（短期） | dts-stack dbt 模型（长期） | 物化策略 | 上游依赖 | 主要 tags |
|---|---|---|---|---|
| `authority.finance.settlement_summary` | `models/xycyl/ads/xycyl_ads_finance_settlement_summary.sql` | `table` | `xycyl_dws_finance_project_monthly` + `xycyl_dwd_finance_settlement` | `xycyl, xycyl-finance, settlement, ads` |
| `authority.finance.receivable_overview` | `models/xycyl/ads/xycyl_ads_finance_receivable_overview.sql` | `table`（每日刷新） | `xycyl_dws_finance_customer_monthly` | `xycyl, xycyl-finance, ar, kpi, ads` |
| `authority.finance.pending_receipts_detail` | `models/xycyl/ads/xycyl_ads_finance_pending_receipts_detail.sql` | `view` | `xycyl_dwd_finance_settlement` + `xycyl_dwd_finance_collection` | `xycyl, xycyl-finance, ar, detail, ads` |
| `authority.finance.pending_payment_approval` | `models/xycyl/ads/xycyl_ads_finance_pending_payment_approval.sql` | `view` | `xycyl_dwd_finance_payment` | `xycyl, xycyl-finance, payment, ads` |
| `authority.finance.project_collection_progress` | `models/xycyl/ads/xycyl_ads_finance_project_collection_progress.sql` | `table` | `xycyl_dws_finance_project_monthly` | `xycyl, xycyl-finance, collection, ads` |
| `authority.finance.advance_request_status` | `models/xycyl/ads/xycyl_ads_finance_advance_request_status.sql` | `view` | `xycyl_dwd_finance_advance` | `xycyl, xycyl-finance, advance, ads` |
| `authority.finance.reimbursement_status` | `models/xycyl/ads/xycyl_ads_finance_reimbursement_status.sql` | `view` | `xycyl_dwd_finance_reimbursement` | `xycyl, xycyl-finance, reimbursement, ads` |
| `authority.finance.reimbursement_list` | `models/xycyl/ads/xycyl_ads_finance_reimbursement_list.sql` | `view` | `xycyl_dwd_finance_reimbursement` | `xycyl, xycyl-finance, reimbursement, detail, ads` |
| `authority.finance.invoice_reconciliation` | `models/xycyl/ads/xycyl_ads_finance_invoice_reconciliation.sql` | `view` | `xycyl_dwd_finance_invoice` | `xycyl, xycyl-finance, invoice, ads` |
| `mart.finance.customer_ar_rank_daily` | `models/xycyl/ads/xycyl_ads_finance_customer_ar_rank_daily.sql` | `incremental`（按 `snapshot_date`） | `xycyl_dws_finance_customer_monthly` | `xycyl, xycyl-finance, ar, kpi, snapshot, ads` |

### 中间层（dwd / dws）需要建的模型

| 模型 | 层 | 来源 | 备注 |
|---|---|---|---|
| `xycyl_dwd_finance_settlement` | dwd | `xycyl_stg_settlement_info` + 项目维度 | 含状态翻译 |
| `xycyl_dwd_finance_collection` | dwd | `xycyl_stg_collection_record` | 回款明细 |
| `xycyl_dwd_finance_payment` | dwd | `xycyl_stg_pay_record` | 含 Flowable 节点信息（如已入湖） |
| `xycyl_dwd_finance_advance` | dwd | `xycyl_stg_advance_info` | 预支单 |
| `xycyl_dwd_finance_reimbursement` | dwd | `xycyl_stg_expense_account_info` | 报销单 |
| `xycyl_dwd_finance_invoice` | dwd | `xycyl_stg_invoice_info` | 发票主表 |
| `xycyl_dws_finance_project_monthly` | dws | dwd_settlement + dwd_collection | 项目 × 月汇总 |
| `xycyl_dws_finance_customer_monthly` | dws | dwd_settlement + dwd_collection | 客户 × 月汇总（账龄从这里派生） |

### 维度表（dim_*_alias）

| 模型 | 状态枚举类型 | 用途 |
|---|---|---|
| `xycyl_dim_finance_settlement_status_alias` | `settlement.status` | 0/1/2/3/4/9 → 中文 |
| `xycyl_dim_finance_payment_status_alias` | `payment.status` | 0/1/2/3/9 → 中文 |
| `xycyl_dim_finance_pay_type_alias` | `payment.pay_type` | 1-5 → 中文 |
| `xycyl_dim_finance_advance_status_alias` | `advance.status` | 0/1/2/3/4/9 → 中文 |
| `xycyl_dim_finance_reimbursement_status_alias` | `reimbursement.status` | 0/1/2/3/9 → 中文 |
| `xycyl_dim_finance_reimbursement_category_alias` | `reimbursement.category` | 1-9 → 中文 |
| `xycyl_dim_finance_invoice_status_alias` | `invoice.status` | 0/1/2/3/9 → 中文 |
| `xycyl_dim_finance_aging_bucket` | `ar_aging_bucket` | 0-30 / 30-60 / 60-90 / 90+ |

> 这些维度表的具体值见 `features/F1-财务域语义资产基线/T04-财务枚举字典扩充.md`。F4 实施时，T04 的字典数据（`BizEnumDictionary` INSERT）与本表（dbt seed 或 hard-code values）保持完全一致。

### 双轨切换映射（datasource 切换时用）

dts-copilot 在 datasource = "花卉财务 Mart (dbt)" 时，query template 中视图名按以下映射替换：

```
authority.finance.<X>     →  xycyl_ads.xycyl_ads_finance_<X>
mart.finance.<X>          →  xycyl_ads.xycyl_ads_finance_<X>
```

切换由 `Nl2SqlService` 在路由阶段根据当前 datasource 的 tag（`dbt-output` vs 默认）选择，详见 F4-T05。
