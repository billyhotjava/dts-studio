# F1: 共享维度与项目域 dbt 建模

**优先级**: P0  
**状态**: BLOCKED

## 目标

在 P0 决策完成后，实现共享维度和项目域事实模型，使后续采购域、财务域可以直接复用 `ref()`。

## 阻塞

F0 已确认本地默认数据湖缺少项目域核心 ODS：`p_project_green`、`p_position`、`p_contract`、`b_goods`、`b_goods_price`。在这些表入湖和 P0 口径 RESOLVED 前，不提交 dbt 生产模型。

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
