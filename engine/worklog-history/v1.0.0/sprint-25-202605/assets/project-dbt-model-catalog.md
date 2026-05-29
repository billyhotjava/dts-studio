# 项目域 dbt 模型目录（Sprint-25）

> 本目录定义目标模型，不代表已完成生产实现。P0 口径决策 RESOLVED 后，按此清单逐步落地。

## Sources

`models/xycyl/xycyl_project_sources.yml`：

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
| `xycyl_stg_project` | view | 过滤软删项目，保留 status/type 原始值 |
| `xycyl_stg_customer` | view | 客户主数据标准化 |
| `xycyl_stg_contract` | view | 合同状态、结算方式原始字段标准化 |
| `xycyl_stg_position` | view | 摆位、楼层、楼号关联键标准化 |
| `xycyl_stg_goods` | view | 物品主数据标准化 |
| `xycyl_stg_goods_price` | view | 物品价格与成本字段标准化 |
| `xycyl_stg_project_green` | view | 实摆事实，保留 parent/import/status/lock 字段 |
| `xycyl_stg_position_adjustment` | view | 摆位调整主表 |
| `xycyl_stg_position_adjustment_item` | view | 摆位调整明细 |

## 共享维度

| 模型 | 物化 | 上游 | 用途 |
|---|---|---|---|
| `xycyl_dim_customer` | table | `xycyl_stg_customer` | 采购、财务、项目共用客户轴 |
| `xycyl_dim_contract` | table | `xycyl_stg_contract` | 财务应收、合同预警 |
| `xycyl_dim_project` | table | `xycyl_stg_project` + `xycyl_dim_contract` + `xycyl_dim_customer` | 主项目轴 |
| `xycyl_dim_position` | table | `xycyl_stg_position` + floor sources | 摆位层级轴 |
| `xycyl_dim_goods` | table | `xycyl_stg_goods_price` + `xycyl_stg_goods` | 物品/价格轴 |

## 状态维度

| 模型 | 枚举 |
|---|---|
| `xycyl_dim_project_status` / `xycyl_dim_project_status_alias` | `1` 正常，`2` 停用 |
| `xycyl_dim_project_green_status` / `xycyl_dim_project_green_status_alias` | `1` 摆放中，`2` 换花中，`3` 加花中，`4` 减花中，`5` 调花中，`6` 坏账处理中，`7` 已结束 |
| `xycyl_dim_contract_status` / `xycyl_dim_contract_status_alias` | `1` 草稿，`2` 履行中，`3` 已结束 |
| `xycyl_dim_position_adjustment_status` / `xycyl_dim_position_adjustment_status_alias` | `-1` 已作废，`0` 待确认，`1` 已结束，`10` 草稿 |

## DWD / DWS / ADS

| 层 | 模型 | 物化 | 说明 |
|---|---|---|---|
| DWD | `xycyl_dwd_project_green_snapshot` | table | 项目实摆事实，透传 `is_orphan_project` / `is_orphan_position` |
| DWD | `xycyl_dwd_position_adjustment` | table | 摆位调整事实，主表 + 明细打宽 |
| DWS | `xycyl_dws_project_green_monthly` | table | 项目 x 月，实摆组数、数量、租金、成本双口径 |
| ADS | `xycyl_ads_project_overview` | table | 项目点实摆总览 |
| ADS | `xycyl_ads_project_green_change_monthly` | table | 绿植加减调换月报 |
| ADS | `xycyl_ads_project_status_dist` | view | 项目和实摆状态分布 |
| ADS | `xycyl_ads_contract_expiry_alert` | view | 合同到期预警 |

## dbt 测试门槛

- sources freshness 不做硬失败，生产 ODS 入湖延迟先以 warn 记录。
- 软外键 relationships 全部 `severity: warn`，不能因为历史孤儿阻塞构建。
- ADS 中文列名要在 schema.yml 中声明描述，避免 NL2SQL 直接猜字段。
- 金额类字段统一 `numeric(18,2)` 口径，P0 未拍板前不写死乘法。

## NL2SQL 对接面

- `project.json` 语义包默认只暴露 ADS/DWS，不允许经营汇总类问句访问 ODS。
- 问“某项目有哪些摆位/当前实摆绿植”可走 L0 业务对象，但必须标记 `dataSurface=L0_BUSINESS_OBJECT_PROFILE`。
- `AgentBiReportCatalogService` 新增项目总览、合同到期预警、实摆状态分布三个 report entry。
- `BusinessObjectCatalogService` 新增项目、合同、摆位、实摆绿植四类 object entry。
