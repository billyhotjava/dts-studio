# T03: 收款明细 ads

**优先级**: P1
**状态**: DONE
**依赖**: T01

## 目标

建收款模型,把 `a_collection_record` / `a_collection_item` 规范成回款明细与回款来源口径,与月对账/销售账单回款闭环对齐。

## 技术设计

- dwd:展开 `a_collection_item.biz_type` 与 `with_invoice`(1关联发票/2关联销售账单),关联到月对账(biz_type=1)或销售账单。
- ads:`xycyl_ads_finance_collection`——按项目/月/收款方式输出:回款金额、关联账单、是否带票。
- 与 T01 的"已回款"口径交叉校验(月对账 total_amount 累加 ↔ 收款明细汇总),误差登记。

## 影响范围

- dbt 模型(finance);ads 视图;dbt test

## 验证

- [x] 收款汇总 ADS 与月对账/开票对账 SQL 已成文
- [x] with_invoice 两类来源单列,不混

## 完成标准

- [x] 收款明细 ads 落地,与回款进度闭环一致

## 证据

- `dts-stack/services/dts-dbt/models/ads/model/xycyl_ads_finance_collection.sql`
- `it/sql/f4_finance_adminweb_reconciliation.sql`
