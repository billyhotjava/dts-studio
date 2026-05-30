# Sprint-25: 项目管理域 + 共享维度数据面

**时间**: 2026-05  
**前缀**: PJ (Project)  
**状态**: IN_PROGRESS  
**目标**: 在报花域 dbt 范式之上，先把项目、客户、合同、摆位、物品这些共享维度和项目实摆事实建成可信数据面，为后续采购域和财务域提供可复用的 `ref()` 底座。

## 背景

报花域已经验证了 `public.ods_ptr_mysql_* -> xycyl_stg_* -> xycyl_dwd_* -> xycyl_dws_* -> xycyl_ads_*` 的 5 层模式，也暴露过两个关键问题：不能假设独立 ODS schema，语义资产存在于资源目录不代表运行时已加载。Sprint-25 因此先做项目域和共享维度，不急于写最终财务 mart。

本 sprint 的入口条件是 **P0 口径决策全部 RESOLVED 才能进 P1**。在业务方没有拍板前，只允许落地 source catalog、数据画像脚本、模型清单和验收框架。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 说明 |
|----|---------|---------|--------|------|------|
| F0 | 项目域 P0 数据画像与口径决策 | 4 | P0 | BLOCKED | T01 已完成；核心 ODS 已建空表，T02 等待入数 |
| F1 | 共享维度与项目域 dbt 建模 | 4 | P0 | BLOCKED | 等 F0 数据画像 + 口径决策 |
| F2 | 项目域 NL2SQL 接入 | 3 | P1 | BLOCKED | 等 F1 ADS/DWS 产物 |
| F3 | 项目域回归与验收 | 3 | P1 | BLOCKED | 等 F1/F2 |

## 本 sprint 不做

- 不修改 `adminapi/adminweb` 业务运行时代码。
- 不在口径未拍板前提交 dbt 生产模型。
- 不临时直连 `rs_cloud_flower` 业务库作为经营汇总默认取数面；缺表时先补 `public.ods_ptr_mysql_*` 入湖。
- 不把采购域和财务域混进 Sprint-25；它们依赖本 sprint 的共享维度。

## 完成标准

- [x] P0 表清单确认：项目域源表全部能映射到 `public.ods_ptr_mysql_*` 或明确标为入湖缺口。
- [ ] `p_project_green` 快照粒度、金额口径、停用项目过滤、实摆组数定义有业务方书面结论。
- [ ] 共享维度清单落地：`xycyl_dim_customer`、`xycyl_dim_project`、`xycyl_dim_position`、`xycyl_dim_goods`、`xycyl_dim_contract`。
- [ ] 项目域事实和汇总清单落地：`xycyl_dwd_project_green_snapshot`、`xycyl_dwd_position_adjustment`、`xycyl_dws_project_green_monthly`。
- [ ] 项目域 ADS 首批覆盖：项目点实摆总览、绿植加减调换月报、状态分布、合同到期预警。
- [ ] 项目域 NL2SQL 运行时加载路径有测试覆盖，不只检查文件存在。
- [ ] IT 证据包含本地结构校验、dbt build 或 parse 结果、项目类 Golden Questions 回归结果。

## 与相邻 sprint 的关系

| Sprint | 关系 |
|---|---|
| Sprint-22 | 复用报花域 dbt 范式、口径决策表模板、`public.ods_ptr_mysql_*` 约定 |
| Sprint-24 | 复用业务对象目录和 Agent BI 报表面，不改其前端未完成工作 |
| Sprint-26 | 采购域依赖本 sprint 的 `dim_project` / `dim_goods` / `dim_position` |
| Sprint-27 | 财务域依赖本 sprint 的 `dim_project` / `dim_customer` / `dim_contract`，并复用 sprint-22 归档财务版本 |

## 输出物清单

- `assets/project-source-catalog.md`：项目域和共享维度源表清单。
- `assets/project-caliber-decisions.md`：P0 口径决策表。
- `assets/project-dbt-model-catalog.md`：dbt 模型和验收清单。
- `assets/xycyl-project-dbt-model.zip`：项目域 dbt 模型 DTS 导入包。
- `features/F0-项目域P0数据画像与口径决策/`：P0 任务入口。
- `features/F1-共享维度与项目域dbt建模/`：dbt 建模任务入口。
- `features/F2-项目域NL2SQL接入/`：智能层接入任务入口。
- `features/F3-项目域回归与验收/`：IT 和对账任务入口。
- `it/README.md` 与 `it/test_sprint25_datasurface_plan.sh`：本 sprint 验收入口。

## 2026-05-29 实施记录

- F0-T01 已完成本地入湖范围核验：原始状态只发现 `ods_ptr_mysql_p_project` / `ods_ptr_mysql_p_customer`。
- 已通过 `it/sql/project_ods_create_tables.sql` 在本地 DTS `biadmin.public` 补齐 9 张缺失 ODS 空表：`p_contract`、`p_position`、`p_floor_layer`、`p_floor_number`、`b_goods`、`b_goods_price`、`p_project_green`、`p_position_adjustment`、`p_position_adjustment_item`。
- 2026-05-29 live profile 确认 11 张 Sprint-25 必需 ODS 均为 FOUND 且有 `_dts_*`；其中新建 9 张表当前 0 行，F0-T02/F1/F2/F3 仍等待源业务数据入数和 P0 口径决策。
- 已补齐 `assets/xycyl-project-dbt-model.zip`，包含 11 sources、27 models、50 data tests；本地 DTS PostgreSQL 已通过 dbt parse/run/test，导入后仍需等待源业务数据入数再复核事实口径。
