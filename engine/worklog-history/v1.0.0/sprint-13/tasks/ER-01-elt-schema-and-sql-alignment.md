# ER-01: ELT 物理表与同步 SQL 对齐

**优先级**: P0
**状态**: READY
**依赖**: EL-01, EL-03, EL-04

## 问题

Sprint-11 中两个同步 Job 的目标表名和 UPSERT 字段，与 Liquibase 建表结构不一致，当前存在“DDL 一套、Job SQL 另一套”的问题。

## 范围

- 对齐 `0038_elt_mart_tables.xml`
- 对齐 `ProjectFulfillmentSyncJob`
- 对齐 `FieldOperationSyncJob`
- 统一字段命名、必填列、唯一键和索引假设

## 核对项

- 目标表名是否与 Liquibase 一致
- UPSERT 列名是否与建表列一致
- NOT NULL 列是否全部赋值
- `sync_batch_id / source_updated_at / event_date` 等审计列是否真实落库

## 验收标准

- [ ] 两个 Job 的 `targetTable()` 与 Liquibase 表名完全一致
- [ ] 所有 INSERT/UPSERT 列可在目标表找到
- [ ] 不再存在历史草稿字段名残留

