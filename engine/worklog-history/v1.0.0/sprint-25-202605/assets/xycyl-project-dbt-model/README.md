# 馨懿诚项目域 dbt Model (xycyl-project)

Sprint-25 可导入 DTS 平台的项目管理域 dbt 包。

## 边界

- ODS source 指向 `public.ods_ptr_mysql_*`，不使用独立 `xycyl_ods` schema。
- STG 模型使用 `xycyl_stg_project_*` 命名，避免覆盖 Sprint-22 报花包里的 `xycyl_stg_project` / `xycyl_stg_customer`。
- 新补的 `p_contract`、`p_position`、`p_project_green` 等 ODS 当前为空表；本包能 parse/import，事实口径仍需入数后复核。
- 金额字段在 DWD/DWS/ADS 中保留源表 raw 金额合计，不在 P0 未拍板前固化 `rent * total_number` 或 `cost * total_number` 口径。

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

## 首批 ADS

| model | grain | note |
|---|---|---|
| `xycyl_ads_project_overview` | 项目 | 项目主数据 + 位置/实摆汇总 |
| `xycyl_ads_project_green_change_monthly` | 项目 x 月 x 实摆状态 | 实摆状态月度分布 |
| `xycyl_ads_project_status_dist` | 状态 | 项目/实摆状态分布 |
| `xycyl_ads_contract_expiry_alert` | 合同 | 合同到期预警 |
