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
| ods_ptr_mysql_b_goods | FOUND |
| ods_ptr_mysql_b_goods_price | FOUND |
| ods_ptr_mysql_p_contract | FOUND |
| ods_ptr_mysql_p_customer | FOUND |
| ods_ptr_mysql_p_floor_layer | FOUND |
| ods_ptr_mysql_p_floor_number | FOUND |
| ods_ptr_mysql_p_position | FOUND |
| ods_ptr_mysql_p_position_adjustment | FOUND |
| ods_ptr_mysql_p_position_adjustment_item | FOUND |
| ods_ptr_mysql_p_project | FOUND |
| ods_ptr_mysql_p_project_green | FOUND |

## Found Table Metadata

| ods_table | has `_dts_source_system` | has `_dts_import_time` | column_count |
|---|---:|---:|---:|
| ods_ptr_mysql_b_goods | 1 | 1 | 22 |
| ods_ptr_mysql_b_goods_price | 1 | 1 | 22 |
| ods_ptr_mysql_p_contract | 1 | 1 | 48 |
| ods_ptr_mysql_p_customer | 1 | 1 | 24 |
| ods_ptr_mysql_p_floor_layer | 1 | 1 | 18 |
| ods_ptr_mysql_p_floor_number | 1 | 1 | 17 |
| ods_ptr_mysql_p_position | 1 | 1 | 30 |
| ods_ptr_mysql_p_position_adjustment | 1 | 1 | 30 |
| ods_ptr_mysql_p_position_adjustment_item | 1 | 1 | 26 |
| ods_ptr_mysql_p_project | 1 | 1 | 43 |
| ods_ptr_mysql_p_project_green | 1 | 1 | 40 |

## Found Table Row Counts

| ods_table | rows |
|---|---:|
| ods_ptr_mysql_b_goods | 0 |
| ods_ptr_mysql_b_goods_price | 0 |
| ods_ptr_mysql_p_contract | 0 |
| ods_ptr_mysql_p_customer | 178 |
| ods_ptr_mysql_p_floor_layer | 0 |
| ods_ptr_mysql_p_floor_number | 0 |
| ods_ptr_mysql_p_position | 0 |
| ods_ptr_mysql_p_position_adjustment | 0 |
| ods_ptr_mysql_p_position_adjustment_item | 0 |
| ods_ptr_mysql_p_project | 240 |
| ods_ptr_mysql_p_project_green | 0 |

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

F0-T01 can close because all required `public.ods_ptr_mysql_*` physical tables now exist in local DTS. F0-T02/F1 remain blocked for business conclusions because the 9 newly created project-domain ODS tables are empty; source ingestion still needs to populate `ods_ptr_mysql_p_project_green`, `ods_ptr_mysql_p_position`, `ods_ptr_mysql_p_contract`, `ods_ptr_mysql_b_goods`, `ods_ptr_mysql_b_goods_price`, and `ods_ptr_mysql_p_position_adjustment*`.
