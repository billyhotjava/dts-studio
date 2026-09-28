# EL-06: 主题层预制查询模板补充

**优先级**: P1
**状态**: READY
**依赖**: EL-03, EL-04

## 目标

为主题层表补充 10+ 条预制查询模板，覆盖趋势分析、月度对比、环比等场景。

## 模板清单

### mart_project_fulfillment_daily 相关

**TPL-21**: 项目绿植数月度趋势
- 问句: "XX项目最近3个月绿植数量趋势"
- SQL: `SELECT snapshot_date, green_count FROM mart_project_fulfillment_daily WHERE project_name LIKE ? AND snapshot_date >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH) ORDER BY snapshot_date`

**TPL-22**: 各项目加花次数月度对比
- 问句: "各项目最近3个月加花次数对比"

**TPL-23**: 项目月度应收环比
- 问句: "各项目上月和本月应收对比"

**TPL-24**: 项目经营排名（综合）
- 问句: "本月项目经营综合排名"

**TPL-25**: 养护覆盖率月度趋势
- 问句: "各项目养护覆盖率月度变化"

### fact_field_operation_event 相关

**TPL-26**: 加花月度趋势
- 问句: "最近半年各月加花次数趋势"

**TPL-27**: 各类型业务月度分布趋势
- 问句: "最近3个月各类型报花业务趋势"

**TPL-28**: 紧急业务占比趋势
- 问句: "各月紧急报花单占比变化"

**TPL-29**: 项目报花成本月度趋势
- 问句: "XX项目最近半年报花成本趋势"

**TPL-30**: 养护人工作量月度分布
- 问句: "各养护人最近3个月工作量对比"

## 完成标准

- [ ] 10+ 条主题层模板入库
- [ ] 模板 SQL 均引用主题层表
- [ ] 与 EL-05 路由扩展联动：趋势问句命中模板时走主题层
