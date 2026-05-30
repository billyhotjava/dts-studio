# F1: 共享维度与项目域 dbt 建模

**优先级**: P0  
**状态**: BLOCKED

## 目标

在 P0 决策完成后，实现共享维度和项目域事实模型，使后续采购域、财务域可以直接复用 `ref()`。

## 阻塞

F0 已确认本地默认数据湖存在项目域核心 ODS，但 `p_project_green`、`p_position`、`p_contract`、`b_goods`、`b_goods_price` 等新建表当前仍为空。源业务数据入数和 P0 口径 RESOLVED 前，不提交 dbt 生产模型。

## 当前交付

已补齐 DTS 平台导入包 `assets/xycyl-project-dbt-model.zip`，包含 project 域 sources、STG、共享维度、DWD/DWS/ADS 首批模型。2026-05-29 本地 DTS PostgreSQL 已通过 `dbt run --select tag:xycyl-project` 和 `dbt test --select tag:xycyl-project`；由于核心事实 ODS 仍为空，模型作为 import-ready 基线，不代表事实口径已最终确认。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | project sources 与 stg 模型 | P0 | READY | F0 |
| T02 | 共享维度与状态维度 | P0 | READY | T01 |
| T03 | 项目实摆和摆位调整 DWD/DWS | P0 | READY | T02 |
| T04 | 项目域 ADS 首批模型 | P1 | READY | T03 |

## 完成标准

- [ ] dbt sources 使用 `schema: public` + `identifier: ods_ptr_mysql_*`。
- [ ] `xycyl_dim_project` / `xycyl_dim_customer` / `xycyl_dim_position` / `xycyl_dim_goods` / `xycyl_dim_contract` 可被后续域 ref。
- [ ] DWD 透传软外键孤儿标记，relationships 测试为 warn。
- [ ] ADS 与 adminweb 对账误差满足 P0 门槛。
