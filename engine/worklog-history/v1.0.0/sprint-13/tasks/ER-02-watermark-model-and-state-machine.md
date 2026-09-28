# ER-02: Watermark 模型与状态机收口

**优先级**: P0
**状态**: READY
**依赖**: EL-02, EL-07

## 问题

`EltSyncWatermark` 实体字段与 `elt_sync_watermark` 表结构不一致，导致同步状态、统计行数和最后同步时间的持久化语义不可靠。

## 范围

- 对齐 JPA 实体与 Liquibase schema
- 对齐 `EltWatermarkService`
- 对齐 `EltSyncService`
- 明确 `RUNNING / COMPLETED / FAILED` 状态机

## 重点

- 消除 `status` vs `sync_status`
- 消除 `last_row_count` vs `last_sync_rows`
- 明确是否保留 `last_batch_id`
- 明确失败信息与持续时长字段

## 验收标准

- [ ] 实体字段与数据库列一一对应
- [ ] 编排服务只依赖一套状态机字段
- [ ] 监控接口输出与持久化字段一致

