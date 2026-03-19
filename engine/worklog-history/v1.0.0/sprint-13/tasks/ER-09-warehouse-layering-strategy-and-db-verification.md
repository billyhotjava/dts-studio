# ER-09: 数仓分层策略与落库核验

**优先级**: P1
**状态**: READY
**依赖**: ER-03, ER-04

## 目标

明确当前 ELT 的最终定位：继续维持轻量 `mart/fact` 主题层，还是演进到标准 `dwd/dws/ads` 模型。

## 当前结论

已核实当前 `copilot_analytics` 库内只有：

- `mart_project_fulfillment_daily`
- `fact_field_operation_event`
- `elt_sync_watermark`

未发现：

- `dwd_*`
- `dws_*`
- `ads_*`

## 需要输出

- 当前物理分层现状说明
- 是否保留 `mart/fact-only` 的产品理由
- 若升级到 `dwd/dws/ads`，对应边界、命名和迁移顺序

## 验收标准

- [ ] 形成明确的分层决策，而不是口头约定
- [ ] 数据库实际落库情况有核验记录
- [ ] 后续 Sprint 能基于该决策继续扩展，而不反复改口径

