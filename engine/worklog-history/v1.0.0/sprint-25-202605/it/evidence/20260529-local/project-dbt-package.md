# Sprint-25 Project dbt Package Evidence

**Date**: 2026-05-29  
**Environment**: local DTS PostgreSQL `v223-dts-pg-1`, database `biadmin`, schema `public`  
**Package**: `assets/xycyl-project-dbt-model.zip`

## Package Summary

| item | count |
|---|---:|
| sources | 11 |
| models | 27 |
| data tests | 50 |
| STG views | 11 |
| table models | 16 |

## Local Validation

| check | result |
|---|---|
| `bash it/test_project_dbt_package.sh` | PASS |
| `RUN_DBT_PARSE=1 bash it/test_project_dbt_package.sh` | PASS |
| dbt-core container `parse` | PASS |
| dbt-core container `run --select tag:xycyl-project` | PASS=27, WARN=0, ERROR=0 |
| dbt-core container `test --select tag:xycyl-project` | PASS=50, WARN=0, ERROR=0 |

Summary: dbt run: PASS=27; dbt test: PASS=50.

## Local Model Row Counts

| model | rows |
|---|---:|
| `xycyl_dim_customer` | 163 |
| `xycyl_dim_project` | 221 |
| `xycyl_ads_project_overview` | 221 |
| `xycyl_dim_contract` | 0 |
| `xycyl_dim_position` | 0 |
| `xycyl_dim_goods` | 0 |
| `xycyl_dwd_project_green_snapshot` | 0 |
| `xycyl_dwd_position_adjustment` | 0 |
| `xycyl_dws_project_green_monthly` | 0 |

## Notes

- The package uses `source name: xycyl_project_ods`, `schema: public`, and `identifier: ods_ptr_mysql_*`.
- STG names use `xycyl_stg_project_*` to avoid overwriting Sprint-22 flowerbiz `xycyl_stg_project` / `xycyl_stg_customer`.
- Status dimensions output stable ASCII `standard_code` values such as `PRJ-ACTIVE`, `PGS-PLACED`, and `PAS-PENDING`.
- Facts are structurally valid but mostly empty because 9 newly created project-domain ODS tables have not received source rows yet.
- Rent/cost fields remain raw sums; P0 has not resolved whether to multiply by `total_number` or `good_number`.
