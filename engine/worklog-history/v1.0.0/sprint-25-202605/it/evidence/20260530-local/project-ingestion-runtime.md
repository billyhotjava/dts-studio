# Sprint-25 Project Ingestion Runtime Evidence

**Date**: 2026-05-30  
**Environment**: local DTS runtime, `v223-dts-pg-1`, `v223-dts-ingestion-1`, `biadmin.public`

## Trigger Path

The ingestion service listens on internal port `8083`. The successful trigger used the trusted service header from the platform network:

```bash
docker exec v223-dts-platform-1 sh -lc \
  "curl -sS -i -X POST -H 'X-DTS-Service: dts-platform' \
   http://dts-ingestion:8083/api/ingestion/tasks/46/execute/async"
```

Result:

```text
HTTP/1.1 202 Accepted
{"taskId":46,"taskName":"sprint25_project_datasurface","status":"submitted","async":true}
```

## Task State

```text
 id |             name             | status | mapped_tables | last_execution_status |     last_executed_at
----+------------------------------+--------+---------------+-----------------------+---------------------------
 46 | sprint25_project_datasurface | active |            11 | success               | 2026-05-30 15:24:43.06848
```

Latest relevant executions:

```text
 id |               execution_id               | status  | retry_count | failure_category |         start_time         |          end_time
----+------------------------------------------+---------+-------------+------------------+----------------------------+----------------------------
 82 | codex-sprint25-trigger-20260530231816    | failed  |           0 | RUNTIME_ERROR    | 2026-05-30 23:18:16.027094 | 2026-05-30 23:18:16.027094
 83 | manual__2026-05-30T15:24:11.351264+00:00 | success |           0 |                  | 2026-05-30 15:24:10.905034 | 2026-05-30 15:24:43.06848
```

## ODS Row Counts

```text
                ods_table                 | row_count
------------------------------------------+-----------
 ods_ptr_mysql_b_goods                    |      6027
 ods_ptr_mysql_b_goods_price              |      6517
 ods_ptr_mysql_p_contract                 |       306
 ods_ptr_mysql_p_customer                 |       180
 ods_ptr_mysql_p_floor_layer              |      1679
 ods_ptr_mysql_p_floor_number             |       362
 ods_ptr_mysql_p_position                 |     17396
 ods_ptr_mysql_p_position_adjustment      |      6016
 ods_ptr_mysql_p_position_adjustment_item |     21009
 ods_ptr_mysql_p_project                  |       242
 ods_ptr_mysql_p_project_green            |     36295
```

## Verification

```bash
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_ingestion_runtime.sh
```

```text
[static] sprint25 project ingestion task upsert SQL is present
[live] sprint25 project ingestion task maps 11 tables, latest execution succeeded, and required ODS tables are populated
```
