# 馨懿诚项目域 dbt Model (xycyl-project)

Sprint-25 可导入 DTS 平台的项目管理域 dbt 包。

## 边界

- ODS source 指向 `public.ods_ptr_mysql_*`，不使用独立 `xycyl_ods` schema。
- STG 模型使用 `xycyl_stg_project_*` 命名，避免覆盖 Sprint-22 报花包里的 `xycyl_stg_project` / `xycyl_stg_customer`。
- `p_contract`、`p_position`、`p_project_green` 等 ODS 已通过 Sprint-25 task `46` 入数；本包已在本地 DTS PostgreSQL 完成 `dbt build`。
- 金额字段在 DWD/DWS/ADS 中保留源表 raw 金额合计；adminweb 当前运营口径单独输出 `*_adminweb_*` 字段，不在 P0 未拍板前把 `rent * total_number` 或 `cost * total_number` 固化为最终业务口径。

## 目录

```text
dbt_project.yml
macros/
models/
  xycyl_project_sources.yml
  xycyl_project_schema.yml
  stg/
  dwd/
  dws/
  ads/
ods_ddl/
  project_ods_create_tables.sql
```

## 运行

```bash
dbt parse --profile dts
dbt run --profile dts --select tag:xycyl-project
dbt test --profile dts --select tag:xycyl-project
```

2026-05-30 入数后验证：`PASS=76 WARN=1 ERROR=0`，唯一 warning 为 `p_project_green.project_id` 软外键孤儿。2026-05-31 增加 adminweb ProjectSummary 对账字段后重跑 build，结果仍为 `PASS=76 WARN=1 ERROR=0`。

## 首批 ADS

| model | grain | note |
|---|---|---|
| `xycyl_ads_project_overview` | 项目 | 项目主数据 + 位置/实摆汇总 |
| `xycyl_ads_project_green_change_monthly` | 项目 x 月 x 实摆状态 | 实摆状态月度分布 |
| `xycyl_ads_project_status_dist` | 状态 | 项目/实摆状态分布 |
| `xycyl_ads_contract_expiry_alert` | 合同 | 合同到期预警 |

## adminweb 对账字段

`xycyl_ads_project_overview` 包含 `rent_amount_adminweb_sum`、`cost_amount_adminweb_sum`、`real_good_number_adminweb_sum`、`green_number_adminweb_sum`、`flowerpot_number_adminweb_sum`、`flowerrack_number_adminweb_sum`，用于复现 adminweb `ProjectSummaryMapper.listPage` 当前运营报表。2026-05-31 live 对账 7 项指标全部 PASS，最大误差 `0.0000%`。
