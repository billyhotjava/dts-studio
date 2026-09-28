# xycyl finance ODS table info

## Architecture Rule

Finance ADS tables are built by dbt in `dts-stack`. The application MySQL schema `rs_cloud_flower` is an ingestion source only. Copilot/BI should query DWS/ADS outputs after dbt build, not the application MySQL tables directly.

## ODS Source Mapping

| Source database | Source table | Warehouse schema | ODS table | dbt source |
| --- | --- | --- | --- | --- |
| `rs_cloud_flower` | `a_month_accounting` | `public` | `ods_ptr_mysql_a_month_accounting` | `source('xycyl_finance_ods', 'month_accounting')` |
| `rs_cloud_flower` | `a_collection_record` | `public` | `ods_ptr_mysql_a_collection_record` | `source('xycyl_finance_ods', 'collection_record')` |
| `rs_cloud_flower` | `a_sale_account` | `public` | `ods_ptr_mysql_a_sale_account` | `source('xycyl_finance_ods', 'sale_account')` |
| `rs_cloud_flower` | `t_flower_biz_info` | `public` | `ods_ptr_mysql_t_flower_biz_info` | `source('xycyl_finance_ods', 'flower_biz_info')` |
| `rs_cloud_flower` | `f_voucher` | `public` | `ods_ptr_mysql_f_voucher` | `source('xycyl_finance_ods', 'voucher')` |
| `rs_cloud_flower` | `f_voucher_item` | `public` | `ods_ptr_mysql_f_voucher_item` | `source('xycyl_finance_ods', 'voucher_item')` |

## `ods_ptr_mysql_a_month_accounting`

Grain: one row per project settlement month.

Status: 1待确认/2已对账/3开票中/4待回款/5已回款.

Amount caliber:

| Column | Type | Business meaning | dbt usage |
| --- | --- | --- | --- |
| `id` | bigint | 月结算记录 ID | DWD primary key |
| `company_name` | varchar | 公司名称 | passthrough |
| `project_id` | bigint | 项目 ID | settlement and collection matching |
| `project_name` | varchar | 项目名称 | ADS project display |
| `settlement_year` | int | 结算年份 | business month derivation |
| `settlement_month` | int | 结算月份 | business month derivation |
| `year_and_month` | varchar | 结算年月, usually `YYYYMM` or `YYYY-MM` | fallback business month derivation |
| `status` | int | 结算状态 | DWD fact field |
| `start_time` | datetime | 应收开始时间 | DWD fact field |
| `end_time` | datetime | 应收结束时间 | DWD fact field |
| `total_day` | int | 结算天数 | DWD fact field |
| `project_manage_user_id` | bigint | 项目经理 ID | DWD fact field |
| `project_manage_user_name` | varchar | 项目经理名称 | DWD fact field |
| `supervise_user_id` | bigint | 监管人员 ID | DWD fact field |
| `supervise_user_name` | varchar | 监管人员名称 | DWD fact field |
| `biz_user_id` | bigint | 业务经理 ID | DWD fact field |
| `biz_user_name` | varchar | 业务经理名称 | DWD fact field |
| `discount_rate` | double | 折扣率 | DWD fact field |
| `bit_number` | int | 结算小数点位数 | DWD fact field |
| `rent_type` | int | 收租类型: 1实摆租金/2固定租金 | DWD fact field |
| `regular_rent` | decimal | 固定总租金 | DWD fact field |
| `receivable_total_amount` | decimal | 总租金, 应收折前口径 | ADS `应收折前` |
| `net_receipt_total_amount` | decimal | 应收总金额, 实时总租金 | ADS `净收金额` |
| `folding_after_total_amount` | decimal | 折扣总租金, 折后实收口径 | ADS `折后实收` |
| `period_total_amount` | decimal | 期初总租金 | DWS support amount |
| `period_receivable_total_amount` | decimal | 期初应收租金 | ADS `本期应收` |
| `period_net_receivable_total_amount` | decimal | 期初折后总租金 | DWS support amount |
| `add_total_amount` | decimal | 本月加花总租金 | DWS support amount |
| `add_receivable_total_amount` | decimal | 本月加花应收租金, 折扣前 | ADS `加摆应收` |
| `add_net_receipt_total_amount` | decimal | 本月加花实收租金, 折扣后 | DWS support amount |
| `cut_total_amount` | decimal | 本月减花总租金 | DWS support amount |
| `cut_receivable_total_amount` | decimal | 本月减花应收租金, 折扣前 | ADS `撤摆应收` |
| `cut_net_receipt_total_amount` | decimal | 本月减花实收租金, 折扣后 | DWS support amount |
| `adjust_total_amount` | decimal | 本月调花总租金 | DWS support amount |
| `adjust_receivable_total_amount` | decimal | 本月调花应收租金, 折扣前 | ADS `调整应收` |
| `adjust_net_receipt_total_amount` | decimal | 本月调花实收租金, 折扣后 | DWS support amount |
| `sale_total_amount` | decimal | 销售总租金 | DWS support amount |
| `sale_receipt_total_amount` | decimal | 销售总金额, 折扣前 | ADS `售卖回款` |
| `sale_net_receipt_total_amount` | decimal | 销售总金额, 折扣后 | DWS support amount |
| `update_time` | datetime | 更新时间 | DWD fact field |
| `total_amount` | decimal | 实收金额 | fallback paid amount when collection data is absent |

