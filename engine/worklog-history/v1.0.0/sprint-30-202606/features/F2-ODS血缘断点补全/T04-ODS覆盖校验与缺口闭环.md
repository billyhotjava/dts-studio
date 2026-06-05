# T04: ODS 覆盖校验与缺口闭环

**优先级**: P0
**状态**: DONE
**依赖**: T01,T02,T03

## 目标

产出"mart/语义对象引用源表 ↔ ODS 同步任务"对照表,确认所有下游引用的源表都有 ODS,缺口清零。

## 技术设计

- 扫描各 semantic-pack(flowerbiz/procurement/field-operations)对象引用的视图/源表。
- 扫描 dbt 模型 stg 层引用的 `ods_ptr_mysql_*`。
- 比对 Airflow DAG 实际同步的表集合,输出三方对照表与缺口清单。
- 已知断点(t_change_info、t_warehousing_info)在 T01/T02 修复后应消失;校验闭环。

## 影响范围

- `it/` 校验脚本;对照表文档(assets)

## 验证

- [x] 对照表生成,缺口清单为零
- [x] 脚本可重跑

## 完成标准

- [x] 血缘断点闭环,缺口清单清零
