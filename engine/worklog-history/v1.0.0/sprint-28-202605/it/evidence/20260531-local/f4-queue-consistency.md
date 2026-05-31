# F4-T04 Sprint Queue 一致性检查

## 范围

校验 `worklog/v1.0.0/sprint-queue.md` 中 Sprint-25~28 的局部状态、统计行和说明。

## 校准后矩阵

| Sprint | READY | IN_PROGRESS | DONE | BLOCKED | 说明 |
|--------|-------|-------------|------|---------|------|
| Sprint-25 | 0 | 1 | 3 | 0 | F0 业务口径仍推进;F1/F2/F3 工程 baseline 已 DONE |
| Sprint-26 | 0 | 0 | 5 | 0 | README、features、IT 证据均为 DONE |
| Sprint-27 | 0 | 0 | 34 | 0 | IT04/07/08/09 证据已补齐并清理矛盾备注 |
| Sprint-28 | 0 | 0 | 17 | 0 | F1/F2/F3/F4/F5 均 DONE |

## 校验命令

```bash
rg -n "Sprint-25|Sprint-26|Sprint-27|Sprint-28|统计" worklog/v1.0.0/sprint-queue.md
rg -n "DONE_BASELINE" worklog/v1.0.0/sprint-25-202605
```

结果:

- S25~S28 表格状态与统计行一致。
- `worklog/v1.0.0/sprint-25-202605` 不再出现非标准 `DONE_BASELINE`。
- 总体说明已改为“历史 Sprint 全量重算待后续处理”,不再掩盖 S25 的业务口径 `IN_PROGRESS`。

## 结论

F4 完成。S25/S26/S27 的状态不再与当前证据自相矛盾;S28 后续 F5 与最终验证也已关闭。