Optional application fields: the application entity contains `department_id`, `department_name`, and `contract_id`, but the checked `rs_cloud_flower.sql` table definition does not declare them. The v1 dbt model documents this in source metadata but does not query these fields, so the build can run against the conservative ODS contract.

## `ods_ptr_mysql_a_collection_record`

Grain: one row per collection record.

Status: -1已作废/1草稿/2已确认.

| Column | Type | Business meaning | dbt usage |
| --- | --- | --- | --- |
| `id` | bigint | 收款记录 ID | DWD primary key |
| `code` | varchar | 收入流水号 | DWD fact field |
| `income_type` | int | 收入类型: 1租摆收入/2项目点销售收入/3个人销售收入/4其他 | DWD fact field |
| `project_id` | bigint | 项目 ID | settlement and collection matching |
| `project_name` | varchar | 项目名称 | ADS project display |
| `status` | int | 收款状态 | DWD fact field |
| `with_invoice` | int | 是否存在已开发票: 1开过/2未开过 | DWD fact field |
| `payment_type` | int | 收款类型: 1对公账号/2个人账号 | DWD fact field |
| `pay_mode` | varchar | 收款银行 | DWD fact field |
| `colloection_name` | varchar | 收款账号名称, source spelling preserved | DWD fact field |
| `colloection_bank_account` | varchar | 收款账号, source spelling preserved | DWD fact field |
| `pay_time` | datetime | 收款时间 | collection month derivation |
| `pay_amoney` | decimal | 收款金额, source spelling preserved | ADS `收款金额` |
| `create_by` | bigint | 创建人 ID | DWD fact field |
| `create_time` | datetime | 创建时间 | DWD fact field |
| `update_by` | bigint | 更新人 ID | DWD fact field |
| `update_time` | datetime | 更新时间 | DWD fact field |
| `remark` | varchar | 备注 | DWD fact field |
| `tenant_id` | bigint | 租户 ID | DWD fact field |

## `ods_ptr_mysql_a_sale_account`

Grain: one row per sale/gift/bad-debt account.

Status: 1待结算/2开票中/3已回款.

| Column | Type | Business meaning | dbt usage |
| --- | --- | --- | --- |
| `id` | bigint | 销售账单 ID | STG/DWD primary key |
| `biz_id` | bigint | 关联报花业务单 ID | Join to `t_flower_biz_info.id` |
| `settlement_type` | int | 结算类型 | DWD fact field |
| `status` | int | 销售账单状态 | ADS `已回款金额` 判断 |
| `finish_time` | datetime | 完成时间 | fallback business month derivation |
| `update_time` | datetime | 更新时间 | STG fact field |
| `receivable_amount` | decimal | 应收金额 | ADS `应收金额` |
| `biz_amount` | decimal | 业务金额 | ADS `业务金额` |
| `net_receipts_amount` | decimal | 实收/净收金额 | ADS `净收金额` |
| `total_cost` | decimal | 总成本 | ADS `总成本` |
| `invoice_status` | int | 开票状态 | ADS `已开票金额` 判断 |
| `invoice_no` | varchar | 发票号 | DWD fact field |
| `invoice_time` | datetime | 开票时间 | DWD fact field |

## `ods_ptr_mysql_t_flower_biz_info`

Grain: one row per flower business order.

For the sale account ADS, this table supplies project dimensions and the business month. The month caliber follows `SaleMonthlyReportMapper`: use `SUBSTRING(code, 2, 6)` and format it as `YYYY-MM`; `finish_time/apply_time` are only fallbacks when the code is unusable.

| Column | Type | Business meaning | dbt usage |
| --- | --- | --- | --- |
| `id` | bigint | 报花业务单 ID | Join key to `a_sale_account.biz_id` |
| `project_id` | bigint | 项目 ID | ADS `projectId` and application proof dimension |
| `project_name` | varchar | 项目名称 | ADS display |
| `code` | varchar | 单据编号 | primary business month derivation |
| `biz_type` | int | 业务类型: 5售花/6赠花/7坏账 | sale/gift/bad-debt scope filter |
| `apply_time` | datetime | 申请时间 | fallback business month derivation |
| `finish_time` | datetime | 完成时间 | fallback business month derivation |
| `status` | int | 报花业务状态 | DWD fact field |
| `del_flag` | varchar | 删除标记 | reserved fact field |

## `ods_ptr_mysql_f_voucher`

