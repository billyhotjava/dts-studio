# Sprint-25 Project ODS Create Tables Evidence

**Date**: 2026-05-29  
**Environment**: local `v223-dts-pg-1`, database `biadmin`, schema `public`  
**DDL**: `it/sql/project_ods_create_tables.sql`

## Role Check

| role | result |
|---|---|
| `badmin` | not present in local container |
| `biadmin` | used for local DTS PostgreSQL |

## Apply Command

```bash
docker exec -i v223-dts-pg-1 psql -U biadmin -d biadmin -f - < worklog/v1.0.0/sprint-25-202605/it/sql/project_ods_create_tables.sql
```

Result: applied: 9 ODS tables.

## Created Tables

| ods_table | column_count | row_count |
|---|---:|---:|
| ods_ptr_mysql_b_goods | 22 | 0 |
| ods_ptr_mysql_b_goods_price | 22 | 0 |
| ods_ptr_mysql_p_contract | 48 | 0 |
| ods_ptr_mysql_p_floor_layer | 18 | 0 |
| ods_ptr_mysql_p_floor_number | 17 | 0 |
| ods_ptr_mysql_p_position | 30 | 0 |
| ods_ptr_mysql_p_position_adjustment | 30 | 0 |
| ods_ptr_mysql_p_position_adjustment_item | 26 | 0 |
| ods_ptr_mysql_p_project_green | 40 | 0 |

## Validation

`RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_source_profile_sql.sh` confirmed all 11 Sprint-25 ODS tables are `FOUND` and each has `_dts_source_system` / `_dts_import_time`. Existing `ods_ptr_mysql_p_project` and `ods_ptr_mysql_p_customer` were preserved.
