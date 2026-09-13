# F3: 数据新鲜度、血缘与对账执行

**优先级**: P0
**状态**: DONE

## 目标

把 ODS 入湖、dbt 构建、lineage 和 tie-out 结果接入 evidence，证明 SQL 查询的不是空表、旧表或错误模型。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 入湖与 dbt 新鲜度探针 | P0 | DONE | F1-T01 |
| T02 | 对账执行器与 tie-out adapter | P0 | DONE | T01 |
| T03 | Evidence 持久化与审计检索 | P1 | DONE | T02 |

## 完成标准

- [x] evidence 能说明 ODS/dbt 最近一次成功状态。
- [x] 至少支持 count/sum/tie-out 三类对账。
- [x] 历史回答能追溯当时的 evidence。
