# F4/T01 库存 ADS 对账证据

**日期**: 2026-06-03
**环境**: local dts-stack / biadmin.public

## 行数对账

```text
ods_ptr_mysql_s_stock_info       11661
inventory_stg_stock_info        11661
inventory_dwd_stock_balance     11661
inventory_dws_stock_monthly     11660
inventory_ads_overview          11660
inventory_ads_low_stock_alert    9971
```

差异说明：

- STG/DWD 与 ODS 行数一致。
- DWS/ADS overview 比 DWD 少 1 行，原因是 1 条源库存记录缺 `business_time`，月度汇总按 `business_time is not null` 过滤。
- low stock alert 是 overview 的子集，规则为 `库存数量 <= 2`。

## 低库存规则核验

```text
overview_low_stock_rows = 9971
alert_rows              = 9971
missing_price_rows      = 30
min_stock_quantity      = -1.0000
max_stock_quantity      = 9906.0000
```

预警等级分布：

```text
P0-负库存    5
P1-零库存 7735
P2-低库存 2231
```

## 抽样

```text
山水盆   大兴基地       -1.0000 P0-负库存
白欧盆   大兴基地       -1.0000 P0-负库存
紫砂方盆 大兴基地       -1.0000 P0-负库存
高木架   大兴基地       -1.0000 P0-负库存
110      大兴基地        0.0000 P1-零库存
```

## 结论

库存 ADS 与源表、DWD/DWS 层行数关系一致，低库存清单完全等于 overview 中 `库存数量 <= 2` 的集合。对账通过，同时保留 30 条缺价格主数据记录和 1 条缺 `good_price_id` 记录作为后续数据质量治理信号。
