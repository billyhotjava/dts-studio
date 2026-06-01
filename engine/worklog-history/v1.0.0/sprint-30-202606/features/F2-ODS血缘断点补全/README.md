# F2: ODS 血缘断点补全

**优先级**: P0
**状态**: BLOCKED（依赖 F1 完成后再动 ELT 配置）

## 目标

补齐被 mart 设计引用却无 Airflow ODS 同步的源表,并把财务垂直切片(F4)所需源表纳入 ODS,使下游 dbt 模型有数据可建,血缘断点清零。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 补 t_change_info ODS 同步 | P0 | BLOCKED | F1 |
| T02 | 补 t_warehousing_info / t_ex_warehouse_info ODS | P1 | BLOCKED | F1 |
| T03 | 补财务垂直切片源表 ODS | P0 | BLOCKED | F1 |
| T04 | ODS 覆盖校验与缺口闭环 | P0 | BLOCKED | T01,T02,T03 |

## 完成标准

- [ ] mart 设计中 `xycyl_*_change_log` 等引用的源表均有 ODS 同步
- [ ] 财务回款/开票链所需 a_* 表全部进 ODS
- [ ] 产出"mart/语义对象引用源表 ↔ ODS 同步"对照表,缺口为零
