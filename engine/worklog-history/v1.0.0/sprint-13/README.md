# Sprint-13: ELT 主题层收口与数仓分层整改 (ER)

**前缀**: ER (ELT Remediation)
**状态**: READY
**目标**: 收口 Sprint-11 的 ELT 主题层实现偏差，修复物理表/同步任务/水位状态机不一致问题，并明确当前是否继续维持轻量 `mart/fact` 模型，还是演进到 `dwd/dws/ads` 分层。

## 背景

在对 `worklog/v1.0.0/sprint-11` 的代码与文档复核后，确认 Sprint-11 已经落了一部分 ELT 结构，但整体还不能判断为“功能完成”：

- 物理表 DDL 已存在，但同步 Job 与表结构不一致
- watermark 实体与 Liquibase schema 不一致
- `routeWithDataLayer()` 已存在，但没有进入聊天主链
- 同步监控存在接口，但未与健康判定和自动降级联动
- `it/` 目录只有 README，没有可执行验收脚本

## 现状核实

### 代码层

当前代码仓只看到三张 ELT 相关物理表：

- `mart_project_fulfillment_daily`
- `fact_field_operation_event`
- `elt_sync_watermark`

未发现以下典型数仓分层命名：

- `dwd_*`
- `dws_*`
- `ads_*`

### 数据库层

已直接连接本地 `copilot_analytics` schema 核实，当前库内同样只有：

- `copilot_analytics.mart_project_fulfillment_daily`
- `copilot_analytics.fact_field_operation_event`
- `copilot_analytics.elt_sync_watermark`

结论：

- **有新的 ELT/主题层物理表**
- **没有建立标准三层/四层数仓模型**
- 当前实现属于 **轻量 `mart/fact + watermark`**，不是典型 `ODS/DWD/DWS/ADS`

## 整改范围

```
Sprint-11 评审问题
  ├─ P0 运行阻断
  │    ├─ 物理表 schema 与 Job SQL 对齐
  │    ├─ watermark 模型与状态机对齐
  │    └─ Project / Field Operation 两条同步链可真正落表
  │
  ├─ P1 主链接通
  │    ├─ data-layer 路由进入 chat / nl2sql 主链
  │    └─ mart 健康检查与自动降级
  │
  └─ P2 验收与演进
       ├─ 监控/触发路径统一
       ├─ IT / 性能基线补齐
       └─ 数仓分层策略决策（保持 mart-only 或升级 DWD/DWS/ADS）
```

## 任务列表

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| ER-01 | ELT 物理表与同步 SQL 对齐 | P0 | READY | EL-01, EL-03, EL-04 |
| ER-02 | Watermark 模型与状态机收口 | P0 | READY | EL-02, EL-07 |
| ER-03 | 项目履约主题表同步链修复 | P0 | READY | ER-01, ER-02 |
| ER-04 | 现场业务事实表同步链修复 | P0 | READY | ER-01, ER-02 |
| ER-05 | Data-layer 路由接入 Copilot 主链 | P1 | READY | EL-05, ER-03, ER-04 |
| ER-06 | 主题层健康检查与自动降级 | P1 | READY | ER-02, ER-05 |
| ER-07 | 监控、手动触发与编排服务统一 | P2 | READY | ER-02, ER-03, ER-04 |
| ER-08 | IT 验收与性能基线补齐 | P2 | READY | ER-03~ER-07 |
| ER-09 | 数仓分层策略与落库核验 | P1 | READY | ER-03, ER-04 |

## 完成标准

- [ ] `mart_project_fulfillment_daily` 与 `fact_field_operation_event` 能真实增量落表
- [ ] `elt_sync_watermark` 的实体、表结构、状态机一致
- [ ] 主题层路由真正进入聊天主链，而不是停留在孤立方法
- [ ] 主题层不可用时，可基于健康状态回退到视图层
- [ ] 手动触发与定时调度使用同一编排服务
- [ ] `it/` 目录具备可执行脚本或自动化测试，而不是只有文档
- [ ] 明确当前是否继续采用 `mart/fact` 轻量模型，还是升级到 `dwd/dws/ads`

