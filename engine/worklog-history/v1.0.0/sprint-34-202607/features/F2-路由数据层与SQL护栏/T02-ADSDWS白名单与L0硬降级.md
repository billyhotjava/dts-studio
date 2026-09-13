# T02: ADS/DWS 白名单与 L0 硬降级

**优先级**: P0
**状态**: DONE
**依赖**: T01

## 目标

阻止错误数据层被误标高可信，特别是 L0 profile、应用 MySQL 直连、未登记 ADS。

## 技术设计

- 建立 allowed relation resolver：指标、ADS、DWS、受控联邦表分级。
- `public.xycyl_ads_*`、平台指标等可进入中高可信。
- `mysql.rs_cloud_flower.*`、`L0_BUSINESS_OBJECT_PROFILE` 默认弱路径。
- 未登记 relation 一律要求 `warnings` 并降级。

## 影响范围

- SQL execution gate。
- query template seed/test。
- accuracy scorer。

## 验证

- [x] `public.xycyl_ads_finance_voucher_monthly` 可进入 HIGH 候选。
- [x] `mysql.rs_cloud_flower.f_voucher` 返回 `UNTRUSTED`。
- [x] L0 profile 返回 `UNTRUSTED`，不返回统计结论。

## 完成标准

- [x] 数据层判定可测试，且原因展示清楚。
