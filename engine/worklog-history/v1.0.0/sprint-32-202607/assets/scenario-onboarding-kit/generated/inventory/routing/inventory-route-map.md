# 库存路由接线

## 场景

- domain: `inventory`
- owner: `warehouse-team`
- source: `mysql.rs_cloud_flower`
- warehouse: `postgres.public`

## 五层路由

| Tier | 路由目标 | 库存接线 |
|------|----------|--------------------|
| TIER_1_PUBLISHED_INDICATOR | 已发布平台指标 | 待 ADS 稳定后发布 `inventory.stock_balance`、`inventory.low_stock_count` |
| TIER_2_MART_TEMPLATE | dbt ADS / 模板 | 目标为 `postgres.public.inventory_ads_overview`、`postgres.public.inventory_ads_low_stock_alert`，首版模型已定义，待运行态导入/构建 |
| TIER_3_ONTOLOGY_OBJECT_GRAPH | 对象图 links/signals | 库房 -> 库存现量 -> SKU/物品价格 -> 出入库流水 |
| TIER_4_GUARDRAIL_FEDERATED | Trino 联邦查询 | 当前可用源：`mysql.rs_cloud_flower.s_stock_info`、`s_stock_item`、`t_warehousing_*`、`t_ex_warehouse_*`、`b_goods_price` |
| TIER_5_DIRECT_DETAIL | 只读业务明细 | 仅用于字段画像、低库存临时查询、明细定位和 ADS 缺失时的兜底 |

## 弱路径升级规则

如果同一目标连续进入 `martCandidateSignals`，优先补：

1. ADS 模型。
2. semantic pack fewShot。
3. 资产库表格资产。
4. 平台指标。

## 已验证源表

| 表 | 用途 | 运行态证据 |
|----|------|------------|
| `mysql.rs_cloud_flower.s_stock_info` | 库存余额主表 | count = 14732 |
| `mysql.rs_cloud_flower.b_goods_price` | 物品价格/SKU 主数据 | count = 6517 |
| `mysql.rs_cloud_flower.s_stock_item` | 库存批次/出入库关联 | 字段画像已确认 |
| `mysql.rs_cloud_flower.t_warehousing_info/item` | 入库单 | 字段画像已确认 |
| `mysql.rs_cloud_flower.t_ex_warehouse_info/item` | 出库/配送单 | 字段画像已确认 |

## 首版弱路径策略

- `低库存预警`、`展示库存现状` 可先走 Tier 4 只读查询，但回答必须标记为弱路径，直到 `inventory_ads_*` 在运行态完成构建。
- 当库存问题进入 route telemetry 高频候选后，优先把 `inventory_ads_overview` 和 `inventory_ads_low_stock_alert` 导入正式 dbt 发布链路。
- 不把旧 `WH-LOW-STOCK-ALERT` fixed report 占位资产视为已认证库存资产。
