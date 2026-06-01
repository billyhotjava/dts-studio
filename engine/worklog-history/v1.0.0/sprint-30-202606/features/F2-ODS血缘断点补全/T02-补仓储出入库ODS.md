# T02: 补 t_warehousing_info / t_ex_warehouse_info ODS

**优先级**: P1
**状态**: BLOCKED
**依赖**: F1

## 目标

为入库 `t_warehousing_info/item` 与出库 `t_ex_warehouse_info/item` 新增 ODS 同步,补上"回收回库 / 出库成本"的数仓血缘(后续库存域建模前置)。

## 技术设计

- 新增 DAG 同步入库/出库主子表(多 tenant)。
- ODS 建表对齐源(注意 `out_house_type`/`warehousing_type` 枚举列、`price` 在分配时为 0 的时序特性,F3 会写入对应口径护栏)。
- 本 task 只做 ODS 落地,不在本 sprint 建库存 mart(库存域顺延)。

## 影响范围

- `dts-stack/services/dts-airflow/dags/`;ODS 建表脚本

## 验证

- [ ] ODS 表产数,抽样行数与源一致
- [ ] 标注 `price=0` 分配态记录的时序过滤需求(供后续库存建模)

## 完成标准

- [ ] 入库/出库主子表进入 ODS,库存域血缘就绪(建模顺延)
