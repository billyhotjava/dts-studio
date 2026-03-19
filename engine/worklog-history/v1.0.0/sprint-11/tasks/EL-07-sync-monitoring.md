# EL-07: 同步监控与告警

**优先级**: P2
**状态**: READY
**依赖**: EL-02

## 目标

实现 ELT 同步任务的监控能力，包括同步状态查询、延迟告警和手动触发。

## 技术设计

### 监控 API

```
GET  /api/analytics/elt/status          — 查看所有表的同步状态
POST /api/analytics/elt/trigger/{table} — 手动触发同步
POST /api/analytics/elt/reset/{table}   — 重置 watermark（重新全量同步）
```

### 响应示例

```json
[
  {
    "targetTable": "mart_project_fulfillment_daily",
    "syncStatus": "IDLE",
    "lastSyncTime": "2026-03-20T10:00:00Z",
    "lastSyncRows": 156,
    "lastSyncDurationMs": 2340,
    "syncDelayMinutes": 45,
    "isHealthy": true
  },
  {
    "targetTable": "fact_field_operation_event",
    "syncStatus": "IDLE",
    "lastSyncTime": "2026-03-20T10:00:00Z",
    "lastSyncRows": 89,
    "lastSyncDurationMs": 1200,
    "syncDelayMinutes": 45,
    "isHealthy": true
  }
]
```

### 健康判定

- `isHealthy = true`：最近一次同步成功且延迟 < 阈值（默认 2 小时）
- `isHealthy = false`：最近一次同步失败或延迟 > 阈值

### 日志输出

每次同步完成后输出结构化日志，便于运维对接：

```
[ELT] table=mart_project_fulfillment_daily status=COMPLETED rows=156 duration=2340ms watermark=2026-03-20T09:30:00Z
```

## 完成标准

- [ ] 3 个监控 API 实现
- [ ] 健康判定逻辑
- [ ] 结构化日志输出
- [ ] 手动触发和重置功能
