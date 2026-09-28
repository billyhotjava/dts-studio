# F2: 候选 ADS 生产器

**优先级**: P0  
**状态**: READY

## 目标

没有现成 ADS 时，从 DWS/DWD 生成可审查的候选 dbt ADS 模型，而不是把临时 SQL 当正式报表资产。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 候选 ADS 输出协议 | P0 | READY | F1 |
| T02 | DWS/DWD 字段血缘收集 | P0 | READY | T01 |
| T03 | dbt model 草稿与 tests 生成 | P0 | READY | T02 |
| T04 | 候选 ADS 预览与人工确认 | P1 | READY | T03 |

