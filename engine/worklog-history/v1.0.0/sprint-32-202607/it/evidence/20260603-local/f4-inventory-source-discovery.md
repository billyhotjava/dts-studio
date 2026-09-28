# F4/T01 库存源表发现证据

**时间**: 2026-06-03
**入口**: Analytics `/api/dataset`，联邦查询入口 database_id=9 (`trino`)

## 验证目标

确认库存域不是凭页面截图猜测，而是能通过现有联邦入口发现并访问真实业务源表，为后续 dbt ADS 建模提供源表依据。

## 重跑命令

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f4_inventory_source_discovery.sh
```

结果：

```text
[F4] inventory source discovery verified: stock=14732, goods_price=6517
```

## 覆盖源表

脚本通过 `mysql.information_schema.tables` 验证以下表存在：

| 表 | 角色 |
|----|------|
| `mysql.rs_cloud_flower.s_stock_info` | 库存余额主表 |
| `mysql.rs_cloud_flower.s_stock_item` | 库存批次/出入库关联 |
| `mysql.rs_cloud_flower.t_warehousing_info` | 入库单主表 |
| `mysql.rs_cloud_flower.t_warehousing_item` | 入库单明细 |
| `mysql.rs_cloud_flower.t_ex_warehouse_info` | 出库/配送主表 |
| `mysql.rs_cloud_flower.t_ex_warehouse_item` | 出库/配送明细 |
| `mysql.rs_cloud_flower.b_goods_price` | SKU/价格主数据 |
| `mysql.rs_cloud_flower.b_goods` | 物品主数据 |

## 结论

- 库存域源表可通过 Trino 联邦入口读取。
- 首版 ADS 建模应以 `s_stock_info.good_price_id` 为 SKU 主键，关联 `b_goods_price.id` 补规格、单位和成本价。
- 源数据已有负库存样例，后续 ADS 对账必须把负库存作为质量告警，而不是静默过滤。
