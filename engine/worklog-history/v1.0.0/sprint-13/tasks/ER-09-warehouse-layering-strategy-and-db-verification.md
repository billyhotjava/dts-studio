# ER-09: 数仓分层策略与落库核验

**优先级**: P1
**状态**: DONE
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

- [x] 形成明确的分层决策，而不是口头约定
- [x] 数据库实际落库情况有核验记录
- [x] 后续 Sprint 能基于该决策继续扩展，而不反复改口径

## 最终决策

当前保留 `view registry + mart/fact` 轻量双层模型，不立刻升级到标准 `dwd/dws/ads`。

### 保留理由

1. 当前只有两个稳定主题域进入 ELT：项目履约、现场业务。
2. 业务视图层已经承担语义收敛和 join 简化。
3. 两张主题表已经覆盖了趋势与高频统计的主要性能诉求。
4. 直接引入 `dwd/dws/ads` 会显著抬高同步和口径治理成本。

## 核验产物

- [verify_warehouse_layers.sh](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-13/it/verify_warehouse_layers.sh)
- [warehouse-layer-verification.md](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-13/it/warehouse-layer-verification.md)
