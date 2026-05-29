# Sprint-25 Project Source Profile Evidence

**Date**: 2026-05-29  
**Environment**: local `v223-dts-pg-1`, database `biadmin`, schema `public`  
**Command**:

```bash
docker exec v223-dts-pg-1 psql -U biadmin -d biadmin -f /path/to/project_source_profile.sql
```

## Source Presence

| ods_table | status |
|---|---|
| ods_ptr_mysql_b_goods | MISSING |
| ods_ptr_mysql_b_goods_price | MISSING |
| ods_ptr_mysql_p_contract | MISSING |
| ods_ptr_mysql_p_customer | FOUND |
| ods_ptr_mysql_p_floor_layer | MISSING |
| ods_ptr_mysql_p_floor_number | MISSING |
| ods_ptr_mysql_p_position | MISSING |
| ods_ptr_mysql_p_position_adjustment | MISSING |
| ods_ptr_mysql_p_position_adjustment_item | MISSING |
| ods_ptr_mysql_p_project | FOUND |
| ods_ptr_mysql_p_project_green | MISSING |

## Found Table Metadata

| ods_table | has `_dts_source_system` | has `_dts_import_time` | column_count |
|---|---:|---:|---:|
| ods_ptr_mysql_p_customer | 1 | 1 | 24 |
| ods_ptr_mysql_p_project | 1 | 1 | 43 |

## Found Table Row Counts

| ods_table | rows |
|---|---:|
| ods_ptr_mysql_p_customer | 178 |
| ods_ptr_mysql_p_project | 240 |

## `p_project` Status Distribution

| status | rows |
|---|---:|
| 1 | 170 |
| 2 | 70 |

## `p_customer` Status Distribution

| status | rows |
|---|---:|
| 1 | 173 |
| 2 | 5 |

## Implementation Consequence

F0-T01 can close because the入湖范围 is now verified. F0-T02/F1 remain blocked until the missing project-domain ODS tables are produced, especially `ods_ptr_mysql_p_project_green`, `ods_ptr_mysql_p_position`, `ods_ptr_mysql_p_contract`, `ods_ptr_mysql_b_goods`, and `ods_ptr_mysql_b_goods_price`.
