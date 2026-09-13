# T03: Evidence 持久化与审计检索

**优先级**: P1
**状态**: DONE
**依赖**: T02

## 目标

把每次回答的准确性证据保存下来，后续能按 session、message、template、target relation 查回。

## 技术设计

- 保存脱敏 SQL hash、目标表、grade、score、reasons、tieout summary。
- 不保存明文密码、token、JDBC 连接串。
- 与已有 route telemetry / finance audit trail 对齐。

## 影响范围

- chat message persistence。
- audit trail query。
- admin/debug 页面或内部接口。

## 验证

- [x] 回答完成后能查到 evidence。
- [x] 敏感信息扫描无 JDBC password/token。
- [x] evidence 与原 messageId/sessionId 关联。

## 完成标准

- [x] 准确性证据可审计、可回放、可脱敏展示。
