# xycyl-project dbt 治理说明

## 命名

- 包名：`xycyl_project`
- 域 tag：`xycyl-project`
- STG：`xycyl_stg_project_*`
- 共享维度：`xycyl_dim_customer`、`xycyl_dim_project`、`xycyl_dim_position`、`xycyl_dim_goods`、`xycyl_dim_contract`
- 项目事实：`xycyl_dwd_project_green_snapshot`、`xycyl_dwd_position_adjustment`
- 项目 ADS：`xycyl_ads_project_*`、`xycyl_ads_contract_*`

## 状态码

状态维度统一输出稳定 ASCII `standard_code`，展示使用 `label`，下游不直接用中文做 join key。

| dim | example standard_code |
|---|---|
| project status | `PRJ-ACTIVE` |
| contract status | `CON-ACTIVE` |
| project green status | `PGS-PLACED` |
| position adjustment status | `PAS-PENDING` |

## P0 待复核

- `p_project_green.parent_id` 顶层行/子项行如何计入实摆组数。
- `rent` / `cost` 是否为单价，以及是否应乘 `total_number` 或 `good_number`。
- `import_status=2` 是否作为已确认实摆过滤条件。
- 停用项目、停用摆位是否进入 ADS 默认口径。
