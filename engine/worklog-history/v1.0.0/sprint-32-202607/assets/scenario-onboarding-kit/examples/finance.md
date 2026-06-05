# 财务域套件样例

| 六要素 | 当前落点 |
|--------|----------|
| CatalogDomain | `finance`，财务结算 / 开票 / 收款分析域 |
| dbt namespace | `xycyl_ads_finance_*` 为主要 ADS 资产 |
| semantic pack | `dts-copilot-ai/src/main/resources/semantic-packs/finance.json` |
| Trino catalog | `postgres.public.*` for finance ADS；必要时 `mysql.rs_cloud_flower.*` drilldown |
| glossary | 月结算、开票进度、收款明细三类核心口径 |
| routing | Tier 2 优先命中 finance ADS；源表对账和发票明细按弱路径处理 |

## 已认证 ADS

- `public.xycyl_ads_finance_month_settlement`
- `public.xycyl_ads_finance_invoice_progress`
- `public.xycyl_ads_finance_collection`

## 口径护栏

- 月结算金额、开票金额、收款金额是三条链路，不能用一个金额字段混答。
- `source_type=8` 等特殊来源需要独立解释，避免把销售摊入误认为普通租金。
- 需要和 adminweb 财务页面对账后，才能把源表聚合提升为已认证报表资产。

## 对 F4 的启发

库存域也需要先定义核心口径铁律，例如库存成本按 SKU/good_price_id 而不是物品名称粗聚合；否则 Agent 容易生成看似正确但业务不可用的报表。
