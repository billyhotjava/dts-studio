# ER-07: 监控、手动触发与编排服务统一

**优先级**: P2
**状态**: READY
**依赖**: ER-02, ER-03, ER-04

## 问题

当前定时任务与手动触发走了两条不同路径，手动触发绕过了 `EltSyncService`，导致状态更新和监控结果不一致。

## 范围

- 收口 `EltMonitorResource`
- 收口 `EltSyncService`
- 收口 `EltSyncScheduler`

## 目标

- 所有同步入口都经过统一编排
- `RUNNING / COMPLETED / FAILED` 只由一个服务负责
- 手动触发与定时任务输出同构监控数据

## 验收标准

- [ ] 手动触发不再直接调用 `job.sync(...)`
- [ ] 监控接口输出与定时任务状态一致
- [ ] 同步日志字段格式统一

