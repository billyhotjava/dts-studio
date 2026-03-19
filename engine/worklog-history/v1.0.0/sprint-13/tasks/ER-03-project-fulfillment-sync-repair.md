# ER-03: 项目履约主题表同步链修复

**优先级**: P0
**状态**: READY
**依赖**: ER-01, ER-02

## 问题

项目履约主题 Job 当前写入了错误的目标表和字段名，无法保证 `mart_project_fulfillment_daily` 真正可用。

## 范围

- 修正 `ProjectFulfillmentSyncJob`
- 明确快照粒度：`project_id × snapshot_date`
- 统一项目状态、结算方式、经营度量字段

## 关键核对

- `project_status_name`
- `settlement_type_name`
- `change_flower_count`
- `sync_batch_id`
- 当日快照的唯一键和覆盖更新策略

## 验收标准

- [ ] 可对同一项目同一天执行 UPSERT
- [ ] 生成的数据可支撑 Sprint-11 中的 mart 模板 SQL
- [ ] 与 `mart_project_fulfillment_daily` 的 DDL 完整对齐

