# T04: sprint queue 一致性检查

**优先级**: P1
**状态**: DONE
**依赖**: T01,T02,T03

## 目标

让 `sprint-queue.md` 的局部表格、统计行和总体说明不再互相冲突。

## 技术设计

- 为 S25/S26/S27/S28 手工校验表格状态和统计行。
- 检查同一时间 `IN_PROGRESS` 项是否需要收口说明。
- 可增加一个轻量检查脚本或文档化命令,但不为此引入新依赖。

## 影响范围

- `worklog/v1.0.0/sprint-queue.md`
- `worklog/v1.0.0/sprint-28-202605/it/evidence/20260531-local/f4-queue-consistency.md`

## 验证

- [x] `rg -n "Sprint-25|Sprint-26|Sprint-27|Sprint-28|统计" worklog/v1.0.0/sprint-queue.md`
- [x] 证据文件列出校验后的状态矩阵。

## 完成标准

- [x] queue 的 Sprint-25~28 段落自洽。
- [x] 总体说明不掩盖 BLOCKED/IN_PROGRESS。
