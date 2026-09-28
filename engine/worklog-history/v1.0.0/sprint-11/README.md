# Sprint-11: 轻量 ELT 主题层与增量同步 (EL)

**前缀**: EL (ELT Layer)
**状态**: READY
**目标**: 为项目履约和现场业务两个主题域建立轻量 ELT 主题层，预聚合高频统计查询，解决 Sprint-10 视图层在跨月趋势分析和大范围聚合场景下的性能瓶颈。

## 背景

Sprint-10 通过 MySQL VIEW 层实现了 NL2SQL 的业务接地，但视图是查询时实时计算的，在以下场景存在性能问题：

| 场景 | 涉及视图 | 问题 |
|------|---------|------|
| 半年加花趋势 | v_flower_biz_detail | 扫描 6 个月 t_flower_biz_info，数据量大时 >5s |
| 月度养护覆盖率趋势 | v_curing_coverage | 聚合视图跨月查询需重复计算 |
| 全项目绿植变化对比 | v_project_green_current | 历史快照不可得，视图只反映当前状态 |
| 项目经营综合评分 | 多视图 JOIN | 跨视图聚合无法预计算 |

ELT 主题层通过定时物化解决这些问题，同时保持 Sprint-10 的设计原则：

- **模型只查视图/主题表**，不查原始业务表
- **状态码已翻译**，字段有中文语义
- **内嵌在 analytics 中**，不引入 dbt/Airflow 强依赖

## 架构定位

```
用户问句 → 意图路由（BG-07）
              │
              ├─ 明细/实时类 → Sprint-10 视图层（v_* VIEW）→ 实时查询
              │
              └─ 趋势/统计类 → Sprint-11 主题层（mart_*/fact_* TABLE）→ 预聚合查询
```

路由判定规则扩展：
- 含"趋势/变化/对比/近X月/环比/同比" → 优先走主题层
- 含"明细/列表/具体/查一下" → 走视图层
- 默认走视图层（主题层为性能优化补充，不是替代）

## 任务列表

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| EL-01 | 主题层表结构设计 | P0 | READY | BG-02 |
| EL-02 | 增量同步引擎 | P0 | READY | EL-01 |
| EL-03 | 项目履约日维度主题表 | P0 | READY | EL-01, EL-02 |
| EL-04 | 现场业务事件事实表 | P0 | READY | EL-01, EL-02 |
| EL-05 | 意图路由扩展（视图层 vs 主题层判定） | P1 | READY | EL-03, EL-04 |
| EL-06 | 主题层预制查询模板补充 | P1 | READY | EL-03, EL-04 |
| EL-07 | 同步监控与告警 | P2 | READY | EL-02 |
| EL-08 | IT 集成测试与性能基准 | P2 | READY | EL-01~07 |

## 交付范围

### 两个主题层对象

**mart_project_fulfillment_daily** — 项目履约日维度宽表
- 粒度：项目 × 日期
- 维度：project_name, customer_name, manager_name, settlement_type_name
- 度量：green_count, position_count, total_rent, add_flower_count, change_flower_count, cut_flower_count, transfer_flower_count, curing_count, curing_coverage_rate, pending_task_count
- 用途：项目经营趋势分析、月度环比、项目间对比

**fact_field_operation_event** — 现场业务事件事实表
- 粒度：业务事件（每条报花单据）
- 维度：project_name, biz_type_name, biz_status_name, apply_user_name, curing_user_name, event_date, event_month
- 度量：plant_number, rent, cost, is_urgent
- 用途：事件趋势分析、操作量统计、异常检测

### 增量同步机制

- 同步方式：watermark 驱动，基于 `updated_at` 字段
- 同步频率：小时级（可配置）
- 部署方式：内嵌在 `dts-copilot-analytics` 中，Spring `@Scheduled` 定时任务
- 不引入 dbt / Airflow / 外部调度

## 设计原则

- **不做全库搬运**：只物化两个主题域
- **增量优先**：watermark 驱动，每次只处理变化数据
- **可降级**：主题层不可用时自动回退到视图层
- **可追溯**：主题表字段和指标定义可追溯到 Sprint-10 语义资产

## 完成标准

- [ ] 两个主题层表存在且可增量同步
- [ ] 同步任务可配置启停和频率
- [ ] 趋势类问题路由到主题层，明细类走视图层
- [ ] 主题层查询响应 < 1s（万级数据）
- [ ] 视图层响应 < 3s（万级数据）
- [ ] 主题层不可用时自动降级到视图层
- [ ] 同步延迟监控可观测
- [ ] IT 验收矩阵覆盖趋势/环比/跨月场景
