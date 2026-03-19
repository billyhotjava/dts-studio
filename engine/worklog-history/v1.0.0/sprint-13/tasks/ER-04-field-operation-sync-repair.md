# ER-04: 现场业务事实表同步链修复

**优先级**: P0
**状态**: READY
**依赖**: ER-01, ER-02

## 问题

现场业务事实 Job 当前写入的是另一套草稿字段，且未构造 `event_date / event_month / event_year` 等必需列。

## 范围

- 修正 `FieldOperationSyncJob`
- 明确事件事实表的时间派生规则
- 统一业务类型、业务状态、紧急标记、承担成本类型等中文语义字段

## 关键核对

- `biz_type_name`
- `biz_status_name`
- `is_urgent`
- `bear_cost_type_name`
- `event_date / event_month / event_year`
- `source_updated_at / sync_batch_id`

## 验收标准

- [ ] Job 可真实写入 `fact_field_operation_event`
- [ ] 事件时间维度可用于月度趋势查询
- [ ] 与预制模板 SQL 中使用的字段完全一致

