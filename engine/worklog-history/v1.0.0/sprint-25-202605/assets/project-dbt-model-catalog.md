# 项目域 dbt 模型目录（Sprint-25）

> 当前已补齐可导入 DTS 平台的 dbt 包：`assets/xycyl-project-dbt-model.zip`。该包能在本地 DTS PostgreSQL 完成 `dbt run/test`，但 9 张新建 ODS 仍为空表，P0 事实口径仍需入数后复核。

## 交付包

| artifact | 用途 | 状态 |
|---|---|---|
| `assets/xycyl-project-dbt-model/` | dbt 项目源码 | READY |
| `assets/xycyl-project-dbt-model.zip` | DTS 平台导入包 | READY |
| `it/test_project_dbt_package.sh` | 包结构与 parse 验证 | PASS |
| `it/evidence/20260529-local/project-dbt-package.md` | dbt parse/run/test 证据 | PASS |

## Sources

`models/xycyl_project_sources.yml`：

| source name | schema | identifier |
|---|---|---|
| `p_project` | `public` | `ods_ptr_mysql_p_project` |
| `p_customer` | `public` | `ods_ptr_mysql_p_customer` |
| `p_contract` | `public` | `ods_ptr_mysql_p_contract` |
| `p_position` | `public` | `ods_ptr_mysql_p_position` |
| `p_floor_layer` | `public` | `ods_ptr_mysql_p_floor_layer` |
| `p_floor_number` | `public` | `ods_ptr_mysql_p_floor_number` |
| `b_goods` | `public` | `ods_ptr_mysql_b_goods` |
| `b_goods_price` | `public` | `ods_ptr_mysql_b_goods_price` |
| `p_project_green` | `public` | `ods_ptr_mysql_p_project_green` |
| `p_position_adjustment` | `public` | `ods_ptr_mysql_p_position_adjustment` |
| `p_position_adjustment_item` | `public` | `ods_ptr_mysql_p_position_adjustment_item` |

## STG

| 模型 | 物化 | 说明 |
|---|---|---|
| `xycyl_stg_project_project` | view | 过滤软删项目，保留 status/type 原始值；避免覆盖 Sprint-22 `xycyl_stg_project` |
| `xycyl_stg_project_customer` | view | 客户主数据标准化；避免覆盖 Sprint-22 `xycyl_stg_customer` |
| `xycyl_stg_project_contract` | view | 合同状态、结算方式原始字段标准化 |
| `xycyl_stg_project_position` | view | 摆位、楼层、楼号关联键标准化 |
| `xycyl_stg_project_floor_layer` | view | 楼层源表标准化 |
| `xycyl_stg_project_floor_number` | view | 楼号源表标准化 |
| `xycyl_stg_project_goods` | view | 物品主数据标准化 |
| `xycyl_stg_project_goods_price` | view | 物品价格与成本字段标准化 |
| `xycyl_stg_project_green` | view | 实摆事实，保留 parent/import/status/lock 字段 |
| `xycyl_stg_project_position_adjustment` | view | 摆位调整主表 |
| `xycyl_stg_project_position_adjustment_item` | view | 摆位调整明细 |

## 共享维度

| 模型 | 物化 | 上游 | 用途 |
|---|---|---|---|
| `xycyl_dim_customer` | table | `xycyl_stg_project_customer` | 采购、财务、项目共用客户轴 |
| `xycyl_dim_contract` | table | `xycyl_stg_project_contract` + `xycyl_dim_customer` | 财务应收、合同预警 |
| `xycyl_dim_project` | table | `xycyl_stg_project_project` + `xycyl_dim_contract` + `xycyl_dim_customer` | 主项目轴 |
| `xycyl_dim_position` | table | `xycyl_stg_project_position` + floor sources | 摆位层级轴 |
| `xycyl_dim_goods` | table | `xycyl_stg_project_goods_price` + `xycyl_stg_project_goods` | 物品/价格轴 |

## 状态维度

| 模型 | 枚举 |
|---|---|
| `xycyl_dim_project_status` | `standard_code`: `PRJ-ACTIVE` / `PRJ-INACTIVE` / `PRJ-UNKNOWN` |
| `xycyl_dim_project_green_status` | `standard_code`: `PGS-PLACED` / `PGS-CHANGING` / `PGS-ADDING` / `PGS-REDUCING` / `PGS-ADJUSTING` / `PGS-BADDEBT` / `PGS-FINISHED` / `PGS-UNKNOWN` |
| `xycyl_dim_contract_status` | `standard_code`: `CON-DRAFT` / `CON-ACTIVE` / `CON-FINISHED` / `CON-UNKNOWN` |
| `xycyl_dim_position_adjustment_status` | `standard_code`: `PAS-CANCELLED` / `PAS-PENDING` / `PAS-FINISHED` / `PAS-DRAFT` / `PAS-UNKNOWN` |

## DWD / DWS / ADS

| 层 | 模型 | 物化 | 说明 |
|---|---|---|---|
| DWD | `xycyl_dwd_project_green_snapshot` | table | 项目实摆事实，透传 `is_orphan_project` / `is_orphan_position` |
| DWD | `xycyl_dwd_position_adjustment` | table | 摆位调整事实，主表 + 明细打宽 |
| DWS | `xycyl_dws_project_green_monthly` | table | 项目 x 月，实摆行数、数量、租金、成本 raw 汇总；P0 未拍板前不做乘法口径 |
| ADS | `xycyl_ads_project_overview` | table | 项目点实摆总览 |
| ADS | `xycyl_ads_project_green_change_monthly` | table | 绿植加减调换月报 |
| ADS | `xycyl_ads_project_status_dist` | view | 项目和实摆状态分布 |
| ADS | `xycyl_ads_contract_expiry_alert` | view | 合同到期预警 |

## dbt 测试门槛

- sources freshness 不做硬失败，生产 ODS 入湖延迟先以 warn 记录。
- 软外键 relationships 全部 `severity: warn`，不能因为历史孤儿阻塞构建。
- ADS 字段在 schema.yml 中声明中文描述，避免 NL2SQL 直接猜字段。
- 金额类字段统一 `numeric(18,2)` 口径，P0 未拍板前不写死乘法。

## 2026-05-29 本地验证

- `dbt parse`：dbt-fusion 本地命令通过。
- `dbt parse`：DTS `dts-dbt:1.10.0` 容器通过。
- `dbt run --select tag:xycyl-project`：PASS=27，生成 16 table models + 11 view models。
- `dbt test --select tag:xycyl-project`：PASS=50。
- 本地行数受 ODS 空表影响：`xycyl_dim_project` 221、`xycyl_dim_customer` 163、`xycyl_ads_project_overview` 221；实摆、合同、摆位、物品相关模型当前 0 行。

## NL2SQL 后续接入约束

- F2 的 `project.json` 语义包默认只暴露 ADS/DWS，不允许经营汇总类问句访问 ODS。
- 问“某项目有哪些摆位/当前实摆绿植”可走 L0 业务对象，但必须标记 `dataSurface=L0_BUSINESS_OBJECT_PROFILE`。
- F2 应在 `AgentBiReportCatalogService` 增加项目总览、合同到期预警、实摆状态分布三个 report entry。
- F2 应在 `BusinessObjectCatalogService` 增加项目、合同、摆位、实摆绿植四类 object entry。