Grain: one row per accounting voucher header.

Business key note: `code` can repeat in the same accounting period, so dbt joins and counts vouchers by `id`/`voucher_id`, not by voucher code.

Status: 1已提交/2已审核/3已记账/-1已作废.

| Column | Type | Business meaning | dbt usage |
| --- | --- | --- | --- |
| `id` | bigint | 凭证 ID | DWD join key and voucher counting key |
| `code` | varchar | 凭证编号 | display and fallback matching only |
| `account_priod` | varchar | 会计期间, source spelling preserved | ADS `会计月份` |
| `biz_id` | bigint | 业务单据 ID | DWD fact field |
| `biz_code` | varchar | 业务单据编号 | DWD fact field |
| `biz_type` | int | 业务类型 | ADS `业务类型` |
| `description` | varchar | 凭证摘要 | DWD fact field |
| `memo` | varchar | 备注 | DWD fact field |
| `status` | int | 凭证状态 | ADS `凭证状态` |
| `total_debit_amount` | decimal | 凭证头借方合计 | reconciliation support |
| `total_credit_amount` | decimal | 凭证头贷方合计 | reconciliation support |
| `create_by` | bigint | 创建人 ID | DWD fact field |
| `create_time` | datetime | 创建时间 | DWD fact field |
| `update_by` | bigint | 更新人 ID | DWD fact field |
| `update_time` | datetime | 更新时间 | DWD fact field |
| `remark` | varchar | 备注 | DWD fact field |

## `ods_ptr_mysql_f_voucher_item`

Grain: one row per accounting voucher entry.

Only active entries with `status > 0` are included in the voucher ledger and ADS amount statistics.

| Column | Type | Business meaning | dbt usage |
| --- | --- | --- | --- |
| `id` | bigint | 凭证分录 ID | DWD primary key and entry counting key |
| `voucher_id` | bigint | 凭证 ID | DWD join key to `f_voucher.id` |
| `voucher_code` | varchar | 凭证编号 | display and diagnostics |
| `account_priod` | varchar | 会计期间, source spelling preserved | ADS `会计月份` |
| `subject_id` | bigint | 科目 ID | DWD fact field |
| `subject_code` | varchar | 科目编码 | DWD fact field |
| `description` | varchar | 分录摘要 | DWD fact field |
| `gl_account` | varchar | 总账科目 | DWD fact field |
| `detail_account` | varchar | 明细科目 | DWD fact field |
| `debit_amount` | decimal | 借方金额 | ADS `借方金额` |
| `credit_amount` | decimal | 贷方金额 | ADS `贷方金额` |
| `memo` | varchar | 备注 | DWD fact field |
| `status` | int | 分录状态 | active entry filter |
| `create_by` | bigint | 创建人 ID | DWD fact field |
| `create_time` | datetime | 创建时间 | DWD fact field |
| `update_by` | bigint | 更新人 ID | DWD fact field |
| `update_time` | datetime | 更新时间 | DWD fact field |
| `remark` | varchar | 备注 | DWD fact field |

## Runtime Status

The v1 DDL has been executed in the local dts-stack warehouse:

- `public.ods_ptr_mysql_a_month_accounting`: created
- `public.ods_ptr_mysql_a_collection_record`: created
- `public.ods_ptr_mysql_a_sale_account`: create-if-missing DDL included in this v1 package
- `public.ods_ptr_mysql_t_flower_biz_info`: create-if-missing DDL included in this v1 package
- `public.ods_ptr_mysql_f_voucher`: created and loaded by `ptr_mysql_flow`
- `public.ods_ptr_mysql_f_voucher_item`: created and loaded by `ptr_mysql_flow`

Before running a meaningful dbt build in UI, run or configure the ingestion task that lands the source tables above into `public.ods_ptr_mysql_*`.

## dbt Outputs

| Layer | Model |
| --- | --- |
| STG | `xycyl_stg_finance_month_accounting` |
| STG | `xycyl_stg_finance_collection_record` |
| STG | `xycyl_stg_finance_sale_account` |
| STG | `xycyl_stg_finance_flower_biz_info` |
| STG | `xycyl_stg_finance_voucher` |
| STG | `xycyl_stg_finance_voucher_item` |
| DWD | `xycyl_dwd_finance_month_settlement` |
| DWD | `xycyl_dwd_finance_collection` |
| DWD | `xycyl_dwd_finance_sale_account` |
| DWD | `xycyl_dwd_finance_voucher_ledger` |
| DWS | `xycyl_dws_finance_monthly_summary` |
| DWS | `xycyl_dws_finance_sale_account_summary` |
| DWS | `xycyl_dws_finance_voucher_monthly` |
| ADS | `xycyl_ads_finance_month_settlement` |
| ADS | `xycyl_ads_finance_collection` |
| ADS | `xycyl_ads_sale_account_summary` |
| ADS | `xycyl_ads_finance_voucher_monthly` |
