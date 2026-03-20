# Sprint-14 IT

本目录用于 `RF-01` 的文档级验证和人工复核，不包含代码测试。

## Verification Commands

1. `cd /opt/prod/prs/source/dts-copilot && rg -n "P0|report inventory|templateCode" worklog/v1.0.0/sprint-14 docs/plans`
   - 目的: 确认 sprint-14 目录下已存在报表库存清单，并且计划文档中仍能检索到模板化/优先级约束。
   - 预期: 命中 `sprint-14/tasks/RF-01-report-inventory.md` 和当前计划文档。

2. `cd /opt/prod/prs/source/dts-copilot && sed -n '1,220p' worklog/v1.0.0/sprint-14/tasks/RF-01-report-inventory.md`
   - 目的: 复核报表清单是否包含完整的 inventory schema 和至少 20 条候选报表。
   - 预期: 可以直接看到 `domain`、`user role`、`freshness`、`display type`、`owner` 等字段，以及财务/采购 `P0` 条目。

## Review Checklist

- [x] 至少 20 个候选报表
- [x] 财务与采购优先级为 `P0`
- [x] 每条记录都包含统一元数据字段
- [x] 清单覆盖财务、采购、仓库、报花、任务、项目点、运营

