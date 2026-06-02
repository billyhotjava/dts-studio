# Finance Mart Catalog

Sprint-30 F4 发布三个财务回款/开票链 ADS，全部落在 PostgreSQL `public` schema，默认供 `finance` semantic-pack 与 NL2SQL 使用。

## ADS 发布面

| ADS | 粒度 | 核心口径 | 上游 |
|-----|------|----------|------|
| `public.xycyl_ads_finance_month_settlement` | 项目 × 客户 × 业务月份 | 名义租金、应收折前、折后实收、剔除销售摊入折后实收、已回款、回款进度、销售摊入金额 | `a_month_accounting` + `a_green_accounting` |
| `public.xycyl_ads_finance_invoice_progress` | 项目 × 客户 × 开票月份 × 开票业务类型 × 状态 × 税率 | 申请开票金额、实际开票金额、已回款、开票率 | `a_invoice_info` + `a_invoice_item` + `a_invoice_record` |
| `public.xycyl_ads_finance_collection` | 项目 × 收款月份 × 收款业务类型 × 是否带票 × 收款方式 × 状态 | 收款金额、收款单数、收款项目数 | `a_collection_record` + `a_collection_item` |

## 口径约束

- `回款进度 = 已回款 / 折后实收`，分母不是名义租金或应收折前。
- `source_type = '8'` 单列为 `销售摊入金额`，跨销售账单与租摆月对账汇总时不得重复计入。
- 开票业务类型使用开票链自己的来源，不复用报花主单 `biz_type`。
- 收款业务类型使用 `a_collection_item.biz_type`，不复用报花主单或开票枚举。
- 销售账单和租摆月对账属于不同结算链；含税/不含税差异需要在解释中显式提示。

## 验证

- `it/test_f4_finance_vertical_slice.sh`
- `it/evidence/20260601-local/f4-finance-vertical-slice-summary.md`
- `it/evidence/20260601-local/f4-dbt-compile-selected.txt`
- `it/sql/f4_finance_adminweb_reconciliation.sql`
