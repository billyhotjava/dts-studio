# Sprint-13 Warehouse Layer Verification

## Scope

本次核验只回答两个问题：

1. `copilot_analytics` 里实际建立了哪些 ELT/主题层物理表
2. 当前是否已经演进成标准 `dwd/dws/ads` 三层或四层模型

## Verification Commands

```bash
bash worklog/v1.0.0/sprint-13/it/verify_warehouse_layers.sh
```

如本地 PostgreSQL 使用 `15432`：

```bash
PG_PORT=15432 bash worklog/v1.0.0/sprint-13/it/verify_warehouse_layers.sh
```

## What The Script Checks

### Physical tables

- `mart_project_fulfillment_daily`
- `fact_field_operation_event`
- `elt_sync_watermark`

### Required columns

- `mart_project_fulfillment_daily`
  - `snapshot_date`
  - `project_id`
  - `sync_batch_id`
  - `synced_at`
- `fact_field_operation_event`
  - `event_date`
  - `event_month`
  - `biz_type_name`
  - `source_updated_at`
  - `sync_batch_id`
- `elt_sync_watermark`
  - `target_table`
  - `last_watermark`
  - `last_sync_time`
  - `last_sync_rows`
  - `last_sync_duration_ms`
  - `sync_status`

### Logical view layer metadata

脚本同时校验 `business_view_registry` 至少仍包含这些视图元数据：

- `v_project_overview`
- `v_flower_biz_detail`
- `v_project_green_current`
- `v_monthly_settlement`
- `v_task_progress`
- `v_curing_coverage`
- `v_pendulum_progress`

### Explicit absence checks

脚本会明确确认当前不存在：

- `dwd_*`
- `dws_*`
- `ads_*`

## Decision

当前 `dts-copilot` 的仓储分层应继续保持：

- 逻辑层：`business_view_registry` 管理的业务视图层
- 物理层：`mart_project_fulfillment_daily` + `fact_field_operation_event`
- 编排层：`elt_sync_watermark`

这是一套 `view registry + mart/fact` 的轻量双层模型，不是传统 `ODS/DWD/DWS/ADS` 数仓。

## Why We Keep This For Now

1. 当前只稳定承载两个主题域：项目履约、现场业务。
2. `mart/fact` 已能覆盖趋势分析和高频统计优化。
3. 业务视图层已经承担了大量语义收敛职责，再立即引入 `dwd/dws/ads` 会把口径和同步复杂度抬高。
4. 真正需要升级到标准数仓分层的前提，应是：
   - 超过两个稳定主题域同时进入 ELT
   - 跨域指标复用明显增加
   - 需要统一公共明细层，而不是单纯两张主题表
