# Sprint-14: 已知报表优先化与 Copilot 探索兜底 (RF)

**前缀**: RF (Report Fast-path)
**状态**: DONE
**目标**: 锁定首批可产品化的报表清单，优先覆盖财务与采购场景，为后续固定报表快路径、模板目录和 Copilot 探索兜底提供统一入口。

## 背景

客户演示反馈已经明确：当前系统里很多报表需求并不适合继续依赖临时 NL2SQL 现场查库，尤其是财务和采购相关场景。第一步必须先把“已知高频报表”做成可维护的库存清单，后续才能判断哪些报表进入固定模板，哪些问题继续留给 Copilot 探索。

## 范围

```
Sprint-14 Task 1
  └─ RF-01 初始报表目录锁定与候选清单
```

## 任务列表

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| RF-01 | 初始报表目录锁定与候选清单 | P0 | DONE | 2026-03-20 known report fastpath plan |

## 完成标准

- [x] 至少列出 20 个候选报表
- [x] 每个报表包含 domain、user role、freshness、display type、owner
- [x] 财务和采购报表标记为 P0
- [x] 输出结果可直接用于后续模板化和目录化

## 关联文档

- [Task 1 计划](/opt/prod/prs/source/dts-copilot/docs/plans/2026-03-20-known-report-fastpath-and-copilot-exploration-plan.md)
- [RF-01 报表库存清单](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-14/tasks/RF-01-report-inventory.md)
- [IT 验证说明](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-14/it/README.md)

