# F1: 共享维度与项目域 dbt 建模

**优先级**: P0  
**状态**: DONE

## 目标

在 P0 决策完成后，实现共享维度和项目域事实模型，使后续采购域、财务域可以直接复用 `ref()`。

## 当前状态

2026-05-30 task `46` 已完成 11 张项目域 ODS 入湖，`dts-dbt:1.10.0` 容器执行 `dbt build --select tag:xycyl-project` 结果为 `PASS=76 WARN=1 ERROR=0`。唯一 warning 是 `p_project_green.project_id` 对 `xycyl_dim_project` 的 286 条软外键孤儿，符合 warn 策略。

2026-05-31 已补齐 adminweb `ProjectSummaryMapper.listPage` 对账字段，`xycyl_ads_project_overview` 与 adminweb 当前运营口径 7 项指标全部 `PASS`，最大误差 `0.0000%`。

P0 口径未 RESOLVED 前，本 feature 只声明为 dbt baseline 完成，不把 rent/cost 乘法或组数定义晋升为最终业务口径。

## 当前交付

已补齐 DTS 平台导入包 `assets/xycyl-project-dbt-model.zip`，包含 project 域 sources、STG、共享维度、DWD/DWS/ADS 首批模型。2026-05-30 入数后本地 DTS PostgreSQL 已通过 `dbt build --select tag:xycyl-project`。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | project sources 与 stg 模型 | P0 | DONE | F0 |
| T02 | 共享维度与状态维度 | P0 | DONE | T01 |
| T03 | 项目实摆和摆位调整 DWD/DWS | P0 | DONE | T02 |
| T04 | 项目域 ADS 首批模型 | P1 | DONE | T03 |

## 完成标准

- [x] dbt sources 使用 `schema: public` + `identifier: ods_ptr_mysql_*`。
- [x] `xycyl_dim_project` / `xycyl_dim_customer` / `xycyl_dim_position` / `xycyl_dim_goods` / `xycyl_dim_contract` 可被后续域 ref。
- [x] DWD 透传软外键孤儿标记，relationships 测试为 warn。
- [x] ADS 与 adminweb 对账误差满足 P0 门槛。
