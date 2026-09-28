# F4/T01 库存 dbt ADS 模型定义证据

**时间**: 2026-06-03
**范围**: `generated/inventory/dbt/models`

## 背景

F4 的库存域接入此前已完成源表发现、语义包、弱路径模板和多场景隔离，但 dbt 生成物仍是 `placeholder`：

- `inventory_stg_placeholder`
- `inventory_dwd_placeholder`
- `inventory_dws_monthly`
- `inventory_ads_overview` 占位查询

这会让库存域只能停留在 Trino 弱路径，无法证明场景套件能沉淀出 T2 mart/ADS 查询面。

## 本次补齐

将库存生成实例改为首版真实模型链：

| 层 | 模型 | 说明 |
|----|------|------|
| STG | `inventory_stg_stock_info` | 类型化 `s_stock_info`，过滤逻辑删除，保留库房、SKU、数量、成本和状态 |
| STG | `inventory_stg_goods_price` | 类型化 `b_goods_price`，补充物品规格、属性、单位和成本价 |
| DWD | `inventory_dwd_stock_balance` | 按 `good_price_id` 归一库存余额和有效成本，标记负库存/低库存/零库存/正常 |
| DWS | `inventory_dws_stock_monthly` | 按月份、库房、SKU、健康状态汇总库存数量和成本 |
| ADS | `inventory_ads_overview` | 库存现量表格资产查询面 |
| ADS | `inventory_ads_low_stock_alert` | 低库存/零库存/负库存预警清单 |

`schema.yml` 已补齐 owner、classification、domain、data_layer、governed_package 和字段级 `expected_data_type`。

## 验证

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f4_inventory_dbt_ads_models.sh
```

结果：

```text
[F4] inventory dbt ADS model artifacts verified
```

## 当前边界

本证据证明库存 ADS **模型定义** 已完成，不等同于运行态已发布。仍需后续：

1. 将 `generated/inventory/dbt/models/**` 合入正式 dbt 项目或发布包。
2. 按环境把 `inventory_stock_info_relation`、`inventory_goods_price_relation` 指向 ODS 或联邦源。
3. 执行 dbt build，生成 `postgres.public.inventory_ads_overview` 与 `postgres.public.inventory_ads_low_stock_alert`。
4. 用源表和 ADS 结果做数量、低库存、负库存、缺 SKU 主数据对账。

因此 F4/T01 已从“placeholder”推进到“ADS 模型定义完成”，但 F4 仍保持 `IN_PROGRESS`，不能标记 DONE。
